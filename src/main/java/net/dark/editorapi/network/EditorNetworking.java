package net.dark.editorapi.network;

import net.dark.editorapi.EditorConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class EditorNetworking {
    private EditorNetworking() {
    }

    public static void bootstrapCommon() {
        PayloadTypeRegistry.playC2S().register(ProjectSyncRequestPayload.ID, ProjectSyncRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProjectSnapshotPayload.ID, ProjectSnapshotPayload.CODEC);
    }

    public static void bootstrapClient() {
        ClientPlayNetworking.registerGlobalReceiver(ProjectSnapshotPayload.ID, (payload, context) -> {
        });
    }

    public static void bootstrapServer() {
        ServerPlayNetworking.registerGlobalReceiver(ProjectSyncRequestPayload.ID, (payload, context) -> {
            context.player().sendMessage(net.minecraft.text.Text.literal("Editor sync requested for " + EditorConstants.MOD_ID), false);
        });
    }
}
