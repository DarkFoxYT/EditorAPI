package net.dark.editorapi.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import com.google.gson.JsonObject;
import net.minecraft.util.math.BlockPos;

public final class EditorProject {
    private final Map<UUID, EditorBlueprintAsset> blueprintAssets = new LinkedHashMap<>();
    private final Map<UUID, PlacedBlueprint> blueprints = new LinkedHashMap<>();
    private final Map<UUID, TriggerZone> zones = new LinkedHashMap<>();
    private final Map<UUID, EditorEventDefinition> events = new LinkedHashMap<>();
    private final Map<UUID, CutsceneDefinition> cutscenes = new LinkedHashMap<>();

    public Map<UUID, EditorBlueprintAsset> blueprintAssets() {
        return this.blueprintAssets;
    }

    public Map<UUID, PlacedBlueprint> blueprints() {
        return this.blueprints;
    }

    public Map<UUID, TriggerZone> zones() {
        return this.zones;
    }

    public Map<UUID, EditorEventDefinition> events() {
        return this.events;
    }

    public Map<UUID, CutsceneDefinition> cutscenes() {
        return this.cutscenes;
    }

    public TriggerZone createZone(String name) {
        EditorEventDefinition event = createEvent(name + " Event");
        TriggerZone zone = TriggerZone.createDefault(name, event.id());
        this.zones.put(zone.id(), zone);
        return zone;
    }

    public EditorEventDefinition createEvent(String name) {
        var titleData = EditorActionRegistry.getInstance().get(BuiltinEditorActions.SHOW_TITLE).createDefaultData();
        EditorEventDefinition event = new EditorEventDefinition(
                UUID.randomUUID(),
                name,
                List.of(new EditorActionInstance(BuiltinEditorActions.SHOW_TITLE, titleData))
        );
        this.events.put(event.id(), event);
        return event;
    }

    public CutsceneDefinition createCutscene(String name) {
        CutsceneDefinition cutscene = CutsceneDefinition.createDefault(name);
        this.cutscenes.put(cutscene.id(), cutscene);
        return cutscene;
    }

    public void ensureBootstrapData() {
        if (!this.cutscenes.isEmpty() || !this.zones.isEmpty() || !this.events.isEmpty() || !this.blueprints.isEmpty()) {
            return;
        }

        CutsceneDefinition openingCutscene = createCutscene("Opening Test Cutscene");
        openingCutscene.keyframes().add(new CutsceneKeyframe(UUID.randomUUID(), 0, BlockPos.ORIGIN.toCenterPos().add(0.0D, 2.0D, 0.0D), 0.0F, 10.0F, 0.0F, 70.0F, 0.0F, InterpolationMode.SMOOTH));
        openingCutscene.keyframes().add(new CutsceneKeyframe(UUID.randomUUID(), 60, BlockPos.ORIGIN.toCenterPos().add(6.0D, 3.0D, 6.0D), -135.0F, 15.0F, 0.0F, 80.0F, 0.15F, InterpolationMode.EASE_IN_OUT));
        CutsceneDefinition closingCutscene = createCutscene("Closing Test Cutscene");
        closingCutscene.keyframes().add(new CutsceneKeyframe(UUID.randomUUID(), 0, BlockPos.ORIGIN.toCenterPos().add(6.0D, 3.0D, 6.0D), -135.0F, 15.0F, 0.0F, 80.0F, 0.0F, InterpolationMode.SMOOTH));
        closingCutscene.keyframes().add(new CutsceneKeyframe(UUID.randomUUID(), 40, BlockPos.ORIGIN.toCenterPos().add(0.0D, 2.0D, 0.0D), 0.0F, 10.0F, 0.0F, 70.0F, 0.0F, InterpolationMode.EASE_IN_OUT));
        EditorEventDefinition enterEvent = createEvent("Opening Test Event");
        enterEvent.actions().clear();
        JsonObject enterData = EditorActionRegistry.getInstance().get(BuiltinEditorActions.PLAY_CUTSCENE).createDefaultData();
        enterData.addProperty("cutsceneId", openingCutscene.id().toString());
        enterEvent.actions().add(new EditorActionInstance(BuiltinEditorActions.PLAY_CUTSCENE, enterData));
        EditorEventDefinition exitEvent = createEvent("Closing Test Event");
        exitEvent.actions().clear();
        JsonObject exitData = EditorActionRegistry.getInstance().get(BuiltinEditorActions.PLAY_CUTSCENE).createDefaultData();
        exitData.addProperty("cutsceneId", closingCutscene.id().toString());
        exitEvent.actions().add(new EditorActionInstance(BuiltinEditorActions.PLAY_CUTSCENE, exitData));
        TriggerZone zone = createZone("Demo Trigger");
        zone.setEnterEventId(enterEvent.id());
        zone.setExitEventId(exitEvent.id());
        zone.setEventId(enterEvent.id());
    }
}
