package net.dark.editorapi.client.ui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class EditorWidgets {
    private EditorWidgets() {
    }

    public static void drawButton(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height, String label, boolean hovered, boolean active) {
        int fill = active ? EditorPalette.ACCENT : hovered ? 0xFF374153 : 0xFF222A35;
        context.fill(x, y, x + width, y + height, fill);
        context.drawBorder(x, y, width, height, hovered ? 0xFF96A8C4 : 0xFF536178);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + width / 2, y + 3, active ? 0xFFFFFFFF : 0xFFD7DDE7);
    }

    public static void drawRow(DrawContext context, TextRenderer textRenderer, int x, int y, int width, String label, String value, boolean hovered, boolean selected) {
        int fill = selected ? EditorPalette.ROW_SELECTED : hovered ? EditorPalette.ROW_HOVER : EditorPalette.ROW;
        context.fill(x, y, x + width, y + 18, fill);
        context.drawBorder(x, y, width, 18, 0xFF334154);
        context.drawText(textRenderer, Text.literal(label), x + 6, y + 5, EditorPalette.TEXT_MUTED, false);
        context.drawText(textRenderer, Text.literal(value), x + 92, y + 5, EditorPalette.TEXT, false);
    }
}
