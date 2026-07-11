package com.sportspace.scheduler;

import com.sportspace.entity.EstadoReserva;
import com.sportspace.entity.Reserva;
import com.sportspace.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Marca automáticamente como COMPLETADA cualquier reserva CONFIRMADA cuya
 * fecha + hora de fin ya pasó. Esto es lo que alimenta "Completadas" y
 * "Total Gastado" en el dashboard/reservas del cliente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaCompletadaScheduler {

    private final ReservaRepository reservaRepo;

    /** Corre cada 5 minutos. */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void marcarReservasCompletadas() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Reserva> confirmadas = reservaRepo.findByEstado(EstadoReserva.CONFIRMADA);
        int actualizadas = 0;

        for (Reserva r : confirmadas) {
            LocalDateTime finReserva = LocalDateTime.of(r.getFecha(), r.getHoraFin());
            if (finReserva.isBefore(ahora)) {
                r.setEstado(EstadoReserva.COMPLETADA);
                reservaRepo.save(r);
                actualizadas++;
            }
        }

        if (actualizadas > 0)
            log.info("ReservaCompletadaScheduler: {} reserva(s) marcada(s) como COMPLETADA", actualizadas);
    }
}