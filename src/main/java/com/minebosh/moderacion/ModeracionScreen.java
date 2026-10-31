package com.minebosh.moderacion;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;

public class ModeracionScreen extends Screen {

    // ===== EDITA AQUI =====
    // {motivo, tiempo #1, tiempo #2, tiempo #3}
    private static final String[][] MOTIVOS_MUTE = {
            {"Lenguaje Ofensivo", "2h", "6h", "1d"},
            {"Flood", "30m", "1h", "2h"},
            {"Insultos Al Servidor", "8h", "16h", "1d"},
            {"Falta de Respeto al Staff", "4h", "12h", "1d"},
            {"Nombrar servidores Externos", "30m", "1h", "2h"},
            {"Tradeos Externos", "2h", "6h", "15h"}
    };
    // Solo conozco "Evadir SS #1 35d": cambia los demás motivos y tiempos por los tuyos
    private static final String[][] MOTIVOS_BAN = {
            {"Evadir SS", "35d", "35d", "35d"}
    };
    // Servidores del desplegable de /server: pon los tuyos
    private static final String[] SERVIDORES = {"lobby", "survival", "skyblock"};
    // ======================

    private static final int ANCHO = 280;
    private static final int FONDO = 0xFF0A0A14;
    private static final int ACENTO = 0xFF00E5FF;
    private static final int MORADO = 0xFFB000FF;

    private final Config.Nivel nivel;

    private String nick = "";
    private boolean modoBan = false;
    private int lvl = 0;          // 0..2  => #1..#3
    private int motivoIdx = 0;
    private int abierto = 0;      // 0 ninguno, 1 nivel, 2 motivo, 3 server

    private TextFieldWidget campoNick;
    private final List<ButtonWidget> sugerencias = new ArrayList<>();
    private int previewY = -1;

    public ModeracionScreen(Config.Nivel nivel) {
        super(Text.literal("Moderacion"));
        this.nivel = nivel;
    }

    @Override
    protected void init() {
        sugerencias.clear();
        int x = width / 2 - ANCHO / 2;

        campoNick = new TextFieldWidget(textRenderer, x, 50, ANCHO, 20, Text.literal("Nick"));
        campoNick.setMaxLength(32);
        campoNick.setPlaceholder(Text.literal("Nombre de usuario"));
        campoNick.setText(nick);
        campoNick.setChangedListener(t -> {
            nick = t;
            refrescarSugerencias();
        });
        addDrawableChild(campoNick);
        setInitialFocus(campoNick);
        refrescarSugerencias();

        if (nivel == Config.Nivel.HELPER) {
            initHelper(x);
        } else {
            initAdmin(x);
        }
    }

    // ---------- Autocompletado (debajo del campo) ----------
    private void refrescarSugerencias() {
        for (ButtonWidget b : sugerencias) remove(b);
        sugerencias.clear();
        if (client == null || client.getNetworkHandler() == null || nick.isEmpty()) return;

        int x = width / 2 - ANCHO / 2;
        String q = nick.toLowerCase();
        int i = 0;
        for (PlayerListEntry e : client.getNetworkHandler().getPlayerList()) {
            String n = e.getProfile().name();
            if (n.toLowerCase().startsWith(q) && !n.equalsIgnoreCase(nick)) {
                ButtonWidget b = ButtonWidget.builder(Text.literal(n), btn -> campoNick.setText(n))
                        .dimensions(x, 72 + i * 16, ANCHO, 16).build();
                sugerencias.add(b);
                addDrawableChild(b);
                if (++i >= 4) break;
            }
        }
    }

    // ---------- Panel reducido (helper) ----------
    private void initHelper(int x) {
        addDrawableChild(boton("ipmute", x, 140, ANCHO, 20,
                b -> enviar("ipmute " + nick.trim())));
        addDrawableChild(boton("/hist", x, 165, ANCHO, 20,
                b -> enviar("hist " + nick.trim())));
        addDrawableChild(boton("Copiar Último Mute", x, 190, ANCHO, 20, b -> {
            if (UltimoMute.texto != null) client.keyboard.setClipboard(UltimoMute.texto);
        }));
    }

    // ---------- Panel completo (admin) ----------
    private void initAdmin(int x) {
        int y = 140;
        addDrawableChild(boton("Modo: " + (modoBan ? "BAN" : "MUTE"), x, y, 135, 20, b -> {
            modoBan = !modoBan;
            motivoIdx = 0;
            abierto = 0;
            clearAndInit();
        }));
        addDrawableChild(boton("Nivel #" + (lvl + 1), x + 145, y, 135, 20, b -> {
            abierto = abierto == 1 ? 0 : 1;
            clearAndInit();
        }));
        addDrawableChild(boton(lista()[motivoIdx][0], x, y + 25, ANCHO, 20, b -> {
            abierto = abierto == 2 ? 0 : 2;
            clearAndInit();
        }));

        int off = 0;
        int oy = y + 50;
        if (abierto == 1) {
            for (int i = 0; i < 3; i++) {
                final int idx = i;
                addDrawableChild(boton("Nivel #" + (i + 1), x, oy + i * 18, ANCHO, 18, b -> {
                    lvl = idx;
                    abierto = 0;
                    clearAndInit();
                }));
            }
            off = 3 * 18;
        } else if (abierto == 2) {
            String[][] l = lista();
            for (int i = 0; i < l.length; i++) {
                final int idx = i;
                addDrawableChild(boton(l[i][0], x, oy + i * 18, ANCHO, 18, b -> {
                    motivoIdx = idx;
                    abierto = 0;
                    clearAndInit();
                }));
            }
            off = l.length * 18;
        }

        int y2 = oy + off + 4;
        previewY = y2;

        addDrawableChild(boton(modoBan ? "Banear" : "Mutear", x, y2 + 16, 135, 20, b -> ejecutarAccion()));
        addDrawableChild(boton("Historial", x + 145, y2 + 16, 135, 20,
                b -> enviar("hist " + nick.trim())));

        if (!modoBan) {
            addDrawableChild(boton("Copiar", x, y2 + 40, 135, 20, b -> {
                String n = nick.trim();
                if (n.isEmpty()) return;
                UltimoMute.texto = textoCopia(n);
                client.keyboard.setClipboard(UltimoMute.texto);
            }));
        }
        addDrawableChild(boton("Server", x + 145, y2 + 40, 135, 20, b -> {
            abierto = abierto == 3 ? 0 : 3;
            clearAndInit();
        }));

        if (abierto == 3) {
            for (int i = 0; i < SERVIDORES.length; i++) {
                final String s = SERVIDORES[i];
                addDrawableChild(boton(s, x + 145, y2 + 62 + i * 18, 135, 18, b -> {
                    enviar("server " + s);
                    abierto = 0;
                    clearAndInit();
                }));
            }
        }
    }

    // ---------- Lógica ----------
    private String[][] lista() {
        return modoBan ? MOTIVOS_BAN : MOTIVOS_MUTE;
    }

    private String razon() {
        return lista()[motivoIdx][0] + " #" + (lvl + 1);
    }

    private String tiempo() {
        return lista()[motivoIdx][1 + lvl];
    }

    private String textoCopia(String n) {
        return "Nick: " + n + "\nRazón: " + razon() + "\nTiempo: " + tiempo();
    }

    private void ejecutarAccion() {
        String n = nick.trim();
        if (n.isEmpty()) return;
        if (!modoBan) UltimoMute.texto = textoCopia(n);
        enviar((modoBan ? "ban " : "mute ") + n + " " + razon() + " " + tiempo());
    }

    private void enviar(String comando) {
        if (client != null && client.player != null) {
            client.player.networkHandler.sendChatCommand(comando);
        }
    }

    private ButtonWidget boton(String texto, int x, int y, int w, int h, ButtonWidget.PressAction accion) {
        return ButtonWidget.builder(Text.literal(texto), accion).dimensions(x, y, w, h).build();
    }

    // ---------- Dibujo ----------
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = width / 2 - ANCHO / 2;

        context.fill(0, 0, width, height, FONDO);
        context.fill(x - 10, 18, x + ANCHO + 10, 20, ACENTO);
        context.fill(x - 10, height - 20, x + ANCHO + 10, height - 18, MORADO);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("MODERACION MINEBOSH"), width / 2, 30, ACENTO);
        if (nivel == Config.Nivel.HELPER) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal("Modo helper"), width / 2, 40, MORADO);
        }

        if (nivel == Config.Nivel.ADMIN && previewY >= 0) {
            String n = nick.trim().isEmpty() ? "<nick>" : nick.trim();
            String cmd = "/" + (modoBan ? "ban " : "mute ") + n + " " + razon() + " " + tiempo();
            context.drawTextWithShadow(textRenderer, Text.literal(cmd), x, previewY, 0xFFFFFFFF);
        }
    }
}
