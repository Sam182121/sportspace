package com.sportspace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String remitente;

    //  CORREO DE VERIFICACIÓN

    public void enviarCodigoVerificacion(String destinatario, String codigo) {
        String asunto = "SportSpace — Verifica tu correo electrónico";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:15px;color:#111928;font-weight:600;margin:0 0 8px;">Hola, gracias por registrarte :)</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 24px;">Tu código de verificación es:</p>
                    <div style="background:#f1f4f9;border-radius:8px;padding:18px;text-align:center;letter-spacing:8px;font-size:28px;font-weight:700;color:#1a56db;margin-bottom:24px;">
                      %s
                    </div>
                    <p style="font-size:12.5px;color:#9ca3af;margin:0;">
                      Este código es válido por <strong>15 minutos</strong>. Si no solicitaste este código, ignora este mensaje.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(codigo);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    // CORREO DE RECUPERACIÓN DE CONTRASEÑA

    public void enviarEnlaceRecuperacion(String destinatario, String enlace) {
        String asunto = "SportSpace — Restablece tu contraseña";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:15px;color:#111928;font-weight:600;margin:0 0 8px;">Restablece tu contraseña</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 24px;">
                      Recibimos una solicitud para restablecer la contraseña de tu cuenta.<br>
                      Haz clic en el siguiente botón para crear una nueva contraseña:
                    </p>
                    <div style="text-align:center;margin-bottom:28px;">
                      <a href="%s" style="display:inline-block;background:#1a56db;color:#fff;text-decoration:none;padding:13px 28px;border-radius:7px;font-size:14px;font-weight:600;">
                        Restablecer contraseña
                      </a>
                    </div>
                    <p style="font-size:12.5px;color:#9ca3af;margin:0 0 8px;">
                      Este enlace es válido por <strong>30 minutos</strong> y solo puede usarse una vez.
                    </p>
                    <p style="font-size:12.5px;color:#9ca3af;margin:0;">
                      Si no solicitaste restablecer tu contraseña, ignora este correo.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;">
                    <p style="font-size:11px;color:#9ca3af;margin:0 0 4px;">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                    <p style="font-size:11px;color:#1a56db;word-break:break-all;margin:0;">%s</p>
                  </div>
                  <div style="background:#f8f9fc;padding:10px 36px 14px;font-size:11px;color:#c0c7d4;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(enlace, enlace);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO DE SEGURIDAD TRAS CAMBIO DE CONTRASEÑA

    public void enviarCorreoSeguridad(String destinatario, String primerNombre, String enlaceBloqueo) {
        String asunto = "SportSpace — Se modificó la contraseña de tu cuenta";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:15px;color:#111928;font-weight:600;margin:0 0 16px;">
                      %s, se modificó la contraseña de acceso a tu cuenta.
                    </p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 20px;">
                      Si <strong>no fuiste tú</strong>, te recomendamos bloquear temporalmente tu cuenta para proteger tu información.
                    </p>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px 18px;margin-bottom:24px;">
                      <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#991b1b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                          <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                          <line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/>
                        </svg>
                        <p style="font-size:13px;color:#7f1d1d;margin:0;font-weight:600;">¿No reconoces este cambio?</p>
                      </div>
                      <p style="font-size:13px;color:#991b1b;margin:0;line-height:1.5;">
                        Bloquea tu cuenta ahora mismo para evitar accesos no autorizados.
                      </p>
                    </div>
                    <div style="text-align:center;margin-bottom:20px;">
                      <a href="%s" style="display:inline-block;background:#ef4444;color:#fff;text-decoration:none;padding:13px 28px;border-radius:7px;font-size:14px;font-weight:600;">
                        Bloquear mi cuenta
                      </a>
                    </div>
                    <p style="font-size:12px;color:#9ca3af;margin:0;line-height:1.5;">
                      Si <strong>fuiste tú</strong> quien cambió la contraseña, puedes ignorar este mensaje.
                      Este enlace es de un solo uso y expira en 24 horas.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(primerNombre, enlaceBloqueo);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO DE CONFIRMACIÓN DE BLOQUEO


    public void enviarCorreoBloqueoConfirmado(String destinatario, String primerNombre) {
        String asunto = "SportSpace — Notificación de seguridad: tu cuenta fue bloqueada";
        String cuerpo = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">

                  <!-- Cabecera azul -->
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>

                  <!-- Banda roja de alerta -->
                  <div style="background:#fef2f2;border-bottom:2px solid #fecaca;padding:12px 36px;">
                    <table cellpadding="0" cellspacing="0" border="0">
                      <tr>
                        <td style="padding-right:8px;vertical-align:middle;">
                          <span style="display:inline-block;width:20px;height:20px;background:#dc2626;border-radius:50%%;text-align:center;line-height:20px;color:#fff;font-size:12px;font-weight:700;">!</span>
                        </td>
                        <td style="vertical-align:middle;">
                          <span style="font-size:13px;font-weight:700;color:#991b1b;">Notificación de seguridad</span>
                        </td>
                      </tr>
                    </table>
                  </div>

                  <!-- Cuerpo -->
                  <div style="padding:32px 36px;">

                    <!-- Titulo -->
                    <p style="font-size:17px;color:#111928;font-weight:700;margin:0 0 8px;text-align:center;">
                      %s, tu cuenta fue bloqueada temporalmente.
                    </p>
                    <p style="font-size:13.5px;color:#6b7280;text-align:center;margin:0 0 28px;line-height:1.6;">
                      Tu cuenta ha sido bloqueada por seguridad a solicitud tuya.
                    </p>

                    <!-- Cuadro de restricciones -->
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:20px 22px;margin-bottom:20px;">
                      <p style="font-size:13px;font-weight:700;color:#374151;margin:0 0 14px;">
                        Mientras la cuenta esté bloqueada:
                      </p>

                      <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                        <tr>
                          <td style="vertical-align:top;padding-bottom:10px;width:24px;">
                            <span style="display:inline-block;width:18px;height:18px;background:#ef4444;border-radius:50%%;text-align:center;line-height:18px;color:#fff;font-size:11px;font-weight:700;">&times;</span>
                          </td>
                          <td style="padding-bottom:10px;padding-left:8px;vertical-align:top;">
                            <span style="font-size:13px;color:#4b5563;line-height:1.5;">No podrás iniciar sesión.</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="vertical-align:top;padding-bottom:10px;width:24px;">
                            <span style="display:inline-block;width:18px;height:18px;background:#ef4444;border-radius:50%%;text-align:center;line-height:18px;color:#fff;font-size:11px;font-weight:700;">&times;</span>
                          </td>
                          <td style="padding-bottom:10px;padding-left:8px;vertical-align:top;">
                            <span style="font-size:13px;color:#4b5563;line-height:1.5;">No podrás cambiar tu contraseña.</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="vertical-align:top;width:24px;">
                            <span style="display:inline-block;width:18px;height:18px;background:#ef4444;border-radius:50%%;text-align:center;line-height:18px;color:#fff;font-size:11px;font-weight:700;">&times;</span>
                          </td>
                          <td style="padding-left:8px;vertical-align:top;">
                            <span style="font-size:13px;color:#4b5563;line-height:1.5;">No podrás acceder al sistema.</span>
                          </td>
                        </tr>
                      </table>
                    </div>

                    <!-- Cuadro recuperar acceso -->
                    <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;padding:20px 22px;">
                      <table cellpadding="0" cellspacing="0" border="0" width="100%%">
                        <tr>
                          <td style="vertical-align:top;width:24px;">
                            <span style="display:inline-block;width:18px;height:18px;background:#16a34a;border-radius:50%%;text-align:center;line-height:18px;color:#fff;font-size:11px;font-weight:700;">?</span>
                          </td>
                          <td style="padding-left:8px;vertical-align:top;">
                            <span style="font-size:13px;font-weight:700;color:#15803d;">¿Necesitas recuperar el acceso?</span>
                          </td>
                        </tr>
                      </table>
                      <p style="font-size:13px;color:#166534;margin:10px 0 6px;line-height:1.6;">
                        Si fuiste tú quien bloqueó la cuenta, comunícate con nuestro soporte y te ayudaremos a reactivarla.
                      </p>
                      <p style="font-size:13px;color:#166534;margin:0;">
                        Eschíbenos a:
                        <a href="mailto:rondomnims9@gmail.com" style="color:#16a34a;font-weight:700;text-decoration:none;">
                          rondomnims9@gmail.com
                        </a>
                      </p>
                    </div>

                  </div>

                  <!-- Pie de página -->
                  <div style="background:#f8f9fc;padding:16px 36px;border-top:1px solid #e4e8f0;text-align:center;">
                    <p style="font-size:11px;color:#9ca3af;margin:0;">
                      SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                    </p>
                  </div>

                </div>
                """.formatted(primerNombre);
        enviarHtml(destinatario, asunto, cuerpo);
    }


    //  CORREO: RESERVA APROBADA POR EL PROPIETARIO

    public void enviarReservaAprobada(String destinatario, String nombreMostrar, String codigoReserva,
                                      String canchaNombre, String deporte, String ubicacion,
                                      String fechaFormateada, String horario, String montoTotal) {
        String asunto = "✅ Tu reserva ha sido APROBADA — SportSpace";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#f0fdf4;border-bottom:2px solid #bbf7d0;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#15803d;">✅ Reserva aprobada</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 22px;">
                      ¡Buenas noticias! El propietario aprobó tu solicitud de reserva. Aquí tienes los detalles:
                    </p>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:20px 22px;margin-bottom:22px;">
                      <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="font-size:13.5px;color:#374151;">
                        <tr><td style="padding:5px 0;color:#6b7280;">Código de reserva</td><td style="padding:5px 0;text-align:right;font-weight:700;color:#1a56db;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Cancha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Deporte</td><td style="padding:5px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Ubicación</td><td style="padding:5px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Fecha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Horario</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Total</td><td style="padding:5px 0;text-align:right;font-weight:700;color:#16a34a;">%s</td></tr>
                      </table>
                    </div>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 4px;">
                      Te esperamos. ¡Gracias por confiar en <strong>SportSpace</strong>! 🏆
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar, codigoReserva, canchaNombre, deporte, ubicacion, fechaFormateada, horario, montoTotal);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO: RESERVA RECHAZADA POR EL PROPIETARIO (estaba PENDIENTE)

    public void enviarReservaRechazada(String destinatario, String nombreMostrar, String codigoReserva,
                                       String canchaNombre, String ubicacion, String fechaFormateada,
                                       String horario, String motivo, String mensaje) {
        String asunto = "❌ Tu reserva ha sido RECHAZADA — SportSpace";
        String bloqueMensaje = (mensaje == null || mensaje.isBlank()) ? "" : """
                  <p style="font-size:13px;color:#7f1d1d;margin:6px 0 0;line-height:1.5;"><strong>Mensaje del propietario:</strong> %s</p>
                """.formatted(mensaje);
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#fef2f2;border-bottom:2px solid #fecaca;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#991b1b;">❌ Reserva rechazada</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 22px;">
                      Lamentamos informarte que tu solicitud de reserva fue <strong>rechazada</strong> por el propietario.
                    </p>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:20px 22px;margin-bottom:18px;">
                      <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="font-size:13.5px;color:#374151;">
                        <tr><td style="padding:5px 0;color:#6b7280;">Código de reserva</td><td style="padding:5px 0;text-align:right;font-weight:700;color:#1a56db;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Cancha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Ubicación</td><td style="padding:5px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Fecha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Horario</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                      </table>
                    </div>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px 18px;margin-bottom:22px;">
                      <p style="font-size:13px;color:#991b1b;margin:0;"><strong>Motivo:</strong> %s</p>
                      %s
                    </div>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 4px;">
                      Puedes realizar una nueva reserva en otra fecha u horario disponible.<br>
                      Gracias por utilizar <strong>SportSpace</strong>.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar, codigoReserva, canchaNombre, ubicacion, fechaFormateada, horario, motivo, bloqueMensaje);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO: RESERVA CANCELADA POR EL PROPIETARIO (ya estaba CONFIRMADA)

    public void enviarReservaCanceladaPorPropietario(String destinatario, String nombreMostrar,
                                                     String nombreCompletoPropietario, String codigoReserva,
                                                     String canchaNombre, String ubicacion, String fechaFormateada,
                                                     String horario, String motivo, String mensaje) {
        String asunto = "⚠️ Tu reserva ha sido cancelada por el propietario — SportSpace";
        String bloqueMensaje = (mensaje == null || mensaje.isBlank()) ? "" : """
                  <p style="font-size:13px;color:#7f1d1d;margin:6px 0 0;line-height:1.5;"><strong>Mensaje del propietario:</strong> %s</p>
                """.formatted(mensaje);
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#fef2f2;border-bottom:2px solid #fecaca;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#991b1b;">⚠️ Reserva cancelada por el propietario</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 22px;">
                      Lamentamos informarte que el propietario <strong>%s</strong> ha cancelado una reserva que
                      previamente había sido aprobada.
                    </p>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:20px 22px;margin-bottom:18px;">
                      <table cellpadding="0" cellspacing="0" border="0" width="100%%" style="font-size:13.5px;color:#374151;">
                        <tr><td style="padding:5px 0;color:#6b7280;">Código de reserva</td><td style="padding:5px 0;text-align:right;font-weight:700;color:#1a56db;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Cancha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Ubicación</td><td style="padding:5px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Fecha</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Horario</td><td style="padding:5px 0;text-align:right;font-weight:600;">%s</td></tr>
                        <tr><td style="padding:5px 0;color:#6b7280;">Estado</td><td style="padding:5px 0;text-align:right;font-weight:700;color:#991b1b;">Cancelada por el propietario</td></tr>
                      </table>
                    </div>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px 18px;margin-bottom:22px;">
                      <p style="font-size:13px;color:#991b1b;margin:0;"><strong>Motivo:</strong> %s</p>
                      %s
                    </div>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 14px;">
                      Te pedimos disculpas por los inconvenientes ocasionados. Puedes ingresar nuevamente a
                      SportSpace para reservar otra cancha u otro horario disponible.
                    </p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 14px;">
                      Si ya realizaste un pago, el propietario se pondrá en contacto contigo para coordinar
                      el proceso correspondiente.
                    </p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0;">
                      Gracias por tu comprensión.<br><strong>Equipo de SportSpace</strong>
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar, nombreCompletoPropietario, codigoReserva, canchaNombre, ubicacion,
                fechaFormateada, horario, motivo, bloqueMensaje);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO: CUENTA ELIMINADA / ANONIMIZADA POR EL ADMIN

    public void enviarCorreoEliminacionCuenta(String destinatario, String nombreMostrar,
                                              String motivoTexto, String comentario, boolean datosConservados) {
        String asunto = "Tu cuenta de SportSpace fue eliminada";
        String bloqueComentario = (comentario == null || comentario.isBlank()) ? "" : """
                  <p style="font-size:13px;color:#334155;margin:6px 0 0;line-height:1.5;"><strong>Comentario del administrador:</strong> %s</p>
                """.formatted(comentario);
        String notaHistorial = datosConservados ? """
                <p style="font-size:12.5px;color:#64748b;line-height:1.6;margin:0 0 14px;">
                  Por integridad de los datos de otras personas (reservas, pagos vinculados a tu cuenta),
                  tu información personal fue eliminada/anonimizada, pero cierto historial no identificable
                  se conserva en el sistema.
                </p>
                """ : "";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#f8fafc;border-bottom:2px solid #e2e8f0;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#334155;">🗑️ Cuenta eliminada</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 18px;">
                      Te confirmamos que tu cuenta en <strong>SportSpace</strong> fue eliminada por un administrador de la plataforma.
                    </p>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:14px 18px;margin-bottom:18px;">
                      <p style="font-size:13px;color:#334155;margin:0;"><strong>Motivo:</strong> %s</p>
                      %s
                    </div>
                    %s
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0;">
                      Si consideras que esto fue un error o tienes alguna consulta, escríbenos a
                      <a href="mailto:rondomnims9@gmail.com" style="color:#1a56db;font-weight:600;text-decoration:none;">rondomnims9@gmail.com</a>.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar, motivoTexto, bloqueComentario, notaHistorial);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO: CUENTA BLOQUEADA POR EL ADMIN

    public void enviarCorreoBloqueoCuenta(String destinatario, String nombreMostrar,
                                          String motivoTexto, String comentario) {
        String asunto = "🔒 Tu cuenta de SportSpace fue bloqueada";
        String bloqueComentario = (comentario == null || comentario.isBlank()) ? "" : """
                  <p style="font-size:13px;color:#334155;margin:6px 0 0;line-height:1.5;"><strong>Comentario del administrador:</strong> %s</p>
                """.formatted(comentario);
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#fef2f2;border-bottom:2px solid #fecaca;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#991b1b;">🔒 Cuenta bloqueada</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0 0 18px;">
                      El administrador de <strong>SportSpace</strong> bloqueó temporalmente tu cuenta. Mientras esté
                      bloqueada no podrás iniciar sesión, pero toda tu información (reservas, canchas, historial)
                      se mantiene intacta.
                    </p>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
                      <p style="font-size:13px;color:#991b1b;margin:0;"><strong>Motivo:</strong> %s</p>
                      %s
                    </div>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0;">
                      Si consideras que esto fue un error o quieres solicitar que se revise, escríbenos a
                      <a href="mailto:rondomnims9@gmail.com" style="color:#1a56db;font-weight:600;text-decoration:none;">rondomnims9@gmail.com</a>.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar, motivoTexto, bloqueComentario);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    //  CORREO: CUENTA DESBLOQUEADA POR EL ADMIN

    public void enviarCorreoDesbloqueoCuenta(String destinatario, String nombreMostrar) {
        String asunto = "✅ Tu cuenta de SportSpace fue reactivada";
        String cuerpo = """
                <div style="font-family:'DM Sans',Arial,sans-serif;max-width:520px;margin:0 auto;background:#fff;border:1px solid #e4e8f0;border-radius:12px;overflow:hidden;">
                  <div style="background:#1a56db;padding:32px 36px;">
                    <h1 style="color:#fff;font-size:20px;margin:0;font-weight:700;">SportSpace</h1>
                    <p style="color:rgba(255,255,255,0.8);font-size:13px;margin:6px 0 0;">Plataforma de alquiler de canchas deportivas</p>
                  </div>
                  <div style="background:#f0fdf4;border-bottom:2px solid #bbf7d0;padding:12px 36px;">
                    <span style="font-size:13px;font-weight:700;color:#15803d;">✅ Cuenta reactivada</span>
                  </div>
                  <div style="padding:32px 36px;">
                    <p style="font-size:16px;color:#111928;font-weight:700;margin:0 0 6px;">Hola, %s</p>
                    <p style="font-size:13.5px;color:#4b5563;line-height:1.6;margin:0;">
                      Tu cuenta en <strong>SportSpace</strong> fue reactivada. Ya puedes iniciar sesión con normalidad.
                    </p>
                  </div>
                  <div style="background:#f8f9fc;padding:14px 36px;border-top:1px solid #e4e8f0;font-size:11px;color:#9ca3af;text-align:center;">
                    SportSpace &copy; 2026 — Este correo fue enviado automáticamente, no respondas a este mensaje.
                  </div>
                </div>
                """.formatted(nombreMostrar);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    private void enviarHtml(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(mensaje);
            log.info("Correo enviado a: {} | Asunto: {}", destinatario, asunto);
        } catch (MessagingException e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
            throw new RuntimeException("No se pudo enviar el correo. Intenta de nuevo.");
        }
    }
}