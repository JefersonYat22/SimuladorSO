package uni.edu.pe.model;

/**
 * Representa un marco físico (Frame) de la Memoria RAM.
 * Tamaño fijo: 32 KB.
 */
public class MarcoRAM {

    public static final int TAMANO_MARCO_KB = 32;

    private final int numeroMarco;
    private Integer pidProceso;       // null si el marco está libre
    private Integer numeroPagina;     // página del proceso alojada aquí
    private int desperdicioKb;        // fragmentación interna dentro de este marco

    public MarcoRAM(int numeroMarco) {
        this.numeroMarco = numeroMarco;
        this.pidProceso = null;
        this.numeroPagina = null;
        this.desperdicioKb = 0;
    }

    public boolean estaLibre() {
        return pidProceso == null;
    }

    public void asignar(int pid, int pagina, int desperdicioKb) {
        this.pidProceso = pid;
        this.numeroPagina = pagina;
        this.desperdicioKb = desperdicioKb;
    }

    public void liberar() {
        this.pidProceso = null;
        this.numeroPagina = null;
        this.desperdicioKb = 0;
    }

    public int getNumeroMarco() { return numeroMarco; }
    public Integer getPidProceso() { return pidProceso; }
    public Integer getNumeroPagina() { return numeroPagina; }
    public int getDesperdicioKb() { return desperdicioKb; }
}

