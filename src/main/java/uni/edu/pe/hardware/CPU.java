package uni.edu.pe.hardware;

import uni.edu.pe.model.Proceso;

/**
 * Emula la Unidad Central de Procesamiento (CPU).
 * Responsabilidades (Diapositivas 2 y 4):
 *  - Mantiene el Program Counter (PC) apuntando a la instrucción actual.
 *  - Mantiene referencia al proceso actualmente en ejecución.
 *  - Ejecuta ciclos de instrucción mediante el método tick().
 */
public class CPU {

    private int ticksTotales;
    private int programCounter;
    private Proceso procesoActual;

    public CPU() {
        this.ticksTotales = 0;
        this.programCounter = 0;
        this.procesoActual = null;
    }

    public void asignarProceso(Proceso p) {
        this.procesoActual = p;
        this.programCounter = p.getProgramCounter();
    }

    public Proceso desalojarProceso() {
        Proceso anterior = this.procesoActual;
        if (anterior != null) {
            anterior.setProgramCounter(this.programCounter);
        }
        this.procesoActual = null;
        this.programCounter = 0;
        return anterior;
    }

    public boolean tick() {
        ticksTotales++;
        if (procesoActual != null) {
            programCounter++;
            return procesoActual.ejecutarTick(ticksTotales);
        }
        return false;
    }

    public boolean estaOcupada() {
        return procesoActual != null;
    }

    public int getTicksTotales() { return ticksTotales; }
    public int getProgramCounter() { return programCounter; }
    public Proceso getProcesoActual() { return procesoActual; }

    public void reset() {
        this.ticksTotales = 0;
        this.programCounter = 0;
        this.procesoActual = null;
    }
}

