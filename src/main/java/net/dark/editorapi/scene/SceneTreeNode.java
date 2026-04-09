package net.dark.editorapi.scene;

import java.util.UUID;

public record SceneTreeNode(UUID objectId, UUID childId, SceneObjectType type, String label, String detail, int depth, boolean locked, boolean visible) {
}
