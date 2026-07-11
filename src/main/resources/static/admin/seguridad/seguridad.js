'use strict';

const AVATAR_COLORS = ['av-blue', 'av-green', 'av-orange', 'av-purple'];
function colorAvatar(id) { return AVATAR_COLORS[(id || 0) % AVATAR_COLORS.length]; }

/* ── datos en memoria para el filtrado local ── */
let _todosIntentos  = [];
let _todasSesiones  = [];

/* cargar todo */
async function refrescarTodo() {
    await Promise.allSettled([
        cargarStats(),
        cargarIntentos(),
        cargarSesiones(),
    ]);
}

async function cargarStats() {
    try {
        const data = await api.get('/admin/seguridad/stats');
        if (!data) return;

        setText('statIntentos',   data.intentosFallidos  ?? '—');
        setText('statIPs',        data.ipsBloqueadas      ?? '—');
        setText('statSesiones',   data.sesionesActivas    ?? '—');
        setText('statBloqueados', data.usuariosBloqueados ?? '—');

        if (data.intentosUltimas24h) {
            setText('statIntentosSub', `${data.intentosUltimas24h} ultimas 24h`);
        }

        if (data.intentosFallidos > 0) {
            mostrarAlerta(data.intentosFallidos);
        }

    } catch (e) {
        console.error('Error stats seguridad:', e);
    }
}

function mostrarAlerta(cantidad) {
    const el = document.getElementById('alertaSeguridad');
    if (!el) return;
    el.style.display = 'block';
    el.innerHTML = `
    <div class="alerta-seg">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
        <line x1="12" y1="9" x2="12" y2="13"/>
        <line x1="12" y1="17" x2="12.01" y2="17"/>
      </svg>
      <span>Se detectaron <strong>${cantidad} intentos fallidos</strong> de acceso al sistema. Revisa la tabla y bloquea las IPs sospechosas.</span>
    </div>`;
}

/* ── INTENTOS FALLIDOS ── */
async function cargarIntentos() {
    const tbody = document.getElementById('tablaIntentos');
    tbody.innerHTML = loadingRow(6);

    try {
        const data = await api.get('/admin/seguridad/intentos-fallidos');
        if (!data) return;

        _todosIntentos = data.intentos || data || [];
        renderIntentos(_todosIntentos);

    } catch (e) {
        tbody.innerHTML = emptyRow(6, 'Error al cargar intentos fallidos');
        showToast('Error: ' + e.message, 'error');
    }
}

function renderIntentos(intentos) {
    const tbody = document.getElementById('tablaIntentos');
    setText('intentosInfo', `${intentos.length} registro${intentos.length !== 1 ? 's' : ''}`);

    if (intentos.length === 0) {
        tbody.innerHTML = emptyRow(6, 'Sin resultados');
        return;
    }

    tbody.innerHTML = intentos.map(i => {
        const alto      = (i.cantidad || 0) >= 3;
        const bloqueada = i.bloqueada === true;

        return `<tr>
        <td><span class="ip-cell">${i.ip || '—'}</span></td>
        <td>
          <div class="td-main">${i.correoIntentado || '—'}</div>
        </td>
        <td>
          <span class="intentos-badge ${alto ? 'alto' : 'bajo'}">
            ${i.cantidad || 0}
          </span>
        </td>
        <td>${formatDateTime(i.ultimoIntento || i.fecha)}</td>
        <td>
          ${bloqueada
            ? '<span class="ip-bloqueada">Bloqueada</span>'
            : '<span class="ip-libre">Libre</span>'
        }
        </td>
        <td>
          <div class="action-group">
            ${!bloqueada
            ? `<button class="btn-icon btn-block" title="Bloquear IP"
                  onclick="bloquearIP('${i.ip}','${i.correoIntentado || ''}')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                  </svg>
                </button>`
            : `<button class="btn-icon btn-unblock" title="Desbloquear IP"
                  onclick="desbloquearIP('${i.ip}')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2"/>
                    <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                  </svg>
                </button>`
        }
            <button class="btn-icon btn-ignore" title="Ignorar / Limpiar"
              onclick="ignorarIntento(${i.id || 0})">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              </svg>
            </button>
          </div>
        </td>
      </tr>`;
    }).join('');
}

function filtrarIntentos() {
    const q = (document.getElementById('buscarIntentos')?.value || '').trim().toLowerCase();
    const filtrados = q
        ? _todosIntentos.filter(i => (i.correoIntentado || '').toLowerCase().includes(q))
        : _todosIntentos;
    renderIntentos(filtrados);
}

/* ── SESIONES ACTIVAS ── */
async function cargarSesiones() {
    const tbody = document.getElementById('tablaSesiones');
    tbody.innerHTML = loadingRow(5);

    try {
        const data = await api.get('/admin/seguridad/sesiones-activas');
        if (!data) return;

        _todasSesiones = data.sesiones || data || [];
        renderSesiones(_todasSesiones);

    } catch (e) {
        tbody.innerHTML = emptyRow(5, 'Error al cargar sesiones');
        showToast('Error: ' + e.message, 'error');
    }
}

function renderSesiones(sesiones) {
    const tbody = document.getElementById('tablaSesiones');
    setText('sesionesInfo', `${sesiones.length} sesion${sesiones.length !== 1 ? 'es' : ''} activa${sesiones.length !== 1 ? 's' : ''}`);
    setText('statSesiones', sesiones.length);

    if (sesiones.length === 0) {
        tbody.innerHTML = emptyRow(5, 'Sin resultados');
        return;
    }

    tbody.innerHTML = sesiones.map(s => {
        const iniciales  = ((s.nombres || '?')[0] + (s.apellidos || '?')[0]).toUpperCase();
        const color      = colorAvatar(s.usuarioId || 0);
        const esMiSesion = s.esSesionActual === true;

        return `<tr ${esMiSesion ? 'class="fila-actual"' : ''}>
        <td>
          <div class="sesion-user-cell">
            <div class="avatar ${color}">${iniciales}</div>
            <div>
              <div class="td-main">
                ${s.nombres || ''} ${s.apellidos || ''}
                ${esMiSesion ? '<span class="badge badge-info" style="font-size:10px;margin-left:4px">Tu sesion</span>' : ''}
              </div>
              <div class="td-sub">${s.email || ''}</div>
            </div>
          </div>
        </td>
        <td>${badgeRol(s.rol)}</td>
        <td><span class="ip-cell">${s.ip || '—'}</span></td>
        <td>${formatDateTime(s.inicioDeSesion || s.createdAt)}</td>
        <td>
          <div class="action-group">
            ${!esMiSesion
            ? `<button class="btn-icon btn-close" title="Cerrar sesion"
                  onclick="cerrarSesion('${s.sessionId || s.token || ''}','${s.nombres} ${s.apellidos}')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/>
                    <line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                </button>`
            : '<span style="font-size:11.5px;color:var(--text3)">Tu sesion</span>'
        }
          </div>
        </td>
      </tr>`;
    }).join('');
}

function filtrarSesiones() {
    const q = (document.getElementById('buscarSesiones')?.value || '').trim().toLowerCase();
    const filtradas = q
        ? _todasSesiones.filter(s => (s.email || '').toLowerCase().includes(q))
        : _todasSesiones;
    renderSesiones(filtradas);
}

/* ── ACCIONES INTENTOS ── */
function bloquearIP(ip, correo) {
    confirmar(
        `¿Bloquear la IP ${ip}? No podra acceder al sistema.`,
        async () => {
            try {
                await api.post('/admin/seguridad/bloquear-ip', { ip });
                showToast(`IP ${ip} bloqueada correctamente`, 'success');
                await refrescarTodo();
            } catch (e) {
                showToast('Error: ' + e.message, 'error');
            }
        }
    );
}

function desbloquearIP(ip) {
    confirmar(
        `¿Desbloquear la IP ${ip}?`,
        async () => {
            try {
                await api.post('/admin/seguridad/desbloquear-ip', { ip });
                showToast(`IP ${ip} desbloqueada correctamente`, 'success');
                await refrescarTodo();
            } catch (e) {
                showToast('Error: ' + e.message, 'error');
            }
        }
    );
}

function ignorarIntento(id) {
    confirmar(
        '¿Eliminar este registro de intentos fallidos?',
        async () => {
            try {
                await api.delete(`/admin/seguridad/intentos/${id}`);
                showToast('Registro eliminado correctamente', 'success');
                await cargarIntentos();
            } catch (e) {
                showToast('Error: ' + e.message, 'error');
            }
        }
    );
}

/* ── ACCIONES SESIONES ── */
function cerrarSesion(sessionId, nombre) {
    confirmar(
        `¿Cerrar la sesion de "${nombre}"? El usuario sera desconectado inmediatamente.`,
        async () => {
            try {
                await api.post('/admin/seguridad/cerrar-sesion', { sessionId });
                showToast(`Sesion de ${nombre} cerrada correctamente`, 'success');
                await cargarSesiones();
            } catch (e) {
                showToast('Error: ' + e.message, 'error');
            }
        }
    );
}

function cerrarTodasLasSesiones() {
    confirmar(
        '¿Cerrar TODAS las sesiones activas? Todos los usuarios (excepto tu) seran desconectados.',
        async () => {
            try {
                await api.post('/admin/seguridad/cerrar-todas-sesiones', {});
                showToast('Todas las sesiones cerradas correctamente', 'success');
                await cargarSesiones();
            } catch (e) {
                showToast('Error: ' + e.message, 'error');
            }
        }
    );
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

document.addEventListener('DOMContentLoaded', () => {
    refrescarTodo();
});