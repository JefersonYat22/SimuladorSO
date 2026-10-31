package uni.edu.pe.model;

/**
 * Representa una página lógica de un proceso según los requerimientos:
 * - El tamaño del bloque/marco de memoria es múltiplo de 2^n: 32 KB.
 * - Registra la fragmentación interna (desperdicio de memoria).
 * - Mantiene los bits de control para memoria virtual y algoritmos de reemplazo.
 */
public class Pagina {

    public static final int TAMANO_MARCO_KB = 32;

    private final int numeroPagina;
    private final int tamanoRealKb;      // Tamaño ocupado real (ej. si quedan 10 KB, ocupa 10)
    private final int desperdicioKb;     // Fragmentación interna: 32 - tamanoRealKb
    private int burstTotalPagina;
    private int burstRestantePagina;

    // Bits de control de memoria virtual
    private boolean bitValidez;          // true = en RAM (marco asignado), false = en Disco (swap)
    private boolean bitReferencia;       // para algoritmo Segunda Oportunidad / Reloj (1 = referenciada)
    private Integer marcoAsignado;       // Número de marco físico en RAM (null si está en disco)

    public Pagina(int numeroPagina, int tamanoRealKb, int burstTotalPagina) {
        this.numeroPagina = numeroPagina;
        this.tamanoRealKb = Math.min(tamanoRealKb, TAMANO_MARCO_KB);
        this.desperdicioKb = TAMANO_MARCO_KB - this.tamanoRealKb;
        this.burstTotalPagina = burstTotalPagina;
        this.burstRestantePagina = burstTotalPagina;
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

    public boolean ejecutarInstruccion() {
        if (burstRestantePagina > 0) {
            burstRestantePagina--;
            this.bitReferencia = true; // Se marca referenciada al usarse
        }
        return burstRestantePagina == 0;
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

    public int getBurstTotalPagina() {
        return burstTotalPagina;
    }

    public int getBurstRestantePagina() {
        return burstRestantePagina;
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
                "KB, BurstRest=" + burstRestantePagina + ", RAM=" + (bitValidez ? "Marco " + marcoAsignado : "Disco") + ")";
    }
}

