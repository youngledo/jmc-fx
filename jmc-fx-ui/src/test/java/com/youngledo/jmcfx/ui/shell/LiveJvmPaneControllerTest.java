package com.youngledo.jmcfx.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class LiveJvmPaneControllerTest {

    @Test
    void liveJvmPaneOwnsWorkspaceRootAndController() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");
        Element root = document.getDocumentElement();

        assertEquals("VBox", root.getTagName());
        assertEquals("jvmsPane", root.getAttribute("fx:id"));
        assertEquals("com.youngledo.jmcfx.ui.shell.LiveJvmPaneController",
                root.getAttribute("fx:controller"));
    }

    @Test
    void liveJvmPanePreservesBrowserAndLiveTabs() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertEquals("TableView", elementByFxId(document, "jvmsTable").getTagName());
        assertEquals("ALWAYS", elementByFxId(document, "jvmsTable").getAttribute("VBox.vgrow"));
        assertEquals("TabPane", elementByFxId(document, "jvmsLiveTabs").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsOverviewTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsSessionTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsMBeanTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsDiagnosticsTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsTriggersTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsMonitoringTab").getTagName());
        assertEquals("Tab", elementByFxId(document, "jvmsAgentTab").getTagName());
    }

    @Test
    void liveJvmPanePreservesMonitoringNotificationControls() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringContent"), "jvms-live-tab-content"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringToolbar"), "page-toolbar"));
        assertEquals("Button", elementByFxId(document, "jvmsAddMonitoringSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsSampleSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsAddNotificationSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStartNotificationsButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStopNotificationsButton").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringSubscriptionsTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsMonitoringSubscriptionsTable"), "dense-table"));
        assertEquals("LineChart", elementByFxId(document, "jvmsMonitoringChart").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringSamplesTable").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsMonitoringNotificationsTable").getTagName());
    }

    @Test
    void liveJvmPanePreservesAgentDetailPanelContract() throws Exception {
        Document document = fxmlDocument("live-jvm-pane.fxml");

        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentContent"), "jvms-live-tab-content"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentToolbar"), "page-toolbar"));
        assertEquals("ComboBox", elementByFxId(document, "jvmsAgentPresetCombo").getTagName());
        assertEquals("TableView", elementByFxId(document, "jvmsAgentTransformsTable").getTagName());
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentTransformsTable"), "dense-table"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationPane"), "detail-panel"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationTitleLabel"),
                "detail-panel-title"));
        assertTrue(hasStyleClass(elementByFxId(document, "jvmsAgentConfigurationArea"), "detail-panel-body"));
    }

    @Test
    void liveJvmControllerOwnsLiveJvmBindingsAndActions() throws Exception {
        String source = source("src/main/java/com/youngledo/jmcfx/ui/shell/LiveJvmPaneController.java");

        assertTrue(source.contains("class LiveJvmPaneController"));
        assertTrue(source.contains("void configure(I18n i18n, JvmBrowserViewModel viewModel)"));
        assertTrue(source.contains("@FXML private TableView<JvmConnection> jvmsTable;"));
        assertTrue(source.contains("@FXML private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(source.contains(
                "jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(source.contains(
                "jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications())"));
        assertTrue(source.contains(
                "jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications())"));
        assertTrue(source.contains("jvmsAgentTab.textProperty().bind(i18n.text(\"jvms.agent.tab\"))"));
    }

    private Document fxmlDocument(String name) throws Exception {
        try (InputStream stream = LiveJvmPaneControllerTest.class.getResourceAsStream(name)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(stream);
        }
    }

    private static Element elementByFxId(Document document, String fxId) {
        NodeList nodes = document.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && fxId.equals(element.getAttribute("fx:id"))) {
                return element;
            }
        }
        throw new AssertionError("Missing fx:id: " + fxId);
    }

    private static boolean hasStyleClass(Element element, String styleClass) {
        String direct = element.getAttribute("styleClass");
        if (direct != null && java.util.Arrays.asList(direct.split("\\s+")).contains(styleClass)) {
            return true;
        }
        NodeList children = element.getElementsByTagName("String");
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element child && styleClass.equals(child.getAttribute("fx:value"))) {
                return true;
            }
        }
        return false;
    }

    private static String source(String path) throws Exception {
        return java.nio.file.Files.readString(java.nio.file.Path.of(path));
    }
}
