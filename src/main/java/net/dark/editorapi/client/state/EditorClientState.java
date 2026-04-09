package net.dark.editorapi.client.state;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorBlueprintAsset;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.EditorProject;
import net.dark.editorapi.model.InterpolationMode;
import net.dark.editorapi.model.PlacedBlueprint;
import net.dark.editorapi.model.TriggerZone;
import net.dark.editorapi.network.ProjectSyncRequestPayload;
import net.dark.editorapi.runtime.EditorRuntime;
import net.dark.editorapi.runtime.EditorRuntimeMode;
import net.dark.editorapi.serialization.EditorProjectStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class EditorClientState {
    private static final int MAX_HISTORY = 48;

    private final EditorProjectStore store = new EditorProjectStore();
    private final Deque<String> undoHistory = new ArrayDeque<>();
    private final Deque<String> redoHistory = new ArrayDeque<>();
    private EditorProject project;
    private EditorRuntime runtime;
    private boolean editorOpen;
    private boolean mouseInteraction;
    private boolean browserVisible = true;
    private boolean inspectorVisible = true;
    private boolean timelineVisible = true;
    private boolean worldTextVisible = true;
    private boolean debugBoundsVisible = true;
    private boolean cameraNavigating;
    private EditorSelection selection = EditorSelection.NONE;
    private int timelineFrame;
    private EditorToolMode toolMode = EditorToolMode.TRANSLATE;
    private GizmoDrag activeDrag;
    private String lastSavedSnapshot = "";
    private int autosaveCooldown;

    public EditorClientState() {
        reload();
    }

    public void reload() {
        this.project = this.store.loadOrCreate();
        this.runtime = new EditorRuntime(this, this.project);
        this.timelineFrame = 0;
        this.selection = EditorSelection.NONE;
        this.toolMode = EditorToolMode.TRANSLATE;
        this.activeDrag = null;
        this.lastSavedSnapshot = this.store.serialize(this.project);
        this.autosaveCooldown = 0;
        this.undoHistory.clear();
        this.redoHistory.clear();
    }

    public void save() {
        this.store.save(this.project);
        this.lastSavedSnapshot = this.store.serialize(this.project);
        this.autosaveCooldown = 0;
    }

    public void saveAsExport() {
        writeProjectFile(resolveWorkspaceFile("editor-project-export.json"));
    }

    public void importProject() {
        Path path = resolveWorkspaceFile("editor-project-import.json");
        if (!Files.exists(path)) {
            return;
        }
        try {
            applyProject(this.store.deserialize(Files.readString(path)));
        } catch (IOException ignored) {
        }
    }

    public void exportBlueprintLibrary() {
        writeProjectFile(resolveWorkspaceFile("editor-blueprints-export.json"));
    }

    public EditorProject project() {
        return this.project;
    }

    public EditorRuntime runtime() {
        return this.runtime;
    }

    public boolean editorOpen() {
        return this.editorOpen;
    }

    public void setEditorOpen(boolean editorOpen) {
        this.editorOpen = editorOpen;
        this.runtime.setMode(editorOpen ? EditorRuntimeMode.EDITOR : EditorRuntimeMode.RUNTIME);
        if (!editorOpen) {
            this.mouseInteraction = false;
            this.activeDrag = null;
            this.cameraNavigating = false;
        }
    }

    public boolean mouseInteraction() {
        return this.mouseInteraction;
    }

    public void setMouseInteraction(boolean mouseInteraction) {
        this.mouseInteraction = mouseInteraction && this.editorOpen;
        if (!this.mouseInteraction) {
            this.activeDrag = null;
        }
    }

    public void toggleMouseInteraction() {
        setMouseInteraction(!this.mouseInteraction);
    }

    public EditorSelection selection() {
        return this.selection;
    }

    public void setSelection(EditorSelection selection) {
        this.selection = selection;
    }

    public boolean browserVisible() {
        return this.browserVisible;
    }

    public void setBrowserVisible(boolean browserVisible) {
        this.browserVisible = browserVisible;
    }

    public boolean inspectorVisible() {
        return this.inspectorVisible;
    }

    public void setInspectorVisible(boolean inspectorVisible) {
        this.inspectorVisible = inspectorVisible;
    }

    public boolean timelineVisible() {
        return this.timelineVisible;
    }

    public void setTimelineVisible(boolean timelineVisible) {
        this.timelineVisible = timelineVisible;
    }

    public boolean worldTextVisible() {
        return this.worldTextVisible;
    }

    public void setWorldTextVisible(boolean worldTextVisible) {
        this.worldTextVisible = worldTextVisible;
    }

    public boolean debugBoundsVisible() {
        return this.debugBoundsVisible;
    }

    public void setDebugBoundsVisible(boolean debugBoundsVisible) {
        this.debugBoundsVisible = debugBoundsVisible;
    }

    public boolean cameraNavigating() {
        return this.cameraNavigating;
    }

    public void setCameraNavigating(boolean cameraNavigating) {
        this.cameraNavigating = cameraNavigating;
    }

    public int timelineFrame() {
        return this.timelineFrame;
    }

    public void setTimelineFrame(int timelineFrame) {
        this.timelineFrame = Math.max(0, timelineFrame);
        this.runtime.cutscenes().setPreviewFrame(this.timelineFrame);
    }

    public EditorToolMode toolMode() {
        return this.toolMode;
    }

    public void setToolMode(EditorToolMode toolMode) {
        this.toolMode = toolMode;
    }

    public boolean canUndo() {
        return !this.undoHistory.isEmpty();
    }

    public boolean canRedo() {
        return !this.redoHistory.isEmpty();
    }

    public void undo() {
        if (this.undoHistory.isEmpty()) {
            return;
        }
        this.redoHistory.push(this.store.serialize(this.project));
        applyProject(this.store.deserialize(this.undoHistory.pop()));
    }

    public void redo() {
        if (this.redoHistory.isEmpty()) {
            return;
        }
        this.undoHistory.push(this.store.serialize(this.project));
        applyProject(this.store.deserialize(this.redoHistory.pop()));
    }

    public TriggerZone selectedZone() {
        return switch (this.selection.type()) {
            case ZONE, POS1, POS2 -> this.project.zones().get(this.selection.objectId());
            default -> null;
        };
    }

    public PlacedBlueprint selectedBlueprint() {
        return this.selection.type() == EditorSelectionType.BLUEPRINT ? this.project.blueprints().get(this.selection.objectId()) : null;
    }

    public EditorEventDefinition selectedEvent() {
        return this.selection.type() == EditorSelectionType.EVENT ? this.project.events().get(this.selection.objectId()) : null;
    }

    public CutsceneDefinition selectedCutscene() {
        return switch (this.selection.type()) {
            case CUTSCENE, KEYFRAME -> this.project.cutscenes().get(this.selection.objectId());
            default -> null;
        };
    }

    public TriggerZone createZone() {
        pushUndoState();
        TriggerZone zone = this.project.createZone("Zone " + (this.project.zones().size() + 1));
        setSelection(EditorSelection.zone(zone.id()));
        return zone;
    }

    public EditorEventDefinition createEvent() {
        pushUndoState();
        EditorEventDefinition event = this.project.createEvent("Event " + (this.project.events().size() + 1));
        setSelection(EditorSelection.event(event.id()));
        return event;
    }

    public CutsceneDefinition createCutscene() {
        pushUndoState();
        CutsceneDefinition cutscene = this.project.createCutscene("Cutscene " + (this.project.cutscenes().size() + 1));
        setSelection(EditorSelection.cutscene(cutscene.id()));
        return cutscene;
    }

    public void capturePos1() {
        TriggerZone zone = selectedZone();
        if (zone == null) {
            zone = createZone();
        }
        pushUndoState();
        zone.setPos1(resolveTargetBlock());
        setSelection(EditorSelection.pos1(zone.id()));
    }

    public void capturePos2() {
        TriggerZone zone = selectedZone();
        if (zone == null) {
            zone = createZone();
        }
        pushUndoState();
        zone.setPos2(resolveTargetBlock());
        setSelection(EditorSelection.pos2(zone.id()));
    }

    public void addCurrentCameraKeyframe() {
        CutsceneDefinition cutscene = selectedCutscene();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cutscene == null || client.gameRenderer == null) {
            return;
        }

        pushUndoState();
        Camera camera = client.gameRenderer.getCamera();
        CutsceneKeyframe keyframe = new CutsceneKeyframe(
                UUID.randomUUID(),
                this.timelineFrame,
                camera.getPos(),
                camera.getYaw(),
                camera.getPitch(),
                0.0F,
                client.options.getFov().getValue(),
                0.0F,
                InterpolationMode.SMOOTH
        );
        cutscene.keyframes().add(keyframe);
        cutscene.sortKeyframes();
        setSelection(EditorSelection.keyframe(cutscene.id(), keyframe.id()));
    }

    public void upsertAutoKeyframeFromCamera() {
        CutsceneDefinition cutscene = selectedCutscene();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cutscene == null || !cutscene.autoKeyframe() || client.gameRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe existing = cutscene.keyframes().get(index);
            if (existing.frame() == this.timelineFrame) {
                cutscene.keyframes().set(index, existing.withTransform(camera.getPos(), camera.getYaw(), camera.getPitch()));
                return;
            }
        }

        addCurrentCameraKeyframe();
    }

    public void addActionToSelectedEvent(Identifier actionId) {
        EditorEventDefinition event = selectedEvent();
        if (event == null) {
            return;
        }

        var definition = EditorActionRegistry.getInstance().get(actionId);
        if (definition == null) {
            return;
        }

        pushUndoState();
        EditorActionInstance instance = new EditorActionInstance(actionId, definition.createDefaultData());
        if (actionId.equals(BuiltinEditorActions.PLAY_CUTSCENE) && !this.project.cutscenes().isEmpty()) {
            instance.data().addProperty("cutsceneId", this.project.cutscenes().keySet().iterator().next().toString());
        }
        event.actions().add(instance);
    }

    public void tick() {
        this.runtime.tick();
        if (this.runtime.cutscenes().isPlaying()) {
            this.timelineFrame = (int) Math.floor(this.runtime.cutscenes().previewFrame());
        }
        tickAutosave();
    }

    public boolean pickWorldSelection(double mouseX, double mouseY) {
        PickResult pick = pickSelectable(mouseX, mouseY);
        if (pick == null) {
            return false;
        }
        setSelection(pick.selection());
        return true;
    }

    public void deleteSelectedKeyframeInTimeline() {
        if (this.selection.type() != EditorSelectionType.KEYFRAME) {
            return;
        }
        pushUndoState();
        deleteSelectedKeyframe();
    }

    public boolean beginGizmoDrag(double mouseX, double mouseY) {
        if (this.cameraNavigating) {
            return false;
        }
        PickResult handlePick = pickGizmo(mouseX, mouseY);
        if (handlePick == null) {
            return false;
        }

        pushUndoState();
        Vec3d rayOrigin = cameraOrigin();
        Vec3d rayDirection = screenRayDirection(mouseX, mouseY);
        Vec3d axis = axisVector(handlePick.axis());
        if (handlePick.rotation()) {
            Vec3d hit = rayPlaneIntersection(rayOrigin, rayDirection, handlePick.origin(), axis);
            if (hit == null) {
                return false;
            }
            this.activeDrag = GizmoDrag.rotation(handlePick.axis(), handlePick.origin(), hit.subtract(handlePick.origin()));
            return true;
        }

        Vec3d planeNormal = translationPlaneNormal(axis, rayDirection);
        Vec3d hit = rayPlaneIntersection(rayOrigin, rayDirection, handlePick.origin(), planeNormal);
        if (hit == null) {
            return false;
        }
        this.activeDrag = GizmoDrag.translation(handlePick.axis(), handlePick.origin(), planeNormal, axis.dotProduct(hit.subtract(handlePick.origin())));
        return true;
    }

    public boolean updateGizmoDrag(double mouseX, double mouseY) {
        if (this.activeDrag == null || this.cameraNavigating) {
            return false;
        }

        Vec3d rayOrigin = cameraOrigin();
        Vec3d rayDirection = screenRayDirection(mouseX, mouseY);
        Vec3d axis = axisVector(this.activeDrag.axis());
        if (this.activeDrag.rotation()) {
            Vec3d hit = rayPlaneIntersection(rayOrigin, rayDirection, this.activeDrag.origin(), axis);
            if (hit == null) {
                return false;
            }
            Vec3d current = hit.subtract(this.activeDrag.origin()).normalize();
            if (current.lengthSquared() < 1.0E-4D || this.activeDrag.startVector().lengthSquared() < 1.0E-4D) {
                return false;
            }
            applyRotation(this.selection, this.activeDrag.axis(), signedAngle(this.activeDrag.startVector(), current, axis));
            return true;
        }

        Vec3d hit = rayPlaneIntersection(rayOrigin, rayDirection, this.activeDrag.origin(), this.activeDrag.planeNormal());
        if (hit == null) {
            return false;
        }
        applyTranslation(this.selection, this.activeDrag.axis(), axis.dotProduct(hit.subtract(this.activeDrag.origin())) - this.activeDrag.startProjection());
        return true;
    }

    public void endGizmoDrag() {
        this.activeDrag = null;
    }

    public boolean hasActiveDrag() {
        return this.activeDrag != null;
    }

    public void deleteSelection() {
        if (this.selection.type() == EditorSelectionType.NONE) {
            return;
        }
        pushUndoState();
        switch (this.selection.type()) {
            case BLUEPRINT -> this.project.blueprints().remove(this.selection.objectId());
            case ZONE, POS1, POS2 -> this.project.zones().remove(this.selection.objectId());
            case EVENT -> this.project.events().remove(this.selection.objectId());
            case CUTSCENE -> this.project.cutscenes().remove(this.selection.objectId());
            case KEYFRAME -> deleteSelectedKeyframe();
            default -> {
            }
        }
        this.selection = EditorSelection.NONE;
    }

    public void duplicateSelection() {
        switch (this.selection.type()) {
            case BLUEPRINT -> duplicateBlueprint();
            case ZONE, POS1, POS2 -> duplicateZone();
            case CUTSCENE, KEYFRAME -> duplicateCutscene();
            case EVENT -> duplicateEvent();
            default -> {
            }
        }
    }

    public void nudgeSelection(double x, double y, double z) {
        pushUndoState();
        switch (this.selection.type()) {
            case BLUEPRINT -> moveBlueprint(this.project.blueprints().get(this.selection.objectId()), new Vec3d(x, y, z));
            case ZONE -> moveZone(this.project.zones().get(this.selection.objectId()), x, y, z);
            case POS1 -> {
                TriggerZone zone = selectedZone();
                zone.setPos1(zone.pos1().add((int) x, (int) y, (int) z));
            }
            case POS2 -> {
                TriggerZone zone = selectedZone();
                zone.setPos2(zone.pos2().add((int) x, (int) y, (int) z));
            }
            case KEYFRAME -> moveKeyframe(this.selection.objectId(), this.selection.childId(), x, y, z);
            default -> {
            }
        }
    }

    public void saveSelectionAsBlueprint() {
        switch (this.selection.type()) {
            case BLUEPRINT -> saveBlueprintFromPlaced(this.project.blueprints().get(this.selection.objectId()));
            case ZONE, POS1, POS2 -> saveBlueprintFromZone(this.project.zones().get(this.selection.objectId()));
            case CUTSCENE, KEYFRAME -> saveBlueprintFromCutscene(this.project.cutscenes().get(this.selection.objectId()));
            default -> {
            }
        }
    }

    public void instantiateFirstBlueprint() {
        EditorBlueprintAsset asset = this.project.blueprintAssets().values().stream().findFirst().orElse(null);
        if (asset == null) {
            return;
        }
        instantiateBlueprint(asset, resolveTargetBlock().toCenterPos());
    }

    public void instantiateBlueprint(EditorBlueprintAsset asset, Vec3d origin) {
        pushUndoState();
        List<UUID> zoneIds = new ArrayList<>();
        List<UUID> eventIds = new ArrayList<>();
        List<UUID> cutsceneIds = new ArrayList<>();
        Vec3d sourceOrigin = asset.previewBounds().getCenter();
        for (EditorEventDefinition template : asset.events()) {
            EditorEventDefinition copy = copyEvent(template, template.name());
            this.project.events().put(copy.id(), copy);
            eventIds.add(copy.id());
        }
        for (TriggerZone template : asset.zones()) {
            UUID linkedEvent = remapEventId(asset.events(), eventIds, template.eventId());
            TriggerZone copy = copyZone(template, template.name(), linkedEvent);
            moveZoneTo(copy, template.center().add(origin.subtract(sourceOrigin)));
            this.project.zones().put(copy.id(), copy);
            zoneIds.add(copy.id());
        }
        for (CutsceneDefinition template : asset.cutscenes()) {
            CutsceneDefinition copy = copyCutscene(template, template.name());
            moveCutscene(copy, origin.x - sourceOrigin.x, origin.y - sourceOrigin.y, origin.z - sourceOrigin.z);
            this.project.cutscenes().put(copy.id(), copy);
            cutsceneIds.add(copy.id());
        }
        PlacedBlueprint placed = new PlacedBlueprint(UUID.randomUUID(), asset.id(), asset.name(), origin, 0.0F, true, false, zoneIds, eventIds, cutsceneIds);
        this.project.blueprints().put(placed.id(), placed);
        this.selection = EditorSelection.blueprint(placed.id());
    }

    public void requestProjectSync() {
        ClientPlayNetworking.send(new ProjectSyncRequestPayload());
    }

    public String mouseModeLabel() {
        return this.mouseInteraction ? "Editor Mouse" : "Gameplay";
    }

    public UUID findEventIdByReference(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = rawValue.trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
        }
        return this.project.events().values().stream()
                .filter(event -> event.name().equalsIgnoreCase(value))
                .map(EditorEventDefinition::id)
                .findFirst()
                .orElse(null);
    }

    public String describeEventReference(UUID eventId) {
        if (eventId == null) {
            return "";
        }
        EditorEventDefinition event = this.project.events().get(eventId);
        return event == null ? eventId.toString() : event.name();
    }

    private void deleteSelectedKeyframe() {
        CutsceneDefinition cutscene = selectedCutscene();
        if (cutscene != null) {
            cutscene.keyframes().removeIf(keyframe -> keyframe.id().equals(this.selection.childId()));
        }
    }

    private void duplicateZone() {
        TriggerZone source = selectedZone();
        if (source == null) {
            return;
        }
        pushUndoState();
        EditorEventDefinition sourceEvent = this.project.events().get(source.eventId());
        EditorEventDefinition eventCopy = sourceEvent == null ? createDetachedDefaultEvent(source.name() + " Event Copy") : copyEvent(sourceEvent, sourceEvent.name() + " Copy");
        this.project.events().put(eventCopy.id(), eventCopy);
        TriggerZone zoneCopy = copyZone(source, source.name() + " Copy", eventCopy.id());
        moveZone(zoneCopy, 1.0D, 0.0D, 1.0D);
        this.project.zones().put(zoneCopy.id(), zoneCopy);
        this.selection = EditorSelection.zone(zoneCopy.id());
    }

    private void duplicateEvent() {
        EditorEventDefinition event = selectedEvent();
        if (event == null) {
            return;
        }
        pushUndoState();
        EditorEventDefinition copy = copyEvent(event, event.name() + " Copy");
        this.project.events().put(copy.id(), copy);
        this.selection = EditorSelection.event(copy.id());
    }

    private void duplicateCutscene() {
        CutsceneDefinition cutscene = selectedCutscene();
        if (cutscene == null) {
            return;
        }
        pushUndoState();
        CutsceneDefinition copy = copyCutscene(cutscene, cutscene.name() + " Copy");
        moveCutscene(copy, 1.0D, 0.0D, 1.0D);
        this.project.cutscenes().put(copy.id(), copy);
        this.selection = EditorSelection.cutscene(copy.id());
    }

    private void duplicateBlueprint() {
        PlacedBlueprint blueprint = selectedBlueprint();
        if (blueprint == null) {
            return;
        }
        EditorBlueprintAsset asset = this.project.blueprintAssets().get(blueprint.assetId());
        if (asset != null) {
            instantiateBlueprint(asset, blueprint.origin().add(2.0D, 0.0D, 2.0D));
        }
    }

    private void saveBlueprintFromZone(TriggerZone zone) {
        if (zone == null) {
            return;
        }
        pushUndoState();
        List<EditorEventDefinition> events = new ArrayList<>();
        EditorEventDefinition linkedEvent = this.project.events().get(zone.eventId());
        if (linkedEvent != null) {
            events.add(copyEvent(linkedEvent, linkedEvent.name()));
        }
        EditorBlueprintAsset asset = new EditorBlueprintAsset(
                UUID.randomUUID(),
                zone.name() + " Blueprint",
                List.of(copyZone(zone, zone.name(), linkedEvent == null ? zone.eventId() : events.getFirst().id())),
                events,
                List.of()
        );
        this.project.blueprintAssets().put(asset.id(), asset);
    }

    private void saveBlueprintFromCutscene(CutsceneDefinition cutscene) {
        if (cutscene == null) {
            return;
        }
        pushUndoState();
        EditorBlueprintAsset asset = new EditorBlueprintAsset(UUID.randomUUID(), cutscene.name() + " Blueprint", List.of(), List.of(), List.of(copyCutscene(cutscene, cutscene.name())));
        this.project.blueprintAssets().put(asset.id(), asset);
    }

    private void saveBlueprintFromPlaced(PlacedBlueprint blueprint) {
        if (blueprint == null) {
            return;
        }
        pushUndoState();
        List<TriggerZone> zones = new ArrayList<>();
        List<EditorEventDefinition> events = new ArrayList<>();
        List<CutsceneDefinition> cutscenes = new ArrayList<>();
        for (UUID eventId : blueprint.eventIds()) {
            EditorEventDefinition event = this.project.events().get(eventId);
            if (event != null) {
                events.add(copyEvent(event, event.name()));
            }
        }
        for (UUID zoneId : blueprint.zoneIds()) {
            TriggerZone zone = this.project.zones().get(zoneId);
            if (zone != null) {
                zones.add(copyZone(zone, zone.name(), remapEventIdByName(events, this.project.events().get(zone.eventId()))));
            }
        }
        for (UUID cutsceneId : blueprint.cutsceneIds()) {
            CutsceneDefinition cutscene = this.project.cutscenes().get(cutsceneId);
            if (cutscene != null) {
                cutscenes.add(copyCutscene(cutscene, cutscene.name()));
            }
        }
        EditorBlueprintAsset asset = new EditorBlueprintAsset(UUID.randomUUID(), blueprint.name() + " Asset", zones, events, cutscenes);
        this.project.blueprintAssets().put(asset.id(), asset);
    }

    private void moveBlueprint(PlacedBlueprint blueprint, Vec3d delta) {
        if (blueprint == null) {
            return;
        }
        blueprint.setOrigin(blueprint.origin().add(delta));
        for (UUID zoneId : blueprint.zoneIds()) {
            moveZone(this.project.zones().get(zoneId), delta.x, delta.y, delta.z);
        }
        for (UUID cutsceneId : blueprint.cutsceneIds()) {
            moveCutscene(this.project.cutscenes().get(cutsceneId), delta.x, delta.y, delta.z);
        }
    }

    private void moveZone(TriggerZone zone, double x, double y, double z) {
        if (zone == null) {
            return;
        }
        zone.setPos1(zone.pos1().add((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)));
        zone.setPos2(zone.pos2().add((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)));
    }

    private void moveZoneTo(TriggerZone zone, Vec3d targetCenter) {
        Vec3d delta = targetCenter.subtract(zone.center());
        moveZone(zone, delta.x, delta.y, delta.z);
    }

    private void moveCutscene(CutsceneDefinition cutscene, double x, double y, double z) {
        if (cutscene == null) {
            return;
        }
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
            cutscene.keyframes().set(index, keyframe.withTransform(keyframe.position().add(x, y, z), keyframe.yaw(), keyframe.pitch()));
        }
    }

    private void moveKeyframe(UUID cutsceneId, UUID keyframeId, double x, double y, double z) {
        CutsceneDefinition cutscene = this.project.cutscenes().get(cutsceneId);
        if (cutscene == null) {
            return;
        }
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
            if (keyframe.id().equals(keyframeId)) {
                cutscene.keyframes().set(index, keyframe.withTransform(keyframe.position().add(x, y, z), keyframe.yaw(), keyframe.pitch()));
                return;
            }
        }
    }

    private void applyTranslation(EditorSelection selection, GizmoAxis axis, double delta) {
        Vec3d movement = axisVector(axis).multiply(delta);
        if (movement.lengthSquared() < 1.0E-5D) {
            return;
        }
        Vec3d snapped = new Vec3d(Math.round(movement.x), Math.round(movement.y), Math.round(movement.z));
        switch (selection.type()) {
            case BLUEPRINT -> moveBlueprint(this.project.blueprints().get(selection.objectId()), snapped);
            case ZONE -> moveZone(this.project.zones().get(selection.objectId()), snapped.x, snapped.y, snapped.z);
            case POS1 -> selectedZone().setPos1(BlockPos.ofFloored(selectedZone().pos1().toCenterPos().add(snapped)));
            case POS2 -> selectedZone().setPos2(BlockPos.ofFloored(selectedZone().pos2().toCenterPos().add(snapped)));
            case KEYFRAME -> moveKeyframe(selection.objectId(), selection.childId(), movement.x, movement.y, movement.z);
            default -> {
            }
        }
    }

    private void applyRotation(EditorSelection selection, GizmoAxis axis, double radians) {
        float degrees = (float) Math.toDegrees(radians);
        switch (selection.type()) {
            case BLUEPRINT -> rotateBlueprint(this.project.blueprints().get(selection.objectId()), axis, degrees);
            case KEYFRAME -> rotateKeyframe(selection.objectId(), selection.childId(), axis, degrees);
            default -> {
            }
        }
    }

    private void rotateBlueprint(PlacedBlueprint blueprint, GizmoAxis axis, float degrees) {
        if (blueprint == null || axis != GizmoAxis.Y) {
            return;
        }
        float snapped = Math.round(degrees / 15.0F) * 15.0F;
        float delta = snapped - blueprint.yaw();
        if (Math.abs(delta) < 0.01F) {
            return;
        }
        blueprint.setYaw(snapped);
        Vec3d origin = blueprint.origin();
        for (UUID zoneId : blueprint.zoneIds()) {
            TriggerZone zone = this.project.zones().get(zoneId);
            if (zone != null) {
                zone.setPos1(BlockPos.ofFloored(rotateVec(zone.pos1().toCenterPos(), origin, delta)));
                zone.setPos2(BlockPos.ofFloored(rotateVec(zone.pos2().toCenterPos(), origin, delta)));
            }
        }
        for (UUID cutsceneId : blueprint.cutsceneIds()) {
            CutsceneDefinition cutscene = this.project.cutscenes().get(cutsceneId);
            if (cutscene == null) {
                continue;
            }
            for (int index = 0; index < cutscene.keyframes().size(); index++) {
                CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
                cutscene.keyframes().set(index, keyframe.withTransform(rotateVec(keyframe.position(), origin, delta), keyframe.yaw() + delta, keyframe.pitch()));
            }
        }
    }

    private void rotateKeyframe(UUID cutsceneId, UUID keyframeId, GizmoAxis axis, float degrees) {
        CutsceneDefinition cutscene = this.project.cutscenes().get(cutsceneId);
        if (cutscene == null) {
            return;
        }
        for (int index = 0; index < cutscene.keyframes().size(); index++) {
            CutsceneKeyframe keyframe = cutscene.keyframes().get(index);
            if (!keyframe.id().equals(keyframeId)) {
                continue;
            }
            float yaw = keyframe.yaw();
            float pitch = keyframe.pitch();
            float roll = keyframe.roll();
            if (axis == GizmoAxis.Y) {
                yaw += degrees;
            } else if (axis == GizmoAxis.X) {
                pitch = MathHelper.clamp(pitch + degrees, -89.0F, 89.0F);
            } else if (axis == GizmoAxis.Z) {
                roll += degrees;
            }
            cutscene.keyframes().set(index, new CutsceneKeyframe(keyframe.id(), keyframe.frame(), keyframe.position(), yaw, pitch, roll, keyframe.fov(), keyframe.sway(), keyframe.interpolation()));
            return;
        }
    }

    private PickResult pickSelectable(double mouseX, double mouseY) {
        Vec3d origin = cameraOrigin();
        Vec3d direction = screenRayDirection(mouseX, mouseY);
        PickResult best = null;
        for (PlacedBlueprint blueprint : this.project.blueprints().values()) {
            if (blueprint.visible()) {
                best = closer(best, tryPick(EditorSelection.blueprint(blueprint.id()), blueprint.origin(), origin, direction));
            }
        }
        for (TriggerZone zone : this.project.zones().values()) {
            if (!zone.visible()) {
                continue;
            }
            best = closer(best, tryPick(EditorSelection.pos1(zone.id()), zone.pos1().toCenterPos(), origin, direction));
            best = closer(best, tryPick(EditorSelection.pos2(zone.id()), zone.pos2().toCenterPos(), origin, direction));
            best = closer(best, tryPick(EditorSelection.zone(zone.id()), zone.center(), origin, direction));
        }
        for (CutsceneDefinition cutscene : this.project.cutscenes().values()) {
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                best = closer(best, tryPick(EditorSelection.keyframe(cutscene.id(), keyframe.id()), keyframe.position(), origin, direction));
            }
        }
        return best;
    }

    private PickResult tryPick(EditorSelection selection, Vec3d point, Vec3d origin, Vec3d direction) {
        double distance = distanceToRay(point, origin, direction);
        return distance < selectionThreshold(origin, point) ? new PickResult(selection, point, GizmoAxis.NONE, false, distance) : null;
    }

    private PickResult pickGizmo(double mouseX, double mouseY) {
        if (this.selection.type() == EditorSelectionType.NONE) {
            return null;
        }
        Vec3d origin = gizmoOrigin();
        if (origin == null) {
            return null;
        }
        Vec3d rayOrigin = cameraOrigin();
        Vec3d rayDirection = screenRayDirection(mouseX, mouseY);
        PickResult best = null;
        double scale = gizmoScale(origin);
        for (GizmoAxis axis : List.of(GizmoAxis.X, GizmoAxis.Y, GizmoAxis.Z)) {
            double axisDistance = sampledAxisDistance(rayOrigin, rayDirection, origin, axisVector(axis), scale);
            if (axisDistance < Math.max(0.12D, scale * 0.28D)) {
                best = closer(best, new PickResult(this.selection, origin, axis, false, axisDistance));
            }
            if (this.toolMode == EditorToolMode.ROTATE) {
                double ringDistance = sampledRingDistance(rayOrigin, rayDirection, origin, axis, scale * 0.9D);
                if (ringDistance < Math.max(0.1D, scale * 0.18D)) {
                    best = closer(best, new PickResult(this.selection, origin, axis, true, ringDistance));
                }
            }
        }
        return best;
    }

    private PickResult closer(PickResult current, PickResult candidate) {
        return current == null || candidate != null && candidate.distance() < current.distance() ? candidate : current;
    }

    private Vec3d gizmoOrigin() {
        return switch (this.selection.type()) {
            case BLUEPRINT -> selectedBlueprint() == null ? null : selectedBlueprint().origin();
            case ZONE -> selectedZone() == null ? null : selectedZone().center();
            case POS1 -> selectedZone() == null ? null : selectedZone().pos1().toCenterPos();
            case POS2 -> selectedZone() == null ? null : selectedZone().pos2().toCenterPos();
            case KEYFRAME -> selectedCutscene() == null ? null : selectedCutscene().keyframes().stream().filter(item -> item.id().equals(this.selection.childId())).findFirst().map(CutsceneKeyframe::position).orElse(null);
            default -> null;
        };
    }

    private double ringDistance(Vec3d rayOrigin, Vec3d rayDirection, Vec3d ringOrigin, GizmoAxis axis, double radius) {
        Vec3d hit = rayPlaneIntersection(rayOrigin, rayDirection, ringOrigin, axisVector(axis));
        return hit == null ? Double.MAX_VALUE : Math.abs(hit.distanceTo(ringOrigin) - radius);
    }

    private double sampledAxisDistance(Vec3d rayOrigin, Vec3d rayDirection, Vec3d origin, Vec3d axis, double scale) {
        double best = Double.MAX_VALUE;
        for (int sample = 1; sample <= 8; sample++) {
            Vec3d point = origin.add(axis.multiply((scale * sample) / 8.0D));
            best = Math.min(best, distanceToRay(point, rayOrigin, rayDirection));
        }
        return best;
    }

    private double sampledRingDistance(Vec3d rayOrigin, Vec3d rayDirection, Vec3d origin, GizmoAxis axis, double radius) {
        double best = Double.MAX_VALUE;
        for (int sample = 0; sample < 24; sample++) {
            double angle = (Math.PI * 2.0D * sample) / 24.0D;
            Vec3d point = switch (axis) {
                case X -> origin.add(0.0D, Math.cos(angle) * radius, Math.sin(angle) * radius);
                case Y -> origin.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                case Z -> origin.add(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0D);
                default -> origin;
            };
            best = Math.min(best, distanceToRay(point, rayOrigin, rayDirection));
        }
        return best;
    }

    private double distanceToRay(Vec3d point, Vec3d origin, Vec3d direction) {
        Vec3d toPoint = point.subtract(origin);
        double projection = Math.max(0.0D, toPoint.dotProduct(direction));
        return origin.add(direction.multiply(projection)).distanceTo(point);
    }

    private double selectionThreshold(Vec3d origin, Vec3d point) {
        double distance = Math.max(1.0D, origin.distanceTo(point));
        return Math.min(0.7D, 0.14D + distance * 0.016D);
    }

    private double gizmoScale(Vec3d origin) {
        double distance = Math.max(1.0D, cameraOrigin().distanceTo(origin));
        return Math.min(1.8D, 0.62D + distance * 0.04D);
    }

    private Vec3d cameraOrigin() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.gameRenderer == null ? Vec3d.ZERO : client.gameRenderer.getCamera().getPos();
    }

    private Vec3d screenRayDirection(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.gameRenderer == null) {
            return new Vec3d(0.0D, 0.0D, 1.0D);
        }
        Camera camera = client.gameRenderer.getCamera();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        double nx = (mouseX / width) * 2.0D - 1.0D;
        double ny = 1.0D - (mouseY / height) * 2.0D;
        double aspect = width / (double) Math.max(1, height);
        double tanFov = Math.tan(Math.toRadians(client.options.getFov().getValue() * 0.5D));
        Vec3d forward = directionFromAngles(camera.getYaw(), camera.getPitch());
        Vec3d right = forward.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D)).normalize().multiply(-1.0D);
        Vec3d up = right.crossProduct(forward).normalize();
        return forward.add(right.multiply(nx * tanFov * aspect)).add(up.multiply(ny * tanFov)).normalize();
    }

    private Vec3d translationPlaneNormal(Vec3d axis, Vec3d cameraDirection) {
        Vec3d projected = cameraDirection.subtract(axis.multiply(cameraDirection.dotProduct(axis)));
        if (projected.lengthSquared() < 1.0E-4D) {
            projected = axis.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
        }
        return projected.normalize();
    }

    private Vec3d rayPlaneIntersection(Vec3d rayOrigin, Vec3d rayDirection, Vec3d planePoint, Vec3d planeNormal) {
        double denominator = planeNormal.dotProduct(rayDirection);
        if (Math.abs(denominator) < 1.0E-5D) {
            return null;
        }
        double t = planeNormal.dotProduct(planePoint.subtract(rayOrigin)) / denominator;
        return t < 0.0D ? null : rayOrigin.add(rayDirection.multiply(t));
    }

    private double signedAngle(Vec3d from, Vec3d to, Vec3d axis) {
        double dot = MathHelper.clamp(from.normalize().dotProduct(to.normalize()), -1.0D, 1.0D);
        double angle = Math.acos(dot);
        double sign = Math.signum(axis.dotProduct(from.crossProduct(to)));
        return angle * (sign == 0.0D ? 1.0D : sign);
    }

    private Vec3d axisVector(GizmoAxis axis) {
        return switch (axis) {
            case X -> new Vec3d(1.0D, 0.0D, 0.0D);
            case Y -> new Vec3d(0.0D, 1.0D, 0.0D);
            case Z -> new Vec3d(0.0D, 0.0D, 1.0D);
            default -> Vec3d.ZERO;
        };
    }

    private UUID remapEventId(List<EditorEventDefinition> templates, List<UUID> instantiatedIds, UUID originalEventId) {
        for (int index = 0; index < templates.size(); index++) {
            if (templates.get(index).id().equals(originalEventId)) {
                return instantiatedIds.get(index);
            }
        }
        return originalEventId;
    }

    private UUID remapEventIdByName(List<EditorEventDefinition> copiedEvents, EditorEventDefinition original) {
        if (original == null) {
            return UUID.randomUUID();
        }
        return copiedEvents.stream().filter(event -> event.name().equals(original.name())).map(EditorEventDefinition::id).findFirst().orElse(original.id());
    }

    private TriggerZone copyZone(TriggerZone zone, String name, UUID eventId) {
        UUID enterEventId = zone.enterEventId().equals(zone.eventId()) ? eventId : zone.enterEventId();
        UUID exitEventId = zone.exitEventId().equals(zone.eventId()) ? eventId : zone.exitEventId();
        return new TriggerZone(UUID.randomUUID(), name, zone.pos1(), zone.pos2(), zone.showFill(), zone.triggerEnter(), zone.triggerExit(), zone.triggerWhileInside(), zone.triggerTimeInside(), zone.whileInsideIntervalTicks(), zone.requiredTimeTicks(), zone.delayTicks(), zone.radius(), zone.onceMode(), zone.targetMode(), eventId, enterEventId, exitEventId, zone.visible(), zone.locked());
    }

    private EditorEventDefinition copyEvent(EditorEventDefinition event, String name) {
        List<EditorActionInstance> actions = new ArrayList<>();
        for (EditorActionInstance action : event.actions()) {
            actions.add(new EditorActionInstance(action.actionId(), action.data().deepCopy()));
        }
        return new EditorEventDefinition(UUID.randomUUID(), name, actions, event.visible(), event.locked());
    }

    private EditorEventDefinition createDetachedDefaultEvent(String name) {
        JsonObject data = EditorActionRegistry.getInstance().get(BuiltinEditorActions.SHOW_TITLE).createDefaultData();
        return new EditorEventDefinition(UUID.randomUUID(), name, List.of(new EditorActionInstance(BuiltinEditorActions.SHOW_TITLE, data)));
    }

    private CutsceneDefinition copyCutscene(CutsceneDefinition cutscene, String name) {
        List<CutsceneKeyframe> keys = new ArrayList<>();
        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            keys.add(new CutsceneKeyframe(UUID.randomUUID(), keyframe.frame(), keyframe.position(), keyframe.yaw(), keyframe.pitch(), keyframe.roll(), keyframe.fov(), keyframe.sway(), keyframe.interpolation()));
        }
        return new CutsceneDefinition(UUID.randomUUID(), name, cutscene.startFrame(), cutscene.endFrame(), cutscene.loop(), cutscene.showPreview(), cutscene.autoKeyframe(), cutscene.visible(), cutscene.locked(), keys);
    }

    private void pushUndoState() {
        this.undoHistory.push(this.store.serialize(this.project));
        while (this.undoHistory.size() > MAX_HISTORY) {
            this.undoHistory.removeLast();
        }
        this.redoHistory.clear();
    }

    private void applyProject(EditorProject project) {
        this.project = project;
        this.runtime = new EditorRuntime(this, this.project);
        this.runtime.setMode(this.editorOpen ? EditorRuntimeMode.EDITOR : EditorRuntimeMode.RUNTIME);
        this.selection = EditorSelection.NONE;
        this.activeDrag = null;
        this.lastSavedSnapshot = this.store.serialize(this.project);
        this.autosaveCooldown = 0;
    }

    private void tickAutosave() {
        String snapshot = this.store.serialize(this.project);
        if (snapshot.equals(this.lastSavedSnapshot)) {
            this.autosaveCooldown = 0;
            return;
        }
        if (this.autosaveCooldown > 0) {
            this.autosaveCooldown--;
            return;
        }
        this.store.save(this.project);
        this.lastSavedSnapshot = snapshot;
        this.autosaveCooldown = 10;
    }

    private Path resolveWorkspaceFile(String fileName) {
        return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve(EditorConstants.MOD_ID).resolve(fileName);
    }

    private void writeProjectFile(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, this.store.serialize(this.project));
        } catch (IOException ignored) {
        }
    }

    private Vec3d rotateVec(Vec3d position, Vec3d origin, float yawDegrees) {
        double radians = Math.toRadians(yawDegrees);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        Vec3d local = position.subtract(origin);
        return new Vec3d(local.x * cos - local.z * sin, local.y, local.x * sin + local.z * cos).add(origin);
    }

    private Vec3d directionFromAngles(float yaw, float pitch) {
        float yawRad = (float) Math.toRadians(-yaw) - (float) Math.PI;
        float pitchRad = (float) Math.toRadians(-pitch);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        return new Vec3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch).normalize();
    }

    private BlockPos resolveTargetBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.MISS && client.player != null) {
            return BlockPos.ofFloored(client.player.getPos());
        }
        return client.player == null ? BlockPos.ORIGIN : client.player.getBlockPos();
    }

    private record PickResult(EditorSelection selection, Vec3d origin, GizmoAxis axis, boolean rotation, double distance) {
    }

    private record GizmoDrag(GizmoAxis axis, Vec3d origin, Vec3d planeNormal, double startProjection, Vec3d startVector, boolean rotation) {
        private static GizmoDrag translation(GizmoAxis axis, Vec3d origin, Vec3d planeNormal, double startProjection) {
            return new GizmoDrag(axis, origin, planeNormal, startProjection, Vec3d.ZERO, false);
        }

        private static GizmoDrag rotation(GizmoAxis axis, Vec3d origin, Vec3d startVector) {
            return new GizmoDrag(axis, origin, Vec3d.ZERO, 0.0D, startVector.normalize(), true);
        }
    }
}
