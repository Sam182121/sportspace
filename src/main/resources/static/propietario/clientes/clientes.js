'use strict';

let todosClientes = [];

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Clientes');
    await cargarClientes();
});

async function cargarClientes() {
    try {
        const data = await api.get('/propietario/clientes');
        todosClientes = data.clientes ?? [];

        setText('statTotal',      data.totalClientes ?? 0);
        setText('statFrecuentes', data.clientesFrecuentes ?? 0);
        setText('statNuevos',     data.clientesNuevosMes ?? 0);
        setText('statPromedio',   fmtMoney(data.gastoPromedio));
        setText('totalLabel',     `${data.totalClientes ?? 0} clientes`);

        const tbody = document.getElementById('tablaClientesTbody');
        if (tbody) tbody.innerHTML = renderTablaClientes(todosClientes);
    } catch {
        const tbody = document.getElementById('tablaClientesTbody');
        if (tbody) tbody.innerHTML = `<tr><td colspan="7">
            <div class="empty-state"><h4>Error al cargar clientes</h4></div></td></tr>`;
    }
}

function renderTablaClientes(lista) {
    if (!lista.length) {
        return `<tr><td colspan="7"><div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
            </svg>
            <h4>Sin clientes</h4>
            <p>Aún no tienes clientes que hayan reservado tus canchas.</p>
        </div></td></tr>`;
    }
    return lista.map((c, i) => `
        <tr>
            <td>
                <div class="td-main">${c.nombres ?? ''} ${c.apellidos ?? ''}</div>
                <div class="td-sub">${c.email ?? '—'} · ${c.telefono ?? '—'}</div>
            </td>
            <td><span class="badge badge-info">${c.totalReservas ?? 0} reservas</span></td>
            <td>${fmt(c.ultimaVisita)}</td>
            <td style="font-weight:600;color:var(--success)">${fmtMoney(c.totalGastado)}</td>
            <td>${c.canchaFavorita ?? '—'}</td>
            <td>${tipoBadge(c.totalReservas ?? 0)}</td>
            <td>
                <button class="btn-icon view" title="Ver detalle" onclick="verDetalleCliente(${i})">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                    </svg>
                </button>
            </td>
        </tr>`).join('');
}

function tipoBadge(n) {
    if (n >= 10) return '<span class="badge badge-success">Frecuente</span>';
    if (n >= 5)  return '<span class="badge badge-info">Regular</span>';
    if (n >= 2)  return '<span class="badge badge-gray">Ocasional</span>';
    return '<span class="badge badge-orange">Nuevo</span>';
}

function filtrarClientes(q) {
    const t = q.toLowerCase();
    const lista = !t ? todosClientes : todosClientes.filter(c =>
        `${c.nombres} ${c.apellidos}`.toLowerCase().includes(t) ||
        c.email?.toLowerCase().includes(t) ||
        c.telefono?.includes(t)
    );
    const tbody = document.getElementById('tablaClientesTbody');
    if (tbody) tbody.innerHTML = renderTablaClientes(lista);
}

/* ── MODAL DETALLE CLIENTE ──────────────────────────────────── */
function verDetalleCliente(idx) {
    const c = todosClientes[idx];
    if (!c) return;
    const body = document.getElementById('modalClienteBody');
    body.innerHTML = `
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px">
            <div style="width:48px;height:48px;border-radius:50%;background:var(--accent-bg,#e8f0fe);display:flex;align-items:center;justify-content:center;font-size:18px;font-weight:700;color:var(--accent)">
                ${(c.nombres?.[0] ?? '?').toUpperCase()}
            </div>
            <div>
                <div style="font-weight:700;font-size:15px">${c.nombres ?? ''} ${c.apellidos ?? ''}</div>
                <div style="font-size:12px;color:var(--text3)">${tipoBadge(c.totalReservas ?? 0)}</div>
            </div>
        </div>
        <div style="display:grid;gap:9px;font-size:13px">
            <div style="display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding-bottom:8px">
                <span style="color:var(--text3)">Email</span><span>${c.email ?? '—'}</span>
            </div>
            <div style="display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding-bottom:8px">
                <span style="color:var(--text3)">Teléfono</span><span>${c.telefono ?? '—'}</span>
            </div>
            <div style="display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding-bottom:8px">
                <span style="color:var(--text3)">Total reservas</span>
                <strong>${c.totalReservas ?? 0}</strong>
            </div>
            <div style="display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding-bottom:8px">
                <span style="color:var(--text3)">Total gastado</span>
                <strong style="color:var(--success)">${fmtMoney(c.totalGastado)}</strong>
            </div>
            <div style="display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding-bottom:8px">
                <span style="color:var(--text3)">Última visita</span><span>${fmt(c.ultimaVisita)}</span>
            </div>
            <div style="display:flex;justify-content:space-between">
                <span style="color:var(--text3)">Cancha favorita</span>
                <span>${c.canchaFavorita ?? '—'}</span>
            </div>
        </div>`;
    openModal('modalCliente');
}