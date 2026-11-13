package cl.valdora.api.comun;

/** Utilidades para el RUT chileno: normalización, dígito verificador y formato. */
public final class Rut {

    private Rut() {
    }

    /** Devuelve el formato {@code 12345678-K}, o {@code null} si el valor no tiene forma de RUT. */
    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (!limpio.matches("\\d{7,8}[0-9K]")) {
            return null;
        }
        int corte = limpio.length() - 1;
        return limpio.substring(0, corte) + "-" + limpio.charAt(corte);
    }

    public static boolean esValido(String valor) {
        String normalizado = normalizar(valor);
        if (normalizado == null) {
            return false;
        }
        int cuerpo = Integer.parseInt(normalizado.substring(0, normalizado.indexOf('-')));
        return calcularDv(cuerpo) == normalizado.charAt(normalizado.length() - 1);
    }

    public static char calcularDv(int cuerpo) {
        int suma = 0;
        int factor = 2;
        int resto = cuerpo;
        while (resto > 0) {
            suma += (resto % 10) * factor;
            resto /= 10;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int dv = 11 - (suma % 11);
        if (dv == 11) {
            return '0';
        }
        return dv == 10 ? 'K' : (char) ('0' + dv);
    }
}
