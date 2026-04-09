package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class QuickActionMenu {
    private final EditorClientState state;
    private boolean open;
    private int x;
    private int y;

    public QuickActionMenu(EditorClientState state) {
        this.state = state;
    }

    public void open(int x, int y) {
        this.open = true;
        this.x = x;
        this.y = y;
    }

    public void close() {
        this.open = false;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!this.open) {
            return;
        }
        String[] items = items();
        int width = 128;
        int height = items.length * 16 + 4;
        context.fill(this.x, this.y, this.x + width, this.y + height, 0xEE121821);
        context.drawBorder(this.x, this.y, width, height, 0xFF415067);
        for (int i = 0; i < items.length; i++) {
            int rowY = this.y + 2 + i * 16;
            boolean hovered = mouseX >= this.x + 2 && mouseX <= this.x + width - 2 && mouseY >= rowY && mouseY <= rowY + 14;
            context.fill(this.x + 2, rowY, this.x + width - 2, rowY + 14, hovered ? 0xFF2A3445 : 0x00000000);
            context.drawText(textRenderer, Text.literal(items[i]), this.x + 6, rowY + 3, 0xFFE7ECF5, false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.open) {
            return false;
        }
        String[] items = items();
        int width = 128;
        if (mouseX < this.x || mouseX > this.x + width) {
            this.open = false;
            return false;
        }
        for (int i = 0; i < items.length; i++) {
            int rowY = this.y + 2 + i * 16;
            if (mouseY >= rowY && mouseY <= rowY + 14) {
                handleIndex(i);
                this.open = false;
                return true;
            }
        }
        this.open = false;
        return false;
    }

    private String[] items() {
        if (this.state.selection().type() == EditorSelectionType.NONE) {
            return new String[]{"Undo", "Redo", "New Zone", "New Cutscene"};
        }
        return new String[]{"Delete", "Duplicate", "Undo", "Save Blueprint"};
    }

    private void handleIndex(int index) {
        if (this.state.selection().type() == EditorSelectionType.NONE) {
            switch (index) {
                case 0 -> this.state.undo();
                case 1 -> this.state.redo();
                case 2 -> this.state.createZone();
                case 3 -> this.state.createCutscene();
                default -> {
                }
            }
            return;
        }

        switch (index) {
            case 0 -> this.state.deleteSelection();
            case 1 -> this.state.duplicateSelection();
            case 2 -> this.state.undo();
            case 3 -> this.state.saveSelectionAsBlueprint();
            default -> {
            }
        }
    }
}
