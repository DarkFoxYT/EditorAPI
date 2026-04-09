package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.dark.editorapi.scene.SceneObject;
import net.dark.editorapi.scene.SceneObjectType;

public final class EditorEventDefinition implements SceneObject {
    private final UUID id;
    private String name;
    private final List<EditorActionInstance> actions;
    private boolean visible;
    private boolean locked;

    public EditorEventDefinition(UUID id, String name, List<EditorActionInstance> actions) {
        this(id, name, actions, true, false);
    }

    public EditorEventDefinition(UUID id, String name, List<EditorActionInstance> actions, boolean visible, boolean locked) {
        this.id = id;
        this.name = name;
        this.actions = new ArrayList<>(actions);
        this.visible = visible;
        this.locked = locked;
    }

    public UUID id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    @Override
    public SceneObjectType type() {
        return SceneObjectType.EVENT;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EditorActionInstance> actions() {
        return this.actions;
    }

    @Override
    public boolean visible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean locked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
