package uni.edu.pe.core;

import uni.edu.pe.hardware.CPU;
import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.memory.EstrategiaAsignacion;
import uni.edu.pe.memory.MMU;
import uni.edu.pe.memory.PrimerAjusteStrategy;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.InterrupcionIO;
import uni.edu.pe.model.Proceso;
import uni.edu.pe.scheduler.FCFSStrategy;
import uni.edu.pe.scheduler.PlanificadorCPU;
import uni.edu.pe.scheduler.RoundRobinStrategy;

import java.util.*;

/**
 * Motor Principal del Sistema Operativo (El Núcleo Monolítico).
 * Orquesta todos los subsistemas: CPU, RAM, MMU, Planificadores, Colas e Interrupciones.
 */
public class MotorSO {

    // Componentes de Hardware y Memoria
    private final CPU cpu;
    private final MemoriaRAM ram;
    private final MMU mmu;

    // Colas Principales del Sistema
    private final Queue<Proceso> colaTotales;      // Procesos Nuevos
    private final Queue<Proceso> colaListos;       // Esperando CPU
    private final List<Proceso> colaFinalizados;   // Terminados

    // Módulos del Núcleo
    private final Despachador despachador;
    private final GestorDispositivos gestorDispositivos;
    private final GestorInterrupciones gestorInterrupciones;

    // Estrategias intercambiables (Patrón Strategy)
    private PlanificadorCPU planificador;
    private EstrategiaAsignacion estrategiaAsignacion;

    public MotorSO() {
        this.cpu = new CPU();
        this.ram = new MemoriaRAM();
        this.mmu = new MMU();

        this.colaTotales = new LinkedList<>();
        this.colaListos = new LinkedList<>();
        this.colaFinalizados = new ArrayList<>();

        this.despachador = new Despachador();
        this.gestorDispositivos = new GestorDispositivos();
        this.gestorInterrupciones = new GestorInterrupciones();

        this.planificador = new FCFSStrategy();
        this.estrategiaAsignacion = new PrimerAjusteStrategy();
    }

    /**
     * Agrega un nuevo proceso al sistema (Cola de Totales/Nuevos).
     */
    public void agregarProceso(Proceso p) {
        if (p.isEsErrado()) {
            // Proceso defectuoso (0.5%): se cancela directamente
            p.setEstado(EstadoProceso.FINALIZADO);
            colaFinalizados.add(p);
            gestorInterrupciones.registrar(cpu.getTicksTotales(),
                    GestorInterrupciones.TipoInterrupcion.ERROR_ABORT, p.getPid(), "Proceso defectuoso cancelado.");
        } else {
            p.setEstado(EstadoProceso.NUEVO);
            colaTotales.offer(p);
        }
    }

    /**
     * Genera automáticamente 20 procesos para la prueba del profesor (Diapositiva 7, punto 17).
     */
    public void generar20ProcesosAleatorios() {
        Random rnd = new Random();
        for (int i = 0; i < 20; i++) {
            int tamanoKb = 32 + rnd.nextInt(300); // 32 KB a 332 KB
            int burst = 10 + rnd.nextInt(41);      // 10 a 50 ticks
            agregarProceso(new Proceso(tamanoKb, burst));
        }
    }

    /**
     * Ejecuta UN SOLO TICK de reloj del Sistema Operativo.
     * Es el corazón del avance paso a paso.
     */
    public void step() {
        int tickActual = cpu.getTicksTotales() + 1;

        // 1. Planificador de Largo Plazo: Admitir procesos de Nuevos a Listos cargando memoria
        admitirProcesosNuevos();

        // 2. Actualizar dispositivos de E/S
        List<Proceso> ioCompletados = gestorDispositivos.actualizarTicks();
        for (Proceso p : ioCompletados) {
            colaListos.offer(p);
            gestorInterrupciones.registrar(tickActual,
                    GestorInterrupciones.TipoInterrupcion.IO_COMPLETE, p.getPid(), "E/S finalizada. Vuelve a Listos.");
        }

        // 3. Si no hay proceso en CPU, despachar uno de la cola de Listos
        if (!cpu.estaOcupada() && !colaListos.isEmpty()) {
            Proceso seleccionado = planificador.seleccionarSiguiente(colaListos);
            despachador.cambiarContexto(cpu, seleccionado);
        }

        // 4. Ejecutar 1 instrucción en la CPU
        if (cpu.estaOcupada()) {
            Proceso actual = cpu.getProcesoActual();

            // Verificar si salta interrupción de E/S programada en esta instrucción
            InterrupcionIO io = actual.verificarInterrupcionPendiente();
            if (io != null) {
                despachador.cambiarContexto(cpu, null);
                gestorDispositivos.bloquearProceso(actual, io);
                gestorInterrupciones.registrar(tickActual,
                        GestorInterrupciones.TipoInterrupcion.IO_REQUEST, actual.getPid(),
                        "Interrupción solicitando " + io.getDispositivo() + " por " + io.getDuracionTotal() + " ticks.");
                return;
            }

            // Ejecutar tick
            boolean terminado = cpu.tick();

            if (terminado) {
                actual.setEstado(EstadoProceso.FINALIZADO);
                actual.setTiempoFinalizacion(cpu.getTicksTotales());
                colaFinalizados.add(actual);
                despachador.cambiarContexto(cpu, null);

                // Liberar marcos de RAM
                actual.getPaginas().forEach(pag -> {
                    if (pag.getMarcoAsignado() != null) {
                        ram.liberarMarco(pag.getMarcoAsignado());
                        pag.descargarDeRAM();
                    }
                });
            } else if (planificador instanceof RoundRobinStrategy rr) {
                // Verificar quantum para Round Robin
                if (rr.decrementarYVerificarExpiracion()) {
                    gestorInterrupciones.registrar(tickActual,
                            GestorInterrupciones.TipoInterrupcion.TIMER, actual.getPid(), "Quantum expirado.");
                    despachador.cambiarContexto(cpu, null);
                    colaListos.offer(actual);
                }
            }
        } else {
            // CPU ociosa (idle tick)
            cpu.tick();
        }
    }

    private void admitirProcesosNuevos() {
        while (!colaTotales.isEmpty()) {
            Proceso candidato = colaTotales.peek();
            if (ram.getMarcosLibres() >= candidato.getPaginas().size()) {
                colaTotales.poll();
                estrategiaAsignacion.asignar(candidato.getPaginas(), ram, candidato.getPid());
                candidato.setEstado(EstadoProceso.LISTO);
                candidato.setTiempoLlegada(cpu.getTicksTotales());
                colaListos.offer(candidato);
            } else {
                break; // No hay suficiente RAM disponible
            }
        }
    }

    // Getters y Configuraciones
    public CPU getCpu() { return cpu; }
    public MemoriaRAM getRam() { return ram; }
    public MMU getMmu() { return mmu; }
    public Queue<Proceso> getColaTotales() { return colaTotales; }
    public Queue<Proceso> getColaListos() { return colaListos; }
    public List<Proceso> getColaFinalizados() { return colaFinalizados; }
    public GestorDispositivos getGestorDispositivos() { return gestorDispositivos; }
    public GestorInterrupciones getGestorInterrupciones() { return gestorInterrupciones; }
    public PlanificadorCPU getPlanificador() { return planificador; }
    public void setPlanificador(PlanificadorCPU planificador) { this.planificador = planificador; }
    public EstrategiaAsignacion getEstrategiaAsignacion() { return estrategiaAsignacion; }
    public void setEstrategiaAsignacion(EstrategiaAsignacion estrategia) { this.estrategiaAsignacion = estrategia; }

    public void reset() {
        cpu.reset();
        ram.reset();
        colaTotales.clear();
        colaListos.clear();
        colaFinalizados.clear();
        gestorDispositivos.reset();
        gestorInterrupciones.reset();
        Proceso.reiniciarContadorPid();
    }
}

