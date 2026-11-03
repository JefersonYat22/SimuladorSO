package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;

import java.util.ArrayList;
import java.util.List;

/**
 * Primer Ajuste (First-Fit): Asigna las páginas en los primeros marcos libres que encuentra.
 */
public class PrimerAjusteStrategy implements EstrategiaAsignacion {

    @Override
    public List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid) {
        List<Integer> marcosAsignados = new ArrayList<>();
        if (ram.getMarcosLibres() < paginas.size()) {
            return marcosAsignados; // No hay espacio suficiente
        }

        for (Pagina pagina : paginas) {
            int marcoLibre = ram.getPrimerMarcoLibre();
            if (marcoLibre != -1) {
                ram.cargarPagina(marcoLibre, pid, pagina);
                marcosAsignados.add(marcoLibre);
            }
        }
        return marcosAsignados;
    }

    @Override
    public String getNombre() {
        return "Primer Ajuste (First-Fit)";
    }
}

