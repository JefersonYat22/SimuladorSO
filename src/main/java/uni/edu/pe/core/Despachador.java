package uni.edu.pe.core;

import uni.edu.pe.hardware.CPU;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.Proceso;

public class Despachador {

    private int latenciaPorCambio = 1;
    private int CambiosdeContextoTotales = 0;
    private int timeOverheadTotal = 0;

    public Despachador() {
        this(1);
    }

    public Despachador(int latenciaPorCambio) {
        this.latenciaPorCambio = Math.max(0, latenciaPorCambio);
    }

    public void cambiarContexto(CPU cpu, Proceso nuevoProceso) {
        if (cpu.estaOcupada()) {
            Proceso saliente = cpu.desalojarProceso();
            if (saliente != null) {
                if (saliente.getEstado() == EstadoProceso.EJECUTANDO) {
                    saliente.setEstado(EstadoProceso.LISTO);
                }
            }
        }

        if (nuevoProceso != null) {
            nuevoProceso.setEstado(EstadoProceso.EJECUTANDO);
            cpu.asignarProceso(nuevoProceso);
        }

        CambiosdeContextoTotales++;
        timeOverheadTotal += latenciaPorCambio;
    }

    public int getCambiosdeContextoTotales() {
        return CambiosdeContextoTotales;
    }

    public int getTimeOverheadTotal() {
        return timeOverheadTotal;
    }

    public int getLatenciaporCambio() {
        return latenciaPorCambio;
    }

    public void reset() {
        this.CambiosdeContextoTotales = 0;
        this.timeOverheadTotal = 0;
    }
}
