package com.minebosh.moderacion;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ModeracionScreen extends Screen {

    // ---------- Layout ----------
    private static final int ANCHO_ETIQUETA = 190;
    private static final int ANCHO_BOTON = 64;
    private static final int ALTO = 20;
    private static final int ESPACIO = 4;
    private static final int PADDING_PANEL = 12;
    private static final int MARGEN_PANTALLA = 20;
    private static final int FILAS_MIN = 4;
    private static final int FILAS_MAX = 25;

    // ---------- Mini chat de registro (cajita en la esquina) ----------
    private static final int REGISTRO_MARGEN = 40;
    private static final int REGISTRO_ANCHO = 220;
    private static final int REGISTRO_ALTO = 220;
    private static final int REGISTRO_TITULO_ALTO = 16;

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
    private static final int COLOR_REGISTRO_ACCION = 0xFF7CFF9E;
    private static final int COLOR_REGISTRO_CHAT = 0xFFA0C8FF;
    private static final int COLOR_REGISTRO_HORA = 0xFF808080;

    // ---------- Guardado en disco ----------
    private static final Path ARCHIVO_ESTADO = FabricLoader.getInstance().getConfigDir()
            .resolve("moderacion-minebosh-tabs.properties");
    private static final Path ARCHIVO_HISTORIAL = FabricLoader.getInstance().getConfigDir()
            .resolve("moderacion-minebosh-tabs-historial.log");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private enum Pestana { MUTES, BANEOS, SS }

    // ---------- Mensajes predefinidos de la pestaña SS ----------
    private record MensajeSS(String etiqueta, String texto) {}

    private static final MensajeSS[] MENSAJES_SS = new MensajeSS[] {
            new MensajeSS("¿Admites uso de Hacks o prefiere SS? (60s) Si admite su baneo sera mucho menor.", "¿Admites uso de Hacks o prefiere SS? (60s) Si admite su baneo sera mucho menor."),
            new MensajeSS("30s", "30s"),
            new MensajeSS("10s", "10s"),
            new MensajeSS("9s", "9s"),
            new MensajeSS("8s", "8s"),
            new MensajeSS("7s", "7s"),
            new MensajeSS("6s", "6s"),
            new MensajeSS("5s", "5s"),
            new MensajeSS("4s", "4s"),
            new MensajeSS("3s", "3s"),
            new MensajeSS("2s", "2s"),
            new MensajeSS("1s", "1s"),
            new MensajeSS("0s", "0s"),
            new MensajeSS("Tienes 5 minutos para pasarme tu codigo de AnyDesk.com", "Tienes 5 minutos para pasarme tu codigo de AnyDesk.com"),
            new MensajeSS("Tiempo terminado", "Tu tiempo se termino."),
    };

    // ---------------------------------------------------------------
    // Registro en vivo (chat del servidor + acciones del panel).
    // Estático porque debe sobrevivir a que el usuario cierre y abra
    // la pantalla varias veces, y el listener de chat solo puede
    // registrarse una vez con el sistema de eventos de Fabric.
    // ---------------------------------------------------------------
    private static final class RegistroModeracion {
        private enum Tipo { CHAT, ACCION }
        private record Entrada(String hora, Tipo tipo, String texto) {}

        private static final int MAX_ENTRADAS = 400;
        // Tras pulsar "Historial" o "Logs" se capturan los mensajes de chat que
        // lleguen durante esta ventana de tiempo (la respuesta del servidor a
        // /hist o /logs); fuera de esa ventana no se guarda el chat normal.
        private static final long VENTANA_CAPTURA_MS = 8000L;
        private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
        private static final List<Entrada> ENTRADAS = Collections.synchronizedList(new ArrayList<>());
        private static boolean registrado = false;
        private static volatile long capturaHastaMs = 0L;

        static void asegurarRegistro() {
            if (registrado) return;
            registrado = true;
            // Requiere el módulo fabric-message-api-v1 de Fabric API como dependencia del mod.
            ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
                if (overlay) return;
                if (System.currentTimeMillis() > capturaHastaMs) return; // solo interesa /hist y /logs
                String texto = message.getString();
                if (texto == null || texto.isBlank()) return;
                agregar(Tipo.CHAT, texto);
            });
        }

        /** Abre una ventana de captura de chat: se llama justo al enviar /hist o /logs. */
        static void iniciarCapturaChat() {
            capturaHastaMs = System.currentTimeMillis() + VENTANA_CAPTURA_MS;
        }

        static void registrarAccion(String texto) {
            agregar(Tipo.ACCION, texto);
        }

        private static void agregar(Tipo tipo, String texto) {
            Entrada entrada = new Entrada(LocalTime.now().format(FORMATO_HORA), tipo, texto);
            synchronized (ENTRADAS) {
                ENTRADAS.add(entrada);
                while (ENTRADAS.size() > MAX_ENTRADAS) {
                    ENTRADAS.remove(0);
                }
            }
        }

        static List<Entrada> copia() {
            synchronized (ENTRADAS) {
                return new ArrayList<>(ENTRADAS);
            }
        }

        static void limpiar() {
            synchronized (ENTRADAS) {
                ENTRADAS.clear();
            }
        }
    }

    private Pestana pestanaActual = Pestana.MUTES;
    private int paginaMutes = 0;
    private int paginaBaneos = 0;
    private int paginaSS = 0;
    private int filasPorPagina = 6;

    private TextFieldWidget campoUsuario;
    private String ultimoUsuario = "";

    private String ultimoTextoMute = "";
    private String ultimoTextoBan = "";
    private Text mensaje = Text.empty();

    private ButtonWidget tabMutesBtn;
    private ButtonWidget tabBaneosBtn;
    private ButtonWidget tabSSBtn;
    private ButtonWidget anteriorBtn;
    private ButtonWidget siguienteBtn;
    private ButtonWidget logsBtn;
    private ButtonWidget copiarBtn;
    private ButtonWidget limpiarRegistroBtn;

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

    // ---------- Panel de registro (mini chat con logs + historial) ----------
    private boolean mostrarRegistro;
    private int registroX;
    private int registroAncho;
    private int registroCajaTopY;
    private int registroCajaBottomY;
    private int registroContenidoY0;
    private int registroContenidoY1;
    private int registroScroll = 0;
    private boolean registroAutoScroll = true;

    private final List<OrderedText> registroLineasCache = new ArrayList<>();
    private final List<Integer> registroColoresCache = new ArrayList<>();
    private int registroCacheTamano = -1;
    private int registroCacheAncho = -1;

    public ModeracionScreen() {
        super(Text.of("Moderación Minebosh"));
        cargarEstado();
        RegistroModeracion.asegurarRegistro();
    }

    @Override
    protected void init() {
        this.panelAncho = ANCHO_ETIQUETA + (ANCHO_BOTON * 3) + (ESPACIO * 4);

        // El panel principal se ancla arriba a la izquierda (no centrado).
        this.panelX = MARGEN_PANTALLA;
        this.panelTopY = MARGEN_PANTALLA;
        this.panelBottomY = this.height - MARGEN_PANTALLA;

        // El mini chat de registro es una cajita fija anclada en la esquina
        // inferior derecha de la pantalla, por encima de todo lo demás.
        this.registroAncho = REGISTRO_ANCHO;
        this.registroX = this.width - REGISTRO_MARGEN - REGISTRO_ANCHO;
        this.registroCajaBottomY = this.height - REGISTRO_MARGEN;
        this.registroCajaTopY = this.registroCajaBottomY - REGISTRO_ALTO;

        // Solo se muestra si cabe entera en pantalla Y si queda un hueco real
        // (sin tocar) entre el borde derecho del menú principal y el mini chat,
        // para que nunca se choquen ni se superpongan.
        int bordeDerechoPanel = panelX + panelAncho + PADDING_PANEL;
        int huecoEntrePaneles = (registroX - 8) - bordeDerechoPanel;
        this.mostrarRegistro = this.width >= REGISTRO_ANCHO + REGISTRO_MARGEN * 2 + 40
                && this.height >= REGISTRO_ALTO + REGISTRO_MARGEN * 2
                && huecoEntrePaneles >= 24;

        this.tabsY = panelTopY + PADDING_PANEL;
        this.tituloY = tabsY - 20;

        int tercioTab = (panelAncho - ESPACIO * 2) / 3;
        int ultimoTabAncho = panelAncho - tercioTab * 2 - ESPACIO * 2;
        this.tabMutesBtn = ButtonWidget.builder(Text.of("Mutes"), b -> cambiarPestana(Pestana.MUTES))
                .dimensions(panelX, tabsY, tercioTab, ALTO)
                .build();
        this.tabBaneosBtn = ButtonWidget.builder(Text.of("Baneos"), b -> cambiarPestana(Pestana.BANEOS))
                .dimensions(panelX + tercioTab + ESPACIO, tabsY, tercioTab, ALTO)
                .build();
        this.tabSSBtn = ButtonWidget.builder(Text.of("SS"), b -> cambiarPestana(Pestana.SS))
                .dimensions(panelX + (tercioTab + ESPACIO) * 2, tabsY, ultimoTabAncho, ALTO)
                .build();
        this.addDrawableChild(tabMutesBtn);
        this.addDrawableChild(tabBaneosBtn);
        this.addDrawableChild(tabSSBtn);

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
        // Guarda lo escrito al instante, letra a letra, hasta que el usuario lo
        // vuelva a cambiar (no hace falta pulsar ningún botón para que persista).
        this.campoUsuario.setChangedListener(texto -> {
            this.ultimoUsuario = texto;
            // Quita la sugerencia "Nombre del usuario" en cuanto se escribe algo,
            // y la vuelve a mostrar si el campo se deja vacío otra vez.
            this.campoUsuario.setSuggestion(texto.isEmpty() ? "Nombre del usuario" : "");
            guardarEstado();
        });
        this.addDrawableChild(campoUsuario);

        int xBotonesUsuario = panelX + panelAncho - anchoBotonesUsuario;

        ButtonWidget historialBtn = ButtonWidget.builder(Text.of("Historial"), b -> ejecutarHistorial())
                .dimensions(xBotonesUsuario, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.addDrawableChild(historialBtn);

        ButtonWidget ssComandoBtn = ButtonWidget.builder(Text.of("SS"), b -> ejecutarSS())
                .dimensions(xBotonesUsuario + anchoBotonUsuario + ESPACIO, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.addDrawableChild(ssComandoBtn);

        // "Logs" solo tiene sentido para baneos, así que solo se muestra en esa pestaña
        this.logsBtn = ButtonWidget.builder(Text.of("Logs"), b -> ejecutarLogs())
                .dimensions(xBotonesUsuario + (anchoBotonUsuario + ESPACIO) * 2, usuarioY, anchoBotonUsuario, ALTO)
                .build();
        this.logsBtn.visible = (pestanaActual == Pestana.BANEOS);
        this.addDrawableChild(logsBtn);

        this.filasY0 = usuarioY + ALTO + ESPACIO * 3;

        // Reserva fija de la parte inferior (paginación, acciones, utilidades y aviso)
        // para calcular cuántas filas de motivos caben usando TODO el alto disponible.
        int reservaInferior = (ALTO + ESPACIO * 3)   // hasta accionesY
                + (ALTO + ESPACIO * 3)               // hasta utilidadesLabelY
                + 10                                  // hasta utilidadesY
                + (ALTO + 14)                          // hasta mensajeY
                + 12;                                  // hasta panelBottomY
        int espacioParaFilas = panelBottomY - filasY0 - ESPACIO - reservaInferior;
        int filasCalculadas = espacioParaFilas / (ALTO + ESPACIO);
        this.filasPorPagina = Math.max(FILAS_MIN, Math.min(FILAS_MAX, filasCalculadas));

        this.paginacionY = filasY0 + filasPorPagina * (ALTO + ESPACIO) + ESPACIO;
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
        this.copiarBtn = ButtonWidget.builder(Text.of("Copiar último"), b -> copiarUltimo())
                .dimensions(panelX, accionesY, anchoCopiar, ALTO)
                .build();
        this.copiarBtn.visible = (pestanaActual != Pestana.SS);
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

        // ---------- Mini chat de registro: cajita en la esquina inferior derecha ----------
        if (this.mostrarRegistro) {
            int anchoLimpiar = 50;
            this.limpiarRegistroBtn = ButtonWidget.builder(Text.of("Limpiar"), b -> {
                        RegistroModeracion.limpiar();
                        registroCacheTamano = -1; // fuerza reconstrucción del caché
                        registroScroll = 0;
                        registroAutoScroll = true;
                    })
                    .dimensions(registroX + registroAncho - anchoLimpiar - 4, registroCajaTopY + 2, anchoLimpiar, 12)
                    .build();
            this.addDrawableChild(limpiarRegistroBtn);

            this.registroContenidoY0 = registroCajaTopY + REGISTRO_TITULO_ALTO;
            this.registroContenidoY1 = registroCajaBottomY - 6;
            registroCacheTamano = -1; // recalcular al (re)abrir la pantalla
        }

        construirFilas();
        actualizarPaginacion();
    }

    // ---------------------------------------------------------------
    // Datos según la pestaña activa
    // ---------------------------------------------------------------

    private Config.Motivo[] motivosActuales() {
        return pestanaActual == Pestana.MUTES ? Config.MOTIVOS_MUTE : Config.MOTIVOS_BAN;
    }

    private int cantidadItemsActual() {
        return pestanaActual == Pestana.SS ? MENSAJES_SS.length : motivosActuales().length;
    }

    private int paginaActual() {
        return switch (pestanaActual) {
            case MUTES -> paginaMutes;
            case BANEOS -> paginaBaneos;
            case SS -> paginaSS;
        };
    }

    private void setPaginaActual(int pagina) {
        switch (pestanaActual) {
            case MUTES -> paginaMutes = pagina;
            case BANEOS -> paginaBaneos = pagina;
            case SS -> paginaSS = pagina;
        }
    }

    private int totalPaginas() {
        int total = cantidadItemsActual();
        return Math.max(1, (int) Math.ceil(total / (double) filasPorPagina));
    }

    // ---------------------------------------------------------------
    // Interacción
    // ---------------------------------------------------------------

    private void cambiarPestana(Pestana pestana) {
        if (this.pestanaActual == pestana) return;
        this.pestanaActual = pestana;
        this.logsBtn.visible = (pestana == Pestana.BANEOS);
        this.copiarBtn.visible = (pestana != Pestana.SS);
        guardarEstado();
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

        int pagina = paginaActual();
        int inicio = pagina * filasPorPagina;

        if (pestanaActual == Pestana.SS) {
            int fin = Math.min(inicio + filasPorPagina, MENSAJES_SS.length);
            for (int i = inicio; i < fin; i++) {
                MensajeSS item = MENSAJES_SS[i];
                int fila = i - inicio;
                int y = filasY0 + fila * (ALTO + ESPACIO);
                ButtonWidget boton = ButtonWidget.builder(Text.of(item.etiqueta()), b -> ejecutarMensajeSS(item.texto()))
                        .dimensions(panelX, y, panelAncho, ALTO)
                        .build();
                this.addDrawableChild(boton);
                filasBotones.add(boton);
            }
            return;
        }

        Config.Motivo[] motivos = motivosActuales();
        int fin = Math.min(inicio + filasPorPagina, motivos.length);

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

    private void ejecutarMensajeSS(String texto) {
        enviarMensaje(texto);
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
        guardarEstado();
        return usuario;
    }

    private void enviar(String comando) {
        if (comando.startsWith("hist ") || comando.startsWith("logs ")) {
            RegistroModeracion.iniciarCapturaChat();
        }
        registrarHistorial("Comando", "/" + comando);
        if (!Config.ENVIAR_DIRECTO) {
            MinecraftClient.getInstance().setScreen(new net.minecraft.client.gui.screen.ChatScreen("/" + comando));
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatCommand(comando);
        }
    }

    private void enviarMensaje(String texto) {
        registrarHistorial("Mensaje SS", texto);
        if (!Config.ENVIAR_DIRECTO) {
            MinecraftClient.getInstance().setScreen(new net.minecraft.client.gui.screen.ChatScreen(texto));
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.networkHandler != null) {
            client.player.networkHandler.sendChatMessage(texto);
        }
    }

    private void avisar(String texto) {
        this.mensaje = Text.of(texto);
    }

    // ---------------------------------------------------------------
    // Guardado en disco: recuerda el usuario y la pestaña entre sesiones,
    // y deja un registro de cada acción realizada en el panel.
    // ---------------------------------------------------------------

    private void cargarEstado() {
        Properties props = new Properties();
        if (Files.exists(ARCHIVO_ESTADO)) {
            try (InputStream in = Files.newInputStream(ARCHIVO_ESTADO)) {
                props.load(in);
            } catch (IOException ignored) {
                // si el archivo está dañado, simplemente empieza de cero
            }
        }
        this.ultimoUsuario = props.getProperty("ultimoUsuario", "");
        try {
            this.pestanaActual = Pestana.valueOf(props.getProperty("ultimaPestana", "MUTES"));
        } catch (IllegalArgumentException ignored) {
            this.pestanaActual = Pestana.MUTES;
        }
    }

    private void guardarEstado() {
        Properties props = new Properties();
        props.setProperty("ultimoUsuario", this.ultimoUsuario);
        props.setProperty("ultimaPestana", this.pestanaActual.name());
        try {
            Files.createDirectories(ARCHIVO_ESTADO.getParent());
            try (OutputStream out = Files.newOutputStream(ARCHIVO_ESTADO)) {
                props.store(out, "Estado del panel de moderación Minebosh");
            }
        } catch (IOException ignored) {
            // si falla el guardado no debe romper el juego
        }
    }

    private void registrarHistorial(String tipo, String contenido) {
        String linea = "[" + LocalDateTime.now().format(FORMATO_FECHA) + "] " + tipo + ": " + contenido;
        try {
            Files.createDirectories(ARCHIVO_HISTORIAL.getParent());
            Files.writeString(ARCHIVO_HISTORIAL, linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // si falla el guardado no debe romper el juego
        }
        // También va al mini registro en vivo dentro del panel (pestaña de registro).
        RegistroModeracion.registrarAccion(tipo + ": " + contenido);
    }

    // ---------------------------------------------------------------
    // Panel de registro: reconstruye el caché de líneas ajustadas al
    // ancho disponible solo cuando cambia el contenido o el tamaño.
    // ---------------------------------------------------------------

    private void actualizarCacheRegistro() {
        List<RegistroModeracion.Entrada> entradas = RegistroModeracion.copia();
        int anchoContenido = registroAncho - 4;
        if (entradas.size() == registroCacheTamano && anchoContenido == registroCacheAncho) {
            return;
        }
        registroCacheTamano = entradas.size();
        registroCacheAncho = anchoContenido;
        registroLineasCache.clear();
        registroColoresCache.clear();

        for (RegistroModeracion.Entrada entrada : entradas) {
            int color = entrada.tipo() == RegistroModeracion.Tipo.CHAT ? COLOR_REGISTRO_CHAT : COLOR_REGISTRO_ACCION;
            String textoCompleto = "[" + entrada.hora() + "] " + entrada.texto();
            List<OrderedText> lineas = this.textRenderer.wrapLines(Text.of(textoCompleto), anchoContenido);
            for (OrderedText linea : lineas) {
                registroLineasCache.add(linea);
                registroColoresCache.add(color);
            }
        }

        if (registroAutoScroll) {
            registroScroll = calcularScrollMaximo();
        } else {
            registroScroll = Math.min(registroScroll, calcularScrollMaximo());
        }
    }

    private int calcularScrollMaximo() {
        int alturaTotal = registroLineasCache.size() * 10;
        int alturaVisible = Math.max(0, registroContenidoY1 - registroContenidoY0);
        return Math.max(0, alturaTotal - alturaVisible);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mostrarRegistro && mouseX >= registroX && mouseX <= registroX + registroAncho
                && mouseY >= registroContenidoY0 && mouseY <= registroContenidoY1) {
            int scrollMax = calcularScrollMaximo();
            registroScroll = (int) Math.max(0, Math.min(scrollMax, registroScroll - amount * 12));
            registroAutoScroll = (registroScroll >= scrollMax);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
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

        // Franjas alternas detrás de las filas, para leerlas mejor
        int cantidadFondo = cantidadItemsActual();
        int paginaFondo = paginaActual();
        int inicioFondo = paginaFondo * filasPorPagina;
        int finFondo = Math.min(inicioFondo + filasPorPagina, cantidadFondo);
        for (int i = inicioFondo; i < finFondo; i++) {
            int fila = i - inicioFondo;
            if (fila % 2 == 0) {
                int y = filasY0 + fila * (ALTO + ESPACIO) - 1;
                context.fill(panelX - 4, y, panelX + panelAncho + 4, y + ALTO + 2, COLOR_FILA_PAR);
            }
        }

        // Separador antes de la fila de utilidades
        context.fill(panelX, utilidadesLabelY - 2, panelX + panelAncho, utilidadesLabelY - 1, COLOR_SEPARADOR);

        // Mini chat de registro: cajita en la esquina, por encima de todo lo demás
        if (mostrarRegistro) {
            context.fill(registroX - 8, registroCajaTopY, registroX + registroAncho + 8, registroCajaBottomY, COLOR_FONDO_PANEL);
            context.fill(registroX - 8, registroCajaTopY, registroX + registroAncho + 8, registroCajaTopY + 1, COLOR_BORDE_PANEL);
            context.fill(registroX - 8, registroCajaBottomY - 1, registroX + registroAncho + 8, registroCajaBottomY, COLOR_BORDE_PANEL);
            context.fill(registroX - 8, registroCajaTopY, registroX + registroAncho + 8, registroCajaTopY + REGISTRO_TITULO_ALTO - 2, 0x22FFFFFF);
        }

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, panelX + panelAncho / 2, tituloY - 10, COLOR_TITULO);

        int tercioTab = (panelAncho - ESPACIO * 2) / 3;
        int barraY = tabsY + ALTO + 1;
        int barraX;
        int barraAncho;
        switch (pestanaActual) {
            case MUTES -> { barraX = panelX; barraAncho = tercioTab; }
            case BANEOS -> { barraX = panelX + tercioTab + ESPACIO; barraAncho = tercioTab; }
            default -> { barraX = panelX + (tercioTab + ESPACIO) * 2; barraAncho = panelAncho - (tercioTab + ESPACIO) * 2; }
        }
        context.fill(barraX, barraY, barraX + barraAncho, barraY + 2, COLOR_ACENTO);

        if (pestanaActual == Pestana.SS) {
            context.drawTextWithShadow(this.textRenderer, Text.of("Mensajes SS"), panelX, filasY0 - 11, COLOR_SUAVE);
        } else {
            Config.Motivo[] motivos = motivosActuales();
            int pagina = paginaActual();
            int inicio = pagina * filasPorPagina;
            int fin = Math.min(inicio + filasPorPagina, motivos.length);
            for (int i = inicio; i < fin; i++) {
                int fila = i - inicio;
                int y = filasY0 + fila * (ALTO + ESPACIO) + (ALTO - 8) / 2;
                context.drawTextWithShadow(this.textRenderer, Text.of(motivos[i].nombre()), panelX, y, COLOR_TEXTO);
            }
        }

        if (totalPaginas() > 1) {
            String texto = "Página " + (paginaActual() + 1) + "/" + totalPaginas();
            context.drawCenteredTextWithShadow(this.textRenderer, Text.of(texto), panelX + panelAncho / 2, paginacionY + 6, COLOR_SUAVE);
        }

        context.drawCenteredTextWithShadow(this.textRenderer, Text.of("Utilidades"), panelX + panelAncho / 2, utilidadesLabelY - 9, COLOR_SUAVE);

        if (!this.mensaje.getString().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.mensaje, panelX + panelAncho / 2, mensajeY, COLOR_AVISO);
        }

        if (mostrarRegistro) {
            renderizarRegistro(context);
        }
    }

    private void renderizarRegistro(DrawContext context) {
        int tituloY = registroCajaTopY + 4;
        context.drawTextWithShadow(this.textRenderer, Text.of("Registro"), registroX, tituloY, COLOR_ACENTO);

        actualizarCacheRegistro();

        if (registroLineasCache.isEmpty()) {
            context.drawTextWithShadow(this.textRenderer, Text.of("Sin actividad todavía."), registroX, registroContenidoY0 + 2, COLOR_SUAVE);
            return;
        }

        context.enableScissor(registroX - 8, registroContenidoY0, registroX + registroAncho + 8, registroContenidoY1);

        int alturaTotal = registroLineasCache.size() * 10;
        int alturaVisible = registroContenidoY1 - registroContenidoY0;
        int y = registroContenidoY0 - registroScroll + Math.max(0, alturaVisible - alturaTotal);

        for (int i = 0; i < registroLineasCache.size(); i++) {
            if (y >= registroContenidoY0 - 10 && y <= registroContenidoY1) {
                context.drawTextWithShadow(this.textRenderer, registroLineasCache.get(i), registroX, y, registroColoresCache.get(i));
            }
            y += 10;
        }

        context.disableScissor();

        if (!registroAutoScroll) {
            context.drawTextWithShadow(this.textRenderer, Text.of("▼ hay mensajes nuevos, baja con la rueda"), registroX, registroContenidoY1 - 9, COLOR_REGISTRO_HORA);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
