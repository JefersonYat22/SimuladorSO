package uni.edu.pe.model;

/**
 * Representa los dispositivos de Entrada / Salida exigidos por el proyecto (mínimo 5 dispositivos según Diapositiva 4).
 */
public enum DispositivoIO { // Un enum (Enumeración) es un tipo de dato especial que sirve para definir un conjunto
    // cerrado, fijo y limitado de opciones que nunca van a cambiar mientras el programa corre.
    TECLADO("Teclado"),
    DISCO("Disco"),
    IMPRESORA("Impresora"),
    PANTALLA("Pantalla"),
    RED("Tarjeta de Red");

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

