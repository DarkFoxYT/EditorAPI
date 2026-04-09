package net.dark.editorapi.api.action;

import java.util.UUID;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

public record EditorActionContext(
        MinecraftClient client,
        ClientWorld world,
        PlayerEntity player,
        EditorClientState editorState,
        TriggerZone zone,
        EditorEventDefinition eventDefinition,
        UUID triggerId
) {
}
