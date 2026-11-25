package uni.edu.pe.memory;

import uni.edu.pe.model.Pagina;

/**
 * Representa la página seleccionada como víctima para ser desalojada a Swap
 * durante un reemplazo de página en memoria RAM.
 * Basado en la Sesión 8 (Diapositivas 25 y 26).
 */
public class PaginaVictima {

    private final int numeroMarco;
    private final int pidProceso;
    private final int numeroPagina;
    private final Pagina pagina;

    public PaginaVictima(int numeroMarco, int pidProceso, int numeroPagina, Pagina pagina) {
        this.numeroMarco = numeroMarco;
        this.pidProceso = pidProceso;
        this.numeroPagina = numeroPagina;
        this.pagina = pagina;
    }

    public int getNumeroMarco() { return numeroMarco; }
    public int getPidProceso() { return pidProceso; }
    public int getNumeroPagina() { return numeroPagina; }
    public Pagina getPagina() { return pagina; }

    @Override
    public String toString() {
        return String.format("Página Víctima: PID=%d, Pág=%d en Marco=%d", pidProceso, numeroPagina, numeroMarco);
    }
}

