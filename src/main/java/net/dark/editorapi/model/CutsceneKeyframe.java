package net.dark.editorapi.model;

import java.util.UUID;
import net.minecraft.util.math.Vec3d;

public record CutsceneKeyframe(
        UUID id,
        int frame,
        Vec3d position,
        float yaw,
        float pitch,
        InterpolationMode interpolation
) {
    public CutsceneKeyframe withFrame(int newFrame) {
        return new CutsceneKeyframe(this.id, newFrame, this.position, this.yaw, this.pitch, this.interpolation);
    }

    public CutsceneKeyframe withTransform(Vec3d newPosition, float newYaw, float newPitch) {
        return new CutsceneKeyframe(this.id, this.frame, newPosition, newYaw, newPitch, this.interpolation);
    }

    public CutsceneKeyframe withInterpolation(InterpolationMode mode) {
        return new CutsceneKeyframe(this.id, this.frame, this.position, this.yaw, this.pitch, mode);
    }
}
