package net.dark.editorapi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public final class EditorScreen extends Screen {
    private final EditorClientState state;
    private final EditorLayoutStore layoutStore = new EditorLayoutStore();
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
        this.layoutStore.load();
        this.panels.clear();
        int browserWidth = Math.max(170, Math.min(220, this.width / 5));
        int inspectorWidth = Math.max(180, Math.min(220, this.width / 4));
        int timelineHeight = Math.max(100, Math.min(126, this.height / 4));
        int contentWidth = Math.max(240, this.width - browserWidth - inspectorWidth - 16);

        this.panels.add(new MenuBarPanel(this.state, 0, 0, this.width));
        this.browserPanel = new BrowserPanel(this.state, 4, 24, browserWidth, this.height - 28);
        this.panels.add(this.browserPanel);
        this.panels.add(new InspectorPanel(this.state, this.width - inspectorWidth - 4, 24, inspectorWidth, Math.max(140, this.height - 28)));
        this.timelinePanel = new TimelinePanel(this.state, browserWidth + 8, this.height - timelineHeight - 4, contentWidth, timelineHeight);
        this.panels.add(this.timelinePanel);

        for (EditorPanel panel : this.panels) {
            if (!"Menu".equals(panel.panelKey())) {
                this.layoutStore.applyPanel(panel.panelKey(), panel);
            }
        }

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
            if (isPanelVisible(panel) && !(panel instanceof MenuBarPanel)) {
                panel.render(context, this.textRenderer, mouseX, mouseY, delta);
            }
        }
        for (EditorPanel panel : this.panels) {
            if (isPanelVisible(panel) && panel instanceof MenuBarPanel) {
                panel.render(context, this.textRenderer, mouseX, mouseY, delta);
            }
        }
        this.quickActionMenu.render(context, this.textRenderer, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.quickActionMenu.isOpen() && this.quickActionMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (EditorPanel panel : this.panels) {
            if (panel instanceof MenuBarPanel && isPanelVisible(panel) && panel.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        if (button == 1) {
            if (this.state.selection().type() != net.dark.editorapi.client.state.EditorSelectionType.NONE) {
                this.quickActionMenu.open((int) mouseX, (int) mouseY);
                return true;
            }
            return false;
        }

        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (panel instanceof MenuBarPanel) {
                continue;
            }
            if (isPanelVisible(panel) && panel.mouseClicked(mouseX, mouseY, button)) {
                this.panels.remove(index);
                this.panels.add(panel);
                return true;
            }
        }
        for (EditorPanel panel : this.panels) {
            if (isPanelVisible(panel) && panel.contains(mouseX, mouseY)) {
                return true;
            }
        }
        if (button == 0) {
            if (this.state.beginGizmoDrag(mouseX, mouseY)) {
                return true;
            }
            return this.state.pickWorldSelection(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.state.hasActiveDrag()) {
            this.state.endGizmoDrag();
            return true;
        }
        for (EditorPanel panel : this.panels) {
            if (isPanelVisible(panel) && panel.mouseReleased(mouseX, mouseY, button)) {
                if (!"Menu".equals(panel.panelKey())) {
                    this.layoutStore.savePanel(panel.panelKey(), panel);
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.state.hasActiveDrag()) {
            return this.state.updateGizmoDrag(mouseX, mouseY);
        }
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (isPanelVisible(panel) && panel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                if (!"Menu".equals(panel.panelKey())) {
                    this.layoutStore.savePanel(panel.panelKey(), panel);
                }
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (isPanelVisible(panel) && panel.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_SPACE) {
            if (this.timelinePanel != null && this.timelinePanel.handleSpacebar()) {
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.quickActionMenu.close();
            this.state.setEditorOpen(false);
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new GameMenuScreen(true));
            return true;
        }

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_Z) {
            this.state.undo();
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_Y) {
            this.state.redo();
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && keyCode == GLFW.GLFW_KEY_D) {
            this.state.duplicateSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (this.timelinePanel != null && this.timelinePanel.handleDelete()) {
                return true;
            }
            this.state.deleteSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_G) {
            this.state.setToolMode(net.dark.editorapi.client.state.EditorToolMode.TRANSLATE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_R) {
            this.state.setToolMode(net.dark.editorapi.client.state.EditorToolMode.ROTATE);
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

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (int index = this.panels.size() - 1; index >= 0; index--) {
            EditorPanel panel = this.panels.get(index);
            if (isPanelVisible(panel) && panel.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
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
        if (this.state.selection().type() == net.dark.editorapi.client.state.EditorSelectionType.NONE
                || this.state.selection().type() == net.dark.editorapi.client.state.EditorSelectionType.EVENT
                || this.state.selection().type() == net.dark.editorapi.client.state.EditorSelectionType.CUTSCENE) {
            return false;
        }
        this.state.nudgeSelection(x, y, z);
        return true;
    }

    private boolean isPanelVisible(EditorPanel panel) {
        return switch (panel.panelKey()) {
            case "Browser" -> this.state.browserVisible();
            case "Inspector" -> this.state.inspectorVisible();
            case "Timeline" -> this.state.timelineVisible();
            default -> true;
        };
    }

    public boolean isHoveringUi(int mouseX, int mouseY) {
        if (this.quickActionMenu.isOpen()) {
            return true;
        }
        for (EditorPanel panel : this.panels) {
            if (isPanelVisible(panel) && panel.contains(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }
}
