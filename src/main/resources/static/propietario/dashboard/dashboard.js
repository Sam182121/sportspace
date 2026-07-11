'use strict';

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Dashboard');
    mostrarFecha();
    await Promise.all([cargarStats(), cargarPendientes(), cargarChart(), cargarOcupacion()]);
});

function mostrarFecha() {
    const user = getUser();
    if (user) setText('welcomeMsg', `Bienvenido, ${user.nombres}`);
    const hoy = new Date();
    const str = hoy.toLocaleDateString('es-PE', { weekday:'long', year:'numeric', month:'long', day:'numeric' });
    setText('fechaHoy', str.charAt(0).toUpperCase() + str.slice(1));
    setText('fechaOcupacion', hoy.toLocaleDateString('es-PE', { weekday:'long', day:'numeric', month:'short' }));
}

async function cargarStats() {
    try {
        const d = await api.get('/propietario/dashboard/stats');
        setText('statIngresos',    fmtMoney(d.ingresosMes));
        setText('statIngresosSub', `vs ${fmtMoney(d.ingresosMesAnterior)} mes anterior`);
        const pct = d.ingresosMesAnterior > 0
            ? Math.round(((d.ingresosMes - d.ingresosMesAnterior) / d.ingresosMesAnterior) * 100) : 0;
        setText('trendIngresos', `${pct >= 0 ? '+' : ''}${pct}%`);
        setText('statReservasHoy',    d.reservasHoy ?? 0);
        setText('statReservasHoySub', `${d.reservasPendientes ?? 0} pendientes de aprobar`);
        setText('statCanchas',    d.canchasActivas ?? 0);
        setText('statCanchasSub', `de ${d.canchasTotal ?? 0} canchas totales`);
        setText('statClientes',   d.clientesUnicos ?? 0);
    } catch (e) { toast('Error al cargar estadísticas', 'error'); }
}

async function cargarPendientes() {
    const el = document.getElementById('listaPendientes');
    const badge = document.getElementById('badgePendientes');
    try {
        const data = await api.get('/propietario/reservas?estado=PENDIENTE&size=5');
        const lista = data.content ?? data ?? [];
        if (lista.length === 0) {
            el.innerHTML = '<div class="empty-state" style="padding:24px"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:32px;height:32px;stroke:var(--border2)"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/></svg><p style="font-size:12px">Sin reservas pendientes</p></div>';
            return;
        }
        badge.textContent = lista.length;
        badge.style.display = '';
        el.innerHTML = lista.map(r => `
            <div class="pending-item">
                <div class="pending-info">
                    <div class="pending-name">${r.canchaName ?? r.cancha?.nombre ?? '—'}</div>
                    <div class="pending-sub">${r.clienteNombre ?? '—'} · ${fmt(r.fecha)} ${r.horaInicio ?? ''}</div>
                </div>
                <a href="/propietario/reservas" class="btn-icon view" title="Ver en Reservas">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
                </a>
            </div>`).join('');
    } catch { el.innerHTML = '<div style="padding:16px;text-align:center;color:var(--text3);font-size:12px">No se pudo cargar</div>'; }
}

async function cargarChart() {
    const cont = document.getElementById('listaIngresosSemana');
    try {
        const data = await api.get('/propietario/dashboard/ingresos-semana');
        const dias = data.dias ?? [];
        if (!cont) return;

        if (!dias.length) {
            cont.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text3);font-size:12px">Sin datos</div>';
            return;
        }

        const max = Math.max(...dias.map(d => Number(d.total) || 0), 1);
        const diasNom = ['Dom','Lun','Mar','Mié','Jue','Vie','Sáb'];
        const hoyStr = new Date().toISOString().slice(0, 10);

        cont.innerHTML = dias.map(d => {
            const total  = Number(d.total) || 0;
            const pct    = Math.max((total / max) * 100, total > 0 ? 4 : 0);
            const fechaObj = new Date(d.fecha);
            const esHoy  = String(d.fecha).slice(0, 10) === hoyStr;
            const diaTxt = `${diasNom[fechaObj.getDay()]} ${String(fechaObj.getDate()).padStart(2,'0')}/${String(fechaObj.getMonth()+1).padStart(2,'0')}`;
            return `
                <div style="display:flex;align-items:center;gap:10px;padding:8px 2px;
                     border-bottom:1px solid var(--border,#eef1f6)">
                    <div style="width:64px;font-size:12px;color:${esHoy ? 'var(--orange,#d97706)' : 'var(--text2,#334155)'};
                         font-weight:${esHoy ? '700' : '500'}">${diaTxt}</div>
                    <div style="flex:1;background:var(--border,#eef1f6);border-radius:6px;height:8px;overflow:hidden">
                        <div style="width:${pct}%;height:100%;border-radius:6px;
                             background:${esHoy ? 'var(--orange,#d97706)' : 'var(--accent,#2563eb)'}"></div>
                    </div>
                    <div style="width:82px;text-align:right;font-weight:600;font-size:13px">${fmtMoney(total)}</div>
                </div>`;
        }).join('');

        setText('totalSemana', `Total: ${fmtMoney(dias.reduce((a, d) => a + (Number(d.total) || 0), 0))}`);
    } catch (e) {
        if (cont) cont.innerHTML = '<div style="text-align:center;padding:20px;color:var(--text3);font-size:12px">No se pudo cargar</div>';
    }
}

async function cargarOcupacion() {
    const el = document.getElementById('ocupacionList');
    try {
        const data = await api.get('/propietario/dashboard/ocupacion-hoy');
        const lista = data ?? [];
        if (!lista.length) { el.innerHTML = '<p style="color:var(--text3);font-size:13px">Sin datos de ocupación</p>'; return; }
        el.innerHTML = `<div class="ocupacion-row">${lista.map(c => {
            const pct = c.totalHoras > 0 ? Math.round((c.horasOcupadas / c.totalHoras) * 100) : 0;
            const color = pct >= 80 ? 'green' : pct >= 50 ? '' : 'orange';
            return `<div>
                <div class="ocup-item">
                    <span class="ocup-name">${c.nombre}</span>
                    <span class="ocup-hrs">${c.horasOcupadas}/${c.totalHoras} horas</span>
                </div>
                <div class="progress-bar-wrap"><div class="progress-bar ${color}" style="width:${pct}%"></div></div>
            </div>`;
        }).join('')}</div>`;
    } catch { el.innerHTML = '<p style="color:var(--text3);font-size:13px">Sin datos disponibles</p>'; }
}