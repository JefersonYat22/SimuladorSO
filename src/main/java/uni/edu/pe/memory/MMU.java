package uni.edu.pe.memory;

import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

/**
 * Unidad de Manejo de Memoria (MMU - Memory Management Unit).
 * Cumple con los requerimientos exactos de la Diapositiva 8 (punto 4):
 * "El MMU debe gestionar la traducción de las direcciones virtuales a las físicas
 *  y así ubicar los bloques de memoria asignados al proceso".
 *
 * Basado formalmente en la Sesión 8 (Diapositivas 22, 23 y 24):
 * - Dirección Lógica: [ Número de Página (p) | Desplazamiento (d) ].
 * - Dirección Física: [ Número de Marco (f)  | Desplazamiento (d) ].
 * - Valida bit de validez (v/i): si 'i' dispara FalloPaginaException (Trap de Page Fault).
 * - Valida límites de memoria: si fuera de rango dispara ViolacionMemoriaException (Seg Fault).
 * - En acceso exitoso, marca el bit de referencia R = 1 para el algoritmo del Reloj.
 */
public class MMU {

    private long totalAccesos;
    private long totalAciertos;
    private long totalFallosPagina;

    public MMU() {
        this.totalAccesos = 0;
        this.totalAciertos = 0;
        this.totalFallosPagina = 0;
    }

    /**
     * Traduce una dirección lógica dada por (página p, desplazamiento d) a dirección física.
     *
     * @param proceso          Proceso que ejecuta el acceso.
     * @param numeroPagina     Índice de la página virtual (p).
     * @param desplazamientoKb Desplazamiento dentro del bloque (d) en KB (0 a TAMANO_MARCO_KB - 1).
     * @return ResultadoTraduccionMMU con la dirección física y marco asignado.
     * @throws FalloPaginaException      Si la página no está cargada en RAM física (bit v/i = i).
     * @throws ViolacionMemoriaException Si la página o desplazamiento exceden los límites del proceso.
     */
    public ResultadoTraduccionMMU traducir(Proceso proceso, int numeroPagina, int desplazamientoKb)
            throws FalloPaginaException, ViolacionMemoriaException {

        totalAccesos++;

        // 1. Verificación de límites de memoria (Sesión 8, Diapositiva 23 punto 3)
        if (proceso == null || numeroPagina < 0 || numeroPagina >= proceso.getPaginas().size()
                || desplazamientoKb < 0 || desplazamientoKb >= Pagina.TAMANO_MARCO_KB) {
            int dirIlegal = (numeroPagina * Pagina.TAMANO_MARCO_KB) + Math.max(0, desplazamientoKb);
            int limite = (proceso != null) ? proceso.getTamanoTotalKb() : 0;
            int pid = (proceso != null) ? proceso.getPid() : -1;
            throw new ViolacionMemoriaException(pid, dirIlegal, limite);
        }

        Pagina pagina = proceso.getPagina(numeroPagina);

        // 2. Verificación del Bit de Validez (v/i)
        if (pagina == null || !pagina.isBitValidez() || pagina.getMarcoAsignado() == null) {
            totalFallosPagina++;
            int direccionLogica = (numeroPagina * Pagina.TAMANO_MARCO_KB) + desplazamientoKb;
            throw new FalloPaginaException(proceso.getPid(), numeroPagina, direccionLogica);
        }

        // 3. Acierto de Página (Hit): Marcar bit de referencia R = 1 (Sesión 8, Diapositiva 26)
        pagina.referenciar();
        totalAciertos++;

        return new ResultadoTraduccionMMU(proceso.getPid(), numeroPagina, pagina.getMarcoAsignado(), desplazamientoKb);
    }

    /**
     * Traduce una dirección virtual lineal entera (en KB) a dirección física.
     */
    public ResultadoTraduccionMMU traducirDireccionLineal(Proceso proceso, int direccionVirtualKb)
            throws FalloPaginaException, ViolacionMemoriaException {

        if (direccionVirtualKb < 0 || (proceso != null && direccionVirtualKb >= proceso.getTamanoTotalKb())) {
            int limite = (proceso != null) ? proceso.getTamanoTotalKb() : 0;
            int pid = (proceso != null) ? proceso.getPid() : -1;
            throw new ViolacionMemoriaException(pid, direccionVirtualKb, limite);
        }

        int numeroPagina = direccionVirtualKb / Pagina.TAMANO_MARCO_KB;
        int desplazamiento = direccionVirtualKb % Pagina.TAMANO_MARCO_KB;
        return traducir(proceso, numeroPagina, desplazamiento);
    }

    // ─── RENDIMIENTO Y FÓRMULAS OFICIALES (Sesión 8, Diapositiva 24) ───────

    /**
     * Retorna la tasa o probabilidad de fallo de página p (0 <= p <= 1).
     */
    public double getTasaFalloPagina() {
        return (totalAccesos > 0) ? ((double) totalFallosPagina / totalAccesos) : 0.0;
    }

    /**
     * Calcula el Tiempo de Acceso Efectivo (EAT - Effective Access Time):
     * EAT = (1 - p) * ma + p * TRPF
     *
     * @param ma   Tiempo de acceso a RAM en nanosegundos (ej. 100 ns).
     * @param trpf Tiempo de resolución del page fault en nanosegundos (ej. 25 ms = 25,000,000 ns).
     */
    public double calcularTiempoAccesoEfectivo(double ma, double trpf) {
        double p = getTasaFalloPagina();
        return (1.0 - p) * ma + (p * trpf);
    }

    public long getTotalAccesos() { return totalAccesos; }
    public long getTotalAciertos() { return totalAciertos; }
    public long getTotalFallosPagina() { return totalFallosPagina; }

    public void reiniciarEstadisticas() {
        this.totalAccesos = 0;
        this.totalAciertos = 0;
        this.totalFallosPagina = 0;
    }
}
