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

    private TextFieldWidget campo;
    private boolean error = false;

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

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Entrar"), b -> comprobar())
                .dimensions(cx - 100, cy + 20, 98, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancelar"), b -> this.close())
                .dimensions(cx + 2, cy + 20, 98, 20).build());
    }

    private void comprobar() {
        if (Config.claveCorrecta(campo.getText())) {
            Config.desbloqueado = true;
            this.client.setScreen(new ModeracionScreen());
        } else {
            error = true;
            campo.setText("");
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
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 45, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Introduce la clave"), this.width / 2, this.height / 2 - 28, 0xAAAAAA);
        if (error) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Clave incorrecta"), this.width / 2, this.height / 2 + 50, 0xFF5555);
        }
        super.render(context, mouseX, mouseY, delta);
    }
}
