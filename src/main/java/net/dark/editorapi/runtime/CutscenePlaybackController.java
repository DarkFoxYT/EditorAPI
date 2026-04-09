package net.dark.editorapi.runtime;

import java.util.UUID;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.EditorProject;

public final class CutscenePlaybackController {
    private final EditorProject project;
    private UUID activeCutsceneId;
    private double currentFrame;
    private boolean playing;
    private double previewFrame;

    public CutscenePlaybackController(EditorProject project) {
        this.project = project;
    }

    public void start(String rawId) {
        try {
            this.activeCutsceneId = UUID.fromString(rawId);
        } catch (IllegalArgumentException exception) {
            return;
        }

        CutsceneDefinition cutscene = getActiveCutscene();
        if (cutscene == null) {
            this.activeCutsceneId = null;
            return;
        }

        this.currentFrame = cutscene.startFrame();
        this.previewFrame = this.currentFrame;
        this.playing = true;
    }

    public void stop() {
        this.playing = false;
        this.activeCutsceneId = null;
    }

    public void tick() {
        if (!this.playing) {
            return;
        }

        CutsceneDefinition cutscene = getActiveCutscene();
        if (cutscene == null) {
            stop();
            return;
        }

        this.currentFrame += 1.0D;
        if (this.currentFrame > cutscene.endFrame()) {
            if (cutscene.loop()) {
                this.currentFrame = cutscene.startFrame();
            } else {
                stop();
            }
        }
        this.previewFrame = this.currentFrame;
    }

    public void setPreviewFrame(double previewFrame) {
        this.previewFrame = previewFrame;
    }

    public double previewFrame() {
        return this.previewFrame;
    }

    public boolean isPlaying() {
        return this.playing;
    }

    public UUID activeCutsceneId() {
        return this.activeCutsceneId;
    }

    public CutsceneDefinition getActiveCutscene() {
        return this.activeCutsceneId == null ? null : this.project.cutscenes().get(this.activeCutsceneId);
    }

    public CutsceneDefinition.SampledCutsceneFrame sampledPreview(UUID previewCutsceneId, boolean previewEnabled) {
        CutsceneDefinition cutscene = this.playing ? getActiveCutscene() : previewEnabled && previewCutsceneId != null ? this.project.cutscenes().get(previewCutsceneId) : null;
        if (cutscene == null || cutscene.keyframes().isEmpty()) {
            return null;
        }

        double frame = this.playing ? this.currentFrame : this.previewFrame;
        return cutscene.sample(frame);
    }
}
