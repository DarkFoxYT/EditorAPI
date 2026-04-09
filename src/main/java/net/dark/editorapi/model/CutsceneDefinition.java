package net.dark.editorapi.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class CutsceneDefinition {
    private final UUID id;
    private String name;
    private int startFrame;
    private int endFrame;
    private boolean loop;
    private boolean showPreview;
    private boolean autoKeyframe;
    private final List<CutsceneKeyframe> keyframes;

    public CutsceneDefinition(UUID id, String name, int startFrame, int endFrame, boolean loop, boolean showPreview, boolean autoKeyframe, List<CutsceneKeyframe> keyframes) {
        this.id = id;
        this.name = name;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
        this.loop = loop;
        this.showPreview = showPreview;
        this.autoKeyframe = autoKeyframe;
        this.keyframes = new ArrayList<>(keyframes);
        sortKeyframes();
    }

    public static CutsceneDefinition createDefault(String name) {
        return new CutsceneDefinition(UUID.randomUUID(), name, 0, 120, false, true, false, List.of());
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

    public List<CutsceneKeyframe> keyframes() {
        return this.keyframes;
    }

    public void sortKeyframes() {
        this.keyframes.sort(Comparator.comparingInt(CutsceneKeyframe::frame));
    }

    public SampledCutsceneFrame sample(double frame) {
        if (this.keyframes.isEmpty()) {
            return new SampledCutsceneFrame(Vec3d.ZERO, 0.0F, 0.0F);
        }
        if (this.keyframes.size() == 1) {
            CutsceneKeyframe only = this.keyframes.getFirst();
            return new SampledCutsceneFrame(only.position(), only.yaw(), only.pitch());
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
            return new SampledCutsceneFrame(previous.position(), previous.yaw(), previous.pitch());
        }

        double localFrame = MathHelper.clamp((frame - previous.frame()) / (double) (next.frame() - previous.frame()), 0.0D, 1.0D);
        double curve = next.interpolation().apply(localFrame);
        Vec3d sampled = previous.position().lerp(next.position(), curve);
        float yaw = MathHelper.lerp((float) curve, previous.yaw(), next.yaw());
        float pitch = MathHelper.lerp((float) curve, previous.pitch(), next.pitch());
        return new SampledCutsceneFrame(sampled, yaw, pitch);
    }

    public record SampledCutsceneFrame(Vec3d position, float yaw, float pitch) {
    }
}
