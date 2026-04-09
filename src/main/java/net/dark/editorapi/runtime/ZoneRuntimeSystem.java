package net.dark.editorapi.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.model.TriggerOnceMode;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.MinecraftClient;

public final class ZoneRuntimeSystem {
    private final EditorClientState editorState;
    private final EditorRuntime runtime;
    private final Map<UUID, ZonePlayerState> states = new HashMap<>();
    private final Map<UUID, Boolean> globalTriggers = new HashMap<>();

    public ZoneRuntimeSystem(EditorClientState editorState, EditorRuntime runtime) {
        this.editorState = editorState;
        this.runtime = runtime;
    }

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        for (TriggerZone zone : this.editorState.project().zones().values()) {
            ZonePlayerState state = this.states.computeIfAbsent(zone.id(), ignored -> new ZonePlayerState());
            boolean inside = zone.box().contains(client.player.getPos());

            if (!state.wasInside && inside) {
                state.enterTick = client.world.getTime();
                state.lastWhileTick = client.world.getTime();
                state.timeInsideTriggered = false;
                trigger(zone, state, zone.triggerEnter(), zone.enterEventId());
            }

            if (state.wasInside && !inside) {
                trigger(zone, state, zone.triggerExit(), zone.exitEventId());
            }

            if (inside) {
                state.totalTicksInside = (int) (client.world.getTime() - state.enterTick);

                if (zone.triggerWhileInside() && client.world.getTime() - state.lastWhileTick >= zone.whileInsideIntervalTicks()) {
                    state.lastWhileTick = client.world.getTime();
                    trigger(zone, state, true, zone.eventId());
                }

                if (zone.triggerTimeInside() && !state.timeInsideTriggered && state.totalTicksInside >= zone.requiredTimeTicks()) {
                    state.timeInsideTriggered = true;
                    trigger(zone, state, true, zone.eventId());
                }
            }

            state.wasInside = inside;
        }
    }

    private void trigger(TriggerZone zone, ZonePlayerState state, boolean enabled, UUID eventId) {
        if (!enabled || eventId == null || !canTrigger(zone, state)) {
            return;
        }

        if (zone.delayTicks() > 0) {
            this.runtime.scheduleEvent(eventId.toString(), zone, zone.delayTicks());
        } else {
            this.runtime.executeEvent(eventId, zone);
        }

        if (zone.onceMode() == TriggerOnceMode.ONCE_GLOBAL) {
            this.globalTriggers.put(zone.id(), true);
        } else if (zone.onceMode() == TriggerOnceMode.ONCE_PER_PLAYER) {
            state.hasTriggered = true;
        }
    }

    private boolean canTrigger(TriggerZone zone, ZonePlayerState state) {
        return switch (zone.onceMode()) {
            case NONE -> true;
            case ONCE_PER_PLAYER -> !state.hasTriggered;
            case ONCE_GLOBAL -> !this.globalTriggers.getOrDefault(zone.id(), false);
        };
    }

    public record ZonePlayerSnapshot(boolean wasInside, int totalTicksInside, boolean hasTriggered) {
    }

    public ZonePlayerSnapshot snapshot(UUID zoneId) {
        ZonePlayerState state = this.states.get(zoneId);
        if (state == null) {
            return new ZonePlayerSnapshot(false, 0, false);
        }
        return new ZonePlayerSnapshot(state.wasInside, state.totalTicksInside, state.hasTriggered);
    }

    private static final class ZonePlayerState {
        private boolean wasInside;
        private long enterTick;
        private long lastWhileTick;
        private int totalTicksInside;
        private boolean hasTriggered;
        private boolean timeInsideTriggered;
    }
}
