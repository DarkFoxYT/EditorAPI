package net.dark.editorapi.network;

import net.dark.editorapi.EditorConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ProjectSnapshotPayload(String json) implements CustomPayload {
    public static final Id<ProjectSnapshotPayload> ID = new Id<>(EditorConstants.id("project_snapshot"));
    public static final PacketCodec<PacketByteBuf, ProjectSnapshotPayload> CODEC = PacketCodec.of((value, buf) -> buf.writeString(value.json()), buf -> new ProjectSnapshotPayload(buf.readString()));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
