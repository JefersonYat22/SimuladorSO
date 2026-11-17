package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Queue;

/**
 * Interfaz del Patrón Strategy para la Planificación de CPU a Corto Plazo.
 * Define el contrato que deben cumplir todas las políticas requeridas (Diap. 4, punto 5).
 */
public interface PlanificadorCPU {

    /**
     * Selecciona el siguiente proceso a ejecutar de la cola de listos.
     */
    Proceso seleccionarSiguiente(Queue<Proceso> colaListos);

    /**
     * Indica si el algoritmo es apropiativo (expropiativo) o no.
     */
    boolean esApropiativo();

    /**
     * Nombre legible del algoritmo para la interfaz gráfica y reportes.
     */
    String getNombre();
}

