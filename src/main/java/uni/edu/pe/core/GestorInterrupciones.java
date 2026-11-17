package uni.edu.pe.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Módulo encargado de administrar señales e interrupciones (Diapositivas 2 y 4).
 * Maneja eventos de Timer, Requerimientos de E/S, Finalizaciones de E/S y Fallos de Página.
 */
public class GestorInterrupciones {

    public enum TipoInterrupcion {
        TIMER,
        IO_REQUEST,
        IO_COMPLETE,
        PAGE_FAULT,
        ERROR_ABORT
    }

    public record EventoInterrupcion(int tick, TipoInterrupcion tipo, int pid, String detalle) {
        @Override
        public String toString() {
            return String.format("[Tick %03d] %-12s | PID=%d: %s", tick, tipo, pid, detalle);
        }
    }

    private final List<EventoInterrupcion> historialEventos;

    public GestorInterrupciones() {
        this.historialEventos = new ArrayList<>();
    }

    public void registrar(int tick, TipoInterrupcion tipo, int pid, String detalle) {
        historialEventos.add(new EventoInterrupcion(tick, tipo, pid, detalle));
    }

    public List<EventoInterrupcion> getHistorialEventos() {
        return historialEventos;
    }

    public void reset() {
        historialEventos.clear();
    }
}

