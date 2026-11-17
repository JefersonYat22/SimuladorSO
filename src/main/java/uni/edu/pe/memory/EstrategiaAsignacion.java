package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;
import java.util.List;

/**
 * Interfaz Strategy para las Tres Estrategias de Asignación de Memoria
 * exigidas en la Diapositiva 8 (punto 6) y Diapositiva 9 del PDF:
 *  1. Primer Ajuste (First-Fit)
 *  2. Mejor Ajuste (Best-Fit)
 *  3. Peor Ajuste (Worst-Fit)
 */
public interface EstrategiaAsignacion {

    /**
     * Asigna un conjunto de páginas a marcos de memoria RAM física.
     * Retorna la lista de índices de marcos asignados, o lista vacía si no cupo.
     */
    List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid);

    String getNombre();
}

