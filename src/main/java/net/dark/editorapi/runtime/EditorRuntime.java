package net.dark.editorapi.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.dark.editorapi.api.action.EditorActionContext;
import net.dark.editorapi.api.action.EditorActionDefinition;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.EditorProject;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.MinecraftClient;

public final class EditorRuntime {
    private final EditorClientState editorState;
    private final EditorProject project;
    private final ZoneRuntimeSystem zones;
    private final CutscenePlaybackController cutscenes;
    private final List<ScheduledEvent> scheduledEvents = new ArrayList<>();

    public EditorRuntime(EditorClientState editorState, EditorProject project) {
        this.editorState = editorState;
        this.project = project;
        this.zones = new ZoneRuntimeSystem(this.editorState, this);
        this.cutscenes = new CutscenePlaybackController(project);
    }

    public ZoneRuntimeSystem zones() {
        return this.zones;
    }

    public CutscenePlaybackController cutscenes() {
        return this.cutscenes;
    }

    public void tick() {
        this.zones.tick();
        this.cutscenes.tick();
        tickScheduledEvents();
    }

    public void executeEvent(String eventId, TriggerZone zone) {
        try {
            executeEvent(UUID.fromString(eventId), zone);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void executeEvent(UUID eventId, TriggerZone zone) {
        MinecraftClient client = MinecraftClient.getInstance();
        EditorEventDefinition eventDefinition = this.project.events().get(eventId);
        if (client.player == null || client.world == null || eventDefinition == null) {
            return;
        }

        EditorActionContext context = new EditorActionContext(
                client,
                client.world,
                client.player,
                this.editorState,
                zone,
                eventDefinition,
                UUID.randomUUID()
        );

        for (EditorActionInstance instance : eventDefinition.actions()) {
            EditorActionDefinition definition = EditorActionRegistry.getInstance().get(instance.actionId());
            if (definition != null) {
                definition.execute(context, instance.data());
            }
        }
    }

    public void scheduleEvent(String eventId, TriggerZone zone, int delayTicks) {
        try {
            this.scheduledEvents.add(new ScheduledEvent(UUID.fromString(eventId), zone.id(), Math.max(0, delayTicks)));
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void tickScheduledEvents() {
        Iterator<ScheduledEvent> iterator = this.scheduledEvents.iterator();
        while (iterator.hasNext()) {
            ScheduledEvent scheduledEvent = iterator.next();
            scheduledEvent.remainingTicks--;
            if (scheduledEvent.remainingTicks <= 0) {
                TriggerZone zone = this.project.zones().get(scheduledEvent.zoneId);
                if (zone != null) {
                    executeEvent(scheduledEvent.eventId, zone);
                }
                iterator.remove();
            }
        }
    }

    private static final class ScheduledEvent {
        private final UUID eventId;
        private final UUID zoneId;
        private int remainingTicks;

        private ScheduledEvent(UUID eventId, UUID zoneId, int remainingTicks) {
            this.eventId = eventId;
            this.zoneId = zoneId;
            this.remainingTicks = remainingTicks;
        }
    }
}
