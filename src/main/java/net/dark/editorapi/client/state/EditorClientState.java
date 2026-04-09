package net.dark.editorapi.client.state;

import java.util.UUID;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.EditorProject;
import net.dark.editorapi.model.InterpolationMode;
import net.dark.editorapi.model.TriggerZone;
import net.dark.editorapi.runtime.EditorRuntime;
import net.dark.editorapi.serialization.EditorProjectStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class EditorClientState {
    private final EditorProjectStore store = new EditorProjectStore();
    private EditorProject project;
    private EditorRuntime runtime;
    private boolean editorOpen;
    private EditorSelection selection = EditorSelection.NONE;
    private int timelineFrame;

    public EditorClientState() {
        reload();
    }

    public void reload() {
        this.project = this.store.loadOrCreate();
        this.runtime = new EditorRuntime(this, this.project);
        this.timelineFrame = 0;
    }

    public void save() {
        this.store.save(this.project);
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
    }

    public EditorSelection selection() {
        return this.selection;
    }

    public void setSelection(EditorSelection selection) {
        this.selection = selection;
    }

    public int timelineFrame() {
        return this.timelineFrame;
    }

    public void setTimelineFrame(int timelineFrame) {
        this.timelineFrame = Math.max(0, timelineFrame);
        this.runtime.cutscenes().setPreviewFrame(this.timelineFrame);
    }

    public TriggerZone selectedZone() {
        return switch (this.selection.type()) {
            case ZONE, POS1, POS2 -> this.project.zones().get(this.selection.objectId());
            default -> null;
        };
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
        TriggerZone zone = this.project.createZone("Zone " + (this.project.zones().size() + 1));
        setSelection(EditorSelection.zone(zone.id()));
        return zone;
    }

    public EditorEventDefinition createEvent() {
        EditorEventDefinition event = this.project.createEvent("Event " + (this.project.events().size() + 1));
        setSelection(EditorSelection.event(event.id()));
        return event;
    }

    public CutsceneDefinition createCutscene() {
        CutsceneDefinition cutscene = this.project.createCutscene("Cutscene " + (this.project.cutscenes().size() + 1));
        setSelection(EditorSelection.cutscene(cutscene.id()));
        return cutscene;
    }

    public void capturePos1() {
        TriggerZone zone = selectedZone();
        if (zone == null) {
            zone = createZone();
        }
        zone.setPos1(resolveTargetBlock());
        setSelection(EditorSelection.pos1(zone.id()));
    }

    public void capturePos2() {
        TriggerZone zone = selectedZone();
        if (zone == null) {
            zone = createZone();
        }
        zone.setPos2(resolveTargetBlock());
        setSelection(EditorSelection.pos2(zone.id()));
    }

    public void addCurrentCameraKeyframe() {
        CutsceneDefinition cutscene = selectedCutscene();
        MinecraftClient client = MinecraftClient.getInstance();
        if (cutscene == null || client.gameRenderer == null) {
            return;
        }

        Camera camera = client.gameRenderer.getCamera();
        CutsceneKeyframe keyframe = new CutsceneKeyframe(
                UUID.randomUUID(),
                this.timelineFrame,
                camera.getPos(),
                camera.getYaw(),
                camera.getPitch(),
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

    public void addActionToSelectedEvent(net.minecraft.util.Identifier actionId) {
        EditorEventDefinition event = selectedEvent();
        if (event == null) {
            return;
        }

        var definition = EditorActionRegistry.getInstance().get(actionId);
        if (definition == null) {
            return;
        }

        EditorActionInstance instance = new EditorActionInstance(actionId, definition.createDefaultData());
        if (actionId.equals(BuiltinEditorActions.PLAY_CUTSCENE) && !this.project.cutscenes().isEmpty()) {
            instance.data().addProperty("cutsceneId", this.project.cutscenes().keySet().iterator().next().toString());
        }
        event.actions().add(instance);
    }

    public void tick() {
        this.runtime.tick();
    }

    private BlockPos resolveTargetBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.crosshairTarget instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getBlockPos();
        }
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.MISS && client.player != null) {
            Vec3d pos = client.player.getPos();
            return BlockPos.ofFloored(pos);
        }
        return client.player == null ? BlockPos.ORIGIN : client.player.getBlockPos();
    }
}
