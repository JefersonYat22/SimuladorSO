package uni.edu.pe.model;

/**
 * Representa una página lógica de un proceso según los requerimientos:
 * - El tamaño del bloque/marco de memoria es múltiplo de 2^n: 4 KB.
 * - Registra la fragmentación interna (desperdicio de memoria: 4 KB - tamanoRealKb).
 * - Mantiene los bits de control para memoria virtual y algoritmos de reemplazo:
 *   bitValidez (v/i) y bitReferencia (R para el algoritmo del Reloj).
 */
public class Pagina {

    public static final int TAMANO_MARCO_KB = 4;

    private final int numeroPagina;
    private final int tamanoRealKb;      // Tamaño ocupado real (ej. si quedan 2 KB, ocupa 2)
    private final int desperdicioKb;     // Fragmentación interna: 4 - tamanoRealKb

    // Bits de control de memoria virtual (Sesión 8)
    private boolean bitValidez;          // true = en RAM (v), false = en Disco / Swap (i)
    private boolean bitReferencia;       // Bit R: 1 = referenciada (para algoritmo Reloj / 2da Oportunidad)
    private Integer marcoAsignado;       // Número de marco físico en RAM (null si está en swap)

    public Pagina(int numeroPagina, int tamanoRealKb) {
        this.numeroPagina = numeroPagina;
        this.tamanoRealKb = Math.min(tamanoRealKb, TAMANO_MARCO_KB);
        this.desperdicioKb = TAMANO_MARCO_KB - this.tamanoRealKb;
        this.bitValidez = false;         // Por defecto inicia en memoria virtual (disco)
        this.bitReferencia = false;
        this.marcoAsignado = null;
    }

    public void cargarEnRAM(int numeroMarco) {
        this.marcoAsignado = numeroMarco;
        this.bitValidez = true;
        this.bitReferencia = true;
    }

    public void descargarDeRAM() {
        this.marcoAsignado = null;
        this.bitValidez = false;
        this.bitReferencia = false;
    }

    public void referenciar() {
        this.bitReferencia = true;
    }

    // Getters y Setters
    public int getNumeroPagina() {
        return numeroPagina;
    }

    public int getTamanoRealKb() {
        return tamanoRealKb;
    }

    public int getDesperdicioKb() {
        return desperdicioKb;
    }

    public boolean isBitValidez() {
        return bitValidez;
    }

    public void setBitValidez(boolean bitValidez) {
        this.bitValidez = bitValidez;
    }

    public boolean isBitReferencia() {
        return bitReferencia;
    }

    public void setBitReferencia(boolean bitReferencia) {
        this.bitReferencia = bitReferencia;
    }

    public Integer getMarcoAsignado() {
        return marcoAsignado;
    }

    public void setMarcoAsignado(Integer marcoAsignado) {
        this.marcoAsignado = marcoAsignado;
    }

    @Override
    public String toString() {
        return "Pagina[" + numeroPagina + "] (Tam=" + tamanoRealKb + "KB, Desperdicio=" + desperdicioKb +
                "KB, RAM=" + (bitValidez ? "Marco " + marcoAsignado : "Disco") + ", R=" + (bitReferencia ? "1" : "0") + ")";
    }
}
