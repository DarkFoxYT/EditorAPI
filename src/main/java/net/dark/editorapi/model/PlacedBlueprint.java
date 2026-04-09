package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.dark.editorapi.scene.SceneObject;
import net.dark.editorapi.scene.SceneObjectType;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class PlacedBlueprint implements SceneObject {
    private final UUID id;
    private UUID assetId;
    private String name;
    private Vec3d origin;
    private float yaw;
    private boolean visible;
    private boolean locked;
    private final List<UUID> zoneIds;
    private final List<UUID> eventIds;
    private final List<UUID> cutsceneIds;

    public PlacedBlueprint(
            UUID id,
            UUID assetId,
            String name,
            Vec3d origin,
            float yaw,
            boolean visible,
            boolean locked,
            List<UUID> zoneIds,
            List<UUID> eventIds,
            List<UUID> cutsceneIds
    ) {
        this.id = id;
        this.assetId = assetId;
        this.name = name;
        this.origin = origin;
        this.yaw = yaw;
        this.visible = visible;
        this.locked = locked;
        this.zoneIds = new ArrayList<>(zoneIds);
        this.eventIds = new ArrayList<>(eventIds);
        this.cutsceneIds = new ArrayList<>(cutsceneIds);
    }

    @Override
    public UUID id() {
        return this.id;
    }

    public UUID assetId() {
        return this.assetId;
    }

    public void setAssetId(UUID assetId) {
        this.assetId = assetId;
    }

    @Override
    public String name() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Vec3d origin() {
        return this.origin;
    }

    public void setOrigin(Vec3d origin) {
        this.origin = origin;
    }

    public float yaw() {
        return this.yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    @Override
    public SceneObjectType type() {
        return SceneObjectType.BLUEPRINT;
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

    public List<UUID> zoneIds() {
        return this.zoneIds;
    }

    public List<UUID> eventIds() {
        return this.eventIds;
    }

    public List<UUID> cutsceneIds() {
        return this.cutsceneIds;
    }

    public Box bounds(EditorProject project) {
        Box bounds = null;
        for (UUID zoneId : this.zoneIds) {
            TriggerZone zone = project.zones().get(zoneId);
            if (zone != null) {
                bounds = bounds == null ? zone.box() : bounds.union(zone.box());
            }
        }
        for (UUID cutsceneId : this.cutsceneIds) {
            CutsceneDefinition cutscene = project.cutscenes().get(cutsceneId);
            if (cutscene == null) {
                continue;
            }
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                Box keyBox = new Box(keyframe.position(), keyframe.position()).expand(0.35D);
                bounds = bounds == null ? keyBox : bounds.union(keyBox);
            }
        }
        return bounds == null ? new Box(this.origin, this.origin).expand(0.5D) : bounds.expand(0.15D);
    }
}
