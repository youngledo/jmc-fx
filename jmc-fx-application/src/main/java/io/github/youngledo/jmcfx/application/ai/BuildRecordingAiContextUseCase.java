package io.github.youngledo.jmcfx.application.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.github.youngledo.jmcfx.application.AnalyzeRulesUseCase;
import io.github.youngledo.jmcfx.domain.model.ExceptionGrouping;
import io.github.youngledo.jmcfx.domain.model.ExceptionSummary;
import io.github.youngledo.jmcfx.domain.model.FileIOHistogram;
import io.github.youngledo.jmcfx.domain.model.G1GcReport;
import io.github.youngledo.jmcfx.domain.model.GcEvent;
import io.github.youngledo.jmcfx.domain.model.HeapClassHistogram;
import io.github.youngledo.jmcfx.domain.model.HotMethod;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataEventType;
import io.github.youngledo.jmcfx.domain.model.JfrMetadataReport;
import io.github.youngledo.jmcfx.domain.model.LockGrouping;
import io.github.youngledo.jmcfx.domain.model.LockHistogram;
import io.github.youngledo.jmcfx.domain.model.RecordingSummary;
import io.github.youngledo.jmcfx.domain.model.RuleResult;
import io.github.youngledo.jmcfx.domain.model.SocketIOGrouping;
import io.github.youngledo.jmcfx.domain.model.SocketIOHistogram;
import io.github.youngledo.jmcfx.domain.model.ThreadSummary;
import io.github.youngledo.jmcfx.domain.model.TlabAllocation;
import io.github.youngledo.jmcfx.domain.service.ExceptionService;
import io.github.youngledo.jmcfx.domain.service.FileIOService;
import io.github.youngledo.jmcfx.domain.service.G1GcService;
import io.github.youngledo.jmcfx.domain.service.HeapService;
import io.github.youngledo.jmcfx.domain.service.JfrMetadataService;
import io.github.youngledo.jmcfx.domain.service.LockService;
import io.github.youngledo.jmcfx.domain.service.ProfilingService;
import io.github.youngledo.jmcfx.domain.service.SocketIOService;
import io.github.youngledo.jmcfx.domain.service.ThreadService;
import io.github.youngledo.jmcfx.domain.service.TlabService;

public final class BuildRecordingAiContextUseCase {

    private static final int MAX_RULE_RESULTS = 20;
    private static final int MAX_EVENT_TYPES = 30;
    private static final int MAX_GC_PAUSES = 10;
    private static final int MAX_EXCEPTION_GROUPS = 20;
    private static final int MAX_PROFILING_ENTRIES = 30;
    private static final int MAX_THREADS = 20;
    private static final int MAX_HEAP_ENTRIES = 20;
    private static final int MAX_TLAB_ENTRIES = 20;
    private static final int MAX_LOCK_ENTRIES = 20;
    private static final int MAX_FILE_IO_ENTRIES = 20;
    private static final int MAX_SOCKET_IO_ENTRIES = 20;

    private final AnalyzeRulesUseCase analyzeRules;
    private final JfrMetadataService jfrMetadataService;
    private final G1GcService g1GcService;
    private final ExceptionService exceptionService;
    private final ProfilingService profilingService;
    private final ThreadService threadService;
    private final HeapService heapService;
    private final TlabService tlabService;
    private final LockService lockService;
    private final FileIOService fileIOService;
    private final SocketIOService socketIOService;

    public BuildRecordingAiContextUseCase(AnalyzeRulesUseCase analyzeRules) {
        this(analyzeRules, null, null, null, null, null, null, null, null, null, null);
    }

    public BuildRecordingAiContextUseCase(AnalyzeRulesUseCase analyzeRules,
            JfrMetadataService jfrMetadataService,
            G1GcService g1GcService,
            ExceptionService exceptionService,
            ProfilingService profilingService,
            ThreadService threadService,
            HeapService heapService,
            TlabService tlabService,
            LockService lockService,
            FileIOService fileIOService,
            SocketIOService socketIOService) {
        this.analyzeRules = Objects.requireNonNull(analyzeRules, "analyzeRules");
        this.jfrMetadataService = jfrMetadataService;
        this.g1GcService = g1GcService;
        this.exceptionService = exceptionService;
        this.profilingService = profilingService;
        this.threadService = threadService;
        this.heapService = heapService;
        this.tlabService = tlabService;
        this.lockService = lockService;
        this.fileIOService = fileIOService;
        this.socketIOService = socketIOService;
    }

    public RecordingAiContext build(RecordingSummary recording) {
        Objects.requireNonNull(recording, "recording");
        List<RuleResult> allRules = analyzeRules.analyze(recording);
        List<RuleResult> rules = allRules.stream()
                .sorted(Comparator.comparingInt(RuleResult::score).reversed())
                .limit(MAX_RULE_RESULTS)
                .toList();
        List<String> limitations = new ArrayList<>();
        if (allRules.size() > MAX_RULE_RESULTS) {
            limitations.add("Rule results were capped to top 20 by score.");
        }
        List<RecordingAiContextSection> sections = new ArrayList<>();
        addSection(sections, limitations, "JFR metadata", () -> metadataSection(recording));
        addSection(sections, limitations, "GC pauses", () -> gcSection(recording));
        addSection(sections, limitations, "Exceptions", () -> exceptionsSection(recording));
        addSection(sections, limitations, "Profiling", () -> profilingSection(recording));
        addSection(sections, limitations, "Threads", () -> threadsSection(recording));
        addSection(sections, limitations, "Heap", () -> heapSection(recording));
        addSection(sections, limitations, "TLAB allocations", () -> tlabSection(recording));
        addSection(sections, limitations, "Locks", () -> lockSection(recording));
        addSection(sections, limitations, "File I/O", () -> fileIOSection(recording));
        addSection(sections, limitations, "Socket I/O", () -> socketIOSection(recording));
        sections.stream()
                .filter(RecordingAiContextSection::capped)
                .map(section -> "%s were capped to top %d of %d."
                        .formatted(section.title(), section.limit(), section.totalCount()))
                .forEach(limitations::add);
        return new RecordingAiContext(recording, rules, sections, limitations);
    }

    private RecordingAiContextSection metadataSection(RecordingSummary recording) {
        if (jfrMetadataService == null) {
            return null;
        }
        JfrMetadataReport report = jfrMetadataService.loadMetadata(recording);
        List<JfrMetadataEventType> eventTypes = report.eventTypes().stream()
                .sorted(Comparator.comparingLong(JfrMetadataEventType::eventCount).reversed())
                .toList();
        List<String> rows = eventTypes.stream()
                .limit(MAX_EVENT_TYPES)
                .map(type -> "eventType id=%s, name=%s, category=%s, eventCount=%d, fieldCount=%d"
                        .formatted(type.id(), type.name(), type.category(), type.eventCount(), type.fieldCount()))
                .toList();
        return section("jfrMetadata", "JFR metadata", rows, eventTypes.size(), MAX_EVENT_TYPES);
    }

    private RecordingAiContextSection gcSection(RecordingSummary recording) {
        if (g1GcService == null) {
            return null;
        }
        G1GcReport report = g1GcService.loadG1GcReport(recording);
        List<GcEvent> pauses = report.gcPauses().stream()
                .sorted(Comparator.comparingLong(GcEvent::longestPauseMicros).reversed())
                .toList();
        List<String> rows = pauses.stream()
                .limit(MAX_GC_PAUSES)
                .map(pause -> "gc id=%d, name=%s, cause=%s, longestPauseMicros=%d, totalPauseMicros=%d, startTime=%s"
                        .formatted(pause.gcId(), pause.name(), pause.cause(), pause.longestPauseMicros(),
                                pause.totalPauseMicros(), pause.startTime()))
                .toList();
        return section("gcPauses", "GC pauses", rows, pauses.size(), MAX_GC_PAUSES);
    }

    private RecordingAiContextSection exceptionsSection(RecordingSummary recording) {
        if (exceptionService == null) {
            return null;
        }
        List<ExceptionSummary> summaries = exceptionService.loadHistogram(recording, ExceptionGrouping.BY_CLASS_AND_MESSAGE)
                .stream()
                .sorted(Comparator.comparingInt(ExceptionSummary::count).reversed())
                .toList();
        List<String> rows = summaries.stream()
                .limit(MAX_EXCEPTION_GROUPS)
                .map(summary -> "exception class=%s, message=%s, count=%d, percentage=%.2f"
                        .formatted(summary.className(), summary.message(), summary.count(), summary.percentage()))
                .toList();
        return section("exceptions", "Exceptions", rows, summaries.size(), MAX_EXCEPTION_GROUPS);
    }

    private RecordingAiContextSection profilingSection(RecordingSummary recording) {
        if (profilingService == null) {
            return null;
        }
        List<HotMethod> methods = profilingService.loadHotMethods(recording).stream()
                .sorted(Comparator.comparingInt(HotMethod::count).reversed())
                .toList();
        List<String> rows = methods.stream()
                .limit(MAX_PROFILING_ENTRIES)
                .map(method -> "method=%s, frameType=%s, samples=%d, percentage=%.2f"
                        .formatted(method.method(), method.frameType(), method.count(), method.percentage()))
                .toList();
        return section("profiling", "Profiling", rows, methods.size(), MAX_PROFILING_ENTRIES);
    }

    private RecordingAiContextSection threadsSection(RecordingSummary recording) {
        if (threadService == null) {
            return null;
        }
        List<ThreadSummary> threads = threadService.loadThreadSummaries(recording).stream()
                .sorted(Comparator.comparingInt(ThreadSummary::sampleCount).reversed()
                        .thenComparing(Comparator.comparingLong(ThreadSummary::blockedDurationMillis).reversed()))
                .toList();
        List<String> rows = threads.stream()
                .limit(MAX_THREADS)
                .map(thread -> "thread name=%s, id=%d, group=%s, virtual=%s, samples=%d, blockedMillis=%d"
                        .formatted(thread.threadName(), thread.threadId(), thread.threadGroup(), thread.virtual(),
                                thread.sampleCount(), thread.blockedDurationMillis()))
                .toList();
        return section("threads", "Threads", rows, threads.size(), MAX_THREADS);
    }

    private RecordingAiContextSection heapSection(RecordingSummary recording) {
        if (heapService == null) {
            return null;
        }
        List<HeapClassHistogram> classes = heapService.loadHeapClassHistogram(recording).stream()
                .sorted(Comparator.comparingLong(HeapClassHistogram::size).reversed())
                .toList();
        List<String> rows = classes.stream()
                .limit(MAX_HEAP_ENTRIES)
                .map(item -> "class=%s, instances=%d, sizeBytes=%d, deltaBytes=%d, allocationPct=%.2f"
                        .formatted(item.className(), item.instances(), item.size(), item.delta(), item.allocationPct()))
                .toList();
        return section("heap", "Heap", rows, classes.size(), MAX_HEAP_ENTRIES);
    }

    private RecordingAiContextSection tlabSection(RecordingSummary recording) {
        if (tlabService == null) {
            return null;
        }
        List<TlabAllocation> allocations = tlabService.loadTlabAllocations(recording).stream()
                .sorted(Comparator.comparingLong(this::tlabTotalSize).reversed())
                .toList();
        List<String> rows = allocations.stream()
                .limit(MAX_TLAB_ENTRIES)
                .map(item -> "thread=%s, insideCount=%d, outsideCount=%d, insideTotalBytes=%d, outsideTotalBytes=%d"
                        .formatted(item.thread(), item.insideCount(), item.outsideCount(), item.insideTotalSize(),
                                item.outsideTotalSize()))
                .toList();
        return section("tlab", "TLAB allocations", rows, allocations.size(), MAX_TLAB_ENTRIES);
    }

    private RecordingAiContextSection lockSection(RecordingSummary recording) {
        if (lockService == null) {
            return null;
        }
        List<LockHistogram> locks = lockService.loadLockHistogram(recording, LockGrouping.BY_CLASS).stream()
                .sorted(Comparator.comparingLong(LockHistogram::totalDuration).reversed())
                .toList();
        List<String> rows = locks.stream()
                .limit(MAX_LOCK_ENTRIES)
                .map(lock -> "key=%s, count=%d, totalDurationMillis=%d, maxDurationMillis=%d, avgDurationMillis=%.2f, threads=%d"
                        .formatted(lock.key(), lock.count(), lock.totalDuration(), lock.maxDuration(),
                                lock.avgDuration(), lock.distinctThreads()))
                .toList();
        return section("locks", "Locks", rows, locks.size(), MAX_LOCK_ENTRIES);
    }

    private RecordingAiContextSection fileIOSection(RecordingSummary recording) {
        if (fileIOService == null) {
            return null;
        }
        List<FileIOHistogram> files = fileIOService.loadFileIOHistogram(recording).stream()
                .sorted(Comparator.comparingLong(this::fileIOTotalBytes).reversed())
                .toList();
        List<String> rows = files.stream()
                .limit(MAX_FILE_IO_ENTRIES)
                .map(file -> "path=%s, readCount=%d, writeCount=%d, readBytes=%d, writeBytes=%d, totalDurationMillis=%d"
                        .formatted(file.path(), file.readCount(), file.writeCount(), file.readSize(),
                                file.writeSize(), file.totalDuration()))
                .toList();
        return section("fileIO", "File I/O", rows, files.size(), MAX_FILE_IO_ENTRIES);
    }

    private RecordingAiContextSection socketIOSection(RecordingSummary recording) {
        if (socketIOService == null) {
            return null;
        }
        List<SocketIOHistogram> sockets = socketIOService.loadSocketIOHistogram(recording, SocketIOGrouping.BY_HOST_AND_PORT)
                .stream()
                .sorted(Comparator.comparingLong(this::socketIOTotalBytes).reversed())
                .toList();
        List<String> rows = sockets.stream()
                .limit(MAX_SOCKET_IO_ENTRIES)
                .map(socket -> "remote=%s, host=%s, port=%d, readCount=%d, writeCount=%d, readBytes=%d, writeBytes=%d, totalDurationMillis=%d"
                        .formatted(socket.key(), socket.host(), socket.port(), socket.readCount(),
                                socket.writeCount(), socket.readSize(), socket.writeSize(), socket.totalDuration()))
                .toList();
        return section("socketIO", "Socket I/O", rows, sockets.size(), MAX_SOCKET_IO_ENTRIES);
    }

    private static void addSection(List<RecordingAiContextSection> sections, List<String> limitations,
            String title, Supplier<RecordingAiContextSection> supplier) {
        try {
            RecordingAiContextSection section = supplier.get();
            if (section != null && section.available()) {
                sections.add(section);
            }
        } catch (RuntimeException exception) {
            limitations.add(title + " context is unavailable: " + failureMessage(exception));
        }
    }

    private static RecordingAiContextSection section(String id, String title, List<String> rows,
            int totalCount, int limit) {
        return new RecordingAiContextSection(id, title, rows, totalCount > limit, totalCount, limit);
    }

    private long tlabTotalSize(TlabAllocation allocation) {
        return allocation.insideTotalSize() + allocation.outsideTotalSize();
    }

    private long fileIOTotalBytes(FileIOHistogram file) {
        return file.readSize() + file.writeSize();
    }

    private long socketIOTotalBytes(SocketIOHistogram socket) {
        return socket.readSize() + socket.writeSize();
    }

    private static String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
