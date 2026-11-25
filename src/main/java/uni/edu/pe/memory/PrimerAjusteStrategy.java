package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;

import java.util.ArrayList;
import java.util.List;

public class PrimerAjusteStrategy implements EstrategiaAsignacion {

   @Override
    public List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid) {
        List<Integer> marcosAsignados = new ArrayList<>();
        if (ram.getMarcosLibres() < paginas.size()) {
            return marcosAsignados; // No hay espacio suficiente
        }

        int PrimerMarcoAsignado = -1;
        boolean nuevoLibre = false;
        for (int i = 0; i < MemoriaRAM.TOTAL_MARCOS;i++){
            if (ram.getMapaDeBits().get(i) == false && nuevoLibre==false){
                nuevoLibre = true;
                PrimerMarcoAsignado = i;
            }
            if(ram.getMapaDeBits().get(i) == true && nuevoLibre==true){
                nuevoLibre = false;
                if(PrimerMarcoAsignado != -1 && i-PrimerMarcoAsignado >= paginas.size()){
                    break;
                }
            }
        }

        for (Pagina pagina : paginas) {
            int marcoLibre = PrimerMarcoAsignado + marcosAsignados.size();
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

