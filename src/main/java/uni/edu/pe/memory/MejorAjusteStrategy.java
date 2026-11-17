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


        boolean nuevoLibre = false;
        int tamanoBloque = 9999999; //inicializamos con un valor grande para encontrar el bloque más pequeño
        int marcoInicial = -1;
        int posibleMarcoInicial = -1;

        //escanea toda la memoria RAM para encontrar los bloques de marcos libres y sus tamaños
        for (int i = 0; i < MemoriaRAM.TOTAL_MARCOS;i++){
            if (ram.getMapaDeBits().get(i) == false && nuevoLibre==false){
                nuevoLibre = true;
                posibleMarcoInicial=i;
            }
            if(ram.getMapaDeBits().get(i) == true && nuevoLibre==true){
                nuevoLibre = false;
                if(i - posibleMarcoInicial <  tamanoBloque){
                    tamanoBloque = i-posibleMarcoInicial;
                    marcoInicial= posibleMarcoInicial;
                }
            }
        }
        if(nuevoLibre==true){
            if(MemoriaRAM.TOTAL_MARCOS - posibleMarcoInicial <  tamanoBloque){
                tamanoBloque = MemoriaRAM.TOTAL_MARCOS-posibleMarcoInicial;
                marcoInicial= posibleMarcoInicial;
            }            
        }
        
        if (marcoInicial == -1) {
            return marcosAsignados;
        }

        for (Pagina pagina : paginas) {
            int marco = marcoInicial + marcosAsignados.size();
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
