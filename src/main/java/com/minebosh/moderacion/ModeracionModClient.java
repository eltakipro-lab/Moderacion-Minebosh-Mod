package com.minebosh.moderacion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Punto de entrada del mod en el cliente.
 * Registra el atajo de teclado que abre el panel de moderación
 * (ModeracionScreen) y se encarga de mostrarlo cuando se pulsa.
 */
public class ModeracionModClient implements ClientModInitializer {

    private static KeyBinding abrirPanelKey;

    @Override
    public void onInitializeClient() {
        // Tecla por defecto: "]" (corchete derecho). Se puede remapear
        // libremente desde Opciones -> Controles dentro del juego.
        abrirPanelKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderacion-minebosh-tabs.abrir",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                "category.moderacion-minebosh-tabs"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (abrirPanelKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new ModeracionScreen());
                }
            }
        });
    }
}
