package net.dark.editorapi.mixin.client;

import com.google.gson.JsonSyntaxException;
import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.client.EditorapiClient;
import net.dark.editorapi.model.CutsceneDefinition;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    private static final Logger EDITORAPI_LOGGER = LoggerFactory.getLogger("editorapi/experimental-deapth-of-field");
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_BLUR_RADIUS = 4.0F;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_BLUR_MIN_STRENGTH = 0.16F;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_BLUR_MAX_STRENGTH = 0.72F;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_BLUR_STRENGTH = 0.58F;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_FOCUS_DISTANCE = 4.0D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_FOCUS_DISTANCE = 26.0D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FOCUS_MARGIN = 1.75D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_CLOSE_FOCUS_BAND = 2.2D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_FOCUS_BAND = 1.4D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_CLOSE_TRANSITION = 2.8D;
    private static final double EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_TRANSITION = 6.5D;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_SMOOTHING = 0.08F;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_IDLE_SMOOTHING = 0.035F;
    private static final float EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_PLANE = 0.05F;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract void renderBlur(float delta);

    private double editorapi$smoothedFocusDistance = EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_FOCUS_DISTANCE;
    private boolean editorapi$hasFocusSample;
    @Unique
    private PostEffectProcessor editorapi$experimentalDeapthOfFieldProcessor;
    @Unique
    private boolean editorapi$experimentalDeapthOfFieldLoadFailed;

    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void editorapi$overrideFov(Camera camera, float tickProgress, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        CutsceneDefinition.SampledCutsceneFrame sample = EditorapiClient.state().runtime().cutscenes().sampledPreview(EditorapiClient.state().selectedCutscene() == null ? null : EditorapiClient.state().selectedCutscene().id(), false);
        if (sample != null && EditorapiClient.state().runtime().cutscenes().isPlaying()) {
            cir.setReturnValue((double) sample.fov());
        }
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void editorapi$applyExperimentalDeapthOfField(net.minecraft.client.render.RenderTickCounter tickCounter, CallbackInfo ci) {
        if (this.client.world == null || this.client.options.hudHidden) {
            return;
        }

        PostEffectProcessor processor = editorapi$getOrCreateExperimentalDeapthOfFieldProcessor();
        if (processor == null) {
            this.renderBlur(0.18F);
            return;
        }

        editorapi$updateExperimentalDeapthOfFieldUniforms(processor);
        processor.render(tickCounter.getLastFrameDuration());
    }

    @Inject(method = "onResized", at = @At("TAIL"))
    private void editorapi$resizeExperimentalDeapthOfField(int width, int height, CallbackInfo ci) {
        if (this.editorapi$experimentalDeapthOfFieldProcessor != null) {
            this.editorapi$experimentalDeapthOfFieldProcessor.setupDimensions(width, height);
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void editorapi$closeExperimentalDeapthOfField(CallbackInfo ci) {
        if (this.editorapi$experimentalDeapthOfFieldProcessor != null) {
            this.editorapi$experimentalDeapthOfFieldProcessor.close();
            this.editorapi$experimentalDeapthOfFieldProcessor = null;
        }
    }

    @Unique
    private PostEffectProcessor editorapi$getOrCreateExperimentalDeapthOfFieldProcessor() {
        if (this.editorapi$experimentalDeapthOfFieldProcessor != null) {
            return this.editorapi$experimentalDeapthOfFieldProcessor;
        }
        if (this.editorapi$experimentalDeapthOfFieldLoadFailed) {
            return null;
        }

        try {
            PostEffectProcessor processor = new PostEffectProcessor(
                    this.client.getTextureManager(),
                    this.client.getResourceManager(),
                    this.client.getFramebuffer(),
                    EditorConstants.id("shaders/post/deapth_of_field.json")
            );
            processor.setupDimensions(this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight());
            this.editorapi$experimentalDeapthOfFieldProcessor = processor;
            return processor;
        } catch (IOException | JsonSyntaxException exception) {
            this.editorapi$experimentalDeapthOfFieldLoadFailed = true;
            EDITORAPI_LOGGER.warn("Failed to load experimental deapth-of-field shader pipeline, falling back to vanilla blur.", exception);
            return null;
        }
    }

    @Unique
    private void editorapi$updateExperimentalDeapthOfFieldUniforms(PostEffectProcessor processor) {
        float focusDistance = editorapi$computeSmoothedExperimentalDeapthOfFieldFocusDistance();
        float focusAmount = MathHelper.clamp(
                (focusDistance - (float) EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_FOCUS_DISTANCE)
                        / (float) (EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_FOCUS_DISTANCE - EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_FOCUS_DISTANCE),
                0.0F,
                1.0F
        );
        float focusBand = MathHelper.lerp(
                focusAmount,
                (float) EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_CLOSE_FOCUS_BAND,
                (float) EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_FOCUS_BAND
        );
        float transition = MathHelper.lerp(
                focusAmount,
                (float) EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_CLOSE_TRANSITION,
                (float) EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_TRANSITION
        );
        float nearBlurStrength = MathHelper.lerp(
                focusAmount,
                EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_BLUR_MIN_STRENGTH,
                EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_BLUR_MAX_STRENGTH
        );

        processor.setUniforms("Radius", EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_BLUR_RADIUS);
        processor.setUniforms("FocusDistance", focusDistance);
        processor.setUniforms("FocusBand", focusBand);
        processor.setUniforms("Transition", transition);
        processor.setUniforms("NearBlurStrength", nearBlurStrength);
        processor.setUniforms("FarBlurStrength", EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FAR_BLUR_STRENGTH);
        processor.setUniforms("NearPlane", EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_NEAR_PLANE);
        processor.setUniforms("FarPlane", this.client.gameRenderer.getFarPlaneDistance());
    }

    @Unique
    private float editorapi$computeSmoothedExperimentalDeapthOfFieldFocusDistance() {
        Vec3d cameraPos = this.client.gameRenderer.getCamera().getPos();
        double sampledFocusDistance = Double.MAX_VALUE;

        Entity targetedEntity = this.client.targetedEntity;
        if (targetedEntity != null) {
            sampledFocusDistance = cameraPos.distanceTo(targetedEntity.getBoundingBox().getCenter());
        }

        HitResult crosshairTarget = this.client.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() != HitResult.Type.MISS) {
            sampledFocusDistance = Math.min(sampledFocusDistance, cameraPos.distanceTo(crosshairTarget.getPos()));
        }

        if (sampledFocusDistance == Double.MAX_VALUE) {
            this.editorapi$smoothedFocusDistance = MathHelper.lerp(
                    EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_IDLE_SMOOTHING,
                    this.editorapi$smoothedFocusDistance,
                    this.client.gameRenderer.getFarPlaneDistance() * 0.35D
            );
            return (float) this.editorapi$smoothedFocusDistance;
        }

        if (!this.editorapi$hasFocusSample) {
            this.editorapi$smoothedFocusDistance = sampledFocusDistance;
            this.editorapi$hasFocusSample = true;
        } else {
            double delta = sampledFocusDistance - this.editorapi$smoothedFocusDistance;
            if (Math.abs(delta) > EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FOCUS_MARGIN) {
                double adjustedTarget = sampledFocusDistance - Math.copySign(EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_FOCUS_MARGIN, delta);
                this.editorapi$smoothedFocusDistance = MathHelper.lerp(
                        EDITORAPI_EXPERIMENTAL_DEAPTH_OF_FIELD_SMOOTHING,
                        this.editorapi$smoothedFocusDistance,
                        adjustedTarget
                );
            }
        }
        return (float) this.editorapi$smoothedFocusDistance;
    }
}
