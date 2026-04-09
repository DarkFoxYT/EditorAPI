package net.dark.editorapi;

import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.fabricmc.api.ModInitializer;

public class Editorapi implements ModInitializer {

    @Override
    public void onInitialize() {
        BuiltinEditorActions.bootstrap(EditorActionRegistry.getInstance());
    }
}
