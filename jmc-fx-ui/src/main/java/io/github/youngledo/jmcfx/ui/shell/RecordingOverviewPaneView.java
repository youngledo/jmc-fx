package io.github.youngledo.jmcfx.ui.shell;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/// Code-first view for the recording parent overview pages.
final class RecordingOverviewPaneView {

    private final Label javaApplicationTitleLabel = new Label();
    private final Label javaApplicationSummaryLabel = new Label();
    private final Label javaApplicationProfilingTitleLabel = new Label();
    private final Label javaApplicationProfilingSummaryLabel = new Label();
    private final Button javaApplicationProfilingButton = new Button();
    private final Label javaApplicationIoTitleLabel = new Label();
    private final Label javaApplicationIoSummaryLabel = new Label();
    private final Button javaApplicationIoButton = new Button();
    private final Label javaApplicationLocksTitleLabel = new Label();
    private final Label javaApplicationLocksSummaryLabel = new Label();
    private final Button javaApplicationLocksButton = new Button();
    private final Label javaApplicationThreadsTitleLabel = new Label();
    private final Label javaApplicationThreadsSummaryLabel = new Label();
    private final Button javaApplicationThreadsButton = new Button();
    private final Label javaApplicationExceptionsTitleLabel = new Label();
    private final Label javaApplicationExceptionsSummaryLabel = new Label();
    private final Button javaApplicationExceptionsButton = new Button();
    private final Label javaApplicationClassLoadingTitleLabel = new Label();
    private final Label javaApplicationClassLoadingSummaryLabel = new Label();
    private final Button javaApplicationClassLoadingButton = new Button();
    private final Label javaApplicationAllocationTitleLabel = new Label();
    private final Label javaApplicationAllocationSummaryLabel = new Label();
    private final Button javaApplicationAllocationButton = new Button();
    private final Label jvmInternalsTitleLabel = new Label();
    private final Label jvmInternalsSummaryLabel = new Label();
    private final Label jvmInternalsInformationTitleLabel = new Label();
    private final Label jvmInternalsInformationSummaryLabel = new Label();
    private final Button jvmInternalsInformationButton = new Button();
    private final Label jvmInternalsGcTitleLabel = new Label();
    private final Label jvmInternalsGcSummaryLabel = new Label();
    private final Button jvmInternalsGcButton = new Button();
    private final Label jvmInternalsG1TitleLabel = new Label();
    private final Label jvmInternalsG1SummaryLabel = new Label();
    private final Button jvmInternalsG1Button = new Button();
    private final Label jvmInternalsCompilationTitleLabel = new Label();
    private final Label jvmInternalsCompilationSummaryLabel = new Label();
    private final Button jvmInternalsCompilationButton = new Button();
    private final Label jvmInternalsCodeCacheTitleLabel = new Label();
    private final Label jvmInternalsCodeCacheSummaryLabel = new Label();
    private final Button jvmInternalsCodeCacheButton = new Button();
    private final Label jvmInternalsClassLoadingTitleLabel = new Label();
    private final Label jvmInternalsClassLoadingSummaryLabel = new Label();
    private final Button jvmInternalsClassLoadingButton = new Button();
    private final Label jvmInternalsVmOperationsTitleLabel = new Label();
    private final Label jvmInternalsVmOperationsSummaryLabel = new Label();
    private final Button jvmInternalsVmOperationsButton = new Button();
    private final Label environmentTitleLabel = new Label();
    private final Label environmentSummaryLabel = new Label();
    private final Label environmentProcessesTitleLabel = new Label();
    private final Label environmentProcessesSummaryLabel = new Label();
    private final Button environmentProcessesButton = new Button();
    private final Label environmentVariablesTitleLabel = new Label();
    private final Label environmentVariablesSummaryLabel = new Label();
    private final Button environmentVariablesButton = new Button();
    private final Label environmentPropertiesTitleLabel = new Label();
    private final Label environmentPropertiesSummaryLabel = new Label();
    private final Button environmentPropertiesButton = new Button();
    private final Label environmentRecordingTitleLabel = new Label();
    private final Label environmentRecordingSummaryLabel = new Label();
    private final Button environmentRecordingButton = new Button();
    private final Label environmentAgentsTitleLabel = new Label();
    private final Label environmentAgentsSummaryLabel = new Label();
    private final Button environmentAgentsButton = new Button();
    private final Label environmentConstantPoolsTitleLabel = new Label();
    private final Label environmentConstantPoolsSummaryLabel = new Label();
    private final Button environmentConstantPoolsButton = new Button();

    RecordingOverviewPaneView(VBox javaApplicationPane, VBox jvmInternalsPane, VBox environmentPane) {
        configure(javaApplicationPane, jvmInternalsPane, environmentPane);
    }

    RecordingOverviewPagesView view() {
        return new RecordingOverviewPagesView(javaApplicationTitleLabel, javaApplicationSummaryLabel,
                javaApplicationProfilingTitleLabel, javaApplicationProfilingSummaryLabel,
                javaApplicationProfilingButton, javaApplicationIoTitleLabel, javaApplicationIoSummaryLabel,
                javaApplicationIoButton, javaApplicationLocksTitleLabel, javaApplicationLocksSummaryLabel,
                javaApplicationLocksButton, javaApplicationThreadsTitleLabel, javaApplicationThreadsSummaryLabel,
                javaApplicationThreadsButton, javaApplicationExceptionsTitleLabel,
                javaApplicationExceptionsSummaryLabel, javaApplicationExceptionsButton,
                javaApplicationClassLoadingTitleLabel, javaApplicationClassLoadingSummaryLabel,
                javaApplicationClassLoadingButton, javaApplicationAllocationTitleLabel,
                javaApplicationAllocationSummaryLabel, javaApplicationAllocationButton,
                jvmInternalsTitleLabel, jvmInternalsSummaryLabel,
                jvmInternalsInformationTitleLabel, jvmInternalsInformationSummaryLabel,
                jvmInternalsInformationButton, jvmInternalsGcTitleLabel, jvmInternalsGcSummaryLabel,
                jvmInternalsGcButton, jvmInternalsG1TitleLabel, jvmInternalsG1SummaryLabel,
                jvmInternalsG1Button, jvmInternalsCompilationTitleLabel,
                jvmInternalsCompilationSummaryLabel, jvmInternalsCompilationButton,
                jvmInternalsCodeCacheTitleLabel, jvmInternalsCodeCacheSummaryLabel,
                jvmInternalsCodeCacheButton, jvmInternalsClassLoadingTitleLabel,
                jvmInternalsClassLoadingSummaryLabel, jvmInternalsClassLoadingButton,
                jvmInternalsVmOperationsTitleLabel, jvmInternalsVmOperationsSummaryLabel,
                jvmInternalsVmOperationsButton, environmentTitleLabel, environmentSummaryLabel,
                environmentProcessesTitleLabel, environmentProcessesSummaryLabel, environmentProcessesButton,
                environmentVariablesTitleLabel, environmentVariablesSummaryLabel, environmentVariablesButton,
                environmentPropertiesTitleLabel, environmentPropertiesSummaryLabel, environmentPropertiesButton,
                environmentRecordingTitleLabel, environmentRecordingSummaryLabel, environmentRecordingButton,
                environmentAgentsTitleLabel, environmentAgentsSummaryLabel, environmentAgentsButton,
                environmentConstantPoolsTitleLabel, environmentConstantPoolsSummaryLabel,
                environmentConstantPoolsButton);
    }

    private void configure(VBox javaApplicationPane, VBox jvmInternalsPane, VBox environmentPane) {
        configureActionOverview(javaApplicationPane, javaApplicationTitleLabel, javaApplicationSummaryLabel,
                new Node[] { summaryAction(javaApplicationProfilingTitleLabel, javaApplicationProfilingSummaryLabel, javaApplicationProfilingButton),
                        summaryAction(javaApplicationIoTitleLabel, javaApplicationIoSummaryLabel, javaApplicationIoButton),
                        summaryAction(javaApplicationLocksTitleLabel, javaApplicationLocksSummaryLabel, javaApplicationLocksButton),
                        summaryAction(javaApplicationThreadsTitleLabel, javaApplicationThreadsSummaryLabel, javaApplicationThreadsButton),
                        summaryAction(javaApplicationExceptionsTitleLabel, javaApplicationExceptionsSummaryLabel, javaApplicationExceptionsButton),
                        summaryAction(javaApplicationClassLoadingTitleLabel, javaApplicationClassLoadingSummaryLabel, javaApplicationClassLoadingButton),
                        summaryAction(javaApplicationAllocationTitleLabel, javaApplicationAllocationSummaryLabel, javaApplicationAllocationButton) },
                "java-application-overview-page");
        configureActionOverview(jvmInternalsPane, jvmInternalsTitleLabel, jvmInternalsSummaryLabel,
                new Node[] { summaryAction(jvmInternalsInformationTitleLabel, jvmInternalsInformationSummaryLabel, jvmInternalsInformationButton),
                        summaryAction(jvmInternalsGcTitleLabel, jvmInternalsGcSummaryLabel, jvmInternalsGcButton),
                        summaryAction(jvmInternalsG1TitleLabel, jvmInternalsG1SummaryLabel, jvmInternalsG1Button),
                        summaryAction(jvmInternalsCompilationTitleLabel, jvmInternalsCompilationSummaryLabel, jvmInternalsCompilationButton),
                        summaryAction(jvmInternalsCodeCacheTitleLabel, jvmInternalsCodeCacheSummaryLabel, jvmInternalsCodeCacheButton),
                        summaryAction(jvmInternalsClassLoadingTitleLabel, jvmInternalsClassLoadingSummaryLabel, jvmInternalsClassLoadingButton),
                        summaryAction(jvmInternalsVmOperationsTitleLabel, jvmInternalsVmOperationsSummaryLabel, jvmInternalsVmOperationsButton) },
                "jvm-internals-overview-page");
        configureActionOverview(environmentPane, environmentTitleLabel, environmentSummaryLabel,
                new Node[] { summaryAction(environmentProcessesTitleLabel, environmentProcessesSummaryLabel, environmentProcessesButton),
                        summaryAction(environmentVariablesTitleLabel, environmentVariablesSummaryLabel, environmentVariablesButton),
                        summaryAction(environmentPropertiesTitleLabel, environmentPropertiesSummaryLabel, environmentPropertiesButton),
                        summaryAction(environmentRecordingTitleLabel, environmentRecordingSummaryLabel, environmentRecordingButton),
                        summaryAction(environmentAgentsTitleLabel, environmentAgentsSummaryLabel, environmentAgentsButton),
                        summaryAction(environmentConstantPoolsTitleLabel, environmentConstantPoolsSummaryLabel, environmentConstantPoolsButton) },
                "environment-overview-page");
    }

    private void configureActionOverview(VBox pane, Label title, Label summary, Node[] actions, String pageClass) {
        styles(pane, "page", "overview-page", pageClass);
        styles(title, "view-title");
        wrap(summary);
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        styles(grid, "metric-grid");
        for (int index = 0; index < actions.length; index++) {
            grid.add(actions[index], index % 2, index / 2);
        }
        VBox header = vbox(0, title, summary);
        styles(header, "page-header");
        pane.getChildren().setAll(header, grid);
    }

    private VBox summaryAction(Label title, Label summary, Button button) {
        styles(title, "detail-title");
        wrap(summary);
        VBox panel = vbox(6, title, summary, button);
        styles(panel, "summary-panel");
        return panel;
    }

    private static VBox vbox(double spacing, Node... children) {
        return new VBox(spacing, children);
    }

    private static void wrap(Label... labels) {
        for (Label label : labels) {
            label.setWrapText(true);
        }
    }

    private static void styles(Node node, String... styleClasses) {
        node.getStyleClass().addAll(styleClasses);
    }
}
