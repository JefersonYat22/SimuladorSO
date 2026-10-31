package uni.edu.pe.model;

/**
 * Representa una solicitud de E/S programada para un proceso.
 * El profesor exige:
 * - Duración aleatoria entre 5 y 20 unidades de tiempo.
 * - Momento de disparo aleatorio durante el ciclo de vida del proceso.
 */
public class InterrupcionIO {

    private final DispositivoIO dispositivo;
    private final int tickDisparo;       // Momento (instrucción ejecutada del proceso) en que salta
    private final int duracionTotal;     // Ticks que tardará en la cola de E/S (5 a 20)
    private int duracionRestante;
    private boolean completada;

    public InterrupcionIO(DispositivoIO dispositivo, int tickDisparo, int duracionTotal) {
        this.dispositivo = dispositivo;
        this.tickDisparo = tickDisparo;
        this.duracionTotal = duracionTotal;
        this.duracionRestante = duracionTotal;
        this.completada = false;
    }

    public boolean decrementarDuracion() {
        if (duracionRestante > 0) {
            duracionRestante--;
        }
        if (duracionRestante == 0) {
            completada = true;
        }
        return completada;
    }

    public DispositivoIO getDispositivo() {
        return dispositivo;
    }

    public int getTickDisparo() {
        return tickDisparo;
    }

    public int getDuracionTotal() {
        return duracionTotal;
    }

    public int getDuracionRestante() {
        return duracionRestante;
    }

    public boolean isCompletada() {
        return completada;
    }

    @Override
    public String toString() {
        return "E/S[" + dispositivo + ", disparo en t=" + tickDisparo + ", duracion=" + duracionTotal + "]";
    }
}

