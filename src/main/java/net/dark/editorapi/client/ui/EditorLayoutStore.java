package net.dark.editorapi.client.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.dark.editorapi.EditorConstants;
import net.minecraft.client.MinecraftClient;

public final class EditorLayoutStore {
    private JsonObject root = new JsonObject();

    public void load() {
        Path path = resolvePath();
        if (!Files.exists(path)) {
            this.root = new JsonObject();
            return;
        }
        try {
            this.root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (Exception ignored) {
            this.root = new JsonObject();
        }
    }

    public void savePanel(String id, EditorPanel panel) {
        JsonObject layout = this.root.has("panels") ? this.root.getAsJsonObject("panels") : new JsonObject();
        JsonObject bounds = new JsonObject();
        bounds.addProperty("x", panel.x());
        bounds.addProperty("y", panel.y());
        bounds.addProperty("width", panel.width());
        bounds.addProperty("height", panel.height());
        layout.add(id, bounds);
        this.root.add("panels", layout);
        flush();
    }

    public void applyPanel(String id, EditorPanel panel) {
        if (!this.root.has("panels")) {
            return;
        }
        JsonObject layout = this.root.getAsJsonObject("panels");
        if (!layout.has(id)) {
            return;
        }
        JsonObject bounds = layout.getAsJsonObject(id);
        panel.setBounds(
                bounds.get("x").getAsInt(),
                bounds.get("y").getAsInt(),
                bounds.get("width").getAsInt(),
                bounds.get("height").getAsInt()
        );
    }

    private void flush() {
        Path path = resolvePath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, this.root.toString());
        } catch (IOException ignored) {
        }
    }

    private Path resolvePath() {
        return MinecraftClient.getInstance().runDirectory.toPath()
                .resolve("config")
                .resolve(EditorConstants.MOD_ID)
                .resolve("editor-layout.json");
    }
}
