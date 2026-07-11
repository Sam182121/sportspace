'use strict';

/* estado */
let reservas          = [];
let reservasFiltradas = [];
let paginaActual      = 1;
const porPagina       = 10;

/* cargar reservas */
async function cargarReservas() {
    const tbody = document.getElementById('tablaReservas');
    tbody.innerHTML = loadingRow(8);
    try {
        const data = await api.get('/admin/reservas');
        if (!data) return;
        reservas          = data.reservas || data || [];
        reservasFiltradas = [...reservas];
        calcularStats();
        renderTabla();
    } catch (e) {
        tbody.innerHTML = emptyRow(8, 'Error al cargar reservas');
        showToast('Error: ' + e.message, 'error');
    }
}

function calcularStats() {
    const total       = reservas.length;
    const pendientes  = reservas.filter(r => r.estado === 'PENDIENTE').length;
    const confirmadas = reservas.filter(r => r.estado === 'CONFIRMADA').length;
    const canceladas  = reservas.filter(r => r.estado === 'CANCELADA').length;
    setText('statTotal',       total);
    setText('statPendientes',  pendientes);
    setText('statConfirmadas', confirmadas);
    setText('statCanceladas',  canceladas);
}

/* tabla */
function renderTabla() {
    const tbody = document.getElementById('tablaReservas');
    const total = reservasFiltradas.length;

    setText('tablaTitle', `${total} reserva${total !== 1 ? 's' : ''}`);
    const inicio = (paginaActual - 1) * porPagina + 1;
    const fin    = Math.min(paginaActual * porPagina, total);
    setText('tablaInfo', total > 0 ? `Mostrando ${inicio}–${fin} de ${total}` : '');

    if (total === 0) {
        tbody.innerHTML = emptyRow(8, 'No se encontraron reservas');
        document.getElementById('paginacionContainer').innerHTML = '';
        return;
    }

    const pagina = reservasFiltradas.slice(
        (paginaActual - 1) * porPagina,
        paginaActual * porPagina
    );

    tbody.innerHTML = pagina.map(r => {
        return `<tr>
      <td><span class="codigo-cell">#${String(r.id || '').padStart(4,'0')}</span></td>
      <td>
        <div class="td-main">${r.usuarioNombre || '—'}</div>
        <div class="td-sub">${r.usuarioEmail || ''}</div>
      </td>
      <td>
        <div class="td-main">${r.canchaNombre || '—'}</div>
        <div class="td-sub">${r.canhaDistrito || ''}</div>
      </td>
      <td>${formatDate(r.fecha)}</td>
      <td>
        <span class="horario-cell">
          ${r.horaInicio || '—'} - ${r.horaFin || '—'}
        </span>
      </td>
      <td class="td-money">${r.total != null ? formatCurrency(r.total) : '—'}</td>
      <td>${badgeEstado(r.estado)}</td>
      <td>
        <div class="action-group">
          <!-- Solo el ojito: admin solo visualiza, no cancela ni confirma -->
          <button class="btn-icon btn-view" title="Ver detalle" onclick="verDetalle(${r.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
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
function filtrarReservas() {
    const buscar = document.getElementById('filtroBuscar').value.trim().toLowerCase();
    const estado = document.getElementById('filtroEstado').value;
    const fecha  = document.getElementById('filtroFecha').value;

    reservasFiltradas = reservas.filter(r => {
        const codigo  = `#${String(r.id || '').padStart(4,'0')}`.toLowerCase();
        const usuario = (r.usuarioNombre || '').toLowerCase();
        const cancha  = (r.canchaNombre  || '').toLowerCase();
        const matchBuscar = !buscar || codigo.includes(buscar) || usuario.includes(buscar) || cancha.includes(buscar);
        const matchEstado = !estado || r.estado === estado;
        const matchFecha  = !fecha  || (r.fecha || '').startsWith(fecha);
        return matchBuscar && matchEstado && matchFecha;
    });

    paginaActual = 1;
    renderTabla();
}

function limpiarFiltros() {
    document.getElementById('filtroBuscar').value = '';
    document.getElementById('filtroEstado').value = '';
    document.getElementById('filtroFecha').value  = '';
    reservasFiltradas = [...reservas];
    paginaActual = 1;
    renderTabla();
}

/* detalle — solo lectura, sin botones de acción */
async function verDetalle(id) {
    openModal('modalDetalle');
    document.getElementById('detalleBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';

    try {
        const r = await api.get(`/admin/reservas/${id}`);
        if (!r) return;

        document.getElementById('detalleBody').innerHTML = `
      <div class="detalle-reserva-header">
        <div>
          <div class="detalle-codigo">#${String(r.id).padStart(4,'0')}</div>
          <div class="detalle-fecha-hora">
            ${formatDate(r.fecha)} — ${r.horaInicio || '—'} a ${r.horaFin || '—'}
          </div>
        </div>
        ${badgeEstado(r.estado)}
      </div>
      <div class="detalle-grid">
        <div class="detalle-field">
          <div class="detalle-field-label">Usuario</div>
          <div class="detalle-field-value">${r.usuarioNombre || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Correo</div>
          <div class="detalle-field-value">${r.usuarioEmail || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Cancha</div>
          <div class="detalle-field-value">${r.canchaNombre || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Propietario</div>
          <div class="detalle-field-value">${r.propietarioNombre || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Ubicacion</div>
          <div class="detalle-field-value">${r.canhaDistrito || r.canchaDireccion || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Deporte</div>
          <div class="detalle-field-value">${r.deporte || '—'}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Duracion</div>
          <div class="detalle-field-value">${r.duracionHoras ?? '—'} hora${r.duracionHoras !== 1 ? 's' : ''}</div>
        </div>
        <div class="detalle-field">
          <div class="detalle-field-label">Total</div>
          <div class="detalle-field-value money">
            ${r.total != null ? formatCurrency(r.total) : '—'}
          </div>
        </div>
      </div>`;
        /* Sin botones de acción — el admin solo visualiza */

    } catch (e) {
        document.getElementById('detalleBody').innerHTML =
            '<div class="empty-state"><p>Error al cargar detalle</p></div>';
    }
}

/* exportar */
function exportarReservas() {
    exportarExcel(reservasFiltradas, {
        'Codigo':        r => '#' + String(r.id || '').padStart(4, '0'),
        'Usuario':       r => r.usuarioNombre || '',
        'Email Usuario': r => r.usuarioEmail  || '',
        'Cancha':        r => r.canchaNombre  || '',
        'Deporte':       r => r.deporte       || '',
        'Fecha':         r => formatDate(r.fecha) || '',
        'Hora Inicio':   r => r.horaInicio    || '',
        'Hora Fin':      r => r.horaFin       || '',
        'Total':         r => r.total         ?? '',
        'Estado':        r => r.estado        || '',
    }, 'Reservas');
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

document.addEventListener('DOMContentLoaded', () => {
    cargarReservas();
});