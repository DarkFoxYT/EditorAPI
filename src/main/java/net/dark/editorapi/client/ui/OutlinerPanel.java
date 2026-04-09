package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelection;
import net.dark.editorapi.client.ui.widget.EditorPalette;
import net.dark.editorapi.scene.EditorSceneIndex;
import net.dark.editorapi.scene.SceneObjectType;
import net.dark.editorapi.scene.SceneTreeNode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class OutlinerPanel extends EditorPanel {
    private final EditorClientState state;

    public OutlinerPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Hierarchy", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int y = contentY;
        String lastSection = "";
        for (SceneTreeNode node : new EditorSceneIndex(this.state.project()).buildTree()) {
            String section = switch (node.type()) {
                case BLUEPRINT -> "Blueprints";
                case TRIGGER_ZONE -> "Zones";
                case EVENT -> "Events";
                case CUTSCENE, CAMERA_KEYFRAME -> "Cutscenes";
                default -> "Scene";
            };
            if (!section.equals(lastSection)) {
                if (!lastSection.isEmpty()) {
                    y += 6;
                }
                y = drawSection(context, textRenderer, contentX, y, section);
                lastSection = section;
            }
            boolean selected = this.state.selection().objectId() != null
                    && this.state.selection().objectId().equals(node.objectId())
                    && ((node.childId() == null && this.state.selection().childId() == null) || (node.childId() != null && node.childId().equals(this.state.selection().childId())));
            int indent = node.depth() * 14;
            y = drawRow(context, textRenderer, contentX + indent, y, contentWidth - indent, mouseX, mouseY, selected, node.label(), (node.locked() ? "L " : "") + (node.visible() ? "V " : "H ") + node.detail());
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int y = contentY;
        String lastSection = "";
        for (SceneTreeNode node : new EditorSceneIndex(this.state.project()).buildTree()) {
            String section = switch (node.type()) {
                case BLUEPRINT -> "Blueprints";
                case TRIGGER_ZONE -> "Zones";
                case EVENT -> "Events";
                case CUTSCENE, CAMERA_KEYFRAME -> "Cutscenes";
                default -> "Scene";
            };
            if (!section.equals(lastSection)) {
                y += lastSection.isEmpty() ? 16 : 22;
                lastSection = section;
            }
            int indent = node.depth() * 14;
            if (isInside(mouseX, mouseY, contentX + indent, y, contentWidth - indent, 18)) {
                if (node.type() == SceneObjectType.CAMERA_KEYFRAME) {
                    this.state.setSelection(EditorSelection.keyframe(node.objectId(), node.childId()));
                } else if (node.type() == SceneObjectType.TRIGGER_ZONE && mouseX >= contentX + contentWidth - 42 && mouseX <= contentX + contentWidth - 24) {
                    this.state.setSelection(EditorSelection.pos1(node.objectId()));
                } else if (node.type() == SceneObjectType.TRIGGER_ZONE && mouseX >= contentX + contentWidth - 22 && mouseX <= contentX + contentWidth - 4) {
                    this.state.setSelection(EditorSelection.pos2(node.objectId()));
                } else {
                    this.state.setSelection(switch (node.type()) {
                        case BLUEPRINT -> EditorSelection.blueprint(node.objectId());
                        case TRIGGER_ZONE -> EditorSelection.zone(node.objectId());
                        case EVENT -> EditorSelection.event(node.objectId());
                        case CUTSCENE -> EditorSelection.cutscene(node.objectId());
                        default -> EditorSelection.NONE;
                    });
                }
                return true;
            }
            y += 20;
        }
        return false;
    }

    private int drawSection(DrawContext context, TextRenderer textRenderer, int x, int y, String title) {
        context.drawText(textRenderer, Text.literal(title), x, y, EditorPalette.TEXT_SECTION, false);
        return y + 16;
    }

    private int drawRow(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int mouseX, int mouseY, boolean selected, String label, String suffix) {
        int color = selected ? 0xFF314764 : isInside(mouseX, mouseY, x, y, width, 18) ? 0xFF232D3C : 0xFF1A212C;
        context.fill(x, y, x + width, y + 18, color);
        context.drawBorder(x, y, width, 18, 0xFF384558);
        context.drawText(textRenderer, Text.literal(label), x + 6, y + 5, 0xFFE7ECF5, false);
        context.drawText(textRenderer, Text.literal(suffix), x + width - textRenderer.getWidth(suffix) - 6, y + 5, 0xFF9FB0CB, false);
        return y + 20;
    }
}
