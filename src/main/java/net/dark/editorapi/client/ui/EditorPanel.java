package net.dark.editorapi.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public abstract class EditorPanel {
    protected static final int TITLE_HEIGHT = 16;
    protected static final int PADDING = 6;

    private final String title;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean dragging;
    private boolean resizing;
    private int dragOffsetX;
    private int dragOffsetY;

    protected EditorPanel(String title, int x, int y, int width, int height) {
        this.title = title;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0xC8141820);
        context.fill(this.x, this.y, this.x + this.width, this.y + TITLE_HEIGHT, 0xFF1D232D);
        context.drawBorder(this.x, this.y, this.width, this.height, 0xFF394253);
        context.drawText(textRenderer, Text.literal(this.title), this.x + PADDING, this.y + 4, 0xFFE4E8EF, false);
        context.fill(this.x + this.width - 8, this.y + this.height - 8, this.x + this.width - 3, this.y + this.height - 3, 0xFF71809B);
        renderContent(context, textRenderer, mouseX, mouseY, delta, this.x + PADDING, this.y + TITLE_HEIGHT + PADDING, this.width - PADDING * 2, this.height - TITLE_HEIGHT - PADDING * 2);
    }

    protected abstract void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight);

    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        return false;
    }

    protected boolean onMouseReleased(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        return false;
    }

    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int contentX, int contentY, int contentWidth, int contentHeight) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }

        if (button == 0 && inResizeHandle(mouseX, mouseY)) {
            this.resizing = true;
            return true;
        }

        if (button == 0 && inTitleBar(mouseX, mouseY)) {
            this.dragging = true;
            this.dragOffsetX = (int) mouseX - this.x;
            this.dragOffsetY = (int) mouseY - this.y;
            return true;
        }

        return onMouseClicked(mouseX, mouseY, button, contentX(), contentY(), contentWidth(), contentHeight());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = this.dragging || this.resizing;
        this.dragging = false;
        this.resizing = false;
        return handled || onMouseReleased(mouseX, mouseY, button, contentX(), contentY(), contentWidth(), contentHeight());
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.dragging) {
            this.x = (int) mouseX - this.dragOffsetX;
            this.y = (int) mouseY - this.dragOffsetY;
            return true;
        }

        if (this.resizing) {
            this.width = MathHelper.clamp((int) mouseX - this.x, 160, 700);
            this.height = MathHelper.clamp((int) mouseY - this.y, 110, 500);
            return true;
        }

        return onMouseDragged(mouseX, mouseY, button, deltaX, deltaY, contentX(), contentY(), contentWidth(), contentHeight());
    }

    protected void drawButton(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height, String label, boolean hovered, boolean active) {
        int fill = active ? 0xFF4F7EF7 : hovered ? 0xFF374153 : 0xFF222A35;
        context.fill(x, y, x + width, y + height, fill);
        context.drawBorder(x, y, width, height, hovered ? 0xFF96A8C4 : 0xFF536178);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(label), x + width / 2, y + 4, active ? 0xFFFFFFFF : 0xFFD7DDE7);
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    private boolean inTitleBar(double mouseX, double mouseY) {
        return mouseY >= this.y && mouseY <= this.y + TITLE_HEIGHT && mouseX >= this.x && mouseX <= this.x + this.width;
    }

    private boolean inResizeHandle(double mouseX, double mouseY) {
        return mouseX >= this.x + this.width - 10 && mouseY >= this.y + this.height - 10;
    }

    protected int contentX() {
        return this.x + PADDING;
    }

    protected int contentY() {
        return this.y + TITLE_HEIGHT + PADDING;
    }

    protected int contentWidth() {
        return this.width - PADDING * 2;
    }

    protected int contentHeight() {
        return this.height - TITLE_HEIGHT - PADDING * 2;
    }
}
