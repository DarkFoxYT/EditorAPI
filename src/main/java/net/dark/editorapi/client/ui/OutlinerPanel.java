package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelection;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.TriggerZone;
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
        y = drawSection(context, textRenderer, contentX, y, "Zones");
        for (TriggerZone zone : this.state.project().zones().values()) {
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(zone.id());
            y = drawRow(context, textRenderer, contentX, y, contentWidth, mouseX, mouseY, selected, zone.name(), "P1  P2");
        }

        y += 6;
        y = drawSection(context, textRenderer, contentX, y, "Events");
        for (EditorEventDefinition event : this.state.project().events().values()) {
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(event.id());
            y = drawRow(context, textRenderer, contentX, y, contentWidth, mouseX, mouseY, selected, event.name(), event.actions().size() + " actions");
        }

        y += 6;
        y = drawSection(context, textRenderer, contentX, y, "Cutscenes");
        for (CutsceneDefinition cutscene : this.state.project().cutscenes().values()) {
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(cutscene.id());
            y = drawRow(context, textRenderer, contentX, y, contentWidth, mouseX, mouseY, selected, cutscene.name(), cutscene.keyframes().size() + " keys");
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int y = contentY + 18;
        for (TriggerZone zone : this.state.project().zones().values()) {
            if (isInside(mouseX, mouseY, contentX, y, contentWidth, 18)) {
                if (mouseX >= contentX + contentWidth - 42 && mouseX <= contentX + contentWidth - 24) {
                    this.state.setSelection(EditorSelection.pos1(zone.id()));
                } else if (mouseX >= contentX + contentWidth - 22 && mouseX <= contentX + contentWidth - 4) {
                    this.state.setSelection(EditorSelection.pos2(zone.id()));
                } else {
                    this.state.setSelection(EditorSelection.zone(zone.id()));
                }
                return true;
            }
            y += 20;
        }

        y += 24;
        for (EditorEventDefinition event : this.state.project().events().values()) {
            if (isInside(mouseX, mouseY, contentX, y, contentWidth, 18)) {
                this.state.setSelection(EditorSelection.event(event.id()));
                return true;
            }
            y += 20;
        }

        y += 24;
        for (CutsceneDefinition cutscene : this.state.project().cutscenes().values()) {
            if (isInside(mouseX, mouseY, contentX, y, contentWidth, 18)) {
                this.state.setSelection(EditorSelection.cutscene(cutscene.id()));
                return true;
            }
            y += 20;
        }
        return false;
    }

    private int drawSection(DrawContext context, TextRenderer textRenderer, int x, int y, String title) {
        context.drawText(textRenderer, Text.literal(title), x, y, 0xFF98A8C2, false);
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
