package net.dark.editorapi.client.state;

import java.util.UUID;

public record EditorSelection(EditorSelectionType type, UUID objectId, UUID childId) {
    public static final EditorSelection NONE = new EditorSelection(EditorSelectionType.NONE, null, null);

    public static EditorSelection zone(UUID id) {
        return new EditorSelection(EditorSelectionType.ZONE, id, null);
    }

    public static EditorSelection event(UUID id) {
        return new EditorSelection(EditorSelectionType.EVENT, id, null);
    }

    public static EditorSelection cutscene(UUID id) {
        return new EditorSelection(EditorSelectionType.CUTSCENE, id, null);
    }

    public static EditorSelection keyframe(UUID cutsceneId, UUID keyframeId) {
        return new EditorSelection(EditorSelectionType.KEYFRAME, cutsceneId, keyframeId);
    }

    public static EditorSelection pos1(UUID zoneId) {
        return new EditorSelection(EditorSelectionType.POS1, zoneId, null);
    }

    public static EditorSelection pos2(UUID zoneId) {
        return new EditorSelection(EditorSelectionType.POS2, zoneId, null);
    }
}
