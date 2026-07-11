'use strict';

function requireCliente() { return requireAuth('CLIENTE'); }

/* ── NAVBAR LOADER ─────────────────────────────────────────── */
async function loadNavbar() {
    const container = document.getElementById('navbar-container');
    if (!container) return;
    try {
        const res  = await fetch('/cliente/shared/navbar.html');
        const html = await res.text();
        container.innerHTML = html;
        setActiveNavTab();
        fillNavUser();
        initNavDropdown();
    } catch (e) {
        console.error('Error cargando navbar:', e);
    }
}

function setActiveNavTab() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-tab[href]').forEach(a => {
        a.classList.toggle('active', path.startsWith(a.getAttribute('href')));
    });
}

function fillNavUser() {
    const user = getUser();
    if (!user) return;
    const el = id => document.getElementById(id);
    const nombre = `${user.nombres} ${user.apellidos}`;
    if (el('navAvatar')) el('navAvatar').textContent = user.nombres[0].toUpperCase();
    if (el('navUserName')) el('navUserName').textContent = user.nombres;
    if (el('dropdownName'))  el('dropdownName').textContent  = nombre;
    if (el('dropdownEmail')) el('dropdownEmail').textContent = user.email;
}

function initNavDropdown() {
    const wrap = document.getElementById('navUserWrap');
    const btn  = document.getElementById('navUserBtn');
    if (!wrap || !btn) return;
    btn.addEventListener('click', (e) => { e.stopPropagation(); wrap.classList.toggle('open'); });
    document.addEventListener('click', () => wrap.classList.remove('open'));
}

/** Suma badges de "Mis Reservas" en el nav (lo llama cada página tras cargar sus datos). */
function setNavBadgeReservas(n) {
    const b = document.getElementById('navBadgeReservas');
    if (!b) return;
    b.textContent = n;
    b.style.display = n > 0 ? 'inline-flex' : 'none';
}

/* ── INIT PAGE ─────────────────────────────────────────────── */
async function initPage() {
    if (!requireCliente()) return false;
    await loadNavbar();
    initNotificacionesCliente();
    actualizarNotifsCliente();
    setInterval(actualizarNotifsCliente, 20000);
    return true;
}

/* ══════════════════════════════════════════════════════════════
   NOTIFICACIONES (campanita) — aprobada / rechazada / cancelada
   ══════════════════════════════════════════════════════════════ */
function pedirPermisoNotificacionesCliente() {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'default') Notification.requestPermission();
}

function claveUltimaNotifCliente() {
    const user = getUser();
    return `ss_last_notif_id_${user ? user.id : 'anon'}`;
}

function iconoNotifCliente(tipo) {
    if (tipo === 'RESERVA_APROBADA')
        return `<span style="display:inline-flex;width:32px;height:32px;border-radius:50%;background:#f0fdf4;
                color:#16a34a;align-items:center;justify-content:center;flex-shrink:0;font-size:15px;">✅</span>`;
    if (tipo === 'RESERVA_RECHAZADA')
        return `<span style="display:inline-flex;width:32px;height:32px;border-radius:50%;background:#fef2f2;
                color:#dc2626;align-items:center;justify-content:center;flex-shrink:0;font-size:15px;">❌</span>`;
    return `<span style="display:inline-flex;width:32px;height:32px;border-radius:50%;background:#fffbeb;
            color:#d97706;align-items:center;justify-content:center;flex-shrink:0;font-size:15px;">⚠️</span>`;
}

function tiempoRelativoCliente(fechaISO) {
    if (!fechaISO) return '';
    const f = Array.isArray(fechaISO)
        ? new Date(fechaISO[0], fechaISO[1]-1, fechaISO[2], fechaISO[3]||0, fechaISO[4]||0)
        : new Date(fechaISO);
    const seg = Math.floor((Date.now() - f.getTime()) / 1000);
    if (seg < 60)    return 'hace un momento';
    if (seg < 3600)  return `hace ${Math.floor(seg/60)} min`;
    if (seg < 86400) return `hace ${Math.floor(seg/3600)} h`;
    return `hace ${Math.floor(seg/86400)} d`;
}

function lanzarNotificacionesNuevasCliente(lista) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    if (!lista.length) return;

    const clave = claveUltimaNotifCliente();
    const ultimoVisto = Number(localStorage.getItem(clave) || 0);
    const nuevas = lista.filter(n => n.id > ultimoVisto);

    if (ultimoVisto > 0) {
        nuevas.slice(0, 5).forEach(n => {
            try {
                const notif = new Notification(n.titulo, {
                    body: n.mensaje,
                    icon: '/favicon.png',
                    tag: 'sportspace-notif-' + n.id,
                });
                notif.onclick = () => { window.focus(); clickNotifCliente(n.id); };
            } catch (e) {}
        });
    }
    const maxId = Math.max(ultimoVisto, ...lista.map(n => n.id));
    localStorage.setItem(clave, String(maxId));
}

function initNotificacionesCliente() {
    const btn = document.querySelector('.notif-btn');
    const wrap = document.getElementById('notifWrapCliente');
    if (!btn || !wrap || document.getElementById('notifDropdownCliente')) return;

    pedirPermisoNotificacionesCliente();

    btn.insertAdjacentHTML('beforeend',
        `<span id="notifBadgeCountCliente" style="display:none;position:absolute;top:-4px;right:-4px;
             background:#dc2626;color:#fff;border-radius:10px;min-width:16px;height:16px;
             font-size:10px;font-weight:700;line-height:16px;text-align:center;padding:0 4px;"></span>`);

    const panel = document.createElement('div');
    panel.id = 'notifDropdownCliente';
    panel.style.cssText = `display:none;position:absolute;top:46px;right:0;width:320px;max-height:420px;
        overflow-y:auto;background:#fff;border-radius:12px;box-shadow:0 12px 32px rgba(0,0,0,.18);
        border:1px solid #e2e8f0;z-index:2000;`;
    wrap.appendChild(panel);

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const abierto = panel.style.display === 'block';
        panel.style.display = abierto ? 'none' : 'block';
        if (!abierto) actualizarNotifsCliente();
    });
    document.addEventListener('click', (e) => {
        if (!panel.contains(e.target) && e.target !== btn) panel.style.display = 'none';
    });
}

async function actualizarNotifsCliente() {
    const panel = document.getElementById('notifDropdownCliente');
    const count = document.getElementById('notifBadgeCountCliente');
    if (!panel) return;

    try {
        const data = await api.get('/cliente/notificaciones');
        const lista = data.notificaciones || [];
        const noLeidas = data.noLeidas || 0;

        lanzarNotificacionesNuevasCliente(lista);

        if (count) {
            count.style.display = noLeidas > 0 ? 'block' : 'none';
            count.textContent = noLeidas > 9 ? '9+' : noLeidas;
        }

        if (lista.length === 0) {
            panel.innerHTML = `<div style="padding:28px 20px;text-align:center;color:#94a3b8;font-size:13px">
                Sin notificaciones por ahora.</div>`;
            return;
        }

        panel.innerHTML = `
            <div style="display:flex;justify-content:space-between;align-items:center;padding:14px 16px;
                 border-bottom:1px solid #f1f5f9;">
                <strong style="font-size:14px">Notificaciones</strong>
                ${noLeidas > 0 ? `<button onclick="marcarTodasNotifsClienteLeidas()"
                    style="border:none;background:none;color:#2563eb;font-size:12px;cursor:pointer;font-weight:600">
                    Marcar todas leídas</button>` : ''}
            </div>
            ${lista.map(n => `
                <div onclick="clickNotifCliente(${n.id})"
                     style="display:flex;gap:10px;padding:12px 16px;cursor:pointer;border-bottom:1px solid #f8fafc;
                     background:${n.leida ? '#fff' : '#f8faff'}">
                    ${iconoNotifCliente(n.tipo)}
                    <div style="min-width:0">
                        <div style="font-size:13px;font-weight:700;color:#1e293b">${n.titulo}</div>
                        <div style="font-size:12.5px;color:#64748b;line-height:1.4;margin-top:2px">${n.mensaje}</div>
                        <div style="font-size:11px;color:#94a3b8;margin-top:4px">${tiempoRelativoCliente(n.createdAt)}</div>
                    </div>
                    ${!n.leida ? '<span style="width:8px;height:8px;border-radius:50%;background:#2563eb;flex-shrink:0;margin-top:4px"></span>' : ''}
                </div>`).join('')}
        `;
    } catch (e) {
        console.error('Error cargando notificaciones:', e);
    }
}

async function marcarTodasNotifsClienteLeidas() {
    try { await api.patch('/cliente/notificaciones/marcar-todas-leidas'); } catch {}
    actualizarNotifsCliente();
}

async function clickNotifCliente(id) {
    try { await api.patch(`/cliente/notificaciones/${id}/leida`); } catch {}
    window.location.href = '/cliente/reservas';
}

/* ── FORMATO ───────────────────────────────────────────────────
   formatDate() y formatCurrency() ya existen en api.js — se usan tal cual.
   Aquí solo se agrega lo que falta para el flujo de cliente. ───────── */
function fmtFechaLarga(str) {
    if (!str) return '—';
    try {
        const d = new Date(str + 'T12:00:00');
        if (isNaN(d)) return str;
        let txt = d.toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
        return txt.charAt(0).toUpperCase() + txt.slice(1);
    } catch { return str; }
}

function fmtHora(val) {
    if (!val) return '—';
    if (Array.isArray(val)) {
        const [h, m] = val;
        return `${String(h).padStart(2,'0')}:${String(m).padStart(2,'0')}`;
    }
    return String(val).substring(0, 5);
}

function badgeEstadoReserva(e) {
    // Mismas clases de shared.css usadas por admin/propietario (badge-pending, badge-success, badge-inactive, badge-danger)
    const map = {
        PENDIENTE:  ['pending',  '⏳ Pendiente de pago'],
        CONFIRMADA: ['success',  '✓ Confirmada'],
        COMPLETADA: ['inactive', 'Completada'],
        CANCELADA:  ['danger',   'Cancelada'],
    };
    const [cls, lbl] = map[e] || ['inactive', e];
    return `<span class="badge badge-${cls}">${lbl}</span>`;
}

function badgeEstadoPago(e) {
    const map = {
        PENDIENTE:    ['pending', '⏳ En revisión'],
        COMPLETADO:   ['success', '✓ Verificado'],
        RECHAZADO:    ['danger',  'Rechazado'],
        REEMBOLSADO:  ['info',    'Reembolsado'],
    };
    const [cls, lbl] = map[e] || ['inactive', e];
    return `<span class="badge badge-${cls}">${lbl}</span>`;
}

function iconoMetodoPago(m) {
    return { EFECTIVO: '💵', TRANSFERENCIA: '🏦', YAPE: '📲', PLIN: '📲' }[m] || '💳';
}

/** true/false si dos rangos "HH:mm" se superponen */
function rangosSeSuperponen(aIni, aFin, bIni, bFin) {
    return aIni < bFin && bIni < aFin;
}

/* ── ICONO / TEMA / ETIQUETA POR DEPORTE ──────────────────────
   El campo "deporte" en la base de datos es uno de estos 5 valores fijos
   (definidos en el formulario de creación de canchas del propietario):
   FUTBOL, BASQUETBOL, VOLEIBOL, TENIS, PADEL ─────────────────── */
const DEPORTE_MAP = {
    FUTBOL:     { emoji: '⚽',  theme: 'theme-f7',   label: 'Fútbol'  },
    BASQUETBOL: { emoji: '🏀',  theme: 'theme-bask', label: 'Básquet' },
    VOLEIBOL:   { emoji: '🏐',  theme: 'theme-vol',  label: 'Vóley'   },
    TENIS:      { emoji: '🎾',  theme: 'theme-ten',  label: 'Tenis'   },
    PADEL:      { emoji: '🏸',  theme: 'theme-pad',  label: 'Pádel'   },
};
function iconoDeporte(deporte) {
    return DEPORTE_MAP[(deporte || '').toUpperCase()] || { emoji: '🏅', theme: 'theme-otro', label: deporte || 'Deporte' };
}
function labelDeporte(deporte) { return iconoDeporte(deporte).label; }

/* ── UI HELPERS ────────────────────────────────────────────── */
function loadingGrid(msg = 'Cargando canchas...') {
    return `<div class="loading-state" style="grid-column:1/-1"><div class="spinner"></div> ${msg}</div>`;
}
function emptyGrid(title, msg, ctaHtml = '') {
    return `<div class="empty-state" style="grid-column:1/-1">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <h4>${title}</h4><p>${msg}</p>${ctaHtml}
    </div>`;
}