package uni.edu.pe.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import uni.edu.pe.core.MotorSO;

import javax.swing.*;
import java.awt.*;

/**
 * Interfaz Gráfica de Usuario (GUI) moderna del Simulador de SO.
 * Diseñada en Swing con el tema FlatLaf Dark.
 * Muestra en tiempo real:
 *  - Los controles de simulación (Iniciar, Pausar, Paso a Paso).
 *  - La CPU activa y su Program Counter.
 *  - El visor de los 32 marcos de Memoria RAM física.
 *  - Las colas de procesos y dispositivos de E/S.
 *  - El terminal de registro de eventos.
 */
public class VentanaPrincipal extends JFrame {

    private final MotorSO motor;
    private Timer timerAnimacion;

    public VentanaPrincipal(MotorSO motor) {
        this.motor = motor;
        inicializarUI();
    }

    private void inicializarUI() {
        setTitle("Simulador de Sistema Operativo — UNI (Java 21)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        // Panel principal con diseño BorderLayout
        JPanel panelContenedor = new JPanel(new BorderLayout(10, 10));
        panelContenedor.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Barra Superior de Control
        panelContenedor.add(crearBarraControles(), BorderLayout.NORTH);

        // 2. Área Central (Dashboard en construcción)
        JLabel labelEstado = new JLabel("Arquitectura del Sistema Operativo lista y conectada.", SwingConstants.CENTER);
        labelEstado.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panelContenedor.add(labelEstado, BorderLayout.CENTER);

        add(panelContenedor);
    }

    private JPanel crearBarraControles() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnStep = new JButton("⏭ Paso a Paso (Tick)");
        btnStep.addActionListener(e -> {
            motor.step();
            System.out.println("Tick ejecutado: " + motor.getCpu().getTicksTotales());
        });

        JButton btnGen20 = new JButton("+ Generar 20 Procesos");
        btnGen20.addActionListener(e -> {
            motor.generar20ProcesosAleatorios();
            System.out.println("20 procesos generados en cola de Nuevos.");
        });

        barra.add(btnStep);
        barra.add(btnGen20);
        return barra;
    }

    public static void iniciar(MotorSO motor) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(motor);
            ventana.setVisible(true);
        });
    }
}

