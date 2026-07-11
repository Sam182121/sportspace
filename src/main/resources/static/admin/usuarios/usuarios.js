'use strict';

let usuarios          = [];
let usuariosFiltrados = [];
let paginaActual      = 1;
const porPagina       = 10;

const AVATAR_COLORS = ['av-blue', 'av-green', 'av-orange', 'av-purple'];
function colorAvatar(id) {
    return AVATAR_COLORS[id % AVATAR_COLORS.length];
}

// ── Carga inicial ─────────────────────────────────────────────────────────────
async function cargarUsuarios() {
    const tbody = document.getElementById('tablaUsuarios');
    tbody.innerHTML = loadingRow(9);
    try {
        const data        = await api.get('/admin/usuarios');
        if (!data) return;
        usuarios          = data.usuarios || data || [];
        usuariosFiltrados = [...usuarios];
        actualizarStats();
        renderTabla();
    } catch (e) {
        tbody.innerHTML = emptyRow(9, 'Error al cargar usuarios');
        showToast('Error al cargar usuarios: ' + e.message, 'error');
    }
}

// ── Estadísticas ──────────────────────────────────────────────────────────────
function actualizarStats() {
    const total        = usuarios.length;
    const activos      = usuarios.filter(u => estadoStr(u) === 'ACTIVO').length;
    const inactivos    = usuarios.filter(u => estadoStr(u) === 'INACTIVO').length;
    const propietarios = usuarios.filter(u => u.rol === 'PROPIETARIO').length;
    setText('statTotal',        total);
    setText('statActivos',      activos);
    setText('statInactivos',    inactivos);
    setText('statPropietarios', propietarios);
}

function estadoStr(u) {
    return u.estado || (u.activo !== false ? 'ACTIVO' : 'INACTIVO');
}

// ── Helpers de documento ──────────────────────────────────────────────────────
// Devuelve "DNI" o "C.E." como etiqueta legible
function labelTipoDoc(tipo) {
    if (!tipo) return '—';
    return tipo === 'CE' ? 'C.E.' : tipo;
}

// Muestra tipo y número juntos: "DNI · 12345678" o "C.E. · 001077238"
function badgeDocumento(tipo, numero) {
    if (!numero) return '<span style="color:var(--text3)">—</span>';
    const label = labelTipoDoc(tipo);
    const color = tipo === 'CE' ? '#7c3aed' : '#1a56db';
    return `<span style="font-size:11px;font-weight:600;color:${color};
                         background:${color}18;padding:2px 7px;border-radius:99px;
                         white-space:nowrap">${label}</span>
            <span style="margin-left:5px;font-size:13px">${numero}</span>`;
}

// ── Tabla ─────────────────────────────────────────────────────────────────────
function renderTabla() {
    const tbody = document.getElementById('tablaUsuarios');
    const total = usuariosFiltrados.length;
    setText('tablaTitle', `${total} usuario${total !== 1 ? 's' : ''}`);
    const inicio = (paginaActual - 1) * porPagina + 1;
    const fin    = Math.min(paginaActual * porPagina, total);
    setText('tablaInfo', total > 0 ? `Mostrando ${inicio}–${fin} de ${total}` : '');

    if (total === 0) {
        tbody.innerHTML = emptyRow(9, 'No se encontraron usuarios');
        document.getElementById('paginacionContainer').innerHTML = '';
        return;
    }

    const pagina = usuariosFiltrados.slice(
        (paginaActual - 1) * porPagina,
        paginaActual * porPagina
    );

    tbody.innerHTML = pagina.map(u => {
        const iniciales = ((u.nombres || '?')[0] + (u.apellidos || '?')[0]).toUpperCase();
        const color     = colorAvatar(u.id || 0);
        const activo    = estadoStr(u) === 'ACTIVO';

        return `<tr>
      <td>
        <div class="user-cell">
          <div class="avatar ${color}">${iniciales}</div>
          <div class="user-cell-info">
            <div class="td-main">${u.nombres || ''} ${u.apellidos || ''}</div>
          </div>
        </div>
      </td>
      <td>${badgeDocumento(u.tipoDocumento, u.numeroDocumento)}</td>
      <td>${u.nacionalidad || '<span style="color:var(--text3)">—</span>'}</td>
      <td>${u.email || '—'}</td>
      <td>${u.telefono || '—'}</td>
      <td>${badgeRol(u.rol)}</td>
      <td>
        <span class="estado-dot ${activo ? 'activo' : 'inactivo'}">
          ${activo ? 'Activo' : 'Inactivo'}
        </span>
      </td>
      <td>${formatDate(u.fechaRegistro || u.createdAt)}</td>
      <td>
        <div class="action-group">
          <button class="btn-icon btn-view" title="Ver perfil" onclick="verPerfil(${u.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </button>
          <button class="btn-icon btn-edit" title="Editar" onclick="abrirEditar(${u.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
          </button>
          ${u.rol !== 'ADMIN'
            ? (activo
                ? `<button class="btn-icon btn-block" title="Bloquear"
                      onclick="cambiarEstado(${u.id}, false, '${u.nombres} ${u.apellidos}')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="10"/>
                      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                    </svg>
                  </button>`
                : `<button class="btn-icon btn-unblock" title="Activar"
                      onclick="cambiarEstado(${u.id}, true, '${u.nombres} ${u.apellidos}')">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                      <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                  </button>`)
            : ''}
          ${u.rol !== 'ADMIN'
            ? `<button class="btn-icon btn-delete" title="Eliminar"
                  onclick="eliminarUsuario(${u.id}, '${u.nombres} ${u.apellidos}')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                  <path d="M10 11v6M14 11v6"/>
                </svg>
              </button>`
            : ''}
        </div>
      </td>
    </tr>`;
    }).join('');

    renderPagination('paginacionContainer', {
        currentPage:  paginaActual,
        totalPages:   Math.ceil(total / porPagina),
        onPageChange: function(p) {
            paginaActual = p;
            renderTabla();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },
    });
}

// ── Filtros ───────────────────────────────────────────────────────────────────
function filtrarUsuarios() {
    const buscar   = document.getElementById('filtroBuscar').value.trim().toLowerCase();
    const rol      = document.getElementById('filtroRol').value;
    const tipoDoc  = document.getElementById('filtroTipoDoc').value;
    const estado   = document.getElementById('filtroEstado').value;

    usuariosFiltrados = usuarios.filter(u => {
        const nombre   = `${u.nombres} ${u.apellidos}`.toLowerCase();
        const nroDoc   = (u.numeroDocumento || '').toLowerCase();
        const email    = (u.email || '').toLowerCase();

        const matchBuscar  = !buscar  || nombre.includes(buscar) || nroDoc.includes(buscar) || email.includes(buscar);
        const matchRol     = !rol     || u.rol === rol;
        const matchTipoDoc = !tipoDoc || u.tipoDocumento === tipoDoc;
        const matchEstado  = !estado  || estadoStr(u) === estado;

        return matchBuscar && matchRol && matchTipoDoc && matchEstado;
    });

    paginaActual = 1;
    renderTabla();
}

function limpiarFiltros() {
    document.getElementById('filtroBuscar').value  = '';
    document.getElementById('filtroRol').value     = '';
    document.getElementById('filtroTipoDoc').value = '';
    document.getElementById('filtroEstado').value  = '';
    usuariosFiltrados = [...usuarios];
    paginaActual = 1;
    renderTabla();
}

// ── Ver perfil ────────────────────────────────────────────────────────────────
async function verPerfil(id) {
    openModal('modalPerfil');
    document.getElementById('perfilBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';
    try {
        const u       = await api.get(`/admin/usuarios/${id}`);
        if (!u) return;
        const iniciales = ((u.nombres || '?')[0] + (u.apellidos || '?')[0]).toUpperCase();
        const activo    = estadoStr(u) === 'ACTIVO';

        document.getElementById('perfilBody').innerHTML = `
      <div class="perfil-header">
        <div class="perfil-avatar">${iniciales}</div>
        <div>
          <div class="perfil-nombre">${u.nombres || ''} ${u.apellidos || ''}</div>
          <div class="perfil-email">${u.email || ''}</div>
          <div style="margin-top:6px;display:flex;gap:6px;flex-wrap:wrap">
            ${badgeRol(u.rol)}
            ${badgeActivo(activo)}
          </div>
        </div>
      </div>
      <div class="perfil-grid">
        <div class="perfil-field">
          <div class="perfil-field-label">Tipo de documento</div>
          <div class="perfil-field-value">${labelTipoDoc(u.tipoDocumento)}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Número de documento</div>
          <div class="perfil-field-value">${u.numeroDocumento || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Nacionalidad</div>
          <div class="perfil-field-value">${u.nacionalidad || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Teléfono</div>
          <div class="perfil-field-value">${u.telefono || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Fecha de nacimiento</div>
          <div class="perfil-field-value">${formatDate(u.fechaNacimiento) || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Fecha de registro</div>
          <div class="perfil-field-value">${formatDate(u.fechaRegistro || u.createdAt) || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Departamento</div>
          <div class="perfil-field-value">${u.departamento || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Provincia</div>
          <div class="perfil-field-value">${u.provincia || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Distrito</div>
          <div class="perfil-field-value">${u.distrito || '—'}</div>
        </div>
        <div class="perfil-field">
          <div class="perfil-field-label">Dirección</div>
          <div class="perfil-field-value">${u.direccion || '—'}</div>
        </div>
      </div>`;
    } catch (e) {
        document.getElementById('perfilBody').innerHTML =
            `<div class="empty-state"><p>Error al cargar perfil</p></div>`;
    }
}

// ── Editar usuario ────────────────────────────────────────────────────────────
//
// ESTADO del flujo de verificación de correo
let _euEmailOriginal   = '';   // correo guardado en BD al abrir el modal
let _euVerificado      = false; // true solo tras verificar código exitosamente
let _euModoEdicion     = false; // true cuando el lápiz fue pulsado y el campo está activo

// ────────────────────────────────────────────────────────────────────────────
// Helpers de hint (iguales a crear-cuenta)
// ────────────────────────────────────────────────────────────────────────────
const _euIcons = {
    error:   '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    success: '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>',
    info:    '<svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
};
function _euHint(id, msg, type) {
    const el = document.getElementById(id);
    if (!el) return;
    if (!msg) { el.className = 'eu-field-hint'; el.innerHTML = ''; return; }
    el.className = 'eu-field-hint hint-' + (type||'') + ' show';
    el.innerHTML = (_euIcons[type]||'') + '<span>' + msg + '</span>';
}

// ────────────────────────────────────────────────────────────────────────────
// Poner el correo en modo SOLO LECTURA con lápiz visible
// ────────────────────────────────────────────────────────────────────────────
function _euLockEmail(emailValue) {
    const inp  = document.getElementById('eu-email');
    const lapiz= document.getElementById('eu-btn-edit-email');
    inp.value    = emailValue;
    inp.readOnly = true;
    inp.classList.add('eu-doc-locked');
    inp.classList.remove('valid', 'field-error');
    if (lapiz) lapiz.style.display = 'flex';
}

// ────────────────────────────────────────────────────────────────────────────
// Ocultar el bloque de verificación y resetear sus controles internos
// ────────────────────────────────────────────────────────────────────────────
function _euResetVerifyBlock() {
    const verifyRow   = document.getElementById('eu-email-verify-row');
    const paso1       = document.getElementById('eu-email-paso1');
    const paso2       = document.getElementById('eu-email-paso2');
    const codigoInput = document.getElementById('eu-codigo-email');
    const btnEnviar   = document.getElementById('eu-btn-enviar-codigo');
    const btnReenviar = document.getElementById('eu-btn-reenviar-email');
    const btnPedir    = document.getElementById('eu-btn-pedir-codigo');

    if (verifyRow)   verifyRow.style.display  = 'none';
    if (paso1)       paso1.style.display      = 'block';
    if (paso2)       paso2.style.display      = 'none';
    if (codigoInput) { codigoInput.value = ''; codigoInput.disabled = false; }
    if (btnEnviar) {
        btnEnviar.disabled  = true;
        btnEnviar.classList.remove('btn-verified');
        btnEnviar.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Enviar código';
    }
    if (btnPedir) {
        btnPedir.disabled  = false;
        btnPedir.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> Pedir código';
    }
    if (btnReenviar) btnReenviar.style.display = 'none';
    _euHint('eu-hint-email', '');
    _euHint('eu-hint-codigo-email', '');
}

// ────────────────────────────────────────────────────────────────────────────
// LÁPIZ: habilitar edición del correo
// Llamado desde onclick del botón lápiz en el HTML
// ────────────────────────────────────────────────────────────────────────────
function euHabilitarEdicionEmail() {
    _euVerificado  = false;
    _euModoEdicion = true;

    const inp  = document.getElementById('eu-email');
    const lapiz= document.getElementById('eu-btn-edit-email');

    inp.value    = '';
    inp.readOnly = false;
    inp.classList.remove('eu-doc-locked', 'valid', 'field-error');
    if (lapiz) lapiz.style.display = 'none';   // ocultar lápiz mientras escribe

    _euResetVerifyBlock();
    inp.focus();
}

// ────────────────────────────────────────────────────────────────────────────
// Pedir código al nuevo correo  (llamado desde btn "Pedir código")
// ────────────────────────────────────────────────────────────────────────────
async function euPedirCodigo() {
    const email    = (document.getElementById('eu-email').value || '').trim();
    const btnPedir = document.getElementById('eu-btn-pedir-codigo');
    if (!email) return _euHint('eu-hint-codigo-email', 'Ingresa tu correo primero', 'error');
    btnPedir.disabled  = true;
    btnPedir.innerHTML = '<span class="eu-spinner-sm"></span> Enviando...';
    try {
        await api.post('/auth/pre-registro/enviar-codigo-email', { email });
        document.getElementById('eu-email-paso1').style.display = 'none';
        document.getElementById('eu-email-paso2').style.display = 'flex';
        _euHint('eu-hint-codigo-email', 'Código enviado. Revisa tu bandeja de entrada.', 'info');
        document.getElementById('eu-codigo-email').focus();
        _euCountdown('eu-btn-reenviar-email', 60);
    } catch(e) {
        _euHint('eu-hint-codigo-email', e.message || 'Error al enviar código', 'error');
        btnPedir.disabled  = false;
        btnPedir.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> Pedir código';
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Habilitar botón "Enviar código" cuando hay exactamente 6 dígitos
// Llamado desde oninput del input de código en el HTML
// ────────────────────────────────────────────────────────────────────────────
function euHabilitarEnvio() {
    const codigo  = document.getElementById('eu-codigo-email').value;
    const btnEnv  = document.getElementById('eu-btn-enviar-codigo');
    if (btnEnv) btnEnv.disabled = (codigo.length !== 6);
}

// ────────────────────────────────────────────────────────────────────────────
// Verificar código  (llamado desde btn "Enviar código")
// ────────────────────────────────────────────────────────────────────────────
async function euVerificarCodigo() {
    const email  = (document.getElementById('eu-email').value || '').trim();
    const codigo = (document.getElementById('eu-codigo-email').value || '').trim();
    if (codigo.length !== 6) return _euHint('eu-hint-codigo-email', 'El código debe tener 6 dígitos', 'error');
    const btnEnv = document.getElementById('eu-btn-enviar-codigo');
    btnEnv.disabled  = true;
    btnEnv.innerHTML = '<span class="eu-spinner-sm"></span> Verificando...';
    try {
        await api.post('/auth/pre-registro/verificar-email', { email, codigo });
        _euVerificado  = true;
        _euModoEdicion = false;
        _euHint('eu-hint-codigo-email', 'Correo verificado ✓', 'success');
        document.getElementById('eu-codigo-email').disabled = true;
        document.getElementById('eu-btn-reenviar-email').style.display = 'none';
        btnEnv.innerHTML = '✓ Verificado';
        btnEnv.classList.add('btn-verified');
        // Mostrar lápiz para que puedan corregir si quieren
        const lapiz = document.getElementById('eu-btn-edit-email');
        if (lapiz) lapiz.style.display = 'flex';
    } catch(e) {
        _euHint('eu-hint-codigo-email', e.message || 'Código incorrecto o expirado', 'error');
        btnEnv.disabled  = false;
        btnEnv.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Enviar código';
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Countdown reenvío
// ────────────────────────────────────────────────────────────────────────────
function _euCountdown(btnId, segundos) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.style.display = 'inline-flex';
    btn.disabled      = true;
    let restante      = segundos;
    btn.textContent   = 'Reenviar en ' + restante + 's';
    const t = setInterval(function() {
        restante--;
        if (restante <= 0) {
            clearInterval(t);
            btn.disabled    = false;
            btn.textContent = 'Reenviar';
        } else {
            btn.textContent = 'Reenviar en ' + restante + 's';
        }
    }, 1000);
}

// ────────────────────────────────────────────────────────────────────────────
// Validar onblur del campo email (solo activo en modo edición)
// Registrado como listener en abrirEditar, no como atributo inline
// ────────────────────────────────────────────────────────────────────────────
async function _euOnEmailBlur() {
    if (!_euModoEdicion) return;   // solo actuar si el lápiz fue pulsado
    if (_euVerificado)   return;   // ya verificado, nada que hacer

    const email = (document.getElementById('eu-email').value || '').trim();
    if (!email) { _euHint('eu-hint-email', ''); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        _euHint('eu-hint-email', 'Formato de correo inválido', 'error');
        return;
    }
    // Si el nuevo email es igual al original, cancelar modo edición y volver a bloquearlo
    if (email.toLowerCase() === _euEmailOriginal) {
        _euVerificado  = false;
        _euModoEdicion = false;
        _euResetVerifyBlock();
        _euLockEmail(email);
        return;
    }
    // Verificar disponibilidad
    _euHint('eu-hint-email', 'Verificando disponibilidad...', 'info');
    try {
        const check = await api.get('/usuarios/publico/existe-email/' + encodeURIComponent(email)).catch(() => null);
        if (check && check.existe) {
            _euHint('eu-hint-email', 'Este correo ya está registrado por otro usuario.', 'error');
            document.getElementById('eu-email').classList.add('field-error');
            document.getElementById('eu-email').classList.remove('valid');
        } else {
            _euHint('eu-hint-email', 'Correo disponible', 'success');
            document.getElementById('eu-email').classList.add('valid');
            document.getElementById('eu-email').classList.remove('field-error');
            // Bloquear campo y mostrar lápiz + bloque de verificación
            const inp  = document.getElementById('eu-email');
            const lapiz= document.getElementById('eu-btn-edit-email');
            inp.readOnly = true;
            inp.classList.add('eu-doc-locked');
            if (lapiz) lapiz.style.display = 'flex';
            document.getElementById('eu-email-verify-row').style.display = 'block';
            _euHint('eu-hint-codigo-email', 'Haz clic en "Pedir código" para recibir el código.', 'info');
        }
    } catch(e) { _euHint('eu-hint-email', ''); }
}

// ────────────────────────────────────────────────────────────────────────────
// ABRIR MODAL DE EDICIÓN
// ────────────────────────────────────────────────────────────────────────────
async function abrirEditar(id) {
    // 1. Abrir modal y limpiar estado anterior
    openModal('modalEditar');
    ocultarEditError();

    // 2. Resetear estado de verificación de email
    _euVerificado  = false;
    _euModoEdicion = false;
    _euEmailOriginal = '';
    _euResetVerifyBlock();

    // 3. Registrar listener de blur en el campo email (solo una vez)
    const emailInput = document.getElementById('eu-email');
    // Clonar para eliminar listeners previos
    const emailClone = emailInput.cloneNode(true);
    emailInput.parentNode.replaceChild(emailClone, emailInput);
    emailClone.addEventListener('blur', _euOnEmailBlur);

    // 4. Mostrar loading en campos mientras carga
    const camposTexto = ['editNombres','editApellidos','editTipoDocumento','editNumeroDocumento','editNacionalidad'];
    camposTexto.forEach(id => { const el = document.getElementById(id); if(el) el.value = ''; });
    document.getElementById('editTelefono').value = '';
    emailClone.value    = '';
    emailClone.readOnly = true;
    emailClone.classList.add('eu-doc-locked');
    emailClone.classList.remove('valid','field-error');
    document.getElementById('eu-btn-edit-email').style.display = 'none';

    // 5. Cargar datos del usuario desde la API
    try {
        const u = await api.get('/admin/usuarios/' + id);
        if (!u) return;

        // Campos solo lectura
        document.getElementById('editId').value              = u.id;
        document.getElementById('editNombres').value         = u.nombres         || '';
        document.getElementById('editApellidos').value       = u.apellidos       || '';
        document.getElementById('editTipoDocumento').value   = labelTipoDoc(u.tipoDocumento);
        document.getElementById('editNumeroDocumento').value = u.numeroDocumento || '';
        document.getElementById('editNacionalidad').value    = u.nacionalidad    || '';

        // Campo editable
        document.getElementById('editTelefono').value        = u.telefono        || '';

        // Email: mostrar bloqueado con el correo actual + lápiz
        _euEmailOriginal = (u.email || '').trim().toLowerCase();
        _euLockEmail(u.email || '');   // ← pone readOnly=true, añade eu-doc-locked, muestra lápiz

        // Rol
        const grupoRol   = document.getElementById('grupoEditRol');
        const grupoRoles = document.getElementById('grupoRolesHabilitados');
        if (u.rol === 'ADMIN') {
            grupoRol.style.display   = 'none';
            grupoRoles.style.display = 'none';
        } else {
            grupoRol.style.display   = '';
            grupoRoles.style.display = '';
            document.getElementById('editRol').value = u.rol || 'CLIENTE';
            document.getElementById('editEsCliente').checked     = !!u.esCliente;
            document.getElementById('editEsPropietario').checked = !!u.esPropietario;
        }

    } catch(e) {
        showToast('Error al cargar datos del usuario', 'error');
        closeModal('modalEditar');
    }
}

// ────────────────────────────────────────────────────────────────────────────
// GUARDAR EDICIÓN
// ────────────────────────────────────────────────────────────────────────────
async function guardarEdicion() {
    ocultarEditError();
    const id       = document.getElementById('editId').value;
    const email    = (document.getElementById('eu-email').value || '').trim();
    const telefono = (document.getElementById('editTelefono').value || '').trim();
    const grupoRol = document.getElementById('grupoEditRol');
    const esAdmin  = grupoRol.style.display === 'none';
    const rol      = esAdmin ? null : document.getElementById('editRol').value;

    if (!email)
        return mostrarEditError('El correo es obligatorio');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        return mostrarEditError('El formato del correo no es válido');

    // Si el correo cambió, exigir verificación
    const emailCambiado = email.toLowerCase() !== _euEmailOriginal;
    if (emailCambiado && !_euVerificado)
        return mostrarEditError('Debes verificar el nuevo correo antes de guardar');

    if (telefono && !/^\d{9}$/.test(telefono))
        return mostrarEditError('El teléfono debe tener 9 dígitos numéricos');
    if (!esAdmin && rol !== 'CLIENTE' && rol !== 'PROPIETARIO')
        return mostrarEditError('Rol inválido. Solo se permite CLIENTE o PROPIETARIO');

    let esCliente, esPropietario;
    if (!esAdmin) {
        esCliente     = document.getElementById('editEsCliente').checked;
        esPropietario = document.getElementById('editEsPropietario').checked;
        if (!esCliente && !esPropietario)
            return mostrarEditError('Debes dejar al menos un rol habilitado');
    }

    const btn = document.getElementById('btnGuardarEdicion');
    btn.disabled    = true;
    btn.textContent = 'Guardando...';
    try {
        const payload = { email, telefono: telefono || null };
        if (!esAdmin) payload.rol = rol;
        await api.put('/admin/usuarios/' + id, payload);
        if (!esAdmin) {
            await api.patch('/admin/usuarios/' + id + '/roles', { esCliente, esPropietario });
        }
        showToast('Usuario actualizado correctamente', 'success');
        closeModal('modalEditar');
        await cargarUsuarios();
    } catch(e) {
        mostrarEditError(e.message || 'Error al guardar cambios');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Guardar cambios';
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Helpers de errores del modal de edición
// ────────────────────────────────────────────────────────────────────────────
function mostrarEditError(msg) {
    const el = document.getElementById('editError');
    if (!el) return;
    el.textContent   = msg;
    el.style.display = 'block';
}
function ocultarEditError() {
    const el = document.getElementById('editError');
    if (!el) return;
    el.style.display = 'none';
    el.textContent   = '';
}


// ── Cambiar estado ────────────────────────────────────────────────────────────
function cambiarEstado(id, nuevoEstado, nombre) {
    if (!nuevoEstado) {
        // Bloquear: pide motivo + comentario
        pedirMotivoBloqueo(nombre, async (motivo, comentario) => {
            try {
                await api.patch(`/admin/usuarios/${id}/estado`, { activo: false, motivo, comentario });
                showToast('Usuario bloqueado correctamente', 'info');
                await cargarUsuarios();
            } catch (e) {
                showToast('Error al cambiar estado: ' + e.message, 'error');
            }
        });
        return;
    }

    // Reactivar: no requiere motivo
    confirmar(
        `¿Deseas activar al usuario "${nombre}"?`,
        async () => {
            try {
                await api.patch(`/admin/usuarios/${id}/estado`, { activo: true });
                showToast('Usuario activado correctamente', 'success');
                await cargarUsuarios();
            } catch (e) {
                showToast('Error al cambiar estado: ' + e.message, 'error');
            }
        }
    );
}

// ── Eliminar ──────────────────────────────────────────────────────────────────
function eliminarUsuario(id, nombre) {
    confirmar(
        `¿Estás seguro de eliminar al usuario "${nombre}"? Esta acción no se puede deshacer.`,
        () => {
            pedirMotivoEliminacion(nombre, async (motivo, comentario, forzar) => {
                try {
                    const resp = await api.del(`/admin/usuarios/${id}`, { motivo, comentario, forzar: String(forzar) });
                    showToast(resp.mensaje || 'Usuario eliminado correctamente', 'success');
                    await cargarUsuarios();
                } catch (e) {
                    showToast('Error al eliminar usuario: ' + e.message, 'error');
                }
            });
        }
    );
}

// ── Exportar Excel ────────────────────────────────────────────────────────────
async function exportarUsuariosExcel() {
    const btn = document.getElementById('btnExportarExcel');
    if (!usuariosFiltrados.length) {
        showToast('No hay usuarios para exportar', 'info');
        return;
    }
    btn.disabled    = true;
    btn.textContent = 'Generando...';
    try {
        if (typeof XLSX === 'undefined') {
            await cargarScript('https://cdnjs.cloudflare.com/ajax/libs/xlsx/0.18.5/xlsx.full.min.js');
        }
        const filas = usuariosFiltrados.map(u => ({
            'ID':                u.id,
            'Nombres':           u.nombres           || '',
            'Apellidos':         u.apellidos         || '',
            'Tipo Documento':    labelTipoDoc(u.tipoDocumento),
            'Número Documento':  u.numeroDocumento   || '',
            'Nacionalidad':      u.nacionalidad      || '',
            'Correo':            u.email             || '',
            'Teléfono':          u.telefono          || '',
            'Rol':               u.rol               || '',
            'Estado':            estadoStr(u) === 'ACTIVO' ? 'Activo' : 'Inactivo',
            'Departamento':      u.departamento      || '',
            'Provincia':         u.provincia         || '',
            'Distrito':          u.distrito          || '',
            'Dirección':         u.direccion         || '',
            'Fecha Registro':    formatDate(u.fechaRegistro || u.createdAt) || '',
        }));

        const libro = XLSX.utils.book_new();
        const hoja  = XLSX.utils.json_to_sheet(filas);
        hoja['!cols'] = [
            {wch:6},{wch:20},{wch:20},{wch:14},{wch:14},{wch:14},
            {wch:30},{wch:12},{wch:12},{wch:10},{wch:15},{wch:15},{wch:15},{wch:30},{wch:18},
        ];
        XLSX.utils.book_append_sheet(libro, hoja, 'Usuarios');
        const hoy   = new Date();
        const fecha = `${hoy.getFullYear()}${String(hoy.getMonth()+1).padStart(2,'0')}${String(hoy.getDate()).padStart(2,'0')}`;
        XLSX.writeFile(libro, `SportSpace_Usuarios_${fecha}.xlsx`);
        showToast(`Excel exportado: ${usuariosFiltrados.length} usuarios`, 'success');
    } catch (e) {
        showToast('Error al exportar Excel: ' + e.message, 'error');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Exportar Excel';
    }
}

function cargarScript(src) {
    return new Promise((resolve, reject) => {
        if (document.querySelector(`script[src="${src}"]`)) { resolve(); return; }
        const s = document.createElement('script');
        s.src = src; s.onload = resolve;
        s.onerror = () => reject(new Error('No se pudo cargar: ' + src));
        document.head.appendChild(s);
    });
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

document.addEventListener('DOMContentLoaded', () => { cargarUsuarios(); });