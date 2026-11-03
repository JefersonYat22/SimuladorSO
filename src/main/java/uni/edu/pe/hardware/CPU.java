package uni.edu.pe.hardware;

import uni.edu.pe.model.Proceso;

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
