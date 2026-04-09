package net.dark.editorapi.client.ui;

import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.dark.editorapi.client.state.EditorClientState;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class QuickActionMenu {
    private final EditorClientState state;
    private boolean open;
    private int x;
    private int y;
    private Context context = Context.SCENE;

    public QuickActionMenu(EditorClientState state) {
        this.state = state;
    }

    public void open(int x, int y, Context context) {
        this.open = true;
        this.x = x;
        this.y = y;
        this.context = context;
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
        int width = 112;
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
        int width = 112;
        if (mouseX < this.x || mouseX > this.x + width) {
            this.open = false;
            return false;
        }
        for (int i = 0; i < items.length; i++) {
            int rowY = this.y + 2 + i * 16;
            if (mouseY >= rowY && mouseY <= rowY + 14) {
                switch (i) {
                    case 0 -> primaryAction();
                    case 1 -> secondaryAction();
                    case 2 -> tertiaryAction();
                    case 3 -> quaternaryAction();
                    default -> {
                    }
                }
                this.open = false;
                return true;
            }
        }
        this.open = false;
        return false;
    }

    private String[] items() {
        return switch (this.context) {
            case SCENE -> new String[]{"Add Zone", "Set Pos1", "Set Pos2", "Add Event"};
            case EVENTS -> new String[]{"Add Event", "Add Title", "Add Sound", "Add Linked Cut"};
            case CUTSCENES -> new String[]{"Add Cutscene", "Add Camera Key", "Play Cutscene", "Stop Cutscene"};
        };
    }

    private void primaryAction() {
        switch (this.context) {
            case SCENE -> this.state.createZone();
            case EVENTS -> this.state.createEvent();
            case CUTSCENES -> this.state.createCutscene();
        }
    }

    private void secondaryAction() {
        switch (this.context) {
            case SCENE -> this.state.capturePos1();
            case EVENTS -> this.state.addActionToSelectedEvent(BuiltinEditorActions.SHOW_TITLE);
            case CUTSCENES -> this.state.addCurrentCameraKeyframe();
        }
    }

    private void tertiaryAction() {
        switch (this.context) {
            case SCENE -> this.state.capturePos2();
            case EVENTS -> this.state.addActionToSelectedEvent(BuiltinEditorActions.PLAY_SOUND);
            case CUTSCENES -> {
                if (this.state.selectedCutscene() != null) {
                    this.state.runtime().cutscenes().start(this.state.selectedCutscene().id().toString());
                }
            }
        }
    }

    private void quaternaryAction() {
        switch (this.context) {
            case SCENE -> this.state.createEvent();
            case EVENTS -> this.state.addActionToSelectedEvent(BuiltinEditorActions.PLAY_CUTSCENE);
            case CUTSCENES -> this.state.runtime().cutscenes().stop();
        }
    }

    public enum Context {
        SCENE,
        EVENTS,
        CUTSCENES
    }
}
