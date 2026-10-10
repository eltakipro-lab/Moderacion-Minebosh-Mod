package com.minebosh.moderacion;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class Config {

    /**
     * Un motivo con sus tres niveles de tiempo (#1, #2 y #3).
     * El tiempo se escribe tal cual lo entiende tu plugin: "2h", "1d", "35d", etc.
     */
    public record Motivo(String nombre, String t1, String t2, String t3) {
        public String tiempo(int nivel) {
            return switch (nivel) {
                case 1 -> t1;
                case 2 -> t2;
                default -> t3;
            };
        }
    }

    // ------------------------------------------------------------------
    // Ajustes generales
    // ------------------------------------------------------------------

    /** Si es true, añade "#1"/"#2"/"#3" al final del motivo enviado al comando. */
    public static final boolean INCLUIR_NIVEL_EN_MOTIVO = true;

    /** Si es true, cambia los espacios del motivo por "_". Se deja en false para usar espacios normales. */
    public static final boolean ESPACIOS_A_GUION_BAJO = false;

    /** Si es true, el comando se envía directo al servidor al pulsar el botón. */
    public static final boolean ENVIAR_DIRECTO = true;

    // ------------------------------------------------------------------
    // CANDADO DE LICENCIA (anti-robo por AnyDesk / copia del .jar)
    // ------------------------------------------------------------------
    // Cómo generar TU hash:
    //  1) Compila y ejecuta la clase LicenciaUtilidad.java (va aparte) en TU PC,
    //     el que quieres autorizar.
    //  2) Te imprimirá un hash de 64 caracteres.
    //  3) Pega ese hash abajo, en LICENCIA_HASH_AUTORIZADO, sustituyendo el texto de ejemplo.
    //  4) Recompila el mod con tu Gradle/Loom de siempre y listo: solo funcionará en ese PC.
    //
    // Si mañana cambias de PC, repite el proceso y vuelve a compilar.

    private static final String SAL = "minebosh-moderacion-2026";

    /** PEGA AQUÍ el hash que te imprima LicenciaUtilidad en tu PC. */
    private static final String LICENCIA_HASH_AUTORIZADO = "PEGA_AQUI_TU_HASH_DE_64_CARACTERES";

    private static Boolean licenciaCacheada = null;

    private static boolean licenciaValida() {
        if (licenciaCacheada != null) {
            return licenciaCacheada;
        }
        try {
            String guid = leerMachineGuid();
            String hash = calcularHash(guid);
            licenciaCacheada = hash.equalsIgnoreCase(LICENCIA_HASH_AUTORIZADO);
        } catch (Exception e) {
            licenciaCacheada = false;
        }
        return licenciaCacheada;
    }

    private static String leerMachineGuid() throws Exception {
        Process proceso = new ProcessBuilder(
                "reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Cryptography", "/v", "MachineGuid")
                .redirectErrorStream(true)
                .start();
        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(proceso.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.contains("MachineGuid")) {
                    String[] partes = linea.trim().split("\\s+");
                    return partes[partes.length - 1];
                }
            }
        }
        throw new RuntimeException("No se pudo leer el identificador de esta máquina.");
    }

    static String calcularHash(String entrada) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest((SAL + entrada).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Motivos de MUTE (pestaña "Mutes")
    // ------------------------------------------------------------------
    public static final Motivo[] MOTIVOS_MUTE = new Motivo[] {
            new Motivo("Lenguaje Ofensivo", "2h", "6h", "1d"),
            new Motivo("Flood", "30m", "1h", "2h"),
            new Motivo("Insultos Al Servidor", "8h", "16h", "1d"),
            new Motivo("Falta de Respeto al Staff", "4h", "12h", "1d"),
            new Motivo("Nombrar servidores Externos", "30m", "1h", "2h"),
            new Motivo("Tradeos Externos", "2h", "6h", "15h"),
    };

    // ------------------------------------------------------------------
    // Motivos de BANEO (pestaña "Baneos")
    // ------------------------------------------------------------------
    public static final Motivo[] MOTIVOS_BAN = new Motivo[] {
            new Motivo("Evadir SS", "35d", "50d", "60d"),
            new Motivo("Hacks en SS", "35d", "50d", "60d"),
            new Motivo("Hacks Claros", "30d", "40d", "60d"),
            new Motivo("Estafa de Rankeo", "30d", "60d", "90d"),
            new Motivo("Modificaciones de Archivos", "30d", "40d", "60d"),
            new Motivo("Ventas Ilegales", "30d", "60d", "90d"),
            new Motivo("Amenaza de Robo de Cuentas", "30d", "60d", "120d"),
            new Motivo("Amenaza de Doxeo", "30d", "60d", "120d"),
            new Motivo("Hacks Admitidos", "20d", "30d", "40d"),
            new Motivo("Robo en Trade", "20d", "30d", "40d"),
            new Motivo("Complice de Hacker", "15d", "20d", "30d"),
            new Motivo("Estafa", "15d", "30d", "40d"),
            new Motivo("AutoClick en SS", "15d", "20d", "30d"),
            new Motivo("AutoClick Admitido", "10d", "15d", "20d"),
            new Motivo("Bug Abuse", "10d", "15d", "20d"),
            new Motivo("Acoso a Jugadores", "7d", "14d", "20d"),
            new Motivo("FarmKills", "7d", "20d", "30d"),
            new Motivo("TpaKill", "7d", "14d", "20d"),
            new Motivo("Evadir Mute", "3d", "10d", "20d"),
            new Motivo("Suplantar a un Staff", "3d", "5d", "10d"),
            new Motivo("Modificaciones Ilegales", "3d", "5d", "12d"),
            new Motivo("Uso de Multicuenta en Gens", "1d", "3d", "7d"),
            new Motivo("3vs1", "1h", "3h", "1d"),
            new Motivo("Uso de Multicuentas en Keyall", "1h", "3h", "1d"),
    };

    private static String formatearMotivo(Motivo motivo, int nivel) {
        String nombre = ESPACIOS_A_GUION_BAJO ? motivo.nombre().replace(' ', '_') : motivo.nombre();
        return INCLUIR_NIVEL_EN_MOTIVO ? nombre + " #" + nivel : nombre;
    }

    public static String comandoMute(String usuario, Motivo motivo, int nivel) {
        if (!licenciaValida()) {
            return "say Panel no autorizado en este equipo";
        }
        String razon = formatearMotivo(motivo, nivel);
        String tiempo = motivo.tiempo(nivel);
        return "/ipmute " + usuario + " " + razon + " " + tiempo;
    }

    public static String comandoBan(String usuario, Motivo motivo, int nivel) {
        if (!licenciaValida()) {
            return "say Panel no autorizado en este equipo";
        }
        String razon = formatearMotivo(motivo, nivel);
        String tiempo = motivo.tiempo(nivel);
        return "/ipban " + usuario + " " + razon + " " + tiempo;
    }

    /** Texto de 3 líneas para el botón "Copiar último": Nick / Razón / Tiempo. */
    public static String textoCopiable(String usuario, Motivo motivo, int nivel, String tiempo) {
        return "Nick: " + usuario
                + "\nRazón: " + motivo.nombre() + " #" + nivel
                + "\nTiempo: " + tiempo;
    }
}
