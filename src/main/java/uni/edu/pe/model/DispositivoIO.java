package uni.edu.pe.model;

/**
 * Representa los dispositivos de Entrada / Salida exigidos por el proyecto (mínimo 3 a 5).
 */
public enum DispositivoIO { // Un enum (Enumeración) es un tipo de dato especial que sirve para definir un conjunto
    // cerrado, fijo y limitado de opciones que nunca van a cambiar mientras el programa corre.
    TECLADO("Teclado"),
    DISCO("Disco"),
    IMPRESORA("Impresora"),
    PANTALLA("Pantalla");

    private final String nombre;  // final significa que es inmutable: una vez que se le asigna valor al
    // iniciar el programa, nadie lo puede cambiar jamás

    DispositivoIO(String nombre) {
        this.nombre = nombre;
    } // Constructor

    public String getNombre() {
        return nombre;
    } // Método público para obtener el nombre del dispositivo

    @Override
    public String toString() {
        return nombre;
    }

    // En Java, todas las clases y enums heredan automáticamente de una clase padre universal llamada Object
    // Esa clase Object ya viene de fábrica con varios métodos estándar creados por los ingenieros de Java.
    // Uno de ellos es el toString()
    // Por defecto, si no haces nada e intentas imprimir un objeto en pantalla:
    // System.out.println(DispositivoIO.TECLADO);
    // El toString() original de Java imprime algo técnico y feo, como la posición de memoria:
    // uni.edu.pe.model.DispositivoIO@7229724f
    // @Override: "No uses la versión fea por defecto de toString. A partir de ahora, sobreescribe esa función
    // con mi propia lógica: cada vez que alguien intente convertir este dispositivo a texto, entrégale
    // su variable nombre"
}

