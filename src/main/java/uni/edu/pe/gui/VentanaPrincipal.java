package uni.edu.pe.gui;

import com.formdev.flatlaf.FlatDarkLaf;
import uni.edu.pe.core.GestorInterrupciones;
import uni.edu.pe.core.MotorSO;
import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.DispositivoIO;
import uni.edu.pe.model.GeneradorProcesos;
import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;
import uni.edu.pe.model.Proceso;
import uni.edu.pe.scheduler.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Panel de Control y Dashboard Gráfico Interactivo del Sistema Operativo.
 * Implementa la visualización en línea exigida en la Diapositiva 7 (punto 16):
 * - Estado de la CPU en tiempo real, Program Counter y proceso actual.
 * - Mapa visual de los 32 marcos de memoria RAM (4 KB c/u) y desperdicio en KB.
 * - Colas del sistema: Nuevos, Listos, Finalizados y Colas de E/S.
 * - Interacción de Teclado (Cancelación o Continuación) (Diapositivas 10 y 11).
 * - Selección de políticas de CPU y cambio de Quantum en caliente (Diapositiva 5, punto 7).
 * - Generador automático de 20 procesos y reporte de métricas (Diapositiva 7, punto 17).
 */
public class VentanaPrincipal extends JFrame {

    private final MotorSO motor;
    private Timer timerSimulacion;
    private int velocidadMs = 400;

    // Componentes de la Barra de Controles
    private JButton btnIniciar;
    private JButton btnPausar;
    private JButton btnStep;
    private JComboBox<String> comboAlgoritmo;
    private JSpinner spinnerQuantum;
    private JSpinner spinnerVelocidad;

    // Componentes del Panel de CPU
    private JLabel lblTickGlobal;
    private JLabel lblProcesoCPU;
    private JLabel lblProgramCounter;
    private JLabel lblBurstRestante;
    private JLabel lblDespachadorStats;

    // Componentes de Memoria RAM (Grilla de 32 marcos)
    private final JLabel[] celdasMarcosRAM = new JLabel[32];
    private JLabel lblRamStats;

    // Tablas de Procesos
    private DefaultTableModel modeloListos;
    private DefaultTableModel modeloNuevos;
    private DefaultTableModel modeloFinalizados;

    // Colas de Dispositivos E/S
    private JLabel lblColaDisco;
    private JLabel lblColaImpresora;
    private JLabel lblColaPantalla;
    private JLabel lblColaTeclado;
    private JPanel panelAccionesTeclado;
    private JButton btnTecladoContinuar;
    private JButton btnTecladoCancelar;
    private Integer pidEsperandoTeclado = null;

    // Terminal de Eventos / Log
    private JTextArea areaTerminalLogs;
    private int ultimoEventoMostradoIdx = 0;

    public VentanaPrincipal(MotorSO motor) {
        this.motor = motor;
        inicializarUI();
        configurarTimer();
        actualizarVista();
    }

    private void inicializarUI() {
        setTitle("Simulador de Sistema Operativo — UNI (Java 21)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1350, 850);
        setMinimumSize(new Dimension(1150, 750));
        setLocationRelativeTo(null);

        JPanel panelRaiz = new JPanel(new BorderLayout(8, 8));
        panelRaiz.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 1. Barra de Herramientas Superior
        panelRaiz.add(crearBarraSuperior(), BorderLayout.NORTH);

        // 2. Panel Central Dividido
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPrincipal.setDividerLocation(850);
        splitPrincipal.setResizeWeight(0.65);

        // Panel Izquierdo: CPU, RAM y E/S
        JPanel panelIzquierdo = new JPanel(new BorderLayout(6, 6));
        panelIzquierdo.add(crearPanelCPUyRAM(), BorderLayout.CENTER);
        panelIzquierdo.add(crearPanelDispositivosIO(), BorderLayout.SOUTH);
        splitPrincipal.setLeftComponent(panelIzquierdo);

        // Panel Derecho: Colas de Procesos y Terminal de Eventos
        JSplitPane splitDerecho = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitDerecho.setDividerLocation(380);
        splitDerecho.setResizeWeight(0.5);
        splitDerecho.setTopComponent(crearPanelTablasProcesos());
        splitDerecho.setBottomComponent(crearPanelTerminalLogs());
        splitPrincipal.setRightComponent(splitDerecho);

        panelRaiz.add(splitPrincipal, BorderLayout.CENTER);
        add(panelRaiz);
    }

    // ─── 1. BARRA SUPERIOR DE HERRAMIENTAS ───────────────────────────────

    private JPanel crearBarraSuperior() {
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        barra.setBorder(BorderFactory.createEtchedBorder());

        JButton btnGen20 = new JButton("+ Cargar 20 Procesos");
        btnGen20.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGen20.addActionListener(e -> {
            GeneradorProcesos.generar20ProcesosSecuenciales().forEach(motor::agregarProceso);
            agregarLog("Se cargaron 20 procesos de prueba secuenciales en la Cola de Nuevos.");
            actualizarVista();
        });

        btnIniciar = new JButton("▶ Iniciar");
        btnIniciar.addActionListener(e -> iniciarSimulacion());

        btnPausar = new JButton("⏸ Pausar");
        btnPausar.setEnabled(false);
        btnPausar.addActionListener(e -> pausarSimulacion());

        btnStep = new JButton("⏭ Paso (Tick)");
        btnStep.addActionListener(e -> {
            motor.step();
            actualizarVista();
        });

        // Selector de Planificador de CPU (Diapositiva 4, punto 5)
        comboAlgoritmo = new JComboBox<>(new String[]{
                "FCFS (Primero en llegar)",
                "SJF (Trabajo más corto - No apropiativo)",
                "SRTF (SJF Apropiativo)",
                "Round Robin (Turno rotatorio)",
                "Prioridades (No apropiativo)",
                "Prioridades (Apropiativo)"
        });
        comboAlgoritmo.addActionListener(e -> cambiarPlanificadorCPU());

        // Spinner para cambiar Quantum en caliente (Diapositiva 5, punto 7)
        JLabel lblQuantum = new JLabel("Quantum:");
        spinnerQuantum = new JSpinner(new SpinnerNumberModel(4, 1, 50, 1));
        spinnerQuantum.setEnabled(false);
        spinnerQuantum.addChangeListener(e -> {
            int nuevoQ = (int) spinnerQuantum.getValue();
            if (motor.getPlanificador() instanceof RoundRobinStrategy rr) {
                rr.setQuantum(nuevoQ);
                agregarLog("Quantum de Round Robin actualizado a: " + nuevoQ);
            }
        });

        // Velocidad
        JLabel lblVelocidad = new JLabel("Paso (ms):");
        spinnerVelocidad = new JSpinner(new SpinnerNumberModel(400, 50, 2000, 50));
        spinnerVelocidad.addChangeListener(e -> {
            velocidadMs = (int) spinnerVelocidad.getValue();
            if (timerSimulacion != null && timerSimulacion.isRunning()) {
                timerSimulacion.setDelay(velocidadMs);
            }
        });

        JButton btnReporte = new JButton("📊 Reporte Métricas");
        btnReporte.addActionListener(e -> mostrarDialogoMetricas());

        JButton btnReset = new JButton("🔄 Reiniciar");
        btnReset.addActionListener(e -> {
            pausarSimulacion();
            motor.reset();
            ultimoEventoMostradoIdx = 0;
            areaTerminalLogs.setText("");
            agregarLog("Sistema Operativo reiniciado a estado inicial.");
            actualizarVista();
        });

        barra.add(btnGen20);
        barra.add(new JSeparator(SwingConstants.VERTICAL));
        barra.add(btnIniciar);
        barra.add(btnPausar);
        barra.add(btnStep);
        barra.add(new JSeparator(SwingConstants.VERTICAL));
        barra.add(new JLabel("Planificador:"));
        barra.add(comboAlgoritmo);
        barra.add(lblQuantum);
        barra.add(spinnerQuantum);
        barra.add(new JSeparator(SwingConstants.VERTICAL));
        barra.add(lblVelocidad);
        barra.add(spinnerVelocidad);
        barra.add(new JSeparator(SwingConstants.VERTICAL));
        barra.add(btnReporte);
        barra.add(btnReset);

        return barra;
    }

    // ─── 2. PANEL DE CPU Y MEMORIA RAM (32 MARCOS DE 4 KB) ──────────────

    private JPanel crearPanelCPUyRAM() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));

        // Subpanel Superior: Tarjeta de la CPU
        JPanel panelCPU = new JPanel(new GridLayout(2, 3, 10, 5));
        panelCPU.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(75, 110, 175), 2),
                " UNIDAD CENTRAL DE PROCESAMIENTO (CPU) & RELOJ GLOBAL ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("Segoe UI", Font.BOLD, 12)));

        lblTickGlobal = new JLabel("Reloj Global: Tick 0", SwingConstants.CENTER);
        lblTickGlobal.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblProcesoCPU = new JLabel("Estado CPU: OCIOSA", SwingConstants.CENTER);
        lblProcesoCPU.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblProcesoCPU.setForeground(new Color(110, 180, 255));

        lblProgramCounter = new JLabel("Program Counter (PC): 0", SwingConstants.CENTER);
        lblProgramCounter.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        lblBurstRestante = new JLabel("Ráfaga CPU: - / -", SwingConstants.CENTER);
        lblBurstRestante.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        lblDespachadorStats = new JLabel("Cambios de Contexto: 0 | Overhead: 0 ticks", SwingConstants.CENTER);
        lblDespachadorStats.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        panelCPU.add(lblTickGlobal);
        panelCPU.add(lblProcesoCPU);
        panelCPU.add(lblProgramCounter);
        panelCPU.add(lblBurstRestante);
        panelCPU.add(lblDespachadorStats);
        panelCPU.add(new JLabel("")); // Espaciador

        panel.add(panelCPU, BorderLayout.NORTH);

        // Subpanel Central: Grilla Visual de Marcos de RAM (Diapositiva 3)
        JPanel panelRAMContenedor = new JPanel(new BorderLayout(4, 4));
        panelRAMContenedor.setBorder(BorderFactory.createTitledBorder(" MEMORIA RAM FÍSICA (" + MemoriaRAM.TOTAL_MARCOS + " Marcos fijos de " + Pagina.TAMANO_MARCO_KB + " KB = " + MemoriaRAM.CAPACIDAD_TOTAL_KB + " KB Total) "));

        JPanel grillaRAM = new JPanel(new GridLayout(4, 8, 4, 4));
        grillaRAM.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        for (int i = 0; i < 32; i++) {
            JLabel celda = new JLabel("<html><center>M" + i + "<br/><small>Libre</small></center></html>", SwingConstants.CENTER);
            celda.setOpaque(true);
            celda.setBackground(new Color(45, 48, 52));
            celda.setForeground(new Color(170, 175, 180));
            celda.setBorder(BorderFactory.createLineBorder(new Color(65, 70, 75)));
            celda.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            celdasMarcosRAM[i] = celda;
            grillaRAM.add(celda);
        }

        lblRamStats = new JLabel("Marcos Libres: " + MemoriaRAM.TOTAL_MARCOS + "/" + MemoriaRAM.TOTAL_MARCOS + " | Ocupación: 0 KB / " + MemoriaRAM.CAPACIDAD_TOTAL_KB + " KB | Desperdicio (Fragmentación Interna): 0 KB", SwingConstants.CENTER);
        lblRamStats.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblRamStats.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));

        panelRAMContenedor.add(grillaRAM, BorderLayout.CENTER);
        panelRAMContenedor.add(lblRamStats, BorderLayout.SOUTH);

        panel.add(panelRAMContenedor, BorderLayout.CENTER);
        return panel;
    }

    // ─── 3. PANEL DE DISPOSITIVOS DE E/S & TECLADO INTERACTIVO ───────────

    private JPanel crearPanelDispositivosIO() {
        JPanel panel = new JPanel(new BorderLayout(6, 4));
        panel.setBorder(BorderFactory.createTitledBorder(" DISPOSITIVOS DE E/S & COLAS DE ESPERA (Diapositivas 10 y 11) "));

        JPanel gridColas = new JPanel(new GridLayout(2, 2, 8, 4));
        gridColas.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        lblColaDisco = new JLabel("💾 DISCO: [Vacía]");
        lblColaImpresora = new JLabel("🖨 IMPRESORA: [Vacía]");
        lblColaPantalla = new JLabel("🖥 PANTALLA: [Vacía]");
        lblColaTeclado = new JLabel("⌨ TECLADO: [Vacía]");

        gridColas.add(lblColaDisco);
        gridColas.add(lblColaImpresora);
        gridColas.add(lblColaPantalla);
        gridColas.add(lblColaTeclado);

        // Subpanel de Interacción de Teclado (Diapositiva 10 y 11, punto 9)
        panelAccionesTeclado = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        panelAccionesTeclado.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

        JLabel lblAvisoTeclado = new JLabel("Señal de Teclado:");
        btnTecladoContinuar = new JButton("✅ Continuar Proceso");
        btnTecladoContinuar.setEnabled(false);
        btnTecladoContinuar.addActionListener(e -> {
            if (pidEsperandoTeclado != null) {
                motor.getGestorDispositivos().responderTeclado(pidEsperandoTeclado, false);
                agregarLog("Interacción Teclado: Proceso PID=" + pidEsperandoTeclado + " continuado por el usuario.");
                actualizarVista();
            }
        });

        btnTecladoCancelar = new JButton("❌ Cancelar (Abortar)");
        btnTecladoCancelar.setEnabled(false);
        btnTecladoCancelar.addActionListener(e -> {
            if (pidEsperandoTeclado != null) {
                motor.getGestorDispositivos().responderTeclado(pidEsperandoTeclado, true);
                agregarLog("Interacción Teclado: Proceso PID=" + pidEsperandoTeclado + " cancelado por el usuario.");
                actualizarVista();
            }
        });

        panelAccionesTeclado.add(lblAvisoTeclado);
        panelAccionesTeclado.add(btnTecladoContinuar);
        panelAccionesTeclado.add(btnTecladoCancelar);

        panel.add(gridColas, BorderLayout.CENTER);
        panel.add(panelAccionesTeclado, BorderLayout.SOUTH);

        return panel;
    }

    // ─── 4. TABLAS DE PROCESOS (LISTOS, NUEVOS, FINALIZADOS) ─────────────

    private JComponent crearPanelTablasProcesos() {
        JTabbedPane tabs = new JTabbedPane();

        // 1. Cola de Listos
        modeloListos = new DefaultTableModel(new String[]{"PID", "Nombre", "Burst Rest.", "Prioridad", "Pág. PC"}, 0);
        JTable tablaListos = new JTable(modeloListos);
        tabs.addTab("Cola de Listos (Ready)", new JScrollPane(tablaListos));

        // 2. Cola de Nuevos
        modeloNuevos = new DefaultTableModel(new String[]{"PID", "Nombre", "Tamaño KB", "Burst Total"}, 0);
        JTable tablaNuevos = new JTable(modeloNuevos);
        tabs.addTab("Nuevos (Job Queue)", new JScrollPane(tablaNuevos));

        // 3. Finalizados
        modeloFinalizados = new DefaultTableModel(new String[]{"PID", "Nombre", "Retorno (TAT)", "Espera (WT)", "Resp. (RT)", "Estado/Error"}, 0);
        JTable tablaFinalizados = new JTable(modeloFinalizados);
        tabs.addTab("Finalizados", new JScrollPane(tablaFinalizados));

        return tabs;
    }

    // ─── 5. TERMINAL DE LOGS Y EVENTOS DEL SISTEMA ───────────────────────

    private JPanel crearPanelTerminalLogs() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createTitledBorder(" TERMINAL DE EVENTOS & GESTOR DE INTERRUPCIONES (En vivo) "));

        areaTerminalLogs = new JTextArea();
        areaTerminalLogs.setEditable(false);
        areaTerminalLogs.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaTerminalLogs.setBackground(new Color(24, 26, 28));
        areaTerminalLogs.setForeground(new Color(210, 215, 220));

        JScrollPane scroll = new JScrollPane(areaTerminalLogs);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ─── 6. LÓGICA DE CONTROL Y ACTUALIZACIÓN VISUAL ─────────────────────

    private void configurarTimer() {
        timerSimulacion = new Timer(velocidadMs, e -> {
            motor.step();
            actualizarVista();
            // Si ya no hay procesos en ejecución ni pendientes, pausar automáticamente
            if (!motor.getCpu().estaOcupada() && motor.getColaListos().isEmpty() && motor.getColaTotales().isEmpty()) {
                pausarSimulacion();
                agregarLog("--- Simulación completada: todos los procesos han finalizado ---");
            }
        });
    }

    private void iniciarSimulacion() {
        btnIniciar.setEnabled(false);
        btnPausar.setEnabled(true);
        btnStep.setEnabled(false);
        timerSimulacion.start();
    }

    private void pausarSimulacion() {
        if (timerSimulacion != null) {
            timerSimulacion.stop();
        }
        btnIniciar.setEnabled(true);
        btnPausar.setEnabled(false);
        btnStep.setEnabled(true);
    }

    private void cambiarPlanificadorCPU() {
        int index = comboAlgoritmo.getSelectedIndex();
        spinnerQuantum.setEnabled(index == 3);

        switch (index) {
            case 0 -> motor.setPlanificador(new FCFSStrategy());
            case 1 -> motor.setPlanificador(new SJFStrategy(false));
            case 2 -> motor.setPlanificador(new SJFStrategy(true));
            case 3 -> {
                int q = (int) spinnerQuantum.getValue();
                motor.setPlanificador(new RoundRobinStrategy(q));
            }
            case 4 -> motor.setPlanificador(new PrioridadStrategy(false));
            case 5 -> motor.setPlanificador(new PrioridadStrategy(true));
        }
        agregarLog("Planificador cambiado a: " + motor.getPlanificador().getNombre());
    }

    public void actualizarVista() {
        int tick = motor.getCpu().getTicksTotales();
        lblTickGlobal.setText("Reloj Global: Tick " + tick);

        // 1. Estado CPU
        if (motor.getCpu().estaOcupada()) {
            Proceso p = motor.getCpu().getProcesoActual();
            lblProcesoCPU.setText(String.format("CPU: %s (PID=%d)", p.getNombre(), p.getPid()));
            lblProcesoCPU.setForeground(new Color(90, 220, 130));
            lblProgramCounter.setText("Program Counter (PC): " + motor.getCpu().getProgramCounter());
            lblBurstRestante.setText(String.format("Ráfaga CPU: %d / %d ticks", p.getBurstRestante(), p.getBurstTotal()));
        } else {
            lblProcesoCPU.setText("Estado CPU: OCIOSA");
            lblProcesoCPU.setForeground(new Color(230, 160, 60));
            lblProgramCounter.setText("Program Counter (PC): 0");
            lblBurstRestante.setText("Ráfaga CPU: - / -");
        }

        if (motor.getDespachador() != null) {
            lblDespachadorStats.setText(String.format("Cambios de Contexto: %d | Overhead: %d ticks",
                    motor.getDespachador().getCambiosdeContextoTotales(),
                    motor.getDespachador().getTimeOverheadTotal()));
        }

        // 2. Grilla de Marcos de Memoria RAM
        List<MarcoRAM> marcos = motor.getRam().getMarcos();
        for (int i = 0; i < 32; i++) {
            MarcoRAM m = marcos.get(i);
            JLabel celda = celdasMarcosRAM[i];
            if (m.estaLibre()) {
                celda.setBackground(new Color(45, 48, 52));
                celda.setForeground(new Color(150, 155, 160));
                celda.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 70)));
                celda.setText("<html><center>M" + i + "<br/><small>Libre</small></center></html>");
            } else {
                celda.setBackground(new Color(30, 80, 140));
                celda.setForeground(Color.WHITE);
                celda.setBorder(BorderFactory.createLineBorder(new Color(80, 150, 230), 1));
                celda.setText(String.format("<html><center><b>M%d</b><br/>P%d (p%d)<br/><small>Desp:%dK</small></center></html>",
                        i, m.getPidProceso(), m.getNumeroPagina(), m.getDesperdicioKb()));
            }
        }

        int libres = motor.getRam().getMarcosLibres();
        int ocupados = MemoriaRAM.TOTAL_MARCOS - libres;
        int desperdicio = motor.getRam().calcularDesperdicioTotalKb();
        lblRamStats.setText(String.format("Marcos Libres: %d/%d | Ocupación: %d KB / %d KB | Fragmentación Interna: %d KB",
                libres, MemoriaRAM.TOTAL_MARCOS, ocupados * Pagina.TAMANO_MARCO_KB, MemoriaRAM.CAPACIDAD_TOTAL_KB, desperdicio));

        // 3. Colas de Dispositivos E/S
        actualizarColasDispositivos();

        // 4. Tablas de Procesos
        actualizarTablas();

        // 5. Nuevos Eventos en Terminal
        List<GestorInterrupciones.EventoInterrupcion> eventos = motor.getGestorInterrupciones().getHistorialEventos();
        while (ultimoEventoMostradoIdx < eventos.size()) {
            agregarLog(eventos.get(ultimoEventoMostradoIdx).toString());
            ultimoEventoMostradoIdx++;
        }
    }

    private void actualizarColasDispositivos() {
        Queue<Proceso> cDisco = motor.getGestorDispositivos().getCola(DispositivoIO.DISCO);
        Queue<Proceso> cImp = motor.getGestorDispositivos().getCola(DispositivoIO.IMPRESORA);
        Queue<Proceso> cPan = motor.getGestorDispositivos().getCola(DispositivoIO.PANTALLA);
        Queue<Proceso> cTec = motor.getGestorDispositivos().getCola(DispositivoIO.TECLADO);

        lblColaDisco.setText("💾 DISCO: " + formatearCola(cDisco));
        lblColaImpresora.setText("🖨 IMPRESORA: " + formatearCola(cImp));
        lblColaPantalla.setText("🖥 PANTALLA: " + formatearCola(cPan));
        lblColaTeclado.setText("⌨ TECLADO: " + formatearCola(cTec));

        // Gestión del Teclado Interactivo (Diapositiva 10 y 11)
        if (cTec != null && !cTec.isEmpty()) {
            Proceso p = cTec.peek();
            pidEsperandoTeclado = p.getPid();
            btnTecladoContinuar.setEnabled(true);
            btnTecladoCancelar.setEnabled(true);
            btnTecladoContinuar.setText("✅ Continuar " + p.getNombre());
            btnTecladoCancelar.setText("❌ Cancelar " + p.getNombre());
        } else {
            pidEsperandoTeclado = null;
            btnTecladoContinuar.setEnabled(false);
            btnTecladoCancelar.setEnabled(false);
            btnTecladoContinuar.setText("✅ Continuar Proceso");
            btnTecladoCancelar.setText("❌ Cancelar Proceso");
        }
    }

    private String formatearCola(Queue<Proceso> cola) {
        if (cola == null || cola.isEmpty()) return "[Vacía]";
        List<String> nombres = new ArrayList<>();
        cola.forEach(p -> nombres.add(p.getNombre()));
        return String.join(", ", nombres);
    }

    private void actualizarTablas() {
        // Listos
        modeloListos.setRowCount(0);
        for (Proceso p : motor.getColaListos()) {
            modeloListos.addRow(new Object[]{p.getPid(), p.getNombre(), p.getBurstRestante(), p.getPrioridad(), "Pág " + p.getPaginaPorProgramCounter()});
        }

        // Nuevos
        modeloNuevos.setRowCount(0);
        for (Proceso p : motor.getColaTotales()) {
            modeloNuevos.addRow(new Object[]{p.getPid(), p.getNombre(), p.getTamanoTotalKb() + " KB", p.getBurstTotal()});
        }

        // Finalizados
        modeloFinalizados.setRowCount(0);
        for (Proceso p : motor.getColaFinalizados()) {
            String estadoStr = (p.getCodigoError() != null) ? "ABORTADO: " + p.getCodigoError() : "COMPLETADO";
            modeloFinalizados.addRow(new Object[]{
                    p.getPid(), p.getNombre(),
                    p.getTiempoRetorno() + " ticks",
                    p.getTiempoEspera() + " ticks",
                    p.getTiempoRespuesta() + " ticks",
                    estadoStr
            });
        }
    }

    private void agregarLog(String mensaje) {
        areaTerminalLogs.append(mensaje + "\n");
        areaTerminalLogs.setCaretPosition(areaTerminalLogs.getDocument().getLength());
    }

    private void mostrarDialogoMetricas() {
        MetricasRendimiento m = new MetricasRendimiento(motor.getColaFinalizados(), motor.getCpu(), motor.getDespachador());
        String mensaje = String.format("""
                ============================================================
                         REPORTE CONSOLIDADO DE MÉTRICAS (Diapositiva 7)
                ============================================================
                • Total de Procesos Evaluados:      %d
                • Tiempo Medio de Espera (AWT):     %.2f ticks
                • Tiempo Medio de Retorno (ATAT):   %.2f ticks
                • Tiempo Medio de Respuesta (ART):  %.2f ticks
                • Utilización de CPU:               %.2f %%
                • Rendimiento (Throughput):         %.4f procesos/tick
                • Cambios de Contexto Totales:      %d
                • Sobrecarga Dispatcher Total:      %d ticks
                ============================================================""",
                m.getTotalProcesos(),
                m.getTiempoEsperaPromedio(),
                m.getTiempoRetornoPromedio(),
                m.getTiempoRespuestaPromedio(),
                m.getPorcentajeUtilCPU(),
                m.getRendimiento(),
                m.getTotalCambiosContexto(),
                m.getSobrecargaDispatcherTotal()
        );

        JTextArea area = new JTextArea(mensaje);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Métricas de Rendimiento", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void iniciar(MotorSO motor) {
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> {
            VentanaPrincipal ventana = new VentanaPrincipal(motor);
            ventana.setVisible(true);
        });
    }
}
