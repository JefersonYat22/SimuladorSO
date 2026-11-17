package uni.edu.pe.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneradorProcesos {

    private static final Random rnd = new Random();

    /**
     * Genera los 20 procesos secuenciales de prueba (Diapositiva 7, punto 17).
     * Tamaño: 32 a 332 KB. Burst: 10 a 50 ticks.
     */
    public static List<Proceso> generar20ProcesosSecuenciales() {
        List<Proceso> lista = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            int tamanoKb = 32 + rnd.nextInt(301); // 32 a 332 KB
            int burst = 10 + rnd.nextInt(41); // 10 a 50 ticks
            lista.add(new Proceso(tamanoKb, burst));
        }
        return lista;
    }

    /**
     * Crea un proceso de ingreso manual con parámetros explícitos.
     */
    public static Proceso crearProcesoManual(int tamanoKb, int burst, int prioridad) {
        Proceso p = new Proceso(tamanoKb, burst);
        p.setPrioridad(prioridad);
        return p;
    }
}