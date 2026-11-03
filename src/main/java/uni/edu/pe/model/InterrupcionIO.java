package uni.edu.pe.model;

public class InterrupcionIO {

    private final DispositivoIO dispositivo;
    private final int tickDisparo;
    private final int duracionTotal;
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
        return "E/S[" + dispositivo + ", disparo=" + tickDisparo + ", duracion=" + duracionTotal + "]";
    }
}
