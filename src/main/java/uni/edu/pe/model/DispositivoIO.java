package uni.edu.pe.model;

/**
 * Representa los dispositivos de Entrada / Salida exigidos por el proyecto (mínimo 3 a 5).
 */
public enum DispositivoIO {
    TECLADO("Teclado"),
    DISCO("Disco"),
    IMPRESORA("Impresora"),
    PANTALLA("Pantalla");

    private final String nombre;

    DispositivoIO(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return nombre;
    }
}

