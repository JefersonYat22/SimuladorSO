package uni.edu.pe.core;

import uni.edu.pe.hardware.CPU;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.Proceso;

public class Despachador {

    public void cambiarContexto(CPU cpu, Proceso nuevoProceso) {
        if (cpu.estaOcupada()) {
            Proceso saliente = cpu.desalojarProceso();
            if (saliente != null && saliente.getEstado() == EstadoProceso.EJECUTANDO) {
                saliente.setEstado(EstadoProceso.LISTO);
            }
        }

        if (nuevoProceso != null) {
            nuevoProceso.setEstado(EstadoProceso.EJECUTANDO);
            cpu.asignarProceso(nuevoProceso);
        }
    }
}

