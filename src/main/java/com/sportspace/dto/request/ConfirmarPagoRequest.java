package com.sportspace.dto.request;

import lombok.Data;

/**
 * Body opcional para confirmar, rechazar o reembolsar manualmente un pago.
 */
@Data
public class ConfirmarPagoRequest {

    /** Notas/mensaje corto del propietario (motivo de rechazo o nota del reembolso). */
    private String notas;

    /**
     * Comprobante (voucher) de la devolución en base64 (data URL).
     * Solo se usa en el endpoint de reembolso.
     */
    private String voucherUrl;
}