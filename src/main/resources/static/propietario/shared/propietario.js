'use strict';

/* ── AUTH ──────────────────────────────────────────────────── */
function requirePropietario() {
    const token = getToken();
    const user  = getUser();
    if (!token || !user) { window.location.href = '/login'; return false; }
    if (user.rol !== 'PROPIETARIO') { window.location.href = '/login'; return false; }
    return true;
}

/* ── SIDEBAR LOADER ────────────────────────────────────────── */
async function loadSidebar() {
    const container = document.getElementById('sidebar-container');
    if (!container) return;
    try {
        const res  = await fetch('/propietario/shared/sidebar.html');
        const html = await res.text();
        container.innerHTML = html;
        setActiveNav();
        fillSidebarUser();
        initHamburger();
    } catch (e) {
        console.error('Error cargando sidebar:', e);
    }
}

function setActiveNav() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-item[href]').forEach(a => {
        a.classList.toggle('active', path.startsWith(a.getAttribute('href')));
    });
}

function fillSidebarUser() {
    const user = getUser();
    if (!user) return;
    const el = id => document.getElementById(id);
    const nombre = `${user.nombres} ${user.apellidos}`;
    if (el('sidebarAvatar')) el('sidebarAvatar').textContent = user.nombres[0].toUpperCase();
    if (el('sidebarNombre')) el('sidebarNombre').textContent = nombre;
    if (el('sidebarEmail'))  el('sidebarEmail').textContent  = user.email;
}

function initHamburger() {
    const btn = document.getElementById('hamburger');
    const sb  = document.getElementById('sidebar');
    if (btn && sb) btn.addEventListener('click', () => sb.classList.toggle('mobile-open'));
}

/* ── HEADER USER ───────────────────────────────────────────── */
function fillHeaderUser() {
    const user = getUser();
    if (!user) return;
    const el = id => document.getElementById(id);
    const nombre = `${user.nombres} ${user.apellidos}`;
    if (el('headerAvatar'))   el('headerAvatar').textContent   = user.nombres[0].toUpperCase();
    if (el('headerName'))     el('headerName').textContent     = nombre;
    if (el('headerRole'))     el('headerRole').textContent     = 'Propietario';
    if (el('dropdownName'))   el('dropdownName').textContent   = nombre;
    if (el('dropdownRole'))   el('dropdownRole').textContent   = 'Propietario';
    if (el('dropdownEmail'))  el('dropdownEmail').textContent  = user.email;
}

function initHeaderDropdown() {
    const wrap = document.getElementById('userDropdownWrap');
    const btn  = document.getElementById('headerUser');
    if (!wrap || !btn) return;
    btn.addEventListener('click', (e) => { e.stopPropagation(); wrap.classList.toggle('open'); });
    document.addEventListener('click', () => wrap.classList.remove('open'));
}

/* ── FORMATO ───────────────────────────────────────────────── */
function fmt(str) {
    if (!str) return '—';
    try {
        if (Array.isArray(str)) {
            const [y, mo, d, h = 0, mi = 0] = str;
            return `${String(d).padStart(2,'0')}/${String(mo).padStart(2,'0')}/${y} ${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}`;
        }
        const d = new Date(str);
        if (isNaN(d)) return str;
        return d.toLocaleDateString('es-PE', { day:'2-digit', month:'2-digit', year:'numeric' });
    } catch { return str; }
}

function fmtHora(str) {
    if (!str) return '—';
    try {
        if (Array.isArray(str)) {
            const [,, , h = 0, mi = 0] = str;
            return `${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}`;
        }
        const d = new Date(str);
        if (isNaN(d)) return str;
        return d.toLocaleTimeString('es-PE', { hour:'2-digit', minute:'2-digit' });
    } catch { return str; }
}

function fmtMoney(n) { return 'S/. ' + Number(n || 0).toFixed(2); }

function badgeEstado(e) {
    const map = {
        PENDIENTE:  ['warning',  'Pendiente'],
        CONFIRMADA: ['success',  'Confirmada'],
        CANCELADA:  ['danger',   'Cancelada'],
        COMPLETADA: ['success',  'Completada'],
        RECHAZADA:  ['danger',   'Rechazada'],
    };
    const [cls, lbl] = map[e] || ['gray', e];
    return `<span class="badge badge-${cls}">${lbl}</span>`;
}

function badgePago(m) {
    const map = { YAPE:'purple', PLIN:'success', TRANSFERENCIA:'info', EFECTIVO:'orange' };
    return `<span class="badge badge-${map[m] || 'gray'}">${m}</span>`;
}

function loadingRow(cols) {
    return `<tr><td colspan="${cols}" style="text-align:center;padding:32px">
        <div class="loading-state"><div class="spinner"></div> Cargando...</div></td></tr>`;
}

function emptyRow(cols, msg = 'Sin registros') {
    return `<tr><td colspan="${cols}"><div class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
        </svg><h4>${msg}</h4></div></td></tr>`;
}

function setText(id, val) { const el = document.getElementById(id); if (el) el.textContent = val ?? '—'; }

/* ── INIT PAGE ─────────────────────────────────────────────── */
async function initPage(pageTitle) {
    if (!requirePropietario()) return;
    await loadSidebar();
    fillHeaderUser();
    initHeaderDropdown();
    const bc = document.getElementById('bcPage');
    if (bc && pageTitle) bc.textContent = pageTitle;

    initNotificaciones();
    actualizarBadgesReservas();
    setInterval(() => { cargarNotificaciones(); actualizarBadgesReservas(); }, 20000);
}

/* ══════════════════════════════════════════════════════════════
   NOTIFICACIONES (campanita) — nueva reserva / cancelada por cliente
   ══════════════════════════════════════════════════════════════ */
function iconoNotif(tipo) {
    if (tipo === 'RESERVA_CANCELADA_CLIENTE') {
        return `<span style="display:inline-flex;width:32px;height:32px;border-radius:50%;background:#fef2f2;
                color:#dc2626;align-items:center;justify-content:center;flex-shrink:0;font-size:15px;">✕</span>`;
    }
    return `<span style="display:inline-flex;width:32px;height:32px;border-radius:50%;background:#eff6ff;
            color:#2563eb;align-items:center;justify-content:center;flex-shrink:0;font-size:15px;">🔔</span>`;
}

function tiempoRelativo(fechaISO) {
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

/* ── Notificaciones nativas del navegador ──────────────────── */
function pedirPermisoNotificaciones() {
    if (!('Notification' in window)) return;
    if (Notification.permission === 'default') {
        Notification.requestPermission();
    }
}

function claveUltimaNotif() {
    const user = getUser();
    return `ss_last_notif_id_${user ? user.id : 'anon'}`;
}

function lanzarNotificacionesNuevas(lista) {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    if (!lista.length) return;

    const clave = claveUltimaNotif();
    const ultimoVisto = Number(localStorage.getItem(clave) || 0);
    const nuevas = lista.filter(n => n.id > ultimoVisto);

    // La primera vez (sin nada guardado) solo se guarda el máximo actual,
    // para no bombardear con notificaciones viejas al iniciar sesión.
    if (ultimoVisto > 0) {
        nuevas.slice(0, 5).forEach(n => {
            try {
                const notif = new Notification(n.titulo, {
                    body: n.mensaje,
                    icon: '/favicon.png',
                    tag: 'sportspace-notif-' + n.id,
                });
                notif.onclick = () => { window.focus(); clickNotificacion(n.id, n.reservaId); };
            } catch (e) { /* algunos navegadores bloquean si no hay foco */ }
        });
    }

    const maxId = Math.max(ultimoVisto, ...lista.map(n => n.id));
    localStorage.setItem(clave, String(maxId));
}

function initNotificaciones() {
    const btn = document.querySelector('.notif-btn');
    if (!btn || document.getElementById('notifDropdown')) return;

    pedirPermisoNotificaciones();

    btn.style.position = 'relative';
    btn.insertAdjacentHTML('beforeend',
        `<span id="notifBadgeCount" style="display:none;position:absolute;top:-2px;right:-2px;
             background:#dc2626;color:#fff;border-radius:10px;min-width:16px;height:16px;
             font-size:10px;font-weight:700;line-height:16px;text-align:center;padding:0 4px;"></span>`);

    const panel = document.createElement('div');
    panel.id = 'notifDropdown';
    panel.style.cssText = `display:none;position:absolute;top:56px;right:70px;width:340px;max-height:420px;
        overflow-y:auto;background:#fff;border-radius:12px;box-shadow:0 12px 32px rgba(0,0,0,.18);
        border:1px solid var(--border2,#e2e8f0);z-index:2000;`;
    document.body.appendChild(panel);

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const abierto = panel.style.display === 'block';
        panel.style.display = abierto ? 'none' : 'block';
        if (!abierto) cargarNotificaciones();
    });
    document.addEventListener('click', (e) => {
        if (!panel.contains(e.target) && e.target !== btn) panel.style.display = 'none';
    });

    cargarNotificaciones();
}

async function cargarNotificaciones() {
    const panel = document.getElementById('notifDropdown');
    const count = document.getElementById('notifBadgeCount');
    if (!panel) return;

    try {
        const data = await api.get('/propietario/notificaciones');
        const lista = data.notificaciones || [];
        const noLeidas = data.noLeidas || 0;

        lanzarNotificacionesNuevas(lista);

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
                ${noLeidas > 0 ? `<button onclick="marcarTodasNotifsLeidas()"
                    style="border:none;background:none;color:#2563eb;font-size:12px;cursor:pointer;font-weight:600">
                    Marcar todas leídas</button>` : ''}
            </div>
            ${lista.map(n => `
                <div onclick="clickNotificacion(${n.id}, ${n.reservaId})"
                     style="display:flex;gap:10px;padding:12px 16px;cursor:pointer;border-bottom:1px solid #f8fafc;
                     background:${n.leida ? '#fff' : '#f8faff'}">
                    ${iconoNotif(n.tipo)}
                    <div style="min-width:0">
                        <div style="font-size:13px;font-weight:700;color:#1e293b">${n.titulo}</div>
                        <div style="font-size:12.5px;color:#64748b;line-height:1.4;margin-top:2px">${n.mensaje}</div>
                        <div style="font-size:11px;color:#94a3b8;margin-top:4px">${tiempoRelativo(n.createdAt)}</div>
                    </div>
                    ${!n.leida ? '<span style="width:8px;height:8px;border-radius:50%;background:#2563eb;flex-shrink:0;margin-top:4px"></span>' : ''}
                </div>`).join('')}
        `;
    } catch (e) {
        console.error('Error cargando notificaciones:', e);
    }
}

async function marcarTodasNotifsLeidas() {
    try { await api.patch('/propietario/notificaciones/marcar-todas-leidas'); } catch {}
    cargarNotificaciones();
}

async function clickNotificacion(id, reservaId) {
    try { await api.patch(`/propietario/notificaciones/${id}/leida`); } catch {}
    if (reservaId) window.location.href = '/propietario/reservas';
    else cargarNotificaciones();
}

/* ══════════════════════════════════════════════════════════════
   BADGES junto a "Reservas" en el sidebar
   ══════════════════════════════════════════════════════════════ */
async function actualizarBadgesReservas() {
    const cont = document.getElementById('badge-reservas');
    if (!cont) return;

    try {
        const data = await api.get('/propietario/notificaciones/badges');
        const piezas = [];
        if (data.nuevas > 0)
            piezas.push(`<span title="Reservas nuevas" style="background:#2563eb;color:#fff;border-radius:8px;
                padding:1px 6px;font-size:10.5px;font-weight:700;margin-left:3px">${data.nuevas}</span>`);
        if (data.canceladas > 0)
            piezas.push(`<span title="Canceladas por el cliente" style="background:#dc2626;color:#fff;border-radius:8px;
                padding:1px 6px;font-size:10.5px;font-weight:700;margin-left:3px">${data.canceladas}</span>`);
        if (data.reembolsoPendiente > 0)
            piezas.push(`<span title="Reembolso pendiente" style="background:#d97706;color:#fff;border-radius:8px;
                padding:1px 6px;font-size:10.5px;font-weight:700;margin-left:3px">${data.reembolsoPendiente}</span>`);

        if (piezas.length) {
            cont.style.cssText = 'display:inline-flex;margin-left:auto';
            cont.innerHTML = piezas.join('');
        } else {
            cont.style.display = 'none';
            cont.innerHTML = '';
        }
    } catch (e) {
        console.error('Error cargando badges de reservas:', e);
    }
}