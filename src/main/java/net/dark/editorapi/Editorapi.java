package net.dark.editorapi;

import net.dark.editorapi.api.action.EditorActionRegistry;
import net.dark.editorapi.api.action.builtin.BuiltinEditorActions;
import net.dark.editorapi.network.EditorNetworking;
import net.fabricmc.api.ModInitializer;

public class Editorapi implements ModInitializer {

    @Override
    public void onInitialize() {
        EditorNetworking.bootstrapCommon();
        EditorNetworking.bootstrapServer();
        BuiltinEditorActions.bootstrap(EditorActionRegistry.getInstance());
    }
}
