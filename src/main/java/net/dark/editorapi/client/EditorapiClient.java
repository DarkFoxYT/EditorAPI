package net.dark.editorapi.client;

import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.client.render.EditorWorldRenderer;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.ui.EditorScreen;
import net.dark.editorapi.network.EditorNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class EditorapiClient implements ClientModInitializer {
    private static final EditorClientState STATE = new EditorClientState();

    private KeyBinding toggleEditorKey;
    private EditorScreen overlayScreen;
    private int overlayWidth = -1;
    private int overlayHeight = -1;
    private boolean rightMouseDown;
    private boolean navigatingCamera;
    private double lastRawMouseX;
    private double lastRawMouseY;

    @Override
    public void onInitializeClient() {
        EditorNetworking.bootstrapClient();
        this.toggleEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + EditorConstants.MOD_ID + ".toggle_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, EditorConstants.KEY_CATEGORY));

        EditorWorldRenderer renderer = new EditorWorldRenderer(STATE);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(renderer::renderWorld);
        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            renderer.renderHud(drawContext, tickCounter);
            renderOverlay(drawContext);
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
    }

    public static EditorClientState state() {
        return STATE;
    }

    private void tickClient(MinecraftClient client) {
        STATE.tick();

        while (this.toggleEditorKey.wasPressed()) {
            STATE.setEditorOpen(!STATE.editorOpen());
            if (STATE.editorOpen()) {
                ensureOverlay(client);
                STATE.setMouseInteraction(true);
                STATE.setCameraNavigating(false);
                this.navigatingCamera = false;
                client.setScreen(this.overlayScreen);
                client.mouse.unlockCursor();
            } else {
                STATE.setMouseInteraction(false);
                STATE.setCameraNavigating(false);
                this.navigatingCamera = false;
                client.setScreen(null);
                client.mouse.lockCursor();
            }
        }

        if (!STATE.editorOpen()) {
            return;
        }

        ensureOverlay(client);
        STATE.setMouseInteraction(true);
        suppressVanillaControls(client);
        if (client.currentScreen != this.overlayScreen) {
            client.setScreen(this.overlayScreen);
        }
        client.mouse.unlockCursor();
        handleOverlayInput(client);
    }

    private void ensureOverlay(MinecraftClient client) {
        if (this.overlayScreen == null) {
            this.overlayScreen = new EditorScreen(STATE);
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        if (width != this.overlayWidth || height != this.overlayHeight) {
            this.overlayScreen.init(client, width, height);
            this.overlayWidth = width;
            this.overlayHeight = height;
        }
    }

    private void renderOverlay(net.minecraft.client.gui.DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!STATE.editorOpen() || this.overlayScreen == null || client.player == null || client.currentScreen == this.overlayScreen) {
            return;
        }
        int mouseX = scaledMouseX(client);
        int mouseY = scaledMouseY(client);
        this.overlayScreen.render(drawContext, mouseX, mouseY, client.getRenderTickCounter().getTickDelta(false));
    }

    private void handleOverlayInput(MinecraftClient client) {
        if (this.overlayScreen == null) {
            return;
        }

        long handle = client.getWindow().getHandle();
        int mouseX = scaledMouseX(client);
        int mouseY = scaledMouseY(client);

        boolean rightNow = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        double rawMouseX = client.mouse.getX();
        double rawMouseY = client.mouse.getY();

        boolean overUi = this.overlayScreen.isHoveringUi(mouseX, mouseY);
        if (rightNow && !this.rightMouseDown && !overUi) {
            this.navigatingCamera = true;
            STATE.setCameraNavigating(true);
            this.lastRawMouseX = rawMouseX;
            this.lastRawMouseY = rawMouseY;
        }
        if (!rightNow && this.rightMouseDown) {
            this.navigatingCamera = false;
            STATE.setCameraNavigating(false);
        }

        if (this.navigatingCamera) {
            handleEditorNavigation(client, handle, rawMouseX, rawMouseY);
            this.rightMouseDown = rightNow;
            return;
        }
        this.rightMouseDown = rightNow;
        this.lastRawMouseX = rawMouseX;
        this.lastRawMouseY = rawMouseY;
    }

    private int scaledMouseX(MinecraftClient client) {
        return MathHelper.floor(client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
    }

    private int scaledMouseY(MinecraftClient client) {
        return MathHelper.floor(client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
    }

    private void handleEditorNavigation(MinecraftClient client, long handle, double rawMouseX, double rawMouseY) {
        if (client.player == null) {
            return;
        }

        double deltaX = rawMouseX - this.lastRawMouseX;
        double deltaY = rawMouseY - this.lastRawMouseY;
        this.lastRawMouseX = rawMouseX;
        this.lastRawMouseY = rawMouseY;

        float sensitivity = 0.18F;
        client.player.setYaw(client.player.getYaw() + (float) deltaX * sensitivity);
        client.player.setPitch(MathHelper.clamp(client.player.getPitch() + (float) deltaY * sensitivity, -89.5F, 89.5F));
        client.player.prevYaw = client.player.getYaw();
        client.player.prevPitch = client.player.getPitch();

        double speed = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT) ? 0.55D : 0.25D;
        float yawRad = (float) Math.toRadians(client.player.getYaw());
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);
        double moveX = 0.0D;
        double moveY = 0.0D;
        double moveZ = 0.0D;

        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_W)) {
            moveX -= sin * speed;
            moveZ += cos * speed;
        }
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_S)) {
            moveX += sin * speed;
            moveZ -= cos * speed;
        }
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_A)) {
            moveX -= cos * speed;
            moveZ -= sin * speed;
        }
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_D)) {
            moveX += cos * speed;
            moveZ += sin * speed;
        }
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_E) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_SPACE)) {
            moveY += speed;
        }
        if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_Q) || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_CONTROL)) {
            moveY -= speed;
        }

        if (moveX != 0.0D || moveY != 0.0D || moveZ != 0.0D) {
            client.player.setVelocity(0.0D, 0.0D, 0.0D);
            client.player.setPos(client.player.getX() + moveX, client.player.getY() + moveY, client.player.getZ() + moveZ);
        }
    }

    private void suppressVanillaControls(MinecraftClient client) {
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.pickItemKey.setPressed(false);
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
    }
}
