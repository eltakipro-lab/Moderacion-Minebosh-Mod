package com.minebosh.moderacion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ModeracionModClient implements ClientModInitializer {

    private static KeyBinding abrirPanel;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category categoria = KeyBinding.Category.create(Identifier.of("moderacion", "general"));

        abrirPanel = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderacion.abrir",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                categoria
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (abrirPanel.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new ClaveScreen());
                }
            }
        });
    }
}
