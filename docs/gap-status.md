# JMC Gap Status

This document records deliberate gap decisions so future JMC alignment reviews
do not repeatedly report deferred or out-of-scope items as current core gaps.

## Gap 5: Optional Integrations And Eclipse Ecosystem

Status as of 2026-05-30:

- P1 Jolokia remote connection support: implemented.
  `service:jmx:jolokia://host:port/path` URLs are supported through the
  existing JVM Browser manual and saved-target connection workflow.
- Jolokia multicast or discovery: deferred.
  Manual and saved URLs are the supported workflow for now.
- Credentials storage and TLS trust-store UI for Jolokia endpoints: deferred.
  Add only when the product has a broader secure remote-connection design.
- WebSocket integration: deferred.
  Do not count this as a missing core feature in Gap 5 comparisons.
- JConsole plug-in compatibility: deferred pending a product decision.
  Do not assume Eclipse or JConsole plug-in parity is required for JMC FX.
- Eclipse IDE, PDE, RCP product, and update-site integration: out of scope.
  JMC FX is a standalone JavaFX application, not an Eclipse RCP product.

Future gap comparisons should treat P1 Jolokia support as handled and should
separate the deferred/out-of-scope optional integrations from active core
workflow gaps.
