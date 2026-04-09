package net.dark.editorapi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class EditorScreen extends Screen {
    private final EditorClientState state;
    private final List<EditorPanel> panels = new ArrayList<>();

    public EditorScreen(EditorClientState state) {
        super(Text.literal("EditorAPI"));
        this.state = state;
    }

    @Override
    protected void init() {
        this.panels.clear();
        this.panels.add(new ToolbarPanel(this.state, 10, 10, 220, 170));
        this.panels.add(new OutlinerPanel(this.state, 10, 190, 250, 240));
        this.panels.add(new InspectorPanel(this.state, this.width - 270, 10, 260, 280));
        this.panels.add(new TimelinePanel(this.state, 270, this.height - 170, this.width - 540, 160));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        this.state.setEditorOpen(false);
        super.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.fillGradient(0, 0, this.width, this.height, 0xAA0A0C11, 0xCC090B10);
        context.drawText(this.textRenderer, Text.literal("EditorAPI  |  F6 Toggle  |  F7 Pos1  |  F8 Pos2  |  F9 Add Key"), 12, this.height - 14, 0xFFAFBDD4, false);
        for (EditorPanel panel : this.panels) {
            panel.render(context, this.textRenderer, mouseX, mouseY, delta);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                this.panels.remove(index);
                this.panels.add(panel);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (EditorPanel panel : this.panels) {
            if (panel.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            if (this.panels.get(index).mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }

        double step = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0 ? 5.0D : 1.0D;
        if (moveSelection(keyCode, step)) {
            this.state.upsertAutoKeyframeFromCamera();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            nudge(0.0D, step, 0.0D);
            this.state.upsertAutoKeyframeFromCamera();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            nudge(0.0D, -step, 0.0D);
            this.state.upsertAutoKeyframeFromCamera();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean moveSelection(int keyCode, double step) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> nudge(-step, 0.0D, 0.0D);
            case GLFW.GLFW_KEY_RIGHT -> nudge(step, 0.0D, 0.0D);
            case GLFW.GLFW_KEY_UP -> nudge(0.0D, 0.0D, -step);
            case GLFW.GLFW_KEY_DOWN -> nudge(0.0D, 0.0D, step);
            default -> false;
        };
    }

    private boolean nudge(double x, double y, double z) {
        switch (this.state.selection().type()) {
            case POS1 -> {
                TriggerZone zone = this.state.selectedZone();
                zone.setPos1(zone.pos1().add((int) x, (int) y, (int) z));
                return true;
            }
            case POS2 -> {
                TriggerZone zone = this.state.selectedZone();
                zone.setPos2(zone.pos2().add((int) x, (int) y, (int) z));
                return true;
            }
            case KEYFRAME -> {
                CutsceneDefinition cutscene = this.state.selectedCutscene();
                if (cutscene == null) {
                    return false;
                }
                for (int index = 0; index < cutscene.keyframes().size(); index++) {
                    CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
                    if (keyframe.id().equals(this.state.selection().childId())) {
                        Vec3d moved = keyframe.position().add(x, y, z);
                        cutscene.keyframes().set(index, keyframe.withTransform(moved, keyframe.yaw(), keyframe.pitch()));
                        return true;
                    }
                }
            }
            default -> {
                return false;
            }
        }
        return false;
    }
}
