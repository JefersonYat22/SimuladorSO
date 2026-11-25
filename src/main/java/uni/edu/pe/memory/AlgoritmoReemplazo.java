package uni.edu.pe.memory;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Proceso;

import java.util.List;
import java.util.Map;

/**
 * Interfaz Strategy para los Algoritmos de Reemplazo de Páginas Físicas en RAM
 * exigidos en la Diapositiva 8 (punto 7) y detallados en la Sesión 8 (Diapositivas 25 y 26).
 */
public interface AlgoritmoReemplazo {

    /**
     * Selecciona una página víctima para ser desalojada a Swap cuando la RAM está llena.
     *
     * @param marcos        Lista de los 256 marcos físicos de la RAM.
     * @param tablaProcesos Mapa de procesos activos en el sistema (por PID) para acceder a sus páginas.
     * @return PaginaVictima seleccionada con su marco y datos de control.
     */
    PaginaVictima seleccionarVictima(List<MarcoRAM> marcos, Map<Integer, Proceso> tablaProcesos);

    /**
     * Notifica al algoritmo que un marco acaba de ser cargado o asignado.
     */
    void registrarCarga(int numeroMarco);

    /**
     * Reinicia el estado interno del algoritmo (punteros, colas, etc.).
     */
    void reiniciar();

    /**
     * Retorna el nombre formal del algoritmo.
     */
    String getNombre();
}

