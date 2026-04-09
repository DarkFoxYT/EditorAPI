package net.dark.editorapi.client.ui;

import java.util.List;
import net.dark.editorapi.api.action.EditorActionDefinition;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public final class InspectorPanel extends EditorPanel {
    private final EditorClientState state;

    public InspectorPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Inspector", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int y = contentY;
        switch (this.state.selection().type()) {
            case ZONE, POS1, POS2 -> {
                TriggerZone zone = this.state.selectedZone();
                if (zone == null) {
                    return;
                }
                y = drawValue(context, textRenderer, contentX, y, "Name", zone.name());
                y = drawValue(context, textRenderer, contentX, y, "Pos1", formatPos(zone.pos1()));
                y = drawValue(context, textRenderer, contentX, y, "Pos2", formatPos(zone.pos2()));
                y = drawValue(context, textRenderer, contentX, y, "Mode", zone.onceMode().name() + " / " + zone.targetMode().name());
                y = drawToggle(context, textRenderer, contentX, y, "Fill", zone.showFill());
                y = drawToggle(context, textRenderer, contentX, y, "Enter", zone.triggerEnter());
                y = drawToggle(context, textRenderer, contentX, y, "Exit", zone.triggerExit());
                y = drawToggle(context, textRenderer, contentX, y, "Inside", zone.triggerWhileInside());
                y = drawToggle(context, textRenderer, contentX, y, "Time", zone.triggerTimeInside());
                y = drawValue(context, textRenderer, contentX, y, "Delay", zone.delayTicks() + " ticks");
                drawHint(context, textRenderer, contentX, y + 4, "Click rows to cycle/toggle");
            }
            case EVENT -> {
                EditorEventDefinition event = this.state.selectedEvent();
                if (event == null) {
                    return;
                }
                y = drawValue(context, textRenderer, contentX, y, "Event", event.name());
                for (EditorActionInstance action : event.actions()) {
                    EditorActionDefinition definition = EditorActionRegistry.getInstance().get(action.actionId());
                    y = drawValue(context, textRenderer, contentX, y, definition == null ? action.actionId().toString() : definition.displayName(), summarize(definition, action));
                }
            }
            case CUTSCENE, KEYFRAME -> {
                CutsceneDefinition cutscene = this.state.selectedCutscene();
                if (cutscene == null) {
                    return;
                }
                y = drawValue(context, textRenderer, contentX, y, "Cutscene", cutscene.name());
                y = drawValue(context, textRenderer, contentX, y, "Frames", cutscene.startFrame() + " -> " + cutscene.endFrame());
                y = drawToggle(context, textRenderer, contentX, y, "Loop", cutscene.loop());
                y = drawToggle(context, textRenderer, contentX, y, "Preview", cutscene.showPreview());
                y = drawToggle(context, textRenderer, contentX, y, "Auto Key", cutscene.autoKeyframe());
                if (this.state.selection().type() == EditorSelectionType.KEYFRAME) {
                    CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                    if (keyframe != null) {
                        y = drawValue(context, textRenderer, contentX, y, "Key", keyframe.frame() + " / " + keyframe.interpolation().name());
                        y = drawValue(context, textRenderer, contentX, y, "Pos", formatVec(keyframe.position().x, keyframe.position().y, keyframe.position().z));
                        y = drawValue(context, textRenderer, contentX, y, "Rot", String.format("%.1f / %.1f", keyframe.yaw(), keyframe.pitch()));
                    }
                }
                drawHint(context, textRenderer, contentX, y + 4, "Arrow keys nudge selection");
            }
            default -> drawHint(context, textRenderer, contentX, y, "Select a zone, event, or cutscene");
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int rowY = contentY;
        switch (this.state.selection().type()) {
            case ZONE, POS1, POS2 -> {
                TriggerZone zone = this.state.selectedZone();
                if (zone == null) {
                    return false;
                }
                rowY += 4 * 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setShowFill(!zone.showFill());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setTriggerEnter(!zone.triggerEnter());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setTriggerExit(!zone.triggerExit());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setTriggerWhileInside(!zone.triggerWhileInside());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setTriggerTimeInside(!zone.triggerTimeInside());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setDelayTicks(zone.delayTicks() + 10);
                    zone.setOnceMode(zone.onceMode().next());
                    zone.setTargetMode(zone.targetMode().next());
                    return true;
                }
            }
            case CUTSCENE, KEYFRAME -> {
                CutsceneDefinition cutscene = this.state.selectedCutscene();
                if (cutscene == null) {
                    return false;
                }
                rowY += 2 * 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    cutscene.setLoop(!cutscene.loop());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    cutscene.setShowPreview(!cutscene.showPreview());
                    return true;
                }
                rowY += 20;
                if (clickRow(mouseX, mouseY, contentX, rowY)) {
                    cutscene.setAutoKeyframe(!cutscene.autoKeyframe());
                    return true;
                }
                rowY += 20;
                if (this.state.selection().type() == EditorSelectionType.KEYFRAME && clickRow(mouseX, mouseY, contentX, rowY)) {
                    CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                    if (keyframe != null) {
                        int index = cutscene.keyframes().indexOf(keyframe);
                        cutscene.keyframes().set(index, keyframe.withInterpolation(keyframe.interpolation().next()));
                        return true;
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private int drawValue(DrawContext context, TextRenderer textRenderer, int x, int y, String label, String value) {
        context.fill(x, y, x + contentWidth(), y + 18, 0xFF18202A);
        context.drawBorder(x, y, contentWidth(), 18, 0xFF334154);
        context.drawText(textRenderer, Text.literal(label), x + 6, y + 5, 0xFF9FB1CD, false);
        context.drawText(textRenderer, Text.literal(value), x + 92, y + 5, 0xFFE7ECF5, false);
        return y + 20;
    }

    private int drawToggle(DrawContext context, TextRenderer textRenderer, int x, int y, String label, boolean value) {
        return drawValue(context, textRenderer, x, y, label, value ? "ON" : "OFF");
    }

    private void drawHint(DrawContext context, TextRenderer textRenderer, int x, int y, String value) {
        context.drawText(textRenderer, Text.literal(value), x, y, 0xFF90A1BD, false);
    }

    private boolean clickRow(double mouseX, double mouseY, int contentX, int y) {
        return isInside(mouseX, mouseY, contentX, y, contentWidth(), 18);
    }

    private String summarize(EditorActionDefinition definition, EditorActionInstance action) {
        if (definition == null) {
            return action.actionId().toString();
        }
        List<String> lines = definition.summarize(action.data());
        return lines.isEmpty() ? definition.displayName() : String.join(" | ", lines);
    }

    private String formatPos(BlockPos pos) {
        return formatVec(pos.getX(), pos.getY(), pos.getZ());
    }

    private String formatVec(double x, double y, double z) {
        return String.format("%.2f, %.2f, %.2f", x, y, z);
    }
}
