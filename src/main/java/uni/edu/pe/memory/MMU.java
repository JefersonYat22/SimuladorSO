package uni.edu.pe.memory;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

public class MMU {

    public int traducirDireccion(Proceso proceso, int numeroPagina, int offsetKb) {
        if (numeroPagina < 0 || numeroPagina >= proceso.getPaginas().size()) {
            return -1; // Dirección inválida (Segmentation Fault)
        }

        Pagina pagina = proceso.getPaginas().get(numeroPagina);
        if (!pagina.isBitValidez() || pagina.getMarcoAsignado() == null) {
            return -1; // PAGE FAULT: no está cargada en RAM física
        }

        int direccionBaseFisica = pagina.getMarcoAsignado() * MarcoRAM.TAMANO_MARCO_KB;
        return direccionBaseFisica + offsetKb;
    }
}

