package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;

import java.util.ArrayList;
import java.util.List;

/**
 * Peor Ajuste (Worst-Fit): Asigna páginas en las regiones con mayores huecos disponibles.
 */
public class PeorAjusteStrategy implements EstrategiaAsignacion {

    @Override
    public List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid) {
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
        return "Peor Ajuste (Worst-Fit)";
    }
}

