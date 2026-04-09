package net.dark.editorapi.scene;

import java.util.List;
import java.util.UUID;

public interface SceneObject {
    UUID id();

    String name();

    SceneObjectType type();

    boolean visible();

    boolean locked();

    default List<SceneObject> children() {
        return List.of();
    }
}
