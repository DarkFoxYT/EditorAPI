package net.dark.editorapi.network;

import net.dark.editorapi.EditorConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ProjectSyncRequestPayload() implements CustomPayload {
    public static final Id<ProjectSyncRequestPayload> ID = new Id<>(EditorConstants.id("project_sync_request"));
    public static final PacketCodec<PacketByteBuf, ProjectSyncRequestPayload> CODEC = PacketCodec.of((value, buf) -> {
    }, buf -> new ProjectSyncRequestPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
