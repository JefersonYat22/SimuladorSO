package uni.edu.pe.core;

import uni.edu.pe.model.DispositivoIO;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.InterrupcionIO;
import uni.edu.pe.model.Proceso;

import java.util.*;

public class GestorDispositivos {

    // Device Queues independientes
    private final Map<DispositivoIO, Queue<Proceso>> colasDispositivos;
    private final Map<Integer, InterrupcionIO> interrupcionActivaPorPid;

    public GestorDispositivos() {
        this.colasDispositivos = new EnumMap<>(DispositivoIO.class);
        this.interrupcionActivaPorPid = new HashMap<>();

        for (DispositivoIO disp : DispositivoIO.values()) {
            this.colasDispositivos.put(disp, new LinkedList<>());
        }
    }

    public void bloquearProceso(Proceso proceso, InterrupcionIO interrupcion) {
        proceso.setEstado(EstadoProceso.BLOQUEADO);
        colasDispositivos.get(interrupcion.getDispositivo()).offer(proceso);
        interrupcionActivaPorPid.put(proceso.getPid(), interrupcion);
    }

    /**
     * Ticks de controladores de E/S:
     * Resta 1 tick a la duración del proceso al frente de cada cola automática.
     * Al llegar a 0, emite la señal de fin de E/S y pasa el proceso a LISTO.
     */
    public List<Proceso> actualizarTicks() {
        List<Proceso> completados = new ArrayList<>();

        for (Map.Entry<DispositivoIO, Queue<Proceso>> entry : colasDispositivos.entrySet()) {
            DispositivoIO dispositivo = entry.getKey();

            // El teclado interactivo no decrementa por ticks automáticos; espera al usuario
            if (dispositivo == DispositivoIO.TECLADO) {
                continue;
            }

            Queue<Proceso> cola = entry.getValue();
            if (!cola.isEmpty()) {
                Proceso procesoEnAtencion = cola.peek();
                InterrupcionIO io = interrupcionActivaPorPid.get(procesoEnAtencion.getPid());

                if (io != null && io.decrementarDuracion()) {
                    cola.poll();
                    interrupcionActivaPorPid.remove(procesoEnAtencion.getPid());
                    procesoEnAtencion.setEstado(EstadoProceso.LISTO);
                    completados.add(procesoEnAtencion);
                }
            }
        }
        return completados;

    }

    /**
     * Captura la señal interactiva del usuario para el dispositivo Teclado:
     * - Cancelación: Trap de aborto inmediato -> FINALIZADO con código de error.
     * - Continuación: Finaliza la espera de E/S -> LISTO para volver a la CPU.
     */
    public Proceso responderTeclado(int pid, boolean cancelar) {
        Queue<Proceso> colaTeclado = colasDispositivos.get(DispositivoIO.TECLADO);
        Proceso objetivo = null;

        for (Proceso p : colaTeclado) {
            if (p.getPid() == pid) {
                objetivo = p;
                break;
            }
        }

        if (objetivo != null) {
            colaTeclado.remove(objetivo);
            InterrupcionIO io = interrupcionActivaPorPid.remove(pid);
            if (io != null) {
                while (!io.isCompletada()) {
                    io.decrementarDuracion(); // Se da por satisfecha la E/S
                }
            }

            if (cancelar) {
                objetivo.setEstado(EstadoProceso.FINALIZADO);
                objetivo.setCodigoError("CANCELADO_POR_USUARIO_TECLADO");
            } else {
                objetivo.setEstado(EstadoProceso.LISTO);
            }
        }
        return objetivo;
    }

    public Queue<Proceso> getCola(DispositivoIO disp) {
        return colasDispositivos.get(disp);
    }

    public void reset() {
        colasDispositivos.values().forEach(Queue::clear);
        interrupcionActivaPorPid.clear();
    }
}
