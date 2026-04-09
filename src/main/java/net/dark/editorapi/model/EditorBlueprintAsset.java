package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class EditorBlueprintAsset {
    private final UUID id;
    private String name;
    private final List<TriggerZone> zones;
    private final List<EditorEventDefinition> events;
    private final List<CutsceneDefinition> cutscenes;

    public EditorBlueprintAsset(UUID id, String name, List<TriggerZone> zones, List<EditorEventDefinition> events, List<CutsceneDefinition> cutscenes) {
        this.id = id;
        this.name = name;
        this.zones = new ArrayList<>(zones);
        this.events = new ArrayList<>(events);
        this.cutscenes = new ArrayList<>(cutscenes);
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

    public List<TriggerZone> zones() {
        return this.zones;
    }

    public List<EditorEventDefinition> events() {
        return this.events;
    }

    public List<CutsceneDefinition> cutscenes() {
        return this.cutscenes;
    }

    public Box previewBounds() {
        Box bounds = null;
        for (TriggerZone zone : this.zones) {
            bounds = bounds == null ? zone.box() : bounds.union(zone.box());
        }
        for (CutsceneDefinition cutscene : this.cutscenes) {
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                Box keyBox = new Box(keyframe.position(), keyframe.position()).expand(0.35D);
                bounds = bounds == null ? keyBox : bounds.union(keyBox);
            }
        }
        if (bounds == null) {
            bounds = new Box(Vec3d.ZERO, Vec3d.ZERO).expand(0.5D);
        }
        return bounds;
    }
}
