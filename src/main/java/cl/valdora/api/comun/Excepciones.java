package cl.valdora.api.comun;

/** Excepciones de negocio que el manejador de errores traduce a respuestas RFC 9457. */
public final class Excepciones {

    private Excepciones() {
    }

    /** 404: el recurso pedido no existe. */
    public static class RecursoNoEncontrado extends RuntimeException {
        public RecursoNoEncontrado(String mensaje) {
            super(mensaje);
        }
    }

    /** 409: la operación choca con el estado actual (duplicados, transiciones no permitidas, etc.). */
    public static class Conflicto extends RuntimeException {
        public Conflicto(String mensaje) {
            super(mensaje);
        }
    }
}
