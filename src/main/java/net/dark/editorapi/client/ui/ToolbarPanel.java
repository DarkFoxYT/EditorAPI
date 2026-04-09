package net.dark.editorapi.client.ui;

import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.dark.editorapi.client.state.EditorClientState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class ToolbarPanel extends EditorPanel {
    private final EditorClientState state;

    public ToolbarPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Toolbar", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int buttonWidth = (contentWidth - 6) / 2;
        int buttonHeight = 18;
        String[] labels = {"New Zone", "New Event", "New Cut", "Set Pos1", "Set Pos2", "Save", "Reload", "Add Key", "Play", "Stop", "+Title", "+Sound", "+FX", "+Cut"};
        for (int index = 0; index < labels.length; index++) {
            int x = contentX + (index % 2) * (buttonWidth + 6);
            int y = contentY + (index / 2) * (buttonHeight + 6);
            drawButton(context, textRenderer, x, y, buttonWidth, buttonHeight, labels[index], isInside(mouseX, mouseY, x, y, buttonWidth, buttonHeight), false);
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int buttonWidth = (contentWidth - 6) / 2;
        int buttonHeight = 18;
        for (int index = 0; index < 14; index++) {
            int x = contentX + (index % 2) * (buttonWidth + 6);
            int y = contentY + (index / 2) * (buttonHeight + 6);
            if (!isInside(mouseX, mouseY, x, y, buttonWidth, buttonHeight)) {
                continue;
            }

            switch (index) {
                case 0 -> this.state.createZone();
                case 1 -> this.state.createEvent();
                case 2 -> this.state.createCutscene();
                case 3 -> this.state.capturePos1();
                case 4 -> this.state.capturePos2();
                case 5 -> this.state.save();
                case 6 -> this.state.reload();
                case 7 -> this.state.addCurrentCameraKeyframe();
                case 8 -> {
                    if (this.state.selectedCutscene() != null) {
                        this.state.runtime().cutscenes().start(this.state.selectedCutscene().id().toString());
                    }
                }
                case 9 -> this.state.runtime().cutscenes().stop();
                case 10 -> this.state.addActionToSelectedEvent(BuiltinEditorActions.SHOW_TITLE);
                case 11 -> this.state.addActionToSelectedEvent(BuiltinEditorActions.PLAY_SOUND);
                case 12 -> this.state.addActionToSelectedEvent(BuiltinEditorActions.SPAWN_PARTICLES);
                case 13 -> this.state.addActionToSelectedEvent(BuiltinEditorActions.PLAY_CUTSCENE);
                default -> {
                }
            }
            return true;
        }
        return false;
    }
}
