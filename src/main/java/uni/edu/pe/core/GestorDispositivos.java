package uni.edu.pe.core;

import uni.edu.pe.model.DispositivoIO;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.InterrupcionIO;
import uni.edu.pe.model.Proceso;

import java.util.*;

/**
 * Administra las colas de atención de los dispositivos de Entrada/Salida (Diapositivas 4 y 10).
 * Maneja las colas independientes para: Teclado, Disco, Impresora y Pantalla.
 */
public class GestorDispositivos {

    // Cada dispositivo tiene su propia cola de procesos esperando
    private final Map<DispositivoIO, Queue<Proceso>> colasDispositivos;
    // Mapa auxiliar para saber qué interrupción está atendiendo cada proceso
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
     * Avanza el tiempo de E/S en 1 tick para los procesos que están siendo atendidos.
     * Retorna la lista de procesos que completaron su E/S en este tick.
     */
    public List<Proceso> actualizarTicks() {
        List<Proceso> procesosCompletados = new ArrayList<>();

        for (Map.Entry<DispositivoIO, Queue<Proceso>> entry : colasDispositivos.entrySet()) {
            Queue<Proceso> cola = entry.getValue();
            if (!cola.isEmpty()) {
                Proceso procesoEnAtencion = cola.peek();
                InterrupcionIO io = interrupcionActivaPorPid.get(procesoEnAtencion.getPid());

                if (io != null && io.decrementarDuracion()) {
                    // Terminó su requerimiento de E/S
                    cola.poll();
                    interrupcionActivaPorPid.remove(procesoEnAtencion.getPid());
                    procesoEnAtencion.setEstado(EstadoProceso.LISTO);
                    procesosCompletados.add(procesoEnAtencion);
                }
            }
        }

        return procesosCompletados;
    }

    /**
     * Requerimiento especial para el Teclado (Diapositiva 10, punto 9):
     * "Para el dispositivo Teclado, capturar la señal que llega y actuar (Cancelación o Continuación)".
     */
    public boolean resolverInterrupcionTeclado(int pid, boolean cancelar) {
        Queue<Proceso> colaTeclado = colasDispositivos.get(DispositivoIO.TECLADO);
        for (Proceso p : colaTeclado) {
            if (p.getPid() == pid) {
                colaTeclado.remove(p);
                interrupcionActivaPorPid.remove(pid);
                if (cancelar) {
                    p.setEstado(EstadoProceso.FINALIZADO);
                    p.setCodigoError("CANCELADO_POR_USUARIO_TECLADO");
                } else {
                    p.setEstado(EstadoProceso.LISTO);
                }
                return true;
            }
        }
        return false;
    }

    public Queue<Proceso> getCola(DispositivoIO disp) {
        return colasDispositivos.get(disp);
    }

    public void reset() {
        colasDispositivos.values().forEach(Queue::clear);
        interrupcionActivaPorPid.clear();
    }
}

