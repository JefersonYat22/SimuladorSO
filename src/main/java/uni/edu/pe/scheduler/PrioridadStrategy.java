package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Comparator;
import java.util.Queue;

/**
 * Planificación por Prioridades: Menor valor numérico = Mayor prioridad.
 */
public class PrioridadStrategy implements PlanificadorCPU {

    private final boolean apropiativo;

    public PrioridadStrategy(boolean apropiativo) {
        this.apropiativo = apropiativo;
    }

    @Override
    public Proceso seleccionarSiguiente(Queue<Proceso> colaListos) {
        if (colaListos.isEmpty()) return null;

        Proceso mayorPrioridad = colaListos.stream()
                .min(Comparator.comparingInt(Proceso::getPrioridad))
                .orElse(null);

        if (mayorPrioridad != null) {
            colaListos.remove(mayorPrioridad);
        }
        return mayorPrioridad;
    }

    @Override
    public boolean esApropiativo() {
        return apropiativo;
    }

    @Override
    public String getNombre() {
        return apropiativo ? "Prioridades (Apropiativo)" : "Prioridades (No Apropiativo)";
    }
}

