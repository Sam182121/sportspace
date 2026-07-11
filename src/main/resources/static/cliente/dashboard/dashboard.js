'use strict';
/* ── DASHBOARD CLIENTE ──────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', async () => {
    if (!await initPage()) return;

    const user = getUser();
    const hora = new Date().getHours();
    const saludo = hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
    document.getElementById('welcomeMsg').textContent = `${saludo}, ${user.nombres}`;
    document.getElementById('fechaHoy').textContent = new Date().toLocaleDateString('es-PE',
        { weekday:'long', day:'numeric', month:'long', year:'numeric' });

    await Promise.all([loadStats(), loadSugeridas()]);
});

/* Convierte fecha+hora de una reserva a timestamp comparable */
function timestampReserva(r) {
    try { return new Date(`${r.fecha}T${fmtHora(r.horaInicio)}:00`).getTime(); }
    catch { return 0; }
}

/* ── STATS PERSONALES ───────────────────────────────────────── */
async function loadStats() {
    try {
        const reservas = await api.get('/reservas/mis-reservas');

        const activas    = reservas.filter(r => ['PENDIENTE','CONFIRMADA'].includes(r.estado)).length;
        const completadas= reservas.filter(r => r.estado === 'COMPLETADA').length;
        const gasto      = reservas
            .filter(r => r.estado === 'COMPLETADA')
            .reduce((s, r) => s + (Number(r.total) || 0), 0);

        document.getElementById('statActivas').textContent     = activas;
        document.getElementById('statCompletadas').textContent = completadas;
        document.getElementById('statGasto').textContent       = formatCurrency(gasto);

        setNavBadgeReservas(activas);

        const proximas = reservas
            .filter(r => ['PENDIENTE','CONFIRMADA'].includes(r.estado))
            .sort((a, b) => timestampReserva(a) - timestampReserva(b))
            .slice(0, 3);
        renderProximas(proximas);
    } catch (e) {
        document.getElementById('proximasWrap').innerHTML =
            `<p style="font-size:13px;color:var(--text3)">No se pudieron cargar las reservas.</p>`;
        console.error(e);
    }
}

function renderProximas(list) {
    const wrap = document.getElementById('proximasWrap');
    if (!list.length) {
        wrap.innerHTML = `
        <div class="empty-state" style="padding:28px">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>
            <h4>Sin reservas activas</h4>
            <p>¿Listo para jugar? Busca una cancha y reserva ahora.</p>
            <a href="/cliente/buscar" class="btn btn-primary btn-sm" style="margin-top:8px">Buscar cancha</a>
        </div>`;
        return;
    }
    const SVG_PIN = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`;
    const SVG_CAL = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M3 10h18"/></svg>`;
    const SVG_CLK = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>`;

    wrap.innerHTML = list.map(r => {
        const { emoji } = iconoDeporte(r.canchaDeporte || r.deporte || '');
        return `
        <div class="reserva-card">
            <div class="res-icon" style="background:var(--accent-light)">${emoji}</div>
            <div class="res-info">
                <h3>#${String(r.id).padStart(4,'0')} · ${r.canchaNombre || 'Cancha'} — ${labelDeporte(r.canchaDeporte || r.deporte || '')}</h3>
                <div class="res-meta">
                    <span>${SVG_CAL} ${formatDate(r.fecha)}</span>
                    <span>${SVG_CLK} ${fmtHora(r.horaInicio)} – ${fmtHora(r.horaFin)}</span>
                    <span>${SVG_PIN} ${r.canchaDistrito || ''}</span>
                </div>
                ${badgeEstadoReserva(r.estado)}
            </div>
            <div class="res-right">
                <div class="res-precio">${formatCurrency(r.total)}</div>
            </div>
        </div>`;
    }).join('');
}

/* ── CANCHAS SUGERIDAS ──────────────────────────────────────── */
async function loadSugeridas() {
    const grid = document.getElementById('sugeridosGrid');
    try {
        const data = await api.get('/canchas/publico');
        const canchas = Array.isArray(data) ? data.slice(0, 6) : (data.content || []).slice(0, 6);
        if (!canchas.length) {
            grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1"><h4>No hay canchas disponibles</h4></div>`;
            return;
        }
        grid.innerHTML = canchas.map(c => renderCanchaCard(c)).join('');
    } catch (e) {
        grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <h4>Error al cargar canchas</h4><p>Intenta refrescar la página.</p>
        </div>`;
        console.error(e);
    }
}

/* ── CARD DE CANCHA (sin emoji superpuesto, sin favoritos) ──── */
const PLACEHOLDER_SVG = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:34px;height:34px;color:rgba(0,0,0,.25)"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>`;

function renderCanchaCard(c) {
    const { theme, label: deporteLabel } = iconoDeporte(c.deporte || '');
    const PIN = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`;

    const fotoUrl = c.fotos && c.fotos.length ? c.fotos[0] : null;
    const thumbContent = fotoUrl
        ? `<img src="${fotoUrl}" alt="${c.nombre}" style="width:100%;height:100%;object-fit:cover;position:absolute;inset:0"/>`
        : `<div style="position:relative;z-index:1;display:flex;align-items:center;justify-content:center;width:100%;height:100%">${PLACEHOLDER_SVG}</div>`;

    return `
    <div class="cancha-card ${theme}" onclick="abrirModal(${c.id})" data-id="${c.id}">
        <div class="card-thumb" style="position:relative">
            <span class="card-sport-badge">${deporteLabel}</span>
            ${thumbContent}
        </div>
        <div class="card-name">${c.nombre}</div>
        <div class="card-location">${PIN} ${c.distrito || ''}, ${c.departamento || ''}</div>
        <div class="card-footer">
            <div class="card-price">S/. ${Number(c.precioPorHora || 0).toFixed(2)} <span>/ hora</span></div>
            <button class="btn btn-primary btn-sm" onclick="event.stopPropagation(); abrirModal(${c.id})">Reservar</button>
        </div>
    </div>`;
}

/* ── MODAL reserva ──────────────────────────────────────────── */
let modalReservaReady = false;
async function abrirModal(canchaId) {
    if (!modalReservaReady) {
        await loadModalReserva();
        modalReservaReady = true;
    }
    abrirModalReserva(canchaId);
}

async function loadModalReserva() {
    const res  = await fetch('/cliente/shared/modal-reserva.html');
    const html = await res.text();
    document.getElementById('modal-reserva-container').innerHTML = html;
    await new Promise((resolve, reject) => {
        const s = document.createElement('script');
        s.src = '/cliente/shared/modal-reserva.js';
        s.onload = resolve; s.onerror = reject;
        document.body.appendChild(s);
    });
}