package uni.edu.pe.hardware;

import uni.edu.pe.model.MarcoRAM;
import uni.edu.pe.model.Pagina;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class MemoriaRAM {

    public static final int TOTAL_MARCOS = 32;
    public static final int CAPACIDAD_TOTAL_KB = TOTAL_MARCOS * MarcoRAM.TAMANO_MARCO_KB;

    private final List<MarcoRAM> marcos;
    private final BitSet mapaDeBits;

    public MemoriaRAM() {
        this.marcos = new ArrayList<>(TOTAL_MARCOS);
        this.mapaDeBits = new BitSet(TOTAL_MARCOS);
        for (int i = 0; i < TOTAL_MARCOS; i++) {
            this.marcos.add(new MarcoRAM(i));
        }
    }

    public int getMarcosLibres() {
        return TOTAL_MARCOS - mapaDeBits.cardinality();
    }

    public int getPrimerMarcoLibre() {
        int indice = mapaDeBits.nextClearBit(0);
        return (indice < TOTAL_MARCOS) ? indice : -1;
    }

    public boolean cargarPagina(int marcoIdx, int pid, Pagina pagina) {
        if (marcoIdx >= 0 && marcoIdx < TOTAL_MARCOS && !mapaDeBits.get(marcoIdx)) {
            mapaDeBits.set(marcoIdx);
            marcos.get(marcoIdx).asignar(pid, pagina.getNumeroPagina(), pagina.getDesperdicioKb());
            pagina.cargarEnRAM(marcoIdx);
            return true;
        }
        return false;
    }

    public void liberarMarco(int marcoIdx) {
        if (marcoIdx >= 0 && marcoIdx < TOTAL_MARCOS) {
            mapaDeBits.clear(marcoIdx);
            marcos.get(marcoIdx).liberar();
        }
    }

    public int calcularDesperdicioTotalKb() {
        return marcos.stream()
                .filter(m -> !m.estaLibre())
                .mapToInt(MarcoRAM::getDesperdicioKb)
                .sum();
    }

    public List<MarcoRAM> getMarcos() { return marcos; }
    public BitSet getMapaDeBits() { return mapaDeBits; }

    public void reset() {
        mapaDeBits.clear();
        marcos.forEach(MarcoRAM::liberar);
    }
}
