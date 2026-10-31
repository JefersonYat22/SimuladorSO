package uni.edu.pe.model;

/**
 * Representa los 5 estados en los que puede encontrarse un proceso en el SO:
 * 1. NUEVO: Proceso recién creado en cola de totales, aún no admitido a memoria.
 * 2. LISTO: Proceso cargado y esperando que la CPU le sea asignada.
 * 3. EJECUTANDO: Proceso actualmente asignado a la CPU ejecutando sus ráfagas.
 * 4. BLOQUEADO: Proceso esperando que termine una operación de E/S o fallo de página.
 * 5. FINALIZADO: Proceso que completó todo su burst o fue cancelado por error.
 */
public enum EstadoProceso {
    NUEVO,
    LISTO,
    EJECUTANDO,
    BLOQUEADO,
    FINALIZADO
}

