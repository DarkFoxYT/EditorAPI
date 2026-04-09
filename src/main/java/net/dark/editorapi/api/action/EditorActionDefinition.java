package net.dark.editorapi.api.action;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.Identifier;

public final class EditorActionDefinition {
    private final Identifier id;
    private final String displayName;
    private final Supplier<JsonObject> defaultFactory;
    private final BiConsumer<EditorActionContext, JsonObject> executor;
    private final Function<JsonObject, List<String>> summaryFactory;

    public EditorActionDefinition(
            Identifier id,
            String displayName,
            Supplier<JsonObject> defaultFactory,
            BiConsumer<EditorActionContext, JsonObject> executor,
            Function<JsonObject, List<String>> summaryFactory
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.defaultFactory = Objects.requireNonNull(defaultFactory, "defaultFactory");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.summaryFactory = Objects.requireNonNull(summaryFactory, "summaryFactory");
    }

    public Identifier id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public JsonObject createDefaultData() {
        return this.defaultFactory.get().deepCopy();
    }

    public void execute(EditorActionContext context, JsonObject data) {
        this.executor.accept(context, data);
    }

    public List<String> summarize(JsonObject data) {
        return this.summaryFactory.apply(data.deepCopy());
    }
}
