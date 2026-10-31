package uni.edu.pe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Representa un Proceso y su Bloque de Control de Proceso (PCB).
 * Cumple con todos los requerimientos especificados:
 * - 5 Estados clásicos.
 * - Program Counter (PC) explícito.
 * - Desglose de memoria: Código, Datos y Stack/Heap (% del ejecutable).
 * - Segmentación en páginas de 32 KB con registro de desperdicio.
 * - Regla del 0.5% de procesos errados (5 por cada 1,000).
 * - Generación de 5 a 20 interrupciones según tamaño y burst.
 * - Registro de métricas: tiempos de llegada, espera, retorno y respuesta.
 */
public class Proceso {

    private static int contadorPid = 1;
    private static final Random random = new Random();

    // ─── CAMPOS OBLIGATORIOS DEL PCB ─────────────────────────────────────
    private final int pid;
    private final String nombre;
    private EstadoProceso estado;
    private int programCounter;          // PC: Apunta al índice de página o instrucción actual
    private int prioridad;

    // ─── TIEMPOS DE CPU (BURST) ──────────────────────────────────────────
    private final int burstTotal;
    private int burstRestante;
    private int instruccionesEjecutadas; // Total de ticks completados por este proceso

    // ─── DESGLOSE DE MEMORIA (Diapositiva 3) ──────────────────────────────
    private final int tamanoCodigoKb;    // Tamaño del ejecutable
    private final int tamanoDatosKb;     // Segmento de datos (% del ejecutable)
    private final int tamanoVariableKb;  // Memoria dinámica (Stack y Heap) (% del ejecutable)
    private final int tamanoTotalKb;     // Suma total requerida en memoria

    // ─── PAGINACIÓN (32 KB por marco/bloque) ──────────────────────────────
    private final List<Pagina> paginas;
    private int paginaActualIdx;

    // ─── ADMINISTRACIÓN DE ERRORES (Diapositiva 6, punto 8 y 9) ───────────
    private final boolean esErrado;
    private String codigoError;          // Ej. "ERR_005_SEG_FAULT" si fue cancelado

    // ─── INTERRUPCIONES DE E/S (Diapositiva 6, puntos 10 y 11) ────────────
    private final List<InterrupcionIO> interrupciones;

    // ─── MÉTRICAS DE EVALUACIÓN (Diapositiva 7, punto 17) ─────────────────
    private int tiempoLlegada;
    private int tiempoPrimerAccesoCPU = -1;
    private int tiempoFinalizacion;

    /**
     * Constructor manual o programático.
     */
    public Proceso(int tamanoCodigoKb, int burstTotal) {
        this.pid = contadorPid++;
        this.nombre = "P" + this.pid;
        this.estado = EstadoProceso.NUEVO;
        this.burstTotal = burstTotal;
        this.burstRestante = burstTotal;
        this.prioridad = burstTotal; // Menor burst = mayor prioridad por defecto
        this.instruccionesEjecutadas = 0;
        this.programCounter = 0;

        // Desglose de memoria según Diapositiva 3:
        // Datos: 20% a 40% del ejecutable
        // Variable (Stack/Heap): 15% a 30% del ejecutable
        this.tamanoCodigoKb = tamanoCodigoKb;
        this.tamanoDatosKb = Math.max(8, (int) (tamanoCodigoKb * (0.20 + random.nextDouble() * 0.20)));
        this.tamanoVariableKb = Math.max(8, (int) (tamanoCodigoKb * (0.15 + random.nextDouble() * 0.15)));
        this.tamanoTotalKb = this.tamanoCodigoKb + this.tamanoDatosKb + this.tamanoVariableKb;

        // Regla del 0.5% (5 por cada 1,000) de procesos errados
        this.esErrado = (random.nextInt(1000) < 5);
        this.codigoError = this.esErrado ? "ERR_0.5%_PROCESO_DEFECTUOSO" : null;

        // Generar páginas de 32 KB
        this.paginas = new ArrayList<>();
        this.paginaActualIdx = 0;
        generarPaginas();

        // Generar 5 a 20 interrupciones según fórmula
        this.interrupciones = new ArrayList<>();
        generarInterrupciones();
    }

    /**
     * Reinicia el contador de PIDs (útil para reiniciar simulaciones).
     */
    public static void reiniciarContadorPid() {
        contadorPid = 1;
    }

    /**
     * Divide la memoria total del proceso en páginas fijas de 32 KB.
     */
    private void generarPaginas() {
        int numPaginas = (int) Math.ceil((double) tamanoTotalKb / Pagina.TAMANO_MARCO_KB);
        if (numPaginas == 0) numPaginas = 1;

        int burstRestanteADistribuir = this.burstTotal;
        int tamanoRestanteADistribuir = this.tamanoTotalKb;

        for (int i = 0; i < numPaginas; i++) {
            int tamanoPag = Math.min(Pagina.TAMANO_MARCO_KB, tamanoRestanteADistribuir);
            tamanoRestanteADistribuir -= tamanoPag;

            // Distribuir burst entre las páginas
            int burstPag;
            if (i == numPaginas - 1) {
                burstPag = burstRestanteADistribuir;
            } else {
                int promedio = Math.max(1, burstRestanteADistribuir / (numPaginas - i));
                burstPag = Math.max(1, Math.min(promedio, burstRestanteADistribuir - (numPaginas - i - 1)));
                burstRestanteADistribuir -= burstPag;
            }

            paginas.add(new Pagina(i, tamanoPag, burstPag));
        }
    }

    /**
     * Genera entre 5 y 20 interrupciones de E/S con duración entre 5 y 20 ticks.
     * Fórmula basada en peso de proceso y burst time (Diapositiva 6, punto 10 y 11).
     */
    private void generarInterrupciones() {
        int cantInterrupciones;
        if (tamanoTotalKb > 512 && burstTotal > 20) {
            cantInterrupciones = 10 + random.nextInt(11); // 10 a 20
        } else if (tamanoTotalKb > 512 || burstTotal > 20) {
            cantInterrupciones = 7 + random.nextInt(8);   // 7 a 14
        } else {
            cantInterrupciones = 5 + random.nextInt(5);   // 5 a 9
        }

        // Si el burst es muy pequeño, limitamos para que no hayan más interrupciones que ticks
        cantInterrupciones = Math.min(cantInterrupciones, Math.max(1, burstTotal / 2));

        DispositivoIO[] dispositivos = DispositivoIO.values();

        for (int i = 0; i < cantInterrupciones; i++) {
            DispositivoIO disp = dispositivos[random.nextInt(dispositivos.length)];
            int tickDisparo = random.nextInt(Math.max(1, burstTotal));
            int duracion = 5 + random.nextInt(16); // Duración entre 5 y 20 ticks

            interrupciones.add(new InterrupcionIO(disp, tickDisparo, duracion));
        }

        // Ordenar interrupciones cronológicamente según su tick de disparo
        interrupciones.sort((a, b) -> Integer.compare(a.getTickDisparo(), b.getTickDisparo()));
    }

    /**
     * Ejecuta UN tick del proceso en la CPU.
     * Retorna true si el proceso ha completado todo su burst.
     */
    public boolean ejecutarTick(int tickRelojGlobal) {
        if (tiempoPrimerAccesoCPU == -1) {
            tiempoPrimerAccesoCPU = tickRelojGlobal;
        }

        if (burstRestante > 0) {
            burstRestante--;
            instruccionesEjecutadas++;
            programCounter++;

            // Ejecutar en la página actual
            Pagina pagina = getPaginaActual();
            if (pagina != null) {
                boolean paginaCompleta = pagina.ejecutarInstruccion();
                if (paginaCompleta && paginaActualIdx + 1 < paginas.size()) {
                    paginaActualIdx++; // Avanzamos a la siguiente página lógica
                }
            }
        }

        return burstRestante == 0;
    }

    /**
     * Retorna la página lógica que se está ejecutando actualmente en CPU.
     */
    public Pagina getPaginaActual() {
        if (paginaActualIdx < paginas.size()) {
            return paginas.get(paginaActualIdx);
        }
        return null;
    }

    /**
     * Verifica si en el tick actual de este proceso le toca disparar una interrupción de E/S.
     */
    public InterrupcionIO verificarInterrupcionPendiente() {
        for (InterrupcionIO io : interrupciones) {
            if (!io.isCompletada() && io.getTickDisparo() == instruccionesEjecutadas) {
                return io;
            }
        }
        return null;
    }

    // ─── CÁLCULO DE MÉTRICAS (Para el informe y la tabla final) ───────────
    public int getTiempoRetorno() { // Turnaround Time
        return tiempoFinalizacion - tiempoLlegada;
    }

    public int getTiempoEspera() {   // Waiting Time
        return getTiempoRetorno() - burstTotal;
    }

    public int getTiempoRespuesta() { // Response Time
        return (tiempoPrimerAccesoCPU != -1) ? (tiempoPrimerAccesoCPU - tiempoLlegada) : 0;
    }

    // ─── GETTERS Y SETTERS ───────────────────────────────────────────────
    public int getPid() { return pid; }
    public String getNombre() { return nombre; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso estado) { this.estado = estado; }
    public int getProgramCounter() { return programCounter; }
    public void setProgramCounter(int pc) { this.programCounter = pc; }
    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }
    public int getBurstTotal() { return burstTotal; }
    public int getBurstRestante() { return burstRestante; }
    public int getInstruccionesEjecutadas() { return instruccionesEjecutadas; }
    public int getTamanoTotalKb() { return tamanoTotalKb; }
    public int getTamanoCodigoKb() { return tamanoCodigoKb; }
    public int getTamanoDatosKb() { return tamanoDatosKb; }
    public int getTamanoVariableKb() { return tamanoVariableKb; }
    public List<Pagina> getPaginas() { return Collections.unmodifiableList(paginas); }
    public int getPaginaActualIdx() { return paginaActualIdx; }
    public boolean isEsErrado() { return esErrado; }
    public String getCodigoError() { return codigoError; }
    public void setCodigoError(String codigoError) { this.codigoError = codigoError; }
    public List<InterrupcionIO> getInterrupciones() { return interrupciones; }
    public int getTiempoLlegada() { return tiempoLlegada; }
    public void setTiempoLlegada(int tiempoLlegada) { this.tiempoLlegada = tiempoLlegada; }
    public int getTiempoFinalizacion() { return tiempoFinalizacion; }
    public void setTiempoFinalizacion(int tiempoFinalizacion) { this.tiempoFinalizacion = tiempoFinalizacion; }

    @Override
    public String toString() {
        return nombre + "(PID=" + pid + ", Estado=" + estado + ", Tam=" + tamanoTotalKb + "KB, Burst=" + burstRestante + "/" + burstTotal + ")";
    }
}

