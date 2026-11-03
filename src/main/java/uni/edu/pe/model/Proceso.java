package uni.edu.pe.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Proceso {

    private static int contadorPid = 1;
    private static final Random random = new Random();

    private final int pid;
    private final String nombre;
    private EstadoProceso estado;
    private int programCounter;
    private int prioridad;

    private final int burstTotal;
    private int burstRestante;
    private int instruccionesEjecutadas;

    private final int tamanoCodigoKb;
    private final int tamanoDatosKb;
    private final int tamanoVariableKb;
    private final int tamanoTotalKb;

    private final List<Pagina> paginas;
    private int paginaActualIdx;

    private final boolean esErrado;
    private String codigoError;
    private final List<InterrupcionIO> interrupciones;

    private int tiempoLlegada;
    private int tiempoPrimerAccesoCPU = -1;
    private int tiempoFinalizacion;

    public Proceso(int tamanoCodigoKb, int burstTotal) {
        this.pid = contadorPid++;
        this.nombre = "P" + this.pid;
        this.estado = EstadoProceso.NUEVO;
        this.burstTotal = burstTotal;
        this.burstRestante = burstTotal;
        this.prioridad = burstTotal;
        this.instruccionesEjecutadas = 0;
        this.programCounter = 0;

        this.tamanoCodigoKb = tamanoCodigoKb;
        this.tamanoDatosKb = Math.max(8, (int) (tamanoCodigoKb * (0.20 + random.nextDouble() * 0.20)));
        this.tamanoVariableKb = Math.max(8, (int) (tamanoCodigoKb * (0.15 + random.nextDouble() * 0.15)));
        this.tamanoTotalKb = this.tamanoCodigoKb + this.tamanoDatosKb + this.tamanoVariableKb;

        this.esErrado = (random.nextInt(1000) < 5);
        this.codigoError = this.esErrado ? "ERR_PROCESO_DEFECTUOSO" : null;

        this.paginas = new ArrayList<>();
        this.paginaActualIdx = 0;
        generarPaginas();

        this.interrupciones = new ArrayList<>();
        generarInterrupciones();
    }

    public static void reiniciarContadorPid() {
        contadorPid = 1;
    }

    private void generarPaginas() {
        int numPaginas = (int) Math.ceil((double) tamanoTotalKb / Pagina.TAMANO_MARCO_KB);
        if (numPaginas == 0) numPaginas = 1;

        int burstRestanteADistribuir = this.burstTotal;
        int tamanoRestanteADistribuir = this.tamanoTotalKb;

        for (int i = 0; i < numPaginas; i++) {
            int tamanoPag = Math.min(Pagina.TAMANO_MARCO_KB, tamanoRestanteADistribuir);
            tamanoRestanteADistribuir -= tamanoPag;

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

    private void generarInterrupciones() {
        int cantInterrupciones;
        if (tamanoTotalKb > 512 && burstTotal > 20) {
            cantInterrupciones = 10 + random.nextInt(11);
        } else if (tamanoTotalKb > 512 || burstTotal > 20) {
            cantInterrupciones = 7 + random.nextInt(8);
        } else {
            cantInterrupciones = 5 + random.nextInt(5);
        }

        cantInterrupciones = Math.min(cantInterrupciones, Math.max(1, burstTotal / 2));
        DispositivoIO[] dispositivos = DispositivoIO.values();

        for (int i = 0; i < cantInterrupciones; i++) {
            DispositivoIO disp = dispositivos[random.nextInt(dispositivos.length)];
            int tickDisparo = random.nextInt(Math.max(1, burstTotal));
            int duracion = 5 + random.nextInt(16);

            interrupciones.add(new InterrupcionIO(disp, tickDisparo, duracion));
        }

        interrupciones.sort((a, b) -> Integer.compare(a.getTickDisparo(), b.getTickDisparo()));
    }

    public boolean ejecutarTick(int tickRelojGlobal) {
        if (tiempoPrimerAccesoCPU == -1) {
            tiempoPrimerAccesoCPU = tickRelojGlobal;
        }

        if (burstRestante > 0) {
            burstRestante--;
            instruccionesEjecutadas++;
            programCounter++;

            Pagina pagina = getPaginaActual();
            if (pagina != null) {
                boolean paginaCompleta = pagina.ejecutarInstruccion();
                if (paginaCompleta && paginaActualIdx + 1 < paginas.size()) {
                    paginaActualIdx++;
                }
            }
        }

        return burstRestante == 0;
    }

    public Pagina getPaginaActual() {
        if (paginaActualIdx < paginas.size()) {
            return paginas.get(paginaActualIdx);
        }
        return null;
    }

    public InterrupcionIO verificarInterrupcionPendiente() {
        for (InterrupcionIO io : interrupciones) {
            if (!io.isCompletada() && io.getTickDisparo() == instruccionesEjecutadas) {
                return io;
            }
        }
        return null;
    }

    public int getTiempoRetorno() {
        return tiempoFinalizacion - tiempoLlegada;
    }

    public int getTiempoEspera() {
        return getTiempoRetorno() - burstTotal;
    }

    public int getTiempoRespuesta() {
        return (tiempoPrimerAccesoCPU != -1) ? (tiempoPrimerAccesoCPU - tiempoLlegada) : 0;
    }

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
