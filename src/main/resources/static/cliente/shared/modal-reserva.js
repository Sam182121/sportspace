'use strict';
/* MODAL RESERVA v2 */

let _cancha       = null;
let _slotSel      = null;          // { inicio:'HH:mm', fin:'HH:mm' }
let _fechaSel     = null;          // 'YYYY-MM-DD'
let _calMes       = new Date();    // mes mostrado en el calendario
let _metodosProp  = [];            // métodos de pago del propietario
let _voucherBase64= null;          // comprobante en base64
let _carIndex     = 0;             // foto actual del carrusel
let _fotos        = [];

/*  ABRIR  */
async function abrirModalReserva(canchaId, canchaCache) {
    _cancha = null; _slotSel = null; _fechaSel = null;
    _voucherBase64 = null; _carIndex = 0;

    document.getElementById('modalReservaOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
    mGoTo('info', false);
    document.getElementById('mNombre').textContent = 'Cargando…';

    try {
        _cancha = canchaCache || await api.get(`/canchas/publico/${canchaId}`);
        poblarInfo();
    } catch(e) {
        toast('No se pudo cargar la cancha', 'error');
        cerrarModalReserva();
    }
}

/* CARRUSEL DE FOTOS  */
function renderCarrusel() {
    _fotos = (_cancha.fotos && _cancha.fotos.length) ? _cancha.fotos : [];
    const track = document.getElementById('mCarouselTrack');
    const dots  = document.getElementById('mCarouselDots');
    const car   = document.getElementById('mCarousel');
    const { theme } = iconoDeporte(_cancha.deporte || '');
    car.className = 'modal-carousel ' + theme;

    if (!_fotos.length) {
        track.innerHTML = `<div class="carousel-placeholder">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>
            <span>Sin fotos disponibles</span>
        </div>`;
        dots.innerHTML = '';
        document.getElementById('mArrowPrev').style.display = 'none';
        document.getElementById('mArrowNext').style.display = 'none';
        document.getElementById('mZoomBtn').style.display   = 'none';
        return;
    }

    track.innerHTML = _fotos.map(f => `<img src="${f}" alt="${_cancha.nombre}" class="carousel-img"/>`).join('');
    dots.innerHTML  = _fotos.map((_, i) => `<span class="car-dot ${i===0?'active':''}" onclick="carruselIrA(${i})"></span>`).join('');

    const multi = _fotos.length > 1;
    document.getElementById('mArrowPrev').style.display = multi ? 'flex' : 'none';
    document.getElementById('mArrowNext').style.display = multi ? 'flex' : 'none';
    document.getElementById('mZoomBtn').style.display    = 'flex';
    carruselIrA(0);
}

function carruselIrA(i) {
    if (!_fotos.length) return;
    _carIndex = (i + _fotos.length) % _fotos.length;
    const track = document.getElementById('mCarouselTrack');
    track.style.transform = `translateX(-${_carIndex * 100}%)`;
    document.querySelectorAll('#mCarouselDots .car-dot').forEach((d, idx) =>
        d.classList.toggle('active', idx === _carIndex));
    const lb = document.getElementById('lightboxOverlay');
    if (lb.classList.contains('open')) {
        document.getElementById('lightboxImg').src = _fotos[_carIndex];
    }
}
function carruselMover(dir) { carruselIrA(_carIndex + dir); }

function abrirZoom() {
    if (!_fotos.length) return;
    document.getElementById('lightboxImg').src = _fotos[_carIndex];
    document.getElementById('lightboxOverlay').classList.add('open');
}
function cerrarZoom(e) {
    if (e && e.target !== document.getElementById('lightboxOverlay')) return;
    document.getElementById('lightboxOverlay').classList.remove('open');
}

/* POBLAR INFO  */
function poblarInfo() {
    const c = _cancha;
    const { emoji, label } = iconoDeporte(c.deporte || '');

    renderCarrusel();

    document.getElementById('mNombre').textContent      = c.nombre;
    document.getElementById('mDeporteBadge').textContent= `${emoji} ${label}`;
    document.getElementById('mCapacidad').textContent   = c.capacidad ? `${c.capacidad} personas` : '';
    document.getElementById('mPrecio').textContent      = `S/. ${Number(c.precioPorHora||0).toFixed(2)}`;
    document.getElementById('mDireccion').textContent   = c.direccion || '—';
    document.getElementById('mDistrito').textContent    = [c.distrito, c.provincia, c.departamento].filter(Boolean).join(', ') || '—';
    document.getElementById('mPropietario').textContent = c.propietarioNombre || '—';
    document.getElementById('mContacto').textContent    = c.propietarioTelefono || c.propietarioEmail || '—';
    document.getElementById('mDescripcion').textContent = c.descripcion || '';

    const tags = [];
    if (c.deporte)   tags.push(`${emoji} ${label}`);
    if (c.distrito)  tags.push(`📍 ${c.distrito}`);
    if (c.capacidad) tags.push(`👥 ${c.capacidad} pers.`);
    document.getElementById('mTags').innerHTML = tags.map(t => `<span class="m-tag">${t}</span>`).join('');

    // Reset calendario al mes actual
    _calMes = new Date();
    _fechaSel = null; _slotSel = null;
    document.getElementById('mHorasWrap').innerHTML =
        `<p style="font-size:12.5px;color:var(--text3);grid-column:1/-1">Selecciona una fecha en el calendario.</p>`;
    document.getElementById('mSelResumen').style.display = 'none';
    document.getElementById('mBtnIrPago').disabled = true;
    renderCalendario();

    document.getElementById('prCancha').textContent = c.nombre;
    document.getElementById('prTotal').textContent  = `S/. ${Number(c.precioPorHora||0).toFixed(2)}`;
}

/* PASOS */
const PASOS = ['info', 'horario', 'pago', 'listo'];

function mGoTo(paso, animar = true) {
    PASOS.forEach(p => {
        const el = document.getElementById(`mstep-${p}`);
        if (el) el.style.display = p === paso ? 'block' : 'none';
    });
    actualizarSteps(paso);
    if (paso === 'pago') prepararPago();
}

function actualizarSteps(activo) {
    const idx = PASOS.indexOf(activo);
    PASOS.forEach((p, i) => {
        const step = document.getElementById(`mStep${i + 1}`);
        const line = document.getElementById(`mLine${i + 1}`);
        if (!step) return;
        step.className = 'step';
        if (i < idx)        { step.classList.add('done');   if (line) line.className = 'step-line done'; }
        else if (i === idx) { step.classList.add('active'); if (line) line.className = 'step-line'; }
        else                { if (line) line.className = 'step-line'; }
    });
}

/* CALENDARIO MENSUAL */
const MESES_ES = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre'];

function calCambiarMes(delta) {
    _calMes = new Date(_calMes.getFullYear(), _calMes.getMonth() + delta, 1);
    renderCalendario();
}

function renderCalendario() {
    const year  = _calMes.getFullYear();
    const month = _calMes.getMonth();
    document.getElementById('calMonthLabel').textContent = `${MESES_ES[month]} ${year}`;

    const hoy = new Date(); hoy.setHours(0,0,0,0);
    const noPermitirMesAnterior =
        (year < hoy.getFullYear()) || (year === hoy.getFullYear() && month <= hoy.getMonth());
    document.getElementById('calPrevBtn').disabled = noPermitirMesAnterior;
    document.getElementById('calPrevBtn').style.opacity = noPermitirMesAnterior ? '.3' : '1';
    document.getElementById('calPrevBtn').style.cursor = noPermitirMesAnterior ? 'not-allowed' : 'pointer';

    const primerDia = new Date(year, month, 1);
    // Lunes=0 ... Domingo=6
    let offset = primerDia.getDay() - 1;
    if (offset < 0) offset = 6;

    const diasEnMes = new Date(year, month + 1, 0).getDate();
    const grid = document.getElementById('calGrid');
    let html = '';

    for (let i = 0; i < offset; i++) html += `<span class="cal-day empty"></span>`;

    for (let d = 1; d <= diasEnMes; d++) {
        const fechaObj = new Date(year, month, d); fechaObj.setHours(0,0,0,0);
        const fechaStr = `${year}-${String(month+1).padStart(2,'0')}-${String(d).padStart(2,'0')}`;
        const esPasado = fechaObj < hoy;
        const esHoy    = fechaObj.getTime() === hoy.getTime();
        const esSel    = fechaStr === _fechaSel;
        const cls = ['cal-day'];
        if (esPasado) cls.push('disabled');
        if (esHoy)    cls.push('today');
        if (esSel)    cls.push('selected');
        html += `<button class="${cls.join(' ')}" ${esPasado ? 'disabled' : `onclick="calSeleccionarFecha('${fechaStr}', this)"`}>${d}</button>`;
    }
    grid.innerHTML = html;
}

function calSeleccionarFecha(fechaStr, btn) {
    document.querySelectorAll('.cal-day.selected').forEach(b => b.classList.remove('selected'));
    if (btn) btn.classList.add('selected');
    _fechaSel = fechaStr;
    _slotSel  = null;
    document.getElementById('mSelResumen').style.display = 'none';
    document.getElementById('mBtnIrPago').disabled = true;
    cargarDisponibilidad();
}

/* DISPONIBILIDAD (tipo cine) */
async function cargarDisponibilidad() {
    if (!_fechaSel || !_cancha) return;
    const wrap = document.getElementById('mHorasWrap');
    wrap.innerHTML = `<div class="loading-state" style="grid-column:1/-1;padding:16px"><div class="spinner"></div> Consultando disponibilidad…</div>`;

    try {
        const disp = await api.get(`/reservas/disponibilidad/${_cancha.id}?fecha=${_fechaSel}`);

        if (disp.bloqueada) {
            wrap.innerHTML = `<p style="font-size:12.5px;color:var(--danger);grid-column:1/-1;padding:8px 0">
                No disponible esta fecha${disp.motivoBloqueo ? ': ' + disp.motivoBloqueo : '.'}
            </p>`;
            return;
        }

        const slots = disp.slots || [];
        if (!slots.length) {
            wrap.innerHTML = `<p style="font-size:12.5px;color:var(--text3);grid-column:1/-1;padding:8px 0">
                El propietario no configuró horarios para este día.
            </p>`;
            return;
        }

        wrap.innerHTML = slots.map(s => {
            const ini = fmtHora(s.inicio);
            const fin = fmtHora(s.fin);
            if (!s.disponible) {
                return `<button class="hora-btn ocupada" disabled>${ini}</button>`;
            }
            return `<button class="hora-btn" onclick="selSlot(this,'${ini}','${fin}')">${ini}</button>`;
        }).join('');
    } catch(e) {
        wrap.innerHTML = `<p style="font-size:12.5px;color:var(--danger);grid-column:1/-1">Error al consultar disponibilidad.</p>`;
        console.error(e);
    }
}

function selSlot(btn, ini, fin) {
    document.querySelectorAll('#mHorasWrap .hora-btn').forEach(b => b.classList.remove('selected'));
    btn.classList.add('selected');
    _slotSel = { inicio: ini, fin: fin };
    document.getElementById('mBtnIrPago').disabled = false;

    const resEl = document.getElementById('mSelResumen');
    resEl.style.display = 'flex';
    document.getElementById('mResumenTexto').textContent = `${fmtFechaLarga(_fechaSel)} · ${ini} – ${fin}`;

    document.getElementById('prFecha').textContent = fmtFechaLarga(_fechaSel);
    document.getElementById('prHora').textContent  = `${ini} – ${fin}`;
    document.getElementById('prTotal').textContent = `S/. ${Number(_cancha.precioPorHora||0).toFixed(2)}`;
}

/*  PASO PAGO  */
async function prepararPago() {
    const sel = document.getElementById('selMetodoPago');
    sel.innerHTML = `<option value="">Cargando métodos…</option>`;
    document.getElementById('metodosPagoWrap').innerHTML = '';
    document.getElementById('voucherGroup').style.display = 'none';
    document.getElementById('mBtnConfirmar').disabled = true;
    document.getElementById('mPagoError').style.display = 'none';
    resetVoucher();

    try {
        _metodosProp = await api.get(`/canchas/${_cancha.id}/metodos-pago`);
        sel.innerHTML = `<option value="">Selecciona un método…</option>`;
        _metodosProp.forEach(m => {
            const lbl = { TRANSFERENCIA: '🏦 Transferencia bancaria', YAPE: '📲 Yape', PLIN: '📲 Plin' }[m.tipo] || m.tipo;
            sel.innerHTML += `<option value="${m.tipo}">${lbl}</option>`;
        });
        sel.innerHTML += `<option value="EFECTIVO">💵 Efectivo (al llegar)</option>`;
    } catch(e) {
        sel.innerHTML = `<option value="EFECTIVO">💵 Efectivo (al llegar)</option>`;
        console.error(e);
    }
}

function onMetodoPagoChange() {
    const metodo = document.getElementById('selMetodoPago').value;
    const wrap   = document.getElementById('metodosPagoWrap');
    const vGroup = document.getElementById('voucherGroup');

    if (!metodo) { wrap.innerHTML = ''; vGroup.style.display = 'none'; actualizarBtnConfirmar(); return; }

    if (metodo === 'EFECTIVO') {
        wrap.innerHTML = `
        <div style="background:var(--bg3);border:1px solid var(--border);border-radius:var(--radius2);padding:12px 14px;font-size:13px;color:var(--text2);margin-bottom:8px">
            <strong>Pago al llegar</strong><br/>
            <span style="color:var(--text3);font-size:12px">Lleva el monto exacto en efectivo a la cancha.</span>
        </div>`;
        vGroup.style.display = 'none';
        _voucherBase64 = null;
    } else {
        const m = _metodosProp.find(x => x.tipo === metodo);
        if (metodo === 'TRANSFERENCIA' && m) {
            wrap.innerHTML = `
            <div style="background:var(--text);border-radius:var(--radius);padding:14px 16px;margin-bottom:8px">
                <div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:rgba(255,255,255,.4);margin-bottom:10px">🏦 Datos de transferencia</div>
                <div style="display:flex;justify-content:space-between;font-size:12.5px;color:rgba(255,255,255,.55);margin-bottom:4px"><span>Banco</span><strong style="color:#fff">${m.banco || '—'}</strong></div>
                <div style="display:flex;justify-content:space-between;font-size:12.5px;color:rgba(255,255,255,.55);margin-bottom:4px"><span>N° Cuenta</span><strong style="color:#fff">${m.numeroCuenta || '—'}</strong></div>
                ${m.cci ? `<div style="display:flex;justify-content:space-between;font-size:12.5px;color:rgba(255,255,255,.55);margin-bottom:4px"><span>CCI</span><strong style="color:#fff">${m.cci}</strong></div>` : ''}
                <div style="display:flex;justify-content:space-between;font-size:12.5px;color:rgba(255,255,255,.55)"><span>Titular</span><strong style="color:#fff">${m.titular || '—'}</strong></div>
            </div>`;
        } else if (m) {
            const icon = metodo === 'YAPE' ? '📲 Yape' : '📲 Plin';
            wrap.innerHTML = `
            <div style="background:var(--success-bg);border:1px solid rgba(14,159,110,.2);border-radius:var(--radius);padding:12px 14px;margin-bottom:8px">
                <div style="font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:var(--success);margin-bottom:8px">${icon}</div>
                <div style="font-size:13px;color:var(--text2)">Número: <strong>${m.numeroTelefono || '—'}</strong></div>
                <div style="font-size:12px;color:var(--text3);margin-top:2px">A nombre de: ${m.titular || '—'}</div>
            </div>`;
        } else {
            wrap.innerHTML = '';
        }
        vGroup.style.display = 'block';
    }
    actualizarBtnConfirmar();
}

/*  VOUCHER: se SUBE al servidor (disco) */
function handleVoucherFile(e) {
    const f = e.target.files[0];
    if (f) leerVoucher(f);
}
function handleVoucherDrop(e) {
    e.preventDefault();
    document.getElementById('voucherZone').classList.remove('drag');
    const f = e.dataTransfer.files[0];
    if (f) leerVoucher(f);
}
async function leerVoucher(file) {
    if (!file.type.startsWith('image/')) {
        toast('Solo se permiten imágenes (JPG, PNG o WEBP). PDF, videos u otros archivos no están permitidos.', 'error');
        return;
    }
    if (file.size > 5 * 1024 * 1024) { toast('El archivo supera 5 MB', 'error'); return; }

    const wrap = document.getElementById('voucherPreviewWrap');
    wrap.innerHTML = `<div class="voucher-text">Subiendo comprobante…</div>`;

    try {
        const url = await uploadToCloudinary(file, 'vouchers');
        _voucherBase64 = url; // ahora contiene la URL del archivo, no base64
        wrap.innerHTML = `
            ${file.type.startsWith('image/')
            ? `<img src="${url}" style="max-height:120px;border-radius:6px;margin-bottom:8px"/>`
            : ''}
            <div class="voucher-text"><strong style="color:var(--success)">${file.name}</strong><br/><small style="color:var(--success)">Listo para enviar ✓</small></div>`;
        const vz = document.getElementById('voucherZone');
        vz.style.borderColor = 'var(--success)'; vz.style.background = 'var(--success-bg)';
    } catch (e) {
        _voucherBase64 = null;
        wrap.innerHTML = `<div class="voucher-text" style="color:var(--danger,#e11)">Error al subir. Intenta de nuevo.</div>`;
        toast(e.message || 'Error al subir el comprobante', 'error');
    }
    actualizarBtnConfirmar();
}
function resetVoucher() {
    _voucherBase64 = null;
    const wrap = document.getElementById('voucherPreviewWrap');
    if (wrap) wrap.innerHTML = `
        <div class="voucher-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 16 12 12 8 16"/><line x1="12" y1="12" x2="12" y2="21"/><path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"/></svg>
        </div>
        <div class="voucher-text" id="voucherLabel">Toca para subir tu comprobante<br/><small>JPG, PNG o PDF · Máx. 5 MB</small></div>`;
    const vz = document.getElementById('voucherZone');
    if (vz) { vz.style.borderColor = ''; vz.style.background = ''; }
    const input = document.getElementById('voucherInput');
    if (input) input.value = '';
}

function actualizarBtnConfirmar() {
    const metodo = document.getElementById('selMetodoPago').value;
    const necesitaVoucher = metodo && metodo !== 'EFECTIVO';
    const ok = metodo && (!necesitaVoucher || _voucherBase64);
    document.getElementById('mBtnConfirmar').disabled = !ok;
}

/* CONFIRMAR */
async function confirmarReserva() {
    if (!_cancha || !_slotSel || !_fechaSel) return;
    const metodo = document.getElementById('selMetodoPago').value;
    if (!metodo) { toast('Selecciona un método de pago', 'error'); return; }
    if (metodo !== 'EFECTIVO' && !_voucherBase64) {
        toast('Sube tu comprobante de pago', 'error'); return;
    }

    const btn = document.getElementById('mBtnConfirmar');
    const errEl = document.getElementById('mPagoError');
    btn.disabled = true;
    btn.textContent = 'Procesando…';
    errEl.style.display = 'none';

    try {
        const reserva = await api.post('/reservas', {
            canchaId:   _cancha.id,
            fecha:      _fechaSel,
            horaInicio: _slotSel.inicio,
            horaFin:    _slotSel.fin,
        });

        await api.post('/pagos', {
            reservaId:  reserva.id,
            metodo:     metodo,
            voucherUrl: _voucherBase64 || null,
        });

        mGoTo('listo');
        toast('Reserva enviada, pendiente de aprobación', 'success');

        const badge = document.getElementById('navBadgeReservas');
        if (badge) {
            const n = (parseInt(badge.textContent) || 0) + 1;
            badge.textContent = n;
            badge.style.display = 'inline-flex';
        }
    } catch(e) {
        btn.disabled = false;
        btn.innerHTML = `Confirmar reserva
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><polyline points="20 6 9 17 4 12"/></svg>`;
        errEl.style.display = 'block';
        errEl.innerHTML = `<div style="background:var(--danger-bg);border:1px solid var(--danger);border-radius:var(--radius2);padding:10px 12px;font-size:12.5px;color:var(--danger)">${e.message || 'Ocurrió un error. Intenta nuevamente.'}</div>`;
        console.error(e);
    }
}

/* CERRAR */
function cerrarModalReserva(e) {
    if (e && e.target !== document.getElementById('modalReservaOverlay')) return;
    document.getElementById('modalReservaOverlay').classList.remove('open');
    document.body.style.overflow = '';
}