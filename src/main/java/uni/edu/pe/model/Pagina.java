package uni.edu.pe.model;

public class Pagina {

    public static final int TAMANO_MARCO_KB = 32;

    private final int numeroPagina;
    private final int tamanoRealKb;
    private final int desperdicioKb;
    private int burstTotalPagina;
    private int burstRestantePagina;

    private boolean bitValidez;
    private boolean bitReferencia;
    private Integer marcoAsignado;

    public Pagina(int numeroPagina, int tamanoRealKb, int burstTotalPagina) {
        this.numeroPagina = numeroPagina;
        this.tamanoRealKb = Math.min(tamanoRealKb, TAMANO_MARCO_KB);
        this.desperdicioKb = TAMANO_MARCO_KB - this.tamanoRealKb;
        this.burstTotalPagina = burstTotalPagina;
        this.burstRestantePagina = burstTotalPagina;
        this.bitValidez = false;
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
            this.bitReferencia = true;
        }
        return burstRestantePagina == 0;
    }

    public int getNumeroPagina() { return numeroPagina; }
    public int getTamanoRealKb() { return tamanoRealKb; }
    public int getDesperdicioKb() { return desperdicioKb; }
    public int getBurstTotalPagina() { return burstTotalPagina; }
    public int getBurstRestantePagina() { return burstRestantePagina; }
    public boolean isBitValidez() { return bitValidez; }
    public void setBitValidez(boolean bitValidez) { this.bitValidez = bitValidez; }
    public boolean isBitReferencia() { return bitReferencia; }
    public void setBitReferencia(boolean bitReferencia) { this.bitReferencia = bitReferencia; }
    public Integer getMarcoAsignado() { return marcoAsignado; }
    public void setMarcoAsignado(Integer marcoAsignado) { this.marcoAsignado = marcoAsignado; }

    @Override
    public String toString() {
        return "Pagina[" + numeroPagina + "] (Tam=" + tamanoRealKb + "KB, Desperdicio=" + desperdicioKb +
                "KB, BurstRest=" + burstRestantePagina + ", RAM=" + (bitValidez ? "Marco " + marcoAsignado : "Disco") + ")";
    }
}
