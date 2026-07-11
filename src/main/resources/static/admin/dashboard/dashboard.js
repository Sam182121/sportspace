'use strict';

/* reloj */
function mostrarFechaHoy() {
    const hoy = new Date();
    const opciones = {
        weekday: 'long', year: 'numeric',
        month: 'long', day: 'numeric',
    };
    const fecha = hoy.toLocaleDateString('es-PE', opciones);
    const el = document.getElementById('fechaHoy');
    if (el) el.textContent = fecha.charAt(0).toUpperCase() + fecha.slice(1);
}

/* estadisticas carga */
async function cargarEstadisticas() {
    try {
        const data = await api.get('/admin/dashboard/stats');
        if (!data) return;

        // Usuarios
        setText('statUsuarios',       data.totalUsuarios     ?? '—');
        setText('statUsuariosSub',    data.nuevosEstesMes
            ? `+${data.nuevosEsteMes} este mes` : '');

        // Propietarios
        setText('statPropietarios',   data.totalPropietarios ?? '—');
        setText('statPropietariosSub', data.propietariosPendientes
            ? `${data.propietariosPendientes} pendientes` : '');

        // Canchas
        setText('statCanchas',        data.totalCanchas      ?? '—');
        setText('statCanchasSub',     data.canchasActivas
            ? `${data.canchasActivas} activas` : '');

        // Reservas hoy
        setText('statReservasHoy',    data.reservasHoy       ?? '—');
        setText('statReservasHoySub', data.reservasPendientes
            ? `${data.reservasPendientes} pendientes` : '');

        // Ingresos dia
        setText('statIngresosDia',    data.ingresosDia != null
            ? formatCurrency(data.ingresosDia) : '—');
        setText('statIngresosDiaSub', '');

        // Ingresos mes
        setText('statIngresosMes',    data.ingresosMes != null
            ? formatCurrency(data.ingresosMes) : '—');
        setText('statIngresosMesSub', '');

        // Usuarios conectados
        setText('statConectados',     data.usuariosConectados ?? '—');



    } catch (e) {
        console.error('Error cargando estadisticas:', e);
        // Mostrar guiones en todos los stats
        ['statUsuarios','statPropietarios','statCanchas','statReservasHoy',
            'statIngresosDia','statIngresosMes','statConectados']
            .forEach(id => setText(id, '—'));
    }
}

/* ingresos semanalas grafico */
async function cargarGraficoIngresos() {
    const container = document.getElementById('chartIngresos');
    if (!container) return;

    try {
        const data = await api.get('/admin/dashboard/ingresos-semana');
        if (!data || !data.dias) {
            container.innerHTML = emptyDashboard('Sin datos de ingresos');
            return;
        }

        const max = Math.max(...data.dias.map(d => d.monto), 1);
        let html = '';
        data.dias.forEach(dia => {
            const height = Math.max(Math.round((dia.monto / max) * 110), 4);
            html += `
        <div class="chart-bar-col" title="${dia.label}: ${formatCurrency(dia.monto)}">
          <div class="chart-bar blue" style="height:${height}px"></div>
          <div class="chart-bar-label">${dia.label}</div>
        </div>`;
        });
        container.innerHTML = html;

        // Actualizar label semana
        if (data.rango) setText('semanaLabel', data.rango);

    } catch (e) {
        container.innerHTML = emptyDashboard('No disponible');
    }
}

/* reserva por deporte graficos */
async function cargarGraficoDeportes() {
    const container = document.getElementById('chartDeportes');
    if (!container) return;

    try {
        const data = await api.get('/admin/dashboard/reservas-deporte');
        if (!data || !data.deportes || data.deportes.length === 0) {
            container.innerHTML = emptyDashboard('Sin datos de reservas');
            return;
        }

        const colores = ['var(--accent)', 'var(--success)', 'var(--warning)', 'var(--purple)', 'var(--text3)'];
        let html = '';
        data.deportes.forEach((d, i) => {
            html += `
        <div class="progress-item">
          <div class="progress-header">
            <span class="progress-name">${d.deporte}</span>
            <span class="progress-val">${d.porcentaje}%</span>
          </div>
          <div class="progress-track">
            <div class="progress-fill" style="width:${d.porcentaje}%;background:${colores[i] || 'var(--text3)'}"></div>
          </div>
        </div>`;
        });
        container.innerHTML = html;

    } catch (e) {
        container.innerHTML = emptyDashboard('No disponible');
    }
}

/* alertas pero aun no existe */
/* ultimas reservas */
async function cargarUltimasReservas() {
    const tbody = document.getElementById('tablaReservas');
    if (!tbody) return;

    try {
        const data = await api.get('/admin/dashboard/ultimas-reservas');
        if (!data || !data.reservas || data.reservas.length === 0) {
            tbody.innerHTML = emptyRow(5, 'Sin reservas recientes');
            return;
        }

        tbody.innerHTML = data.reservas.map(r => `
      <tr>
        <td>
          <div class="td-main">${r.usuarioNombre || '—'}</div>
        </td>
        <td>${r.canchaNombre || '—'}</td>
        <td>${formatDate(r.fecha)}</td>
        <td class="td-money">${r.total != null ? formatCurrency(r.total) : '—'}</td>
        <td>${badgeEstado(r.estado)}</td>
      </tr>`).join('');

    } catch (e) {
        tbody.innerHTML = emptyRow(5, 'Error al cargar reservas');
    }
}

/* utlimos pagos */
async function cargarUltimosPagos() {
    const tbody = document.getElementById('tablaPagos');
    if (!tbody) return;

    try {
        const data = await api.get('/admin/dashboard/ultimos-pagos');
        if (!data || !data.pagos || data.pagos.length === 0) {
            tbody.innerHTML = emptyRow(4, 'Sin pagos recientes');
            return;
        }

        tbody.innerHTML = data.pagos.map(p => `
      <tr>
        <td>
          <div class="td-main">${p.usuarioNombre || '—'}</div>
        </td>
        <td>${payMethodHtml(p.metodo)}</td>
        <td class="td-money">${p.monto != null ? formatCurrency(p.monto) : '—'}</td>
        <td>${badgeEstado(p.estado)}</td>
      </tr>`).join('');

    } catch (e) {
        tbody.innerHTML = emptyRow(4, 'Error al cargar pagos');
    }
}


function setText(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = value;
}

function emptyDashboard(msg) {
    return `<div style="text-align:center;padding:24px;color:var(--text3);font-size:13px">${msg}</div>`;
}

/*DASHBOARD*/
async function initDashboard() {
    mostrarFechaHoy();

    // Cargar todo en paralelo
    await Promise.allSettled([
        cargarEstadisticas(),
        cargarGraficoIngresos(),
        cargarGraficoDeportes(),
        cargarUltimasReservas(),
        cargarUltimosPagos(),
    ]);
}

document.addEventListener('DOMContentLoaded', () => {
    initDashboard();
});