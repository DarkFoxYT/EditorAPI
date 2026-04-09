package net.dark.editorapi.api.action;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

public final class EditorActionRegistry {
    private static final EditorActionRegistry INSTANCE = new EditorActionRegistry();

    private final Map<Identifier, EditorActionDefinition> actions = new LinkedHashMap<>();

    public static EditorActionRegistry getInstance() {
        return INSTANCE;
    }

    public void register(EditorActionDefinition definition) {
        this.actions.put(definition.id(), definition);
    }

    public EditorActionDefinition get(Identifier id) {
        return this.actions.get(id);
    }

    public Collection<EditorActionDefinition> values() {
        return this.actions.values();
    }
}
