package net.dark.editorapi.client.ui;

import java.util.UUID;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelection;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class TimelinePanel extends EditorPanel {
    private final EditorClientState state;
    private UUID draggingKeyframeId;
    private boolean draggingPlayhead;
    private boolean draggingScrollbar;
    private int visibleStartFrame;
    private int visibleFrameSpan = 120;

    public TimelinePanel(EditorClientState state, int x, int y, int width, int height) {
        super("Timeline", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            context.drawText(textRenderer, Text.literal("Select a cutscene to edit keyframes."), contentX, contentY, 0xFF9CADC8, false);
            return;
        }
        clampVisibleRange(cutscene);

        int controlsY = contentY;
        int rulerY = contentY + 24;
        int start = this.visibleStartFrame;
        int end = Math.max(start + 1, start + this.visibleFrameSpan);

        drawButton(context, textRenderer, contentX, controlsY, 36, 18, this.state.runtime().cutscenes().isPlaying() ? "Stop" : "Play", isInside(mouseX, mouseY, contentX, controlsY, 36, 18), this.state.runtime().cutscenes().isPlaying());
        drawButton(context, textRenderer, contentX + 42, controlsY, 30, 18, "<<", isInside(mouseX, mouseY, contentX + 42, controlsY, 30, 18), false);
        drawButton(context, textRenderer, contentX + 78, controlsY, 30, 18, ">>", isInside(mouseX, mouseY, contentX + 78, controlsY, 30, 18), false);
        drawButton(context, textRenderer, contentX + 114, controlsY, 24, 18, "-", isInside(mouseX, mouseY, contentX + 114, controlsY, 24, 18), false);
        drawButton(context, textRenderer, contentX + 142, controlsY, 24, 18, "+", isInside(mouseX, mouseY, contentX + 142, controlsY, 24, 18), false);
        context.drawText(textRenderer, Text.literal("Frame " + this.state.timelineFrame() + "  |  " + start + "-" + end + "  |  Space play"), contentX + 176, controlsY + 5, 0xFFE8EDF8, false);

        context.fill(contentX, rulerY, contentX + contentWidth, rulerY + 70, 0xFF151B23);
        context.drawBorder(contentX, rulerY, contentWidth, 70, 0xFF384355);

        for (int frame = start; frame <= end; frame += 10) {
            int frameX = frameToX(frame, contentX + 6, contentWidth - 12, start, end);
            context.fill(frameX, rulerY, frameX + 1, rulerY + 56, 0xFF2A3340);
            context.drawText(textRenderer, Text.literal(Integer.toString(frame)), frameX - 4, rulerY + 4, 0xFF8FA0BC, false);
        }

        int currentX = frameToX(this.state.timelineFrame(), contentX + 6, contentWidth - 12, start, end);
        String currentLabel = Integer.toString(this.state.timelineFrame());
        int labelWidth = textRenderer.getWidth(currentLabel) + 8;
        context.fill(currentX - labelWidth / 2, rulerY + 2, currentX + labelWidth / 2, rulerY + 14, 0xFF4F7EF7);
        context.drawBorder(currentX - labelWidth / 2, rulerY + 2, labelWidth, 12, 0xFF9EC0FF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(currentLabel), currentX, rulerY + 4, 0xFFFFFFFF);
        context.fill(currentX, rulerY + 14, currentX + 2, rulerY + 56, 0xFF4F7EF7);

        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            if (keyframe.frame() < start || keyframe.frame() > end) {
                continue;
            }
            int keyX = frameToX(keyframe.frame(), contentX + 6, contentWidth - 12, start, end) - 4;
            boolean selected = this.state.selection().childId() != null && this.state.selection().childId().equals(keyframe.id());
            context.fill(keyX, rulerY + 30, keyX + 8, rulerY + 44, selected ? 0xFFFFCA5A : 0xFFEAEEF7);
            context.drawBorder(keyX, rulerY + 30, 8, 14, 0xFF283242);
        }

        renderScrollbar(context, contentX + 6, rulerY + 60, contentWidth - 12, cutscene.startFrame(), Math.max(cutscene.endFrame(), start + 1));
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return false;
        }
        clampVisibleRange(cutscene);

        int rulerY = contentY + 24;
        int start = this.visibleStartFrame;
        int end = Math.max(start + 1, start + this.visibleFrameSpan);

        if (isInside(mouseX, mouseY, contentX, contentY, 36, 18)) {
            togglePlayback(cutscene);
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + 42, contentY, 30, 18)) {
            this.state.setTimelineFrame(Math.max(cutscene.startFrame(), this.state.timelineFrame() - 10));
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + 78, contentY, 30, 18)) {
            this.state.setTimelineFrame(Math.min(cutscene.endFrame(), this.state.timelineFrame() + 10));
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + 114, contentY, 24, 18)) {
            this.visibleFrameSpan = Math.min(400, this.visibleFrameSpan + 20);
            clampVisibleRange(cutscene);
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + 142, contentY, 24, 18)) {
            this.visibleFrameSpan = Math.max(30, this.visibleFrameSpan - 20);
            clampVisibleRange(cutscene);
            return true;
        }

        int currentX = frameToX(this.state.timelineFrame(), contentX + 6, contentWidth - 12, start, end);
        if (isInside(mouseX, mouseY, currentX - 14, rulerY + 2, 28, 54)) {
            this.draggingPlayhead = true;
            this.state.setTimelineFrame(xToFrame((int) mouseX, contentX + 6, contentWidth - 12, start, end));
            return true;
        }

        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            if (keyframe.frame() < start || keyframe.frame() > end) {
                continue;
            }
            int keyX = frameToX(keyframe.frame(), contentX + 6, contentWidth - 12, start, end) - 4;
            if (isInside(mouseX, mouseY, keyX, rulerY + 30, 8, 14)) {
                this.state.setSelection(EditorSelection.keyframe(cutscene.id(), keyframe.id()));
                this.draggingKeyframeId = keyframe.id();
                return true;
            }
        }

        if (isInside(mouseX, mouseY, contentX + 6, rulerY + 60, contentWidth - 12, 8)) {
            this.draggingScrollbar = true;
            updateScrollbarPosition((int) mouseX, contentX + 6, contentWidth - 12, cutscene.startFrame(), Math.max(cutscene.endFrame(), this.visibleFrameSpan));
            return true;
        }

        if (isInside(mouseX, mouseY, contentX, rulerY, contentWidth, 56)) {
            this.draggingPlayhead = true;
            this.state.setTimelineFrame(xToFrame((int) mouseX, contentX + 6, contentWidth - 12, start, end));
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int contentX, int contentY, int contentWidth, int contentHeight) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return false;
        }
        clampVisibleRange(cutscene);

        int start = this.visibleStartFrame;
        int end = Math.max(start + 1, start + this.visibleFrameSpan);

        if (this.draggingScrollbar) {
            updateScrollbarPosition((int) mouseX, contentX + 6, contentWidth - 12, cutscene.startFrame(), Math.max(cutscene.endFrame(), this.visibleFrameSpan));
            return true;
        }

        if (this.draggingPlayhead) {
            this.state.setTimelineFrame(xToFrame((int) mouseX, contentX + 6, contentWidth - 12, start, end));
            return true;
        }

        if (this.draggingKeyframeId == null) {
            return false;
        }

        int frame = xToFrame((int) mouseX, contentX + 6, contentWidth - 12, start, end);
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
            if (keyframe.id().equals(this.draggingKeyframeId)) {
                cutscene.keyframes().set(index, keyframe.withFrame(frame));
                cutscene.sortKeyframes();
                this.state.setTimelineFrame(frame);
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        boolean handled = this.draggingKeyframeId != null || this.draggingPlayhead || this.draggingScrollbar;
        this.draggingKeyframeId = null;
        this.draggingPlayhead = false;
        this.draggingScrollbar = false;
        return handled;
    }

    public boolean handleSpacebar() {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return false;
        }
        togglePlayback(cutscene);
        return true;
    }

    public boolean handleDelete() {
        if (this.state.selection().type() != net.dark.editorapi.client.state.EditorSelectionType.KEYFRAME) {
            return false;
        }
        this.state.deleteSelectedKeyframeInTimeline();
        return true;
    }

    private void togglePlayback(CutsceneDefinition cutscene) {
        if (this.state.runtime().cutscenes().isPlaying()) {
            this.state.runtime().cutscenes().stop();
        } else {
            this.state.runtime().cutscenes().start(cutscene.id().toString());
        }
    }

    private int frameToX(int frame, int x, int width, int start, int end) {
        double normalized = (frame - start) / (double) Math.max(1, end - start);
        return x + MathHelper.floor(normalized * width);
    }

    private int xToFrame(int x, int startX, int width, int start, int end) {
        double normalized = MathHelper.clamp((x - startX) / (double) Math.max(1, width), 0.0D, 1.0D);
        return start + MathHelper.floor(normalized * (end - start));
    }

    private void renderScrollbar(DrawContext context, int x, int y, int width, int minFrame, int maxFrame) {
        context.fill(x, y, x + width, y + 8, 0xFF202A38);
        context.drawBorder(x, y, width, 8, 0xFF384355);
        int totalSpan = Math.max(this.visibleFrameSpan + 1, maxFrame - minFrame + 1);
        int thumbWidth = Math.max(24, MathHelper.floor(width * (this.visibleFrameSpan / (float) totalSpan)));
        int range = Math.max(0, totalSpan - this.visibleFrameSpan);
        int thumbX = range == 0 ? x : x + MathHelper.floor((width - thumbWidth) * ((this.visibleStartFrame - minFrame) / (float) range));
        context.fill(thumbX, y + 1, thumbX + thumbWidth, y + 7, 0xFF7C8DA8);
    }

    private void updateScrollbarPosition(int mouseX, int x, int width, int minFrame, int maxFrame) {
        int totalSpan = Math.max(this.visibleFrameSpan + 1, maxFrame - minFrame + 1);
        int range = Math.max(0, totalSpan - this.visibleFrameSpan);
        if (range == 0) {
            this.visibleStartFrame = minFrame;
            return;
        }
        double normalized = MathHelper.clamp((mouseX - x) / (double) Math.max(1, width), 0.0D, 1.0D);
        this.visibleStartFrame = minFrame + MathHelper.floor(range * normalized);
    }

    private void clampVisibleRange(CutsceneDefinition cutscene) {
        int min = cutscene.startFrame();
        int max = Math.max(min + this.visibleFrameSpan, cutscene.endFrame());
        this.visibleStartFrame = MathHelper.clamp(this.visibleStartFrame, min, Math.max(min, max - this.visibleFrameSpan));
    }
}
