package uni.edu.pe;

import com.formdev.flatlaf.FlatDarkLaf;
import uni.edu.pe.core.MotorSO;
import uni.edu.pe.gui.VentanaPrincipal;
import uni.edu.pe.model.Proceso;

public class Main {
    public static void main(String[] args) {
        // Look & Feel moderno oscuro
        FlatDarkLaf.setup();

        System.out.println("==========================================================");
        System.out.println("   SIMULADOR DE SISTEMA OPERATIVO — UNI (Java 21)");
        System.out.println("   Arquitectura Monolítica Modular Inicializada");
        System.out.println("==========================================================\n");

        // Instanciamos el Motor del Sistema Operativo
        MotorSO motor = new MotorSO();

        // 1. Creamos 2 procesos de prueba
        Proceso p1 = new Proceso(90, 10);
        Proceso p2 = new Proceso(120, 15);

        motor.agregarProceso(p1);
        motor.agregarProceso(p2);

        System.out.println("Procesos creados en Cola de Nuevos: " + motor.getColaTotales().size());

        // 2. Ejecutamos 5 ticks de simulación paso a paso
        System.out.println("\n--- Ejecutando 5 Ticks de Simulación ---");
        for (int i = 1; i <= 5; i++) {
            motor.step();
            System.out.printf("[Tick %d] CPU: %s | RAM Marcos Libres: %d/32 | Desperdicio RAM: %d KB%n",
                    motor.getCpu().getTicksTotales(),
                    (motor.getCpu().getProcesoActual() != null ? motor.getCpu().getProcesoActual().getNombre() + " (PC=" + motor.getCpu().getProgramCounter() + ")" : "OCIOSA"),
                    motor.getRam().getMarcosLibres(),
                    motor.getRam().calcularDesperdicioTotalKb()
            );
        }

        System.out.println("\nEventos registrados en el Núcleo:");
        motor.getGestorInterrupciones().getHistorialEventos().forEach(System.out::println);

        System.out.println("\n==========================================================");
        System.out.println("¡Todas las capas conectadas y operativas con éxito!");
        System.out.println("==========================================================");

        // Lanzamos la ventana gráfica
        VentanaPrincipal.iniciar(motor);
    }
}
