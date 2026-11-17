package uni.edu.pe.memory;

/**
 * Trampa de Error de Hardware (Trap de la MMU) por violación de límites de memoria
 * (Segmentation Fault / Acceso Ilegal).
 * Basado en la Sesión 7 (Diapositiva 18) y Sesión 8 (Diapositiva 23, punto 3).
 */
public class ViolacionMemoriaException extends RuntimeException {

    private final int pid;
    private final int direccionIlegal;
    private final int limitePermitido;

    public ViolacionMemoriaException(int pid, int direccionIlegal, int limitePermitido) {
        super(String.format("Violación de Límite de Memoria (Segmentation Fault): Proceso PID=%d intentó acceder a dirección %d KB, superando el límite asignado de %d KB.",
                pid, direccionIlegal, limitePermitido));
        this.pid = pid;
        this.direccionIlegal = direccionIlegal;
        this.limitePermitido = limitePermitido;
    }

    public int getPid() {
        return pid;
    }

    public int getDireccionIlegal() {
        return direccionIlegal;
    }

    public int getLimitePermitido() {
        return limitePermitido;
    }
}

