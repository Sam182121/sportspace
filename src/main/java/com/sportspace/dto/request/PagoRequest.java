package com.sportspace.dto.request;

import com.sportspace.entity.Pago;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagoRequest {

    /**
     * ID de la reserva a la que pertenece el pago.
     * Debe existir y estar en estado PENDIENTE.
     */
    @NotNull(message = "El ID de la reserva es obligatorio")
    private Long reservaId;

    /**
     * Método de pago elegido por el cliente.
     * Valores aceptados: EFECTIVO, TRANSFERENCIA, YAPE, PLIN
     */
    @NotNull(message = "El método de pago es obligatorio")
    private Pago.MetodoPago metodo;

    /**
     * Comprobante de pago (voucher) en formato base64 (data URL).
     * Obligatorio para TRANSFERENCIA, YAPE y PLIN. No aplica para EFECTIVO.
     */
    private String voucherUrl;
}