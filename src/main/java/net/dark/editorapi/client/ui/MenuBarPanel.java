package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class MenuBarPanel extends EditorPanel {
    private final EditorClientState state;

    public MenuBarPanel(EditorClientState state, int x, int y, int width) {
        super("Menu", x, y, width, 34);
        this.state = state;
        setLocked(true);
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        String[] items = {"File", "Edit", "Create", "Playback", "View"};
        int x = contentX;
        for (String item : items) {
            int width = Math.max(34, textRenderer.getWidth(item) + 12);
            drawButton(context, textRenderer, x, contentY, width, 14, item, isInside(mouseX, mouseY, x, contentY, width, 14), false);
            x += width + 4;
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        String[] items = {"File", "Edit", "Create", "Playback", "View"};
        int x = contentX;
        for (String item : items) {
            int width = Math.max(34, net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(item) + 12);
            if (isInside(mouseX, mouseY, x, contentY, width, 14)) {
                switch (item) {
                    case "File" -> this.state.save();
                    case "Edit" -> this.state.reload();
                    case "Create" -> this.state.createZone();
                    case "Playback" -> {
                        if (this.state.selectedCutscene() != null) {
                            this.state.runtime().cutscenes().start(this.state.selectedCutscene().id().toString());
                        }
                    }
                    case "View" -> this.state.requestProjectSync();
                    default -> {
                    }
                }
                return true;
            }
            x += width + 4;
        }
        return false;
    }
}
