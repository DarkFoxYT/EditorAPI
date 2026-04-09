package net.dark.editorapi.client;

import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.client.render.EditorWorldRenderer;
import net.dark.editorapi.client.state.EditorClientState;
import net.dark.editorapi.client.ui.EditorScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class EditorapiClient implements ClientModInitializer {
    private static final EditorClientState STATE = new EditorClientState();

    private KeyBinding toggleEditorKey;
    private KeyBinding setPos1Key;
    private KeyBinding setPos2Key;
    private KeyBinding addKeyframeKey;

    @Override
    public void onInitializeClient() {
        this.toggleEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + EditorConstants.MOD_ID + ".toggle_editor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F6, EditorConstants.KEY_CATEGORY));
        this.setPos1Key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + EditorConstants.MOD_ID + ".set_pos1", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F7, EditorConstants.KEY_CATEGORY));
        this.setPos2Key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + EditorConstants.MOD_ID + ".set_pos2", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, EditorConstants.KEY_CATEGORY));
        this.addKeyframeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key." + EditorConstants.MOD_ID + ".add_keyframe", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, EditorConstants.KEY_CATEGORY));

        EditorWorldRenderer renderer = new EditorWorldRenderer(STATE);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(renderer::renderWorld);
        WorldRenderEvents.START_MAIN.register(renderer::applyCamera);
        HudRenderCallback.EVENT.register(renderer::renderHud);

        ClientTickEvents.END_CLIENT_TICK.register(this::tickClient);
    }

    public static EditorClientState state() {
        return STATE;
    }

    private void tickClient(MinecraftClient client) {
        STATE.tick();

        while (this.toggleEditorKey.wasPressed()) {
            STATE.setEditorOpen(!STATE.editorOpen());
            client.setScreen(STATE.editorOpen() ? new EditorScreen(STATE) : null);
        }

        while (this.setPos1Key.wasPressed()) {
            STATE.capturePos1();
        }

        while (this.setPos2Key.wasPressed()) {
            STATE.capturePos2();
        }

        while (this.addKeyframeKey.wasPressed()) {
            STATE.addCurrentCameraKeyframe();
        }
    }
}
