package uni.edu.pe.memory;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Algoritmo de Reemplazo FIFO (First-In, First-Out).
 * Especificación según Sesión 8 (Diapositiva 25):
 * - Desaloja la página que lleva más tiempo en memoria principal utilizando una cola.
 * - Sirve como punto de referencia para comparar contra el algoritmo del Reloj.
 */
public class ReemplazoFIFO implements AlgoritmoReemplazo {

    private final Queue<Integer> colaMarcos;

    public ReemplazoFIFO() {
        this.colaMarcos = new LinkedList<>();
    }

    @Override
    public PaginaVictima seleccionarVictima(List<MarcoRAM> marcos, Map<Integer, Proceso> tablaProcesos) {
        while (!colaMarcos.isEmpty()) {
            int marcoIdx = colaMarcos.poll();
            if (marcoIdx >= 0 && marcoIdx < marcos.size()) {
                MarcoRAM marco = marcos.get(marcoIdx);
                if (!marco.estaLibre()) {
                    int pid = marco.getPidProceso();
                    int numPag = marco.getNumeroPagina();
                    Proceso p = tablaProcesos.get(pid);
                    if (p != null) {
                        Pagina pag = p.getPagina(numPag);
                        return new PaginaVictima(marcoIdx, pid, numPag, pag);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void registrarCarga(int numeroMarco) {
        if (!colaMarcos.contains(numeroMarco)) {
            colaMarcos.offer(numeroMarco);
        }
    }

    @Override
    public void reiniciar() {
        colaMarcos.clear();
    }

    @Override
    public String getNombre() {
        return "FIFO (First In, First Out)";
    }
}

