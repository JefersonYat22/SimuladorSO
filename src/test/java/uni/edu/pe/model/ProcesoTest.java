package uni.edu.pe.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProcesoTest {

    @BeforeEach
    void setUp() {
        Proceso.reiniciarContadorPid();
    }

    @Test
    void testCreacionProcesoBasico() {
        // Creamos un proceso de 100 KB de código y 15 ticks de burst
        Proceso p = new Proceso(100, 15);

        assertEquals(1, p.getPid());
        assertEquals("P1", p.getNombre());
        assertEquals(EstadoProceso.NUEVO, p.getEstado());
        assertEquals(15, p.getBurstTotal());
        assertEquals(15, p.getBurstRestante());
        assertEquals(0, p.getProgramCounter());

        // Verificar que el tamaño total incluye código + datos + memoria variable (stack/heap)
        assertTrue(p.getTamanoTotalKb() > 100, "El tamaño total debe incluir stack, heap y datos");
        assertTrue(p.getTamanoDatosKb() > 0);
        assertTrue(p.getTamanoVariableKb() > 0);

        // Verificar división en páginas de 4 KB
        int paginasEsperadas = (int) Math.ceil((double) p.getTamanoTotalKb() / Pagina.TAMANO_MARCO_KB);
        assertEquals(paginasEsperadas, p.getPaginas().size());

        // Verificar que las páginas registren desperdicio (fragmentación interna)
        Pagina ultimaPagina = p.getPaginas().get(p.getPaginas().size() - 1);
        assertTrue(ultimaPagina.getDesperdicioKb() >= 0 && ultimaPagina.getDesperdicioKb() < Pagina.TAMANO_MARCO_KB);
    }

    @Test
    void testEjecucionTicksYProgramCounter() {
        Proceso p = new Proceso(64, 5);

        // Tick 1
        boolean finalizado = p.ejecutarTick(1);
        assertFalse(finalizado);
        assertEquals(4, p.getBurstRestante());
        assertEquals(1, p.getProgramCounter());

        // Ejecutar los 4 restantes
        p.ejecutarTick(2);
        p.ejecutarTick(3);
        p.ejecutarTick(4);
        finalizado = p.ejecutarTick(5);

        assertTrue(finalizado, "El proceso debió haber terminado");
        assertEquals(0, p.getBurstRestante());
        assertEquals(5, p.getProgramCounter());
    }

    @Test
    void testInterrupcionesGeneradas() {
        Proceso p = new Proceso(600, 30); // Grande y con burst largo

        // Debe generar entre 10 y 20 interrupciones según requerimientos (Diapositiva 10 punto 10)
        assertFalse(p.getInterrupciones().isEmpty());
        assertTrue(p.getInterrupciones().size() >= 10 && p.getInterrupciones().size() <= 20);

        // Verificar que las duraciones estén en el rango de 5 a 20 ticks
        for (InterrupcionIO io : p.getInterrupciones()) {
            assertTrue(io.getDuracionTotal() >= 5 && io.getDuracionTotal() <= 20);
        }
    }
}

