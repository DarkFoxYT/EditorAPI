package net.dark.editorapi.client.ui;

import java.util.List;
import net.dark.editorapi.api.action.EditorActionDefinition;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.client.ui.widget.EditorWidgets;
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
    private InspectorTab activeTab = InspectorTab.GENERAL;

    public InspectorPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Inspector", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(44, (contentWidth - 12) / 4);
        drawButton(context, textRenderer, contentX, contentY, tabWidth, 14, "General", isInside(mouseX, mouseY, contentX, contentY, tabWidth, 14), this.activeTab == InspectorTab.GENERAL);
        drawButton(context, textRenderer, contentX + tabWidth + 4, contentY, tabWidth, 14, "Camera", isInside(mouseX, mouseY, contentX + tabWidth + 4, contentY, tabWidth, 14), this.activeTab == InspectorTab.CAMERA);
        drawButton(context, textRenderer, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14, "Player", isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14), this.activeTab == InspectorTab.PLAYER);
        drawButton(context, textRenderer, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14, "Events", isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14), this.activeTab == InspectorTab.EVENTS);

        int y = contentY + 20;
        switch (this.state.selection().type()) {
            case ZONE, POS1, POS2 -> y = renderZone(context, textRenderer, y, contentX);
            case EVENT -> y = renderEvent(context, textRenderer, y, contentX);
            case CUTSCENE, KEYFRAME -> y = renderCutscene(context, textRenderer, y, contentX);
            default -> drawHint(context, textRenderer, contentX, y, "Select a zone, event, or cutscene");
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(44, (contentWidth - 12) / 4);
        if (isInside(mouseX, mouseY, contentX, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.GENERAL;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + tabWidth + 4, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.CAMERA;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.PLAYER;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.EVENTS;
            return true;
        }

        int rowY = contentY + 20;
        switch (this.state.selection().type()) {
            case ZONE, POS1, POS2 -> {
                TriggerZone zone = this.state.selectedZone();
                if (zone == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL) {
                    rowY += 4 * 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setVisible(!zone.visible());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setLocked(!zone.locked());
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.PLAYER) {
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerEnter(!zone.triggerEnter());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerExit(!zone.triggerExit());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerWhileInside(!zone.triggerWhileInside());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerTimeInside(!zone.triggerTimeInside());
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.EVENTS && clickRow(mouseX, mouseY, contentX, rowY)) {
                    zone.setDelayTicks(zone.delayTicks() + 10);
                    zone.setRadius(zone.radius() + 1.0F);
                    zone.setOnceMode(zone.onceMode().next());
                    zone.setTargetMode(zone.targetMode().next());
                    return true;
                }
            }
            case EVENT -> {
                EditorEventDefinition event = this.state.selectedEvent();
                if (event == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 18)) {
                    event.setVisible(!event.visible());
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 36)) {
                    event.setLocked(!event.locked());
                    return true;
                }
            }
            case CUTSCENE, KEYFRAME -> {
                CutsceneDefinition cutscene = this.state.selectedCutscene();
                if (cutscene == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL) {
                    rowY += 2 * 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setVisible(!cutscene.visible());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setLocked(!cutscene.locked());
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.CAMERA) {
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setLoop(!cutscene.loop());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setShowPreview(!cutscene.showPreview());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setAutoKeyframe(!cutscene.autoKeyframe());
                        return true;
                    }
                    rowY += 18;
                    if (this.state.selection().type() == EditorSelectionType.KEYFRAME && clickRow(mouseX, mouseY, contentX, rowY)) {
                        CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                        if (keyframe != null) {
                            int index = cutscene.keyframes().indexOf(keyframe);
                            cutscene.keyframes().set(index, keyframe.withInterpolation(keyframe.interpolation().next()));
                            return true;
                        }
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private int renderZone(DrawContext context, TextRenderer textRenderer, int y, int x) {
        TriggerZone zone = this.state.selectedZone();
        if (zone == null) {
            return y;
        }
        return switch (this.activeTab) {
            case GENERAL -> {
                y = drawValue(context, textRenderer, x, y, "Name", zone.name());
                y = drawValue(context, textRenderer, x, y, "Pos1", formatPos(zone.pos1()));
                y = drawValue(context, textRenderer, x, y, "Pos2", formatPos(zone.pos2()));
                y = drawValue(context, textRenderer, x, y, "Mode", zone.onceMode().name() + " / " + zone.targetMode().name());
                y = drawToggle(context, textRenderer, x, y, "Visible", zone.visible());
                y = drawToggle(context, textRenderer, x, y, "Locked", zone.locked());
                yield y;
            }
            case PLAYER -> {
                y = drawToggle(context, textRenderer, x, y, "On Enter", zone.triggerEnter());
                y = drawToggle(context, textRenderer, x, y, "On Exit", zone.triggerExit());
                y = drawToggle(context, textRenderer, x, y, "While Inside", zone.triggerWhileInside());
                y = drawToggle(context, textRenderer, x, y, "Timed Trigger", zone.triggerTimeInside());
                y = drawValue(context, textRenderer, x, y, "Radius", String.format("%.1f", zone.radius()));
                yield y;
            }
            case EVENTS -> {
                y = drawValue(context, textRenderer, x, y, "Delay", zone.delayTicks() + " ticks");
                y = drawValue(context, textRenderer, x, y, "Once", zone.onceMode().name());
                y = drawValue(context, textRenderer, x, y, "Target", zone.targetMode().name());
                drawHint(context, textRenderer, x, y + 4, "Click the first row to cycle delay/mode/target");
                yield y;
            }
            case CAMERA -> {
                drawHint(context, textRenderer, x, y, "Zone selection has no camera settings");
                yield y;
            }
        };
    }

    private int renderEvent(DrawContext context, TextRenderer textRenderer, int y, int x) {
        EditorEventDefinition event = this.state.selectedEvent();
        if (event == null) {
            return y;
        }
        if (this.activeTab == InspectorTab.GENERAL) {
            y = drawValue(context, textRenderer, x, y, "Event", event.name());
            y = drawToggle(context, textRenderer, x, y, "Visible", event.visible());
            y = drawToggle(context, textRenderer, x, y, "Locked", event.locked());
            return y;
        }
        if (this.activeTab == InspectorTab.EVENTS) {
            for (EditorActionInstance action : event.actions()) {
                EditorActionDefinition definition = EditorActionRegistry.getInstance().get(action.actionId());
                y = drawValue(context, textRenderer, x, y, definition == null ? action.actionId().toString() : definition.displayName(), summarize(definition, action));
            }
            return y;
        }
        drawHint(context, textRenderer, x, y, "No data for this tab on the selected event");
        return y;
    }

    private int renderCutscene(DrawContext context, TextRenderer textRenderer, int y, int x) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return y;
        }
        return switch (this.activeTab) {
            case GENERAL -> {
                y = drawValue(context, textRenderer, x, y, "Cutscene", cutscene.name());
                y = drawValue(context, textRenderer, x, y, "Frames", cutscene.startFrame() + " -> " + cutscene.endFrame());
                y = drawToggle(context, textRenderer, x, y, "Visible", cutscene.visible());
                y = drawToggle(context, textRenderer, x, y, "Locked", cutscene.locked());
                yield y;
            }
            case CAMERA -> {
                y = drawToggle(context, textRenderer, x, y, "Loop", cutscene.loop());
                y = drawToggle(context, textRenderer, x, y, "Preview", cutscene.showPreview());
                y = drawToggle(context, textRenderer, x, y, "Auto Key", cutscene.autoKeyframe());
                if (this.state.selection().type() == EditorSelectionType.KEYFRAME) {
                    CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                    if (keyframe != null) {
                        y = drawValue(context, textRenderer, x, y, "Key", keyframe.frame() + " / " + keyframe.interpolation().label());
                        y = drawValue(context, textRenderer, x, y, "Pos", formatVec(keyframe.position().x, keyframe.position().y, keyframe.position().z));
                        y = drawValue(context, textRenderer, x, y, "Rot", String.format("%.1f / %.1f", keyframe.yaw(), keyframe.pitch()));
                    }
                }
                yield y;
            }
            case PLAYER -> {
                drawHint(context, textRenderer, x, y, "Player impact and camera target tools go here");
                yield y;
            }
            case EVENTS -> {
                drawHint(context, textRenderer, x, y, "Cutscene markers, cues, and linked events go here");
                yield y;
            }
        };
    }

    private int drawValue(DrawContext context, TextRenderer textRenderer, int x, int y, String label, String value) {
        EditorWidgets.drawRow(context, textRenderer, x, y, contentWidth(), label, value, false, false);
        return y + 18;
    }

    private int drawToggle(DrawContext context, TextRenderer textRenderer, int x, int y, String label, boolean value) {
        return drawValue(context, textRenderer, x, y, label, value ? "ON" : "OFF");
    }

    private void drawHint(DrawContext context, TextRenderer textRenderer, int x, int y, String value) {
        context.drawText(textRenderer, Text.literal(value), x, y, 0xFF90A1BD, false);
    }

    private boolean clickRow(double mouseX, double mouseY, int contentX, int y) {
        return isInside(mouseX, mouseY, contentX, y, contentWidth(), 16);
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

    private enum InspectorTab {
        GENERAL,
        CAMERA,
        PLAYER,
        EVENTS
    }
}
