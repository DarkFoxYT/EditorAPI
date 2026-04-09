package net.dark.editorapi.model;

public enum TriggerOnceMode {
    NONE,
    ONCE_PER_PLAYER,
    ONCE_GLOBAL;

    public TriggerOnceMode next() {
        TriggerOnceMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
