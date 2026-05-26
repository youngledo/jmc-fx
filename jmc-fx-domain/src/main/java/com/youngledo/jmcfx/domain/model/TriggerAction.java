package com.youngledo.jmcfx.domain.model;

import java.util.List;
import java.util.Objects;

public record TriggerAction(TriggerActionType type, String commandName, List<String> arguments) {

    public TriggerAction {
        type = Objects.requireNonNull(type, "type");
        commandName = Objects.requireNonNullElse(commandName, "");
        arguments = List.copyOf(Objects.requireNonNullElse(arguments, List.of()));
    }

    public static TriggerAction notifyOnly() {
        return new TriggerAction(TriggerActionType.NOTIFY, "", List.of());
    }

    public static TriggerAction diagnosticCommand(String commandName, List<String> arguments) {
        return new TriggerAction(TriggerActionType.DIAGNOSTIC_COMMAND, commandName, arguments);
    }
}
