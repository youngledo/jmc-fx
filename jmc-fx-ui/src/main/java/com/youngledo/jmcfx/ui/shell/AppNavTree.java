package com.youngledo.jmcfx.ui.shell;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

import com.youngledo.jmcfx.ui.i18n.I18n;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

final class AppNavTree extends TreeView<AppNavItem> {

    private final BooleanProperty recordingOpen = new SimpleBooleanProperty(false);
    private final Map<String, TreeItem<AppNavItem>> pageItems;
    private Consumer<String> navigationHandler = section -> { };
    private I18n i18n;

    AppNavTree(I18n i18n) {
        this.i18n = i18n;
        TreeItem<AppNavItem> rootItem = new TreeItem<>(AppNavItem.group("nav.root", Material2AL.ACCOUNT_TREE));
        TreeItem<AppNavItem> workspace = group("nav.group.workspace", Material2AL.HOME);
        TreeItem<AppNavItem> recording = group("nav.group.recording", Material2AL.ANALYTICS);
        TreeItem<AppNavItem> javaApp = group("nav.group.javaApplication", Material2AL.INSIGHTS);
        TreeItem<AppNavItem> memoryAnalysis = group("nav.group.memoryAnalysis", Material2MZ.MEMORY);
        TreeItem<AppNavItem> jvmInternals = group("nav.group.jvmInternals", Material2MZ.MEMORY);
        TreeItem<AppNavItem> environment = group("nav.group.environment", Material2MZ.PUBLIC);
        TreeItem<AppNavItem> application = group("nav.group.application", Material2MZ.SETTINGS);

        TreeItem<AppNavItem> home = page("home", "nav.home", Material2AL.HOME, false);
        TreeItem<AppNavItem> jvms = unavailablePage("jvms", "jvms.title", Material2MZ.MEMORY);
        TreeItem<AppNavItem> analysis = page("analysis", "analysis.title", Material2AL.INSIGHTS, true);
        TreeItem<AppNavItem> overview = page("overview", "overview.title", Material2MZ.PAGEVIEW, true);
        TreeItem<AppNavItem> events = page("events", "events.title", Material2AL.EVENT, true);
        TreeItem<AppNavItem> profiling = page("profiling", "nav.profiling", Material2AL.ASSIGNMENT, true);
        TreeItem<AppNavItem> exceptions = page("exceptions", "nav.exceptions", Material2MZ.REPORT, true);
        TreeItem<AppNavItem> threads = page("threads", "nav.threads", Material2AL.LIST, true);
        TreeItem<AppNavItem> fileio = page("fileio", "nav.fileio", Material2AL.FOLDER, true);
        TreeItem<AppNavItem> socketio = page("socketio", "nav.socketio", Material2MZ.NETWORK_CHECK, true);
        TreeItem<AppNavItem> locks = page("locks", "nav.locks", Material2AL.LOCK, true);
        TreeItem<AppNavItem> threadHistogram = page("threadHistogram", "nav.threadHistogram", Material2AL.LIST, true);
        TreeItem<AppNavItem> security = page("security", "nav.security", Material2MZ.SECURITY, true);
        TreeItem<AppNavItem> nativeLibraries = page("nativeLibraries", "nav.nativeLibraries", Material2MZ.STORAGE, true);
        TreeItem<AppNavItem> threadDumps = page("threadDumps", "nav.threadDumps", Material2MZ.VIEW_LIST, true);
        TreeItem<AppNavItem> heap = page("heap", "nav.heap", Material2MZ.MEMORY, true);
        TreeItem<AppNavItem> leaks = page("leaks", "nav.leaks", Material2AL.BUG_REPORT, true);
        TreeItem<AppNavItem> tlab = page("tlab", "nav.tlab", Material2MZ.STORAGE, true);
        TreeItem<AppNavItem> processPage = page("processes", "nav.processes", Material2MZ.MEMORY, true);
        TreeItem<AppNavItem> envVarsPage = page("envVars", "nav.envVars", Material2MZ.PUBLIC, true);
        TreeItem<AppNavItem> sysPropsPage = page("sysProps", "nav.sysProps", Material2MZ.SETTINGS, true);
        TreeItem<AppNavItem> recordingInfoPage = page("recordingInfo", "nav.recordingInfo", Material2MZ.SAVE, true);
        TreeItem<AppNavItem> agentsPage = page("agents", "nav.agents", Material2MZ.SECURITY, true);
        TreeItem<AppNavItem> constantPoolsPage = page("constantPools", "nav.constantPools", Material2MZ.VIEW_LIST, true);
        TreeItem<AppNavItem> jvmInfoPage = page("jvmInfo", "nav.jvmInfo", Material2MZ.VIEW_LIST, true);
        TreeItem<AppNavItem> gcConfigPage = page("gcConfig", "nav.gcConfig", Material2MZ.SETTINGS, true);
        TreeItem<AppNavItem> gcSummaryPage = page("gcSummary", "nav.gcSummary", Material2AL.LIST, true);
        TreeItem<AppNavItem> gcDetailsPage = page("gcDetails", "nav.gcDetails", Material2AL.BUG_REPORT, true);
        TreeItem<AppNavItem> compilationsPage = page("compilations", "nav.compilations", Material2MZ.PUBLIC, true);
        TreeItem<AppNavItem> codeCachePage = page("codeCache", "nav.codeCache", Material2MZ.STORAGE, true);
        TreeItem<AppNavItem> classLoadingPage = page("classLoading", "nav.classLoading", Material2MZ.SAVE, true);
        TreeItem<AppNavItem> vmOperationsPage = page("vmOperations", "nav.vmOperations", Material2MZ.SECURITY, true);
        TreeItem<AppNavItem> settings = page("settings", "settings.title", Material2MZ.SETTINGS, false);

        workspace.getChildren().setAll(List.of(home, jvms));
        recording.getChildren().setAll(List.of(analysis, overview, events));
        javaApp.getChildren().setAll(List.of(profiling, exceptions, threads, fileio, socketio, locks,
                threadHistogram, security, nativeLibraries, threadDumps));
        memoryAnalysis.getChildren().setAll(List.of(heap, leaks, tlab));
        environment.getChildren().setAll(List.of(processPage, envVarsPage, sysPropsPage,
                recordingInfoPage, agentsPage, constantPoolsPage));
        jvmInternals.getChildren().setAll(List.of(jvmInfoPage, gcConfigPage, gcSummaryPage, gcDetailsPage,
                compilationsPage, codeCachePage, classLoadingPage, vmOperationsPage));
        application.getChildren().setAll(List.of(settings));
        rootItem.getChildren().setAll(List.of(workspace, recording, javaApp, jvmInternals, memoryAnalysis, environment, application));
        rootItem.setExpanded(true);
        workspace.setExpanded(true);
        recording.setExpanded(true);
        javaApp.setExpanded(true);
        jvmInternals.setExpanded(true);
        memoryAnalysis.setExpanded(true);
        environment.setExpanded(true);
        application.setExpanded(true);

        pageItems = new HashMap<>(Map.of(
                "home", home,
                "jvms", jvms,
                "analysis", analysis,
                "overview", overview,
                "events", events,
                "profiling", profiling,
                "exceptions", exceptions,
                "threads", threads,
                "settings", settings));
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
        viewModel.recordingOpenProperty()
                .addListener((observable, oldValue, newValue) -> refresh());
        viewModel.selectedSectionProperty()
                .addListener((observable, oldValue, newValue) -> selectSection(newValue));
        selectSection(viewModel.selectedSectionProperty().get());
    }

    void setNavigationHandler(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler == null ? section -> { } : navigationHandler;
    }

    private void navigate(TreeItem<AppNavItem> item) {
        if (item == null || item.getValue() == null || !item.getValue().page()) {
            return;
        }
        AppNavItem navItem = item.getValue();
        if (navItem.unavailable(recordingOpen.get())) {
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
        getSelectionModel().select(item);
    }

    private static TreeItem<AppNavItem> group(String titleKey, org.kordamp.ikonli.Ikon icon) {
        return new TreeItem<>(AppNavItem.group(titleKey, icon));
    }

    private static TreeItem<AppNavItem> page(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon,
            boolean recordingScoped) {
        return new TreeItem<>(AppNavItem.page(sectionId, titleKey, icon, recordingScoped));
    }

    private static TreeItem<AppNavItem> unavailablePage(String sectionId, String titleKey, org.kordamp.ikonli.Ikon icon) {
        return new TreeItem<>(AppNavItem.unavailablePage(sectionId, titleKey, icon));
    }
}
