# Live JVM JMX Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the first-phase Live JVM Monitoring tab workflow for creating, starting, stopping, and observing JMX notification subscriptions.

**Architecture:** Keep the implementation inside the existing Live JVM Monitoring page. Add focused methods to `JvmBrowserViewModel`, wire new toolbar buttons in `AppShellController` and `app-shell.fxml`, localize labels, and update `docs/roadmap.md` to reflect that the first-phase toolbar workflow is complete.

**Tech Stack:** Java 26, JavaFX 26, Maven 4, JUnit 5, FXML, Java properties resource bundles.

---

### Task 1: ViewModel Notification Workflow

**Files:**
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModel.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModelTest.java`

- [x] **Step 1: Add failing ViewModel tests**

Add these tests near the existing JMX monitoring tests in `JvmBrowserViewModelTest`:

```java
    @Test
    void addMBeanNotificationSubscriptionCreatesPersistedSubscription() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeMBeanBrowserService mbeans = new FakeMBeanBrowserService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, mbeans, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        MBeanNode notifier = MBeanNode.objectName("demo:type=Notifier", "Notifier");
        mbeans.setTree(connected.id(), List.of(MBeanNode.domain("demo", List.of(notifier))));
        mbeans.setAttributes(connected.id(), notifier.objectName(), List.of());
        mbeans.setOperations(connected.id(), notifier.objectName(), List.of());
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedMBeanProperty().set(notifier);

        viewModel.addMBeanNotificationSubscription(notifier, 100, true);

        assertEquals(1, viewModel.jmxNotificationSubscriptionsProperty().size());
        JmxNotificationSubscription subscription = viewModel.jmxNotificationSubscriptionsProperty().getFirst();
        assertEquals(connected.id(), subscription.connectionId());
        assertEquals("demo:type=Notifier", subscription.objectName());
        assertEquals("Notifier", subscription.label());
        assertEquals(100, subscription.maxEvents());
        assertTrue(subscription.enabled());
        assertTrue(subscription.persisted());
        assertEquals(subscription, viewModel.selectedJmxNotificationSubscriptionProperty().get());
        assertEquals(List.of(subscription), repository.findNotificationSubscriptions(connected.id()));
    }

    @Test
    void addMBeanNotificationSubscriptionRejectsDomainNode() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        viewModel.selectedConnectionProperty().set(connected);

        viewModel.addMBeanNotificationSubscription(MBeanNode.domain("demo", List.of()), 100, true);

        assertTrue(viewModel.jmxMonitoringErrorProperty().get());
        assertEquals("Select an MBean object to subscribe to notifications.",
                viewModel.jmxMonitoringErrorMessageProperty().get());
        assertTrue(viewModel.jmxNotificationSubscriptionsProperty().isEmpty());
    }

    @Test
    void startSelectedJmxNotificationsAppendsInitialEvents() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 2, true, true);
        JmxNotificationEvent first = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo.first", "source", 1, "first", "");
        JmxNotificationEvent second = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH.plusSeconds(1), "demo.second", "source", 2, "second", "");
        monitoring.setNotificationEvents(connected.id(), subscription.id(), List.of(first, second));
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedJmxNotificationSubscriptionProperty().set(subscription);

        viewModel.startSelectedJmxNotifications();

        assertEquals(List.of(first, second), viewModel.jmxNotificationEventsProperty());
        assertEquals(List.of(subscription), repository.findNotificationSubscriptions(connected.id()));
        assertEquals(List.of(first, second), repository.findNotificationEvents(subscription.id()));
        assertFalse(viewModel.jmxMonitoringLoadingProperty().get());
        assertFalse(viewModel.jmxMonitoringErrorProperty().get());
    }

    @Test
    void stopSelectedJmxNotificationsStopsServiceAndKeepsEvents() {
        FakeJmxConnectionService jmx = new FakeJmxConnectionService();
        FakeJmxMonitoringService monitoring = new FakeJmxMonitoringService();
        FakeJmxMonitoringRepository repository = new FakeJmxMonitoringRepository();
        JvmBrowserViewModel viewModel = viewModel(new FakeJvmDiscoveryService(), jmx, null, monitoring, repository);
        JvmConnection connected = connectedWithMBeans(viewModel, jmx);
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "notif-1", connected.id(), "demo:type=Notifier", "Notifier", 2, true, false);
        JmxNotificationEvent event = new JmxNotificationEvent(
                "notif-1", Instant.EPOCH, "demo", "source", 1, "message", "");
        repository.saveNotificationSubscription(subscription);
        repository.appendNotificationEvent(event);
        viewModel.selectedConnectionProperty().set(connected);
        viewModel.selectedJmxNotificationSubscriptionProperty().set(subscription);

        viewModel.stopSelectedJmxNotifications();

        assertEquals(List.of(subscription.id()), monitoring.stoppedNotificationIds());
        assertEquals(List.of(event), viewModel.jmxNotificationEventsProperty());
        assertFalse(viewModel.jmxMonitoringLoadingProperty().get());
        assertFalse(viewModel.jmxMonitoringErrorProperty().get());
    }
```

- [x] **Step 2: Run tests and verify failure**

Run:

```bash
./mvnw -pl jmc-fx-ui -Dtest=JvmBrowserViewModelTest test
```

Expected: fails because `addMBeanNotificationSubscription`, `startSelectedJmxNotifications`, and `stopSelectedJmxNotifications` do not exist.

- [x] **Step 3: Implement ViewModel methods**

In `JvmBrowserViewModel`, add these public methods near the existing monitoring methods:

```java
    public void addMBeanNotificationSubscription(MBeanNode node, int maxEvents, boolean persisted) {
        JvmSessionSnapshot snapshot = selectedSession.get();
        if (snapshot == null || !jmxMonitoringAvailable.get() || jmxMonitoringService == null) {
            failJmxMonitoring("Select a connected JVM with JMX monitoring available.");
            return;
        }
        if (node == null || node.domain()) {
            failJmxMonitoring("Select an MBean object to subscribe to notifications.");
            return;
        }
        String label = node.name() == null || node.name().isBlank() ? node.objectName() : node.name();
        JmxNotificationSubscription subscription = new JmxNotificationSubscription(
                "",
                snapshot.connection().id(),
                node.objectName(),
                label,
                maxEvents,
                true,
                persisted);
        jmxNotificationSubscriptions.add(subscription);
        selectedJmxNotificationSubscription.set(subscription);
        if (persisted && jmxMonitoringRepository != null) {
            jmxMonitoringRepository.saveNotificationSubscription(subscription);
        }
        updateOverviewPersistenceSummary();
        clearJmxMonitoringError();
    }

    public void startSelectedJmxNotifications() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        JmxNotificationSubscription subscription = selectedJmxNotificationSubscription.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX notification subscription to start.");
            return;
        }
        startJmxNotifications(subscription);
    }

    public void stopSelectedJmxNotifications() {
        JvmSessionSnapshot snapshot = selectedSession.get();
        JmxNotificationSubscription subscription = selectedJmxNotificationSubscription.get();
        if (snapshot == null || subscription == null || jmxMonitoringService == null) {
            failJmxMonitoring("Select a JMX notification subscription to stop.");
            return;
        }
        long generation = nextJmxMonitoringGeneration();
        jmxMonitoringLoading.set(true);
        clearJmxMonitoringError();
        executor.execute(() -> {
            try {
                jmxMonitoringService.stopNotifications(snapshot.connection(), subscription.id());
                runOnFx(() -> {
                    if (!isCurrentJmxMonitoringGeneration(generation, snapshot)) {
                        return;
                    }
                    jmxMonitoringLoading.set(false);
                    clearJmxMonitoringError();
                });
            } catch (RuntimeException exception) {
                failJmxMonitoring(generation, snapshot, exception);
            }
        });
    }
```

- [x] **Step 4: Run ViewModel test**

Run:

```bash
./mvnw -pl jmc-fx-ui -Dtest=JvmBrowserViewModelTest test
```

Expected: pass.

### Task 2: Monitoring Toolbar UI Wiring

**Files:**
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`

- [x] **Step 1: Add failing shell contract tests**

Update `AppShellTest` monitoring assertions so they require:

```java
        assertEquals("Button", elementByFxId(document, "jvmsAddNotificationSubscriptionButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStartNotificationsButton").getTagName());
        assertEquals("Button", elementByFxId(document, "jvmsStopNotificationsButton").getTagName());
```

Add assertions in an existing JVM Browser binding/i18n test:

```java
        assertTrue(controller.contains("@FXML private Button jvmsAddNotificationSubscriptionButton;"));
        assertTrue(controller.contains("@FXML private Button jvmsStartNotificationsButton;"));
        assertTrue(controller.contains("@FXML private Button jvmsStopNotificationsButton;"));
        assertTrue(controller.contains("jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription())"));
        assertTrue(controller.contains("jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications())"));
        assertTrue(controller.contains("jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications())"));
        assertTrue(controller.contains("jvmsAddNotificationSubscriptionButton.textProperty().bind(i18n.text(\"jvms.monitoring.addNotification\"))"));
        assertTrue(controller.contains("jvmsStartNotificationsButton.textProperty().bind(i18n.text(\"jvms.monitoring.startNotifications\"))"));
        assertTrue(controller.contains("jvmsStopNotificationsButton.textProperty().bind(i18n.text(\"jvms.monitoring.stopNotifications\"))"));
        assertTrue(english.contains("jvms.monitoring.addSubscription=Add Attribute"));
        assertTrue(english.contains("jvms.monitoring.addNotification=Add Notification"));
        assertTrue(english.contains("jvms.monitoring.startNotifications=Start Notifications"));
        assertTrue(english.contains("jvms.monitoring.stopNotifications=Stop Notifications"));
        assertTrue(chinese.contains("jvms.monitoring.addSubscription=添加属性"));
        assertTrue(chinese.contains("jvms.monitoring.addNotification=添加通知"));
        assertTrue(chinese.contains("jvms.monitoring.startNotifications=开始通知"));
        assertTrue(chinese.contains("jvms.monitoring.stopNotifications=停止通知"));
```

- [x] **Step 2: Run shell test and verify failure**

Run:

```bash
./mvnw -pl jmc-fx-ui -Dtest=AppShellTest test
```

Expected: fails because buttons, controller fields, bindings, and i18n keys are missing.

- [x] **Step 3: Add FXML buttons**

In `app-shell.fxml`, inside `jvmsMonitoringToolbar`, change:

```xml
                                                <Button fx:id="jvmsAddMonitoringSubscriptionButton"/>
                                                <Button fx:id="jvmsSampleSubscriptionButton"/>
```

to:

```xml
                                                <Button fx:id="jvmsAddMonitoringSubscriptionButton"/>
                                                <Button fx:id="jvmsAddNotificationSubscriptionButton"/>
                                                <Button fx:id="jvmsStartNotificationsButton"/>
                                                <Button fx:id="jvmsStopNotificationsButton"/>
                                                <Button fx:id="jvmsSampleSubscriptionButton"/>
```

- [x] **Step 4: Wire controller fields, actions, disable state, and helper**

In `AppShellController`, add fields beside the existing monitoring buttons:

```java
    @FXML private Button jvmsAddNotificationSubscriptionButton;
    @FXML private Button jvmsStartNotificationsButton;
    @FXML private Button jvmsStopNotificationsButton;
```

When the JVM browser service is absent, disable the new buttons beside existing monitoring controls.

In action binding, add:

```java
        jvmsAddNotificationSubscriptionButton.setOnAction(event -> addSelectedNotificationSubscription());
        jvmsStartNotificationsButton.setOnAction(event -> jvmBrowserViewModel.startSelectedJmxNotifications());
        jvmsStopNotificationsButton.setOnAction(event -> jvmBrowserViewModel.stopSelectedJmxNotifications());
```

In `bindJmxMonitoring()`, add disable bindings:

```java
        jvmsAddNotificationSubscriptionButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringAvailableProperty().not()
                .or(jvmBrowserViewModel.selectedMBeanProperty().isNull()));
        jvmsStartNotificationsButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringLoadingProperty()
                .or(jvmBrowserViewModel.selectedJmxNotificationSubscriptionProperty().isNull()));
        jvmsStopNotificationsButton.disableProperty().bind(jvmBrowserViewModel.jmxMonitoringLoadingProperty()
                .or(jvmBrowserViewModel.selectedJmxNotificationSubscriptionProperty().isNull()));
```

Add helper near `addSelectedMonitoringSubscription()`:

```java
    private void addSelectedNotificationSubscription() {
        jvmBrowserViewModel.addMBeanNotificationSubscription(
                jvmBrowserViewModel.selectedMBeanProperty().get(), 100, true);
    }
```

In localized text binding, add:

```java
        jvmsAddNotificationSubscriptionButton.textProperty().bind(i18n.text("jvms.monitoring.addNotification"));
        jvmsStartNotificationsButton.textProperty().bind(i18n.text("jvms.monitoring.startNotifications"));
        jvmsStopNotificationsButton.textProperty().bind(i18n.text("jvms.monitoring.stopNotifications"));
```

- [x] **Step 5: Update i18n labels**

In `messages.properties`, change and add:

```properties
jvms.monitoring.addSubscription=Add Attribute
jvms.monitoring.addNotification=Add Notification
jvms.monitoring.startNotifications=Start Notifications
jvms.monitoring.stopNotifications=Stop Notifications
```

In `messages_zh_CN.properties`, change and add:

```properties
jvms.monitoring.addSubscription=添加属性
jvms.monitoring.addNotification=添加通知
jvms.monitoring.startNotifications=开始通知
jvms.monitoring.stopNotifications=停止通知
```

- [x] **Step 6: Run shell test**

Run:

```bash
./mvnw -pl jmc-fx-ui -Dtest=AppShellTest test
```

Expected: pass.

### Task 3: Roadmap Update And Verification

**Files:**
- Modify: `docs/roadmap.md`

- [x] **Step 1: Update roadmap JMX capability and P1 item**

In `docs/roadmap.md`, replace:

```markdown
- Provides JMX notification model, storage, and service support, but the
  notification UI workflow is not yet complete.
```

with:

```markdown
- Provides a first-phase JMX notification workflow from the Monitoring tab:
  create a subscription from the selected MBean, start or stop listening, and
  retain observed events.
```

Replace the P1 item:

```markdown
- Complete the Live JVM JMX monitoring notification workflow.
  - Add user-facing controls for creating/selecting notification
    subscriptions.
  - Add start and stop actions for notification listeners.
  - Surface active/listening state and failure state in the monitoring page.
  - Preserve persisted notification subscriptions and retained events.
```

with:

```markdown
- Expand Live JVM JMX notification management beyond the first-phase toolbar
  workflow.
  - Add a dedicated notification-subscription list only if users need to manage
    multiple simultaneous notification sources.
  - Surface active/listening state per subscription when multi-subscription
    management is introduced.
```

- [x] **Step 2: Run final targeted tests**

Run:

```bash
./mvnw -pl jmc-fx-ui -Dtest=JvmBrowserViewModelTest test
./mvnw -pl jmc-fx-ui -Dtest=AppShellTest test
```

Expected: both pass.

- [x] **Step 3: Run AGENTS verification**

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw -v
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./mvnw verify
rg -n "<modules>|<module>" pom.xml **/pom.xml
rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain
```

Expected:

- Maven reports `4.0.0-rc-5`.
- Java reports `26.0.1`.
- `./mvnw verify` ends with `BUILD SUCCESS`.
- Both `rg` boundary checks produce no matches and exit code `1`.

### Task 4: Commit Implementation

**Files:**
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModel.java`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModelTest.java`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml`
- Modify: `jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties`
- Modify: `jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties`
- Modify: `jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java`
- Modify: `docs/roadmap.md`
- Add: `docs/superpowers/plans/2026-06-01-live-jvm-jmx-notifications.md`

- [x] **Step 1: Review diff**

Run:

```bash
git diff --stat
git diff -- jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModel.java
git diff -- jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModelTest.java
git diff -- jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml
git diff -- jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java
git diff -- jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties
git diff -- jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties
git diff -- jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java
git diff -- docs/roadmap.md
```

Expected: changes match this plan and do not introduce unrelated refactors.

- [x] **Step 2: Stage and commit**

Run:

```bash
git add jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModel.java
git add jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/jvms/JvmBrowserViewModelTest.java
git add jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/shell/app-shell.fxml
git add jmc-fx-ui/src/main/java/com/youngledo/jmcfx/ui/shell/AppShellController.java
git add jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages.properties
git add jmc-fx-ui/src/main/resources/com/youngledo/jmcfx/ui/i18n/messages_zh_CN.properties
git add jmc-fx-ui/src/test/java/com/youngledo/jmcfx/ui/shell/AppShellTest.java
git add docs/roadmap.md
git add -f docs/superpowers/plans/2026-06-01-live-jvm-jmx-notifications.md
git commit -m "feat(jvms): complete jmx notification workflow"
```

Expected: commit succeeds.

## Self-Review

- Spec coverage: tasks cover Monitoring toolbar workflow, ViewModel creation/start/stop behavior, persistence, errors, FXML/controller/i18n contracts, roadmap update, targeted tests, and AGENTS verification.
- Completion-marker scan: this plan intentionally contains no unfinished implementation instructions.
- Scope check: this plan does not add MBean Browser entry points, a notification subscription table, shell decomposition, credential/TLS/Jolokia discovery/WebSocket/JConsole behavior, or UI/app/domain JMC API usage.
