'use strict';
/* MIS RESERVAS */

let _todas       = [];
let _tabActual    = 'activas';
let _cancelarId   = null;
let _detalleResId = null;

const SVG_CAL = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M3 10h18"/></svg>`;
const SVG_CLK = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`;
const SVG_PIN = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`;

function numReserva(id) { return '#' + String(id).padStart(4, '0'); }

document.addEventListener('DOMContentLoaded', async () => {
    if (!await initPage()) return;
    await cargarReservas();
});

/* CARGA */
async function cargarReservas() {
    try {
        _todas = await api.get('/reservas/mis-reservas');
        calcularStats();
        renderTab(_tabActual);
    } catch(e) {
        document.getElementById('listaActivas').innerHTML = `
            <div class="empty-state">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                <h4>Error al cargar reservas</h4>
                <p>Recarga la página para intentarlo de nuevo.</p>
            </div>`;
        console.error(e);
    }
}

/* STATS */
function calcularStats() {
    const ahora = Date.now();
    const pend  = _todas.filter(r => r.estado === 'PENDIENTE').length;
    const conf  = _todas.filter(r => r.estado === 'CONFIRMADA' && timestampReserva(r) > ahora).length;
    const comp  = _todas.filter(r => r.estado === 'COMPLETADA').length;
    const gasto = _todas
        .filter(r => r.estado === 'COMPLETADA')
        .reduce((s, r) => s + Number(r.total || 0), 0);

    document.getElementById('stPendientes').textContent  = pend;
    document.getElementById('stConfirmadas').textContent = conf;
    document.getElementById('stCompletadas').textContent = comp;
    document.getElementById('stGasto').textContent       = formatCurrency(gasto);

    // Badge: solo las que aún requieren acción (pend) + las confirmadas que faltan por jugar
    setNavBadgeReservas(pend + conf);
}

/* TABS */
function switchTab(tab) {
    _tabActual = tab;
    document.getElementById('tabActivas').classList.toggle('active',   tab === 'activas');
    document.getElementById('tabHistorial').classList.toggle('active', tab === 'historial');
    document.getElementById('listaActivas').style.display   = tab === 'activas'   ? 'block' : 'none';
    document.getElementById('listaHistorial').style.display = tab === 'historial' ? 'block' : 'none';
    document.getElementById('filtrosHistorial').style.display = tab === 'historial' ? 'block' : 'none';
    renderTab(tab);
}

function renderTab(tab) {
    const ahora = Date.now();

    // "Activa" = reserva que AÚN NO ocurrió y no está terminada/cancelada.
    // FIX — antes solo se filtraba por estado. El problema es que una reserva
    // queda como CONFIRMADA en la BD para siempre a menos que algo la marque
    // COMPLETADA (no hay proceso automático en el backend). Así que una reserva
    // de hace 3 semanas aún tenía estado=CONFIRMADA y aparecía en "Activas".
    // Solución: se considera "activa" solo si el partido todavía no empezó.
    const esActiva = r =>
        (r.estado === 'PENDIENTE' || r.estado === 'CONFIRMADA')
        && timestampReserva(r) > ahora;

    // "Historial" = cancelada, completada, o CONFIRMADA cuyo partido ya pasó.
    const esHistorial = r =>
        r.estado === 'COMPLETADA'
        || r.estado === 'CANCELADA'
        || ((r.estado === 'CONFIRMADA') && timestampReserva(r) <= ahora);

    if (tab === 'activas') {
        const lista = _todas.filter(esActiva);
        document.getElementById('listaActivas').innerHTML = renderLista(lista, true);
    } else {
        let lista = _todas.filter(esHistorial);

        // filtro por categoría
        if (_filtroHistorial === 'confirmada') {
            lista = lista.filter(r => r.estado === 'CONFIRMADA');
        } else if (_filtroHistorial === 'completada') {
            lista = lista.filter(r => r.estado === 'COMPLETADA');
        } else if (_filtroHistorial === 'cancelada') {
            lista = lista.filter(r => r.estado === 'CANCELADA' && !r.reembolsoProcesado);
        } else if (_filtroHistorial === 'reembolsada') {
            lista = lista.filter(r => r.estado === 'CANCELADA' && r.reembolsoProcesado);
        }

        // buscador: cancha, deporte, código de reserva, fecha
        const q = _buscarHistorial.trim().toLowerCase();
        if (q) {
            lista = lista.filter(r => {
                const codigo = numReserva(r.id).toLowerCase();
                const cancha = (r.canchaNombre || '').toLowerCase();
                const deporte = (r.deporte || r.canchaDeporte || '').toLowerCase();
                const fecha = _fechaStr(r).toLowerCase();
                return codigo.includes(q) || cancha.includes(q) || deporte.includes(q) || fecha.includes(q);
            });
        }

        document.getElementById('listaHistorial').innerHTML =
            lista.length ? renderLista(lista, false)
                : `<div class="empty-state" style="padding:30px;text-align:center;color:var(--text3);font-size:13px">
                                Sin resultados para este filtro/búsqueda.
                            </div>`;
    }
}

/*  FILTRO Y BÚSQUEDA (Historial) */
let _filtroHistorial = 'todas';
let _buscarHistorial = '';

function setFiltroHistorial(tipo, btn) {
    _filtroHistorial = tipo;
    document.querySelectorAll('.filtro-chip').forEach(b => {
        const activo = b === btn;
        b.style.background = activo ? 'var(--primary, #2563eb)' : 'var(--bg3, #f1f5f9)';
        b.style.color      = activo ? '#fff' : 'var(--text2, #334155)';
        b.classList.toggle('active', activo);
    });
    renderTab('historial');
}

function buscarEnHistorial(valor) {
    _buscarHistorial = valor;
    renderTab('historial');
}

/* RENDER CARDS  */
/* Spring sin write-dates-as-timestamps=false devuelve arrays: fecha=[2026,7,15], hora=[9,0,0].
   _fechaStr() normaliza ambos formatos a "YYYY-MM-DD" para que new Date() funcione. */
function _fechaStr(r) {
    if (Array.isArray(r.fecha)) {
        const [y,mo,d] = r.fecha;
        return `${y}-${String(mo).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
    }
    return r.fecha || '';
}
function timestampReserva(r) {
    try {
        const t = new Date(`${_fechaStr(r)}T${fmtHora(r.horaInicio)}:00`).getTime();
        return isNaN(t) ? 0 : t;
    } catch { return 0; }
}

function renderLista(lista, mostrarCancelar) {
    if (!lista.length) {
        return `<div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
            <h4>${mostrarCancelar ? 'No tienes reservas activas' : 'Sin historial todavía'}</h4>
            <p>${mostrarCancelar ? '¡Reserva una cancha y empieza a jugar!' : 'Tus reservas completadas y canceladas aparecerán aquí.'}</p>
            ${mostrarCancelar ? `<a href="/cliente/buscar" class="btn btn-primary btn-sm" style="margin-top:8px">Buscar cancha</a>` : ''}
        </div>`;
    }

    // Activas: la más próxima a jugarse primero. Historial: la más reciente primero.
    const ordenada = [...lista].sort((a, b) =>
        mostrarCancelar
            ? timestampReserva(a) - timestampReserva(b)
            : timestampReserva(b) - timestampReserva(a));

    return ordenada.map(r => renderCard(r, mostrarCancelar)).join('');
}

/** true si el horario de la reserva ya empezó o pasó */
function partidoYaEmpezo(r) {
    try {
        const inicio = new Date(`${_fechaStr(r)}T${fmtHora(r.horaInicio)}:00`);
        return !isNaN(inicio) && inicio <= new Date();
    } catch { return false; }
}

function renderCard(r, mostrarCancelar) {
    const { emoji } = iconoDeporte(r.canchaDeporte || r.deporte || '');
    const iconBg = {
        PENDIENTE:  'var(--warning-bg)',
        CONFIRMADA: 'var(--accent-light)',
        COMPLETADA: 'var(--success-bg)',
        CANCELADA:  'var(--danger-bg)',
    }[r.estado] || 'var(--bg3)';

    const alertaPago = r.estado === 'PENDIENTE'
        ? `<div class="voucher-alert" style="margin-top:6px">
               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
               Esperando aprobación del propietario.
           </div>`
        : '';

    const yaEmpezo   = partidoYaEmpezo(r);
    const esPendiente = r.estado === 'PENDIENTE';
    const esConfirmada= r.estado === 'CONFIRMADA';

    let btnCancelar = '';
    if (mostrarCancelar) {
        // PENDIENTE: no se puede cancelar — el propietario aún no ha aprobado.
        // CONFIRMADA: se puede cancelar solo si el partido no empezó todavía.
        if (esConfirmada && !yaEmpezo) {
            btnCancelar = `<button class="btn-cancelar" onclick="pedirCancelar(${r.id}, '${(r.canchaNombre||'').replace(/'/g,"")}', '${formatDate(r.fecha)}')">Cancelar</button>`;
        } else if (esPendiente) {
            btnCancelar = `<span style="font-size:11px;color:var(--text3)">Esperando aprobación</span>`;
        } else if (esConfirmada && yaEmpezo) {
            btnCancelar = `<span style="font-size:11px;color:var(--text3)">Ya no se puede cancelar</span>`;
        }
    }

    return `
    <div class="reserva-card">
        <div class="res-icon" style="background:${iconBg}">${emoji}</div>
        <div class="res-info">
            <h3>${numReserva(r.id)} · ${r.canchaNombre || 'Cancha'} — ${labelDeporte(r.canchaDeporte || r.deporte || '')}</h3>
            <div class="res-meta">
                <span>${SVG_CAL} ${formatDate(r.fecha)}</span>
                <span>${SVG_CLK} ${fmtHora(r.horaInicio)} – ${fmtHora(r.horaFin)}</span>
                <span>${SVG_PIN} ${r.canchaDistrito || r.canhaDistrito || ''}</span>
            </div>
            ${badgeEstadoReserva(r.estado)}
            ${alertaPago}
        </div>
        <div class="res-right">
            <div class="res-precio">${formatCurrency(r.total)}</div>
            <button class="btn btn-secondary btn-sm" style="margin-bottom:6px" onclick="verDetalle(${r.id})">Ver detalles</button>
            ${btnCancelar}
        </div>
    </div>`;
}

/* VER DETALLES (estado real: aprobada / rechazada / en reembolso / reembolsada) */
async function verDetalle(reservaId) {
    _detalleResId = reservaId;
    document.getElementById('modalDetalle').classList.add('open');
    document.body.style.overflow = 'hidden';
    const body = document.getElementById('detalleBody');
    body.innerHTML = `<div class="loading-state"><div class="spinner"></div> Cargando…</div>`;

    const reserva = _todas.find(r => r.id === reservaId);

    try {
        const pago = await api.get(`/pagos/reserva/${reservaId}`);
        body.innerHTML = renderDetalle(reserva, pago);
    } catch(e) {
        // Puede no existir pago aún (reserva creada pero sin pago registrado)
        body.innerHTML = renderDetalle(reserva, null);
    }
}

function renderDetalle(r, pago) {
    if (!r) return `<p style="font-size:13px;color:var(--text3)">No se encontró la reserva.</p>`;

    let estadoBox = '';
    const ICON_OK    = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>`;
    const ICON_CLOCK = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`;
    const ICON_X     = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`;
    const ICON_REF   = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-9.5L1 10"/></svg>`;

    if (r.estado === 'PENDIENTE') {
        estadoBox = `<div class="detalle-estado-box pendiente">${ICON_CLOCK}
            <div><h4>Esperando aprobación</h4><p>El propietario está revisando tu comprobante de pago. Te avisaremos apenas la confirme.</p></div>
        </div>`;
    } else if (r.estado === 'CONFIRMADA') {
        estadoBox = `<div class="detalle-estado-box ok">${ICON_OK}
            <div><h4>Reserva confirmada</h4><p>El propietario aprobó tu pago. ¡Tu cancha está reservada!</p></div>
        </div>`;
    } else if (r.estado === 'COMPLETADA') {
        estadoBox = `<div class="detalle-estado-box info">${ICON_OK}
            <div><h4>Partido completado</h4><p>Esperamos que la hayas pasado bien.</p></div>
        </div>`;
    } else if (r.estado === 'CANCELADA') {
        const pe = pago ? pago.estado : null;
        const porPropietario = r.canceladoPor === 'PROPIETARIO';

        if (pe === 'RECHAZADO') {
            // Nunca llegó a aprobarse — el propietario rechazó el comprobante/pago.
            estadoBox = `<div class="detalle-estado-box error">${ICON_X}
                <div><h4>Rechazada por el propietario</h4>
                <p>${pago.notas ? esc(pago.notas) : 'No se indicó un motivo.'}</p></div>
            </div>`;
        } else if (pe === 'COMPLETADO' && porPropietario) {
            // La reserva SÍ estaba confirmada (pago ya verificado), pero el
            // propietario la canceló después (cancha dañada, error suyo, etc).
            estadoBox = `<div class="detalle-estado-box pendiente">${ICON_REF}
                <div><h4>El propietario canceló tu reserva</h4>
                <p>${pago.notas ? esc(pago.notas) : 'Tu reserva ya estaba confirmada, pero el propietario tuvo que cancelarla.'} Tu dinero está pendiente de devolución.</p></div>
            </div>`;
        } else if (pe === 'COMPLETADO') {
            // Cancelada por el propio cliente, pendiente de reembolso.
            estadoBox = `<div class="detalle-estado-box pendiente">${ICON_REF}
                <div><h4>En reembolso</h4><p>Cancelaste esta reserva. El propietario está procesando la devolución de tu dinero.</p></div>
            </div>`;
        } else if (pe === 'REEMBOLSADO') {
            estadoBox = `<div class="detalle-estado-box ok">${ICON_REF}
                <div><h4>Reembolsada</h4>
                <p>${porPropietario ? 'El propietario canceló esta reserva y procesó tu devolución.' : 'Cancelaste esta reserva y el propietario procesó tu devolución.'}
                ${pago.notas ? ' ' + esc(pago.notas) : ''}</p></div>
            </div>`;
        } else {
            estadoBox = `<div class="detalle-estado-box error">${ICON_X}
                <div><h4>Cancelada</h4><p>Esta reserva fue cancelada.</p></div>
            </div>`;
        }
    }

    const infoGrid = `
    <div class="info-grid" style="margin-bottom:14px">
        <div class="info-item"><label>Cancha</label><span>${r.canchaNombre || '—'}</span></div>
        <div class="info-item"><label>Deporte</label><span>${labelDeporte(r.canchaDeporte || r.deporte || '')}</span></div>
        <div class="info-item"><label>Fecha</label><span>${formatDate(r.fecha)}</span></div>
        <div class="info-item"><label>Hora</label><span>${fmtHora(r.horaInicio)} – ${fmtHora(r.horaFin)}</span></div>
        <div class="info-item"><label>Total</label><span>${formatCurrency(r.total)}</span></div>
        <div class="info-item"><label>Propietario</label><span>${r.propietarioNombre || '—'}</span></div>
    </div>`;

    let pagoBox = '';
    if (pago) {
        pagoBox = `
        <div class="subsec-label">Pago</div>
        <div class="info-grid" style="margin-bottom:10px">
            <div class="info-item"><label>Método</label><span>${pago.metodo || '—'}</span></div>
            <div class="info-item"><label>Estado del pago</label><span>${pago.estado || '—'}</span></div>
        </div>
        ${pago.voucherUrl ? `
            <div class="subsec-label">Tu comprobante</div>
            <img src="${pago.voucherUrl}" class="detalle-voucher-img" onclick="verImagenZoom('${pago.voucherUrl}')"/>
        ` : ''}
        ${pago.voucherReembolsoUrl ? `
            <div class="subsec-label" style="margin-top:12px">Comprobante de la devolución</div>
            <img src="${pago.voucherReembolsoUrl}" class="detalle-voucher-img" onclick="verImagenZoom('${pago.voucherReembolsoUrl}')"/>
        ` : ''}`;
    }

    return estadoBox + infoGrid + pagoBox;
}

function esc(s) {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}

function cerrarModalDetalle(e) {
    if (e && e.target !== document.getElementById('modalDetalle')) return;
    document.getElementById('modalDetalle').classList.remove('open');
    document.body.style.overflow = '';
}

/* CANCELAR  */
function pedirCancelar(id, nombre, fecha) {
    _cancelarId = id;
    const reserva = _todas.find(r => r.id === id);
    const esPendiente = reserva?.estado === 'PENDIENTE';

    document.getElementById('detalleCancelar').textContent = `${nombre} · ${fecha}`;

    // Actualizar el texto del modal según el tipo de cancelación
    const info = document.querySelector('#modalCancelar .modal-body p');
    if (info) {
        if (esPendiente) {
            info.textContent = 'Tu solicitud de reserva aún no fue aprobada. Al cancelar, el horario quedará libre.';
        } else {
            info.innerHTML = '¿Seguro que quieres cancelar? Solo puedes cancelar reservas confirmadas con <strong>más de 24 horas</strong> de anticipación. Si ya pagaste, el propietario procesará tu reembolso.';
        }
    }

    document.getElementById('modalCancelar').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function cerrarModalCancelar(e) {
    if (e && e.target !== document.getElementById('modalCancelar')) return;
    document.getElementById('modalCancelar').classList.remove('open');
    document.body.style.overflow = '';
    _cancelarId = null;
}

async function ejecutarCancelar() {
    if (!_cancelarId) return;
    const btn = document.getElementById('btnConfirmarCancelar');
    btn.disabled    = true;
    btn.textContent = 'Cancelando…';
    try {
        await api.patch(`/reservas/${_cancelarId}/cancelar`);
        cerrarModalCancelar();
        toast('Reserva cancelada', 'info');
        await cargarReservas();
        renderTab(_tabActual);
    } catch(e) {
        toast(e.message || 'No se pudo cancelar la reserva', 'error');
        console.error(e);
    } finally {
        btn.disabled    = false;
        btn.innerHTML   = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Sí, cancelar`;
    }
}

/* LIGHTBOX ver comprobante */
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