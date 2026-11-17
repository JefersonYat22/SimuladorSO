package uni.edu.pe.memory;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

import java.util.List;
import java.util.Map;

/**
 * Algoritmo del Reloj (Clock / Segunda Oportunidad).
 * Especificación exacta según Sesión 8 (Diapositiva 26):
 * - Implementación circular con un puntero/manecilla sobre los marcos físicos de la RAM.
 * - Inspecciona el bit de referencia (R):
 *    * Si R == 0: Se selecciona como víctima inmediatamente y la manecilla avanza.
 *    * Si R == 1: Se le otorga una "segunda oportunidad", se limpia su bit (R <- 0)
 *      y la manecilla avanza a la siguiente posición hasta encontrar una víctima.
 */
public class ReemplazoReloj implements AlgoritmoReemplazo {

    private int punteroManecilla;

    public ReemplazoReloj() {
        this.punteroManecilla = 0;
    }

    @Override
    public PaginaVictima seleccionarVictima(List<MarcoRAM> marcos, Map<Integer, Proceso> tablaProcesos) {
        int totalMarcos = marcos.size();
        if (totalMarcos == 0) return null;

        // Máximo 2 vueltas completas garantizan encontrar una víctima
        int maxIteraciones = totalMarcos * 2;
        int iteraciones = 0;

        while (iteraciones < maxIteraciones) {
            MarcoRAM marco = marcos.get(punteroManecilla);
            int marcoActualIdx = punteroManecilla;
            punteroManecilla = (punteroManecilla + 1) % totalMarcos;
            iteraciones++;

            if (!marco.estaLibre()) {
                int pid = marco.getPidProceso();
                int numPagina = marco.getNumeroPagina();

                Proceso proceso = tablaProcesos.get(pid);
                if (proceso != null) {
                    Pagina pagina = proceso.getPagina(numPagina);
                    if (pagina != null) {
                        if (!pagina.isBitReferencia()) {
                            // R == 0: ¡Víctima seleccionada!
                            return new PaginaVictima(marcoActualIdx, pid, numPagina, pagina);
                        } else {
                            // R == 1: Segunda oportunidad, se limpia a 0
                            pagina.setBitReferencia(false);
                        }
                    }
                }
            }
        }

        // Fallback: si por alguna razón no se encontró, tomar el primer marco ocupado
        for (int i = 0; i < totalMarcos; i++) {
            MarcoRAM marco = marcos.get(i);
            if (!marco.estaLibre()) {
                Proceso p = tablaProcesos.get(marco.getPidProceso());
                if (p != null) {
                    Pagina pag = p.getPagina(marco.getNumeroPagina());
                    return new PaginaVictima(i, marco.getPidProceso(), marco.getNumeroPagina(), pag);
                }
            }
        }

        return null;
    }

    @Override
    public void registrarCarga(int numeroMarco) {
        // En Clock, al cargar una página su bit de referencia se inicia en 1 (R = 1)
    }

    @Override
    public void reiniciar() {
        this.punteroManecilla = 0;
    }

    public int getPunteroManecilla() {
        return punteroManecilla;
    }

    @Override
    public String getNombre() {
        return "Reloj (Clock / Segunda Oportunidad)";
    }
}

