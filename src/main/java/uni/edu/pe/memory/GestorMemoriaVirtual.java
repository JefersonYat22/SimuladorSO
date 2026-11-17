package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

import java.util.*;

/**
 * Gestor Central de Memoria Virtual y Paginación.
 * Implementa los requerimientos de la Diapositiva 8 y Diapositiva 3:
 * 1. Carga de procesos en su totalidad en memoria virtual (Swap), con carga total o parcial en RAM física.
 * 2. Soporte de asignación dinámica (Stack y Heap).
 * 3. Direccionamiento lógico (virtual) vs físico (RAM).
 * 4. Gestión de traducción y resolución de Fallos de Página mediante la MMU.
 * 5. Administración del mapa de bits y políticas de reemplazo de páginas (Reloj / FIFO).
 * 6. Control continuo de fragmentación interna (desperdicio en KB) en bloques de 32 KB.
 */
public class GestorMemoriaVirtual {

    private final MemoriaRAM ram;
    private final MMU mmu;
    private AlgoritmoReemplazo algoritmoReemplazo;

    // Tabla de Procesos activos registrados en memoria virtual (Swap/RAM)
    private final Map<Integer, Proceso> tablaProcesos;

    // Contadores y métricas de memoria virtual
    private long totalSwapIn;
    private long totalSwapOut;
    private final List<String> historialEventos;

    public GestorMemoriaVirtual(MemoriaRAM ram, AlgoritmoReemplazo algoritmoReemplazo) {
        this.ram = ram;
        this.mmu = new MMU();
        this.algoritmoReemplazo = (algoritmoReemplazo != null) ? algoritmoReemplazo : new ReemplazoReloj();
        this.tablaProcesos = new HashMap<>();
        this.totalSwapIn = 0;
        this.totalSwapOut = 0;
        this.historialEventos = new ArrayList<>();
    }

    public GestorMemoriaVirtual(MemoriaRAM ram) {
        this(ram, new ReemplazoReloj());
    }

    // ─── 1. ADMISIÓN Y CARGA DE PROCESOS (Diapositiva 8, punto 1) ─────────

    /**
     * Registra un proceso en la memoria virtual (Swap) en su totalidad, y lo carga
     * de forma total o parcial en la memoria RAM física según el parámetro.
     *
     * @param proceso           Proceso a admitir.
     * @param cargaTotalEnRAM   true = carga todos los marcos que pueda; false = carga parcial (lazy swapper).
     * @return Cantidad de páginas que lograron cargarse en la RAM física.
     */
    public int admitirProceso(Proceso proceso, boolean cargaTotalEnRAM) {
        if (proceso == null) return 0;

        tablaProcesos.put(proceso.getPid(), proceso);
        List<Pagina> paginas = proceso.getPaginas();
        int paginasCargadas = 0;

        // Por defecto, todas las páginas ya inician en memoria virtual (bitValidez = false)
        if (cargaTotalEnRAM) {
            // Carga Total: intentar alojar tantas páginas en marcos libres de RAM como sea posible
            for (Pagina pag : paginas) {
                int marcoLibre = ram.getPrimerMarcoLibre();
                if (marcoLibre != -1) {
                    ram.cargarPagina(marcoLibre, proceso.getPid(), pag);
                    algoritmoReemplazo.registrarCarga(marcoLibre);
                    paginasCargadas++;
                } else {
                    break; // No hay más marcos libres en RAM, las restantes quedan en Swap
                }
            }
        } else {
            // Carga Parcial (Paginación por demanda / Lazy Swapper - Sesión 8, Diapositiva 21):
            // Solo se carga la primera página inicial (código de arranque), las demás quedan en swap.
            if (!paginas.isEmpty()) {
                int marcoLibre = ram.getPrimerMarcoLibre();
                if (marcoLibre != -1) {
                    Pagina primeraPagina = paginas.get(0);
                    ram.cargarPagina(marcoLibre, proceso.getPid(), primeraPagina);
                    algoritmoReemplazo.registrarCarga(marcoLibre);
                    paginasCargadas = 1;
                }
            }
        }

        registrarEvento(String.format("Proceso %s (PID=%d, Tam=%d KB) admitido en Memoria Virtual (%d páginas). Residentes en RAM: %d/%d.",
                proceso.getNombre(), proceso.getPid(), proceso.getTamanoTotalKb(), paginas.size(), paginasCargadas, paginas.size()));

        return paginasCargadas;
    }

    // ─── 2. ACCESO A MEMORIA Y TRADUCCIÓN MMU (Diapositiva 8, punto 4) ────

    /**
     * Realiza un acceso de memoria a una dirección virtual lógica (página, desplazamiento).
     * Si la página no está en RAM, gestiona la rutina de Fallo de Página automáticamente.
     *
     * @return ResultadoTraduccionMMU con la dirección física resultante.
     */
    public ResultadoTraduccionMMU accederMemoria(Proceso proceso, int numeroPagina, int desplazamientoKb) {
        try {
            // Intento directo de traducción mediante hardware de la MMU
            return mmu.traducir(proceso, numeroPagina, desplazamientoKb);

        } catch (FalloPaginaException e) {
            // ¡PAGE FAULT detectado!
            registrarEvento(String.format("¡Fallo de Página detectado! PID=%d intentó acceder a Página %d no residente.",
                    proceso.getPid(), numeroPagina));

            // Ejecutar la rutina formal de resolución de fallo de página (Sesión 8, Diapositiva 23)
            resolverFalloPagina(proceso, numeroPagina);

            // Reintentar la instrucción tras cargar la página (Instruction Restart)
            try {
                return mmu.traducir(proceso, numeroPagina, desplazamientoKb);
            } catch (FalloPaginaException ex) {
                throw new IllegalStateException("Error crítico: el fallo de página no pudo resolverse en RAM.", ex);
            }
        }
    }

    // ─── 3. RUTINA DE ATENCIÓN DE FALLO DE PÁGINA (Sesión 8, Diapositiva 23) ─

    /**
     * Resuelve un Fallo de Página trayendo la página demandada desde el Disco/Swap a la RAM.
     * Si la RAM está 100% llena, invoca al algoritmo de reemplazo para desalojar una página víctima.
     *
     * @param proceso      Proceso causante del fallo.
     * @param numeroPagina Página lógica a cargar.
     * @return Número del marco físico de RAM donde quedó alojada la página.
     */
    public int resolverFalloPagina(Proceso proceso, int numeroPagina) {
        Pagina paginaDemandada = proceso.getPagina(numeroPagina);
        if (paginaDemandada == null) {
            throw new IllegalArgumentException("La página " + numeroPagina + " no existe en el proceso PID=" + proceso.getPid());
        }

        int marcoDestino = ram.getPrimerMarcoLibre();

        if (marcoDestino != -1) {
            // Caso A: Hay marco libre en RAM física
            ram.cargarPagina(marcoDestino, proceso.getPid(), paginaDemandada);
            algoritmoReemplazo.registrarCarga(marcoDestino);
            totalSwapIn++;

            registrarEvento(String.format("Swap-in: Página %d del Proceso %s cargada en Marco libre %d.",
                    numeroPagina, proceso.getNombre(), marcoDestino));

        } else {
            // Caso B: La RAM está 100% llena -> ¡Ejecutar Algoritmo de Reemplazo! (Diapositiva 8 punto 7)
            PaginaVictima victima = algoritmoReemplazo.seleccionarVictima(ram.getMarcos(), tablaProcesos);
            if (victima == null) {
                throw new IllegalStateException("No se pudo seleccionar una víctima para el reemplazo de páginas.");
            }

            marcoDestino = victima.getNumeroMarco();

            // 1. Swap-out: Desalojar página víctima de la RAM hacia el Swap/Disco
            ram.liberarMarco(marcoDestino);
            victima.getPagina().descargarDeRAM();
            totalSwapOut++;

            registrarEvento(String.format("Swap-out [%s]: Página %d de PID=%d desalojada del Marco %d hacia Swap.",
                    algoritmoReemplazo.getNombre(), victima.getNumeroPagina(), victima.getPidProceso(), marcoDestino));

            // 2. Swap-in: Cargar página demandada en el marco recién liberado
            ram.cargarPagina(marcoDestino, proceso.getPid(), paginaDemandada);
            algoritmoReemplazo.registrarCarga(marcoDestino);
            totalSwapIn++;

            registrarEvento(String.format("Swap-in: Página demandada %d de PID=%d cargada en Marco %d.",
                    numeroPagina, proceso.getPid(), marcoDestino));
        }

        return marcoDestino;
    }

    // ─── 4. DESALOJO Y LIBERACIÓN DE MEMORIA ──────────────────────────────

    /**
     * Libera todos los marcos ocupados por un proceso cuando este finaliza su ejecución.
     */
    public void liberarMemoriaProceso(Proceso proceso) {
        if (proceso == null) return;

        int marcosLiberados = 0;
        for (Pagina pag : proceso.getPaginas()) {
            if (pag.isBitValidez() && pag.getMarcoAsignado() != null) {
                ram.liberarMarco(pag.getMarcoAsignado());
                pag.descargarDeRAM();
                marcosLiberados++;
            }
        }
        tablaProcesos.remove(proceso.getPid());

        registrarEvento(String.format("Memoria liberada: Proceso %s finalizado. Se desocuparon %d marcos de RAM.",
                proceso.getNombre(), marcosLiberados));
    }

    // ─── 5. CONTROL DE FRAGMENTACIÓN Y DESPERDICIO (Diapositiva 3) ────────

    /**
     * Calcula la fragmentación interna total (desperdicio en KB) de los marcos ocupados en RAM.
     */
    public int calcularFragmentacionInternaTotalKb() {
        return ram.calcularDesperdicioTotalKb();
    }

    /**
     * Retorna la lista de páginas residentes en RAM física de un proceso determinado.
     */
    public List<Pagina> getPaginasResidentes(Proceso proceso) {
        List<Pagina> residentes = new ArrayList<>();
        if (proceso != null) {
            for (Pagina p : proceso.getPaginas()) {
                if (p.isBitValidez()) {
                    residentes.add(p);
                }
            }
        }
        return residentes;
    }

    // ─── GETTERS, SETTERS Y ESTADÍSTICAS ─────────────────────────────────

    public MemoriaRAM getRam() { return ram; }
    public MMU getMmu() { return mmu; }
    public AlgoritmoReemplazo getAlgoritmoReemplazo() { return algoritmoReemplazo; }

    public void setAlgoritmoReemplazo(AlgoritmoReemplazo algoritmoReemplazo) {
        this.algoritmoReemplazo = algoritmoReemplazo;
        registrarEvento("Algoritmo de reemplazo cambiado a: " + algoritmoReemplazo.getNombre());
    }

    public Map<Integer, Proceso> getTablaProcesos() { return Collections.unmodifiableMap(tablaProcesos); }
    public long getTotalSwapIn() { return totalSwapIn; }
    public long getTotalSwapOut() { return totalSwapOut; }
    public List<String> getHistorialEventos() { return Collections.unmodifiableList(historialEventos); }

    private void registrarEvento(String desc) {
        historialEventos.add(desc);
    }

    public void reset() {
        ram.reset();
        mmu.reiniciarEstadisticas();
        algoritmoReemplazo.reiniciar();
        tablaProcesos.clear();
        totalSwapIn = 0;
        totalSwapOut = 0;
        historialEventos.clear();
    }
}

