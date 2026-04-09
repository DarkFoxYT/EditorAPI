package net.dark.editorapi.client.render;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.client.state.EditorToolMode;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.PlacedBlueprint;
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
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (PlacedBlueprint blueprint : this.state.project().blueprints().values()) {
            if (!blueprint.visible()) {
                continue;
            }
            boolean selected = this.state.selection().type() == EditorSelectionType.BLUEPRINT && blueprint.id().equals(this.state.selection().objectId());
            Box bounds = blueprint.bounds(this.state.project());
            drawVolumeBox(matrices, consumers, bounds, 1.0F, 1.0F, 1.0F, selected ? 0.22F : 0.08F, selected ? 0.95F : 0.55F);
            if (this.state.worldTextVisible()) {
                DebugRenderer.drawString(matrices, consumers, blueprint.name(), bounds.getCenter().x, bounds.maxY + 0.45D, bounds.getCenter().z, 0xFFFFFFFF, 0.02F, true, 0.0F, true);
            }
            if (selected && !this.state.cameraNavigating()) {
                drawWorldGizmo(matrices, consumers, blueprint.origin(), cameraPos);
                if (this.state.debugBoundsVisible()) {
                    drawGuides(matrices, consumers, blueprint.origin(), gizmoLength(cameraPos, blueprint.origin()) * 2.6D);
                }
            }
        }

        for (TriggerZone zone : this.state.project().zones().values()) {
            if (!zone.visible()) {
                continue;
            }
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(zone.id());
            float red = selected ? 1.0F : 0.25F;
            float green = selected ? 0.82F : 0.74F;
            float blue = selected ? 0.35F : 1.0F;
            drawVolumeBox(matrices, consumers, zone.box(), 1.0F, 1.0F, 1.0F, selected ? 0.18F : 0.06F, selected ? 0.98F : 0.5F);
            double pos1Size = handleSize(cameraPos, zone.pos1().toCenterPos(), this.state.selection().type() == EditorSelectionType.POS1);
            double pos2Size = handleSize(cameraPos, zone.pos2().toCenterPos(), this.state.selection().type() == EditorSelectionType.POS2);
            drawHandleCube(matrices, consumers, zone.pos1().toCenterPos(), pos1Size, 1.0F, 0.4F, 0.3F, 0.95F);
            drawHandleCube(matrices, consumers, zone.pos2().toCenterPos(), pos2Size, 0.3F, 0.8F, 1.0F, 0.95F);
            Vec3d center = zone.center();
            if (selected && !this.state.cameraNavigating()) {
                drawWorldGizmo(matrices, consumers, center, cameraPos);
                if (this.state.debugBoundsVisible()) {
                    drawGuides(matrices, consumers, center, gizmoLength(cameraPos, center) * 2.2D);
                }
            }
            if (this.state.worldTextVisible()) {
                DebugRenderer.drawString(matrices, consumers, zone.name(), center.x, center.y + 0.7D, center.z, 0xFFE7ECF5, 0.02F, true, 0.0F, true);
            }
        }

        for (CutsceneDefinition cutscene : this.state.project().cutscenes().values()) {
            if (!cutscene.visible()) {
                continue;
            }
            CutsceneKeyframe previous = null;
            for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
                boolean keySelected = this.state.selection().type() == EditorSelectionType.KEYFRAME && keyframe.id().equals(this.state.selection().childId());
                double keySize = handleSize(cameraPos, keyframe.position(), keySelected);
                drawHandleCube(matrices, consumers, keyframe.position(), keySize, keySelected ? 1.0F : 0.85F, keySelected ? 0.95F : 0.85F, 0.25F, 0.9F);
                if (previous != null && this.state.debugBoundsVisible()) {
                    drawLinkLine(matrices, consumers, previous.position(), keyframe.position(), cameraPos);
                }
                if (keySelected && !this.state.cameraNavigating()) {
                    drawWorldGizmo(matrices, consumers, keyframe.position(), cameraPos);
                    if (this.state.debugBoundsVisible()) {
                        drawGuides(matrices, consumers, keyframe.position(), gizmoLength(cameraPos, keyframe.position()) * 1.8D);
                    }
                }
                if (this.state.worldTextVisible()) {
                    DebugRenderer.drawString(matrices, consumers, Integer.toString(keyframe.frame()), keyframe.position().x, keyframe.position().y + 0.35D, keyframe.position().z, 0xFFFFCA5A, 0.018F, true, 0.0F, true);
                }
                previous = keyframe;
            }
        }
        matrices.pop();
    }

    public void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        if (!this.state.editorOpen()) {
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        if (this.state.worldTextVisible()) {
            context.drawText(textRenderer, Text.literal(this.state.cameraNavigating() ? "Navigation: RMB drag active" : "Editor: LMB select/drag gizmos"), 10, 26, 0xFFE7ECF5, false);
        }

        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null || !cutscene.showPreview() || !this.state.worldTextVisible()) {
            return;
        }

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

    private void drawWorldGizmo(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d origin, Vec3d cameraPos) {
        double length = gizmoLength(cameraPos, origin);
        double thickness = Math.max(0.025D, length * 0.08D);
        double headSize = Math.max(0.08D, length * 0.18D);
        drawHandleCube(matrices, consumers, origin, thickness * 0.9D, 0.95F, 0.95F, 0.95F, 0.95F);
        drawAxisArrow(matrices, consumers, origin, length, thickness, headSize, Axis.X);
        drawAxisArrow(matrices, consumers, origin, length, thickness, headSize, Axis.Y);
        drawAxisArrow(matrices, consumers, origin, length, thickness, headSize, Axis.Z);
        if (this.state.toolMode() == EditorToolMode.ROTATE) {
            drawRing(matrices, consumers, origin, length * 0.9D, Axis.X);
            drawRing(matrices, consumers, origin, length * 0.9D, Axis.Y);
            drawRing(matrices, consumers, origin, length * 0.9D, Axis.Z);
        }
    }

    private void drawGuides(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d origin, double length) {
        double thickness = Math.max(0.008D, length * 0.01D);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x - length, origin.y, origin.z, origin.x + length, origin.y + thickness, origin.z + thickness), 0.95F, 0.35F, 0.35F, 0.45F);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x, origin.y - length, origin.z, origin.x + thickness, origin.y + length, origin.z + thickness), 0.35F, 0.95F, 0.35F, 0.45F);
        DebugRenderer.drawBox(matrices, consumers, new Box(origin.x, origin.y, origin.z - length, origin.x + thickness, origin.y + thickness, origin.z + length), 0.35F, 0.55F, 0.95F, 0.45F);
    }

    private void drawRing(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d origin, double radius, Axis axis) {
        int segments = 32;
        for (int index = 0; index < segments; index++) {
            double angle = (Math.PI * 2.0D * index) / segments;
            Vec3d point = switch (axis) {
                case X -> origin.add(0.0D, Math.cos(angle) * radius, Math.sin(angle) * radius);
                case Y -> origin.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
                case Z -> origin.add(Math.cos(angle) * radius, Math.sin(angle) * radius, 0.0D);
            };
            float red = axis == Axis.X ? 0.95F : axis == Axis.Y ? 0.35F : 0.35F;
            float green = axis == Axis.X ? 0.35F : axis == Axis.Y ? 0.95F : 0.55F;
            float blue = axis == Axis.Z ? 0.95F : 0.35F;
            drawHandleCube(matrices, consumers, point, Math.max(0.03D, radius * 0.035D), red, green, blue, 0.9F);
        }
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

    private void drawAxisArrow(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d origin, double length, double thickness, double headSize, Axis axis) {
        float red = axis == Axis.X ? 0.95F : axis == Axis.Y ? 0.35F : 0.35F;
        float green = axis == Axis.X ? 0.35F : axis == Axis.Y ? 0.95F : 0.55F;
        float blue = axis == Axis.Z ? 0.95F : 0.35F;
        Box shaft = switch (axis) {
            case X -> new Box(origin.x, origin.y - thickness * 0.35D, origin.z - thickness * 0.35D, origin.x + length - headSize, origin.y + thickness * 0.35D, origin.z + thickness * 0.35D);
            case Y -> new Box(origin.x - thickness * 0.35D, origin.y, origin.z - thickness * 0.35D, origin.x + thickness * 0.35D, origin.y + length - headSize, origin.z + thickness * 0.35D);
            case Z -> new Box(origin.x - thickness * 0.35D, origin.y - thickness * 0.35D, origin.z, origin.x + thickness * 0.35D, origin.y + thickness * 0.35D, origin.z + length - headSize);
        };
        DebugRenderer.drawBox(matrices, consumers, shaft, red, green, blue, 0.95F);
        Vec3d headCenter = switch (axis) {
            case X -> origin.add(length, 0.0D, 0.0D);
            case Y -> origin.add(0.0D, length, 0.0D);
            case Z -> origin.add(0.0D, 0.0D, length);
        };
        drawHandleCube(matrices, consumers, headCenter, headSize * 0.45D, red, green, blue, 1.0F);
    }

    private void drawHandleCube(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d center, double radius, float red, float green, float blue, float alpha) {
        DebugRenderer.drawBox(matrices, consumers, boxAround(center, radius), red, green, blue, alpha);
    }

    private void drawVolumeBox(MatrixStack matrices, VertexConsumerProvider consumers, Box box, float red, float green, float blue, float fillAlpha, float lineAlpha) {
        double thickness = Math.max(0.015D, Math.min(box.getLengthX(), Math.min(box.getLengthY(), box.getLengthZ())) * 0.03D + 0.01D);
        DebugRenderer.drawBox(matrices, consumers, box, red, green, blue, lineAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY + thickness, box.maxZ), red, green, blue, fillAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.minX, box.maxY - thickness, box.minZ, box.maxX, box.maxY, box.maxZ), red, green, blue, fillAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.minX, box.minY, box.minZ, box.minX + thickness, box.maxY, box.maxZ), red, green, blue, fillAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.maxX - thickness, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ), red, green, blue, fillAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ + thickness), red, green, blue, fillAlpha);
        DebugRenderer.drawBox(matrices, consumers, new Box(box.minX, box.minY, box.maxZ - thickness, box.maxX, box.maxY, box.maxZ), red, green, blue, fillAlpha);
    }

    private double handleSize(Vec3d cameraPos, Vec3d position, boolean selected) {
        double distance = Math.max(1.0D, cameraPos.distanceTo(position));
        double base = Math.min(0.16D, 0.035D + distance * 0.006D);
        return selected ? base * 1.35D : base;
    }

    private double gizmoLength(Vec3d cameraPos, Vec3d origin) {
        double distance = Math.max(1.0D, cameraPos.distanceTo(origin));
        return Math.min(1.2D, 0.28D + distance * 0.03D);
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
