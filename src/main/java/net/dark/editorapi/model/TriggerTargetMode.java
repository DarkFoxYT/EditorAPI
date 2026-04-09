package net.dark.editorapi.model;

public enum TriggerTargetMode {
    TRIGGERING_PLAYER,
    ALL_PLAYERS,
    PLAYERS_IN_RADIUS;

    public TriggerTargetMode next() {
        TriggerTargetMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
