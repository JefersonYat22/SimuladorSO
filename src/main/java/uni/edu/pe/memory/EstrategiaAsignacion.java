package uni.edu.pe.memory;

import uni.edu.pe.hardware.MemoriaRAM;
import uni.edu.pe.model.Pagina;
import java.util.List;

public interface EstrategiaAsignacion {
    List<Integer> asignar(List<Pagina> paginas, MemoriaRAM ram, int pid);

    String getNombre();
}

