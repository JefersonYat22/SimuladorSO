package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Queue;

/**
 * First-Come, First-Served (FCFS): Atiende por estricto orden de llegada. No apropiativo.
 */
public class FCFSStrategy implements PlanificadorCPU {

    @Override
    public Proceso seleccionarSiguiente(Queue<Proceso> colaListos) {
        return colaListos.poll();
    }

    @Override
    public boolean esApropiativo() {
        return false;
    }

    @Override
    public String getNombre() {
        return "FCFS (First-Come, First-Served)";
    }
}

