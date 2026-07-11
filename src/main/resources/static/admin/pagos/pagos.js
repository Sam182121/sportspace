'use strict';

let pagos          = [];
let pagosFiltrados = [];
let paginaActual   = 1;
const porPagina    = 10;

async function cargarPagos() {
    const tbody = document.getElementById('tablaPagos');
    tbody.innerHTML = loadingRow(7);
    try {
        const data = await api.get('/admin/pagos');
        if (!data) return;
        pagos          = data.pagos || data || [];
        pagosFiltrados = [...pagos];
        calcularStats();
        cargarGraficos();
        renderTabla();
    } catch (e) {
        tbody.innerHTML = emptyRow(7, 'Error al cargar pagos');
        showToast('Error: ' + e.message, 'error');
    }
}

function calcularStats() {
    // Solo APROBADO cuenta como ingreso real
    // EN_REEMBOLSO y REEMBOLSADO NO suman
    const aprobados  = pagos.filter(p => p.estado === 'APROBADO');
    const pendientes = pagos.filter(p => p.estado === 'PENDIENTE').length;
    const rechazados = pagos.filter(p => p.estado === 'RECHAZADO').length;

    const ingresos = aprobados.reduce((acc, p) => acc + (p.monto || 0), 0);

    const ahora = new Date();
    const mes   = ahora.getMonth();
    const anio  = ahora.getFullYear();
    const ingresosMes = aprobados
        .filter(p => {
            const f = new Date(p.fecha);
            return f.getMonth() === mes && f.getFullYear() === anio;
        })
        .reduce((acc, p) => acc + (p.monto || 0), 0);

    setText('statIngresos',   formatCurrency(ingresos));
    setText('statMes',        formatCurrency(ingresosMes));
    setText('statPendientes', pendientes);
    setText('statRechazados', rechazados);
}

async function cargarGraficos() {
    cargarGraficoMetodos();
    await cargarGraficoIngresos();
}

function cargarGraficoMetodos() {
    const container = document.getElementById('chartMetodos');
    if (!container) return;

    // Solo APROBADO cuenta para el gráfico de métodos
    const aprobados = pagos.filter(p => p.estado === 'APROBADO');

    if (aprobados.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>Sin pagos aprobados</p></div>';
        return;
    }

    const total = aprobados.length;
    const metodos = {};

    aprobados.forEach(p => {
        const m = (p.metodoPago || p.metodo || 'OTRO').toUpperCase();
        if (!metodos[m]) metodos[m] = { label: labelMetodo(m), color: colorMetodo(m), count: 0 };
        metodos[m].count++;
    });

    container.innerHTML = Object.entries(metodos).map(([key, m]) => {
        const pct = Math.round((m.count / total) * 100);
        return `
      <div class="metodo-bar-item">
        <div class="metodo-bar-header">
          <span class="metodo-bar-label">
            <span class="pay-method pay-${key.toLowerCase()}">${m.label}</span>
          </span>
          <span class="metodo-bar-val">${pct}% (${m.count})</span>
        </div>
        <div class="metodo-bar-track">
          <div class="metodo-bar-fill" style="width:${pct}%;background:${m.color}"></div>
        </div>
      </div>`;
    }).join('');
}

function labelMetodo(m) {
    const map = { TRANSFERENCIA: 'Transferencia', YAPE: 'Yape', PLIN: 'Plin', EFECTIVO: 'Efectivo' };
    return map[m] || m;
}

function colorMetodo(m) {
    const map = { TRANSFERENCIA: '#1e40af', YAPE: '#6b21a8', PLIN: '#15803d', EFECTIVO: '#b45309' };
    return map[m] || '#6b7280';
}

async function cargarGraficoIngresos() {
    const container = document.getElementById('chartIngresos');
    if (!container) return;

    try {
        const data = await api.get('/admin/pagos/ingresos-semana');
        if (!data || !data.dias || data.dias.length === 0) {
            container.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text3);font-size:13px">Sin datos esta semana</div>';
            return;
        }

        const max = Math.max(...data.dias.map(d => Number(d.monto) || 0), 1);
        container.innerHTML = data.dias.map(d => {
            const monto = Number(d.monto) || 0;
            const h = Math.max(Math.round((monto / max) * 110), 4);
            return `
        <div class="chart-bar-col" title="${d.label}: ${formatCurrency(monto)}">
          <div class="chart-bar green" style="height:${h}px"></div>
          <div class="chart-bar-label">${d.label}</div>
        </div>`;
        }).join('');

        if (data.rango) setText('labelSemana', data.rango);

    } catch(e) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text3);font-size:13px">No disponible</div>';
    }
}

function renderTabla() {
    const tbody = document.getElementById('tablaPagos');
    const total = pagosFiltrados.length;

    setText('tablaTitle', `${total} pago${total !== 1 ? 's' : ''}`);
    const inicio = (paginaActual - 1) * porPagina + 1;
    const fin    = Math.min(paginaActual * porPagina, total);
    setText('tablaInfo', total > 0 ? `Mostrando ${inicio}-${fin} de ${total}` : '');

    if (total === 0) {
        tbody.innerHTML = emptyRow(7, 'No se encontraron pagos');
        document.getElementById('paginacionContainer').innerHTML = '';
        return;
    }

    const pagina = pagosFiltrados.slice(
        (paginaActual - 1) * porPagina,
        paginaActual * porPagina
    );

    tbody.innerHTML = pagina.map(p => {
        const metodo = (p.metodoPago || p.metodo || '').toUpperCase();
        return `<tr>
      <td>
        <div class="td-main">${p.usuarioNombre || '—'}</div>
        <div class="td-sub">${p.usuarioEmail || ''}</div>
      </td>
      <td>
        <span style="font-family:var(--font-head);font-size:12px;font-weight:700;color:var(--text3)">
          #${String(p.reservaId || '').padStart(4,'0')}
        </span>
      </td>
      <td>${payMethodHtml(metodo)}</td>
      <td class="td-money">${p.monto != null ? formatCurrency(p.monto) : '—'}</td>
      <td>${formatDate(p.fecha)}</td>
      <td>${badgeEstadoPago(p.estado)}</td>
      <td>
        <div class="action-group">
          <button class="btn-icon btn-view" title="Ver detalle" onclick="verComprobante(${p.id})">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
              <circle cx="12" cy="12" r="3"/>
            </svg>
          </button>
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

/* Badge de estado con todos los casos incluyendo EN_REEMBOLSO */
function badgeEstadoPago(estado) {
    const map = {
        APROBADO:     { label: 'Aprobado',         cls: 'badge-success'  },
        PENDIENTE:    { label: 'Pendiente',         cls: 'badge-warning'  },
        RECHAZADO:    { label: 'Rechazado',         cls: 'badge-danger'   },
        EN_REEMBOLSO: { label: 'En reembolso',      cls: 'badge-info'     },
        REEMBOLSADO:  { label: 'Reembolsado',       cls: 'badge-secondary'},
    };
    const e = map[estado] || { label: estado || '—', cls: 'badge-secondary' };
    return `<span class="badge ${e.cls}">${e.label}</span>`;
}

function filtrarPagos() {
    const buscar = document.getElementById('filtroBuscar').value.trim().toLowerCase();
    const metodo = document.getElementById('filtroMetodo').value;
    const estado = document.getElementById('filtroEstado').value;
    const fecha  = document.getElementById('filtroFecha').value;

    pagosFiltrados = pagos.filter(p => {
        const usuario   = (p.usuarioNombre || '').toLowerCase();
        const reservaId = `#${String(p.reservaId || '').padStart(4,'0')}`;
        const matchBuscar = !buscar || usuario.includes(buscar) || reservaId.includes(buscar);
        const matchMetodo = !metodo || (p.metodoPago || p.metodo || '').toUpperCase() === metodo;
        const matchEstado = !estado || p.estado === estado;
        const matchFecha  = !fecha  || (p.fecha || '').startsWith(fecha);
        return matchBuscar && matchMetodo && matchEstado && matchFecha;
    });

    paginaActual = 1;
    renderTabla();
}

function limpiarFiltros() {
    document.getElementById('filtroBuscar').value = '';
    document.getElementById('filtroMetodo').value = '';
    document.getElementById('filtroEstado').value = '';
    document.getElementById('filtroFecha').value  = '';
    pagosFiltrados = [...pagos];
    paginaActual = 1;
    renderTabla();
}

async function verComprobante(id) {
    openModal('modalComprobante');
    document.getElementById('comprobanteBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';
    try {
        const p = await api.get(`/admin/pagos/${id}`);
        if (!p) return;
        const metodo = (p.metodoPago || p.metodo || '').toUpperCase();
        document.getElementById('comprobanteBody').innerHTML = `
      <div class="comprobante-header">
        <div>
          <div class="comprobante-monto">${p.monto != null ? formatCurrency(p.monto) : '—'}</div>
          <div class="comprobante-fecha">Pago: ${formatDate(p.fechaPago || p.fecha)}</div>
        </div>
        ${badgeEstadoPago(p.estado)}
      </div>
      <div class="comprobante-grid">
        <div class="comp-field">
          <div class="comp-field-label">Usuario</div>
          <div class="comp-field-value">${p.clienteNombre || p.usuarioNombre || '—'}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Correo</div>
          <div class="comp-field-value">${p.clienteEmail || p.usuarioEmail || '—'}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Reserva</div>
          <div class="comp-field-value">#${String(p.reservaId || '').padStart(4,'0')}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Fecha de reserva</div>
          <div class="comp-field-value">${formatDate(p.reservaFecha) || '—'}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Horario</div>
          <div class="comp-field-value">${p.reservaHoraInicio || '—'} – ${p.reservaHoraFin || '—'}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Metodo de pago</div>
          <div class="comp-field-value">${payMethodHtml(metodo)}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Cancha</div>
          <div class="comp-field-value">${p.canchaNombre || '—'}</div>
        </div>
        <div class="comp-field">
          <div class="comp-field-label">Distrito</div>
          <div class="comp-field-value">${p.canchaDistrito || '—'}</div>
        </div>
      </div>`;
    } catch (e) {
        document.getElementById('comprobanteBody').innerHTML =
            '<div class="empty-state"><p>Error al cargar detalle</p></div>';
    }
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

document.addEventListener('DOMContentLoaded', () => { cargarPagos(); });

function exportarPagos() {
    exportarExcel(pagosFiltrados, {
        'ID':            p => p.id,
        'Usuario':       p => p.usuarioNombre || '',
        'Email':         p => p.usuarioEmail  || '',
        'Metodo Pago':   p => (p.metodoPago || p.metodo || '').toUpperCase(),
        'Monto':         p => p.monto ?? '',
        'Estado':        p => p.estado || '',
        'Fecha':         p => formatDate(p.fecha) || '',
    }, 'Pagos');
}