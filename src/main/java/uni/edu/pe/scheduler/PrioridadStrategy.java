package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Comparator;
import java.util.Queue;

/**
 * Planificación por Prioridades: Menor valor numérico = Mayor prioridad.
 */
public class PrioridadStrategy implements PlanificadorCPU {

    private final boolean apropiativo;
    private int ticksAumentaPrioridad = 5;
    private int contTicks = 0;

    public PrioridadStrategy(boolean apropiativo) {
        this.apropiativo = apropiativo;
    }

    public PrioridadStrategy(boolean apropiativo, int ticksAumentaPrioridad) {
        this.apropiativo = apropiativo;
        this.ticksAumentaPrioridad = Math.max(1, ticksAumentaPrioridad);
    }

    @Override
    public Proceso seleccionarSiguiente(Queue<Proceso> colaListos) {
        if (colaListos.isEmpty())
            return null;

        Proceso mayorPrioridad = colaListos.stream()
                .min(Comparator.comparingInt(Proceso::getPrioridad))
                .orElse(null);

        if (mayorPrioridad != null) {
            colaListos.remove(mayorPrioridad);
        }
        return mayorPrioridad;
    }

    public void aplicarAging(Queue<Proceso> colalistos) {
        contTicks++;
        if (contTicks >= ticksAumentaPrioridad) {
            contTicks = 0;
            for (Proceso p : colalistos) {
                if (p.getPrioridad() > 1) {
                    p.setPrioridad(p.getPrioridad() - 1);
                }
            }
        }
    }

    public boolean debeExpropiar(Proceso Ejecutandose, Proceso candidato) {
        if (!apropiativo || Ejecutandose == null || candidato == null) {
            return false;
        }
        return candidato.getPrioridad() < Ejecutandose.getPrioridad();
    }

    @Override
    public boolean esApropiativo() {
        return apropiativo;
    }

    @Override
    public String getNombre() {
        return apropiativo ? "Prioridades (Apropiativo)" : "Prioridades (No Apropiativo)";
    }

    public int getTicksAumentaPrioridad() {
        return ticksAumentaPrioridad;
    }

    public void setTicksAumentaPrioridad(int ticksAumentaPrioridad) {
        this.ticksAumentaPrioridad = Math.max(1, ticksAumentaPrioridad);
    }
}
