package com.sportspace.dto.response;

import com.sportspace.entity.Pago;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {

    // Pago
    private Long              id;
    private BigDecimal        monto;
    private Pago.MetodoPago   metodo;
    private Pago.EstadoPago   estado;
    private LocalDateTime     fechaPago;

    /** Motivo de rechazo o mensaje del propietario al procesar el reembolso. */
    private String            notas;
    /** Comprobante subido por el cliente al pagar. */
    private String            voucherUrl;
    /** Comprobante de la devolución subido por el propietario (solo si fue reembolsado). */
    private String            voucherReembolsoUrl;

    // Reserva asociada
    private Long              reservaId;
    private String            reservaEstado;
    private LocalDate         reservaFecha;
    private LocalTime         reservaHoraInicio;
    private LocalTime         reservaHoraFin;

    // Cancha
    private Long              canchaId;
    private String            canchaNombre;
    private String            canchaDeporte;
    private String            canchaDistrito;

    // Cliente
    private Long              clienteId;
    private String            clienteNombre;
    private String            clienteEmail;
}