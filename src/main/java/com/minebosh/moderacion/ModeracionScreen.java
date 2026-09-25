package com.minebosh.moderacion;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ModeracionScreen extends Screen {

    // ---------- Layout ----------
    private static final int ANCHO_ETIQUETA = 190;
    private static final int ANCHO_BOTON = 64;
    private static final int ALTO = 20;
    private static final int ESPACIO = 4;
    private static final int FILAS_POR_PAGINA = 6;
    private static final int PADDING_PANEL = 12;

    // ---------- Colores ----------
    private static final int COLOR_TITULO = 0x55FFFF;
    private static final int COLOR_ACENTO = 0xFF55FFFF;
    private static final int COLOR_TEXTO = 0xFFFFFF;
    private static final int COLOR_SUAVE = 0xAAAAAA;
    private static final int COLOR_AVISO = 0xFFFF55;
    private static final int COLOR_FONDO_PANEL = 0xC8141420;
    private static final int COLOR_BORDE_PANEL = 0xFF3AA0FF;
    private static final int COLOR_FILA_PAR = 0x14FFFFFF;
    private static final int COLOR_SEPARADOR = 0x40FFFFFF;

    private enum Pestana { MUTES, BANEOS }

    private Pestana pestanaActual = Pestana.MUTES;
    private int paginaMutes = 0;
    private int paginaBaneos = 0;

    private TextFieldWidget campoUsuario;
    private String ultimoUsuario = "";

    private String ultimoTextoMute = "";
    private String ultimoTextoBan = "";
    private Text mensaje = Text.empty();

    private ButtonWidget tabMutesBtn;
    private ButtonWidget tabBaneosBtn;
    private ButtonWidget anteriorBtn;
    private ButtonWidget siguienteBtn;
    private ButtonWidget logsBtn;

    private final List<ButtonWidget> filasBotones = new ArrayList<>();

    private int panelX;
    private int panelAncho;
    private int tituloY;
    private int tabsY;
    private int usuarioY;
    private int filasY0;
    private int paginacionY;
    private int accionesY;
    private int utilidadesLabelY;
    private int utilidadesY;
    private int mensajeY;
    private int panelTopY;
    private int panelBottomY;

    public ModeracionScreen() {
        super(Text.of("Moderación Minebosh"));
    }

    @Override
    protected void init() {
        this.panelAncho = ANCHO_ETIQUETA + (ANCHO_BOTON * 3) + (ESPACIO * 4);
        this.panelX = (this.width - panelAncho) / 2;

        this.tituloY = 14;
        this.tabsY = tituloY + 20;

        int mitadTab = panelAncho / 2 - 2;
        this.tabMutesBtn = ButtonWidget.builder(Text.of("Mutes"), b -> cambiarPestana(Pestana.MUTES))
                .dimensions(panelX, tabsY, mitadTab, ALTO)
                .build();
        this.tabBaneosBtn = ButtonWidget.builder(Text.of("Baneos"), b -> cambiarPestana(Pestana.BANEOS))
                .dimensions(panelX + mitadTab + 4, tabsY, mitadTab, ALTO)
                .build();
        this.addDrawableChild(tabMutesBtn);
        this.addDrawableChild(tabBaneosBtn);

        this.usuarioY = tabsY + ALTO + ESPACIO * 2;
        int anchoBotonUsuario = 54;
        int anchoBotonesUsuario = anchoBotonUsuario * 3 + ESPACIO * 2; // Historial + SS + Logs
        this.campoUsuario = new TextFieldWidget(
                this.textRenderer,
                panelX,
                usuarioY,
                panelAncho - anchoBotonesUsuario - ESPACIO,
                ALTO,
                Text.of("Usuario"));
        this.campoUsuario.setMaxLength(32);
        this.campoUsuario.setSuggestion("Nombre del usuario"); // en 1.20.1 no existe setPlaceholder
        this.campoUsuario.setText(ultimoUsuario);
        this.addDrawableChild(campoUsuario);

        int xBotonesUsuario = panelX + panelAncho - anchoBotonesUsuario;

        ButtonWidget historialBtn = ButtonWidget.builder(Text.of("Historial"), b -> ejecutarHistorial())
                .dimensions(xBotonesUsuario, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.addDrawableChild(historialBtn);

        ButtonWidget ssBtn = ButtonWidget.builder(Text.of("SS"), b -> ejecutarSS())
                .dimensions(xBotonesUsuario + anchoBotonUsuario + ESPACIO, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.addDrawableChild(ssBtn);

        // "Logs" solo tiene sentido para baneos, así que solo se muestra en esa pestaña
        this.logsBtn = ButtonWidget.builder(Text.of("Logs"), b -> ejecutarLogs())
                .dimensions(xBotonesUsuario + (anchoBotonUsuario + ESPACIO) * 2, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.logsBtn.visible = (pestanaActual == Pestana.BANEOS);
        this.addDrawableChild(logsBtn);

        this.filasY0 = usuarioY + ALTO + ESPACIO * 3;
        this.paginacionY = filasY0 + FILAS_POR_PAGINA * (ALTO + ESPACIO) + ESPACIO;
        this.accionesY = paginacionY + ALTO + ESPACIO * 3;

        this.anteriorBtn = ButtonWidget.builder(Text.of("◀"), b -> cambiarPagina(-1))
                .dimensions(panelX, paginacionY, 40, ALTO)
                .build();
        this.siguienteBtn = ButtonWidget.builder(Text.of("▶"), b -> cambiarPagina(1))
                .dimensions(panelX + panelAncho - 40, paginacionY, 40, ALTO)
                .build();
        this.addDrawableChild(anteriorBtn);
        this.addDrawableChild(siguienteBtn);

        int anchoCopiar = panelAncho - 90 - ESPACIO;
        ButtonWidget copiarBtn = ButtonWidget.builder(Text.of("Copiar último"), b -> copiarUltimo())
                .dimensions(panelX, accionesY, anchoCopiar, ALTO)
                .build();
        this.addDrawableChild(copiarBtn);

        ButtonWidget cerrarBtn = ButtonWidget.builder(Text.of("Cerrar"), b -> this.close())
                .dimensions(panelX + panelAncho - 90, accionesY, 90, ALTO)
                .build();
        this.addDrawableChild(cerrarBtn);

        // ---------- Fila de utilidades: comandos fijos, sin usuario ----------
        this.utilidadesLabelY = accionesY + ALTO + ESPACIO * 3;
        this.utilidadesY = utilidadesLabelY + 10;

        int anchoUtil = (panelAncho - ESPACIO * 4) / 5;
        int xUtil = panelX;

        ButtonWidget vanishBtn = ButtonWidget.builder(Text.of("Vanish"), b -> enviar("vanish"))
                .dimensions(xUtil, utilidadesY, anchoUtil, ALTO)
                .build();
        this.addDrawableChild(vanishBtn);
        xUtil += anchoUtil + ESPACIO;

        ButtonWidget flyBtn = ButtonWidget.builder(Text.of("Fly"), b -> enviar("fly"))
                .dimensions(xUtil, utilidadesY, anchoUtil, ALTO)
                .build();
        this.addDrawableChild(flyBtn);
        xUtil += anchoUtil + ESPACIO;

        ButtonWidget velSueloBtn = ButtonWidget.builder(Text.of("Vel. Suelo"), b -> enviar("flyspeed walk 10"))
                .dimensions(xUtil, utilidadesY, anchoUtil, ALTO)
                .build();
        this.addDrawableChild(velSueloBtn);
        xUtil += anchoUtil + ESPACIO;

        ButtonWidget velVueloBtn = ButtonWidget.builder(Text.of("Vel. Vuelo"), b -> enviar("flyspeed fly 4"))
                .dimensions(xUtil, utilidadesY, anchoUtil, ALTO)
                .build();
        this.addDrawableChild(velVueloBtn);
        xUtil += anchoUtil + ESPACIO;

        ButtonWidget alertsBtn = ButtonWidget.builder(Text.of("Alertas"), b -> enviar("alerts"))
                .dimensions(xUtil, utilidadesY, anchoUtil, ALTO)
                .build();
        this.addDrawableChild(alertsBtn);

        this.mensajeY = utilidadesY + ALTO + 14;

        this.panelTopY = tabsY - PADDING_PANEL;
        this.panelBottomY = mensajeY + 12;

        construirFilas();
        actualizarPaginacion();
    }

    // ---------------------------------------------------------------
    // Datos según la pestaña activa
    // ---------------------------------------------------------------

    private Config.Motivo[] motivosActuales() {
        return pestanaActual == Pestana.MUTES ? Config.MOTIVOS_MUTE : Config.MOTIVOS_BAN;
    }

    private int paginaActual() {
        return pestanaActual == Pestana.MUTES ? paginaMutes : paginaBaneos;
    }

    private void setPaginaActual(int pagina) {
        if (pestanaActual == Pestana.MUTES) {
            paginaMutes = pagina;
        } else {
            paginaBaneos = pagina;
        }
    }

    private int totalPaginas() {
        int total = motivosActuales().length;
        return Math.max(1, (int) Math.ceil(total / (double) FILAS_POR_PAGINA));
    }

    // ---------------------------------------------------------------
    // Interacción
    // ---------------------------------------------------------------

    private void cambiarPestana(Pestana pestana) {
        if (this.pestanaActual == pestana) return;
        this.pestanaActual = pestana;
        this.logsBtn.visible = (pestana == Pestana.BANEOS);
        construirFilas();
        actualizarPaginacion();
    }

    private void cambiarPagina(int delta) {
        int total = totalPaginas();
        int nueva = Math.floorMod(paginaActual() + delta, total);
        setPaginaActual(nueva);
        construirFilas();
        actualizarPaginacion();
    }

    private void actualizarPaginacion() {
        boolean multiPagina = totalPaginas() > 1;
        this.anteriorBtn.visible = multiPagina;
        this.siguienteBtn.visible = multiPagina;
    }

    private void construirFilas() {
        for (ButtonWidget boton : filasBotones) {
            this.remove(boton);
        }
        filasBotones.clear();

        Config.Motivo[] motivos = motivosActuales();
        int pagina = paginaActual();
        int inicio = pagina * FILAS_POR_PAGINA;
        int fin = Math.min(inicio + FILAS_POR_PAGINA, motivos.length);

        for (int i = inicio; i < fin; i++) {
            Config.Motivo motivo = motivos[i];
            int fila = i - inicio;
            int y = filasY0 + fila * (ALTO + ESPACIO);
            for (int nivel = 1; nivel <= 3; nivel++) {
                final int nivelFinal = nivel; // copia final para poder usarla dentro de la lambda
                int x = panelX + ANCHO_ETIQUETA + ESPACIO + (nivel - 1) * (ANCHO_BOTON + ESPACIO);
                String etiqueta = "#" + nivel + " (" + motivo.tiempo(nivel) + ")";
                ButtonWidget boton = ButtonWidget.builder(Text.of(etiqueta), b -> ejecutarAccion(motivo, nivelFinal))
                        .dimensions(x, y, ANCHO_BOTON, ALTO)
                        .build();
                this.addDrawableChild(boton);
                filasBotones.add(boton);
            }
        }
    }

    private void ejecutarAccion(Config.Motivo motivo, int nivel) {
        String usuario = usuarioActual();
        if (usuario == null) return;

        String tiempo = motivo.tiempo(nivel);
        String texto = Config.textoCopiable(usuario, motivo, nivel, tiempo);

        if (pestanaActual == Pestana.MUTES) {
            ultimoTextoMute = texto;
            enviar(Config.comandoMute(usuario, motivo, nivel));
        } else {
            ultimoTextoBan = texto;
            enviar(Config.comandoBan(usuario, motivo, nivel));
        }
    }

    private void ejecutarHistorial() {
        String usuario = usuarioActual();
        if (usuario == null) return;
        enviar("hist " + usuario);
    }

    private void ejecutarSS() {
        String usuario = usuarioActual();
        if (usuario == null) return;
        enviar("ss " + usuario);
    }

    private void ejecutarLogs() {
        String usuario = usuarioActual();
        if (usuario == null) return;
        enviar("logs " + usuario);
    }

    private void copiarUltimo() {
        String texto = pestanaActual == Pestana.MUTES ? ultimoTextoMute : ultimoTextoBan;
        if (texto.isEmpty()) {
            avisar("Todavía no hay ninguna acción para copiar.");
            return;
        }
        MinecraftClient.getInstance().keyboard.setClipboard(texto);
        avisar("Copiado al portapapeles.");
    }

    private String usuarioActual() {
        String usuario = this.campoUsuario.getText().trim();
        if (usuario.isEmpty()) {
            avisar("Escribe primero el usuario.");
            return null;
        }
        this.ultimoUsuario = usuario;
        return usuario;
    }

    private void enviar(String comando) {
        if (!Config.ENVIAR_DIRECTO) {
            MinecraftClient.getInstance().setScreen(new net.minecraft.client.gui.screen.ChatScreen("/" + comando));
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(comando);
        }
    }

    private void avisar(String texto) {
        this.mensaje = Text.of(texto);
    }

    // ---------------------------------------------------------------
    // Render
    // ---------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Panel de fondo con borde, para separar visualmente del mundo detrás
        context.fill(panelX - PADDING_PANEL, panelTopY, panelX + panelAncho + PADDING_PANEL, panelBottomY, COLOR_FONDO_PANEL);
        context.fill(panelX - PADDING_PANEL, panelTopY, panelX + panelAncho + PADDING_PANEL, panelTopY + 1, COLOR_BORDE_PANEL);
        context.fill(panelX - PADDING_PANEL, panelBottomY - 1, panelX + panelAncho + PADDING_PANEL, panelBottomY, COLOR_BORDE_PANEL);

        // Franjas alternas detrás de las filas de motivos, para leerlas mejor
        Config.Motivo[] motivosFondo = motivosActuales();
        int paginaFondo = paginaActual();
        int inicioFondo = paginaFondo * FILAS_POR_PAGINA;
        int finFondo = Math.min(inicioFondo + FILAS_POR_PAGINA, motivosFondo.length);
        for (int i = inicioFondo; i < finFondo; i++) {
            int fila = i - inicioFondo;
            if (fila % 2 == 0) {
                int y = filasY0 + fila * (ALTO + ESPACIO) - 1;
                context.fill(panelX - 4, y, panelX + panelAncho + 4, y + ALTO + 2, COLOR_FILA_PAR);
            }
        }

        // Separador antes de la fila de utilidades
        context.fill(panelX, utilidadesLabelY - 2, panelX + panelAncho, utilidadesLabelY - 1, COLOR_SEPARADOR);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, tituloY - 10, COLOR_TITULO);

        int mitadTab = panelAncho / 2 - 2;
        int barraY = tabsY + ALTO + 1;
        if (pestanaActual == Pestana.MUTES) {
            context.fill(panelX, barraY, panelX + mitadTab, barraY + 2, COLOR_ACENTO);
        } else {
            context.fill(panelX + mitadTab + 4, barraY, panelX + mitadTab + 4 + mitadTab, barraY + 2, COLOR_ACENTO);
        }

        Config.Motivo[] motivos = motivosActuales();
        int pagina = paginaActual();
        int inicio = pagina * FILAS_POR_PAGINA;
        int fin = Math.min(inicio + FILAS_POR_PAGINA, motivos.length);
        for (int i = inicio; i < fin; i++) {
            int fila = i - inicio;
            int y = filasY0 + fila * (ALTO + ESPACIO) + (ALTO - 8) / 2;
            context.drawTextWithShadow(this.textRenderer, Text.of(motivos[i].nombre()), panelX, y, COLOR_TEXTO);
        }

        if (totalPaginas() > 1) {
            String texto = "Página " + (pagina + 1) + "/" + totalPaginas();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(texto), this.width / 2, paginacionY + 6, COLOR_SUAVE);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Utilidades"), this.width / 2, utilidadesLabelY - 9, COLOR_SUAVE);

        if (!this.mensaje.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.mensaje, this.width / 2, mensajeY, COLOR_AVISO);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
