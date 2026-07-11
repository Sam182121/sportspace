const API = 'http://localhost:8080/api';

// token — lee localStorage primero, luego sessionStorage
function getToken()  {
    return localStorage.getItem('ss_token') || sessionStorage.getItem('ss_token') || null;
}
function getUser()   {
    try {
        const raw = localStorage.getItem('ss_user') || sessionStorage.getItem('ss_user');
        return JSON.parse(raw || 'null');
    } catch { return null; }
}
function setSession(token, user) {
    localStorage.setItem('ss_token', token);
    localStorage.setItem('ss_user', JSON.stringify(user));
}
function clearSession() {
    localStorage.removeItem('ss_token');
    localStorage.removeItem('ss_user');
    sessionStorage.removeItem('ss_token');
    sessionStorage.removeItem('ss_user');
}

function requireAuth(rol) {
    const token = getToken();
    const user  = getUser();
    if (!token || !user) { window.location.href = '/login'; return false; }
    if (rol && user.rol !== rol) { window.location.href = '/login'; return false; }
    return true;
}

function logout() {
    clearSession();
    window.location.href = '/login';
}

async function http(method, url, body = null) {
    const headers = { 'Content-Type': 'application/json' };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res  = await fetch(API + url, opts);
    const data = await res.json().catch(() => ({}));

    if (res.status === 401) { clearSession(); window.location.href = '/login'; return; }
    if (!res.ok) throw new Error(data.mensaje || data.message || 'Error en la solicitud');
    return data;
}

/** Sube un archivo (foto de cancha / voucher) como multipart/form-data.
 *  Devuelve { url: "/uploads/..." }. El archivo NO se guarda en la BD. */
async function httpUpload(url, file) {
    const headers = {};
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const formData = new FormData();
    formData.append('file', file);

    const res  = await fetch(API + url, { method: 'POST', headers, body: formData });
    const data = await res.json().catch(() => ({}));

    if (res.status === 401) { clearSession(); window.location.href = '/login'; return; }
    if (!res.ok) throw new Error(data.mensaje || data.message || 'Error al subir el archivo');
    return data; // { url: "/uploads/..." }
}

const api = {
    get:    (url)       => http('GET',   url),
    post:   (url, body) => http('POST',  url, body),
    put:    (url, body) => http('PUT',   url, body),
    patch:  (url, body) => http('PATCH', url, body),
    del:    (url, body) => http('DELETE',url, body),
    upload: (url, file) => httpUpload(url, file),
};

/* ══════════════════════════════════════════════════════════════
   MIS ROLES — opción en el menú superior (dropdown de usuario)
   ══════════════════════════════════════════════════════════════ */
const ADMIN_EMAIL = 'rondomnims9@gmail.com';

async function iniciarMisRolesMenu() {
    const user = getUser();
    if (!user || (user.rol !== 'CLIENTE' && user.rol !== 'PROPIETARIO')) return;

    let me;
    try { me = await api.get('/usuarios/me'); } catch { return; }

    let etiqueta = 'Mis Roles';
    if (!me.esCliente && me.esPropietario)      etiqueta = '¿Quieres ser también Cliente?';
    else if (!me.esPropietario && me.esCliente) etiqueta = '¿Quieres ser también Propietario?';

    let intentos = 0;
    const t = setInterval(() => {
        intentos++;
        const dropdown  = document.querySelector('.user-dropdown');
        const logoutBtn = dropdown ? dropdown.querySelector('.dropdown-item.danger') : null;
        if (logoutBtn && !document.getElementById('btnMisRoles')) {
            const item = document.createElement('button');
            item.id = 'btnMisRoles';
            item.className = 'dropdown-item';
            item.innerHTML = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                    style="width:16px;height:16px;flex-shrink:0">
                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
                    <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg> ${etiqueta}`;
            item.onclick = () => abrirMisRoles(me);
            logoutBtn.parentElement.insertBefore(item, logoutBtn);
            clearInterval(t);
        }
        if (intentos > 25) clearInterval(t); // ~5s máximo esperando el menú
    }, 200);
}
document.addEventListener('DOMContentLoaded', iniciarMisRolesMenu);

function mailtoSolicitar(asunto, nombreCompleto, dni, telefono) {
    const cuerpo = `Nombre completo: ${nombreCompleto}\nNúmero de DNI: ${dni || ''}\nNúmero de teléfono: ${telefono || ''}`;
    return `mailto:${ADMIN_EMAIL}?subject=${encodeURIComponent(asunto)}&body=${encodeURIComponent(cuerpo)}`;
}

function abrirMisRoles(me) {
    if (document.getElementById('misRolesOverlay')) return;

    const nombreCompleto = `${me.nombres} ${me.apellidos}`;
    const dni = me.numeroDocumento, tel = me.telefono;

    const filaRol = (nombre, activo) => `
        <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 14px;
             border:1px solid #e2e8f0;border-radius:10px;margin-bottom:8px;">
            <span style="font-weight:600;font-size:14px;">${nombre}</span>
            <span style="font-size:13px;font-weight:600;color:${activo ? '#16a34a' : '#dc2626'}">
                ${activo ? '✅ Activo' : '❌ Inactivo'}
            </span>
        </div>`;

    const beneficios = {
        CLIENTE: 'Podrás reservar canchas deportivas, ver tu historial de reservas y calificar tus experiencias.',
        PROPIETARIO: 'Podrás publicar tus canchas, gestionar reservas y horarios, y ver tus ingresos en un panel dedicado.',
    };
    const nombreRol = { CLIENTE: 'Cliente', PROPIETARIO: 'Propietario' };

    const bloqueSolicitar = (rol) => `
        <div style="border:1px dashed #94a3b8;border-radius:10px;padding:14px;margin-top:10px;">
            <p style="margin:0 0 8px;font-size:13.5px;color:#334155;">
                <strong>¿Quieres también ser ${nombreRol[rol]}?</strong><br/>${beneficios[rol]}
            </p>
            <p style="margin:0 0 10px;font-size:12.5px;color:#64748b;">
                Envía un correo a <strong>${ADMIN_EMAIL}</strong> con el asunto
                <strong>"QUIERO SER ${rol}"</strong> incluyendo tu nombre completo, DNI y teléfono.
                Una vez validada tu información, el administrador activará el nuevo rol en tu cuenta.
            </p>
            <a href="${mailtoSolicitar('QUIERO SER ' + rol, nombreCompleto, dni, tel)}"
               style="display:inline-block;background:#2563eb;color:#fff;padding:9px 14px;border-radius:8px;
               font-size:13px;font-weight:600;text-decoration:none;">Solicitar por correo</a>
        </div>`;

    const bloqueEliminar = (rol) => `
        <div style="border:1px dashed #fca5a5;border-radius:10px;padding:14px;margin-top:10px;">
            <p style="margin:0 0 8px;font-size:13.5px;color:#334155;">
                ¿Ya no quieres ser ${nombreRol[rol]}?
            </p>
            <p style="margin:0 0 10px;font-size:12.5px;color:#64748b;">
                Envía un correo a <strong>${ADMIN_EMAIL}</strong> con el asunto
                <strong>"YA NO QUIERO SER ${rol}"</strong> incluyendo tu nombre completo, DNI y teléfono.
                El administrador revisará tu solicitud y, una vez aprobada, el rol será eliminado de tu cuenta.
            </p>
            <a href="${mailtoSolicitar('YA NO QUIERO SER ' + rol, nombreCompleto, dni, tel)}"
               style="display:inline-block;background:#fff;color:#dc2626;border:1px solid #dc2626;padding:9px 14px;
               border-radius:8px;font-size:13px;font-weight:600;text-decoration:none;">Solicitar eliminación</a>
        </div>`;

    const overlay = document.createElement('div');
    overlay.id = 'misRolesOverlay';
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,.6);backdrop-filter:blur(3px);' +
        'z-index:9999;display:flex;align-items:center;justify-content:center;padding:16px;';
    overlay.innerHTML = `
        <div style="background:#fff;border-radius:16px;padding:26px 24px;max-width:420px;width:100%;
             max-height:88vh;overflow-y:auto;box-shadow:0 20px 50px rgba(0,0,0,.25);">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px;">
                <h2 style="margin:0;font-size:18px;">Mis Roles</h2>
                <button id="cerrarMisRoles" style="border:none;background:none;font-size:20px;cursor:pointer;color:#64748b;">&times;</button>
            </div>
            ${filaRol('Cliente', me.esCliente)}
            ${filaRol('Propietario', me.esPropietario)}
            ${!me.esCliente ? bloqueSolicitar('CLIENTE') : bloqueEliminar('CLIENTE')}
            ${!me.esPropietario ? bloqueSolicitar('PROPIETARIO') : bloqueEliminar('PROPIETARIO')}
        </div>`;

    document.body.appendChild(overlay);
    document.getElementById('cerrarMisRoles').onclick = () => overlay.remove();
    overlay.addEventListener('click', (e) => { if (e.target === overlay) overlay.remove(); });
}

function toast(msg, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        container.id = 'toastContainer';
        document.body.appendChild(container);
    }
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.textContent = msg;
    container.appendChild(t);
    setTimeout(() => t.remove(), 3500);
}

// alerta
function setAlert(id, msg, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerHTML = msg
        ? `<div class="alert alert-${type}">${msg}</div>`
        : '';
}

// formato
function formatDate(str) {
    if (!str) return '—';
    if (str.includes('/')) return str;
    const [y, m, d] = str.split('-');
    return `${d}/${m}/${y}`;
}

function formatCurrency(n) {
    return 'S/. ' + Number(n || 0).toFixed(2);
}

function badgeEstado(e) {
    const cls = { PENDIENTE:'pending', CONFIRMADA:'success', CANCELADA:'danger' };
    const lbl = { PENDIENTE:'Pendiente', CONFIRMADA:'Confirmada', CANCELADA:'Cancelada' };
    return `<span class="badge badge-${cls[e]||'info'}">${lbl[e]||e}</span>`;
}

function badgeRol(r) {
    const cls = { ADMIN:'admin', PROPIETARIO:'info', CLIENTE:'success' };
    return `<span class="badge badge-${cls[r]||'info'}">${r}</span>`;
}

function loading() {
    return `<div class="loading-state"><div class="spinner"></div> Cargando...</div>`;
}

function emptyState(msg = 'Sin registros') {
    return `<div class="empty-state">
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
      <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
    </svg>
    <h4>${msg}</h4><p>No hay datos disponibles.</p>
  </div>`;
}

function openModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }

document.addEventListener('click', e => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('open');
    }
});

function initSidebarUser() {
    const user = getUser();
    if (!user) return;
    const name = `${user.nombres} ${user.apellidos}`;
    const av = document.getElementById('userAvatar');
    const nm = document.getElementById('userName');
    const rl = document.getElementById('userRole');
    if (av) av.textContent = user.nombres[0].toUpperCase();
    if (nm) nm.textContent = name;
    if (rl) rl.textContent = user.rol;
}

function setActiveNav() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-item[href]').forEach(a => {
        if (path.endsWith(a.getAttribute('href'))) {
            a.classList.add('active');
        }
    });
}

document.addEventListener('DOMContentLoaded', () => {
    initSidebarUser();
    setActiveNav();
});

// iconos
const icon = {
    chart:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 20V10M12 20V4M6 20v-6"/></svg>`,
    users:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>`,
    field:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M12 7v14M2 14h20"/></svg>`,
    calendar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>`,
    logout:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/></svg>`,
    pin:      `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`,
    money:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>`,
};