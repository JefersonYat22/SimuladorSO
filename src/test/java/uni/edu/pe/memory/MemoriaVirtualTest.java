package uni.edu.pe.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;

import static org.junit.jupiter.api.Assertions.*;

public class MemoriaVirtualTest {

    private MemoriaRAM ram;
    private GestorMemoriaVirtual gestor;

    @BeforeEach
    void setUp() {
        Proceso.reiniciarContadorPid();
        ram = new MemoriaRAM();
        gestor = new GestorMemoriaVirtual(ram, new ReemplazoReloj());
    }

    @Test
    void testCargaTotalVsParcialEnMemoriaVirtual() {
        // Proceso de 100 KB de código -> requerirá 4 páginas de 32 KB
        Proceso p1 = new Proceso(100, 10);
        int totalPaginas = p1.getPaginas().size();
        assertTrue(totalPaginas >= 4);

        // 1. Carga Parcial (Lazy Swapper / Demand Paging, Diapositiva 8 punto 1)
        int cargadasParcial = gestor.admitirProceso(p1, false);
        assertEquals(1, cargadasParcial, "En carga parcial solo debe cargarse la primera página en RAM");
        assertTrue(p1.getPagina(0).isBitValidez(), "La página 0 debe estar en RAM");
        assertNotNull(p1.getPagina(0).getMarcoAsignado());

        for (int i = 1; i < totalPaginas; i++) {
            assertFalse(p1.getPagina(i).isBitValidez(), "Las páginas restantes deben permanecer en Swap");
            assertNull(p1.getPagina(i).getMarcoAsignado());
        }

        // 2. Carga Total
        Proceso p2 = new Proceso(64, 10); // 2 o 3 páginas
        int cargadasTotal = gestor.admitirProceso(p2, true);
        assertEquals(p2.getPaginas().size(), cargadasTotal, "En carga total deben cargarse todas las páginas en RAM");
        for (Pagina pag : p2.getPaginas()) {
            assertTrue(pag.isBitValidez());
            assertNotNull(pag.getMarcoAsignado());
        }
    }

    @Test
    void testTraduccionExitosaMMU() throws FalloPaginaException {
        Proceso p = new Proceso(32, 10);
        gestor.admitirProceso(p, true);

        Pagina pag0 = p.getPagina(0);
        assertTrue(pag0.isBitValidez());
        int marco = pag0.getMarcoAsignado();

        // Traducir dirección lógica: página 0, offset 2 KB
        ResultadoTraduccionMMU res = gestor.getMmu().traducir(p, 0, 2);
        assertNotNull(res);
        assertEquals(marco, res.getNumeroMarco());
        assertEquals(2, res.getDesplazamientoKb());
        assertEquals((marco * Pagina.TAMANO_MARCO_KB) + 2, res.getDireccionFisicaKb());

        // Verificar que el bit de referencia R se puso en 1 al acceder
        assertTrue(pag0.isBitReferencia());
    }

    @Test
    void testPageFaultCuandoPaginaNoEstaEnRAM() {
        Proceso p = new Proceso(120, 10); // Múltiples páginas
        gestor.admitirProceso(p, false); // Solo la pág 0 se carga

        // Intentar traducir la página 1 que está en Swap -> Debe arrojar FalloPaginaException
        assertThrows(FalloPaginaException.class, () -> {
            gestor.getMmu().traducir(p, 1, 2);
        });

        assertEquals(1, gestor.getMmu().getTotalFallosPagina());
    }

    @Test
    void testViolacionMemoriaPorLimites() {
        Proceso p = new Proceso(32, 10);
        gestor.admitirProceso(p, true);

        // Página inexistente (fuera de límites) -> Violación de memoria (Seg Fault)
        assertThrows(ViolacionMemoriaException.class, () -> {
            gestor.getMmu().traducir(p, 99, 0);
        });

        // Offset mayor o igual a 4 KB -> Violación de memoria
        assertThrows(ViolacionMemoriaException.class, () -> {
            gestor.getMmu().traducir(p, 0, Pagina.TAMANO_MARCO_KB);
        });
    }

    @Test
    void testResolucionAutomaticaDePageFaultConMarcoLibre() {
        Proceso p = new Proceso(100, 10);
        gestor.admitirProceso(p, false); // Solo la página 0 está en RAM

        assertFalse(p.getPagina(1).isBitValidez(), "Página 1 debe empezar en Swap");

        // Al acceder a través de gestor.accederMemoria(), debe atrapar el Page Fault,
        // resolverlo cargando en un marco libre de RAM, y retornar la dirección física válida
        ResultadoTraduccionMMU res = gestor.accederMemoria(p, 1, 2);
        assertNotNull(res);
        assertTrue(p.getPagina(1).isBitValidez(), "Página 1 ahora debe estar en RAM");
        assertEquals(1, gestor.getTotalSwapIn(), "Debe registrarse 1 Swap-in");
    }

    @Test
    void testReemplazoPorRelojCuandoRAMEstaTotalmenteLlena() {
        // La RAM tiene MemoriaRAM.TOTAL_MARCOS (256) marcos. Vamos a llenarla por completo.
        // Cargamos 256 procesos en carga parcial (1 marco de 4 KB cada uno):
        for (int i = 0; i < MemoriaRAM.TOTAL_MARCOS; i++) {
            Proceso p = new Proceso(4, 5);
            gestor.admitirProceso(p, false);
        }

        assertEquals(0, ram.getMarcosLibres(), "La RAM debe estar 100% llena");

        // Ponemos todas las páginas en R = 1 excepto la página en el marco 5, que tendrá R = 0
        for (MarcoRAM m : ram.getMarcos()) {
            Proceso proc = gestor.getTablaProcesos().get(m.getPidProceso());
            Pagina pag = proc.getPagina(m.getNumeroPagina());
            pag.setBitReferencia(true); // Todos con segunda oportunidad
        }

        // Dejamos el marco 5 con R = 0 (candidata a víctima inmediata si la manecilla pasa por ella)
        MarcoRAM marcoVictimaEsperado = ram.getMarcos().get(5);
        Proceso procVictima = gestor.getTablaProcesos().get(marcoVictimaEsperado.getPidProceso());
        Pagina pagVictima = procVictima.getPagina(marcoVictimaEsperado.getNumeroPagina());
        pagVictima.setBitReferencia(false);

        // Creamos un nuevo proceso que no cabe en RAM y solicita acceder a su página 0
        Proceso nuevo = new Proceso(32, 5);
        gestor.admitirProceso(nuevo, false); // Nace en Swap

        // Al acceder, se produce un Page Fault con RAM llena -> Algoritmo del Reloj desaloja a la víctima
        ResultadoTraduccionMMU res = gestor.accederMemoria(nuevo, 0, 0);
        assertNotNull(res);

        // Verificamos que ocurrió al menos 1 Swap-out y 1 Swap-in
        assertTrue(gestor.getTotalSwapOut() >= 1, "Debe haber ocurrido al menos un desalojo (Swap-out)");
        assertTrue(gestor.getTotalSwapIn() >= 1, "Debe haber ocurrido una carga (Swap-in)");

        // La nueva página del proceso debe estar en RAM
        assertTrue(nuevo.getPagina(0).isBitValidez());
    }

    @Test
    void testControlDeFragmentacionInterna() {
        // Proceso de 40 KB
        Proceso p = new Proceso(40, 10);
        gestor.admitirProceso(p, true);

        // La fragmentación interna total debe ser >= 0 y menor a 32 KB por marco
        int fragmentacion = gestor.calcularFragmentacionInternaTotalKb();
        assertTrue(fragmentacion >= 0);
        assertEquals(ram.calcularDesperdicioTotalKb(), fragmentacion);
    }

    @Test
    void testCalculoEAT() {
        MMU mmu = gestor.getMmu();
        // Simulamos un acceso de prueba con valores conocidos
        // ma = 100 ns, trpf = 25,000,000 ns (25 ms)
        double ma = 100.0;
        double trpf = 25000000.0;

        // Con 0 fallos de página (p = 0), EAT = ma
        double eatSinFallos = mmu.calcularTiempoAccesoEfectivo(ma, trpf);
        assertEquals(100.0, eatSinFallos, 0.001);
    }
}

