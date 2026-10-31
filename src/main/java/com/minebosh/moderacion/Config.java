package TU.PAQUETE;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Config {
    private Config() {}

    public enum Nivel { NINGUNO, HELPER, ADMIN }

    // Deja aquí el hash de admin que ya tenías en tu Config.java anterior
    private static final String HASH_ADMIN = "PEGA_AQUI_TU_HASH_ADMIN";
    // Hash SHA-256 de la clave helper
    private static final String HASH_HELPER = "ff7936e807d742a93d3a26197ae07133716e5bd277a05dec484bd8db4b53696b";

    public static Nivel nivelPara(String clave) {
        String h = sha256(clave == null ? "" : clave.trim());
        if (h.equals(HASH_ADMIN)) return Nivel.ADMIN;
        if (h.equals(HASH_HELPER)) return Nivel.HELPER;
        return Nivel.NINGUNO;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(s.getBytes(StandardCharsets.UTF_8))) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
