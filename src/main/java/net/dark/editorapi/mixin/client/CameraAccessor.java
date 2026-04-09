package net.dark.editorapi.mixin.client;

import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPos")
    void editorapi$setPos(net.minecraft.util.math.Vec3d pos);

    @Invoker("setRotation")
    void editorapi$setRotation(float yaw, float pitch);

    @Accessor("rotation")
    Quaternionf editorapi$getRotation();
}
