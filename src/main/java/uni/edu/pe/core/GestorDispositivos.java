package uni.edu.pe.core;

import uni.edu.pe.model.DispositivoIO;
import uni.edu.pe.model.EstadoProceso;
import uni.edu.pe.model.InterrupcionIO;
import uni.edu.pe.model.Proceso;

import java.util.*;

public class GestorDispositivos {
   
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

