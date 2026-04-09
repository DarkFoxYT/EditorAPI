package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelection;
import net.dark.editorapi.client.ui.widget.EditorPalette;
import net.dark.editorapi.scene.EditorSceneIndex;
import net.dark.editorapi.scene.SceneObjectType;
import net.dark.editorapi.scene.SceneTreeNode;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class BrowserPanel extends EditorPanel {
    private final EditorClientState state;
    private BrowserTab activeTab = BrowserTab.SCENE;
    private int scrollOffset;
    private boolean draggingScrollbar;

    public BrowserPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Browser", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(52, (contentWidth - 8) / 3);
        int y = contentY;
        drawButton(context, textRenderer, contentX, y, tabWidth, 14, "Scene", isInside(mouseX, mouseY, contentX, y, tabWidth, 14), this.activeTab == BrowserTab.SCENE);
        drawButton(context, textRenderer, contentX + tabWidth + 4, y, tabWidth, 14, "Events", isInside(mouseX, mouseY, contentX + tabWidth + 4, y, tabWidth, 14), this.activeTab == BrowserTab.EVENTS);
        drawButton(context, textRenderer, contentX + (tabWidth + 4) * 2, y, tabWidth, 14, "Cuts", isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, y, tabWidth, 14), this.activeTab == BrowserTab.CUTSCENES);
        y += 20;
        int listStartY = y;
        int rowIndex = 0;
        int totalRows = visibleRowCount();

        for (SceneTreeNode node : new EditorSceneIndex(this.state.project()).buildTree()) {
            if (!matchesTab(node.type())) {
                continue;
            }
            if (rowIndex++ < this.scrollOffset) {
                continue;
            }
            boolean selected = this.state.selection().objectId() != null
                    && this.state.selection().objectId().equals(node.objectId())
                    && ((node.childId() == null && this.state.selection().childId() == null) || (node.childId() != null && node.childId().equals(this.state.selection().childId())));
            int indent = node.depth() * 10;
            int rowWidth = Math.max(42, contentWidth - indent - 8);
            int color = selected ? EditorPalette.ROW_SELECTED : isInside(mouseX, mouseY, contentX + indent, y, rowWidth, 16) ? EditorPalette.ROW_HOVER : EditorPalette.ROW_ALT;
            context.fill(contentX + indent, y, contentX + indent + rowWidth, y + 16, color);
            context.drawBorder(contentX + indent, y, rowWidth, 16, 0xFF384558);
            context.drawText(textRenderer, Text.literal(node.label()), contentX + indent + 5, y + 4, EditorPalette.TEXT, false);
            y += 18;
            if (y > contentY + contentHeight - 16) {
                break;
            }
        }

        renderScrollbar(context, contentX + contentWidth - 6, listStartY, contentHeight - 20, totalRows);
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(52, (contentWidth - 8) / 3);
        if (isInside(mouseX, mouseY, contentX, contentY, tabWidth, 14)) {
            this.activeTab = BrowserTab.SCENE;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + tabWidth + 4, contentY, tabWidth, 14)) {
            this.activeTab = BrowserTab.EVENTS;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14)) {
            this.activeTab = BrowserTab.CUTSCENES;
            return true;
        }

        int y = contentY + 20;
        int rowIndex = 0;
        for (SceneTreeNode node : new EditorSceneIndex(this.state.project()).buildTree()) {
            if (!matchesTab(node.type())) {
                continue;
            }
            if (rowIndex++ < this.scrollOffset) {
                continue;
            }
            int indent = node.depth() * 10;
            int rowWidth = Math.max(42, contentWidth - indent - 8);
            if (isInside(mouseX, mouseY, contentX + indent, y, rowWidth, 16)) {
                if (node.type() == SceneObjectType.CAMERA_KEYFRAME) {
                    this.state.setSelection(EditorSelection.keyframe(node.objectId(), node.childId()));
                } else {
                    this.state.setSelection(switch (node.type()) {
                        case TRIGGER_ZONE -> EditorSelection.zone(node.objectId());
                        case EVENT -> EditorSelection.event(node.objectId());
                        case CUTSCENE -> EditorSelection.cutscene(node.objectId());
                        default -> EditorSelection.NONE;
                    });
                }
                return true;
            }
            y += 18;
            if (y > contentY + contentHeight - 16) {
                break;
            }
        }

        int totalRows = visibleRowCount();
        int scrollbarHeight = contentHeight - 20;
        int thumbHeight = scrollbarThumbHeight(scrollbarHeight, totalRows);
        int thumbY = scrollbarY(contentY + 20, scrollbarHeight, thumbHeight, totalRows);
        if (isInside(mouseX, mouseY, contentX + contentWidth - 8, thumbY, 8, thumbHeight)) {
            this.draggingScrollbar = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY, int contentX, int contentY, int contentWidth, int contentHeight) {
        if (!this.draggingScrollbar) {
            return false;
        }
        int totalRows = visibleRowCount();
        int viewportRows = Math.max(1, (contentHeight - 20) / 18);
        int maxOffset = Math.max(0, totalRows - viewportRows);
        if (maxOffset == 0) {
            this.scrollOffset = 0;
            return true;
        }
        double normalized = MathHelper.clamp((mouseY - (contentY + 20)) / (double) Math.max(1, contentHeight - 40), 0.0D, 1.0D);
        this.scrollOffset = MathHelper.floor(normalized * maxOffset);
        return true;
    }

    @Override
    protected boolean onMouseReleased(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        boolean handled = this.draggingScrollbar;
        this.draggingScrollbar = false;
        return handled;
    }

    private boolean matchesTab(SceneObjectType type) {
        return switch (this.activeTab) {
            case SCENE -> type == SceneObjectType.TRIGGER_ZONE;
            case EVENTS -> type == SceneObjectType.EVENT;
            case CUTSCENES -> type == SceneObjectType.CUTSCENE || type == SceneObjectType.CAMERA_KEYFRAME;
        };
    }

    public enum BrowserTab {
        SCENE,
        EVENTS,
        CUTSCENES
    }

    public BrowserTab activeTab() {
        return this.activeTab;
    }

    private int visibleRowCount() {
        int count = 0;
        for (SceneTreeNode node : new EditorSceneIndex(this.state.project()).buildTree()) {
            if (matchesTab(node.type())) {
                count++;
            }
        }
        return count;
    }

    private void renderScrollbar(DrawContext context, int x, int y, int height, int totalRows) {
        int viewportRows = Math.max(1, height / 18);
        if (totalRows <= viewportRows) {
            return;
        }
        context.fill(x, y, x + 4, y + height, 0x55263547);
        int thumbHeight = scrollbarThumbHeight(height, totalRows);
        int thumbY = scrollbarY(y, height, thumbHeight, totalRows);
        context.fill(x, thumbY, x + 4, thumbY + thumbHeight, 0xFF7C8DA8);
    }

    private int scrollbarThumbHeight(int height, int totalRows) {
        int viewportRows = Math.max(1, height / 18);
        return Math.max(16, MathHelper.floor(height * (viewportRows / (float) Math.max(1, totalRows))));
    }

    private int scrollbarY(int y, int height, int thumbHeight, int totalRows) {
        int viewportRows = Math.max(1, height / 18);
        int maxOffset = Math.max(0, totalRows - viewportRows);
        if (maxOffset == 0) {
            return y;
        }
        double normalized = this.scrollOffset / (double) maxOffset;
        return y + MathHelper.floor((height - thumbHeight) * normalized);
    }
}
