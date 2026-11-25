package uni.edu.pe.memory;

/**
 * Representa el resultado exitoso de una traducción de dirección lógica a física
 * realizada por el hardware de la MMU.
 * Basado en la Sesión 8 (Diapositiva 22): Dirección Física = [ Número de Marco (f) | Offset (d) ].
 */
public class ResultadoTraduccionMMU {

    private final int pid;
    private final int numeroPagina;
    private final int numeroMarco;
    private final int desplazamientoKb;
    private final int direccionFisicaKb;

    public ResultadoTraduccionMMU(int pid, int numeroPagina, int numeroMarco, int desplazamientoKb) {
        this.pid = pid;
        this.numeroPagina = numeroPagina;
        this.numeroMarco = numeroMarco;
        this.desplazamientoKb = desplazamientoKb;
        // Dirección Física Base = marco * TAMANO_MARCO_KB + desplazamiento
        this.direccionFisicaKb = (numeroMarco * uni.edu.pe.model.Pagina.TAMANO_MARCO_KB) + desplazamientoKb;
    }

    public int getPid() { return pid; }
    public int getNumeroPagina() { return numeroPagina; }
    public int getNumeroMarco() { return numeroMarco; }
    public int getDesplazamientoKb() { return desplazamientoKb; }
    public int getDireccionFisicaKb() { return direccionFisicaKb; }

    @Override
    public String toString() {
        return String.format("Traducción MMU [PID=%d]: Lógica (p=%d, d=%d KB) -> Física (Marco=%d, DirFísica=%d KB)",
                pid, numeroPagina, desplazamientoKb, numeroMarco, direccionFisicaKb);
    }
}

