package net.dark.editorapi.client.render;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.TriggerZone;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.debug.DebugRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class EditorWorldRenderer {
    private final EditorClientState state;

    public EditorWorldRenderer(EditorClientState state) {
        this.state = state;
    }

    public void renderWorld(WorldRenderContext context) {
        if (!this.state.editorOpen() || context.matrixStack() == null || context.consumers() == null) {
            return;
        }

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        Vec3d cameraPos = context.camera().getPos();

        for (TriggerZone zone : this.state.project().zones().values()) {
            if (!zone.visible()) {
                continue;
            }
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(zone.id());
            float red = selected ? 1.0F : 0.25F;
            float green = selected ? 0.82F : 0.74F;
            float blue = selected ? 0.35F : 1.0F;
            DebugRenderer.drawBox(matrices, consumers, zone.box(), red, green, blue, 0.9F);
            double pos1Size = handleSize(cameraPos, zone.pos1().toCenterPos(), this.state.selection().type() == EditorSelectionType.POS1);
            double pos2Size = handleSize(cameraPos, zone.pos2().toCenterPos(), this.state.selection().type() == EditorSelectionType.POS2);
            DebugRenderer.drawBox(matrices, consumers, boxAround(zone.pos1().toCenterPos(), pos1Size), 1.0F, 0.4F, 0.3F, 0.95F);
            DebugRenderer.drawBox(matrices, consumers, boxAround(zone.pos2().toCenterPos(), pos2Size), 0.3F, 0.8F, 1.0F, 0.95F);
            Vec3d center = zone.center();
            if (selected) {
                drawAxisGizmo(matrices, consumers, center, gizmoLength(cameraPos, center));
            }
            DebugRenderer.drawString(matrices, consumers, zone.name(), center.x, center.y + 0.7D, center.z, 0xFFE7ECF5, 0.02F, true, 0.0F, true);
        }

        for (CutsceneDefinition cutscene : this.state.project().cutscenes().values()) {
            if (!cutscene.visible()) {
                continue;
            }
            CutsceneKeyframe previous = null;
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                boolean keySelected = this.state.selection().type() == EditorSelectionType.KEYFRAME && keyframe.id().equals(this.state.selection().childId());
                double keySize = handleSize(cameraPos, keyframe.position(), keySelected);
                DebugRenderer.drawBox(matrices, consumers, boxAround(keyframe.position(), keySize), keySelected ? 1.0F : 0.85F, keySelected ? 0.95F : 0.85F, 0.25F, 0.9F);
                if (previous != null) {
                    drawLinkLine(matrices, consumers, previous.position(), keyframe.position(), cameraPos);
                }
                if (keySelected) {
                    drawAxisGizmo(matrices, consumers, keyframe.position(), gizmoLength(cameraPos, keyframe.position()));
                }
                DebugRenderer.drawString(matrices, consumers, Integer.toString(keyframe.frame()), keyframe.position().x, keyframe.position().y + 0.35D, keyframe.position().z, 0xFFFFCA5A, 0.018F, true, 0.0F, true);
                previous = keyframe;
            }
        }
    }

    public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!this.state.editorOpen()) {
            return;
        }

        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null || !cutscene.showPreview()) {
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int width = 180;
        int height = 56;
        int x = context.getScaledWindowWidth() - width - 10;
        int y = 24;
        context.fill(x, y, x + width, y + height, 0xD010141B);
        context.drawBorder(x, y, width, height, 0xFF56647A);
        context.drawText(textRenderer, Text.literal("Camera Preview"), x + 8, y + 8, 0xFFE8EEF7, false);
        context.drawText(textRenderer, Text.literal(cutscene.name()), x + 8, y + 22, 0xFF9FB2D1, false);
        context.drawText(textRenderer, Text.literal("Frame " + (int) Math.floor(this.state.runtime().cutscenes().previewFrame())), x + 8, y + 36, 0xFF9FB2D1, false);
    }

    private void drawAxisGizmo(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d origin, double length) {
        double thickness = Math.max(0.01D, length * 0.035D);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x, origin.y, origin.z, origin.x + length, origin.y + thickness, origin.z + thickness), 0.95F, 0.35F, 0.35F, 0.95F);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x, origin.y, origin.z, origin.x + thickness, origin.y + length, origin.z + thickness), 0.35F, 0.95F, 0.35F, 0.95F);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x, origin.y, origin.z, origin.x + thickness, origin.y + thickness, origin.z + length), 0.35F, 0.55F, 0.95F, 0.95F);
    }

    private void drawLinkLine(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d from, Vec3d to, Vec3d cameraPos) {
        double step = 0.18D;
        Vec3d delta = to.subtract(from);
        int segments = Math.max(1, (int) Math.ceil(delta.length() / step));
        for (int i = 0; i <= segments; i++) {
            Vec3d point = from.lerp(to, i / (double) segments);
            double size = Math.max(0.015D, handleSize(cameraPos, point, false) * 0.25D);
            DebugRenderer.drawBox(matrices, consumers, boxAround(point, size), 0.45F, 0.78F, 1.0F, 0.65F);
        }
    }

    private Box boxAround(Vec3d center, double radius) {
        return new Box(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
    }

    private double handleSize(Vec3d cameraPos, Vec3d position, boolean selected) {
        double distance = Math.max(1.0D, cameraPos.distanceTo(position));
        double base = Math.min(0.16D, 0.035D + distance * 0.006D);
        return selected ? base * 1.35D : base;
    }

    private double gizmoLength(Vec3d cameraPos, Vec3d origin) {
        double distance = Math.max(1.0D, cameraPos.distanceTo(origin));
        return Math.min(0.85D, 0.22D + distance * 0.025D);
    }
}
