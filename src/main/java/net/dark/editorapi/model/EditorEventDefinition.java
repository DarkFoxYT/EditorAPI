package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class EditorEventDefinition {
    private final UUID id;
    private String name;
    private final List<EditorActionInstance> actions;

    public EditorEventDefinition(UUID id, String name, List<EditorActionInstance> actions) {
        this.id = id;
        this.name = name;
        this.actions = new ArrayList<>(actions);
    }

    public UUID id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EditorActionInstance> actions() {
        return this.actions;
    }
}
