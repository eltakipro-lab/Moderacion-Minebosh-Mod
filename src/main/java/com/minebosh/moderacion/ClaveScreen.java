package com.minebosh.moderacion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/** Pantalla que pide la clave antes de abrir el panel de moderación (tema Halloween: naranja y negro). */
public class ClaveScreen extends Screen {

    /** Fallos seguidos que activan el bloqueo. */
    private static final int MAX_INTENTOS = 3;

    /** Duración del bloqueo: 5 minutos. */
    private static final long BLOQUEO_MS = 5 * 60 * 1000L;

    // Estado estático: se conserva aunque cierres y vuelvas a abrir esta pantalla.
    private static int intentosFallidos = 0;
    /** Tiempo que le queda al bloqueo (0 = sin bloqueo). Solo baja mientras esta pantalla está abierta. */
    private static long restanteMs = 0;

    // ------------------------------------------------------------------
    // Colores del tema Halloween
    // ------------------------------------------------------------------
    private static final int NEGRO = 0xFF000000;
    private static final int NARANJA = 0xFFFF7A00;
    private static final int NARANJA_CLARO = 0xFFFF9A1A;
    private static final int NARANJA_OSCURO = 0xFF8A3A00;
    private static final int NARANJA_ROJIZO = 0xFFFF4D00;
    private static final int VERDE_TALLO = 0xFF3B7A1E;

    private static final int PANEL_ANCHO = 260;
    private static final int PANEL_ALTO = 140;

    /** Calabaza en pixel art: O = naranja, K = negro (ojos y boca), S = tallo, . = vacío. */
    private static final String[] CALABAZA = {
            "....SS.....",
            "...OSSOO...",
            ".OOOOOOOOO.",
            "OOOOOOOOOOO",
            "OOKOOOOOKOO",
            "OKKKOOOKKKO",
            "OOOOOOOOOOO",
            "OKOKKKKKOKO",
            ".OKKOKOKKO.",
            "..OOOOOOO..",
    };

    private TextFieldWidget campo;
    private ButtonWidget botonEntrar;
    private boolean error = false;
    private long ultimoMs;

    private int panelX;
    private int panelY;

    public ClaveScreen() {
        super(Text.literal("PANEL DE MODERACIÓN"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        panelX = cx - PANEL_ANCHO / 2;
        panelY = this.height / 2 - PANEL_ALTO / 2;

        campo = new TextFieldWidget(this.textRenderer, cx - 100, panelY + 62, 200, 20, Text.literal("Clave"));
        campo.setMaxLength(64);
        // Muestra asteriscos naranjas en lugar de lo que se escribe.
        campo.setRenderTextProvider((texto, inicio) ->
                OrderedText.styledForwardsVisitedString("*".repeat(texto.length()),
                        Style.EMPTY.withColor(NARANJA_CLARO & 0xFFFFFF)));
        this.addDrawableChild(campo);
        this.setInitialFocus(campo);

        botonEntrar = ButtonWidget.builder(textoBoton("Entrar"), b -> comprobar())
                .dimensions(cx - 100, panelY + 90, 98, 20).build();
        this.addDrawableChild(botonEntrar);
        this.addDrawableChild(ButtonWidget.builder(textoBoton("Cancelar"), b -> this.close())
                .dimensions(cx + 2, panelY + 90, 98, 20).build());

        // Al abrir la pantalla la cuenta atrás continúa desde donde se quedó.
        ultimoMs = System.currentTimeMillis();
        actualizarBloqueo();
    }

    private static Text textoBoton(String texto) {
        return Text.literal(texto).styled(s -> s.withColor(NARANJA_CLARO & 0xFFFFFF).withBold(true));
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

    // ------------------------------------------------------------------
    // Dibujo
    // ------------------------------------------------------------------

    /** Mezcla dos colores ARGB opacos; t va de 0 a 1. */
    private static int mezclar(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private void dibujarMarco(DrawContext c, int x, int y, int w, int h, int grosor, int color) {
        c.fill(x, y, x + w, y + grosor, color);
        c.fill(x, y + h - grosor, x + w, y + h, color);
        c.fill(x, y, x + grosor, y + h, color);
        c.fill(x + w - grosor, y, x + w, y + h, color);
    }

    private void dibujarCalabaza(DrawContext c, int x, int y, int p) {
        for (int fila = 0; fila < CALABAZA.length; fila++) {
            String linea = CALABAZA[fila];
            for (int col = 0; col < linea.length(); col++) {
                char ch = linea.charAt(col);
                int color;
                if (ch == 'O') {
                    color = NARANJA;
                } else if (ch == 'S') {
                    color = VERDE_TALLO;
                } else if (ch == 'K') {
                    color = NEGRO;
                } else {
                    continue;
                }
                c.fill(x + col * p, y + fila * p, x + (col + 1) * p, y + (fila + 1) * p, color);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        actualizarBloqueo();

        int cx = this.width / 2;

        // Fondo: negro con un resplandor naranja muy oscuro abajo.
        context.fillGradient(0, 0, this.width, this.height, NEGRO, 0xFF2A1000);

        // Panel negro con borde naranja que "late" suavemente.
        float pulso = (float) ((Math.sin(System.currentTimeMillis() / 450.0) + 1.0) / 2.0);
        int colorBorde = mezclar(NARANJA_OSCURO, NARANJA_CLARO, pulso);
        context.fill(panelX, panelY, panelX + PANEL_ANCHO, panelY + PANEL_ALTO, NEGRO);
        dibujarMarco(context, panelX, panelY, PANEL_ANCHO, PANEL_ALTO, 2, colorBorde);
        dibujarMarco(context, panelX + 5, panelY + 5, PANEL_ANCHO - 10, PANEL_ALTO - 10, 1, NARANJA_OSCURO);

        // Calabazas a los lados del título.
        int p = 3;
        int anchoCalabaza = CALABAZA[0].length() * p;
        dibujarCalabaza(context, panelX + 14, panelY + 12, p);
        dibujarCalabaza(context, panelX + PANEL_ANCHO - 14 - anchoCalabaza, panelY + 12, p);

        // Textos naranjas.
        Text titulo = Text.literal("PANEL DE MODERACIÓN").formatted(Formatting.BOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, titulo, cx, panelY + 16, NARANJA_CLARO);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("- Solo para el staff -"), cx, panelY + 30, 0xFFCC6600);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Introduce la clave"), cx, panelY + 48, NARANJA);

        if (restanteMs > 0) {
            long seg = (restanteMs + 999) / 1000;
            String tiempo = String.format("%d:%02d", seg / 60, seg % 60);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Demasiados intentos. Espera " + tiempo).formatted(Formatting.BOLD),
                    cx, panelY + 118, NARANJA_ROJIZO);
        } else if (error) {
            int quedan = MAX_INTENTOS - intentosFallidos;
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Clave incorrecta (intentos restantes: " + quedan + ")"),
                    cx, panelY + 118, NARANJA_ROJIZO);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}
