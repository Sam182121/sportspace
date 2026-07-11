'use strict';

/* estado */
let propietarios          = [];
let propietariosFiltrados = [];
let paginaActual          = 1;
const porPagina           = 10;

const AVATAR_COLORS = ['av-blue', 'av-green', 'av-orange', 'av-purple'];
function colorAvatar(id) { return AVATAR_COLORS[(id || 0) % AVATAR_COLORS.length]; }

/* cargar propietarios */
async function cargarPropietarios() {
    const tbody = document.getElementById('tablaPropietarios');
    tbody.innerHTML = loadingRow(7);

    try {
        const data = await api.get('/admin/propietarios');
        if (!data) return;

        propietarios          = data.propietarios || data || [];
        propietariosFiltrados = [...propietarios];

        calcularStats();
        mostrarAlertaPendientes();
        renderTabla();

    } catch (e) {
        tbody.innerHTML = emptyRow(7, 'Error al cargar propietarios');
        showToast('Error: ' + e.message, 'error');
    }
}

function calcularStats() {
    const total      = propietarios.length;
    const activos    = propietarios.filter(p => (p.estado || 'ACTIVO') === 'ACTIVO').length;
    const pendientes = propietarios.filter(p => p.estado === 'PENDIENTE').length;
    const ingresos   = propietarios.reduce((acc, p) => acc + (p.ingresosGenerados || 0), 0);

    setText('statTotal',      total);
    setText('statActivos',    activos);
    setText('statPendientes', pendientes);
    setText('statIngresos',   formatCurrency(ingresos));
}

function mostrarAlertaPendientes() {
    const existing = document.getElementById('alertaPendientes');
    if (existing) existing.remove();

    const pendientes = propietarios.filter(p => p.estado === 'PENDIENTE');
    if (pendientes.length === 0) return;

    const alerta = document.createElement('div');
    alerta.id        = 'alertaPendientes';
    alerta.className = 'alerta-pendientes';
    alerta.innerHTML = `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>
      <line x1="12" y1="9" x2="12" y2="13"/>
      <line x1="12" y1="17" x2="12.01" y2="17"/>
    </svg>
    <span>Hay <strong>${pendientes.length} propietario${pendientes.length > 1 ? 's' : ''}</strong>
    pendiente${pendientes.length > 1 ? 's' : ''} de aprobacion.</span>`;

    const filterBar = document.querySelector('.filter-bar');
    if (filterBar) filterBar.before(alerta);
}

/* tabla */
function renderTabla() {
    const tbody = document.getElementById('tablaPropietarios');
    const total = propietariosFiltrados.length;

    setText('tablaTitle', `${total} propietario${total !== 1 ? 's' : ''}`);
    const inicio = (paginaActual - 1) * porPagina + 1;
    const fin    = Math.min(paginaActual * porPagina, total);
    setText('tablaInfo', total > 0 ? `Mostrando ${inicio}–${fin} de ${total}` : '');

    if (total === 0) {
        tbody.innerHTML = emptyRow(7, 'No se encontraron propietarios');
        document.getElementById('paginacionContainer').innerHTML = '';
        return;
    }

    const pagina = propietariosFiltrados.slice(
        (paginaActual - 1) * porPagina,
        paginaActual * porPagina
    );

    tbody.innerHTML = pagina.map(p => {
        const iniciales = ((p.nombres || '?')[0] + (p.apellidos || '?')[0]).toUpperCase();
        const color     = colorAvatar(p.id);
        const estado    = (p.estado || 'ACTIVO').toUpperCase();
        const estadoCls = estado === 'ACTIVO' ? 'activo'
            : estado === 'PENDIENTE' ? 'pendiente' : 'inactivo';

        return `<tr>
      <td>
        <div class="prop-cell">
          <div class="avatar ${color}">${iniciales}</div>
          <div class="prop-cell-info">
            <div class="td-main">${p.nombres || ''} ${p.apellidos || ''}</div>
            <div class="td-sub">${p.email || ''}</div>
          </div>
        </div>
      </td>
      <td>${p.totalCanchas ?? '—'}</td>
      <td>${p.reservasActivas ?? '—'}</td>
      <td class="td-money">${p.ingresosGenerados != null ? formatCurrency(p.ingresosGenerados) : '—'}</td>
      <td><span class="estado-prop ${estadoCls}">${estado.charAt(0) + estado.slice(1).toLowerCase()}</span></td>
      <td>${formatDate(p.fechaRegistro || p.createdAt)}</td>
      <td>
        <div class="action-group">
          <button class="btn-icon btn-view" title="Ver detalle" onclick="verDetalle(${p.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </button>
          <button class="btn-icon btn-stats" title="Estadisticas" onclick="verEstadisticas(${p.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="20" x2="18" y2="10"/>
              <line x1="12" y1="20" x2="12" y2="4"/>
              <line x1="6"  y1="20" x2="6"  y2="14"/>
            </svg>
          </button>
          ${estado === 'PENDIENTE'
            ? `<button class="btn-icon btn-approve" title="Aprobar"
                onclick="cambiarEstado(${p.id},'ACTIVO','${p.nombres} ${p.apellidos}')">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </button>`
            : estado === 'ACTIVO'
                ? `<button class="btn-icon btn-suspend" title="Suspender"
                  onclick="cambiarEstado(${p.id},'INACTIVO','${p.nombres} ${p.apellidos}')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                  </svg>
                </button>`
                : `<button class="btn-icon btn-approve" title="Reactivar"
                  onclick="cambiarEstado(${p.id},'ACTIVO','${p.nombres} ${p.apellidos}')">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                  </svg>
                </button>`
        }
          <button class="btn-icon btn-delete" title="Eliminar"
            onclick="eliminarPropietario(${p.id},'${p.nombres} ${p.apellidos}')">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6M14 11v6"/>
            </svg>
          </button>
        </div>
      </td>
    </tr>`;
    }).join('');

    const totalPaginas = Math.ceil(total / porPagina);
    renderPagination('paginacionContainer', {
        currentPage:  paginaActual,
        totalPages:   totalPaginas,
        onPageChange: function(p) {
            paginaActual = p;
            renderTabla();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        },
    });
}

/* filtros */
function filtrarPropietarios() {
    const buscar = document.getElementById('filtroBuscar').value.trim().toLowerCase();
    const estado = document.getElementById('filtroEstado').value;

    propietariosFiltrados = propietarios.filter(p => {
        const nombre      = `${p.nombres} ${p.apellidos}`.toLowerCase();
        const email       = (p.email || '').toLowerCase();
        const matchBuscar = !buscar || nombre.includes(buscar) || email.includes(buscar);
        const matchEstado = !estado || (p.estado || 'ACTIVO') === estado;
        return matchBuscar && matchEstado;
    });

    paginaActual = 1;
    renderTabla();
}

function limpiarFiltros() {
    document.getElementById('filtroBuscar').value = '';
    document.getElementById('filtroEstado').value = '';
    propietariosFiltrados = [...propietarios];
    paginaActual = 1;
    renderTabla();
}

/* detalle */
async function verDetalle(id) {
    openModal('modalDetalle');
    document.getElementById('detalleBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';

    try {
        const p = await api.get(`/admin/propietarios/${id}`);
        if (!p) return;

        const iniciales = ((p.nombres || '?')[0] + (p.apellidos || '?')[0]).toUpperCase();
        const estado    = (p.estado || 'ACTIVO').toUpperCase();
        const estadoCls = estado === 'ACTIVO' ? 'activo'
            : estado === 'PENDIENTE' ? 'pendiente' : 'inactivo';

        document.getElementById('detalleBody').innerHTML = `
      <div class="detalle-header">
        <div class="detalle-avatar">${iniciales}</div>
        <div>
          <div class="detalle-nombre">${p.nombres || ''} ${p.apellidos || ''}</div>
          <div class="detalle-email">${p.email || ''}</div>
          <div style="margin-top:6px">
            <span class="estado-prop ${estadoCls}">
              ${estado.charAt(0) + estado.slice(1).toLowerCase()}
            </span>
          </div>
        </div>
      </div>
      <div class="detalle-grid">
        <div class="detalle-field">
          <div class="detalle-field-label">DNI / C.E.</div>
          <div class="detalle-field-value">${p.numeroDocumento || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Telefono</div>
          <div class="detalle-field-value">${p.telefono || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Total Canchas</div>
          <div class="detalle-field-value">${p.totalCanchas ?? '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Reservas Activas</div>
          <div class="detalle-field-value">${p.reservasActivas ?? '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Ingresos Generados</div>
          <div class="detalle-field-value money">
            ${p.ingresosGenerados != null ? formatCurrency(p.ingresosGenerados) : '—'}
          </div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Fecha Registro</div>
          <div class="detalle-field-value">${formatDate(p.fechaRegistro || p.createdAt)}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Departamento</div>
          <div class="detalle-field-value">${p.departamento || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Distrito</div>
          <div class="detalle-field-value">${p.distrito || '—'}</div>
        </div>
      </div>`;

    } catch (e) {
        document.getElementById('detalleBody').innerHTML =
            '<div class="empty-state"><p>Error al cargar detalle</p></div>';
    }
}

/* estadisticas */
async function verEstadisticas(id) {
    openModal('modalEstadisticas');
    document.getElementById('estadisticasBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';

    try {
        const data = await api.get(`/admin/propietarios/${id}/estadisticas`);
        if (!data) return;

        document.getElementById('estadisticasBody').innerHTML = `
      <div class="stats-prop-grid">
        <div class="stat-prop-card">
          <div class="stat-prop-value">${data.totalReservas ?? '—'}</div>
          <div class="stat-prop-label">Total Reservas</div>
        </div>
        <div class="stat-prop-card">
          <div class="stat-prop-value">${data.reservasConfirmadas ?? '—'}</div>
          <div class="stat-prop-label">Confirmadas</div>
        </div>
        <div class="stat-prop-card">
          <div class="stat-prop-value">${data.reservasCanceladas ?? '—'}</div>
          <div class="stat-prop-label">Canceladas</div>
        </div>
        <div class="stat-prop-card">
          <div class="stat-prop-value">${data.totalCanchas ?? '—'}</div>
          <div class="stat-prop-label">Canchas</div>
        </div>
        <div class="stat-prop-card">
          <div class="stat-prop-value money">
            ${data.ingresosMes != null ? formatCurrency(data.ingresosMes) : '—'}
          </div>
          <div class="stat-prop-label">Ingresos este mes</div>
        </div>
        <div class="stat-prop-card">
          <div class="stat-prop-value money">
            ${data.ingresosTotal != null ? formatCurrency(data.ingresosTotal) : '—'}
          </div>
          <div class="stat-prop-label">Ingresos totales</div>
        </div>
      </div>`;

    } catch (e) {
        document.getElementById('estadisticasBody').innerHTML =
            '<div class="empty-state"><p>Error al cargar estadisticas</p></div>';
    }
}

/* cambiar estado */
function cambiarEstado(id, nuevoEstado, nombre) {
    const msgs = {
        ACTIVO:     `¿Deseas aprobar/reactivar al propietario "${nombre}"?`,
        INACTIVO: `¿Deseas suspender al propietario "${nombre}"?`,
    };

    confirmar(msgs[nuevoEstado] || `¿Cambiar estado de "${nombre}"?`, async () => {
        try {
            await api.patch(`/admin/propietarios/${id}/estado`, { estado: nuevoEstado });
            const toasts = {
                ACTIVO:     'Propietario aprobado correctamente',
                INACTIVO: 'Propietario suspendido correctamente',
            };
            showToast(toasts[nuevoEstado] || 'Estado actualizado', 'success');
            await cargarPropietarios();
        } catch (e) {
            showToast('Error: ' + e.message, 'error');
        }
    });
}

/* eliminar */
function eliminarPropietario(id, nombre) {
    confirmar(
        `¿Eliminar al propietario "${nombre}"? Si no tiene historial se borra por completo; ` +
        `si tiene canchas o reservas registradas, la cuenta se anonimiza para no romper ese historial.`,
        () => {
            pedirMotivoEliminacion(nombre, async (motivo, comentario, forzar) => {
                try {
                    const resp = await api.delete(`/admin/propietarios/${id}`, { motivo, comentario, forzar: String(forzar) });
                    showToast(resp.mensaje || 'Propietario eliminado correctamente', 'success');
                    await cargarPropietarios();
                } catch (e) {
                    showToast('Error: ' + e.message, 'error');
                }
            });
        }
    );
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

document.addEventListener('DOMContentLoaded', () => {
    cargarPropietarios();
});

function exportarPropietarios() {
    exportarExcel(propietariosFiltrados, {
        'ID':             p => p.id,
        'Nombres':        p => p.nombres   || '',
        'Apellidos':      p => p.apellidos || '',
        'DNI/C.E.':       p => p.numeroDocumento || '',
        'Correo':         p => p.email     || '',
        'Telefono':       p => p.telefono  || '',
        'Estado':         p => p.estado    || 'ACTIVO',
        'Canchas':        p => p.totalCanchas ?? '',
        'Fecha Registro': p => formatDate(p.fechaRegistro || p.createdAt) || '',
    }, 'Propietarios');
}