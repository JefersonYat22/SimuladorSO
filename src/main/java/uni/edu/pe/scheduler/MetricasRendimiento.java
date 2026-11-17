package uni.edu.pe.scheduler;

import uni.edu.pe.hardware.CPU;
import uni.edu.pe.core.Despachador;
import uni.edu.pe.model.Proceso;

import java.util.List;

public class MetricasRendimiento {
    private final double tiempoEsperaPromedio;
    private final double tiempoRetornoPromedio;
    private final double tiempoRespuestaPromedio;
    private final double porcentajeUtilCPU;
    private final double Rendimiento;
    private final int totalProcesos;
    private final int totalCambiosContextos;
    private final int sobrecargaDispatcherTotal;

    public MetricasRendimiento(List<Proceso> procesosTerminados, CPU cpu, Despachador dispatcher) {
        this.totalProcesos = procesosTerminados.size();
        this.totalCambiosContextos = (dispatcher != null) ? dispatcher.getCambiosdeContextoTotales() : 0;
        this.sobrecargaDispatcherTotal = (dispatcher != null) ? dispatcher.getTimeOverheadTotal() : 0;

        if (totalProcesos == 0) {
            this.tiempoEsperaPromedio = 0.0;
            this.tiempoRetornoPromedio = 0.0;
            this.tiempoRespuestaPromedio = 0.0;
            this.Rendimiento = 0.0;
            this.porcentajeUtilCPU = 0.0;
            return;
        }

        long sumaET = 0;
        long sumaRTT = 0;
        long sumaRST = 0;
        long sumaBurstCPU = 0;

        for (Proceso p : procesosTerminados) {
            sumaET += p.getTiempoEspera();
            sumaRST += p.getTiempoRespuesta();
            sumaRTT += p.getTiempoRetorno();
            sumaBurstCPU += p.getBurstTotal();
        }

        this.tiempoEsperaPromedio = (double) sumaET / totalProcesos;
        this.tiempoRetornoPromedio = (double) sumaRTT / totalProcesos;
        this.tiempoRespuestaPromedio = (double) sumaRST / totalProcesos;

        int ticksTotales = (cpu != null) ? cpu.getTicksTotales() : 1;
        this.Rendimiento = (ticksTotales > 0) ? ((double) totalProcesos / ticksTotales) : 0.0;
        this.porcentajeUtilCPU = (ticksTotales > 0) ? ((double) sumaBurstCPU / ticksTotales) * 100.0 : 0.0;
    }

    public double getTiempoEsperaPromedio() {
        return tiempoEsperaPromedio;
    }

    public double getTiempoRetornoPromedio() {
        return tiempoRetornoPromedio;
    }

    public double getTiempoRespuestaPromedio() {
        return tiempoRespuestaPromedio;
    }

    public double getPorcentajeUtilCPU() {
        return porcentajeUtilCPU;
    }

    public double getRendimiento() {
        return Rendimiento;
    }

    public int getTotalProcesos() {
        return totalProcesos;
    }

    public int getTotalCambiosContexto() {
        return totalCambiosContextos;
    }

    public int getSobrecargaDispatcherTotal() {
        return sobrecargaDispatcherTotal;
    }
}
