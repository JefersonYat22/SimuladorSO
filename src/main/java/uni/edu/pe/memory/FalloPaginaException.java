package uni.edu.pe.memory;

/**
 * Excepción / Trampa (Trap) que emite la MMU al intentar acceder a una página
 * marcada con bit de validez 'i' (inválida/ausente de la RAM física).
 * Basado en la Sesión 8 (Memoria Virtual, Diapositiva 23).
 */
public class FalloPaginaException extends Exception {

    private final int pid;
    private final int numeroPagina;
    private final int direccionLogica;

    public FalloPaginaException(int pid, int numeroPagina, int direccionLogica) {
        super(String.format("Fallo de Página (Page Fault): Proceso PID=%d intentó acceder a la Página %d (Dir. Lógica: %d KB) ausente en RAM física.",
                pid, numeroPagina, direccionLogica));
        this.pid = pid;
        this.numeroPagina = numeroPagina;
        this.direccionLogica = direccionLogica;
    }

    public int getPid() {
        return pid;
    }

    public int getNumeroPagina() {
        return numeroPagina;
    }

    public int getDireccionLogica() {
        return direccionLogica;
    }
}

