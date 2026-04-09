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
    private final QuickActionMenu quickActionMenu;
    private BrowserPanel browserPanel;
    private TimelinePanel timelinePanel;

    public EditorScreen(EditorClientState state) {
        super(Text.literal("EditorAPI"));
        this.state = state;
        this.quickActionMenu = new QuickActionMenu(state);
    }

    @Override
    protected void init() {
        this.panels.clear();
        int menuWidth = Math.min(this.width - 8, 260);
        int browserWidth = Math.max(170, Math.min(220, this.width / 5));
        int inspectorWidth = Math.max(180, Math.min(220, this.width / 4));
        int timelineHeight = Math.max(100, Math.min(126, this.height / 4));
        int contentWidth = Math.max(240, this.width - browserWidth - inspectorWidth - 16);

        this.panels.add(new MenuBarPanel(this.state, 4, 2, menuWidth));
        this.browserPanel = new BrowserPanel(this.state, 4, 20, browserWidth, this.height - 24);
        this.panels.add(this.browserPanel);
        this.panels.add(new InspectorPanel(this.state, this.width - inspectorWidth - 4, 20, inspectorWidth, Math.max(140, this.height - 24)));
        this.timelinePanel = new TimelinePanel(this.state, browserWidth + 8, this.height - timelineHeight - 4, contentWidth, timelineHeight);
        this.panels.add(this.timelinePanel);

        this.panels.get(0).setLocked(true);
        this.panels.get(1).setLocked(true);
        this.panels.get(2).setLocked(true);
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
        for (EditorPanel panel : this.panels) {
            panel.render(context, this.textRenderer, mouseX, mouseY, delta);
        }
        this.quickActionMenu.render(context, this.textRenderer, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            this.quickActionMenu.open((int) mouseX, (int) mouseY, switch (this.browserPanel.activeTab()) {
                case SCENE -> QuickActionMenu.Context.SCENE;
                case EVENTS -> QuickActionMenu.Context.EVENTS;
                case CUTSCENES -> QuickActionMenu.Context.CUTSCENES;
            });
            return true;
        }

        if (this.quickActionMenu.isOpen() && this.quickActionMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (panel.mouseClicked(mouseX, mouseY, button)) {
                this.panels.remove(index);
                this.panels.add(panel);
                return true;
            }
        }
        if (button == 0) {
            return this.state.pickHoveredGizmo();
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
        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (this.timelinePanel != null && this.timelinePanel.handleSpacebar()) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.quickActionMenu.close();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER && this.state.selectedCutscene() != null) {
            this.state.addCurrentCameraKeyframe();
            return true;
        }

        if (this.state.selectedCutscene() != null && (keyCode == GLFW.GLFW_KEY_KP_ADD || keyCode == GLFW.GLFW_KEY_EQUAL)) {
            this.state.addCurrentCameraKeyframe();
            return true;
        }

        if (this.state.selectedCutscene() != null && keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            this.state.runtime().cutscenes().stop();
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
