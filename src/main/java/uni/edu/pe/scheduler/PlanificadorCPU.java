package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Queue;

public interface PlanificadorCPU {

    Proceso seleccionarSiguiente(Queue<Proceso> colaListos);

    boolean esApropiativo();

    String getNombre();

    default boolean debeExpropiar(Proceso Ejecutandose, Proceso candidato) {
        return false;
    }

    default void aplicarAging(Queue<Proceso> colalistos) {

    }
}
