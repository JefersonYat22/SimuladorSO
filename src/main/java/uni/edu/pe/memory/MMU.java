package uni.edu.pe.memory;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

/**
 * Memory Management Unit (MMU) — Diapositiva 8, punto 4:
 * "El MMU debe gestionar la traducción de las direcciones virtuales a las físicas
 * y así ubicar los bloques de memoria asignados al proceso".
 */
public class MMU {

    /**
     * Traduce una dirección lógica (página virtual + offset) a una dirección física en RAM.
     * Retorna la dirección física en KB, o -1 si ocurre Page Fault (la página no está en RAM).
     */
    public int traducirDireccion(Proceso proceso, int numeroPagina, int offsetKb) {
        if (numeroPagina < 0 || numeroPagina >= proceso.getPaginas().size()) {
            return -1; // Dirección inválida (Segmentation Fault)
        }

        Pagina pagina = proceso.getPaginas().get(numeroPagina);
        if (!pagina.isBitValidez() || pagina.getMarcoAsignado() == null) {
            return -1; // PAGE FAULT: no está cargada en RAM física
        }

        // Dirección Física = (Número de marco * Tamaño de marco) + offset
        int direccionBaseFisica = pagina.getMarcoAsignado() * MarcoRAM.TAMANO_MARCO_KB;
        return direccionBaseFisica + offsetKb;
    }
}

