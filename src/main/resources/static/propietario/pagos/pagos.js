'use strict';

let datosPagos = null; // cache del último fetch

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Pagos e Ingresos');
    await cargarPagos();
    await cargarMetodosPago();
});

async function cargarPagos() {
    const periodo = document.getElementById('filtroPeriodo')?.value ?? 'mes';
    try {
        const data = await api.get(`/propietario/pagos/resumen?periodo=${periodo}`);
        datosPagos = data;

        // ── Tarjetas de resumen (sólo ingresos CONFIRMADOS, sin canceladas) ──
        setText('statHoy',       fmtMoney(data.ingresoHoy));
        setText('statHoySub',    `${data.reservasHoy ?? 0} reservas confirmadas`);

        setText('statSemana',    fmtMoney(data.ingresoSemana));
        const varSem = data.variacionSemana ?? 0;
        setText('statSemanaSub', `${varSem >= 0 ? '+' : ''}${varSem}% vs sem. anterior`);
        const elSemanaSub = document.getElementById('statSemanaSub');
        if (elSemanaSub) elSemanaSub.style.color = varSem >= 0 ? 'var(--success)' : 'var(--danger)';
        const barSem = document.getElementById('barSemana');
        if (barSem) barSem.style.width = Math.min(data.pctSemana ?? 0, 100) + '%';

        setText('statMes',    fmtMoney(data.ingresoMes));
        const varMes = data.variacionMes ?? 0;
        setText('statMesSub', `${varMes >= 0 ? '+' : ''}${varMes}% vs mes anterior`);
        const elMesSub = document.getElementById('statMesSub');
        if (elMesSub) elMesSub.style.color = varMes >= 0 ? 'var(--success)' : 'var(--danger)';
        const barMes = document.getElementById('barMes');
        if (barMes) barMes.style.width = Math.min(data.pctMes ?? 0, 100) + '%';

        setText('statPendiente',    fmtMoney(data.ingresoPendiente));
        setText('statPendienteSub', `${data.reservasPendientesVerif ?? 0} vouchers por verificar`);

        // ── Gráfico por cancha ──────────────────────────────────
        const periodLabel = { mes:'este mes', semana:'esta semana', hoy:'hoy', '3meses':'últimos 3 meses' };
        const chartTitle = document.getElementById('chartTitle');
        if (chartTitle) chartTitle.textContent = `Ingresos por cancha — ${periodLabel[periodo] ?? periodo}`;

        const chartCanchas = document.getElementById('chartCanchas');
        if (chartCanchas) {
            const ingCancha = data.ingresosPorCancha ?? [];
            if (!ingCancha.length) {
                chartCanchas.innerHTML = '<p style="color:var(--text3);font-size:12px;text-align:center;padding:20px">Sin datos de ingresos para el período</p>';
            } else {
                chartCanchas.style.display = 'grid';
                chartCanchas.style.gridTemplateColumns = 'repeat(auto-fill,minmax(110px,1fr))';
                chartCanchas.style.gap = '16px';
                chartCanchas.style.alignItems = 'flex-end';
                const maxVal = Math.max(...ingCancha.map(c => Number(c.total) || 0), 1);
                chartCanchas.innerHTML = ingCancha.map(c => {
                    const pct = (Number(c.total) / maxVal) * 100;
                    return `<div style="text-align:center">
                        <div style="font-size:11px;color:var(--text3);margin-bottom:6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${c.nombre}</div>
                        <div style="height:80px;display:flex;align-items:flex-end;justify-content:center">
                            <div style="width:40px;background:var(--accent);border-radius:4px 4px 0 0;height:${Math.max(pct, 4)}%;transition:height .3s"></div>
                        </div>
                        <div style="font-weight:700;font-size:13px;margin-top:6px">${fmtMoney(c.total)}</div>
                        <div style="font-size:11px;color:var(--text3)">${c.reservas ?? 0} res.</div>
                    </div>`;
                }).join('');
            }
        }

        // ── Tabla de últimos pagos (con estados de reembolso) ───
        const periodoLabelEl = document.getElementById('periodoLabel');
        if (periodoLabelEl) periodoLabelEl.textContent = etiquetaPeriodo(periodo);

        const tbody = document.getElementById('tablaPagosTbody');
        if (tbody) {
            const pagos = data.ultimosPagos ?? [];
            if (!pagos.length) {
                tbody.innerHTML = `<tr><td colspan="7"><div style="padding:24px;text-align:center;color:var(--text3);font-size:13px">Sin movimientos para este período</div></td></tr>`;
            } else {
                tbody.innerHTML = pagos.map(p => renderFilaPago(p)).join('');
            }
        }
    } catch (e) {
        const tbody = document.getElementById('tablaPagosTbody');
        if (tbody) tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state"><h4>Error al cargar pagos</h4></div></td></tr>`;
    }
}

function renderFilaPago(p) {
    const esCancelada = p.cancelada || p.estadoReserva === 'CANCELADA';
    const estadoPago  = p.estadoPago ?? (p.verificado ? 'COMPLETADO' : 'PENDIENTE');

    let estadoBadge;
    let rowClass = '';
    if (esCancelada && estadoPago === 'REEMBOLSADO') {
        estadoBadge = '<span class="badge badge-gray">Reembolsado</span>';
        rowClass = 'tr-cancelada';
    } else if (esCancelada) {
        estadoBadge = '<span class="badge badge-warning">En reembolso</span>';
        rowClass = 'tr-cancelada';
    } else if (estadoPago === 'COMPLETADO') {
        estadoBadge = '<span class="badge badge-success">Verificado</span>';
    } else if (estadoPago === 'RECHAZADO') {
        estadoBadge = '<span class="badge badge-danger">Rechazado</span>';
    } else {
        estadoBadge = '<span class="badge badge-warning">Pendiente</span>';
    }

    return `<tr class="${rowClass}">
        <td>
            <div class="td-main">${p.clienteNombre ?? '—'}</div>
            <div class="td-sub">${p.clienteEmail ?? ''}</div>
        </td>
        <td>${p.canchaName ?? p.canchaNombre ?? '—'}</td>
        <td>${fmt(p.fecha ?? p.fechaPago)}</td>
        <td>${p.metodoPago ? badgePago(p.metodoPago) : '—'}</td>
        <td style="font-weight:600;color:${esCancelada ? 'var(--text3)' : 'var(--success)'};${esCancelada ? 'text-decoration:line-through' : ''}">${fmtMoney(p.monto)}</td>
        <td>${estadoBadge}</td>
        <td>
            <button class="btn btn-secondary btn-sm" onclick="verDetallePago(${p.reservaId ?? p.id}, ${p.id}, ${esCancelada && estadoPago === 'COMPLETADO'})">Ver detalles</button>
        </td>
    </tr>`;
}

let _reembolsoVoucherBase64 = null;
let _reembolsoPagoId = null;

async function verDetallePago(reservaId, pagoId, puedeReembolsar) {
    if (!reservaId) return;
    try {
        const r = await api.get(`/propietario/reservas/${reservaId}`);
        const esCancelada = r.estado === 'CANCELADA';

        const body = document.getElementById('detallePagoBody');
        const footer = document.getElementById('detallePagoFooter');
        if (!body) { // fallback si el modal aún no existe en el HTML
            alert(`Reserva ${String(r.id).padStart(4,'0')}\nCliente: ${r.clienteNombre ?? '—'}\nCancha: ${r.canchaName ?? '—'}\nFecha: ${fmt(r.fecha)}\nMonto: ${fmtMoney(r.montoTotal)}\nEstado: ${r.estado}`);
            return;
        }

        body.innerHTML = `
            <div class="info-grid" style="display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:14px">
                <div class="info-item"><label>Cliente</label><span>${r.clienteNombre ?? '—'}</span></div>
                <div class="info-item"><label>Cancha</label><span>${r.canchaName ?? '—'}</span></div>
                <div class="info-item"><label>Fecha</label><span>${fmt(r.fecha)}</span></div>
                <div class="info-item"><label>Monto</label><span>${fmtMoney(r.montoTotal)}</span></div>
            </div>
            ${r.voucherUrl ? `<div style="margin-bottom:14px"><label style="font-size:11px;font-weight:700;color:var(--text3);text-transform:uppercase">Comprobante del cliente</label><br/><img src="${r.voucherUrl}" style="max-width:100%;border-radius:8px;margin-top:6px;cursor:zoom-in" onclick="verImagenZoom('${r.voucherUrl}')"/></div>` : ''}
            ${esCancelada ? `<p style="font-size:13px;color:var(--warning);background:var(--warning-bg);border-radius:6px;padding:10px 12px;margin-bottom:10px">El cliente canceló esta reserva. ${puedeReembolsar ? 'Procesa la devolución abajo.' : ''}</p>` : ''}
            ${puedeReembolsar ? `
            <div class="form-group">
                <label>Comprobante de la devolución <span style="color:var(--danger)">*</span></label>
                <div class="voucher-zone" onclick="document.getElementById('reembolsoVoucherInput').click()" style="border:2px dashed var(--border2);border-radius:8px;padding:16px;text-align:center;cursor:pointer">
                    <input type="file" id="reembolsoVoucherInput" accept="image/*" style="display:none" onchange="handleReembolsoVoucher(event)"/>
                    <div id="reembolsoVoucherPreview" style="font-size:12.5px;color:var(--text2)">Toca para subir el comprobante de devolución</div>
                </div>
            </div>
            <div class="form-group">
                <label>Mensaje para el cliente</label>
                <textarea class="form-textarea" id="reembolsoMensaje" rows="2" placeholder="Ej: Reembolso procesado vía Yape"></textarea>
            </div>` : ''}
        `;

        _reembolsoVoucherBase64 = null;
        _reembolsoPagoId = pagoId;

        footer.innerHTML = puedeReembolsar
            ? `<button class="btn btn-secondary" onclick="closeModal('modalDetallePago')">Cerrar</button>
               <button class="btn btn-primary" id="btnProcesarReembolso" onclick="procesarReembolsoPago()">Marcar como reembolsado</button>`
            : `<button class="btn btn-secondary" onclick="closeModal('modalDetallePago')" style="width:100%">Cerrar</button>`;

        openModal('modalDetallePago');
    } catch { toast('No se pudo cargar el detalle', 'error'); }
}

async function handleReembolsoVoucher(e) {
    const f = e.target.files[0];
    if (!f) return;

    if (!f.type.startsWith('image/')) {
        toast('Solo se permiten imágenes (JPG, PNG o WEBP). PDF, videos u otros archivos no están permitidos.', 'error');
        e.target.value = '';
        return;
    }

    const preview = document.getElementById('reembolsoVoucherPreview');
    preview.innerHTML = `<span style="font-size:12px">Subiendo…</span>`;

    try {
        const url = await uploadToCloudinary(f, 'vouchers');
        _reembolsoVoucherBase64 = url; // ahora es la URL del archivo, no base64
        preview.innerHTML =
            `<strong style="color:var(--success)">${f.name}</strong><br/><small style="color:var(--success)">Listo ✓</small>`;
    } catch (err) {
        _reembolsoVoucherBase64 = null;
        preview.innerHTML = `<span style="color:var(--danger,#e11)">Error al subir. Intenta de nuevo.</span>`;
        toast(err.message || 'Error al subir el comprobante', 'error');
    }
}

async function procesarReembolsoPago() {
    if (!_reembolsoVoucherBase64) { toast('Sube el comprobante de la devolución', 'error'); return; }
    const btn = document.getElementById('btnProcesarReembolso');
    btn.disabled = true; btn.textContent = 'Procesando…';
    try {
        await api.patch(`/pagos/${_reembolsoPagoId}/reembolso`, {
            notas: document.getElementById('reembolsoMensaje')?.value || '',
            voucherUrl: _reembolsoVoucherBase64,
        });
        toast('Reembolso registrado', 'success');
        closeModal('modalDetallePago');
        await cargarPagos();
    } catch(e) {
        toast(e.message || 'No se pudo procesar el reembolso', 'error');
        btn.disabled = false; btn.textContent = 'Marcar como reembolsado';
    }
}

/* ── EXPORTAR A EXCEL ───────────────────────────────────────── */
function exportarExcel() {
    const pagos = datosPagos?.ultimosPagos ?? [];
    if (!pagos.length) { toast('Sin datos para exportar', 'info'); return; }

    // Crear CSV (Excel lo abre nativamente)
    const bom = '\uFEFF'; // UTF-8 BOM para que Excel reconozca tildes
    const header = ['#Reserva','Cliente','Email','Cancha','Fecha','Método','Monto','Estado'];
    const rows = pagos.map(p => {
        const esCancelada = p.cancelada || p.estadoReserva === 'CANCELADA';
        const estado = esCancelada
            ? (p.estadoPago === 'REEMBOLSADO' ? 'Reembolsado' : 'En reembolso')
            : (p.estadoPago === 'COMPLETADO' ? 'Verificado'
                : p.estadoPago === 'RECHAZADO' ? 'Rechazado' : 'Pendiente');
        return [
            String(p.reservaId ?? p.id ?? '').padStart(4,'0'),
            p.clienteNombre ?? '',
            p.clienteEmail ?? '',
            p.canchaName ?? '',
            fmt(p.fecha ?? p.fechaPago),
            p.metodoPago ?? '',
            p.monto ?? 0,
            estado
        ].map(v => `"${String(v).replace(/"/g,'""')}"`);
    });

    const csv = bom + [header, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = `pagos_${new Date().toISOString().slice(0,10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast('Archivo exportado correctamente', 'success');
}

function etiquetaPeriodo(periodo) {
    const ahora = new Date();
    const mes   = ahora.toLocaleDateString('es-PE', { month: 'long', year: 'numeric' });
    return { mes, semana: 'Esta semana', hoy: 'Hoy', '3meses': 'Últimos 3 meses' }[periodo] ?? mes;
}

/* ═══════════════════════════════════════════════════════════════
   MÉTODOS DE PAGO
   ═══════════════════════════════════════════════════════════════ */

let metodosPagoCache = [];
let metodoPagoEditando = null; // tipo del método que se está editando/creando

const MP_DEF = {
    TRANSFERENCIA: { label: 'Transferencia bancaria', color: 'var(--info,#3b82f6)', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M2 11h20M7 15h.01M12 15h.01"/></svg>` },
    YAPE:          { label: 'Yape',                 color: 'var(--purple,#8b5cf6)', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>` },
    PLIN:          { label: 'Plin',                 color: 'var(--success,#10b981)', icon: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="5" y="2" width="14" height="20" rx="2"/><path d="M12 18h.01"/></svg>` },
};
const MP_TIPOS = ['TRANSFERENCIA', 'YAPE', 'PLIN'];

const BANCOS_PERU = [
    'Banco de Crédito del Perú (BCP)',
    'BBVA Perú',
    'Scotiabank Perú',
    'Interbank',
    'Mibanco',
    'Banco Interamericano de Finanzas (BanBif)',
    'Banco Pichincha',
    'Banco Santander Perú',
    'Banco Falabella',
    'Banco Ripley',
    'Banco GNB Perú',
    'Citibank del Perú',
    'Banco de Comercio',
    'ICBC Perú Bank',
    'Alfin Banco',
    'Banco Efectiva',
    'Bank of China (Perú)',
];

async function cargarMetodosPago() {
    const cont = document.getElementById('metodosPagoContainer');
    if (!cont) return;
    try {
        const data = await api.get('/propietario/metodos-pago');
        metodosPagoCache = data ?? [];
        renderMetodosPago();
    } catch (e) {
        cont.innerHTML = `<div class="empty-state"><h4>Error al cargar métodos de pago</h4></div>`;
    }
}

function renderMetodosPago() {
    const cont = document.getElementById('metodosPagoContainer');
    if (!cont) return;

    cont.innerHTML = `<div class="mp-grid">${MP_TIPOS.map(tipo => renderTarjetaMetodoPago(tipo)).join('')}</div>`;
}

function renderTarjetaMetodoPago(tipo) {
    const def = MP_DEF[tipo];
    const m = metodosPagoCache.find(x => x.tipo === tipo);
    const configurado = !!m;
    const activo = configurado && m.activo;

    const datosHtml = configurado
        ? (tipo === 'TRANSFERENCIA'
            ? `<div class="mp-datos">
                <div class="mp-dato"><span>Banco</span><strong>${m.banco ?? '—'}</strong></div>
                <div class="mp-dato"><span>Cuenta</span><strong>${m.numeroCuenta ?? '—'}</strong></div>
                ${m.cci ? `<div class="mp-dato"><span>CCI</span><strong>${m.cci}</strong></div>` : ''}
                <div class="mp-dato"><span>Titular</span><strong>${m.titularCuenta ?? '—'}</strong></div>
              </div>`
            : `<div class="mp-datos">
                <div class="mp-dato"><span>Número</span><strong>${m.numeroTelefono ?? '—'}</strong></div>
                <div class="mp-dato"><span>Titular</span><strong>${m.nombreTitular ?? '—'}</strong></div>
              </div>`)
        : `<div class="mp-datos"><div class="mp-dato" style="justify-content:center;color:var(--text3)">Sin configurar</div></div>`;

    return `<div class="mp-card ${activo ? 'mp-activa' : ''}">
        <div class="mp-card-top">
            <div class="mp-icon" style="background:${def.color}20;color:${def.color}">${def.icon}</div>
            <div class="mp-info">
                <div class="mp-label">${def.label}</div>
                <div class="mp-status">${
        !configurado
            ? '<span class="badge badge-gray">No configurado</span>'
            : activo
                ? '<span class="badge badge-success">Activo</span>'
                : '<span class="badge badge-gray">Inactivo</span>'
    }</div>
            </div>
            ${configurado ? `
            <label class="toggle-switch" title="${activo ? 'Desactivar' : 'Activar'}">
                <input type="checkbox" ${activo ? 'checked' : ''} onchange="toggleMetodoPago(${m.id})"/>
                <span class="toggle-slider"></span>
            </label>` : ''}
        </div>
        ${datosHtml}
        <div class="mp-actions">
            <button class="btn btn-secondary btn-sm" onclick="abrirModalMetodoPago('${tipo}')">
                ${configurado ? 'Editar' : 'Configurar'}
            </button>
            ${configurado ? `<button class="btn btn-danger btn-sm" onclick="eliminarMetodoPago(${m.id}, '${def.label}')">Eliminar</button>` : ''}
        </div>
    </div>`;
}

function abrirModalMetodoPago(tipo) {
    metodoPagoEditando = tipo;
    const def = MP_DEF[tipo];
    const m = metodosPagoCache.find(x => x.tipo === tipo) ?? {};

    const title = document.getElementById('modalMetodoPagoTitle');
    if (title) title.textContent = `Configurar ${def.label}`;

    const body = document.getElementById('modalMetodoPagoBody');
    if (!body) return;

    if (tipo === 'TRANSFERENCIA') {
        body.innerHTML = `
            <div class="form-group">
                <label class="form-label">Banco *</label>
                <select class="form-select" id="mpBanco">
                    <option value="">Selecciona un banco...</option>
                    ${BANCOS_PERU.map(b => `<option value="${b}" ${m.banco === b ? 'selected' : ''}>${b}</option>`).join('')}
                </select>
            </div>
            <div class="form-group">
                <label class="form-label">Número de cuenta *</label>
                <input class="form-input" id="mpNumeroCuenta" value="${m.numeroCuenta ?? ''}" placeholder="Número de cuenta" maxlength="20" inputmode="numeric"/>
            </div>
            <div class="form-group">
                <label class="form-label">CCI *</label>
                <input class="form-input" id="mpCci" value="${m.cci ?? ''}" placeholder="Código de cuenta interbancario (20 dígitos)" maxlength="20" inputmode="numeric"/>
            </div>
            <div class="form-group">
                <label class="form-label">Titular de la cuenta *</label>
                <input class="form-input" id="mpTitularCuenta" value="${m.titularCuenta ?? ''}" placeholder="Nombre completo del titular"/>
            </div>`;
    } else {
        body.innerHTML = `
            <div class="form-group">
                <label class="form-label">Número de teléfono *</label>
                <input class="form-input" id="mpNumeroTelefono" value="${m.numeroTelefono ?? ''}" placeholder="Número asociado a ${def.label}"/>
            </div>
            <div class="form-group">
                <label class="form-label">Nombre del titular *</label>
                <input class="form-input" id="mpNombreTitular" value="${m.nombreTitular ?? ''}" placeholder="Nombre completo del titular"/>
            </div>`;
    }

    openModal('modalMetodoPago');
}

async function guardarMetodoPago() {
    if (!metodoPagoEditando) return;
    const tipo = metodoPagoEditando;
    const btn = document.getElementById('btnGuardarMetodoPago');

    const payload = { tipo };
    if (tipo === 'TRANSFERENCIA') {
        payload.banco          = document.getElementById('mpBanco')?.value.trim();
        payload.numeroCuenta   = document.getElementById('mpNumeroCuenta')?.value.trim();
        payload.cci            = document.getElementById('mpCci')?.value.trim();
        payload.titularCuenta  = document.getElementById('mpTitularCuenta')?.value.trim();

        if (!payload.banco || !payload.numeroCuenta || !payload.cci || !payload.titularCuenta) {
            toast('Completa banco, número de cuenta, CCI y titular', 'error');
            return;
        }
        if (!/^\d{1,20}$/.test(payload.numeroCuenta)) {
            toast('El número de cuenta debe tener solo dígitos (máx. 20)', 'error');
            return;
        }
        if (!/^\d{20}$/.test(payload.cci)) {
            toast('El CCI debe tener exactamente 20 dígitos', 'error');
            return;
        }
    } else {
        payload.numeroTelefono = document.getElementById('mpNumeroTelefono')?.value.trim();
        payload.nombreTitular  = document.getElementById('mpNombreTitular')?.value.trim();

        if (!payload.numeroTelefono || !payload.nombreTitular) {
            toast('Completa número de teléfono y nombre del titular', 'error');
            return;
        }
    }

    try {
        if (btn) btn.disabled = true;
        await api.post('/propietario/metodos-pago', payload);
        toast('Método de pago guardado correctamente', 'success');
        closeModal('modalMetodoPago');
        await cargarMetodosPago();
    } catch (e) {
        toast(e.message || 'No se pudo guardar el método de pago', 'error');
    } finally {
        if (btn) btn.disabled = false;
    }
}

async function toggleMetodoPago(id) {
    try {
        await api.patch(`/propietario/metodos-pago/${id}/toggle`);
        await cargarMetodosPago();
    } catch (e) {
        toast(e.message || 'No se pudo actualizar el método de pago', 'error');
    }
}

async function eliminarMetodoPago(id, label) {
    if (!confirm(`¿Eliminar el método de pago "${label}"? Esta acción no se puede deshacer.`)) return;
    try {
        await api.del(`/propietario/metodos-pago/${id}`);
        toast('Método de pago eliminado', 'success');
        await cargarMetodosPago();
    } catch (e) {
        toast(e.message || 'No se pudo eliminar el método de pago', 'error');
    }
}
/* ── LIGHTBOX (ver imagen sin salir de la página) ─────────────── */
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