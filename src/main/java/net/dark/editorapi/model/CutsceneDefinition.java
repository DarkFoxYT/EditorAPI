package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.dark.editorapi.scene.SceneObject;
import net.dark.editorapi.scene.SceneObjectType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class CutsceneDefinition implements SceneObject {
    private final UUID id;
    private String name;
    private int startFrame;
    private int endFrame;
    private boolean loop;
    private boolean showPreview;
    private boolean autoKeyframe;
    private boolean visible;
    private boolean locked;
    private final List<CutsceneKeyframe> keyframes;

    public CutsceneDefinition(UUID id, String name, int startFrame, int endFrame, boolean loop, boolean showPreview, boolean autoKeyframe, boolean visible, boolean locked, List<CutsceneKeyframe> keyframes) {
        this.id = id;
        this.name = name;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.loop = loop;
        this.showPreview = showPreview;
        this.autoKeyframe = autoKeyframe;
        this.visible = visible;
        this.locked = locked;
        this.keyframes = new ArrayList<>(keyframes);
        sortKeyframes();
    }

    public static CutsceneDefinition createDefault(String name) {
        return new CutsceneDefinition(UUID.randomUUID(), name, 0, 120, false, true, false, true, false, List.of());
    }

    public UUID id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    @Override
    public SceneObjectType type() {
        return SceneObjectType.CUTSCENE;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int startFrame() {
        return this.startFrame;
    }

    public void setStartFrame(int startFrame) {
        this.startFrame = Math.max(0, startFrame);
    }

    public int endFrame() {
        return this.endFrame;
    }

    public void setEndFrame(int endFrame) {
        this.endFrame = Math.max(this.startFrame + 1, endFrame);
    }

    public boolean loop() {
        return this.loop;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public boolean showPreview() {
        return this.showPreview;
    }

    public void setShowPreview(boolean showPreview) {
        this.showPreview = showPreview;
    }

    public boolean autoKeyframe() {
        return this.autoKeyframe;
    }

    public void setAutoKeyframe(boolean autoKeyframe) {
        this.autoKeyframe = autoKeyframe;
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

    public List<CutsceneKeyframe> keyframes() {
        return this.keyframes;
    }

    public void sortKeyframes() {
        this.keyframes.sort(Comparator.comparingInt(CutsceneKeyframe::frame));
    }

    public SampledCutsceneFrame sample(double frame) {
        if (this.keyframes.isEmpty()) {
            return new SampledCutsceneFrame(Vec3d.ZERO, 0.0F, 0.0F, 0.0F, 70.0F, 0.0F);
        }
        if (this.keyframes.size() == 1) {
            CutsceneKeyframe only = this.keyframes.getFirst();
            return new SampledCutsceneFrame(only.position(), only.yaw(), only.pitch(), only.roll(), only.fov(), only.sway());
        }

        sortKeyframes();
        CutsceneKeyframe previous = this.keyframes.getFirst();
        CutsceneKeyframe next = this.keyframes.getLast();

        for (CutsceneKeyframe keyframe : this.keyframes) {
            if (keyframe.frame() <= frame) {
                previous = keyframe;
            }
            if (keyframe.frame() >= frame) {
                next = keyframe;
                break;
            }
        }

        if (previous == next || next.frame() == previous.frame()) {
            return new SampledCutsceneFrame(previous.position(), previous.yaw(), previous.pitch(), previous.roll(), previous.fov(), previous.sway());
        }

        double localFrame = MathHelper.clamp((frame - previous.frame()) / (double) (next.frame() - previous.frame()), 0.0D, 1.0D);
        double curve = next.interpolation().apply(localFrame);
        Vec3d sampled = previous.position().lerp(next.position(), curve);
        float yaw = MathHelper.lerp((float) curve, previous.yaw(), next.yaw());
        float pitch = MathHelper.lerp((float) curve, previous.pitch(), next.pitch());
        float roll = MathHelper.lerp((float) curve, previous.roll(), next.roll());
        float fov = MathHelper.lerp((float) curve, previous.fov(), next.fov());
        float sway = MathHelper.lerp((float) curve, previous.sway(), next.sway());
        return new SampledCutsceneFrame(sampled, yaw, pitch, roll, fov, sway);
    }

    public record SampledCutsceneFrame(Vec3d position, float yaw, float pitch, float roll, float fov, float sway) {
    }
}
