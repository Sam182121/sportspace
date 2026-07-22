'use strict';

const API = '/api';

/* sesion */
function getToken() {
    return localStorage.getItem('ss_token') || sessionStorage.getItem('ss_token');
}

function getUser() {
    try {
        const raw = localStorage.getItem('ss_user') || sessionStorage.getItem('ss_user');
        return JSON.parse(raw || 'null');
    } catch {
        return null;
    }
}

function clearSession() {
    // Solo borra token y usuario, NO las credenciales guardadas (recordar sesion)
    localStorage.removeItem('ss_token');
    localStorage.removeItem('ss_user');
    sessionStorage.removeItem('ss_token');
    sessionStorage.removeItem('ss_user');
}

function logout() {
    clearSession();
    window.location.href = '/login?logout=true';
}

function requireAdmin() {
    const token = getToken();
    const user  = getUser();
    if (!token || !user) {
        window.location.href = '/login';
        return false;
    }
    if (user.rol !== 'ADMIN') {
        window.location.href = '/login';
        return false;
    }
    return true;
}

/* http */
async function http(method, url, body = null) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept':       'application/json',
    };
    const token = getToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);

    const res  = await fetch(API + url, opts);
    const data = await res.json().catch(() => ({}));

    if (res.status === 401) {
        clearSession();
        window.location.href = '/login';
        return null;
    }
    if (res.status === 403) {
        showToast('No tienes permisos para esta accion', 'error');
        return null;
    }
    if (!res.ok) {
        throw new Error(data.mensaje || data.message || 'Error en la solicitud');
    }
    return data;
}

/* ══════════════════════════════════════════════════════════════
   Modal: motivo de eliminación (Usuarios / Propietarios)
   onConfirm(motivo, comentario) se llama solo si el admin confirma.
   ══════════════════════════════════════════════════════════════ */
function pedirMotivoEliminacion(nombre, onConfirm) {
    if (document.getElementById('modalMotivoElim')) document.getElementById('modalMotivoElim').remove();

    const overlay = document.createElement('div');
    overlay.id = 'modalMotivoElim';
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,.6);backdrop-filter:blur(2px);' +
        'z-index:9999;display:flex;align-items:center;justify-content:center;padding:16px;';

    overlay.innerHTML = `
        <div style="background:#fff;border-radius:14px;padding:24px;max-width:420px;width:100%;
             box-shadow:0 20px 50px rgba(0,0,0,.25);">
            <h3 style="margin:0 0 4px;font-size:16px">Eliminar cuenta</h3>
            <p style="margin:0 0 16px;font-size:13px;color:#64748b">
                Vas a eliminar a <strong>${nombre}</strong>. Indica el motivo:
            </p>

            <label style="font-size:12.5px;font-weight:600;color:#334155">Motivo</label>
            <select id="motivoElimSelect" style="width:100%;padding:9px 10px;margin:6px 0 12px;
                    border:1px solid #cbd5e1;border-radius:8px;font-size:13px">
                <option value="SOLICITADO_POR_USUARIO">Solicitado por el usuario</option>
                <option value="MAL_USO_PLATAFORMA">Mal uso de la plataforma</option>
                <option value="CUENTA_DUPLICADA_PRUEBA">Cuenta duplicada o de prueba</option>
                <option value="OTRO">Otro</option>
            </select>

            <label style="font-size:12.5px;font-weight:600;color:#334155">
                Comentario <span id="comentarioObligatorio" style="display:none;color:#dc2626">(obligatorio)</span>
            </label>
            <textarea id="comentarioElim" rows="3" placeholder="Detalles adicionales (opcional)"
                      style="width:100%;padding:9px 10px;margin:6px 0 16px;border:1px solid #cbd5e1;
                      border-radius:8px;font-size:13px;resize:vertical"></textarea>

            <div id="errorMotivoElim" style="display:none;color:#dc2626;font-size:12.5px;margin-bottom:10px"></div>

            <label style="display:flex;align-items:flex-start;gap:8px;background:#fef2f2;border:1px solid #fecaca;
                   border-radius:8px;padding:10px 12px;margin-bottom:16px;cursor:pointer">
                <input type="checkbox" id="forzarElimCheck" style="margin-top:2px"/>
                <span style="font-size:12px;color:#991b1b;line-height:1.4">
                    <strong>Eliminar por completo de la base de datos</strong> (borra también todo su historial de
                    reservas, pagos, canchas, etc). Esta acción es irreversible. Si no marcas esto, si la cuenta
                    tiene historial se anonimizará en vez de borrarse, para no romper datos de terceros.
                </span>
            </label>

            <div style="display:flex;gap:10px;justify-content:flex-end">
                <button onclick="document.getElementById('modalMotivoElim').remove()"
                        style="padding:9px 16px;border:1px solid #cbd5e1;border-radius:8px;background:#fff;
                        font-size:13px;font-weight:600;cursor:pointer">Cancelar</button>
                <button id="btnConfirmarMotivoElim"
                        style="padding:9px 16px;border:none;border-radius:8px;background:#dc2626;color:#fff;
                        font-size:13px;font-weight:600;cursor:pointer">Eliminar cuenta</button>
            </div>
        </div>`;
    document.body.appendChild(overlay);

    const selectMotivo = document.getElementById('motivoElimSelect');
    const avisoObl     = document.getElementById('comentarioObligatorio');
    selectMotivo.addEventListener('change', () => {
        avisoObl.style.display = selectMotivo.value === 'OTRO' ? 'inline' : 'none';
    });

    document.getElementById('btnConfirmarMotivoElim').onclick = async () => {
        const motivo     = selectMotivo.value;
        const comentario = document.getElementById('comentarioElim').value.trim();
        const forzar     = document.getElementById('forzarElimCheck').checked;
        const errorDiv   = document.getElementById('errorMotivoElim');

        if (motivo === 'OTRO' && !comentario) {
            errorDiv.textContent = 'Debes escribir un comentario cuando el motivo es "Otro".';
            errorDiv.style.display = 'block';
            return;
        }

        if (forzar) {
            const seguro = confirm(
                'Vas a ELIMINAR POR COMPLETO esta cuenta y todo su historial relacionado ' +
                '(reservas, pagos, canchas). No se puede deshacer. ¿Confirmas?'
            );
            if (!seguro) return;
        }

        overlay.remove();
        await onConfirm(motivo, comentario, forzar);
    };
}

/* bloqueo */
function pedirMotivoBloqueo(nombre, onConfirm) {
    if (document.getElementById('modalMotivoBloqueo')) document.getElementById('modalMotivoBloqueo').remove();

    const overlay = document.createElement('div');
    overlay.id = 'modalMotivoBloqueo';
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(15,23,42,.6);backdrop-filter:blur(2px);' +
        'z-index:9999;display:flex;align-items:center;justify-content:center;padding:16px;';

    overlay.innerHTML = `
        <div style="background:#fff;border-radius:14px;padding:24px;max-width:420px;width:100%;
             box-shadow:0 20px 50px rgba(0,0,0,.25);">
            <h3 style="margin:0 0 4px;font-size:16px">Bloquear cuenta</h3>
            <p style="margin:0 0 16px;font-size:13px;color:#64748b">
                Vas a bloquear a <strong>${nombre}</strong>. No podrá iniciar sesión, pero su información se conserva. Indica el motivo:
            </p>

            <label style="font-size:12.5px;font-weight:600;color:#334155">Motivo</label>
            <select id="motivoBloqueoSelect" style="width:100%;padding:9px 10px;margin:6px 0 12px;
                    border:1px solid #cbd5e1;border-radius:8px;font-size:13px">
                <option value="SOLICITADO_POR_USUARIO">Solicitado por el usuario</option>
                <option value="ACTIVIDAD_SOSPECHOSA_FRAUDE">Actividad sospechosa o posible fraude</option>
                <option value="INCUMPLIMIENTO_TERMINOS">Incumplimiento de los términos y condiciones</option>
                <option value="QUEJAS_REITERADAS">Quejas o reportes reiterados de otros usuarios</option>
                <option value="DATOS_NO_VERIFICADOS">No se pudo verificar su identidad / datos</option>
                <option value="OTRO">Otro</option>
            </select>

            <label style="font-size:12.5px;font-weight:600;color:#334155">
                Comentario <span id="comentarioBloqueoObligatorio" style="display:none;color:#dc2626">(obligatorio)</span>
            </label>
            <textarea id="comentarioBloqueo" rows="3" placeholder="Detalles adicionales (opcional)"
                      style="width:100%;padding:9px 10px;margin:6px 0 16px;border:1px solid #cbd5e1;
                      border-radius:8px;font-size:13px;resize:vertical"></textarea>

            <div id="errorMotivoBloqueo" style="display:none;color:#dc2626;font-size:12.5px;margin-bottom:10px"></div>

            <div style="display:flex;gap:10px;justify-content:flex-end">
                <button onclick="document.getElementById('modalMotivoBloqueo').remove()"
                        style="padding:9px 16px;border:1px solid #cbd5e1;border-radius:8px;background:#fff;
                        font-size:13px;font-weight:600;cursor:pointer">Cancelar</button>
                <button id="btnConfirmarMotivoBloqueo"
                        style="padding:9px 16px;border:none;border-radius:8px;background:#dc2626;color:#fff;
                        font-size:13px;font-weight:600;cursor:pointer">Bloquear cuenta</button>
            </div>
        </div>`;
    document.body.appendChild(overlay);

    const selectMotivo = document.getElementById('motivoBloqueoSelect');
    const avisoObl      = document.getElementById('comentarioBloqueoObligatorio');
    selectMotivo.addEventListener('change', () => {
        avisoObl.style.display = selectMotivo.value === 'OTRO' ? 'inline' : 'none';
    });

    document.getElementById('btnConfirmarMotivoBloqueo').onclick = async () => {
        const motivo     = selectMotivo.value;
        const comentario = document.getElementById('comentarioBloqueo').value.trim();
        const errorDiv    = document.getElementById('errorMotivoBloqueo');

        if (motivo === 'OTRO' && !comentario) {
            errorDiv.textContent = 'Debes escribir un comentario cuando el motivo es "Otro".';
            errorDiv.style.display = 'block';
            return;
        }
        overlay.remove();
        await onConfirm(motivo, comentario);
    };
}

const api = {
    get:    (url)        => http('GET',    url),
    post:   (url, body)  => http('POST',   url, body),
    put:    (url, body)  => http('PUT',    url, body),
    patch:  (url, body)  => http('PATCH',  url, body),
    delete: (url, body)  => http('DELETE', url, body),
    del:    (url, body)  => http('DELETE', url, body), // alias, algunas páginas usan api.del
};

/* carga */
async function loadSidebar() {
    const container = document.getElementById('sidebar-container');
    if (!container) return;

    try {
        const res  = await fetch('/admin/shared/sidebar.html');
        const html = await res.text();
        container.innerHTML = html;
        setActiveNavItem();
    } catch (e) {
        console.error('Error cargando sidebar:', e);
    }
}

function setActiveNavItem() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-item[data-page]').forEach(item => {
        const page = item.getAttribute('data-page');
        if (path.includes(page)) {
            item.classList.add('active');
        }
    });
}

/* navbar */
function initHeader() {
    const user = getUser();
    if (!user) return;

    // avatar inicial
    const avatarEl = document.getElementById('headerAvatar');
    if (avatarEl) {
        avatarEl.textContent = (user.nombres || 'A')[0].toUpperCase();
    }

    // nombre y rol
    const nameEl = document.getElementById('headerName');
    const roleEl = document.getElementById('headerRole');
    if (nameEl) nameEl.textContent = `${user.nombres} ${user.apellidos}`;
    if (roleEl) roleEl.textContent = user.rol;

    //  info
    const dropNameEl  = document.getElementById('dropName');
    const dropRoleEl  = document.getElementById('dropRole');
    const dropEmailEl = document.getElementById('dropEmail');
    if (dropNameEl)  dropNameEl.textContent  = `${user.nombres} ${user.apellidos}`;
    if (dropRoleEl)  dropRoleEl.textContent  = user.rol;
    if (dropEmailEl) dropEmailEl.textContent = user.email || '';

    //  usuario
    const dropWrap = document.getElementById('userDropdownWrap');
    const headerUser = document.getElementById('headerUser');
    if (dropWrap && headerUser) {
        headerUser.addEventListener('click', (e) => {
            e.stopPropagation();
            dropWrap.classList.toggle('open');
        });
        // Cerrar al click fuera
        document.addEventListener('click', () => {
            dropWrap.classList.remove('open');
        });
    }

    const bcPage = document.getElementById('bcPage');
    if (bcPage) {
        const path = window.location.pathname;
        const segments = path.split('/').filter(Boolean);
        const lastSegment = segments[segments.length - 1];
        const labels = {
            dashboard:     'Dashboard',
            usuarios:      'Usuarios',
            propietarios:  'Propietarios',
            canchas:       'Canchas',
            reservas:      'Reservas',
            pagos:         'Pagos',
            reportes:      'Reportes',
            estadisticas:  'Estadisticas',
            seguridad:     'Seguridad',
            configuracion: 'Configuracion',
        };
        bcPage.textContent = labels[lastSegment] || lastSegment;
    }
}

/* hamburguesa sidebar */
function initHamburger() {
    const btn     = document.getElementById('hamburger');
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');

    if (btn && sidebar) {
        btn.addEventListener('click', () => {
            sidebar.classList.toggle('mobile-open');
            if (overlay) overlay.classList.toggle('open');
        });
    }
}

function closeSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (sidebar) sidebar.classList.remove('mobile-open');
    if (overlay) overlay.classList.remove('open');
}

const TOAST_ICONS = {
    success: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0e9f6e" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>`,
    error:   `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#e02424" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="15" y1="9" x2="9" y2="15"/>
              <line x1="9" y1="9" x2="15" y2="15"/>
            </svg>`,
    info:    `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#1a56db" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>`,
};

function showToast(msg, type = 'info', duration = 3000) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        container.id = 'toastContainer';
        document.body.appendChild(container);
    }
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    t.innerHTML = `${TOAST_ICONS[type] || ''}<span>${msg}</span>`;
    container.appendChild(t);
    setTimeout(() => {
        t.style.opacity = '0';
        t.style.transform = 'translateX(14px)';
        t.style.transition = 'all .3s';
        setTimeout(() => t.remove(), 300);
    }, duration);
}

function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('open');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('open');
}

function initModals() {
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) overlay.classList.remove('open');
        });
    });
}

/* formato */
function formatDate(str) {
    if (!str) return '—';
    if (str.includes('/')) return str;
    try {
        const [y, m, d] = str.split('T')[0].split('-');
        return `${d}/${m}/${y}`;
    } catch {
        return str;
    }
}

function formatCurrency(n) {
    if (n === null || n === undefined) return '—';
    return 'S/. ' + Number(n).toLocaleString('es-PE', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}

function formatDateTime(str) {
    if (!str) return '—';
    try {
        // Spring devuelve arrays [y,m,d,h,min,sec] o strings ISO
        if (Array.isArray(str)) {
            const [y, mo, d, h = 0, mi = 0] = str;
            return `${String(d).padStart(2,'0')}/${String(mo).padStart(2,'0')}/${y} ${String(h).padStart(2,'0')}:${String(mi).padStart(2,'0')}`;
        }
        const d = new Date(str);
        if (isNaN(d.getTime())) return str;
        return d.toLocaleString('es-PE', {
            day: '2-digit', month: '2-digit', year: 'numeric',
            hour: '2-digit', minute: '2-digit',
        });
    } catch {
        return str;
    }
}

function badgeEstado(estado) {
    const map = {
        PENDIENTE:   { cls: 'badge-warning', label: 'Pendiente'   },
        CONFIRMADA:  { cls: 'badge-success', label: 'Confirmada'  },
        CANCELADA:   { cls: 'badge-danger',  label: 'Cancelada'   },
        COMPLETADA:  { cls: 'badge-gray',    label: 'Completada'  },
        APROBADO:    { cls: 'badge-success', label: 'Aprobado'    },
        RECHAZADO:   { cls: 'badge-danger',  label: 'Rechazado'   },
    };
    const b = map[estado] || { cls: 'badge-gray', label: estado };
    return `<span class="badge ${b.cls}">${b.label}</span>`;
}

function badgeRol(rol) {
    const map = {
        ADMIN:       { cls: 'badge-danger',  label: 'Admin'       },
        PROPIETARIO: { cls: 'badge-purple',  label: 'Propietario' },
        CLIENTE:     { cls: 'badge-info',    label: 'Cliente'     },
    };
    const b = map[rol] || { cls: 'badge-gray', label: rol };
    return `<span class="badge ${b.cls}">${b.label}</span>`;
}

function badgeActivo(activo) {
    return activo
        ? `<span class="badge badge-success">Activo</span>`
        : `<span class="badge badge-danger">Inactivo</span>`;
}

/* function avatarHtml(nombre, apellido, color = 'av-blue') {
    const inicial = ((nombre || '?')[0] + (apellido || '?')[0]).toUpperCase();
    return `<div class="avatar ${color}">${inicial}</div>`;
} */

function payMethodHtml(metodo) {
    const map = {
        YAPE:         `<span class="pay-method pay-yape">Yape</span>`,
        PLIN:         `<span class="pay-method pay-plin">Plin</span>`,
        TRANSFERENCIA:`<span class="pay-method pay-transfer">Transferencia</span>`,
        EFECTIVO:     `<span class="pay-method pay-efectivo">Efectivo</span>`,
    };
    return map[metodo] || `<span class="pay-method pay-efectivo">${metodo}</span>`;
}

/* carga */
function loadingRow(cols) {
    return `<tr>
    <td colspan="${cols}" class="td-loading">
      <div class="loading-center">
        <div class="spinner"></div>
        Cargando...
      </div>
    </td>
  </tr>`;
}

function emptyRow(cols, msg = 'Sin registros') {
    return `<tr>
    <td colspan="${cols}">
      <div class="empty-state">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0h-2.586
                   a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293
                   l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
        </svg>
        <h4>${msg}</h4>
        <p>No hay datos disponibles.</p>
      </div>
    </td>
  </tr>`;
}

/* function emptyCards(msg = 'Sin registros') {
   return `<div class="empty-state">
   <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
     <path d="M20 13V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7m16 0v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-5m16 0
              h-2.586a1 1 0 0 0-.707.293l-2.414 2.414a1 1 0 0 1-.707.293h-3.172a1 1 0 0 1-.707-.293
              l-2.414-2.414A1 1 0 0 0 6.586 13H4"/>
   </svg>
   <h4>${msg}</h4>
   <p>No hay datos disponibles.</p>
 </div>`;
} */

/* paginacion */
function renderPagination(containerId, { currentPage, totalPages, onPageChange }) {
    const container = document.getElementById(containerId);
    if (!container || totalPages <= 1) {
        if (container) container.innerHTML = '';
        return;
    }

    let html = '';
    html += `<button class="page-btn" ${currentPage === 1 ? 'disabled' : ''}
             onclick="(${onPageChange})(${currentPage - 1})">«</button>`;

    for (let i = 1; i <= totalPages; i++) {
        if (
            i === 1 || i === totalPages ||
            (i >= currentPage - 1 && i <= currentPage + 1)
        ) {
            html += `<button class="page-btn ${i === currentPage ? 'active' : ''}"
                 onclick="(${onPageChange})(${i})">${i}</button>`;
        } else if (i === currentPage - 2 || i === currentPage + 2) {
            html += `<span style="color:var(--text3);font-size:13px;padding:0 2px">...</span>`;
        }
    }

    html += `<button class="page-btn" ${currentPage === totalPages ? 'disabled' : ''}
             onclick="(${onPageChange})(${currentPage + 1})">»</button>`;

    container.innerHTML = html;
}

/* confirmacion */
function confirmar(mensaje, onConfirm) {
    const overlay = document.getElementById('confirmModal');
    const msgEl   = document.getElementById('confirmMsg');
    const btnOk   = document.getElementById('confirmOk');

    if (!overlay || !msgEl || !btnOk) {
        if (window.confirm(mensaje)) onConfirm();
        return;
    }

    msgEl.textContent = mensaje;
    overlay.classList.add('open');

    const handler = () => {
        overlay.classList.remove('open');
        btnOk.removeEventListener('click', handler);
        onConfirm();
    };
    btnOk.addEventListener('click', handler);
}

/* exporta excel */
async function exportarExcel(datos, columnas, nombreArchivo) {
    if (!datos || !datos.length) {
        showToast('No hay datos para exportar', 'info');
        return;
    }

    try {
        if (typeof XLSX === 'undefined') {
            await new Promise((resolve, reject) => {
                const s = document.createElement('script');
                s.src = 'https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js';
                s.onload = resolve;
                s.onerror = () => reject(new Error('No se pudo cargar SheetJS'));
                document.head.appendChild(s);
            });
        }

        // Transforma cada objeto usando el mapa de columnas
        const filas = datos.map(fila => {
            const row = {};
            Object.entries(columnas).forEach(([header, getter]) => {
                row[header] = getter(fila);
            });
            return row;
        });

        const libro = XLSX.utils.book_new();
        const hoja  = XLSX.utils.json_to_sheet(filas);

        // Ancho automatico basado en el header más largo
        const anchos = Object.keys(columnas).map(h => ({ wch: Math.max(h.length + 2, 14) }));
        hoja['!cols'] = anchos;

        XLSX.utils.book_append_sheet(libro, hoja, nombreArchivo);

        const hoy   = new Date();
        const fecha = `${hoy.getFullYear()}${String(hoy.getMonth()+1).padStart(2,'0')}${String(hoy.getDate()).padStart(2,'0')}`;
        XLSX.writeFile(libro, `SportSpace_${nombreArchivo}_${fecha}.xlsx`);

        showToast(`Excel exportado: ${datos.length} registros`, 'success');

    } catch (e) {
        showToast('Error al exportar Excel: ' + e.message, 'error');
    }
}

/* llama cada pagina */
async function initAdmin() {
    if (!requireAdmin()) return;
    await loadSidebar();
    initHeader();
    initHamburger();
    initModals();
    initNotifSystem();   // <-- notificaciones en vivo
}

document.addEventListener('DOMContentLoaded', () => {
    initAdmin();
});

//  NOTIFICACIONES EN VIVO
//
//  Fuente: GET /api/admin/seguridad/stats  →  campo intentosUltimas24h
//  Poll automático cada 30 s (sin recargar la página)
//  Switch "errorSistema" en Configuración activa/desactiva
//  Click en la notificación → redirige a /admin/seguridad


(function () {
    'use strict';

    const CFG_KEY = 'ss_admin_notif_config';
    const POLL_MS = 30_000;

    let _intentos = 0;   // último valor de intentosUltimas24h
    let _visto    = false; // el admin ya abrió el panel tras este valor

    /* config */
    function switchActivo() {
        try {
            const raw = localStorage.getItem(CFG_KEY);
            const cfg = raw ? JSON.parse(raw) : {};
            return cfg.errorSistema !== false;   // activo por defecto
        } catch { return true; }
    }

    /* indicadores (dot + badge) */
    function actualizarIndicadores() {
        const mostrar = _intentos > 0 && switchActivo() && !_visto;
        const dot   = document.getElementById('notifDot');
        const badge = document.getElementById('notifBadge');
        if (dot)   dot.style.display   = mostrar ? 'block' : 'none';
        if (badge) badge.style.display = mostrar ? 'flex'  : 'none';
    }

    /* contenido del panel */
    function renderPanel() {
        const list = document.getElementById('_npList');
        if (!list) return;

        if (!switchActivo()) {
            list.innerHTML = `
                <div class="_np-empty">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="26" height="26">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                        <line x1="2" y1="2" x2="22" y2="22"/>
                    </svg>
                    <span>Notificaciones desactivadas</span>
                    <a href="/admin/configuracion">Activar en Configuración</a>
                </div>`;
            return;
        }

        if (_intentos === 0) {
            list.innerHTML = `
                <div class="_np-empty">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="26" height="26">
                        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>
                        <path d="M13.73 21a2 2 0 0 1-3.46 0"/>
                    </svg>
                    <span>Sin notificaciones</span>
                </div>`;
            return;
        }

        list.innerHTML = `
            <div class="_np-item" onclick="window.location.href='/admin/seguridad'">
                <div class="_np-ico">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15">
                        <rect x="3" y="11" width="18" height="11" rx="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                </div>
                <div class="_np-body">
                    <div class="_np-title">Intentos fallidos de login</div>
                    <div class="_np-desc">${_intentos} intento${_intentos !== 1 ? 's' : ''} en las últimas 24 h</div>
                    <div class="_np-ir">Ir a Seguridad →</div>
                </div>
            </div>`;
    }

    /*  poll  */
    async function poll() {
        try {
            const res = await fetch(API + '/admin/seguridad/stats', {
                headers: {
                    'Authorization': 'Bearer ' + (getToken() || ''),
                    'Content-Type': 'application/json',
                }
            });
            if (!res.ok) return;
            const data = await res.json();
            const prev = _intentos;
            _intentos  = Number(data.intentosUltimas24h) || 0;

            // Si llegaron nuevos intentos → marcar como no visto
            if (_intentos > prev) _visto = false;

            actualizarIndicadores();

            // Refrescar lista si el panel está abierto
            const panel = document.getElementById('_npPanel');
            if (panel && panel.classList.contains('_np-open')) renderPanel();

        } catch { /* silencioso */ }
    }

    /* crear panel e inyectarlo junto al botón */
    function crearPanel() {
        // Buscar el botón por id (dashboard) o por clase (resto de páginas)
        const btn = document.getElementById('notifBtn')
            || document.querySelector('.notif-btn');
        if (!btn) return;

        // Asegurar que el botón tiene id para el click handler
        if (!btn.id) btn.id = 'notifBtn';

        // Agregar badge "!" dentro del botón si no existe
        if (!document.getElementById('notifBadge')) {
            const badge = document.createElement('span');
            badge.id    = 'notifBadge';
            badge.style.cssText = [
                'position:absolute', 'top:-4px', 'right:-4px',
                'width:14px', 'height:14px',
                'background:var(--danger,#e02424)', 'color:#fff',
                'font-size:9px', 'font-weight:800',
                'border-radius:50%', 'border:2px solid var(--bg2,#fff)',
                'display:none', 'align-items:center', 'justify-content:center',
                'pointer-events:none', 'z-index:3', 'line-height:1',
                'font-family:var(--font,sans-serif)',
            ].join(';');
            badge.textContent = '!';
            btn.style.position = 'relative';
            btn.appendChild(badge);
        }

        // Crear el panel y colocarlo DESPUÉS del botón en el DOM
        if (!document.getElementById('_npPanel')) {
            const panel = document.createElement('div');
            panel.id = '_npPanel';
            panel.style.cssText = [
                'position:absolute', 'top:calc(100% + 8px)', 'right:0',
                'width:280px', 'background:var(--bg2,#fff)',
                'border:1px solid var(--border,#e5e7eb)',
                'border-radius:10px',
                'box-shadow:0 8px 24px rgba(0,0,0,.13)',
                'z-index:1000', 'display:none', 'flex-direction:column',
                'overflow:hidden',
            ].join(';');
            panel.innerHTML = `
                <div style="padding:11px 14px 10px;border-bottom:1px solid var(--border,#e5e7eb)">
                    <span style="font-size:12.5px;font-weight:600;color:var(--text,#111);font-family:var(--font-head,inherit)">
                        Notificaciones
                    </span>
                </div>
                <div id="_npList" style="min-height:60px"></div>
                <div style="padding:8px 12px;border-top:1px solid var(--border,#e5e7eb)">
                    <a href="/admin/configuracion" style="display:flex;align-items:center;gap:6px;font-size:11.5px;color:var(--text3,#9ca3af);text-decoration:none;padding:4px 5px;border-radius:6px">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="11" height="11"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
                        Configurar notificaciones
                    </a>
                </div>`;

            // Posicionar el panel: el padre del botón necesita position:relative
            const parentEl = btn.parentElement;
            parentEl.style.position = 'relative';
            parentEl.insertBefore(panel, btn.nextSibling);
        }

        // CSS interno para los items (inyectado una sola vez)
        if (!document.getElementById('_npStyles')) {
            const style = document.createElement('style');
            style.id = '_npStyles';
            style.textContent = `
                #_npPanel._np-open { display:flex !important; animation:_npIn .15s ease; }
                @keyframes _npIn { from{opacity:0;transform:translateY(-4px)} to{opacity:1;transform:translateY(0)} }
                ._np-item {
                    display:flex; align-items:flex-start; gap:10px;
                    padding:12px 14px; cursor:pointer;
                    border-bottom:1px solid var(--border,#e5e7eb);
                    transition:background .12s;
                }
                ._np-item:hover { background:var(--bg3,#f9fafb); }
                ._np-ico {
                    width:32px; height:32px; border-radius:8px; flex-shrink:0;
                    background:var(--danger-bg,#fde8e8); color:var(--danger,#e02424);
                    display:flex; align-items:center; justify-content:center;
                }
                ._np-body { flex:1; min-width:0; }
                ._np-title { font-size:12.5px; font-weight:600; color:var(--text,#111); margin-bottom:2px; }
                ._np-desc  { font-size:11.5px; color:var(--text2,#4b5563); line-height:1.4; margin-bottom:3px; }
                ._np-ir    { font-size:11px; color:var(--accent,#3b82f6); font-weight:500; }
                ._np-empty {
                    display:flex; flex-direction:column; align-items:center;
                    justify-content:center; gap:7px; padding:26px 14px;
                    color:var(--text3,#9ca3af); font-size:12px; text-align:center;
                }
                ._np-empty svg { stroke:var(--border2,#d1d5db); }
                ._np-empty a { font-size:11.5px; color:var(--accent,#3b82f6); text-decoration:none; }
            `;
            document.head.appendChild(style);
        }

        /* ─── eventos del botón ───────────────────────────────── */
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            const panel = document.getElementById('_npPanel');
            if (!panel) return;

            // Cerrar dropdown usuario
            document.getElementById('userDropdownWrap')?.classList.remove('open');

            const abriendo = !panel.classList.contains('_np-open');
            panel.classList.toggle('_np-open');

            if (abriendo) {
                renderPanel();
                _visto = true;
                actualizarIndicadores();
            }
        });

        // Cerrar al hacer click fuera
        document.addEventListener('click', function (e) {
            const panel = document.getElementById('_npPanel');
            const btn2  = document.getElementById('notifBtn');
            if (!panel) return;
            if (!panel.contains(e.target) && e.target !== btn2) {
                panel.classList.remove('_np-open');
            }
        });
    }

    /* init público */
    window.initNotifSystem = function () {
        crearPanel();
        // Primer poll a 1.5 s para no bloquear el render
        setTimeout(poll, 1500);
        // Poll automático cada 30 s
        setInterval(poll, POLL_MS);
    };

})();