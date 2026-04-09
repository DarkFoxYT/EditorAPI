package net.dark.editorapi.mixin.client;

import net.dark.editorapi.client.EditorapiClient;
import net.dark.editorapi.model.CutsceneDefinition;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void editorapi$overrideFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        CutsceneDefinition.SampledCutsceneFrame sample = EditorapiClient.state().runtime().cutscenes().sampledPreview(EditorapiClient.state().selectedCutscene() == null ? null : EditorapiClient.state().selectedCutscene().id(), false);
        if (sample != null && EditorapiClient.state().runtime().cutscenes().isPlaying()) {
            cir.setReturnValue((double) sample.fov());
        }
    }
}
