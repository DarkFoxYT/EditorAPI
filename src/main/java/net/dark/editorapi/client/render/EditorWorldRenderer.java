package net.dark.editorapi.client.render;

import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.state.EditorSelectionType;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.TriggerZone;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldTerrainRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public final class EditorWorldRenderer {
    private final EditorClientState state;

    public EditorWorldRenderer(EditorClientState state) {
        this.state = state;
    }

    public void applyCamera(WorldTerrainRenderContext context) {
        Camera camera = context.gameRenderer().getCamera();
        CutsceneDefinition preview = this.state.selectedCutscene();
        this.state.runtime().cutscenes().applyCamera(camera, preview == null ? null : preview.id(), preview != null && preview.showPreview());
    }

    public void renderWorld(WorldRenderContext context) {
        if (MinecraftClient.getInstance().world == null || context.matrices() == null) {
            return;
        }

        MatrixStack matrices = context.matrices();
        Vec3d cameraPos = context.gameRenderer().getCamera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        VertexConsumer lines = context.consumers().getBuffer(RenderLayer.getLines());
        VertexConsumer fills = context.consumers().getBuffer(RenderLayer.getDebugFilledBox());

        for (TriggerZone zone : this.state.project().zones().values()) {
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(zone.id());
            float red = selected ? 1.0F : 0.24F;
            float green = selected ? 0.78F : 0.72F;
            float blue = selected ? 0.37F : 1.0F;

            WorldRenderer.drawBox(matrices, lines, zone.box(), red, green, blue, 0.95F);
            if (zone.showFill()) {
                Box box = zone.box();
                WorldRenderer.renderFilledBox(matrices, fills, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, selected ? 0.12F : 0.06F);
            }

            renderHandle(matrices, lines, fills, zone.pos1(), this.state.selection().type() == EditorSelectionType.POS1 && selected, 1.0F, 0.45F, 0.3F);
            renderHandle(matrices, lines, fills, zone.pos2(), this.state.selection().type() == EditorSelectionType.POS2 && selected, 0.3F, 0.8F, 1.0F);
            renderDirectionBars(matrices, fills, zone);
        }

        for (CutsceneDefinition cutscene : this.state.project().cutscenes().values()) {
            boolean selected = this.state.selection().objectId() != null && this.state.selection().objectId().equals(cutscene.id());
            renderCutscenePath(matrices, lines, fills, cutscene, selected);
        }

        matrices.pop();
    }

    public void renderHud(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        CutsceneDefinition cutscene = this.state.selectedCutscene();
        if (cutscene == null || !cutscene.showPreview()) {
            return;
        }

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int width = 170;
        int height = 54;
        int x = context.getScaledWindowWidth() - width - 12;
        int y = context.getScaledWindowHeight() - height - 12;
        context.fill(x, y, x + width, y + height, 0xD010141B);
        context.drawBorder(x, y, width, height, 0xFF56647A);
        context.drawText(textRenderer, Text.literal("Camera Preview"), x + 8, y + 8, 0xFFE8EEF7, false);
        context.drawText(textRenderer, Text.literal(cutscene.name()), x + 8, y + 22, 0xFF9FB2D1, false);
        context.drawText(textRenderer, Text.literal("Frame " + MathHelper.floor(this.state.runtime().cutscenes().previewFrame())), x + 8, y + 36, 0xFF9FB2D1, false);
    }

    private void renderDirectionBars(MatrixStack matrices, VertexConsumer fills, TriggerZone zone) {
        Vec3d center = zone.center();
        if (zone.triggerEnter()) {
            WorldRenderer.renderFilledBox(matrices, fills, center.x - 0.05D, center.y + 0.05D, center.z - 0.05D, center.x + 0.05D, center.y + 1.0D, center.z + 0.05D, 0.2F, 1.0F, 0.35F, 0.7F);
        }
        if (zone.triggerExit()) {
            WorldRenderer.renderFilledBox(matrices, fills, center.x - 0.35D, center.y + 0.05D, center.z - 0.05D, center.x - 0.25D, center.y + 1.0D, center.z + 0.05D, 1.0F, 0.4F, 0.35F, 0.7F);
        }
        if (zone.triggerWhileInside() || zone.triggerTimeInside()) {
            WorldRenderer.renderFilledBox(matrices, fills, center.x + 0.25D, center.y + 0.05D, center.z - 0.05D, center.x + 0.35D, center.y + 1.0D, center.z + 0.05D, 0.35F, 0.7F, 1.0F, 0.7F);
        }
    }

    private void renderHandle(MatrixStack matrices, VertexConsumer lines, VertexConsumer fills, BlockPos pos, boolean selected, float red, float green, float blue) {
        Box box = new Box(pos).expand(selected ? 0.18D : 0.12D);
        WorldRenderer.drawBox(matrices, lines, box, red, green, blue, 1.0F);
        WorldRenderer.renderFilledBox(matrices, fills, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, selected ? 0.4F : 0.2F);
    }

    private void renderCutscenePath(MatrixStack matrices, VertexConsumer lines, VertexConsumer fills, CutsceneDefinition cutscene, boolean selected) {
        if (cutscene.keyframes().isEmpty()) {
            return;
        }

        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            boolean keySelected = selected && this.state.selection().childId() != null && this.state.selection().childId().equals(keyframe.id());
            renderSampleCube(matrices, lines, fills, keyframe.position(), keySelected ? 0.22D : 0.14D, keySelected ? 1.0F : 0.83F, keySelected ? 0.92F : 0.83F, 0.3F);
            if (keySelected) {
                renderGizmo(matrices, fills, keyframe.position());
            }
        }

        int step = 4;
        for (int frame = cutscene.startFrame(); frame <= cutscene.endFrame(); frame += step) {
            Vec3d point = cutscene.sample(frame).position();
            renderSampleCube(matrices, lines, fills, point, 0.05D, selected ? 0.9F : 0.5F, 0.75F, 1.0F);
        }
    }

    private void renderSampleCube(MatrixStack matrices, VertexConsumer lines, VertexConsumer fills, Vec3d position, double radius, float red, float green, float blue) {
        Box box = new Box(position.x - radius, position.y - radius, position.z - radius, position.x + radius, position.y + radius, position.z + radius);
        WorldRenderer.drawBox(matrices, lines, box, red, green, blue, 1.0F);
        WorldRenderer.renderFilledBox(matrices, fills, box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, 0.15F);
    }

    private void renderGizmo(MatrixStack matrices, VertexConsumer fills, Vec3d origin) {
        WorldRenderer.renderFilledBox(matrices, fills, origin.x, origin.y, origin.z, origin.x + 1.2D, origin.y + 0.04D, origin.z + 0.04D, 1.0F, 0.25F, 0.25F, 0.85F);
        WorldRenderer.renderFilledBox(matrices, fills, origin.x, origin.y, origin.z, origin.x + 0.04D, origin.y + 1.2D, origin.z + 0.04D, 0.25F, 1.0F, 0.25F, 0.85F);
        WorldRenderer.renderFilledBox(matrices, fills, origin.x, origin.y, origin.z, origin.x + 0.04D, origin.y + 0.04D, origin.z + 1.2D, 0.25F, 0.6F, 1.0F, 0.85F);
    }
}
