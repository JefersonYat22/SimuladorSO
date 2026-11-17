package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Queue;

/**
 * Round Robin (RR): Asigna un quantum de tiempo fijo a cada proceso.
 * El quantum puede ser modificado en tiempo de ejecución (Diapositiva 5, punto 7).
 */
public class RoundRobinStrategy implements PlanificadorCPU {

    private int quantum;
    private int quantumRestante;

    public RoundRobinStrategy(int quantumInicial) {
        this.quantum = Math.max(1, quantumInicial);
        this.quantumRestante = this.quantum;
    }

    @Override
    public Proceso seleccionarSiguiente(Queue<Proceso> colaListos) {
        resetearQuantum();
        return colaListos.poll();
    }

    public boolean decrementarYVerificarExpiracion() {
        quantumRestante--;
        return quantumRestante <= 0;
    }

    public void resetearQuantum() {
        this.quantumRestante = this.quantum;
    }

    public void setQuantum(int nuevoQuantum) {
        this.quantum = Math.max(1, nuevoQuantum);
    }

    public int getQuantum() { return quantum; }
    public int getQuantumRestante() { return quantumRestante; }

    @Override
    public boolean esApropiativo() {
        return true;
    }

    @Override
    public String getNombre() {
        return "Round Robin (Quantum = " + quantum + ")";
    }
}

