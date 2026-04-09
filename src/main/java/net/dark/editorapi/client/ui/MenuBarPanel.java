package net.dark.editorapi.client.ui;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.ui.widget.EditorPalette;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public final class MenuBarPanel extends EditorPanel {
    private final EditorClientState state;
    private String openMenu = "";
    private static final int DROPDOWN_WIDTH = 164;

    public MenuBarPanel(EditorClientState state, int x, int y, int width) {
        super("Menu", x, y, width, 34);
        this.state = state;
        setLocked(true);
    }

    @Override
    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width(), 20, 0xEE141A22);
        context.drawBorder(0, 0, this.width(), 20, EditorPalette.PANEL_BORDER);
        renderContent(context, textRenderer, mouseX, mouseY, delta, 6, 1, this.width() - 12, 16);
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        String[] items = {"File", "Edit", "Tools", "View"};
        int x = contentX;
        for (String item : items) {
            int width = Math.max(36, textRenderer.getWidth(item) + 14);
            drawButton(context, textRenderer, x, contentY, width, 14, item, isInside(mouseX, mouseY, x, contentY, width, 14), this.openMenu.equals(item));
            x += width + 4;
        }
        if (!this.openMenu.isEmpty()) {
            renderDropdown(context, textRenderer, mouseX, mouseY, contentX, contentY + 16);
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        String[] items = {"File", "Edit", "Tools", "View"};
        int x = contentX;
        for (String item : items) {
            int width = Math.max(36, net.minecraft.client.MinecraftClient.getInstance().textRenderer.getWidth(item) + 14);
            if (isInside(mouseX, mouseY, x, contentY, width, 14)) {
                this.openMenu = this.openMenu.equals(item) ? "" : item;
                return true;
            }
            x += width + 4;
        }
        if (!this.openMenu.isEmpty()) {
            String[] options = optionsFor(this.openMenu);
            for (int index = 0; index < options.length; index++) {
                int rowY = contentY + 16 + index * 16;
                if (isInside(mouseX, mouseY, contentX, rowY, DROPDOWN_WIDTH, 14)) {
                    runOption(this.openMenu, index);
                    this.openMenu = "";
                    return true;
                }
            }
        }
        this.openMenu = "";
        return false;
    }

    private void renderDropdown(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, int x, int y) {
        String[] options = optionsFor(this.openMenu);
        int height = options.length * 16 + 4;
        context.fill(x, y, x + DROPDOWN_WIDTH, y + height, 0xF0141A22);
        context.drawBorder(x, y, DROPDOWN_WIDTH, height, EditorPalette.PANEL_BORDER);
        for (int index = 0; index < options.length; index++) {
            int rowY = y + 2 + index * 16;
            boolean hovered = isInside(mouseX, mouseY, x + 2, rowY, DROPDOWN_WIDTH - 4, 14);
            context.fill(x + 2, rowY, x + DROPDOWN_WIDTH - 2, rowY + 14, hovered ? 0xFF263244 : 0x00000000);
            context.drawText(textRenderer, Text.literal(options[index]), x + 6, rowY + 3, EditorPalette.TEXT, false);
        }
    }

    private String[] optionsFor(String menu) {
        return switch (menu) {
            case "File" -> new String[]{"Save", "Save As", "Import", "Export"};
            case "Edit" -> new String[]{"Undo", "Redo", "Delete Selected", "Duplicate Selected"};
            case "Tools" -> new String[]{"Translate Gizmo", "Rotate Gizmo", "Save Blueprint", "Place Saved Blueprint"};
            case "View" -> new String[]{
                    toggleLabel("Browser", this.state.browserVisible()),
                    toggleLabel("Inspector", this.state.inspectorVisible()),
                    toggleLabel("Timeline", this.state.timelineVisible()),
                    toggleLabel("World Text", this.state.worldTextVisible()),
                    toggleLabel("Debug Bounds", this.state.debugBoundsVisible())
            };
            default -> new String[0];
        };
    }

    private void runOption(String menu, int index) {
        switch (menu) {
            case "File" -> {
                if (index == 0) {
                    this.state.save();
                } else if (index == 1) {
                    this.state.saveAsExport();
                } else if (index == 2) {
                    this.state.importProject();
                } else if (index == 3) {
                    this.state.exportBlueprintLibrary();
                }
            }
            case "Edit" -> {
                if (index == 0) {
                    this.state.undo();
                } else if (index == 1) {
                    this.state.redo();
                } else if (index == 2) {
                    this.state.deleteSelection();
                } else if (index == 3) {
                    this.state.duplicateSelection();
                }
            }
            case "Tools" -> {
                if (index == 0) {
                    this.state.setToolMode(net.dark.editorapi.client.state.EditorToolMode.TRANSLATE);
                } else if (index == 1) {
                    this.state.setToolMode(net.dark.editorapi.client.state.EditorToolMode.ROTATE);
                } else if (index == 2) {
                    this.state.saveSelectionAsBlueprint();
                } else if (index == 3) {
                    this.state.instantiateFirstBlueprint();
                }
            }
            case "View" -> {
                if (index == 0) {
                    this.state.setBrowserVisible(!this.state.browserVisible());
                } else if (index == 1) {
                    this.state.setInspectorVisible(!this.state.inspectorVisible());
                } else if (index == 2) {
                    this.state.setTimelineVisible(!this.state.timelineVisible());
                } else if (index == 3) {
                    this.state.setWorldTextVisible(!this.state.worldTextVisible());
                } else if (index == 4) {
                    this.state.setDebugBoundsVisible(!this.state.debugBoundsVisible());
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean contains(double mouseX, double mouseY) {
        if (super.contains(mouseX, mouseY)) {
            return true;
        }
        return !this.openMenu.isEmpty() && mouseX >= 6 && mouseX <= 6 + DROPDOWN_WIDTH && mouseY >= 17 && mouseY <= 17 + optionsFor(this.openMenu).length * 16 + 4;
    }

    private String toggleLabel(String name, boolean enabled) {
        return (enabled ? "Hide " : "Show ") + name;
    }
}
