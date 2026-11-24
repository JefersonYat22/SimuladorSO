package uni.edu.pe.scheduler;

import uni.edu.pe.model.Proceso;
import java.util.Queue;

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
        if (this.quantumRestante > this.quantum) {
            this.quantumRestante = this.quantum;
        }
    }

    public int getQuantum() {
        return quantum;
    }

    public int getQuantumRestante() {
        return quantumRestante;
    }

    @Override
    public boolean esApropiativo() {
        return true;
    }

    @Override
    public String getNombre() {
        return "Round Robin (Quantum = " + quantum + ")";
    }
}
