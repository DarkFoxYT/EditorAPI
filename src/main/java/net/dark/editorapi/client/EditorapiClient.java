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
import net.minecraft.client.gui.screen.ChatScreen;
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
    private boolean leftMouseDown;
    private boolean rightMouseDown;
    private boolean spaceDown;

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
                client.mouse.unlockCursor();
            } else {
                client.mouse.lockCursor();
            }
        }

        if (!STATE.editorOpen()) {
            return;
        }

        ensureOverlay(client);
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
        if (!STATE.editorOpen() || this.overlayScreen == null || client.player == null) {
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

        boolean leftNow = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightNow = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean spaceNow = InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_SPACE);

        if (leftNow && !this.leftMouseDown) {
            this.overlayScreen.mouseClicked(mouseX, mouseY, 0);
        }
        if (!leftNow && this.leftMouseDown) {
            this.overlayScreen.mouseReleased(mouseX, mouseY, 0);
        }
        if (leftNow && this.leftMouseDown) {
            this.overlayScreen.mouseDragged(mouseX, mouseY, 0, 0.0D, 0.0D);
        }

        if (rightNow && !this.rightMouseDown) {
            this.overlayScreen.mouseClicked(mouseX, mouseY, 1);
        }
        if (!rightNow && this.rightMouseDown) {
            this.overlayScreen.mouseReleased(mouseX, mouseY, 1);
        }

        if (spaceNow && !this.spaceDown) {
            this.overlayScreen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);
        }

        if (client.currentScreen instanceof ChatScreen) {
            // Let chat keep mouse ownership when open while the overlay remains visible.
        }

        this.leftMouseDown = leftNow;
        this.rightMouseDown = rightNow;
        this.spaceDown = spaceNow;
    }

    private int scaledMouseX(MinecraftClient client) {
        return MathHelper.floor(client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth());
    }

    private int scaledMouseY(MinecraftClient client) {
        return MathHelper.floor(client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight());
    }
}
