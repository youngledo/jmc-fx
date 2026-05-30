package com.youngledo.jmcfx.adapter.jmc;

import static org.openjdk.jmc.common.item.Attribute.attr;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IMCThread;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;

import com.youngledo.jmcfx.domain.model.JavaFxEventReport;
import com.youngledo.jmcfx.domain.model.JavaFxInputEvent;
import com.youngledo.jmcfx.domain.model.JavaFxPulsePhase;
import com.youngledo.jmcfx.domain.model.JavaFxPulseSummary;
import com.youngledo.jmcfx.domain.model.RecordingSummary;
import com.youngledo.jmcfx.domain.service.JavaFxEventService;

public class JmcJavaFxEventService implements JavaFxEventService {

    private static final String TYPE_ID_PULSE_PHASE_8 =
            "http://www.oracle.com/technetwork/java/javafx/index.html/javafx/pulse";
    private static final String TYPE_ID_INPUT_8 =
            "http://www.oracle.com/technetwork/java/javafx/index.html/javafx/input";
    private static final String TYPE_ID_PULSE_PHASE_12 = "javafx.PulsePhase";
    private static final String TYPE_ID_INPUT_12 = "javafx.Input";
    private static final long SLOW_PHASE_THRESHOLD_MICROS = 16_667;

    private static final IAttribute<IQuantity> PULSE_ID_8 =
            attr("pulseNumber", "Pulse ID", UnitLookup.NUMBER);
    private static final IAttribute<IQuantity> PULSE_ID_12 =
            attr("pulseId", "Pulse ID", UnitLookup.NUMBER);
    private static final IAttribute<String> PHASE_NAME_8 =
            attr("phase", "Phase Name", UnitLookup.PLAIN_TEXT);
    private static final IAttribute<String> PHASE_NAME_12 =
            attr("phaseName", "Phase Name", UnitLookup.PLAIN_TEXT);
    private static final IAttribute<String> INPUT_TYPE =
            attr("input", "Input Type", UnitLookup.PLAIN_TEXT);

    @Override
    public JavaFxEventReport loadJavaFxEvents(RecordingSummary recording) {
        IItemCollection events = JmcRecordingDataCache.SHARED.events(recording);
        List<JavaFxPulsePhase> phases = readPulsePhases(events);
        List<JavaFxInputEvent> inputs = readInputEvents(events);
        List<JavaFxPulseSummary> summaries = summarizePulses(phases);
        long maxPhaseDurationMicros = phases.stream()
                .mapToLong(JavaFxPulsePhase::durationMicros)
                .max()
                .orElse(0);
        long slowPhaseCount = phases.stream()
                .filter(phase -> phase.durationMicros() > SLOW_PHASE_THRESHOLD_MICROS)
                .count();

        return new JavaFxEventReport(
                summaries.size(),
                phases.size(),
                inputs.size(),
                slowPhaseCount,
                maxPhaseDurationMicros,
                summaries,
                JmcResultLimiter.limitRows(phases),
                JmcResultLimiter.limitRows(inputs));
    }

    private List<JavaFxPulsePhase> readPulsePhases(IItemCollection events) {
        List<JavaFxPulsePhase> phases = new ArrayList<>();
        readPulsePhases(events.apply(ItemFilters.type(TYPE_ID_PULSE_PHASE_8)), PULSE_ID_8, PHASE_NAME_8, phases);
        readPulsePhases(events.apply(ItemFilters.type(TYPE_ID_PULSE_PHASE_12)), PULSE_ID_12, PHASE_NAME_12, phases);
        phases.sort(Comparator.comparing(JavaFxPulsePhase::startTime)
                .thenComparingLong(JavaFxPulsePhase::pulseId)
                .thenComparing(JavaFxPulsePhase::phaseName));
        return phases;
    }

    private void readPulsePhases(IItemCollection events, IAttribute<IQuantity> pulseIdAttribute,
            IAttribute<String> phaseNameAttribute, List<JavaFxPulsePhase> phases) {
        events.stream().flatMap(IItemIterable::stream).forEach(item -> phases.add(new JavaFxPulsePhase(
                readLong(pulseIdAttribute, item),
                readString(phaseNameAttribute, item),
                readDurationMicros(item),
                readInstant(JfrAttributes.START_TIME, item),
                readEventThreadName(item))));
    }

    private List<JavaFxInputEvent> readInputEvents(IItemCollection events) {
        List<JavaFxInputEvent> inputs = new ArrayList<>();
        readInputEvents(events.apply(ItemFilters.type(TYPE_ID_INPUT_8)), inputs);
        readInputEvents(events.apply(ItemFilters.type(TYPE_ID_INPUT_12)), inputs);
        inputs.sort(Comparator.comparing(JavaFxInputEvent::startTime)
                .thenComparing(JavaFxInputEvent::inputType));
        return inputs;
    }

    private void readInputEvents(IItemCollection events, List<JavaFxInputEvent> inputs) {
        events.stream().flatMap(IItemIterable::stream).forEach(item -> inputs.add(new JavaFxInputEvent(
                readString(INPUT_TYPE, item),
                readDurationMicros(item),
                readInstant(JfrAttributes.START_TIME, item),
                readEventThreadName(item))));
    }

    private List<JavaFxPulseSummary> summarizePulses(List<JavaFxPulsePhase> phases) {
        Map<Long, PulseAccumulator> byPulse = new LinkedHashMap<>();
        for (JavaFxPulsePhase phase : phases) {
            byPulse.computeIfAbsent(phase.pulseId(), PulseAccumulator::new).add(phase);
        }
        return List.copyOf(byPulse.values().stream()
                .map(PulseAccumulator::toSummary)
                .toList());
    }

    @SuppressWarnings("unchecked")
    private <T> T read(IAttribute<T> attribute, IItem item) {
        IMemberAccessor<T, IItem> accessor =
                (IMemberAccessor<T, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private String readString(IAttribute<?> attribute, IItem item) {
        Object value = read(attribute, item);
        if (value != null) {
            return value.toString();
        }
        Object raw = readRaw(attribute, item);
        return raw == null ? "" : raw.toString();
    }

    private long readLong(IAttribute<?> attribute, IItem item) {
        Object value = readRaw(attribute, item);
        if (value instanceof IQuantity quantity) {
            return quantity.clampedLongValueIn(UnitLookup.NUMBER_UNITY);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }

    private Instant readInstant(IAttribute<IQuantity> attribute, IItem item) {
        IQuantity quantity = read(attribute, item);
        if (quantity == null) {
            return Instant.EPOCH;
        }
        return UnitLookup.toDate(quantity).toInstant();
    }

    private long readDurationMicros(IItem item) {
        IQuantity duration = read(JfrAttributes.DURATION, item);
        return duration == null ? 0 : duration.clampedLongValueIn(UnitLookup.MICROSECOND);
    }

    private String readEventThreadName(IItem item) {
        String name = readString(JdkAttributes.EVENT_THREAD_NAME, item);
        if (!name.isEmpty()) {
            return name;
        }
        Object thread = read(JfrAttributes.EVENT_THREAD, item);
        if (thread instanceof IMCThread mcThread) {
            String threadName = mcThread.getThreadName();
            return threadName == null ? "" : threadName;
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private Object readRaw(IAttribute<?> attribute, IItem item) {
        IMemberAccessor<Object, IItem> accessor =
                (IMemberAccessor<Object, IItem>) item.getType().getAccessor(attribute.getKey());
        return accessor == null ? null : accessor.getMember(item);
    }

    private static final class PulseAccumulator {
        private final long pulseId;
        private long phaseCount;
        private long totalDurationMicros;
        private long maxPhaseDurationMicros;
        private Instant startTime = Instant.MAX;

        private PulseAccumulator(long pulseId) {
            this.pulseId = pulseId;
        }

        private void add(JavaFxPulsePhase phase) {
            phaseCount++;
            totalDurationMicros += phase.durationMicros();
            maxPhaseDurationMicros = Math.max(maxPhaseDurationMicros, phase.durationMicros());
            if (phase.startTime().isBefore(startTime)) {
                startTime = phase.startTime();
            }
        }

        private JavaFxPulseSummary toSummary() {
            return new JavaFxPulseSummary(pulseId, phaseCount, totalDurationMicros,
                    maxPhaseDurationMicros, startTime == Instant.MAX ? Instant.EPOCH : startTime);
        }
    }
}
