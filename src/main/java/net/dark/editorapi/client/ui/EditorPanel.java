package net.dark.editorapi.client.ui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.dark.editorapi.client.ui.widget.EditorPalette;
import net.dark.editorapi.client.ui.widget.EditorWidgets;

public abstract class EditorPanel {
    protected static final int TITLE_HEIGHT = 14;
    protected static final int PADDING = 4;

    private final String title;
    private int x;
    private int y;
    private int width;
    private int height;
    private boolean locked;
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
        clampToScreen();
        context.fill(this.x, this.y, this.x + this.width, this.y + this.height, EditorPalette.PANEL_BACKGROUND);
        context.fill(this.x, this.y, this.x + this.width, this.y + TITLE_HEIGHT, EditorPalette.PANEL_TITLE);
        context.drawBorder(this.x, this.y, this.width, this.height, EditorPalette.PANEL_BORDER);
        context.drawText(textRenderer, Text.literal(this.title), this.x + PADDING, this.y + 3, EditorPalette.TEXT, false);
        context.drawText(textRenderer, Text.literal(this.locked ? "P" : "o"), this.x + this.width - 20, this.y + 3, this.locked ? EditorPalette.ACCENT_ALT : EditorPalette.TEXT_MUTED, false);
        context.fill(this.x + this.width - 7, this.y + this.height - 7, this.x + this.width - 3, this.y + this.height - 3, 0xFF71809B);
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

    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers, int contentX, int contentY, int contentWidth, int contentHeight) {
        return false;
    }

    protected boolean onCharTyped(char chr, int modifiers, int contentX, int contentY, int contentWidth, int contentHeight) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }

        if (button == 0 && isInside(mouseX, mouseY, this.x + this.width - 24, this.y, 18, TITLE_HEIGHT)) {
            this.locked = !this.locked;
            return true;
        }

        if (this.locked) {
            return onMouseClicked(mouseX, mouseY, button, contentX(), contentY(), contentWidth(), contentHeight());
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
            snapToEdges();
            clampToScreen();
            return true;
        }

        if (this.resizing) {
            int maxWidth = MinecraftClient.getInstance().getWindow().getScaledWidth() - this.x - 4;
            int maxHeight = MinecraftClient.getInstance().getWindow().getScaledHeight() - this.y - 4;
            this.width = MathHelper.clamp((int) mouseX - this.x, 140, Math.max(140, maxWidth));
            this.height = MathHelper.clamp((int) mouseY - this.y, 90, Math.max(90, maxHeight));
            return true;
        }

        return onMouseDragged(mouseX, mouseY, button, deltaX, deltaY, contentX(), contentY(), contentWidth(), contentHeight());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return onKeyPressed(keyCode, scanCode, modifiers, contentX(), contentY(), contentWidth(), contentHeight());
    }

    public boolean charTyped(char chr, int modifiers) {
        return onCharTyped(chr, modifiers, contentX(), contentY(), contentWidth(), contentHeight());
    }

    protected void drawButton(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height, String label, boolean hovered, boolean active) {
        EditorWidgets.drawButton(context, textRenderer, x, y, width, height, label, hovered, active);
    }

    protected boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
    }

    public String panelKey() {
        return this.title;
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

    public int x() {
        return this.x;
    }

    public int y() {
        return this.y;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean locked() {
        return this.locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clampToScreen();
    }

    private void clampToScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        this.width = MathHelper.clamp(this.width, 140, Math.max(140, screenWidth - 8));
        this.height = MathHelper.clamp(this.height, 90, Math.max(90, screenHeight - 8));
        this.x = MathHelper.clamp(this.x, 2, Math.max(2, screenWidth - this.width - 2));
        this.y = MathHelper.clamp(this.y, 18, Math.max(18, screenHeight - this.height - 2));
    }

    private void snapToEdges() {
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        if (Math.abs(this.x - 2) <= 10) {
            this.x = 2;
        }
        if (Math.abs(this.y - 18) <= 10) {
            this.y = 18;
        }
        if (Math.abs((this.x + this.width) - (screenWidth - 2)) <= 10) {
            this.x = screenWidth - this.width - 2;
        }
        if (Math.abs((this.y + this.height) - (screenHeight - 2)) <= 10) {
            this.y = screenHeight - this.height - 2;
        }
    }
}
