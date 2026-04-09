package net.dark.editorapi.model;

import com.google.gson.JsonObject;
import net.minecraft.util.Identifier;

public record EditorActionInstance(Identifier actionId, JsonObject data) {
    public EditorActionInstance copy() {
        return new EditorActionInstance(this.actionId, this.data.deepCopy());
    }
}
