# Live JVM JMX Notifications Design

## Purpose

JMC FX already has JMX notification domain models, repository support, adapter
service support, and a notifications table in the Live JVM Monitoring tab. The
missing product gap is the user-facing workflow that lets users create,
start, stop, and observe notification subscriptions from the Monitoring page.

This design completes that workflow without expanding the scope to the MBean
Browser page or restructuring the shell.

## Goals

- Add a Monitoring-tab workflow for creating a notification subscription from
  the currently selected MBean.
- Add start and stop actions for the selected notification subscription.
- Preserve existing JMX attribute monitoring and Sample Now behavior.
- Surface monitoring errors through the existing monitoring error label.
- Persist notification subscriptions and retained events through the existing
  monitoring repository path.
- Keep long-running JMX work off the FX Application Thread.
- Cover ViewModel behavior and FXML/controller/i18n contracts with tests.

## Non-Goals

- Do not move notification creation into the MBean Browser page in this phase.
- Do not introduce a second notification-subscription table in this phase.
- Do not redesign the Monitoring tab layout beyond toolbar controls needed for
  the workflow.
- Do not split `AppShellController` or `app-shell.fxml` in this phase.
- Do not change adapter boundaries or call JMC APIs from UI, app, or domain
  modules.
- Do not add credential, TLS, Jolokia discovery, WebSocket, or JConsole plug-in
  behavior.

## UX Scope

The affected surface is the Live JVM workspace's Monitoring tab.

Applicable UI/UX contracts:

- `docs/ui-ux-system.md` Live JVM Experience.
- Control Panel Page for connected-JVM actions.
- Selection-Driven Detail for selecting an MBean or subscription and seeing
  samples/events.
- `page-toolbar` for compact command controls.
- Dense `TableView` for subscriptions, samples, and notifications.
- Existing shell status/error patterns rather than modal-first workflows.

The workflow is:

1. User connects to a JVM with MBean server capability.
2. User selects an MBean in the MBean tab or any UI path that sets the selected
   MBean in `JvmBrowserViewModel`.
3. User opens the Monitoring tab.
4. User clicks `Add Notification`.
5. JMC FX creates and selects a notification subscription for the selected
   MBean.
6. User clicks `Start Notifications`.
7. JMC FX starts the listener through `JmxMonitoringService`.
8. Incoming or initially returned events appear in the existing notifications
   table.
9. User clicks `Stop Notifications` to remove the listener for the selected
   subscription.

## UI Changes

The Monitoring toolbar should contain:

- `Add Attribute`: renamed label for the existing attribute subscription
  action.
- `Add Notification`: creates a notification subscription from the selected
  MBean.
- `Start Notifications`: starts the selected notification subscription.
- `Stop Notifications`: stops the selected notification subscription.
- `Sample Now`: retains the existing manual sample action for selected
  attribute subscriptions.

No cards or feature-specific detail-panel classes are added. Existing
`page-toolbar`, `dense-table`, and monitoring page classes remain the primary
layout structure.

The current table layout remains:

- Attribute subscriptions table.
- Samples table.
- Notifications table.

Notification subscription selection is maintained in the ViewModel. In this
phase, the UI does not add a dedicated notification-subscription table; the
most recently created or started notification subscription becomes selected.

## ViewModel Design

`JvmBrowserViewModel` should gain focused notification workflow methods:

- `addMBeanNotificationSubscription(MBeanNode node, int maxEvents, boolean persisted)`
- `startSelectedJmxNotifications()`
- `stopSelectedJmxNotifications()`

The default UI action should call notification creation with:

- selected MBean from `selectedMBeanProperty()`
- `maxEvents = 100`
- `persisted = true`

The subscription should use:

- current connected JVM id
- selected MBean object name
- selected MBean display name, falling back to object name when needed
- enabled state true
- persisted state from the action argument

Invalid creation conditions should set the existing monitoring error state:

- no selected connected session
- monitoring service unavailable
- MBean server capability unavailable
- no selected MBean
- selected MBean is a domain node

Start should:

- require selected connected session, selected notification subscription, and
  monitoring service
- persist the subscription when configured to persist
- call `JmxMonitoringService.startNotifications(...)` through the existing
  executor
- append initially returned events and callback events through the existing
  bounded event append path
- clear the monitoring error state on success

Stop should:

- require selected connected session, selected notification subscription, and
  monitoring service
- call `JmxMonitoringService.stopNotifications(...)` through the existing
  executor
- clear the monitoring loading and error states on success
- leave the subscription and retained events visible

The ViewModel should expose enough bindable state for the controller to keep
buttons disabled when actions cannot run. Prefer reusing existing observable
properties where sufficient; add narrowly scoped read-only properties only if
controller-side expressions would otherwise duplicate business rules.

## Controller And FXML Design

`app-shell.fxml` should add three toolbar buttons:

- `jvmsAddNotificationSubscriptionButton`
- `jvmsStartNotificationsButton`
- `jvmsStopNotificationsButton`

The existing button `jvmsAddMonitoringSubscriptionButton` remains the attribute
subscription action but receives clearer localized text.

`AppShellController` should:

- bind the new button texts through `I18n`
- wire `Add Notification` to a small controller helper that calls
  `JvmBrowserViewModel.addMBeanNotificationSubscription(...)` with default
  values
- wire `Start Notifications` to
  `JvmBrowserViewModel.startSelectedJmxNotifications()`
- wire `Stop Notifications` to
  `JvmBrowserViewModel.stopSelectedJmxNotifications()`
- keep disable bindings tied to existing monitoring availability, selected
  MBean, selected notification subscription, and loading state
- keep errors bound to `jvmsMonitoringErrorLabel`

## Localization

Add English and Simplified Chinese strings for:

- Add Attribute
- Add Notification
- Start Notifications
- Stop Notifications

Chinese text should follow existing spacing rules for product terms and avoid
spaces between Chinese characters and technical abbreviations.

## Testing

Add or update `JvmBrowserViewModelTest` coverage for:

- adding a notification subscription from a selected MBean
- rejecting creation when no usable MBean is selected
- starting selected notifications and appending initial events
- stopping selected notifications and calling the service
- preserving persisted subscriptions and retained events

Add or update `AppShellTest` coverage for:

- FXML contains the new toolbar buttons
- controller wires the new actions to the ViewModel methods or helper
- English and Chinese bundles contain the new keys
- the existing Add Attribute label remains clear

Run targeted tests:

- `./mvnw -pl jmc-fx-ui -Dtest=JvmBrowserViewModelTest test`
- `./mvnw -pl jmc-fx-ui -Dtest=AppShellTest test`

Run project verification required by `AGENTS.md`:

- `sdk env`
- `./mvnw -v`
- `./mvnw verify`
- `rg -n "<modules>|<module>" pom.xml **/pom.xml`
- `rg -n "org.openjdk.jmc|JfrLoaderToolkit" jmc-fx-ui jmc-fx-app jmc-fx-domain`

## Rollout Notes

After implementation, update `docs/roadmap.md` so the Live JVM JMX monitoring
item no longer says the notification UI workflow is incomplete. If the work
only covers the first-phase toolbar workflow and not multi-subscription
management, record that remaining detail explicitly.

## Approval Status

The user approved the Monitoring-tab-first approach on 2026-06-01.
