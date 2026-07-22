'use strict';

let todasReservas = [];
let reservaAccion = null;
let filtroEstado  = '';

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Reservas');
    await cargarCanchasFilter();
    await cargarReservas();
    document.getElementById('btnConfirmRechazar').addEventListener('click', confirmarRechazar);
    document.getElementById('btnConfirmCancelarConfirmada').addEventListener('click', confirmarCancelarConfirmada);
});

/* CARGAR FILTRO CANCHAS */
async function cargarCanchasFilter() {
    try {
        const list = await api.get('/propietario/canchas');
        const sel = document.getElementById('filtroCanchaSelect');
        if (!sel) return;
        list.forEach(c => sel.insertAdjacentHTML('beforeend',
            `<option value="${c.id}">${c.nombre}</option>`));
    } catch { /* ignora */ }
}

/*  CARGAR RESERVAS  */
async function cargarReservas() {
    const container = document.getElementById('reservasContainer');
    container.innerHTML = '<div class="loading-state"><div class="spinner"></div> Cargando...</div>';
    try {
        // El backend ya las devuelve ordenadas por fecha de creación (más nuevas primero)
        const data = await api.get('/propietario/reservas');
        todasReservas = Array.isArray(data) ? data : (data.content ?? []);
        actualizarBadge();
        renderReservas();
    } catch {
        container.innerHTML = '<div class="empty-state"><h4>Error al cargar reservas</h4></div>';
    }
}

function actualizarBadge() {
    const bdg = document.getElementById('bdgPendiente');
    if (!bdg) return;
    const n = todasReservas.filter(r => r.estado === 'PENDIENTE').length;
    bdg.textContent = n;
    bdg.style.display = n > 0 ? 'inline-flex' : 'none';
}

/* FILTRAR */
function filtrar() { renderReservas(); }

function getReservasFiltradas() {
    const q         = (document.getElementById('buscarReserva')?.value ?? '').toLowerCase();
    const canchaId  = document.getElementById('filtroCanchaSelect')?.value ?? '';
    const estado    = document.getElementById('filtroEstadoSelect')?.value ?? '';

    return todasReservas.filter(r => {
        const matchQ = !q ||
            r.clienteEmail?.toLowerCase().includes(q) ||
            String(r.id).padStart(4, '0').includes(q) ||
            String(r.id).includes(q) ||
            r.clienteNombre?.toLowerCase().includes(q);
        const matchCancha = !canchaId || String(r.canchaId) === canchaId;
        const matchEstado = !estado
            || (estado === 'RECHAZADA' ? (r.estado === 'CANCELADA' && r.estadoPago === 'RECHAZADO')
                : r.estado === estado);
        return matchQ && matchCancha && matchEstado;
    });
}

/* RENDER */
function renderReservas() {
    const container = document.getElementById('reservasContainer');
    const lista = getReservasFiltradas(); // ya viene ordenada del backend, no se reordena aquí

    const total = document.getElementById('totalReservas');
    if (total) total.textContent = `${lista.length} reserva${lista.length !== 1 ? 's' : ''}`;

    if (!lista.length) {
        container.innerHTML = `<div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="3" y="4" width="18" height="18" rx="2"/>
                <path d="M16 2v4M8 2v4M3 10h18"/>
            </svg>
            <h4>Sin reservas</h4>
            <p>No hay reservas con los filtros seleccionados.</p>
        </div>`;
        return;
    }
    container.innerHTML = `<div class="reservas-list">${lista.map(r => renderCard(r)).join('')}</div>`;
}

function numReserva(id) {
    return '#' + String(id).padStart(4, '0');
}

function renderCard(r) {
    const esPendiente  = r.estado === 'PENDIENTE';
    const esCancelada  = r.estado === 'CANCELADA';
    const puedeReembolsar = esCancelada && r.estadoPago === 'COMPLETADO';
    const yaReembolsado   = esCancelada && r.estadoPago === 'REEMBOLSADO';

    let etiquetaCancelada = '';
    if (puedeReembolsar)      etiquetaCancelada = '<div class="rc-reembolso-label">⟳ Pendiente de reembolso</div>';
    else if (yaReembolsado)   etiquetaCancelada = '<div class="rc-reembolso-label" style="color:var(--text3)">✓ Reembolsado</div>';
    else if (esCancelada && r.estadoPago === 'RECHAZADO') etiquetaCancelada = '<div class="rc-reembolso-label" style="color:var(--danger)">✕ Rechazada por ti</div>';

    return `<div class="reserva-card ${esCancelada ? 'rc-cancelada' : ''}" id="rc-${r.id}">
        <div class="rc-left">
            <div class="rc-id">${numReserva(r.id)}</div>
            <div class="rc-nombre">${r.clienteNombre ?? '—'}</div>
            <div class="rc-email">${r.clienteEmail ?? '—'}</div>
        </div>
        <div class="rc-center">
            <div class="rc-cancha">${r.canchaName ?? '—'}</div>
            <div class="rc-fecha">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
                ${fmt(r.fecha)}
                &nbsp;·&nbsp;
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:13px;height:13px"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                ${fmtHora(r.horaInicio)} – ${fmtHora(r.horaFin)}
            </div>
            ${etiquetaCancelada}
        </div>
        <div class="rc-right">
            <div class="rc-monto ${esCancelada ? 'rc-monto-cancelada' : ''}">${fmtMoney(r.montoTotal)}</div>
            <div style="margin:4px 0">${badgeEstado(r.estado)}</div>
            <div class="rc-actions">
                <button class="btn btn-secondary btn-sm" onclick="verDetalle(${r.id})">Ver detalle</button>
                ${esPendiente ? `
                    <button class="btn btn-primary btn-sm"  onclick="aprobar(${r.id})">Aprobar</button>
                    <button class="btn btn-danger btn-sm"   onclick="abrirRechazar(${r.id})">Rechazar</button>
                ` : ''}
                ${r.estado === 'CONFIRMADA' ? `<button class="btn btn-danger btn-sm" onclick="abrirCancelarConfirmada(${r.id})">Cancelar reserva</button>` : ''}
                ${puedeReembolsar ? `<button class="btn btn-primary btn-sm" onclick="verDetalle(${r.id})">Reembolsar</button>` : ''}
            </div>
        </div>
    </div>`;
}

function fmtHora(val) {
    if (!val) return '—';
    if (Array.isArray(val)) {
        const [h, m] = val;
        return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
    }
    return String(val).substring(0, 5);
}

/*  LIGHTBOX ver imagen */
function verImagenZoom(url) {
    if (!url) return;
    let lb = document.getElementById('imgZoomOverlay');
    if (!lb) {
        document.body.insertAdjacentHTML('beforeend', `
            <div id="imgZoomOverlay" onclick="this.style.display='none'"
                 style="position:fixed;inset:0;background:rgba(0,0,0,.9);z-index:99999;display:none;align-items:center;justify-content:center;padding:30px;cursor:zoom-out">
                <button onclick="document.getElementById('imgZoomOverlay').style.display='none'"
                        style="position:absolute;top:20px;right:20px;width:38px;height:38px;border-radius:50%;background:rgba(255,255,255,.15);border:none;color:#fff;font-size:18px;cursor:pointer">✕</button>
                <img id="imgZoomTarget" src="" style="max-width:100%;max-height:100%;border-radius:8px;object-fit:contain"/>
            </div>`);
        lb = document.getElementById('imgZoomOverlay');
    }
    document.getElementById('imgZoomTarget').src = url;
    lb.style.display = 'flex';
}

/* DETALLE (+ acción de reembolso) */
let _reembolsoVoucherB64 = null;
let _reembolsoPagoIdActual = null;

async function verDetalle(id) {
    const body = document.getElementById('modalDetalleBody');
    const foot = document.getElementById('modalDetalleFoot');
    body.innerHTML = '<div class="loading-state"><div class="spinner"></div></div>';
    foot.innerHTML = '';
    openModal('modalDetalle');
    _reembolsoVoucherB64 = null;
    try {
        const r = await api.get(`/propietario/reservas/${id}`);
        const esCancelada = r.estado === 'CANCELADA';
        const puedeReembolsar = esCancelada && r.estadoPago === 'COMPLETADO';
        const yaReembolsado   = esCancelada && r.estadoPago === 'REEMBOLSADO';
        _reembolsoPagoIdActual = r.pagoId;

        body.innerHTML = `
            <div style="display:grid;gap:10px;font-size:13.5px">
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Reserva</span><strong>${numReserva(r.id)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Cliente</span><span>${r.clienteNombre ?? '—'}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Email</span><span>${r.clienteEmail ?? '—'}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Teléfono</span><span>${r.clienteTelefono ?? '—'}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Cancha</span><span>${r.canchaName ?? '—'}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Fecha</span><span>${fmt(r.fecha)}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Horario</span><span>${fmtHora(r.horaInicio)} – ${fmtHora(r.horaFin)}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Método pago</span><span>${r.metodoPago ? badgePago(r.metodoPago) : '—'}</span>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span style="color:var(--text3)">Monto</span>
                    <strong style="color:${esCancelada ? 'var(--danger)' : 'var(--success)'}">${fmtMoney(r.montoTotal)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between;align-items:center">
                    <span style="color:var(--text3)">Estado</span>${badgeEstado(r.estado)}
                </div>
                ${r.voucherUrl ? `
                    <div>
                        <label style="font-size:11px;font-weight:700;color:var(--text3);text-transform:uppercase">Comprobante del cliente</label><br/>
                        <img src="${r.voucherUrl}" onclick="verImagenZoom('${r.voucherUrl}')"
                             style="max-width:100%;max-height:160px;border-radius:8px;margin-top:6px;cursor:zoom-in;border:1px solid var(--border)"/>
                    </div>` : ''}
                ${esCancelada && r.notasPago && r.estadoPago === 'RECHAZADO' ? `
                    <div style="background:var(--danger-bg,#fef2f2);border-radius:8px;padding:10px;font-size:12px;color:var(--danger)">
                        Tú rechazaste esta reserva. Motivo: ${r.notasPago}
                    </div>` : ''}
                ${puedeReembolsar ? `
                    <div style="background:var(--warning-bg,#fff8e1);border-radius:8px;padding:12px;font-size:12.5px;color:var(--orange)">
                        ⟳ El cliente canceló esta reserva. Pendiente de reembolsar.
                    </div>
                    <div class="form-group">
                        <label>Comprobante de la devolución <span style="color:var(--danger)">*</span></label>
                        <div onclick="document.getElementById('reembVoucherInput').click()"
                             style="border:2px dashed var(--border2);border-radius:8px;padding:14px;text-align:center;cursor:pointer">
                            <input type="file" id="reembVoucherInput" accept="image/*" style="display:none" onchange="handleReembVoucher(event)"/>
                            <div id="reembVoucherPreview" style="font-size:12px;color:var(--text2)">Toca para subir el comprobante</div>
                        </div>
                    </div>
                    <div class="form-group">
                        <label>Mensaje para el cliente</label>
                        <textarea class="form-textarea" id="reembMensaje" rows="2" placeholder="Ej: Reembolso procesado vía Yape"></textarea>
                    </div>` : ''}
                ${yaReembolsado ? `
                    <div style="background:var(--bg3);border-radius:8px;padding:10px;font-size:12px;color:var(--text2)">
                        ✓ Ya reembolsaste esta reserva. ${r.notasPago ? 'Nota: ' + r.notasPago : ''}
                    </div>
                    ${r.voucherReembolsoUrl ? `<img src="${r.voucherReembolsoUrl}" onclick="verImagenZoom('${r.voucherReembolsoUrl}')" style="max-width:100%;max-height:140px;border-radius:8px;margin-top:6px;cursor:zoom-in;border:1px solid var(--border)"/>` : ''}
                ` : ''}
                ${esCancelada && r.canceladoPor === 'CLIENTE' ? `
                    <div style="background:var(--bg3);border-radius:8px;padding:10px;font-size:12px;color:var(--text2)">
                        El cliente canceló esta reserva por su cuenta.
                    </div>` : ''}
            </div>`;

        if (r.estado === 'PENDIENTE') {
            foot.innerHTML = `
                <button class="btn btn-secondary" onclick="closeModal('modalDetalle')">Cerrar</button>
                <button class="btn btn-danger"    onclick="closeModal('modalDetalle');abrirRechazar(${r.id})">Rechazar</button>
                <button class="btn btn-primary"   onclick="closeModal('modalDetalle');aprobar(${r.id})">Aprobar</button>`;
        } else if (r.estado === 'CONFIRMADA') {
            foot.innerHTML = `
                <button class="btn btn-secondary" onclick="closeModal('modalDetalle')">Cerrar</button>
                <button class="btn btn-danger"    onclick="closeModal('modalDetalle');abrirCancelarConfirmada(${r.id})">Cancelar reserva</button>`;
        } else if (puedeReembolsar) {
            foot.innerHTML = `
                <button class="btn btn-secondary" onclick="closeModal('modalDetalle')">Cerrar</button>
                <button class="btn btn-primary" id="btnConfirmarReembolso" onclick="procesarReembolsoDesdeReserva()">Marcar como reembolsado</button>`;
        } else {
            foot.innerHTML = `<button class="btn btn-secondary" onclick="closeModal('modalDetalle')" style="width:100%">Cerrar</button>`;
        }
    } catch {
        body.innerHTML = '<div class="empty-state"><h4>Error al cargar detalle</h4></div>';
        foot.innerHTML = `<button class="btn btn-secondary" onclick="closeModal('modalDetalle')">Cerrar</button>`;
    }
}

async function handleReembVoucher(e) {
    const f = e.target.files[0];
    if (!f) return;

    if (!f.type.startsWith('image/')) {
        toast('Solo se permiten imágenes (JPG, PNG o WEBP). PDF, videos u otros archivos no están permitidos.', 'error');
        e.target.value = '';
        return;
    }

    const preview = document.getElementById('reembVoucherPreview');
    preview.innerHTML = `<span style="font-size:12px">Subiendo…</span>`;

    try {
        const url = await uploadToCloudinary(f, 'vouchers');
        _reembolsoVoucherB64 = url; // ahora es la URL del archivo, no base64
        preview.innerHTML =
            `<strong style="color:var(--success)">${f.name}</strong><br/><small style="color:var(--success)">Listo ✓</small>`;
    } catch (err) {
        _reembolsoVoucherB64 = null;
        preview.innerHTML = `<span style="color:var(--danger,#e11)">Error al subir. Intenta de nuevo.</span>`;
        toast(err.message || 'Error al subir el comprobante', 'error');
    }
}

async function procesarReembolsoDesdeReserva() {
    if (!_reembolsoVoucherB64) { toast('Sube el comprobante de la devolución', 'error'); return; }
    const btn = document.getElementById('btnConfirmarReembolso');
    btn.disabled = true; btn.textContent = 'Procesando…';
    try {
        await api.patch(`/pagos/${_reembolsoPagoIdActual}/reembolso`, {
            notas: document.getElementById('reembMensaje')?.value || '',
            voucherUrl: _reembolsoVoucherB64,
        });
        toast('Reembolso registrado', 'success');
        closeModal('modalDetalle');
        await cargarReservas();
    } catch (e) {
        toast(e.message || 'No se pudo procesar el reembolso', 'error');
        btn.disabled = false; btn.textContent = 'Marcar como reembolsado';
    }
}

/*  APROBAR  */
async function aprobar(id) {
    try {
        await api.patch(`/propietario/reservas/${id}/aprobar`, {});
        toast('Reserva aprobada correctamente', 'success');
        await cargarReservas();
    } catch (e) { toast(e.message || 'Error al aprobar', 'error'); }
}

/*  RECHAZAR (reserva PENDIENTE) */
function abrirRechazar(id) {
    reservaAccion = id;
    const motivo = document.getElementById('motivoRechazo');
    if (motivo) motivo.selectedIndex = 0;
    const msg = document.getElementById('mensajeRechazo');
    if (msg) msg.value = '';
    onMotivoRechazoChange();
    openModal('modalRechazar');
}

function onMotivoRechazoChange() {
    const sel = document.getElementById('motivoRechazo');
    const chk = document.getElementById('chkReembolsar');
    if (!sel || !chk) return;
    const opt = sel.options[sel.selectedIndex];
    chk.checked = opt?.dataset.reembolsar === 'true';
}

async function confirmarRechazar() {
    if (!reservaAccion) return;
    const motivo     = document.getElementById('motivoRechazo')?.value ?? '';
    const mensaje    = document.getElementById('mensajeRechazo')?.value.trim() ?? '';
    const reembolsar = document.getElementById('chkReembolsar')?.checked ?? false;

    if (motivo.toLowerCase() === 'otro' && !mensaje) {
        toast('Debes escribir un mensaje cuando el motivo es "Otro"', 'error');
        return;
    }

    try {
        await api.patch(`/propietario/reservas/${reservaAccion}/rechazar`, { motivo, mensaje, reembolsar });
        toast(reembolsar ? 'Reserva rechazada — queda pendiente de reembolso' : 'Reserva rechazada', 'success');
        closeModal('modalRechazar');
        reservaAccion = null;
        await cargarReservas();
    } catch (e) {
        toast(e.message || 'Error al rechazar', 'error');
        reservaAccion = null;
    }
}


function abrirCancelarConfirmada(id) {
    reservaAccion = id;
    const motivo = document.getElementById('motivoCancelarConfirmada');
    if (motivo) motivo.selectedIndex = 0;
    const msg = document.getElementById('mensajeCancelarConfirmada');
    if (msg) msg.value = '';
    openModal('modalCancelarConfirmada');
}

async function confirmarCancelarConfirmada() {
    if (!reservaAccion) return;
    const motivo  = document.getElementById('motivoCancelarConfirmada')?.value ?? '';
    const mensaje = document.getElementById('mensajeCancelarConfirmada')?.value.trim() ?? '';

    if (motivo.toLowerCase() === 'otro' && !mensaje) {
        toast('Debes escribir un mensaje cuando el motivo es "Otro"', 'error');
        return;
    }

    const btn = document.getElementById('btnConfirmCancelarConfirmada');
    btn.disabled = true; btn.textContent = 'Cancelando…';
    try {
        await api.patch(`/propietario/reservas/${reservaAccion}/cancelar`, { motivo, mensaje });
        toast('Reserva cancelada — queda pendiente de reembolso', 'success');
        closeModal('modalCancelarConfirmada');
        reservaAccion = null;
        await cargarReservas();
    } catch (e) {
        toast(e.message || 'No se pudo cancelar la reserva', 'error');
    } finally {
        btn.disabled = false; btn.textContent = 'Cancelar reserva';
    }
}