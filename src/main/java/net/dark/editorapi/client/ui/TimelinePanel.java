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

        int rulerY = contentY + 18;
        int start = cutscene.startFrame();
        int end = Math.max(start + 1, cutscene.endFrame());

        context.drawText(textRenderer, Text.literal("Frame " + this.state.timelineFrame()), contentX, contentY, 0xFFE8EDF8, false);
        context.fill(contentX, rulerY, contentX + contentWidth, rulerY + 48, 0xFF151B23);
        context.drawBorder(contentX, rulerY, contentWidth, 48, 0xFF384355);

        for (int frame = start; frame <= end; frame += 10) {
            int frameX = frameToX(frame, contentX + 6, contentWidth - 12, start, end);
            context.fill(frameX, rulerY, frameX + 1, rulerY + 48, 0xFF2A3340);
            context.drawText(textRenderer, Text.literal(Integer.toString(frame)), frameX - 4, rulerY + 4, 0xFF8FA0BC, false);
        }

        int currentX = frameToX(this.state.timelineFrame(), contentX + 6, contentWidth - 12, start, end);
        context.fill(currentX, rulerY, currentX + 2, rulerY + 48, 0xFF4F7EF7);

        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            int keyX = frameToX(keyframe.frame(), contentX + 6, contentWidth - 12, start, end) - 4;
            boolean selected = this.state.selection().childId() != null && this.state.selection().childId().equals(keyframe.id());
            context.fill(keyX, rulerY + 24, keyX + 8, rulerY + 36, selected ? 0xFFFFCA5A : 0xFFEAEEF7);
            context.drawBorder(keyX, rulerY + 24, 8, 12, 0xFF283242);
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return false;
        }

        int rulerY = contentY + 18;
        int start = cutscene.startFrame();
        int end = Math.max(start + 1, cutscene.endFrame());
        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            int keyX = frameToX(keyframe.frame(), contentX + 6, contentWidth - 12, start, end) - 4;
            if (isInside(mouseX, mouseY, keyX, rulerY + 24, 8, 12)) {
                this.state.setSelection(EditorSelection.keyframe(cutscene.id(), keyframe.id()));
                this.draggingKeyframeId = keyframe.id();
                return true;
            }
        }

        if (isInside(mouseX, mouseY, contentX, rulerY, contentWidth, 48)) {
            this.state.setTimelineFrame(xToFrame((int) mouseX, contentX + 6, contentWidth - 12, start, end));
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int contentX, int contentY, int contentWidth, int contentHeight) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null || this.draggingKeyframeId == null) {
            return false;
        }

        int start = cutscene.startFrame();
        int end = Math.max(start + 1, cutscene.endFrame());
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
        this.draggingKeyframeId = null;
        return false;
    }

    private int frameToX(int frame, int x, int width, int start, int end) {
        double normalized = (frame - start) / (double) Math.max(1, end - start);
        return x + MathHelper.floor(normalized * width);
    }

    private int xToFrame(int x, int startX, int width, int start, int end) {
        double normalized = MathHelper.clamp((x - startX) / (double) Math.max(1, width), 0.0D, 1.0D);
        return start + MathHelper.floor(normalized * (end - start));
    }
}
