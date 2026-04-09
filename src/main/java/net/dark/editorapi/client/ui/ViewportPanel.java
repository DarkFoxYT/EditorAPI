package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class ViewportPanel extends EditorPanel {
    private final EditorClientState state;

    public ViewportPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Viewport", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        context.fillGradient(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0xAA111923, 0xAA0D1219);
        context.drawBorder(contentX, contentY, contentWidth, contentHeight, 0xFF334154);
        int centerX = contentX + contentWidth / 2;
        int centerY = contentY + contentHeight / 2;
        context.fill(centerX - 1, centerY - 18, centerX + 1, centerY + 18, 0x66FFFFFF);
        context.fill(centerX - 18, centerY - 1, centerX + 18, centerY + 1, 0x66FFFFFF);
        context.fill(centerX + 24, centerY - 1, centerX + 72, centerY + 1, 0xCCF25F5C);
        context.fill(centerX - 1, centerY - 72, centerX + 1, centerY - 24, 0xCC66D17A);
        context.fill(centerX - 72, centerY - 1, centerX - 24, centerY + 1, 0xCC59A8F2);
        context.drawText(textRenderer, Text.literal("Viewport"), contentX + 8, contentY + 8, 0xFFE7ECF5, false);
        context.drawText(textRenderer, Text.literal("Player view is the active viewport"), contentX + 8, contentY + 22, 0xFF9FB1CD, false);
        context.drawText(textRenderer, Text.literal("RMB: quick add menu  |  arrows: move selection"), contentX + 8, contentY + 36, 0xFF9FB1CD, false);
        context.drawText(textRenderer, Text.literal("Selection: " + this.state.selection().type().name()), contentX + 8, contentY + contentHeight - 16, 0xFFFFCA5A, false);
        if (this.state.selection().type() == EditorSelectionType.KEYFRAME) {
            context.drawText(textRenderer, Text.literal("Gizmo: XYZ move handles shown in viewport"), contentX + 8, contentY + 50, 0xFF9FD7A9, false);
        }
    }
}
