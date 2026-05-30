package com.youngledo.jmcfx.ui.shell;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.youngledo.jmcfx.ui.i18n.I18n;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

final class AppNavTree extends TreeView<AppNavItem> {

    private final BooleanProperty recordingOpen = new SimpleBooleanProperty(false);
    private final ObjectProperty<AppWorkspaceKind> activeWorkspaceKind =
            new SimpleObjectProperty<>(AppWorkspaceKind.GLOBAL);
    private final TreeItem<AppNavItem> rootItem;
    private final List<TreeItem<AppNavItem>> globalGroups;
    private final List<TreeItem<AppNavItem>> recordingGroups;
    private final List<TreeItem<AppNavItem>> heapDumpGroups;
    private final List<TreeItem<AppNavItem>> liveJvmGroups;
    private final Map<String, TreeItem<AppNavItem>> pageItems;
    private Consumer<String> navigationHandler = section -> { };
    private I18n i18n;

    AppNavTree(I18n i18n) {
        this.i18n = i18n;
        rootItem = new TreeItem<>(AppNavItem.group("nav.root", Material2AL.ACCOUNT_TREE));
        TreeItem<AppNavItem> global = group("nav.group.global", Material2AL.HOME, NavIconTone.WORKSPACE);
        TreeItem<AppNavItem> recording = group("nav.group.recording", Material2AL.ANALYTICS, NavIconTone.RECORDING);
        TreeItem<AppNavItem> javaApp = group("nav.group.javaApplication", Material2AL.INSIGHTS, NavIconTone.JAVA);
        TreeItem<AppNavItem> memoryAnalysis = group("nav.group.memoryAnalysis", Material2MZ.MEMORY, NavIconTone.MEMORY);
        TreeItem<AppNavItem> jvmInternals = group("nav.group.jvmInternals", Material2MZ.MEMORY, NavIconTone.JVM);
        TreeItem<AppNavItem> environment = group("nav.group.environment", Material2MZ.PUBLIC, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> home = page("home", "nav.home", Material2AL.HOME, false, NavIconTone.WORKSPACE);
        TreeItem<AppNavItem> jvms = page("jvms", "jvms.title", Material2MZ.MEMORY, AppWorkspaceKind.LIVE_JVM,
                NavIconTone.WORKSPACE);
        TreeItem<AppNavItem> heapDumpAnalysis = page("heapDumpAnalysis", "nav.heapDumpAnalysis",
                Material2MZ.STORAGE, AppWorkspaceKind.HEAP_DUMP, NavIconTone.MEMORY);
        TreeItem<AppNavItem> analysis = page("analysis", "analysis.title", Material2AL.INSIGHTS, true, NavIconTone.RECORDING);
        TreeItem<AppNavItem> overview = page("overview", "overview.title", Material2MZ.PAGEVIEW, true, NavIconTone.RECORDING);
        TreeItem<AppNavItem> events = page("events", "events.title", Material2AL.EVENT, true, NavIconTone.RECORDING);
        TreeItem<AppNavItem> metadata = page("metadata", "nav.metadata", Material2MZ.VIEW_LIST, true, NavIconTone.RECORDING);
        TreeItem<AppNavItem> advancedJfr = page("advancedJfr", "nav.advancedJfr", Material2AL.GRID_ON, true, NavIconTone.RECORDING);
        TreeItem<AppNavItem> profiling = page("profiling", "nav.profiling", Material2AL.ASSIGNMENT, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> exceptions = page("exceptions", "nav.exceptions", Material2MZ.REPORT, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> threads = page("threads", "nav.threads", Material2AL.LIST, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> fileio = page("fileio", "nav.fileio", Material2AL.FOLDER, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> socketio = page("socketio", "nav.socketio", Material2MZ.NETWORK_CHECK, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> locks = page("locks", "nav.locks", Material2AL.LOCK, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> threadHistogram = page("threadHistogram", "nav.threadHistogram", Material2AL.LIST, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> security = page("security", "nav.security", Material2MZ.SECURITY, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> nativeLibraries = page("nativeLibraries", "nav.nativeLibraries", Material2MZ.STORAGE, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> threadDumps = page("threadDumps", "nav.threadDumps", Material2MZ.VIEW_LIST, true, NavIconTone.JAVA);
        TreeItem<AppNavItem> heap = page("heap", "nav.heap", Material2MZ.MEMORY, true, NavIconTone.MEMORY);
        TreeItem<AppNavItem> leaks = page("leaks", "nav.leaks", Material2AL.BUG_REPORT, true, NavIconTone.MEMORY);
        TreeItem<AppNavItem> tlab = page("tlab", "nav.tlab", Material2MZ.STORAGE, true, NavIconTone.MEMORY);
        TreeItem<AppNavItem> processPage = page("processes", "nav.processes", Material2MZ.MEMORY, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> envVarsPage = page("envVars", "nav.envVars", Material2MZ.PUBLIC, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> sysPropsPage = page("sysProps", "nav.sysProps", Material2MZ.SETTINGS, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> recordingInfoPage = page("recordingInfo", "nav.recordingInfo", Material2MZ.SAVE, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> agentsPage = page("agents", "nav.agents", Material2MZ.SECURITY, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> constantPoolsPage = page("constantPools", "nav.constantPools", Material2MZ.VIEW_LIST, true, NavIconTone.ENVIRONMENT);
        TreeItem<AppNavItem> jvmInfoPage = page("jvmInfo", "nav.jvmInfo", Material2MZ.VIEW_LIST, true, NavIconTone.JVM);
        TreeItem<AppNavItem> gcConfigPage = page("gcConfig", "nav.gcConfig", Material2MZ.SETTINGS, true, NavIconTone.JVM);
        TreeItem<AppNavItem> gcSummaryPage = page("gcSummary", "nav.gcSummary", Material2AL.LIST, true, NavIconTone.JVM);
        TreeItem<AppNavItem> gcDetailsPage = page("gcDetails", "nav.gcDetails", Material2AL.BUG_REPORT, true, NavIconTone.JVM);
        TreeItem<AppNavItem> compilationsPage = page("compilations", "nav.compilations", Material2MZ.PUBLIC, true, NavIconTone.JVM);
        TreeItem<AppNavItem> codeCachePage = page("codeCache", "nav.codeCache", Material2MZ.STORAGE, true, NavIconTone.JVM);
        TreeItem<AppNavItem> classLoadingPage = page("classLoading", "nav.classLoading", Material2MZ.SAVE, true, NavIconTone.JVM);
        TreeItem<AppNavItem> vmOperationsPage = page("vmOperations", "nav.vmOperations", Material2MZ.SECURITY, true, NavIconTone.JVM);
        TreeItem<AppNavItem> settings = page("settings", "settings.title", Material2MZ.SETTINGS, false, NavIconTone.APPLICATION);

        global.getChildren().setAll(List.of(home, settings));
        recording.getChildren().setAll(List.of(analysis, overview, events, metadata, advancedJfr));
        javaApp.getChildren().setAll(List.of(profiling, exceptions, threads, fileio, socketio, locks,
                threadHistogram, security, nativeLibraries, threadDumps));
        memoryAnalysis.getChildren().setAll(List.of(heap, leaks, tlab));
        environment.getChildren().setAll(List.of(processPage, envVarsPage, sysPropsPage,
                recordingInfoPage, agentsPage, constantPoolsPage));
        jvmInternals.getChildren().setAll(List.of(jvmInfoPage, gcConfigPage, gcSummaryPage, gcDetailsPage,
                compilationsPage, codeCachePage, classLoadingPage, vmOperationsPage));
        globalGroups = List.of(global);
        recordingGroups = List.of(global, recording, javaApp, jvmInternals, memoryAnalysis, environment);
        heapDumpGroups = List.of(global, groupWith(heapDumpAnalysis));
        liveJvmGroups = List.of(global);
        rootItem.getChildren().setAll(globalGroups);
        rootItem.setExpanded(true);
        global.setExpanded(true);
        recording.setExpanded(true);
        javaApp.setExpanded(true);
        jvmInternals.setExpanded(true);
        memoryAnalysis.setExpanded(true);
        environment.setExpanded(true);

        pageItems = new HashMap<>();
        pageItems.put("home", home);
        pageItems.put("heapDumpAnalysis", heapDumpAnalysis);
        pageItems.put("analysis", analysis);
        pageItems.put("overview", overview);
        pageItems.put("events", events);
        pageItems.put("metadata", metadata);
        pageItems.put("advancedJfr", advancedJfr);
        pageItems.put("profiling", profiling);
        pageItems.put("exceptions", exceptions);
        pageItems.put("threads", threads);
        pageItems.put("settings", settings);
        pageItems.put("fileio", fileio);
        pageItems.put("socketio", socketio);
        pageItems.put("locks", locks);
        pageItems.put("threadHistogram", threadHistogram);
        pageItems.put("security", security);
        pageItems.put("nativeLibraries", nativeLibraries);
        pageItems.put("threadDumps", threadDumps);
        pageItems.put("heap", heap);
        pageItems.put("leaks", leaks);
        pageItems.put("tlab", tlab);
        pageItems.put("processes", processPage);
        pageItems.put("envVars", envVarsPage);
        pageItems.put("sysProps", sysPropsPage);
        pageItems.put("recordingInfo", recordingInfoPage);
        pageItems.put("agents", agentsPage);
        pageItems.put("constantPools", constantPoolsPage);
        pageItems.put("jvmInfo", jvmInfoPage);
        pageItems.put("gcConfig", gcConfigPage);
        pageItems.put("gcSummary", gcSummaryPage);
        pageItems.put("gcDetails", gcDetailsPage);
        pageItems.put("compilations", compilationsPage);
        pageItems.put("codeCache", codeCachePage);
        pageItems.put("classLoading", classLoadingPage);
        pageItems.put("vmOperations", vmOperationsPage);

        getStyleClass().add("app-nav-tree");
        setShowRoot(false);
        setRoot(rootItem);
        setCellFactory(tree -> new AppNavTreeCell(recordingOpen, i18n));
        ChangeListener<TreeItem<AppNavItem>> selectionListener =
                (observable, oldValue, newValue) -> navigate(newValue);
        getSelectionModel().selectedItemProperty().addListener(selectionListener);
    }

    void setI18n(I18n i18n) {
        this.i18n = i18n;
        setCellFactory(tree -> new AppNavTreeCell(recordingOpen, i18n));
        refresh();
    }

    void bind(AppShellViewModel viewModel) {
        recordingOpen.bind(viewModel.recordingOpenProperty());
        activeWorkspaceKind.bind(viewModel.activeWorkspaceKindProperty());
        viewModel.activeWorkspaceKindProperty()
                .addListener((observable, oldValue, newValue) -> updateNavigationContext(newValue));
        viewModel.recordingOpenProperty()
                .addListener((observable, oldValue, newValue) -> refresh());
        viewModel.selectedSectionProperty()
                .addListener((observable, oldValue, newValue) -> selectSection(newValue));
        selectSection(viewModel.selectedSectionProperty().get());
        updateNavigationContext(viewModel.activeWorkspaceKindProperty().get());
    }

    void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler == null ? section -> { } : navigationHandler;
    }

    List<AppNavSearchResult> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        return pageItems.values().stream()
                .map(TreeItem::getValue)
                .filter(item -> item.visibleIn(activeWorkspaceKind.get()))
                .filter(item -> !item.unavailable(recordingOpen.get()))
                .map(item -> new AppNavSearchResult(item.sectionId(), i18n.get(item.titleKey())))
                .filter(result -> matches(result, normalizedQuery))
                .sorted((left, right) -> left.title().compareToIgnoreCase(right.title()))
                .toList();
    }

    void navigateToSection(String sectionId) {
        TreeItem<AppNavItem> item = pageItems.get(sectionId);
        if (item == null || !item.getValue().visibleIn(activeWorkspaceKind.get())
                || item.getValue().unavailable(recordingOpen.get())) {
            return;
        }
        if (getSelectionModel().getSelectedItem() == item) {
            navigationHandler.accept(sectionId);
            return;
        }
        getSelectionModel().select(item);
    }

    void setRecordingOpenForTesting(boolean open) {
        recordingOpen.set(open);
    }

    void setActiveWorkspaceKindForTesting(AppWorkspaceKind workspaceKind) {
        activeWorkspaceKind.set(workspaceKind == null ? AppWorkspaceKind.GLOBAL : workspaceKind);
        updateNavigationContext(activeWorkspaceKind.get());
    }

    private void navigate(TreeItem<AppNavItem> item) {
        if (item == null || item.getValue() == null || !item.getValue().page()) {
            return;
        }
        AppNavItem navItem = item.getValue();
        if (!navItem.visibleIn(activeWorkspaceKind.get()) || navItem.unavailable(recordingOpen.get())) {
            selectSection(null);
            return;
        }
        navigationHandler.accept(navItem.sectionId());
    }

    private void selectSection(String sectionId) {
        TreeItem<AppNavItem> item = pageItems.get(sectionId);
        if (item == null) {
            getSelectionModel().clearSelection();
            return;
        }
        if (!item.getValue().visibleIn(activeWorkspaceKind.get())) {
            getSelectionModel().clearSelection();
            return;
        }
        getSelectionModel().select(item);
    }

    private void updateNavigationContext(AppWorkspaceKind workspaceKind) {
        AppWorkspaceKind kind = workspaceKind == null ? AppWorkspaceKind.GLOBAL : workspaceKind;
        String selectedSectionId = getSelectionModel().getSelectedItem() == null
                ? null : getSelectionModel().getSelectedItem().getValue().sectionId();
        List<TreeItem<AppNavItem>> groups = switch (kind) {
            case GLOBAL -> globalGroups;
            case RECORDING -> recordingGroups;
            case HEAP_DUMP -> heapDumpGroups;
            case LIVE_JVM -> liveJvmGroups;
        };
        getSelectionModel().clearSelection();
        setRoot(null);
        rootItem.getChildren().setAll(groups);
        rootItem.setExpanded(true);
        groups.forEach(group -> group.setExpanded(true));
        setRoot(rootItem);
        selectSection(selectedSectionId);
        refresh();
    }

    private static boolean matches(AppNavSearchResult result, String normalizedQuery) {
        return normalize(result.sectionId()).contains(normalizedQuery)
                || normalize(result.title()).contains(normalizedQuery);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static TreeItem<AppNavItem> group(String titleKey, org.kordamp.ikonli.Ikon icon) {
        return new TreeItem<>(AppNavItem.group(titleKey, icon));
    }

    private static TreeItem<AppNavItem> group(String titleKey, org.kordamp.ikonli.Ikon icon, NavIconTone iconTone) {
        return new TreeItem<>(AppNavItem.group(titleKey, icon, iconTone));
    }

    private static TreeItem<AppNavItem> page(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon,
            boolean recordingScoped) {
        return new TreeItem<>(AppNavItem.page(sectionId, titleKey, icon, recordingScoped));
    }

    private static TreeItem<AppNavItem> page(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon,
            boolean recordingScoped, NavIconTone iconTone) {
        return new TreeItem<>(AppNavItem.page(sectionId, titleKey, icon, recordingScoped, iconTone));
    }

    private static TreeItem<AppNavItem> page(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon,
            AppWorkspaceKind workspaceKind, NavIconTone iconTone) {
        return new TreeItem<>(AppNavItem.page(sectionId, titleKey, icon, workspaceKind, iconTone));
    }

    private static TreeItem<AppNavItem> groupWith(TreeItem<AppNavItem> page) {
        TreeItem<AppNavItem> group = group(page.getValue().titleKey(), page.getValue().icon(), page.getValue().iconTone());
        group.getChildren().setAll(List.of(page));
        group.setExpanded(true);
        return group;
    }

    private static TreeItem<AppNavItem> unavailablePage(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon) {
        return new TreeItem<>(AppNavItem.unavailablePage(sectionId, titleKey, icon));
    }
}
