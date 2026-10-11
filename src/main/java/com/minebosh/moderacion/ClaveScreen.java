package com.minebosh.moderacion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Pantalla que pide la clave antes de abrir el panel de moderación. */
public class ClaveScreen extends Screen {

    /** Fallos seguidos que activan el bloqueo. */
    private static final int MAX_INTENTOS = 3;

    /** Duración del bloqueo: 5 minutos. */
    private static final long BLOQUEO_MS = 5 * 60 * 1000L;

    // Estado estático: se conserva aunque cierres y vuelvas a abrir esta pantalla.
    private static int intentosFallidos = 0;
    /** Tiempo que le queda al bloqueo (0 = sin bloqueo). Solo baja mientras esta pantalla está abierta. */
    private static long restanteMs = 0;

    private TextFieldWidget campo;
    private ButtonWidget botonEntrar;
    private boolean error = false;
    private long ultimoMs;

    public ClaveScreen() {
        super(Text.literal("Panel de moderación"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        campo = new TextFieldWidget(this.textRenderer, cx - 100, cy - 10, 200, 20, Text.literal("Clave"));
        campo.setMaxLength(64);
        // Muestra asteriscos en lugar de lo que se escribe.
        campo.setRenderTextProvider((texto, inicio) ->
                OrderedText.styledForwardsVisitedString("*".repeat(texto.length()), Style.EMPTY));
        this.addDrawableChild(campo);
        this.setInitialFocus(campo);

        botonEntrar = ButtonWidget.builder(Text.literal("Entrar"), b -> comprobar())
                .dimensions(cx - 100, cy + 20, 98, 20).build();
        this.addDrawableChild(botonEntrar);
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> this.close())
                .dimensions(cx + 2, cy + 20, 98, 20).build());

        // Al abrir la pantalla la cuenta atrás continúa desde donde se quedó.
        ultimoMs = System.currentTimeMillis();
        actualizarBloqueo();
    }

    /** Resta el tiempo transcurrido (solo si hay bloqueo) y activa o desactiva los controles. */
    private void actualizarBloqueo() {
        long ahora = System.currentTimeMillis();
        if (restanteMs > 0) {
            restanteMs -= ahora - ultimoMs;
            if (restanteMs <= 0) {
                restanteMs = 0;
                error = false;
            }
        }
        ultimoMs = ahora;

        boolean bloqueado = restanteMs > 0;
        campo.setEditable(!bloqueado);
        botonEntrar.active = !bloqueado;
    }

    private void comprobar() {
        if (restanteMs > 0) {
            return; // Bloqueado: no se puede probar nada.
        }

        if (Config.claveCorrecta(campo.getText())) {
            intentosFallidos = 0;
            Config.desbloqueado = true;
            this.client.setScreen(new ModeracionScreen());
        } else {
            error = true;
            campo.setText("");
            intentosFallidos++;
            if (intentosFallidos >= MAX_INTENTOS) {
                intentosFallidos = 0;
                restanteMs = BLOQUEO_MS;
                ultimoMs = System.currentTimeMillis();
                actualizarBloqueo();
            }
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            comprobar();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        actualizarBloqueo();

        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Introduce la clave"), this.width / 2, this.height / 2 - 28, 0xAAAAAA);

        if (restanteMs > 0) {
            long seg = (restanteMs + 999) / 1000;
            String tiempo = String.format("%d:%02d", seg / 60, seg % 60);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Demasiados intentos. Espera " + tiempo), this.width / 2, this.height / 2 + 50, 0xFF5555);
        } else if (error) {
            int quedan = MAX_INTENTOS - intentosFallidos;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Clave incorrecta (intentos restantes: " + quedan + ")"), this.width / 2, this.height / 2 + 50, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
