package net.dark.editorapi.scene;

import java.util.ArrayList;
import java.util.List;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.EditorProject;
import net.dark.editorapi.model.TriggerZone;

public final class EditorSceneIndex {
    private final EditorProject project;

    public EditorSceneIndex(EditorProject project) {
        this.project = project;
    }

    public List<SceneTreeNode> buildTree() {
        List<SceneTreeNode> nodes = new ArrayList<>();
        for (TriggerZone zone : this.project.zones().values()) {
            nodes.add(new SceneTreeNode(zone.id(), null, SceneObjectType.TRIGGER_ZONE, zone.name(), "Zone", 0, zone.locked(), zone.visible()));
        }
        for (EditorEventDefinition event : this.project.events().values()) {
            nodes.add(new SceneTreeNode(event.id(), null, SceneObjectType.EVENT, event.name(), event.actions().size() + " actions", 0, event.locked(), event.visible()));
        }
        for (CutsceneDefinition cutscene : this.project.cutscenes().values()) {
            nodes.add(new SceneTreeNode(cutscene.id(), null, SceneObjectType.CUTSCENE, cutscene.name(), cutscene.keyframes().size() + " keys", 0, cutscene.locked(), cutscene.visible()));
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                nodes.add(new SceneTreeNode(cutscene.id(), keyframe.id(), SceneObjectType.CAMERA_KEYFRAME, "Frame " + keyframe.frame(), keyframe.interpolation().label(), 1, false, true));
            }
        }
        return nodes;
    }
}
