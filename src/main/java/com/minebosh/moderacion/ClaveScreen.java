package com.minebosh.moderacion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

public class ClaveScreen extends Screen {
    private static final int BLOQUEO_TICKS = 5 * 60 * 20;
    private static final int NARANJA = 0xFFFF7A00;
    private static final int NARANJA_OSCURO = 0xFFB34F00;

    // static: se conservan al salir y volver a entrar
    private static int fallos = 0;
    private static int ticksRestantes = 0;

    private TextFieldWidget campo;
    private ButtonWidget botonEntrar;
    private String mensaje = "";

    public ClaveScreen() {
        super(Text.literal("Acceso"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        campo = new TextFieldWidget(textRenderer, cx - 100, cy - 10, 200, 20, Text.literal("Clave"));
        campo.setMaxLength(64);
        campo.setRenderTextProvider((s, i) ->
                OrderedText.styledForwardsVisitedString("*".repeat(s.length()), Style.EMPTY));
        addDrawableChild(campo);
        setInitialFocus(campo);

        botonEntrar = ButtonWidget.builder(Text.literal("Entrar"), b -> intentar())
                .dimensions(cx - 50, cy + 20, 100, 20).build();
        addDrawableChild(botonEntrar);

        actualizarEstado();
    }

    private void intentar() {
        if (ticksRestantes > 0) return;

        Config.Nivel nivel = Config.nivelPara(campo.getText());
        if (nivel != Config.Nivel.NINGUNO) {
            fallos = 0;
            client.setScreen(new ModeracionScreen(nivel));
            return;
        }

        fallos++;
        campo.setText("");
        if (fallos >= 3) {
            ticksRestantes = BLOQUEO_TICKS;
            mensaje = "";
        } else {
            mensaje = "Clave incorrecta (" + fallos + "/3)";
        }
        actualizarEstado();
    }

    private void actualizarEstado() {
        boolean bloqueado = ticksRestantes > 0;
        campo.setEditable(!bloqueado);
        botonEntrar.active = !bloqueado;
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksRestantes > 0) {
            ticksRestantes--;
            if (ticksRestantes == 0) {
                fallos = 0;
                mensaje = "";
            }
            actualizarEstado();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xFF0B0B0B);
        context.fill(20, 20, width - 20, 22, NARANJA);
        context.fill(20, height - 22, width - 20, height - 20, NARANJA);
        context.fill(20, 20, 22, height - 20, NARANJA);
        context.fill(width - 22, 20, width - 20, height - 20, NARANJA);

        super.render(context, mouseX, mouseY, delta);

        int cx = width / 2;
        int cy = height / 2;
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("ACCESO RESTRINGIDO"), cx, cy - 50, NARANJA);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Introduce la clave"), cx, cy - 30, NARANJA_OSCURO);

        if (ticksRestantes > 0) {
            int seg = (ticksRestantes + 19) / 20;
            String t = String.format("Bloqueado: %02d:%02d", seg / 60, seg % 60);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(t), cx, cy + 50, NARANJA);
        } else if (!mensaje.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(mensaje), cx, cy + 50, NARANJA);
        }
    }
}
