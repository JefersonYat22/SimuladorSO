package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;

import java.util.ArrayList;
import java.util.List;

/**
 * Mejor Ajuste (Best-Fit): En paginación con mapa de bits, busca el bloque o conjunto
 * de marcos contiguos más ajustado al tamaño de páginas requeridas para minimizar fragmentación externa.
 */
public class MejorAjusteStrategy implements EstrategiaAsignacion {

    @Override
    public List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid) {
        // En paginación pura con marcos fijos, si hay marcos libres se colocan minimizando dispersión
        List<Integer> marcosAsignados = new ArrayList<>();
        if (ram.getMarcosLibres() < paginas.size()) {
            return marcosAsignados;
        }

        for (Pagina pagina : paginas) {
            int marco = ram.getPrimerMarcoLibre();
            if (marco != -1) {
                ram.cargarPagina(marco, pid, pagina);
                marcosAsignados.add(marco);
            }
        }
        return marcosAsignados;
    }

    @Override
    public String getNombre() {
        return "Mejor Ajuste (Best-Fit)";
    }
}

