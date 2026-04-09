package net.dark.editorapi.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.dark.editorapi.EditorConstants;
import net.dark.editorapi.model.EditorBlueprintAsset;
import net.dark.editorapi.model.CutsceneDefinition;
import net.dark.editorapi.model.CutsceneKeyframe;
import net.dark.editorapi.model.EditorActionInstance;
import net.dark.editorapi.model.EditorEventDefinition;
import net.dark.editorapi.model.EditorProject;
import net.dark.editorapi.model.InterpolationMode;
import net.dark.editorapi.model.PlacedBlueprint;
import net.dark.editorapi.model.TriggerOnceMode;
import net.dark.editorapi.model.TriggerTargetMode;
import net.dark.editorapi.model.TriggerZone;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class EditorProjectStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public EditorProject loadOrCreate() {
        Path path = resolvePath();
        if (Files.exists(path)) {
            try {
                return fromJson(JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to read " + path, exception);
            }
        }

        EditorProject project = new EditorProject();
        project.ensureBootstrapData();
        save(project);
        return project;
    }

    public void save(EditorProject project) {
        Path path = resolvePath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, serialize(project));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write " + path, exception);
        }
    }

    public String serialize(EditorProject project) {
        return GSON.toJson(toJson(project));
    }

    public EditorProject deserialize(String rawJson) {
        return fromJson(JsonParser.parseString(rawJson).getAsJsonObject());
    }

    private Path resolvePath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve(EditorConstants.MOD_ID)
                .resolve(EditorConstants.PROJECT_FILE_NAME);
    }

    private JsonObject toJson(EditorProject project) {
        JsonObject root = new JsonObject();
        JsonArray blueprintAssets = new JsonArray();
        JsonArray blueprints = new JsonArray();
        JsonArray zones = new JsonArray();
        JsonArray events = new JsonArray();
        JsonArray cutscenes = new JsonArray();

        for (Map.Entry<UUID, EditorBlueprintAsset> entry : project.blueprintAssets().entrySet()) {
            EditorBlueprintAsset asset = entry.getValue();
            JsonObject json = new JsonObject();
            json.addProperty("id", asset.id().toString());
            json.addProperty("name", asset.name());
            json.add("zones", writeZoneArray(asset.zones()));
            json.add("events", writeEventArray(asset.events()));
            json.add("cutscenes", writeCutsceneArray(asset.cutscenes()));
            blueprintAssets.add(json);
        }

        for (Map.Entry<UUID, PlacedBlueprint> entry : project.blueprints().entrySet()) {
            PlacedBlueprint blueprint = entry.getValue();
            JsonObject json = new JsonObject();
            json.addProperty("id", blueprint.id().toString());
            json.addProperty("assetId", blueprint.assetId().toString());
            json.addProperty("name", blueprint.name());
            json.add("origin", writeVec3(blueprint.origin()));
            json.addProperty("yaw", blueprint.yaw());
            json.addProperty("visible", blueprint.visible());
            json.addProperty("locked", blueprint.locked());
            json.add("zoneIds", writeUuidArray(blueprint.zoneIds()));
            json.add("eventIds", writeUuidArray(blueprint.eventIds()));
            json.add("cutsceneIds", writeUuidArray(blueprint.cutsceneIds()));
            blueprints.add(json);
        }

        for (Map.Entry<UUID, TriggerZone> entry : project.zones().entrySet()) {
            TriggerZone zone = entry.getValue();
            zones.add(writeZone(zone));
        }

        for (Map.Entry<UUID, EditorEventDefinition> entry : project.events().entrySet()) {
            events.add(writeEvent(entry.getValue()));
        }

        for (Map.Entry<UUID, CutsceneDefinition> entry : project.cutscenes().entrySet()) {
            cutscenes.add(writeCutscene(entry.getValue()));
        }

        root.add("blueprintAssets", blueprintAssets);
        root.add("blueprints", blueprints);
        root.add("zones", zones);
        root.add("events", events);
        root.add("cutscenes", cutscenes);
        return root;
    }

    private EditorProject fromJson(JsonObject root) {
        EditorProject project = new EditorProject();

        JsonArray blueprintAssetsJson = root.has("blueprintAssets") ? root.getAsJsonArray("blueprintAssets") : new JsonArray();
        for (JsonElement element : blueprintAssetsJson) {
            JsonObject json = element.getAsJsonObject();
            EditorBlueprintAsset asset = new EditorBlueprintAsset(
                    UUID.fromString(json.get("id").getAsString()),
                    json.get("name").getAsString(),
                    readZoneArray(json.getAsJsonArray("zones")),
                    readEventArray(json.getAsJsonArray("events")),
                    readCutsceneArray(json.getAsJsonArray("cutscenes"))
            );
            project.blueprintAssets().put(asset.id(), asset);
        }

        JsonArray eventsJson = root.has("events") ? root.getAsJsonArray("events") : new JsonArray();
        for (JsonElement element : eventsJson) {
            EditorEventDefinition event = readEvent(element.getAsJsonObject());
            project.events().put(event.id(), event);
        }

        JsonArray zonesJson = root.has("zones") ? root.getAsJsonArray("zones") : new JsonArray();
        for (JsonElement element : zonesJson) {
            TriggerZone zone = readZone(element.getAsJsonObject());
            project.zones().put(zone.id(), zone);
        }

        JsonArray cutscenesJson = root.has("cutscenes") ? root.getAsJsonArray("cutscenes") : new JsonArray();
        for (JsonElement element : cutscenesJson) {
            CutsceneDefinition cutscene = readCutscene(element.getAsJsonObject());
            project.cutscenes().put(cutscene.id(), cutscene);
        }

        JsonArray blueprintsJson = root.has("blueprints") ? root.getAsJsonArray("blueprints") : new JsonArray();
        for (JsonElement element : blueprintsJson) {
            JsonObject json = element.getAsJsonObject();
            PlacedBlueprint blueprint = new PlacedBlueprint(
                    UUID.fromString(json.get("id").getAsString()),
                    UUID.fromString(json.get("assetId").getAsString()),
                    json.get("name").getAsString(),
                    readVec3(json.getAsJsonObject("origin")),
                    json.has("yaw") ? json.get("yaw").getAsFloat() : 0.0F,
                    !json.has("visible") || json.get("visible").getAsBoolean(),
                    json.has("locked") && json.get("locked").getAsBoolean(),
                    readUuidArray(json.getAsJsonArray("zoneIds")),
                    readUuidArray(json.getAsJsonArray("eventIds")),
                    readUuidArray(json.getAsJsonArray("cutsceneIds"))
            );
            project.blueprints().put(blueprint.id(), blueprint);
        }

        project.ensureBootstrapData();
        return project;
    }

    private JsonObject writeZone(TriggerZone zone) {
        JsonObject json = new JsonObject();
        json.addProperty("id", zone.id().toString());
        json.addProperty("name", zone.name());
        json.add("pos1", writeBlockPos(zone.pos1()));
        json.add("pos2", writeBlockPos(zone.pos2()));
        json.addProperty("showFill", zone.showFill());
        json.addProperty("triggerEnter", zone.triggerEnter());
        json.addProperty("triggerExit", zone.triggerExit());
        json.addProperty("triggerWhileInside", zone.triggerWhileInside());
        json.addProperty("triggerTimeInside", zone.triggerTimeInside());
        json.addProperty("whileInsideIntervalTicks", zone.whileInsideIntervalTicks());
        json.addProperty("requiredTimeTicks", zone.requiredTimeTicks());
        json.addProperty("delayTicks", zone.delayTicks());
        json.addProperty("radius", zone.radius());
        json.addProperty("onceMode", zone.onceMode().name());
        json.addProperty("targetMode", zone.targetMode().name());
        json.addProperty("eventId", zone.eventId().toString());
        json.addProperty("enterEventId", zone.enterEventId().toString());
        json.addProperty("exitEventId", zone.exitEventId().toString());
        json.addProperty("visible", zone.visible());
        json.addProperty("locked", zone.locked());
        return json;
    }

    private TriggerZone readZone(JsonObject json) {
        return new TriggerZone(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                readBlockPos(json.getAsJsonObject("pos1")),
                readBlockPos(json.getAsJsonObject("pos2")),
                json.get("showFill").getAsBoolean(),
                json.get("triggerEnter").getAsBoolean(),
                json.get("triggerExit").getAsBoolean(),
                json.get("triggerWhileInside").getAsBoolean(),
                json.get("triggerTimeInside").getAsBoolean(),
                json.get("whileInsideIntervalTicks").getAsInt(),
                json.get("requiredTimeTicks").getAsInt(),
                json.get("delayTicks").getAsInt(),
                json.get("radius").getAsFloat(),
                TriggerOnceMode.valueOf(json.get("onceMode").getAsString()),
                TriggerTargetMode.valueOf(json.get("targetMode").getAsString()),
                UUID.fromString(json.get("eventId").getAsString()),
                json.has("enterEventId") ? UUID.fromString(json.get("enterEventId").getAsString()) : UUID.fromString(json.get("eventId").getAsString()),
                json.has("exitEventId") ? UUID.fromString(json.get("exitEventId").getAsString()) : UUID.fromString(json.get("eventId").getAsString()),
                !json.has("visible") || json.get("visible").getAsBoolean(),
                json.has("locked") && json.get("locked").getAsBoolean()
        );
    }

    private JsonArray writeZoneArray(List<TriggerZone> zones) {
        JsonArray array = new JsonArray();
        for (TriggerZone zone : zones) {
            array.add(writeZone(zone));
        }
        return array;
    }

    private List<TriggerZone> readZoneArray(JsonArray array) {
        List<TriggerZone> zones = new ArrayList<>();
        for (JsonElement element : array) {
            zones.add(readZone(element.getAsJsonObject()));
        }
        return zones;
    }

    private JsonObject writeEvent(EditorEventDefinition event) {
        JsonObject json = new JsonObject();
        json.addProperty("id", event.id().toString());
        json.addProperty("name", event.name());
        json.addProperty("visible", event.visible());
        json.addProperty("locked", event.locked());
        JsonArray actions = new JsonArray();
        for (EditorActionInstance action : event.actions()) {
            JsonObject actionJson = new JsonObject();
            actionJson.addProperty("actionId", action.actionId().toString());
            actionJson.add("data", action.data().deepCopy());
            actions.add(actionJson);
        }
        json.add("actions", actions);
        return json;
    }

    private EditorEventDefinition readEvent(JsonObject json) {
        List<EditorActionInstance> actions = new ArrayList<>();
        for (JsonElement actionElement : json.getAsJsonArray("actions")) {
            JsonObject actionJson = actionElement.getAsJsonObject();
            actions.add(new EditorActionInstance(
                    Identifier.of(actionJson.get("actionId").getAsString()),
                    actionJson.getAsJsonObject("data")
            ));
        }
        return new EditorEventDefinition(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                actions,
                !json.has("visible") || json.get("visible").getAsBoolean(),
                json.has("locked") && json.get("locked").getAsBoolean()
        );
    }

    private JsonArray writeEventArray(List<EditorEventDefinition> events) {
        JsonArray array = new JsonArray();
        for (EditorEventDefinition event : events) {
            array.add(writeEvent(event));
        }
        return array;
    }

    private List<EditorEventDefinition> readEventArray(JsonArray array) {
        List<EditorEventDefinition> events = new ArrayList<>();
        for (JsonElement element : array) {
            events.add(readEvent(element.getAsJsonObject()));
        }
        return events;
    }

    private JsonObject writeCutscene(CutsceneDefinition cutscene) {
        JsonObject json = new JsonObject();
        json.addProperty("id", cutscene.id().toString());
        json.addProperty("name", cutscene.name());
        json.addProperty("startFrame", cutscene.startFrame());
        json.addProperty("endFrame", cutscene.endFrame());
        json.addProperty("loop", cutscene.loop());
        json.addProperty("showPreview", cutscene.showPreview());
        json.addProperty("autoKeyframe", cutscene.autoKeyframe());
        json.addProperty("visible", cutscene.visible());
        json.addProperty("locked", cutscene.locked());
        JsonArray keyframes = new JsonArray();
        for (CutsceneKeyframe keyframe : cutscene.keyframes()) {
            JsonObject keyframeJson = new JsonObject();
            keyframeJson.addProperty("id", keyframe.id().toString());
            keyframeJson.addProperty("frame", keyframe.frame());
            keyframeJson.add("position", writeVec3(keyframe.position()));
            keyframeJson.addProperty("yaw", keyframe.yaw());
            keyframeJson.addProperty("pitch", keyframe.pitch());
            keyframeJson.addProperty("roll", keyframe.roll());
            keyframeJson.addProperty("fov", keyframe.fov());
            keyframeJson.addProperty("sway", keyframe.sway());
            keyframeJson.addProperty("interpolation", keyframe.interpolation().name());
            keyframes.add(keyframeJson);
        }
        json.add("keyframes", keyframes);
        return json;
    }

    private CutsceneDefinition readCutscene(JsonObject json) {
        List<CutsceneKeyframe> keyframes = new ArrayList<>();
        for (JsonElement keyframeElement : json.getAsJsonArray("keyframes")) {
            JsonObject keyframeJson = keyframeElement.getAsJsonObject();
            keyframes.add(new CutsceneKeyframe(
                    UUID.fromString(keyframeJson.get("id").getAsString()),
                    keyframeJson.get("frame").getAsInt(),
                    readVec3(keyframeJson.getAsJsonObject("position")),
                    keyframeJson.get("yaw").getAsFloat(),
                    keyframeJson.get("pitch").getAsFloat(),
                    keyframeJson.has("roll") ? keyframeJson.get("roll").getAsFloat() : 0.0F,
                    keyframeJson.has("fov") ? keyframeJson.get("fov").getAsFloat() : 70.0F,
                    keyframeJson.has("sway") ? keyframeJson.get("sway").getAsFloat() : 0.0F,
                    InterpolationMode.valueOf(keyframeJson.get("interpolation").getAsString())
            ));
        }
        return new CutsceneDefinition(
                UUID.fromString(json.get("id").getAsString()),
                json.get("name").getAsString(),
                json.get("startFrame").getAsInt(),
                json.get("endFrame").getAsInt(),
                json.get("loop").getAsBoolean(),
                json.get("showPreview").getAsBoolean(),
                json.get("autoKeyframe").getAsBoolean(),
                !json.has("visible") || json.get("visible").getAsBoolean(),
                json.has("locked") && json.get("locked").getAsBoolean(),
                keyframes
        );
    }

    private JsonArray writeCutsceneArray(List<CutsceneDefinition> cutscenes) {
        JsonArray array = new JsonArray();
        for (CutsceneDefinition cutscene : cutscenes) {
            array.add(writeCutscene(cutscene));
        }
        return array;
    }

    private List<CutsceneDefinition> readCutsceneArray(JsonArray array) {
        List<CutsceneDefinition> cutscenes = new ArrayList<>();
        for (JsonElement element : array) {
            cutscenes.add(readCutscene(element.getAsJsonObject()));
        }
        return cutscenes;
    }

    private JsonArray writeUuidArray(List<UUID> uuids) {
        JsonArray array = new JsonArray();
        for (UUID uuid : uuids) {
            array.add(uuid.toString());
        }
        return array;
    }

    private List<UUID> readUuidArray(JsonArray array) {
        List<UUID> uuids = new ArrayList<>();
        for (JsonElement element : array) {
            uuids.add(UUID.fromString(element.getAsString()));
        }
        return uuids;
    }

    private JsonObject writeBlockPos(BlockPos pos) {
        JsonObject json = new JsonObject();
        json.addProperty("x", pos.getX());
        json.addProperty("y", pos.getY());
        json.addProperty("z", pos.getZ());
        return json;
    }

    private BlockPos readBlockPos(JsonObject json) {
        return new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
    }

    private JsonObject writeVec3(Vec3d vec3d) {
        JsonObject json = new JsonObject();
        json.addProperty("x", vec3d.x);
        json.addProperty("y", vec3d.y);
        json.addProperty("z", vec3d.z);
        return json;
    }

    private Vec3d readVec3(JsonObject json) {
        return new Vec3d(json.get("x").getAsDouble(), json.get("y").getAsDouble(), json.get("z").getAsDouble());
    }
}
