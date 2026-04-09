package net.dark.editorapi.mixin.client;

import net.dark.editorapi.client.EditorapiClient;
import net.dark.editorapi.model.CutsceneDefinition;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "update", at = @At("TAIL"))
    private void editorapi$applyCutsceneCamera(net.minecraft.world.BlockView area, net.minecraft.entity.Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CutsceneDefinition.SampledCutsceneFrame sample = EditorapiClient.state().runtime().cutscenes().sampledPreview(EditorapiClient.state().selectedCutscene() == null ? null : EditorapiClient.state().selectedCutscene().id(), false);
        if (sample == null || !EditorapiClient.state().runtime().cutscenes().isPlaying()) {
            return;
        }

        CameraAccessor accessor = (CameraAccessor) this;
        accessor.editorapi$setPos(sample.position());
        float sway = sample.sway();
        float time = (float) EditorapiClient.state().runtime().cutscenes().previewFrame() * 0.1F;
        accessor.editorapi$setRotation(sample.yaw() + (float) Math.sin(time) * sway, sample.pitch() + (float) Math.cos(time * 1.2F) * sway * 0.5F);
        accessor.editorapi$getRotation().rotateZ((float) Math.toRadians(sample.roll()));
    }
}
