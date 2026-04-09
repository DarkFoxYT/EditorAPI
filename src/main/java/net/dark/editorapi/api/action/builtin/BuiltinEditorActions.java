package net.dark.editorapi.api.action.builtin;

import com.google.gson.JsonObject;
import java.util.List;
import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.api.action.EditorActionContext;
import net.dark.editorapi.api.action.EditorActionDefinition;
import net.dark.editorapi.api.action.EditorActionRegistry;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class BuiltinEditorActions {
    public static final Identifier PLAY_CUTSCENE = EditorConstants.id("play_cutscene");
    public static final Identifier SPAWN_PARTICLES = EditorConstants.id("spawn_particles");
    public static final Identifier PLAY_SOUND = EditorConstants.id("play_sound");
    public static final Identifier DELAY = EditorConstants.id("delay");
    public static final Identifier RUN_EVENT = EditorConstants.id("run_event");
    public static final Identifier SHOW_TITLE = EditorConstants.id("show_title");
    public static final Identifier RUN_COMMAND = EditorConstants.id("run_command");

    private BuiltinEditorActions() {
    }

    public static void bootstrap(EditorActionRegistry registry) {
        registry.register(new EditorActionDefinition(
                PLAY_CUTSCENE,
                "Play Cutscene",
                BuiltinEditorActions::defaultCutsceneData,
                BuiltinEditorActions::playCutscene,
                data -> List.of("Cutscene: " + data.get("cutsceneId").getAsString())
        ));
        registry.register(new EditorActionDefinition(
                SPAWN_PARTICLES,
                "Spawn Particles",
                BuiltinEditorActions::defaultParticleData,
                BuiltinEditorActions::spawnParticles,
                data -> List.of(
                        "Particle: " + data.get("particle").getAsString(),
                        "Count: " + data.get("count").getAsInt()
                )
        ));
        registry.register(new EditorActionDefinition(
                PLAY_SOUND,
                "Play Sound",
                BuiltinEditorActions::defaultSoundData,
                BuiltinEditorActions::playSound,
                data -> List.of(
                        "Sound: " + data.get("sound").getAsString(),
                        "Volume: " + data.get("volume").getAsFloat()
                )
        ));
        registry.register(new EditorActionDefinition(
                DELAY,
                "Delay",
                BuiltinEditorActions::defaultDelayData,
                BuiltinEditorActions::delay,
                data -> List.of("Delay: " + data.get("ticks").getAsInt() + " ticks")
        ));
        registry.register(new EditorActionDefinition(
                RUN_EVENT,
                "Run Event",
                BuiltinEditorActions::defaultRunEventData,
                BuiltinEditorActions::runEvent,
                data -> List.of("Event: " + data.get("eventId").getAsString())
        ));
        registry.register(new EditorActionDefinition(
                SHOW_TITLE,
                "Show Title",
                BuiltinEditorActions::defaultTitleData,
                BuiltinEditorActions::showTitle,
                data -> List.of(
                        "Title: " + data.get("title").getAsString(),
                        "Subtitle: " + data.get("subtitle").getAsString()
                )
        ));
        registry.register(new EditorActionDefinition(
                RUN_COMMAND,
                "Run Command",
                BuiltinEditorActions::defaultCommandData,
                BuiltinEditorActions::runCommand,
                data -> List.of("Command: " + data.get("command").getAsString())
        ));
    }

    private static JsonObject defaultCutsceneData() {
        JsonObject data = new JsonObject();
        data.addProperty("cutsceneId", "");
        return data;
    }

    private static JsonObject defaultParticleData() {
        JsonObject data = new JsonObject();
        data.addProperty("particle", "minecraft:end_rod");
        data.addProperty("count", 16);
        data.addProperty("offsetX", 0.0D);
        data.addProperty("offsetY", 1.0D);
        data.addProperty("offsetZ", 0.0D);
        return data;
    }

    private static JsonObject defaultSoundData() {
        JsonObject data = new JsonObject();
        data.addProperty("sound", "minecraft:block.note_block.pling");
        data.addProperty("volume", 1.0F);
        data.addProperty("pitch", 1.0F);
        return data;
    }

    private static JsonObject defaultDelayData() {
        JsonObject data = new JsonObject();
        data.addProperty("ticks", 20);
        data.addProperty("eventId", "");
        return data;
    }

    private static JsonObject defaultRunEventData() {
        JsonObject data = new JsonObject();
        data.addProperty("eventId", "");
        return data;
    }

    private static JsonObject defaultTitleData() {
        JsonObject data = new JsonObject();
        data.addProperty("title", "Scene Triggered");
        data.addProperty("subtitle", "EditorAPI");
        data.addProperty("fadeIn", 5);
        data.addProperty("stay", 40);
        data.addProperty("fadeOut", 10);
        return data;
    }

    private static JsonObject defaultCommandData() {
        JsonObject data = new JsonObject();
        data.addProperty("command", "say EditorAPI action fired");
        return data;
    }

    private static void playCutscene(EditorActionContext context, JsonObject data) {
        String rawId = data.get("cutsceneId").getAsString();
        if (!rawId.isBlank()) {
            context.editorState().runtime().cutscenes().start(rawId);
        }
    }

    private static void spawnParticles(EditorActionContext context, JsonObject data) {
        Identifier particleId = Identifier.tryParse(data.get("particle").getAsString());
        if (particleId == null || context.world() == null) {
            return;
        }

        var particleType = Registries.PARTICLE_TYPE.get(particleId);
        if (particleType == null) {
            particleType = ParticleTypes.END_ROD;
        }

        Vec3d origin = context.zone().center();
        context.world().addParticleClient(
                particleType,
                origin.x + data.get("offsetX").getAsDouble(),
                origin.y + data.get("offsetY").getAsDouble(),
                origin.z + data.get("offsetZ").getAsDouble(),
                0.0D,
                0.0D,
                0.0D
        );
        for (int index = 1; index < data.get("count").getAsInt(); index++) {
            context.world().addParticleClient(
                    particleType,
                    origin.x + data.get("offsetX").getAsDouble(),
                    origin.y + data.get("offsetY").getAsDouble(),
                    origin.z + data.get("offsetZ").getAsDouble(),
                    (context.world().random.nextDouble() - 0.5D) * 0.15D,
                    context.world().random.nextDouble() * 0.1D,
                    (context.world().random.nextDouble() - 0.5D) * 0.15D
            );
        }
    }

    private static void playSound(EditorActionContext context, JsonObject data) {
        Identifier soundId = Identifier.tryParse(data.get("sound").getAsString());
        if (soundId == null || context.world() == null) {
            return;
        }

        var soundEvent = Registries.SOUND_EVENT.get(soundId);
        if (soundEvent == null) {
            return;
        }

        Vec3d origin = context.zone().center();
        context.world().playSoundClient(
                origin.x,
                origin.y,
                origin.z,
                soundEvent,
                SoundCategory.MASTER,
                data.get("volume").getAsFloat(),
                data.get("pitch").getAsFloat(),
                false
        );
    }

    private static void delay(EditorActionContext context, JsonObject data) {
        String eventId = data.get("eventId").getAsString();
        if (!eventId.isBlank()) {
            context.editorState().runtime().scheduleEvent(eventId, context.zone(), data.get("ticks").getAsInt());
        }
    }

    private static void runEvent(EditorActionContext context, JsonObject data) {
        String eventId = data.get("eventId").getAsString();
        if (!eventId.isBlank()) {
            context.editorState().runtime().executeEvent(eventId, context.zone());
        }
    }

    private static void showTitle(EditorActionContext context, JsonObject data) {
        if (context.player() instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.networkHandler.sendPacket(new TitleFadeS2CPacket(
                    data.get("fadeIn").getAsInt(),
                    data.get("stay").getAsInt(),
                    data.get("fadeOut").getAsInt()
            ));
            serverPlayer.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(data.get("title").getAsString())));
            serverPlayer.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal(data.get("subtitle").getAsString())));
            return;
        }

        if (context.player() != null) {
            context.player().sendMessage(Text.literal(data.get("title").getAsString() + " | " + data.get("subtitle").getAsString()), true);
        }
    }

    private static void runCommand(EditorActionContext context, JsonObject data) {
        if (context.client().player == null || !data.has("command")) {
            return;
        }

        String command = data.get("command").getAsString();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        context.client().player.networkHandler.sendChatCommand(command);
    }
}
