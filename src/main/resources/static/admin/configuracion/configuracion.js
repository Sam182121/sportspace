'use strict';

/* contraseña */
const NOTIF_KEY = 'ss_admin_notif_config';

/* notifiaciones */
const NOTIFICACIONES = [
    {
        id:    'nuevaReserva',
        title: 'Nueva Reserva',
        desc:  'Recibir alerta cuando un cliente realice una nueva reserva en el sistema.',
        color: 'blue',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2"/>
                    <path d="M16 2v4M8 2v4M3 10h18"/>
                </svg>`,
    },
    {
        id:    'cancelacion',
        title: 'Cancelacion de Reserva',
        desc:  'Recibir alerta cuando se cancele una reserva confirmada.',
        color: 'orange',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="15" y1="9" x2="9" y2="15"/>
                    <line x1="9"  y1="9" x2="15" y2="15"/>
                </svg>`,
    },
    {
        id:    'nuevoPago',
        title: 'Nuevo Pago',
        desc:  'Recibir alerta cuando se registre un pago exitoso en el sistema.',
        color: 'green',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <text x="12" y="17" text-anchor="middle" font-size="13" font-weight="700"
                          font-family="Arial,sans-serif" fill="currentColor" stroke="none">S/</text>
                </svg>`,
    },
    {
        id:    'nuevoUsuario',
        title: 'Nuevo Usuario',
        desc:  'Recibir alerta cuando un nuevo usuario se registre en la plataforma.',
        color: 'blue',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                    <circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>`,
    },
    {
        id:    'nuevaPropietario',
        title: 'Nuevo Propietario',
        desc:  'Recibir alerta cuando se registre un nuevo propietario de cancha.',
        color: 'green',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                    <line x1="12" y1="11" x2="12" y2="17"/>
                    <line x1="9"  y1="14" x2="15" y2="14"/>
                </svg>`,
    },
    {
        id:    'errorSistema',
        title: 'Intentos fallidos de login',
        desc:  'Alerta cuando haya intentos fallidos de acceso en las últimas 24 h.',
        color: 'orange',
        icon:  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
                    <line x1="12" y1="9"  x2="12" y2="13"/>
                    <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>`,
    },
];

/* estado */
let config = {};

/* carga */
function cargarConfig() {
    try {
        const raw = localStorage.getItem(NOTIF_KEY);
        if (raw) return JSON.parse(raw);
    } catch { /* ignora */ }

    //  todas activas
    const defaults = {};
    NOTIFICACIONES.forEach(n => { defaults[n.id] = true; });
    return defaults;
}

function guardarConfig() {
    try {
        localStorage.setItem(NOTIF_KEY, JSON.stringify(config));
        return true;
    } catch {
        return false;
    }
}

/* render */
function renderNotifBody() {
    const body = document.getElementById('notifBody');
    if (!body) return;

    body.innerHTML = NOTIFICACIONES.map(n => {
        const checked = config[n.id] !== false;
        return `
        <div class="notif-item" data-id="${n.id}">
            <div class="notif-item-left">
                <div class="notif-item-icon ${n.color}">
                    ${n.icon}
                </div>
                <div class="notif-item-info">
                    <div class="notif-item-title">${n.title}</div>
                    <div class="notif-item-desc">${n.desc}</div>
                </div>
            </div>
            <div class="switch-wrap">
                <span class="switch-label ${checked ? 'on' : ''}" id="lbl-${n.id}">
                    ${checked ? 'Activo' : 'Inactivo'}
                </span>
                <label class="switch" title="${checked ? 'Desactivar' : 'Activar'} notificacion">
                    <input
                        type="checkbox"
                        id="sw-${n.id}"
                        ${checked ? 'checked' : ''}
                        onchange="onToggle('${n.id}', this.checked)"
                        aria-label="${n.title}"
                    />
                    <span class="slider"></span>
                </label>
            </div>
        </div>`;
    }).join('');
}

/* alterna */
function onToggle(id, checked) {
    config[id] = checked;

    // Actualizar label sin re-render completo
    const lbl = document.getElementById(`lbl-${id}`);
    if (lbl) {
        lbl.textContent = checked ? 'Activo' : 'Inactivo';
        lbl.classList.toggle('on', checked);
    }

    guardarYMostrarEstado();
}

/* guardar */
let saveTimer = null;

function guardarYMostrarEstado() {
    const statusEl = document.getElementById('saveStatus');
    if (!statusEl) return;

    const ok = guardarConfig();

    if (ok) {
        statusEl.innerHTML = `
            <span class="save-ok">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <polyline points="20 6 9 17 4 12"/>
                </svg>
                Guardado
            </span>`;
    } else {
        statusEl.innerHTML = `
            <span class="save-error">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="12" y1="8" x2="12" y2="12"/>
                    <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
                Error al guardar
            </span>`;
        showToast('No se pudo guardar la configuracion', 'error');
    }

    // dura 2.5 segunditos
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
        if (statusEl) statusEl.innerHTML = '';
    }, 2500);
}

/* funcion */
document.addEventListener('DOMContentLoaded', async () => {
    // Espera a que initAdmin (de admin.js) termine su inicializacion
    await new Promise(r => setTimeout(r, 50));

    config = cargarConfig();
    renderNotifBody();
});