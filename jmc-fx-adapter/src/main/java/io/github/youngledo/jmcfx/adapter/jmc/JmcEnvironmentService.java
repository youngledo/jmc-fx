package io.github.youngledo.jmcfx.adapter.jmc;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openjdk.jmc.common.IDisplayable;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;

import io.github.youngledo.jmcfx.domain.model.ActiveRecordingInfo;
import io.github.youngledo.jmcfx.domain.model.ActiveSetting;
import io.github.youngledo.jmcfx.domain.model.AgentInfo;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolEntry;
import io.github.youngledo.jmcfx.domain.model.ConstantPoolType;
import io.github.youngledo.jmcfx.domain.model.EnvironmentVariable;
import io.github.youngledo.jmcfx.domain.model.ProcessInfo;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.SystemProperty;
import io.github.youngledo.jmcfx.domain.service.EnvironmentService;
import io.github.youngledo.jmcfx.domain.service.JmcFxException;

/// JMC-backed environment adapter.
///
/// Queries JFR metadata events (processes, env vars, system properties,
/// active recordings/settings, agents) and recording constant pools.
public class JmcEnvironmentService implements EnvironmentService {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    @Override
    public List<ProcessInfo> loadProcesses(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection processEvents = events.apply(ItemFilters.type(JdkTypeIDs.PROCESSES));
        if (!processEvents.hasItems()) {
            return List.of();
        }
        List<ProcessInfo> result = new ArrayList<>();
        for (IItemIterable itemIter : processEvents) {
            IMemberAccessor<String, IItem> pidAccessor =
                    JdkAttributes.PID.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> cmdAccessor =
                    JdkAttributes.COMMAND_LINE.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JfrAttributes.START_TIME.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String pid = pidAccessor != null ? pidAccessor.getMember(item) : "";
                String cmdLine = cmdAccessor != null ? cmdAccessor.getMember(item) : "";
                String startStr = "";
                if (startAccessor != null) {
                    IQuantity startQty = startAccessor.getMember(item);
                    if (startQty != null) {
                        startStr = formatTimestamp(startQty);
                    }
                }
                result.add(new ProcessInfo(
                        pid != null ? pid : "",
                        cmdLine != null ? cmdLine : "",
                        startStr, startStr));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<EnvironmentVariable> loadEnvironmentVariables(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection envEvents = events.apply(ItemFilters.type(JdkTypeIDs.ENVIRONMENT_VARIABLE));
        if (!envEvents.hasItems()) {
            return List.of();
        }
        List<EnvironmentVariable> result = new ArrayList<>();
        for (IItemIterable itemIter : envEvents) {
            IMemberAccessor<String, IItem> keyAccessor =
                    JdkAttributes.ENVIRONMENT_KEY.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> valueAccessor =
                    JdkAttributes.ENVIRONMENT_VALUE.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String key = keyAccessor != null ? keyAccessor.getMember(item) : "";
                String value = valueAccessor != null ? valueAccessor.getMember(item) : "";
                result.add(new EnvironmentVariable(
                        key != null ? key : "",
                        value != null ? value : ""));
            }
        }
        result.sort(Comparator.comparing(EnvironmentVariable::key));
        return List.copyOf(result);
    }

    @Override
    public List<SystemProperty> loadSystemProperties(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection propEvents = events.apply(ItemFilters.type(JdkTypeIDs.SYSTEM_PROPERTIES));
        if (!propEvents.hasItems()) {
            return List.of();
        }
        List<SystemProperty> result = new ArrayList<>();
        for (IItemIterable itemIter : propEvents) {
            // System properties use the same attributes as environment variables
            IMemberAccessor<String, IItem> keyAccessor =
                    JdkAttributes.ENVIRONMENT_KEY.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> valueAccessor =
                    JdkAttributes.ENVIRONMENT_VALUE.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String key = keyAccessor != null ? keyAccessor.getMember(item) : "";
                String value = valueAccessor != null ? valueAccessor.getMember(item) : "";
                result.add(new SystemProperty(
                        key != null ? key : "",
                        value != null ? value : ""));
            }
        }
        result.sort(Comparator.comparing(SystemProperty::key));
        return List.copyOf(result);
    }

    @Override
    public List<ActiveRecordingInfo> loadActiveRecordings(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection recEvents = events.apply(ItemFilters.type(JdkTypeIDs.RECORDINGS));
        if (!recEvents.hasItems()) {
            return List.of();
        }
        List<ActiveRecordingInfo> result = new ArrayList<>();
        for (IItemIterable itemIter : recEvents) {
            IMemberAccessor<IQuantity, IItem> idAccessor =
                    JdkAttributes.RECORDING_ID.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> nameAccessor =
                    JdkAttributes.RECORDING_NAME.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> destAccessor =
                    JdkAttributes.RECORDING_DESTINATION.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> maxAgeAccessor =
                    JdkAttributes.RECORDING_MAX_AGE.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> maxSizeAccessor =
                    JdkAttributes.RECORDING_MAX_SIZE.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> startAccessor =
                    JdkAttributes.RECORDING_START.getAccessor(itemIter.getType());
            IMemberAccessor<IQuantity, IItem> durationAccessor =
                    JdkAttributes.RECORDING_DURATION.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String id = formatQuantity(idAccessor != null ? idAccessor.getMember(item) : null);
                String name = getMemberStr(nameAccessor, item);
                String dest = getMemberStr(destAccessor, item);
                IQuantity maxAge = maxAgeAccessor != null ? maxAgeAccessor.getMember(item) : null;
                IQuantity maxSize = maxSizeAccessor != null ? maxSizeAccessor.getMember(item) : null;
                IQuantity startTime = startAccessor != null ? startAccessor.getMember(item) : null;
                IQuantity duration = durationAccessor != null ? durationAccessor.getMember(item) : null;
                result.add(new ActiveRecordingInfo(
                        id,
                        name,
                        dest,
                        maxAge != null ? toLongSafe(maxAge, UnitLookup.MILLISECOND) : 0,
                        maxSize != null ? toLongSafe(maxSize, UnitLookup.BYTE) : 0,
                        startTime != null ? formatTimestamp(startTime) : "",
                        duration != null ? duration.displayUsing(IDisplayable.AUTO) : "",
                        0));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<ActiveSetting> loadActiveSettings(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection settingEvents = events.apply(ItemFilters.type(JdkTypeIDs.RECORDING_SETTING));
        if (!settingEvents.hasItems()) {
            return List.of();
        }
        List<ActiveSetting> result = new ArrayList<>();
        for (IItemIterable itemIter : settingEvents) {
            IMemberAccessor<String, IItem> idAccessor =
                    JdkAttributes.REC_SETTING_FOR_ID.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> nameAccessor =
                    JdkAttributes.REC_SETTING_NAME.getAccessor(itemIter.getType());
            IMemberAccessor<String, IItem> valueAccessor =
                    JdkAttributes.REC_SETTING_VALUE.getAccessor(itemIter.getType());
            for (IItem item : itemIter) {
                String eventId = getMemberStr(idAccessor, item);
                String settingName = getMemberStr(nameAccessor, item);
                String settingValue = getMemberStr(valueAccessor, item);
                result.add(new ActiveSetting(eventId, settingName, settingValue));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public List<AgentInfo> loadAgents(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        IItemCollection javaAgents = events.apply(ItemFilters.type(JdkTypeIDs.JAVA_AGENT));
        IItemCollection nativeAgents = events.apply(ItemFilters.type(JdkTypeIDs.NATIVE_AGENT));

        List<AgentInfo> result = new ArrayList<>();
        for (IItemIterable itemIter : javaAgents) {
            for (IItem item : itemIter) {
                result.add(toAgentInfo(item, itemIter, "Java"));
            }
        }
        for (IItemIterable itemIter : nativeAgents) {
            for (IItem item : itemIter) {
                result.add(toAgentInfo(item, itemIter, "Native"));
            }
        }
        return List.copyOf(result);
    }

    private AgentInfo toAgentInfo(IItem item, IItemIterable itemIter, String kind) {
        IMemberAccessor<String, IItem> nameAccessor =
                JdkAttributes.AGENT_NAME.getAccessor(itemIter.getType());
        IMemberAccessor<String, IItem> optionsAccessor =
                JdkAttributes.AGENT_OPTIONS.getAccessor(itemIter.getType());
        @SuppressWarnings("unchecked")
        IMemberAccessor<Boolean, IItem> dynamicAccessor =
                (IMemberAccessor<Boolean, IItem>) JdkAttributes.AGENT_DYNAMIC.getAccessor(itemIter.getType());
        IMemberAccessor<IQuantity, IItem> initTimeAccessor =
                JdkAttributes.AGENT_INITIALIZATION_TIME.getAccessor(itemIter.getType());

        String name = getMemberStr(nameAccessor, item);
        String options = getMemberStr(optionsAccessor, item);
        boolean dynamic = dynamicAccessor != null && Boolean.TRUE.equals(dynamicAccessor.getMember(item));
        String initTimeStr = "";
        if (initTimeAccessor != null) {
            IQuantity initTime = initTimeAccessor.getMember(item);
            if (initTime != null) {
                initTimeStr = formatTimestamp(initTime);
            }
        }
        return new AgentInfo(name, options, initTimeStr, dynamic, kind);
    }

    @Override
    public List<ConstantPoolType> loadConstantPools(RecordingSummary recording) {
        IItemCollection events = loadEvents(recording);
        Map<String, ConstantPoolBuilder> builders = new LinkedHashMap<>();
        for (IItemIterable itemIter : events) {
            String typeId = itemIter.getType().getIdentifier();
            String typeName = itemIter.getType().getName();
            builders.computeIfAbsent(typeId,
                    id -> new ConstantPoolBuilder(typeName, id));
            for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
                if (JfrAttributes.START_TIME.equals(attr)
                        || JfrAttributes.END_TIME.equals(attr)
                        || JfrAttributes.DURATION.equals(attr)
                        || JfrAttributes.EVENT_THREAD.equals(attr)
                        || JfrAttributes.EVENT_STACKTRACE.equals(attr)) {
                    continue;
                }
                ConstantPoolBuilder builder = builders.get(typeId);
                if (!builder.hasAttribute(attr.getIdentifier())) {
                    builder.addAttribute(attr.getIdentifier(), attr.getName());
                }
            }
        }
        List<ConstantPoolType> result = new ArrayList<>();
        for (ConstantPoolBuilder builder : builders.values()) {
            List<ConstantPoolEntry> entries = builder.buildEntries();
            result.add(new ConstantPoolType(
                    builder.typeName, builder.typeId, entries.size(), entries));
        }
        result.sort(Comparator.comparing(ConstantPoolType::typeName));
        return List.copyOf(result);
    }

    // --- Private helpers ---

    private static String formatTimestamp(IQuantity timeQuantity) {
        long epochMs = toLongSafe(timeQuantity, UnitLookup.EPOCH_MS);
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMs));
    }

    private static long toLongSafe(IQuantity qty, IUnit unit) {
        try {
            return qty.longValueIn(unit);
        } catch (QuantityConversionException e) {
            return 0;
        }
    }

    private static String formatQuantity(IQuantity qty) {
        return qty != null ? qty.displayUsing(IDisplayable.AUTO) : "";
    }

    private static String getMemberStr(IMemberAccessor<String, IItem> accessor, IItem item) {
        if (accessor == null) {
            return "";
        }
        String val = accessor.getMember(item);
        return val != null ? val : "";
    }

    @SuppressWarnings("unchecked")
    private static IMemberAccessor<String, IItem> findStringAccessor(IItemIterable itemIter, String attrId) {
        for (IAttribute<?> attr : itemIter.getType().getAttributes()) {
            if (attrId.equals(attr.getIdentifier())) {
                return (IMemberAccessor<String, IItem>) attr.getAccessor(itemIter.getType());
            }
        }
        return null;
    }

    private IItemCollection loadEvents(RecordingSummary recording) {
        return JmcRecordingDataCache.SHARED.events(recording);
    }

    private static final class ConstantPoolBuilder {
        final String typeName;
        final String typeId;
        final Map<String, String> attributes = new LinkedHashMap<>();

        ConstantPoolBuilder(String typeName, String typeId) {
            this.typeName = typeName;
            this.typeId = typeId;
        }

        boolean hasAttribute(String id) {
            return attributes.containsKey(id);
        }

        void addAttribute(String id, String displayName) {
            attributes.put(id, displayName);
        }

        List<ConstantPoolEntry> buildEntries() {
            List<ConstantPoolEntry> entries = new ArrayList<>();
            for (var entry : attributes.entrySet()) {
                entries.add(new ConstantPoolEntry(entry.getKey(), entry.getValue()));
            }
            return entries;
        }
    }
}
