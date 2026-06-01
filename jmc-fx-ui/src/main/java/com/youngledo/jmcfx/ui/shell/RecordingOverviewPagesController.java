package com.youngledo.jmcfx.ui.shell;

import com.youngledo.jmcfx.ui.i18n.I18n;

/// Controller for recording overview page labels and navigation actions.
final class RecordingOverviewPagesController {

    private final RecordingOverviewPagesView view;
    private final AppShellViewModel viewModel;
    private final I18n i18n;

    RecordingOverviewPagesController(RecordingOverviewPagesView view, AppShellViewModel viewModel, I18n i18n) {
        this.view = view;
        this.viewModel = viewModel;
        this.i18n = i18n;
    }

    void configure() {
        bindLocalizedText();
        configureJavaApplicationOverviewActions();
        configureJvmInternalsOverviewActions();
        configureEnvironmentOverviewActions();
    }

    private void bindLocalizedText() {
        view.javaApplicationTitleLabel().textProperty().bind(i18n.text("javaApplication.title"));
        view.javaApplicationSummaryLabel().textProperty().bind(i18n.text("javaApplication.summary"));
        view.javaApplicationProfilingTitleLabel().textProperty().bind(i18n.text("javaApplication.profiling.title"));
        view.javaApplicationProfilingSummaryLabel().textProperty().bind(i18n.text("javaApplication.profiling.summary"));
        view.javaApplicationProfilingButton().textProperty().bind(i18n.text("javaApplication.profiling.action"));
        view.javaApplicationIoTitleLabel().textProperty().bind(i18n.text("javaApplication.io.title"));
        view.javaApplicationIoSummaryLabel().textProperty().bind(i18n.text("javaApplication.io.summary"));
        view.javaApplicationIoButton().textProperty().bind(i18n.text("javaApplication.io.action"));
        view.javaApplicationLocksTitleLabel().textProperty().bind(i18n.text("javaApplication.locks.title"));
        view.javaApplicationLocksSummaryLabel().textProperty().bind(i18n.text("javaApplication.locks.summary"));
        view.javaApplicationLocksButton().textProperty().bind(i18n.text("javaApplication.locks.action"));
        view.javaApplicationThreadsTitleLabel().textProperty().bind(i18n.text("javaApplication.threads.title"));
        view.javaApplicationThreadsSummaryLabel().textProperty().bind(i18n.text("javaApplication.threads.summary"));
        view.javaApplicationThreadsButton().textProperty().bind(i18n.text("javaApplication.threads.action"));
        view.javaApplicationExceptionsTitleLabel().textProperty().bind(i18n.text("javaApplication.exceptions.title"));
        view.javaApplicationExceptionsSummaryLabel().textProperty().bind(i18n.text("javaApplication.exceptions.summary"));
        view.javaApplicationExceptionsButton().textProperty().bind(i18n.text("javaApplication.exceptions.action"));
        view.javaApplicationClassLoadingTitleLabel().textProperty().bind(i18n.text("javaApplication.classLoading.title"));
        view.javaApplicationClassLoadingSummaryLabel().textProperty()
                .bind(i18n.text("javaApplication.classLoading.summary"));
        view.javaApplicationClassLoadingButton().textProperty().bind(i18n.text("javaApplication.classLoading.action"));
        view.javaApplicationAllocationTitleLabel().textProperty().bind(i18n.text("javaApplication.allocation.title"));
        view.javaApplicationAllocationSummaryLabel().textProperty()
                .bind(i18n.text("javaApplication.allocation.summary"));
        view.javaApplicationAllocationButton().textProperty().bind(i18n.text("javaApplication.allocation.action"));

        view.jvmInternalsTitleLabel().textProperty().bind(i18n.text("jvmInternals.title"));
        view.jvmInternalsSummaryLabel().textProperty().bind(i18n.text("jvmInternals.summary"));
        view.jvmInternalsInformationTitleLabel().textProperty().bind(i18n.text("jvmInternals.information.title"));
        view.jvmInternalsInformationSummaryLabel().textProperty().bind(i18n.text("jvmInternals.information.summary"));
        view.jvmInternalsInformationButton().textProperty().bind(i18n.text("jvmInternals.information.action"));
        view.jvmInternalsGcTitleLabel().textProperty().bind(i18n.text("jvmInternals.gc.title"));
        view.jvmInternalsGcSummaryLabel().textProperty().bind(i18n.text("jvmInternals.gc.summary"));
        view.jvmInternalsGcButton().textProperty().bind(i18n.text("jvmInternals.gc.action"));
        view.jvmInternalsG1TitleLabel().textProperty().bind(i18n.text("jvmInternals.g1.title"));
        view.jvmInternalsG1SummaryLabel().textProperty().bind(i18n.text("jvmInternals.g1.summary"));
        view.jvmInternalsG1Button().textProperty().bind(i18n.text("jvmInternals.g1.action"));
        view.jvmInternalsCompilationTitleLabel().textProperty().bind(i18n.text("jvmInternals.compilation.title"));
        view.jvmInternalsCompilationSummaryLabel().textProperty().bind(i18n.text("jvmInternals.compilation.summary"));
        view.jvmInternalsCompilationButton().textProperty().bind(i18n.text("jvmInternals.compilation.action"));
        view.jvmInternalsCodeCacheTitleLabel().textProperty().bind(i18n.text("jvmInternals.codeCache.title"));
        view.jvmInternalsCodeCacheSummaryLabel().textProperty().bind(i18n.text("jvmInternals.codeCache.summary"));
        view.jvmInternalsCodeCacheButton().textProperty().bind(i18n.text("jvmInternals.codeCache.action"));
        view.jvmInternalsClassLoadingTitleLabel().textProperty().bind(i18n.text("jvmInternals.classLoading.title"));
        view.jvmInternalsClassLoadingSummaryLabel().textProperty()
                .bind(i18n.text("jvmInternals.classLoading.summary"));
        view.jvmInternalsClassLoadingButton().textProperty().bind(i18n.text("jvmInternals.classLoading.action"));
        view.jvmInternalsVmOperationsTitleLabel().textProperty().bind(i18n.text("jvmInternals.vmOperations.title"));
        view.jvmInternalsVmOperationsSummaryLabel().textProperty().bind(i18n.text("jvmInternals.vmOperations.summary"));
        view.jvmInternalsVmOperationsButton().textProperty().bind(i18n.text("jvmInternals.vmOperations.action"));

        view.environmentTitleLabel().textProperty().bind(i18n.text("environment.title"));
        view.environmentSummaryLabel().textProperty().bind(i18n.text("environment.summary"));
        view.environmentProcessesTitleLabel().textProperty().bind(i18n.text("environment.processes.title"));
        view.environmentProcessesSummaryLabel().textProperty().bind(i18n.text("environment.processes.summary"));
        view.environmentProcessesButton().textProperty().bind(i18n.text("environment.processes.action"));
        view.environmentVariablesTitleLabel().textProperty().bind(i18n.text("environment.variables.title"));
        view.environmentVariablesSummaryLabel().textProperty().bind(i18n.text("environment.variables.summary"));
        view.environmentVariablesButton().textProperty().bind(i18n.text("environment.variables.action"));
        view.environmentPropertiesTitleLabel().textProperty().bind(i18n.text("environment.properties.title"));
        view.environmentPropertiesSummaryLabel().textProperty().bind(i18n.text("environment.properties.summary"));
        view.environmentPropertiesButton().textProperty().bind(i18n.text("environment.properties.action"));
        view.environmentRecordingTitleLabel().textProperty().bind(i18n.text("environment.recording.title"));
        view.environmentRecordingSummaryLabel().textProperty().bind(i18n.text("environment.recording.summary"));
        view.environmentRecordingButton().textProperty().bind(i18n.text("environment.recording.action"));
        view.environmentAgentsTitleLabel().textProperty().bind(i18n.text("environment.agents.title"));
        view.environmentAgentsSummaryLabel().textProperty().bind(i18n.text("environment.agents.summary"));
        view.environmentAgentsButton().textProperty().bind(i18n.text("environment.agents.action"));
        view.environmentConstantPoolsTitleLabel().textProperty().bind(i18n.text("environment.constantPools.title"));
        view.environmentConstantPoolsSummaryLabel().textProperty().bind(i18n.text("environment.constantPools.summary"));
        view.environmentConstantPoolsButton().textProperty().bind(i18n.text("environment.constantPools.action"));
    }

    private void configureJavaApplicationOverviewActions() {
        view.javaApplicationProfilingButton().setOnAction(event -> viewModel.showSection("profiling"));
        view.javaApplicationIoButton().setOnAction(event -> viewModel.showSection("fileio"));
        view.javaApplicationLocksButton().setOnAction(event -> viewModel.showSection("locks"));
        view.javaApplicationThreadsButton().setOnAction(event -> viewModel.showSection("threadHistogram"));
        view.javaApplicationExceptionsButton().setOnAction(event -> viewModel.showSection("exceptions"));
        view.javaApplicationClassLoadingButton().setOnAction(event -> viewModel.showSection("classLoading"));
        view.javaApplicationAllocationButton().setOnAction(event -> viewModel.showSection("tlab"));
    }

    private void configureJvmInternalsOverviewActions() {
        view.jvmInternalsInformationButton().setOnAction(event -> viewModel.showSection("jvmInfo"));
        view.jvmInternalsGcButton().setOnAction(event -> viewModel.showSection("gcSummary"));
        view.jvmInternalsG1Button().setOnAction(event -> viewModel.showSection("g1Gc"));
        view.jvmInternalsCompilationButton().setOnAction(event -> viewModel.showSection("compilations"));
        view.jvmInternalsCodeCacheButton().setOnAction(event -> viewModel.showSection("codeCache"));
        view.jvmInternalsClassLoadingButton().setOnAction(event -> viewModel.showSection("classLoading"));
        view.jvmInternalsVmOperationsButton().setOnAction(event -> viewModel.showSection("vmOperations"));
    }

    private void configureEnvironmentOverviewActions() {
        view.environmentProcessesButton().setOnAction(event -> viewModel.showSection("processes"));
        view.environmentVariablesButton().setOnAction(event -> viewModel.showSection("envVars"));
        view.environmentPropertiesButton().setOnAction(event -> viewModel.showSection("sysProps"));
        view.environmentRecordingButton().setOnAction(event -> viewModel.showSection("recordingInfo"));
        view.environmentAgentsButton().setOnAction(event -> viewModel.showSection("agents"));
        view.environmentConstantPoolsButton().setOnAction(event -> viewModel.showSection("constantPools"));
    }
}
