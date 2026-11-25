package uni.edu.pe.core;

import uni.edu.pe.hardware.CPU;
import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.memory.EstrategiaAsignacion;
import uni.edu.pe.memory.MMU;
import uni.edu.pe.memory.PrimerAjusteStrategy;
import uni.edu.pe.memory.MejorAjusteStrategy;
import uni.edu.pe.memory.PeorAjusteStrategy;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.InterrupcionIO;
import uni.edu.pe.model.Proceso;
import uni.edu.pe.scheduler.FCFSStrategy;
import uni.edu.pe.scheduler.PlanificadorCPU;
import uni.edu.pe.scheduler.RoundRobinStrategy;

import java.util.*;

/**
 * Motor Principal del Sistema Operativo (El Núcleo Monolítico).
 * Orquesta todos los subsistemas: CPU, RAM, MMU, Planificadores, Colas e
 * Interrupciones.
 */
public class MotorSO {

    // Componentes de Hardware y Memoria
    private final CPU cpu;
    private final MemoriaRAM ram;
    private final MMU mmu;

    // Colas Principales del Sistema
    private final Queue<Proceso> colaTotales; // Procesos Nuevos
    private final Queue<Proceso> colaListos; // Esperando CPU
    private final List<Proceso> colaFinalizados; // Terminados

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
     * Ejecuta UN SOLO TICK de reloj del Sistema Operativo.
     * Es el corazón del avance paso a paso.
     */
    public void step() {
        int tickActual = cpu.getTicksTotales() + 1;

        // 1. Planificador de Largo Plazo: Job Queue (Nuevos) -> Ready Queue (Listos)
        admitirProcesosNuevos();

        // 2. Controladores de E/S: Actualizar Device Queues y mover a LISTO al terminar
        List<Proceso> ioTerminados = gestorDispositivos.actualizarTicks();
        for (Proceso p : ioTerminados) {
            colaListos.offer(p);
            gestorInterrupciones.registrar(tickActual,
                    GestorInterrupciones.TipoInterrupcion.IO_COMPLETE,
                    p.getPid(),
                    "Fin de E/S. Retorna a Cola de Listos.");
        }

        // 3. Despachador / Planificador de Corto Plazo: Asignar proceso a CPU si está
        // libre
        if (!cpu.estaOcupada() && !colaListos.isEmpty()) {
            Proceso seleccionado = planificador.seleccionarSiguiente(colaListos);

            // Detección de Trap en el primer contacto con CPU
            if (seleccionado.isEsErrado()) {
                ejecutarTrapError(seleccionado, tickActual);
                return;
            }

            despachador.cambiarContexto(cpu, seleccionado);
        }

        // 4. Ejecución en CPU
        if (cpu.estaOcupada()) {
            Proceso actual = cpu.getProcesoActual();

            // 4.1 Verificar interrupción de E/S programada
            InterrupcionIO io = actual.verificarInterrupcionPendiente();
            if (io != null) {
                despachador.cambiarContexto(cpu, null);
                gestorDispositivos.bloquearProceso(actual, io);
                gestorInterrupciones.registrar(tickActual,
                        GestorInterrupciones.TipoInterrupcion.IO_REQUEST,
                        actual.getPid(),
                        "Solicitud a " + io.getDispositivo() + " (" + io.getDuracionTotal() + " ticks).");
                return;
            }

            // 4.2 Ejecutar 1 ciclo de reloj (Tick) en la CPU
            boolean terminado = cpu.tick();

            if (terminado) {
                // Proceso completó exitosamente su burst
                actual.setEstado(EstadoProceso.FINALIZADO);
                actual.setTiempoFinalizacion(cpu.getTicksTotales());
                colaFinalizados.add(actual);
                despachador.cambiarContexto(cpu, null);

                // Liberación de marcos de RAM
                actual.getPaginas().forEach(pag -> {
                    if (pag.getMarcoAsignado() != null) {
                        ram.liberarMarco(pag.getMarcoAsignado());
                        pag.descargarDeRAM();
                    }
                });
            } else if (planificador instanceof RoundRobinStrategy rr) {
                // 4.3 Quantum Expirado (Interrupción por Timer)
                if (rr.decrementarYVerificarExpiracion()) {
                    gestorInterrupciones.registrar(tickActual,
                            GestorInterrupciones.TipoInterrupcion.TIMER,
                            actual.getPid(),
                            "Quantum agotado. Desalojo de CPU.");
                    despachador.cambiarContexto(cpu, null);
                    colaListos.offer(actual);
                }
            }
        } else {
            // CPU Ociosa
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
    public CPU getCpu() {
        return cpu;
    }

    public MemoriaRAM getRam() {
        return ram;
    }

    public MMU getMmu() {
        return mmu;
    }

    public Queue<Proceso> getColaTotales() {
        return colaTotales;
    }

    public Queue<Proceso> getColaListos() {
        return colaListos;
    }

    public List<Proceso> getColaFinalizados() {
        return colaFinalizados;
    }

    public GestorDispositivos getGestorDispositivos() {
        return gestorDispositivos;
    }

    public GestorInterrupciones getGestorInterrupciones() {
        return gestorInterrupciones;
    }

    public Despachador getDespachador() {
        return despachador;
    }

    public PlanificadorCPU getPlanificador() {
        return planificador;
    }

    public void setPlanificador(PlanificadorCPU planificador) {
        this.planificador = planificador;
    }

    public EstrategiaAsignacion getEstrategiaAsignacion() {
        return estrategiaAsignacion;
    }

    public void setEstrategiaAsignacion(EstrategiaAsignacion estrategia) {
        this.estrategiaAsignacion = estrategia;
    }

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

    private void ejecutarTrapError(Proceso p, int tickActual) {
        p.setEstado(EstadoProceso.FINALIZADO);
        p.setTiempoFinalizacion(tickActual);
        if (p.getCodigoError() == null) {
            p.setCodigoError("TRAP_0.5%_PROCESO_DEFECTUOSO");
        }

        // 1. Desalojar de CPU si estaba en ejecución
        if (cpu.getProcesoActual() == p) {
            despachador.cambiarContexto(cpu, null);
        }

        // 2. Liberar memoria física (marcos de RAM)
        p.getPaginas().forEach(pag -> {
            if (pag.getMarcoAsignado() != null) {
                ram.liberarMarco(pag.getMarcoAsignado());
                pag.descargarDeRAM();
            }
        });

        // 3. Registrar interrupción tipo Trap
        gestorInterrupciones.registrar(tickActual,
                GestorInterrupciones.TipoInterrupcion.ERROR_ABORT,
                p.getPid(),
                "Trap emitido: " + p.getCodigoError() + " | Recursos liberados.");

        colaFinalizados.add(p);
    }
}
