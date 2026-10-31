package uni.edu.pe;

import com.formdev.flatlaf.FlatDarkLaf;
import uni.edu.pe.model.Proceso;

public class Main {
    public static void main(String[] args) {
        // Inicializamos el Look & Feel moderno oscuro
        FlatDarkLaf.setup();

        System.out.println("==========================================================");
        System.out.println("   SIMULADOR DE SISTEMA OPERATIVO — UNI (Java 21)");
        System.out.println("==========================================================\n");

        // Creamos un proceso de prueba: 120 KB de código ejecutable, 12 ticks de CPU
        Proceso p1 = new Proceso(120, 12);

        System.out.println("Proceso Creado: " + p1);
        System.out.println("----------------------------------------------------------");
        System.out.println(" • PID: " + p1.getPid() + " (" + p1.getNombre() + ")");
        System.out.println(" • Estado inicial: " + p1.getEstado());
        System.out.println(" • Program Counter (PC): " + p1.getProgramCounter());
        System.out.println(" • Desglose de Memoria:");
        System.out.println("     - Código ejecutable: " + p1.getTamanoCodigoKb() + " KB");
        System.out.println("     - Datos:             " + p1.getTamanoDatosKb() + " KB");
        System.out.println("     - Stack / Heap:      " + p1.getTamanoVariableKb() + " KB");
        System.out.println("     -------------------------------------------");
        System.out.println("     - Total requerido:   " + p1.getTamanoTotalKb() + " KB");
        System.out.println(" • ¿Es defectuoso (0.5%)?: " + p1.isEsErrado() + (p1.isEsErrado() ? " [" + p1.getCodigoError() + "]" : ""));
        System.out.println(" • Cantidad de páginas (de 32 KB): " + p1.getPaginas().size());

        System.out.println("\nPáginas generadas (con cálculo de desperdicio):");
        p1.getPaginas().forEach(pag ->
            System.out.printf("   [Pág %d] Tamaño Real: %2d KB | Desperdicio: %2d KB | Burst: %2d ticks%n",
                    pag.getNumeroPagina(), pag.getTamanoRealKb(), pag.getDesperdicioKb(), pag.getBurstTotalPagina())
        );

        System.out.println("\nInterrupciones de E/S programadas (" + p1.getInterrupciones().size() + " en total):");
        p1.getInterrupciones().forEach(io ->
            System.out.printf("   -> Dispositivo: %-10s | Dispara en tick: %2d | Duración: %2d ticks%n",
                    io.getDispositivo(), io.getTickDisparo(), io.getDuracionTotal())
        );

        System.out.println("\n==========================================================");
        System.out.println("¡Componente 1 (Modelo de Proceso y PCB) funcionando al 100%!");
        System.out.println("==========================================================");
    }
}
