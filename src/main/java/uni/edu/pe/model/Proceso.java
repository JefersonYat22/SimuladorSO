package uni.edu.pe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Representa un Proceso y su Bloque de Control de Proceso (PCB).
 * Cumple con todos los requerimientos especificados en el documento:
 * - 5 Estados clásicos (Nuevo, Listo, Ejecutando, Bloqueado, Finalizado).
 * - Program Counter (PC) explícito.
 * - Desglose de memoria: Código, Datos (% del ejecutable) y Stack/Heap (% del ejecutable).
 * - División en páginas de 4 KB con registro de fragmentación interna.
 * - Regla del 0.5% de procesos errados (5 por cada 1,000).
 * - Generación de 10 a 20 interrupciones de E/S.
 * - Registro de métricas: tiempos de llegada, espera, retorno y respuesta.
 */
public class Proceso {

    private static int contadorPid = 1;
    private static final Random random = new Random();

    // ─── CAMPOS OBLIGATORIOS DEL PCB (Diapositiva 4, punto 3) ────────────
    private final int pid;
    private final String nombre;
    private EstadoProceso estado;
    private int programCounter;          // PC: Dirección / contador de la siguiente instrucción
    private int prioridad;

    // ─── TIEMPOS DE CPU (BURST DEL PROCESO) ──────────────────────────────
    private final int burstTotal;
    private int burstRestante;
    private int instruccionesEjecutadas; // Total de ticks completados por este proceso

    // ─── DESGLOSE DE MEMORIA (Diapositiva 3) ──────────────────────────────
    private final int tamanoCodigoKb;    // Tamaño del ejecutable
    private final int tamanoDatosKb;     // Segmento de datos (20% a 40% del ejecutable)
    private final int tamanoVariableKb;  // Memoria dinámica (Stack/Heap) (15% a 30% del ejecutable)
    private final int tamanoTotalKb;     // Suma total requerida en memoria

    // ─── PAGINACIÓN (4 KB por marco/página) ──────────────────────────────
    private final List<Pagina> paginas;

    // ─── ADMINISTRACIÓN DE ERRORES (Diapositiva 6, punto 8 y 9) ───────────
    private final boolean esErrado;
    private String codigoError;          // Ej. "ERR_0.5%_PROCESO_DEFECTUOSO" si fue cancelado

    // ─── INTERRUPCIONES DE E/S (Diapositiva 6 y Diapositivas 10-11) ────────
    private final List<InterrupcionIO> interrupciones;

    // ─── MÉTRICAS DE EVALUACIÓN (Diapositiva 7, punto 17) ─────────────────
    private int tiempoLlegada;
    private int tiempoPrimerAccesoCPU = -1;
    private int tiempoFinalizacion;

    /**
     * Constructor del Proceso.
     */
    public Proceso(int tamanoCodigoKb, int burstTotal) {
        this.pid = contadorPid++;
        this.nombre = "P" + this.pid;
        this.estado = EstadoProceso.NUEVO;
        this.burstTotal = Math.max(1, burstTotal);
        this.burstRestante = this.burstTotal;
        this.prioridad = this.burstTotal; // Menor burst = mayor prioridad por defecto
        this.instruccionesEjecutadas = 0;
        this.programCounter = 0;

        // Desglose de memoria según Diapositiva 3:
        // Datos: 20% a 40% del ejecutable
        // Variable (Stack/Heap): 15% a 30% del ejecutable
        this.tamanoCodigoKb = Math.max(16, tamanoCodigoKb);
        this.tamanoDatosKb = Math.max(8, (int) (this.tamanoCodigoKb * (0.20 + random.nextDouble() * 0.20)));
        this.tamanoVariableKb = Math.max(8, (int) (this.tamanoCodigoKb * (0.15 + random.nextDouble() * 0.15)));
        this.tamanoTotalKb = this.tamanoCodigoKb + this.tamanoDatosKb + this.tamanoVariableKb;

        // Regla del 0.5% (5 por cada 1,000) de procesos errados
        this.esErrado = (random.nextInt(1000) < 5);
        this.codigoError = this.esErrado ? "ERR_0.5%_PROCESO_DEFECTUOSO" : null;

        // Generar páginas de 4 KB (Memoria lógica/virtual)
        this.paginas = new ArrayList<>();
        generarPaginas();

        // Generar interrupciones de E/S
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
     * Divide la memoria total del proceso en páginas fijas de 4 KB.
     * El burst es del proceso, no de las páginas.
     */
    private void generarPaginas() {
        int numPaginas = (int) Math.ceil((double) tamanoTotalKb / Pagina.TAMANO_MARCO_KB);
        if (numPaginas == 0) numPaginas = 1;

        int tamanoRestante = this.tamanoTotalKb;
        for (int i = 0; i < numPaginas; i++) {
            int tamanoPag = Math.min(Pagina.TAMANO_MARCO_KB, tamanoRestante);
            tamanoRestante -= tamanoPag;
            paginas.add(new Pagina(i, tamanoPag));
        }
    }

    /**
     * Genera entre 10 y 20 interrupciones de E/S con duración entre 5 y 20 ticks.
     * Ecuación matemática dependiente del tamaño del proceso y del burst-time
     * (Diapositiva 6 puntos 10-12 y Diapositiva 10 puntos 10-11).
     */
    private void generarInterrupciones() {
        double factorTam = Math.min(1.0, (double) tamanoTotalKb / 512.0);
        double factorBurst = Math.min(1.0, (double) burstTotal / 50.0);
        double factorCarga = (factorTam + factorBurst) / 2.0;

        int baseInterrupciones = 10 + (int) Math.round(5.0 * factorCarga); // 10 a 15
        int cantInterrupciones = Math.min(20, baseInterrupciones + random.nextInt(6)); // 10 a 20

        DispositivoIO[] dispositivos = DispositivoIO.values();

        for (int i = 0; i < cantInterrupciones; i++) {
            DispositivoIO disp = dispositivos[random.nextInt(dispositivos.length)];
            int tickDisparo = random.nextInt(Math.max(1, burstTotal));
            int baseDuracion = 5 + (int) Math.round(7.0 * factorCarga); // 5 a 12
            int duracion = Math.min(20, baseDuracion + random.nextInt(9)); // 5 a 20

            interrupciones.add(new InterrupcionIO(disp, tickDisparo, duracion));
        }

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
        }

        return burstRestante == 0;
    }

    /**
     * Retorna el índice de la página lógica donde reside la instrucción actual
     * apuntada por el Program Counter (PC / 4 KB).
     * Basado en la Sesión 8 (Diapositiva 22): p = Dirección / TamañoMarco.
     */
    public int getPaginaPorProgramCounter() {
        int idx = programCounter / Pagina.TAMANO_MARCO_KB;
        return Math.min(idx, Math.max(0, paginas.size() - 1));
    }

    /**
     * Retorna la página lógica en el índice dado.
     */
    public Pagina getPagina(int index) {
        if (index >= 0 && index < paginas.size()) {
            return paginas.get(index);
        }
        return null;
    }

    /**
     * Retorna la página lógica actual en ejecución según el Program Counter.
     */
    public Pagina getPaginaActual() {
        return getPagina(getPaginaPorProgramCounter());
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

    // ─── CÁLCULO DE MÉTRICAS (Diapositiva 7, punto 17) ─────────────────────
    public int getTiempoRetorno() { // Turnaround Time: Fin - Arribo
        return tiempoFinalizacion - tiempoLlegada;
    }

    public int getTiempoEspera() {   // Waiting Time: Retorno - Burst
        return getTiempoRetorno() - burstTotal;
    }

    public int getTiempoRespuesta() { // Response Time: Primer Acceso - Arribo
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
