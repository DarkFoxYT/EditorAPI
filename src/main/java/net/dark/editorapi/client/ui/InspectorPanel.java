package net.dark.editorapi.client.ui;

import java.util.List;
import java.util.function.Consumer;
import net.dark.editorapi.api.action.EditorActionDefinition;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.client.ui.widget.EditorWidgets;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.InterpolationMode;
import net.dark.editorapi.model.TriggerOnceMode;
import net.dark.editorapi.model.TriggerTargetMode;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public final class InspectorPanel extends EditorPanel {
    private final EditorClientState state;
    private InspectorTab activeTab = InspectorTab.GENERAL;
    private String lastClickKey = "";
    private long lastClickTime;
    private String editingKey = "";
    private String editingValue = "";
    private Consumer<String> editingCommit;
    private String activeOptionKey = "";
    private String[] optionValues = new String[0];
    private Consumer<String> optionCommit;

    public InspectorPanel(EditorClientState state, int x, int y, int width, int height) {
        super("Inspector", x, y, width, height);
        this.state = state;
    }

    @Override
    protected void renderContent(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY, float delta, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(44, (contentWidth - 12) / 4);
        drawButton(context, textRenderer, contentX, contentY, tabWidth, 14, "General", isInside(mouseX, mouseY, contentX, contentY, tabWidth, 14), this.activeTab == InspectorTab.GENERAL);
        drawButton(context, textRenderer, contentX + tabWidth + 4, contentY, tabWidth, 14, "Camera", isInside(mouseX, mouseY, contentX + tabWidth + 4, contentY, tabWidth, 14), this.activeTab == InspectorTab.CAMERA);
        drawButton(context, textRenderer, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14, "Player", isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14), this.activeTab == InspectorTab.PLAYER);
        drawButton(context, textRenderer, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14, "Events", isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14), this.activeTab == InspectorTab.EVENTS);

        int y = contentY + 20;
        switch (this.state.selection().type()) {
            case BLUEPRINT -> y = renderBlueprint(context, textRenderer, y, contentX);
            case ZONE, POS1, POS2 -> y = renderZone(context, textRenderer, y, contentX);
            case EVENT -> y = renderEvent(context, textRenderer, y, contentX);
            case CUTSCENE, KEYFRAME -> y = renderCutscene(context, textRenderer, y, contentX);
            default -> drawHint(context, textRenderer, contentX, y, "Select a zone, event, or cutscene");
        }
    }

    @Override
    protected boolean onMouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
        int tabWidth = Math.max(44, (contentWidth - 12) / 4);
        if (isInside(mouseX, mouseY, contentX, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.GENERAL;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + tabWidth + 4, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.CAMERA;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 2, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.PLAYER;
            return true;
        }
        if (isInside(mouseX, mouseY, contentX + (tabWidth + 4) * 3, contentY, tabWidth, 14)) {
            this.activeTab = InspectorTab.EVENTS;
            return true;
        }

        int rowY = contentY + 20;
        switch (this.state.selection().type()) {
            case BLUEPRINT -> {
                var blueprint = this.state.selectedBlueprint();
                if (blueprint == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("blueprint.name")) {
                    startTextEdit("blueprint.name", blueprint.name(), blueprint::setName);
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 18) && doubleClick("blueprint.origin")) {
                    startTextEdit("blueprint.origin", formatVec(blueprint.origin().x, blueprint.origin().y, blueprint.origin().z), value -> applyVec3(value, blueprint::setOrigin));
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 36) && doubleClick("blueprint.yaw")) {
                    startTextEdit("blueprint.yaw", String.format("%.1f", blueprint.yaw()), value -> applyFloat(value, blueprint::setYaw));
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 54)) {
                    blueprint.setVisible(!blueprint.visible());
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 72)) {
                    blueprint.setLocked(!blueprint.locked());
                    return true;
                }
            }
            case ZONE, POS1, POS2 -> {
                TriggerZone zone = this.state.selectedZone();
                if (zone == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL) {
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("zone.name")) {
                        startTextEdit("zone.name", zone.name(), zone::setName);
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("zone.pos1")) {
                        startTextEdit("zone.pos1", formatPos(zone.pos1()), value -> applyBlockPos(value, zone::setPos1));
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("zone.pos2")) {
                        startTextEdit("zone.pos2", formatPos(zone.pos2()), value -> applyBlockPos(value, zone::setPos2));
                        return true;
                    }
                    rowY += 36;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setVisible(!zone.visible());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setLocked(!zone.locked());
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.PLAYER) {
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerEnter(!zone.triggerEnter());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerExit(!zone.triggerExit());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerWhileInside(!zone.triggerWhileInside());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        zone.setTriggerTimeInside(!zone.triggerTimeInside());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("zone.radius")) {
                        startTextEdit("zone.radius", String.format("%.1f", zone.radius()), value -> applyFloat(value, zone::setRadius));
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.EVENTS) {
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("zone.delay")) {
                        startTextEdit("zone.delay", Integer.toString(zone.delayTicks()), value -> applyInt(value, zone::setDelayTicks));
                        return true;
                    }
                    if (handleOptionClick(mouseX, mouseY, contentX, rowY + 18, "zone.once")) {
                        return true;
                    }
                    if (clickRow(mouseX, mouseY, contentX, rowY + 18) && doubleClick("zone.once")) {
                        startOptions("zone.once", enumNames(TriggerOnceMode.values()), value -> zone.setOnceMode(TriggerOnceMode.valueOf(value)));
                        return true;
                    }
                    if (handleOptionClick(mouseX, mouseY, contentX, rowY + 36, "zone.target")) {
                        return true;
                    }
                    if (clickRow(mouseX, mouseY, contentX, rowY + 36) && doubleClick("zone.target")) {
                        startOptions("zone.target", enumNames(TriggerTargetMode.values()), value -> zone.setTargetMode(TriggerTargetMode.valueOf(value)));
                        return true;
                    }
                    if (clickRow(mouseX, mouseY, contentX, rowY + 54) && doubleClick("zone.enterEvent")) {
                        startTextEdit("zone.enterEvent", this.state.describeEventReference(zone.enterEventId()), value -> zone.setEnterEventId(this.state.findEventIdByReference(value)));
                        return true;
                    }
                    if (clickRow(mouseX, mouseY, contentX, rowY + 72) && doubleClick("zone.exitEvent")) {
                        startTextEdit("zone.exitEvent", this.state.describeEventReference(zone.exitEventId()), value -> zone.setExitEventId(this.state.findEventIdByReference(value)));
                        return true;
                    }
                }
            }
            case EVENT -> {
                EditorEventDefinition event = this.state.selectedEvent();
                if (event == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 18)) {
                    event.setVisible(!event.visible());
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("event.name")) {
                    startTextEdit("event.name", event.name(), event::setName);
                    return true;
                }
                if (this.activeTab == InspectorTab.GENERAL && clickRow(mouseX, mouseY, contentX, rowY + 36)) {
                    event.setLocked(!event.locked());
                    return true;
                }
            }
            case CUTSCENE, KEYFRAME -> {
                CutsceneDefinition cutscene = this.state.selectedCutscene();
                if (cutscene == null) {
                    return false;
                }
                if (this.activeTab == InspectorTab.GENERAL) {
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("cutscene.name")) {
                        startTextEdit("cutscene.name", cutscene.name(), cutscene::setName);
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY) && doubleClick("cutscene.frames")) {
                        startTextEdit("cutscene.frames", cutscene.startFrame() + "," + cutscene.endFrame(), value -> applyFrameRange(value, cutscene));
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setVisible(!cutscene.visible());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setLocked(!cutscene.locked());
                        return true;
                    }
                } else if (this.activeTab == InspectorTab.CAMERA) {
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setLoop(!cutscene.loop());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setShowPreview(!cutscene.showPreview());
                        return true;
                    }
                    rowY += 18;
                    if (clickRow(mouseX, mouseY, contentX, rowY)) {
                        cutscene.setAutoKeyframe(!cutscene.autoKeyframe());
                        return true;
                    }
                    rowY += 18;
                    if (this.state.selection().type() == EditorSelectionType.KEYFRAME && clickRow(mouseX, mouseY, contentX, rowY)) {
                        CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                        if (keyframe != null) {
                            if (handleOptionClick(mouseX, mouseY, contentX, rowY, "key.interpolation")) {
                                return true;
                            }
                            if (doubleClick("key.interpolation")) {
                                startOptions("key.interpolation", interpolationNames(), value -> setKeyInterpolation(cutscene, keyframe.id(), value));
                            } else {
                                int index = cutscene.keyframes().indexOf(keyframe);
                                cutscene.keyframes().set(index, keyframe.withInterpolation(keyframe.interpolation().next()));
                            }
                            return true;
                        }
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private int renderZone(DrawContext context, TextRenderer textRenderer, int y, int x) {
        TriggerZone zone = this.state.selectedZone();
        if (zone == null) {
            return y;
        }
        return switch (this.activeTab) {
            case GENERAL -> {
                y = drawEditableValue(context, textRenderer, x, y, "zone.name", "Name", zone.name());
                y = drawEditableValue(context, textRenderer, x, y, "zone.pos1", "Pos1", formatPos(zone.pos1()));
                y = drawEditableValue(context, textRenderer, x, y, "zone.pos2", "Pos2", formatPos(zone.pos2()));
                y = drawValue(context, textRenderer, x, y, "Mode", zone.onceMode().name() + " / " + zone.targetMode().name());
                y = drawToggle(context, textRenderer, x, y, "Visible", zone.visible());
                y = drawToggle(context, textRenderer, x, y, "Locked", zone.locked());
                yield y;
            }
            case PLAYER -> {
                y = drawToggle(context, textRenderer, x, y, "On Enter", zone.triggerEnter());
                y = drawToggle(context, textRenderer, x, y, "On Exit", zone.triggerExit());
                y = drawToggle(context, textRenderer, x, y, "While Inside", zone.triggerWhileInside());
                y = drawToggle(context, textRenderer, x, y, "Timed Trigger", zone.triggerTimeInside());
                y = drawEditableValue(context, textRenderer, x, y, "zone.radius", "Radius", String.format("%.1f", zone.radius()));
                yield y;
            }
            case EVENTS -> {
                EditorEventDefinition linkedEvent = this.state.project().events().get(zone.eventId());
                y = drawEditableValue(context, textRenderer, x, y, "zone.delay", "Delay", zone.delayTicks() + " ticks");
                y = drawEditableValue(context, textRenderer, x, y, "zone.once", "Once", zone.onceMode().name());
                y = drawEditableValue(context, textRenderer, x, y, "zone.target", "Target", zone.targetMode().name());
                y = drawEditableValue(context, textRenderer, x, y, "zone.enterEvent", "On Enter Event", this.state.describeEventReference(zone.enterEventId()));
                y = drawEditableValue(context, textRenderer, x, y, "zone.exitEvent", "On Exit Event", this.state.describeEventReference(zone.exitEventId()));
                y = drawValue(context, textRenderer, x, y, "Actions", linkedEvent == null ? "0" : String.valueOf(linkedEvent.actions().size()));
                drawHint(context, textRenderer, x, y + 4, "Double click event rows, then type an event name or UUID");
                yield y;
            }
            case CAMERA -> {
                drawHint(context, textRenderer, x, y, "Zone selection has no camera settings");
                yield y;
            }
        };
    }

    private int renderEvent(DrawContext context, TextRenderer textRenderer, int y, int x) {
        EditorEventDefinition event = this.state.selectedEvent();
        if (event == null) {
            return y;
        }
        if (this.activeTab == InspectorTab.GENERAL) {
            y = drawEditableValue(context, textRenderer, x, y, "event.name", "Event", event.name());
            y = drawToggle(context, textRenderer, x, y, "Visible", event.visible());
            y = drawToggle(context, textRenderer, x, y, "Locked", event.locked());
            return y;
        }
        if (this.activeTab == InspectorTab.EVENTS) {
            for (EditorActionInstance action : event.actions()) {
                EditorActionDefinition definition = EditorActionRegistry.getInstance().get(action.actionId());
                y = drawValue(context, textRenderer, x, y, definition == null ? action.actionId().toString() : definition.displayName(), summarize(definition, action));
            }
            y = drawHintRow(context, textRenderer, x, y, "Action list / sound / particle / cutscene routing");
            return y;
        }
        drawHint(context, textRenderer, x, y, "No data for this tab on the selected event");
        return y;
    }

    private int renderCutscene(DrawContext context, TextRenderer textRenderer, int y, int x) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null) {
            return y;
        }
        return switch (this.activeTab) {
            case GENERAL -> {
                y = drawEditableValue(context, textRenderer, x, y, "cutscene.name", "Cutscene", cutscene.name());
                y = drawEditableValue(context, textRenderer, x, y, "cutscene.frames", "Frames", cutscene.startFrame() + " -> " + cutscene.endFrame());
                y = drawToggle(context, textRenderer, x, y, "Visible", cutscene.visible());
                y = drawToggle(context, textRenderer, x, y, "Locked", cutscene.locked());
                yield y;
            }
            case CAMERA -> {
                y = drawToggle(context, textRenderer, x, y, "Loop", cutscene.loop());
                y = drawToggle(context, textRenderer, x, y, "Preview", cutscene.showPreview());
                y = drawToggle(context, textRenderer, x, y, "Auto Key", cutscene.autoKeyframe());
                if (this.state.selection().type() == EditorSelectionType.KEYFRAME) {
                    CutsceneKeyframe keyframe = cutscene.keyframes().stream().filter(item -> item.id().equals(this.state.selection().childId())).findFirst().orElse(null);
                    if (keyframe != null) {
                        y = drawEditableValue(context, textRenderer, x, y, "key.frame", "Key", keyframe.frame() + " / " + keyframe.interpolation().label());
                        y = drawEditableValue(context, textRenderer, x, y, "key.pos", "Pos", formatVec(keyframe.position().x, keyframe.position().y, keyframe.position().z));
                        y = drawEditableValue(context, textRenderer, x, y, "key.rot", "Rot", String.format("%.1f / %.1f", keyframe.yaw(), keyframe.pitch()));
                    }
                }
                yield y;
            }
            case PLAYER -> {
                y = drawValue(context, textRenderer, x, y, "Playback", cutscene.loop() ? "Loop" : "One Shot");
                y = drawValue(context, textRenderer, x, y, "Preview", cutscene.showPreview() ? "Camera Preview" : "Hidden");
                y = drawValue(context, textRenderer, x, y, "Path Keys", Integer.toString(cutscene.keyframes().size()));
                yield y;
            }
            case EVENTS -> {
                y = drawValue(context, textRenderer, x, y, "Linked Blueprint", this.state.selectedBlueprint() == null ? "Standalone" : this.state.selectedBlueprint().name());
                drawHint(context, textRenderer, x, y, "Path/keyframe and cue hooks can be expanded here");
                yield y;
            }
        };
    }

    private int renderBlueprint(DrawContext context, TextRenderer textRenderer, int y, int x) {
        var blueprint = this.state.selectedBlueprint();
        if (blueprint == null) {
            return y;
        }
        return switch (this.activeTab) {
            case GENERAL -> {
                y = drawEditableValue(context, textRenderer, x, y, "blueprint.name", "Blueprint", blueprint.name());
                y = drawEditableValue(context, textRenderer, x, y, "blueprint.origin", "Origin", formatVec(blueprint.origin().x, blueprint.origin().y, blueprint.origin().z));
                y = drawEditableValue(context, textRenderer, x, y, "blueprint.yaw", "Yaw", String.format("%.1f", blueprint.yaw()));
                y = drawToggle(context, textRenderer, x, y, "Visible", blueprint.visible());
                y = drawToggle(context, textRenderer, x, y, "Locked", blueprint.locked());
                yield y;
            }
            case CAMERA -> {
                y = drawValue(context, textRenderer, x, y, "Zones", Integer.toString(blueprint.zoneIds().size()));
                y = drawValue(context, textRenderer, x, y, "Cuts", Integer.toString(blueprint.cutsceneIds().size()));
                yield y;
            }
            case PLAYER -> {
                y = drawValue(context, textRenderer, x, y, "Transform", "Global move/rotate");
                y = drawValue(context, textRenderer, x, y, "Snap", "Minecraft grid");
                yield y;
            }
            case EVENTS -> {
                y = drawValue(context, textRenderer, x, y, "Saved Asset", blueprint.assetId().toString().substring(0, 8));
                y = drawValue(context, textRenderer, x, y, "Event Count", Integer.toString(blueprint.eventIds().size()));
                yield y;
            }
        };
    }

    private int drawValue(DrawContext context, TextRenderer textRenderer, int x, int y, String label, String value) {
        EditorWidgets.drawRow(context, textRenderer, x, y, contentWidth(), label, value, false, false);
        return y + 18;
    }

    private int drawToggle(DrawContext context, TextRenderer textRenderer, int x, int y, String label, boolean value) {
        return drawValue(context, textRenderer, x, y, label, value ? "ON" : "OFF");
    }

    private void drawHint(DrawContext context, TextRenderer textRenderer, int x, int y, String value) {
        context.drawText(textRenderer, Text.literal(value), x, y, 0xFF90A1BD, false);
    }

    private int drawHintRow(DrawContext context, TextRenderer textRenderer, int x, int y, String value) {
        drawHint(context, textRenderer, x, y + 4, value);
        return y + 18;
    }

    private boolean clickRow(double mouseX, double mouseY, int contentX, int y) {
        return isInside(mouseX, mouseY, contentX, y, contentWidth(), 16);
    }

    private boolean doubleClick(String key) {
        long now = System.currentTimeMillis();
        boolean match = this.lastClickKey.equals(key) && now - this.lastClickTime < 350L;
        this.lastClickKey = key;
        this.lastClickTime = now;
        return match;
    }

    @Override
    protected boolean onKeyPressed(int keyCode, int scanCode, int modifiers, int contentX, int contentY, int contentWidth, int contentHeight) {
        if (!this.editingKey.isBlank()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                clearEditing();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                commitEditing();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.editingValue.isEmpty()) {
                    this.editingValue = this.editingValue.substring(0, this.editingValue.length() - 1);
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                this.editingValue = "";
                return true;
            }
            String typed = keyToText(keyCode, modifiers);
            if (!typed.isEmpty()) {
                this.editingValue += typed;
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean onCharTyped(char chr, int modifiers, int contentX, int contentY, int contentWidth, int contentHeight) {
        if (this.editingKey.isBlank()) {
            return false;
        }
        if (Character.isISOControl(chr)) {
            return false;
        }
        this.editingValue += chr;
        return true;
    }

    private int drawEditableValue(DrawContext context, TextRenderer textRenderer, int x, int y, String key, String label, String value) {
        String shown = this.editingKey.equals(key) ? this.editingValue + "_" : value;
        EditorWidgets.drawRow(context, textRenderer, x, y, contentWidth(), label, shown, false, this.editingKey.equals(key) || this.optionKey().equals(key));
        if (this.optionKey().equals(key)) {
            for (int index = 0; index < this.optionValues.length; index++) {
                EditorWidgets.drawRow(context, textRenderer, x + 8, y + 18 + index * 18, contentWidth() - 8, ">", this.optionValues[index], false, false);
            }
            return y + 18 + this.optionValues.length * 18;
        }
        return y + 18;
    }

    private void startTextEdit(String key, String initialValue, Consumer<String> commit) {
        this.editingKey = key;
        this.editingValue = initialValue;
        this.editingCommit = commit;
        this.activeOptionKey = "";
        this.optionValues = new String[0];
        this.optionCommit = null;
    }

    private void startOptions(String key, String[] values, Consumer<String> commit) {
        this.editingKey = "";
        this.editingValue = "";
        this.editingCommit = null;
        this.activeOptionKey = key;
        this.optionValues = values;
        this.optionCommit = commit;
    }

    private void commitEditing() {
        if (this.editingCommit != null) {
            this.editingCommit.accept(this.editingValue.trim());
        }
        clearEditing();
    }

    private void clearEditing() {
        this.editingKey = "";
        this.editingValue = "";
        this.editingCommit = null;
        this.activeOptionKey = "";
        this.optionValues = new String[0];
        this.optionCommit = null;
    }

    private String optionKey() {
        return this.optionValues.length == 0 ? "" : this.activeOptionKey;
    }

    private boolean handleOptionClick(double mouseX, double mouseY, int contentX, int baseY, String key) {
        if (!this.activeOptionKey.equals(key) || this.optionCommit == null) {
            return false;
        }
        for (int index = 0; index < this.optionValues.length; index++) {
            int rowY = baseY + 18 + index * 18;
            if (isInside(mouseX, mouseY, contentX + 8, rowY, contentWidth() - 8, 16)) {
                this.optionCommit.accept(this.optionValues[index]);
                clearEditing();
                return true;
            }
        }
        return false;
    }

    private void applyInt(String raw, Consumer<Integer> setter) {
        try {
            setter.accept(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyFloat(String raw, Consumer<Float> setter) {
        try {
            setter.accept(Float.parseFloat(raw.trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyVec3(String raw, Consumer<net.minecraft.util.math.Vec3d> setter) {
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return;
        }
        try {
            setter.accept(new net.minecraft.util.math.Vec3d(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()), Double.parseDouble(parts[2].trim())));
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyBlockPos(String raw, Consumer<BlockPos> setter) {
        String[] parts = raw.split(",");
        if (parts.length != 3) {
            return;
        }
        try {
            setter.accept(new BlockPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException ignored) {
        }
    }

    private void applyFrameRange(String raw, CutsceneDefinition cutscene) {
        String[] parts = raw.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            cutscene.setStartFrame(Integer.parseInt(parts[0].trim()));
            cutscene.setEndFrame(Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    private String[] enumNames(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            names[index] = values[index].name();
        }
        return names;
    }

    private String[] interpolationNames() {
        InterpolationMode[] modes = InterpolationMode.values();
        String[] names = new String[modes.length];
        for (int index = 0; index < modes.length; index++) {
            names[index] = modes[index].name();
        }
        return names;
    }

    private void setKeyInterpolation(CutsceneDefinition cutscene, java.util.UUID keyId, String value) {
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
            if (keyframe.id().equals(keyId)) {
                cutscene.keyframes().set(index, keyframe.withInterpolation(InterpolationMode.valueOf(value)));
                return;
            }
        }
    }

    private String keyToText(int keyCode, int modifiers) {
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            char base = (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
            return String.valueOf(shift ? Character.toUpperCase(base) : base);
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_KP_0)));
        }
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> " ";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_KP_DECIMAL -> ".";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "-";
            case GLFW.GLFW_KEY_KP_ADD -> "+";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_SEMICOLON -> shift ? ":" : ";";
            default -> "";
        };
    }

    private String summarize(EditorActionDefinition definition, EditorActionInstance action) {
        if (definition == null) {
            return action.actionId().toString();
        }
        List<String> lines = definition.summarize(action.data());
        return lines.isEmpty() ? definition.displayName() : String.join(" | ", lines);
    }

    private String formatPos(BlockPos pos) {
        return formatVec(pos.getX(), pos.getY(), pos.getZ());
    }

    private String formatVec(double x, double y, double z) {
        return String.format("%.2f, %.2f, %.2f", x, y, z);
    }

    private enum InspectorTab {
        GENERAL,
        CAMERA,
        PLAYER,
        EVENTS
    }
}
