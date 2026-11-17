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
       // En paginación pura con marcos fijos, si hay marcos libres se colocan minimizando dispersión
        List<Integer> marcosAsignados = new ArrayList<>();
        if (ram.getMarcosLibres() < paginas.size()) {
            return marcosAsignados;
        }


        List<Integer> PosiblesMarcos = new ArrayList<>();
        List<Integer> TamanoMarcos = new ArrayList<>();
        boolean nuevoLibre = false;

        //escanea toda la memoria RAM para encontrar los bloques de marcos libres y sus tamaños
        for (int i = 0; i < MemoriaRAM.TOTAL_MARCOS;i++){
            if (ram.getMapaDeBits().get(i) == false && nuevoLibre==false){
                nuevoLibre = true;
                PosiblesMarcos.add(i);
            }
            if(ram.getMapaDeBits().get(i) == true && nuevoLibre==true){
                nuevoLibre = false;
                TamanoMarcos.add(i-PosiblesMarcos.get(PosiblesMarcos.size()-1));
            }
        }
        if(nuevoLibre==true){
            TamanoMarcos.add(MemoriaRAM.TOTAL_MARCOS-PosiblesMarcos.get(PosiblesMarcos.size()-1));
        }
        
        int mejorIndice = -1;//se encuentra el bloque de marcos libres más ajustado al tamaño de páginas requeridas
        for (int i = 0; i < PosiblesMarcos.size(); i++) {
            if (TamanoMarcos.get(i) >= paginas.size()) {
                if (mejorIndice == -1 || TamanoMarcos.get(i) > TamanoMarcos.get(mejorIndice)) {
                    mejorIndice = i;
                }
            }
        }

        if (mejorIndice == -1) {
            return marcosAsignados;
        }

        for (Pagina pagina : paginas) {
            int marco = PosiblesMarcos.get(mejorIndice) + marcosAsignados.size();
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

