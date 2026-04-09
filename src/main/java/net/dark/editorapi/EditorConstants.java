package net.dark.editorapi;

import net.minecraft.util.Identifier;

public final class EditorConstants {
    public static final String MOD_ID = "editorapi";
    public static final String PROJECT_FILE_NAME = "editor_project.json";
    public static final String KEY_CATEGORY = "key.categories.editorapi";

    private EditorConstants() {
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
