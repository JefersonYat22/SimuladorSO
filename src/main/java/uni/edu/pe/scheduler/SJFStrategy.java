package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Comparator;
import java.util.Queue;

/**
 * Shortest Job First (SJF): Selecciona el proceso con menor ráfaga restante.
 */
public class SJFStrategy implements PlanificadorCPU {

    private final boolean apropiativo;

    public SJFStrategy(boolean apropiativo) {
        this.apropiativo = apropiativo;
    }

    @Override
    public Proceso seleccionarSiguiente(Queue<Proceso> colaListos) {
        if (colaListos.isEmpty()) return null;

        Proceso masCorto = colaListos.stream()
                .min(Comparator.comparingInt(Proceso::getBurstRestante))
                .orElse(null);

        if (masCorto != null) {
            colaListos.remove(masCorto);
        }
        return masCorto;
    }

    @Override
    public boolean esApropiativo() {
        return apropiativo;
    }

    @Override
    public String getNombre() {
        return apropiativo ? "SRTF (SJF Apropiativo)" : "SJF (No Apropiativo)";
    }
}

