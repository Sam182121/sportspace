'use strict';

/* carga */
async function cargarEstadisticas() {
    await Promise.allSettled([
        cargarResumen(),
        cargarGraficoUsuarios(),
        cargarGraficoIngresos(),
        cargarCanchasMasReservadas(),
        cargarDeportes(),
        cargarHorarios(),
        cargarDiasSemana(),
    ]);
}

/* resumen */
async function cargarResumen() {
    try {
        const data = await api.get('/admin/estadisticas/resumen');
        if (!data) return;

        setText('statUsuarios',    data.totalUsuarios    ?? '—');
        setText('statReservas',    data.totalReservas    ?? '—');
        setText('statCanchas',     data.canchasActivas   ?? '—');
        setText('statIngresos',    data.ingresosTotal != null ? formatCurrency(data.ingresosTotal) : '—');

        if (data.nuevosUsuariosMes)
            setText('statUsuariosSub', `+${data.nuevosUsuariosMes} este mes`);
        if (data.reservasMes)
            setText('statReservasSub', `${data.reservasMes} este mes`);
        if (data.ingresosMes)
            setText('statIngresosSub', `${formatCurrency(data.ingresosMes)} este mes`);

    } catch (e) {
        console.error('Error resumen:', e);
    }
}

/* usuarios graficos */
async function cargarGraficoUsuarios() {
    const container = document.getElementById('chartUsuarios');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/usuarios-mensual');
        if (!data || !data.meses) {
            container.innerHTML = sinDatos();
            return;
        }

        const max = Math.max(...data.meses.map(m => m.cantidad), 1);
        container.innerHTML = data.meses.map(m => {
            const h = Math.max(Math.round((m.cantidad / max) * 110), 4);
            return `
        <div class="chart-bar-col" title="${m.label}: ${m.cantidad} usuarios">
          <div class="chart-bar blue" style="height:${h}"></div>
          <div class="chart-bar-label">${m.label}</div>
        </div>`;
        }).join('');

        if (data.rango) setText('labelUsuarios', data.rango);

    } catch (e) {
        container.innerHTML = sinDatos();
    }
}

/* inresos menusales */
async function cargarGraficoIngresos() {
    const container = document.getElementById('chartIngresos');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/ingresos-mensual');
        if (!data || !data.meses) {
            container.innerHTML = sinDatos();
            return;
        }

        const max = Math.max(...data.meses.map(m => m.monto), 1);
        container.innerHTML = data.meses.map(m => {
            const h = Math.max(Math.round((m.monto / max) * 110), 4);
            return `
        <div class="chart-bar-col" title="${m.label}: ${formatCurrency(m.monto)}">
          <div class="chart-bar green" style="height:${h}"></div>
          <div class="chart-bar-label">${m.label}</div>
        </div>`;
        }).join('');

        if (data.rango) setText('labelIngresos', data.rango);

    } catch (e) {
        container.innerHTML = sinDatos();
    }
}

/* canchas mas reservadas */
async function cargarCanchasMasReservadas() {
    const container = document.getElementById('chartCanchas');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/canchas-top');
        if (!data || !data.canchas || data.canchas.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>Sin datos</p></div>';
            return;
        }

        const max = Math.max(...data.canchas.map(c => c.reservas), 1);
        const colores = [
            'var(--accent)', 'var(--success)', 'var(--warning)',
            'var(--purple)', 'var(--danger)',
        ];

        container.innerHTML = data.canchas.map((c, i) => {
            const pct = Math.round((c.reservas / max) * 100);
            return `
        <div class="est-progress-item">
          <div class="est-progress-header">
            <span class="est-progress-name" title="${c.nombre}">${c.nombre}</span>
            <span class="est-progress-val">${c.reservas} reservas</span>
          </div>
          <div class="est-progress-track">
            <div class="est-progress-fill" style="width:${pct}%;background:${colores[i % colores.length]}"></div>
          </div>
        </div>`;
        }).join('');

    } catch (e) {
        container.innerHTML = '<div class="empty-state"><p>No disponible</p></div>';
    }
}

/* deportes populares */
async function cargarDeportes() {
    const container = document.getElementById('chartDeportes');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/deportes');
        if (!data || !data.deportes || data.deportes.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>Sin datos</p></div>';
            return;
        }

        const total  = data.deportes.reduce((acc, d) => acc + d.reservas, 0) || 1;
        const colores = [
            'var(--accent)', 'var(--success)', 'var(--warning)',
            'var(--purple)', 'var(--danger)',
        ];

        container.innerHTML = data.deportes.map((d, i) => {
            const pct = Math.round((d.reservas / total) * 100);
            return `
        <div class="est-progress-item">
          <div class="est-progress-header">
            <span class="est-progress-name">${d.deporte}</span>
            <span class="est-progress-val">${pct}%</span>
          </div>
          <div class="est-progress-track">
            <div class="est-progress-fill" style="width:${pct}%;background:${colores[i % colores.length]}"></div>
          </div>
        </div>`;
        }).join('');

    } catch (e) {
        container.innerHTML = '<div class="empty-state"><p>No disponible</p></div>';
    }
}

/* horarios mas usados */
async function cargarHorarios() {
    const container = document.getElementById('chartHorarios');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/horarios');
        if (!data || !data.horarios || data.horarios.length === 0) {
            container.innerHTML = '<div class="empty-state"><p>Sin datos</p></div>';
            return;
        }

        const max = Math.max(...data.horarios.map(h => h.reservas), 1);

        container.innerHTML = data.horarios.map((h, i) => {
            const pct = Math.round((h.reservas / max) * 100);
            const color = i === 0 ? 'var(--accent)'
                : i === 1 ? 'var(--success)'
                    : i === 2 ? 'var(--warning)'
                        : 'var(--text3)';
            return `
        <div class="est-progress-item">
          <div class="est-progress-header">
            <span class="est-progress-name">${h.horario}</span>
            <span class="est-progress-val">${h.reservas} reservas</span>
          </div>
          <div class="est-progress-track">
            <div class="est-progress-fill" style="width:${pct}%;background:${color}"></div>
          </div>
        </div>`;
        }).join('');

    } catch (e) {
        container.innerHTML = '<div class="empty-state"><p>No disponible</p></div>';
    }
}

/* reservas por dia semana */
async function cargarDiasSemana() {
    const container = document.getElementById('chartDias');
    if (!container) return;

    try {
        const data = await api.get('/admin/estadisticas/dias-semana');
        if (!data || !data.dias) {
            container.innerHTML = sinDatos();
            return;
        }

        const max = Math.max(...data.dias.map(d => d.reservas), 1);
        container.innerHTML = data.dias.map(d => {
            const h = Math.max(Math.round((d.reservas / max) * 110), 4);
            return `
        <div class="chart-bar-col" title="${d.dia}: ${d.reservas} reservas">
          <div class="chart-bar blue" style="height:${h}"></div>
          <div class="chart-bar-label">${d.dia}</div>
        </div>`;
        }).join('');

    } catch (e) {
        container.innerHTML = sinDatos();
    }
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function sinDatos() {
    return `<div style="text-align:center;padding:24px;color:var(--text3);font-size:13px">Sin datos disponibles</div>`;
}

document.addEventListener('DOMContentLoaded', () => {
    cargarEstadisticas();
});