'use strict';

let canchas          = [];
let canchasFiltradas = [];
let paginaActual     = 1;
const porPagina      = 10;

const DEPORTE_MAP = {
    FUTBOL:     { label: 'Fútbol',     cls: 'dep-futbol'     },
    BASQUETBOL: { label: 'Básquet',    cls: 'dep-basquetbol' },
    VOLEY:      { label: 'Vóley',      cls: 'dep-voley'      },
    TENIS:      { label: 'Tenis',      cls: 'dep-tenis'      },
    PADEL:      { label: 'Pádel',      cls: 'dep-padel'      },
    OTRO:       { label: 'Otro',       cls: 'dep-otro'       },
};

function deporteBadge(deporte) {
    const d = DEPORTE_MAP[(deporte || '').toUpperCase()] || DEPORTE_MAP.OTRO;
    return `<span class="deporte-badge ${d.cls}">${d.label}</span>`;
}

function estadoCancha(estado) {
    const cls   = (estado || 'ACTIVA').toLowerCase();
    const labels = { ACTIVA:'Activa', INACTIVA:'Inactiva', PENDIENTE:'Pendiente', DESTACADA:'Destacada' };
    return `<span class="estado-cancha ${cls}">${labels[estado] ?? estado}</span>`;
}

/* ── CARGA ──────────────────────────────────────────────────── */
async function cargarCanchas() {
    const tbody = document.getElementById('tablaCanchas');
    tbody.innerHTML = loadingRow(7);
    try {
        const data = await api.get('/admin/canchas');
        canchas          = data.canchas || data || [];
        canchasFiltradas = [...canchas];
        calcularStats();
        renderTabla();
    } catch (e) {
        tbody.innerHTML = emptyRow(7, 'Error al cargar canchas');
        showToast('Error: ' + e.message, 'error');
    }
}

function calcularStats() {
    setText('statTotal',      canchas.length);
    setText('statActivas',    canchas.filter(c => c.estado === 'ACTIVA' || c.estado === 'DESTACADA').length);
    setText('statPendientes', canchas.filter(c => c.estado === 'PENDIENTE').length);
    setText('statInactivas',  canchas.filter(c => c.estado === 'INACTIVA').length);
}

/* ── TABLA ──────────────────────────────────────────────────── */
function renderTabla() {
    const tbody = document.getElementById('tablaCanchas');
    const total = canchasFiltradas.length;
    setText('tablaTitle', `${total} cancha${total !== 1 ? 's' : ''}`);

    const inicio = (paginaActual - 1) * porPagina + 1;
    const fin    = Math.min(paginaActual * porPagina, total);
    setText('tablaInfo', total > 0 ? `Mostrando ${inicio}–${fin} de ${total}` : '');

    if (total === 0) {
        tbody.innerHTML = emptyRow(7, 'No se encontraron canchas');
        document.getElementById('paginacionContainer').innerHTML = '';
        return;
    }

    const pagina = canchasFiltradas.slice(
        (paginaActual - 1) * porPagina,
        paginaActual * porPagina
    );

    tbody.innerHTML = pagina.map(c => {
        const estado    = (c.estado || 'ACTIVA').toUpperCase();
        const destacada = estado === 'DESTACADA';
        const esPendiente = estado === 'PENDIENTE';
        const esActiva    = estado === 'ACTIVA' || destacada;
        const esInactiva  = estado === 'INACTIVA';

        return `<tr>
            <td>
                <div class="cancha-cell-info">
                    <div class="td-main">${c.nombre || '—'}${destacada
            ? ' <span class="badge-destacada">DESTACADA</span>' : ''}</div>
                    <div class="td-sub">Cap: ${c.capacidad ?? '—'} personas</div>
                </div>
            </td>
            <td><div class="td-main">${c.propietarioNombre || '—'}</div></td>
            <td>${deporteBadge(c.deporte)}</td>
            <td>
                <div class="td-main">${c.distrito || '—'}</div>
                <div class="td-sub">${c.departamento || ''}</div>
            </td>
            <td class="precio-cell">S/. ${c.precioPorHora ?? '—'}</td>
            <td>${estadoCancha(c.estado)}</td>
            <td>
                <div class="action-group">
                    <!-- Ver detalle -->
                    <button class="btn-icon btn-view" title="Ver detalle"
                            onclick="verDetalle(${c.id})">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>

                    <!-- Aprobar (solo PENDIENTE) -->
                    ${esPendiente ? `
                    <button class="btn-icon btn-approve" title="Aprobar cancha"
                            onclick="cambiarEstado(${c.id},'ACTIVA','${esc(c.nombre)}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                            <polyline points="22 4 12 14.01 9 11.01"/>
                        </svg>
                    </button>` : ''}

                    <!-- Desactivar (si está activa/destacada) -->
                    ${esActiva ? `
                    <button class="btn-icon btn-hide" title="Desactivar cancha"
                            onclick="cambiarEstado(${c.id},'INACTIVA','${esc(c.nombre)}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                            <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                            <line x1="1" y1="1" x2="23" y2="23"/>
                        </svg>
                    </button>` : ''}

                    <!-- Reactivar (si está inactiva) -->
                    ${esInactiva ? `
                    <button class="btn-icon btn-approve" title="Reactivar cancha"
                            onclick="cambiarEstado(${c.id},'ACTIVA','${esc(c.nombre)}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                            <circle cx="12" cy="12" r="3"/>
                        </svg>
                    </button>` : ''}

                    <!-- Eliminar -->
                    <button class="btn-icon btn-delete" title="Eliminar"
                            onclick="eliminarCancha(${c.id},'${esc(c.nombre)}')">
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

/* ── FILTROS ────────────────────────────────────────────────── */
function filtrarCanchas() {
    const buscar  = document.getElementById('filtroBuscar').value.trim().toLowerCase();
    const deporte = document.getElementById('filtroDeporte').value;
    const estado  = document.getElementById('filtroEstado').value;

    canchasFiltradas = canchas.filter(c => {
        const matchBuscar  = !buscar  || (c.nombre||'').toLowerCase().includes(buscar)
            || (c.propietarioNombre||'').toLowerCase().includes(buscar);
        const matchDeporte = !deporte || (c.deporte||'').toUpperCase() === deporte;
        const matchEstado  = !estado  || (c.estado||'').toUpperCase() === estado;
        return matchBuscar && matchDeporte && matchEstado;
    });
    paginaActual = 1;
    renderTabla();
}

function limpiarFiltros() {
    document.getElementById('filtroBuscar').value  = '';
    document.getElementById('filtroDeporte').value = '';
    document.getElementById('filtroEstado').value  = '';
    canchasFiltradas = [...canchas];
    paginaActual = 1;
    renderTabla();
}

/* ── VER DETALLE + FOTOS ────────────────────────────────────── */
let _lbFotos = [], _lbActual = 0;

async function verDetalle(id) {
    openModal('modalDetalle');
    document.getElementById('detalleBody').innerHTML =
        '<div class="loading-center"><div class="spinner"></div></div>';
    try {
        const c = await api.get(`/admin/canchas/${id}`);
        const fotos = Array.isArray(c.fotos) && c.fotos.length > 0 ? c.fotos : [];

        const fotosHtml = fotos.length > 0
            ? `<div class="fotos-section">
                <div class="fotos-label">Fotos de la cancha</div>
                <div class="fotos-grid">
                  ${fotos.map((url, i) => `
                    <div class="foto-thumb" onclick="abrirLightbox('${url}',${i},${JSON.stringify(fotos).replace(/"/g,'&quot;')})">
                        <img src="${url}" alt="Foto ${i+1}" loading="lazy"/>
                        <div class="foto-lupa">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <circle cx="11" cy="11" r="8"/>
                                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                                <line x1="11" y1="8" x2="11" y2="14"/>
                                <line x1="8" y1="11" x2="14" y2="11"/>
                            </svg>
                        </div>
                    </div>`).join('')}
                </div>
               </div>`
            : `<div class="fotos-section">
                <div class="fotos-label">Fotos de la cancha</div>
                <div class="fotos-placeholder">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <rect x="3" y="3" width="18" height="18" rx="2"/>
                        <circle cx="8.5" cy="8.5" r="1.5"/>
                        <polyline points="21 15 16 10 5 21"/>
                    </svg>
                    <span>Sin fotos publicadas</span>
                </div>
               </div>`;

        document.getElementById('detalleBody').innerHTML = `
            <div class="detalle-cancha-header">
                <div class="detalle-cancha-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <rect x="2" y="7" width="20" height="14" rx="2"/>
                        <path d="M12 7v14M2 14h20"/>
                    </svg>
                </div>
                <div>
                    <div class="detalle-cancha-nombre">${c.nombre || '—'}</div>
                    <div class="detalle-cancha-sub">${c.propietarioNombre || '—'} · ${c.propietarioEmail || ''}</div>
                    <div style="margin-top:6px;display:flex;gap:6px;flex-wrap:wrap">
                        ${deporteBadge(c.deporte)}
                        ${estadoCancha(c.estado)}
                    </div>
                </div>
            </div>
            ${fotosHtml}
            <div class="detalle-grid">
                <div class="detalle-field"><div class="detalle-field-label">Precio/hora</div>
                    <div class="detalle-field-value money">S/. ${c.precioPorHora ?? '—'}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Capacidad</div>
                    <div class="detalle-field-value">${c.capacidad ?? '—'} personas</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Departamento</div>
                    <div class="detalle-field-value">${c.departamento || '—'}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Provincia</div>
                    <div class="detalle-field-value">${c.provincia || '—'}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Distrito</div>
                    <div class="detalle-field-value">${c.distrito || '—'}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Dirección</div>
                    <div class="detalle-field-value">${c.direccion || '—'}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Total reservas</div>
                    <div class="detalle-field-value">${c.totalReservas ?? 0}</div></div>
                <div class="detalle-field"><div class="detalle-field-label">Fecha registro</div>
                    <div class="detalle-field-value">${formatDate(c.createdAt)}</div></div>
            </div>
            ${c.descripcion ? `<div class="detalle-field" style="margin-top:4px">
                <div class="detalle-field-label">Descripción</div>
                <div class="detalle-field-value">${c.descripcion}</div>
            </div>` : ''}
            ${c.estado === 'PENDIENTE' ? `
            <div style="background:#fff8e1;border:1px solid #fde047;border-radius:10px;padding:14px 16px;margin-top:12px;font-size:13px;color:#713f12">
                ⏳ <strong>Pendiente de aprobación.</strong> El propietario espera tu decisión.
                Puedes aprobarla con el botón ✓ o eliminarla si no cumple los requisitos.
            </div>` : ''}
            ${c.estado === 'INACTIVA' ? `
            <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:10px;padding:14px 16px;margin-top:12px;font-size:13px;color:#991b1b">
                🚫 <strong>Cancha desactivada.</strong> El propietario no puede editarla ni publicarla.
                Los clientes no pueden hacer reservas en esta cancha.
            </div>` : ''}`;
    } catch {
        document.getElementById('detalleBody').innerHTML =
            '<div class="empty-state"><p>Error al cargar detalle</p></div>';
    }
}

/* ── LIGHTBOX ───────────────────────────────────────────────── */
function abrirLightbox(url, idx, fotosJson) {
    _lbFotos  = typeof fotosJson === 'string' ? JSON.parse(fotosJson) : fotosJson;
    _lbActual = idx;
    _renderLightbox();
    document.getElementById('lightbox').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

function cerrarLightbox() {
    document.getElementById('lightbox').style.display = 'none';
    document.body.style.overflow = '';
}

function lbAnterior()  { _lbActual = (_lbActual - 1 + _lbFotos.length) % _lbFotos.length; _renderLightbox(); }
function lbSiguiente() { _lbActual = (_lbActual + 1) % _lbFotos.length; _renderLightbox(); }

function _renderLightbox() {
    document.getElementById('lbImg').src = _lbFotos[_lbActual];
    document.getElementById('lbCounter').textContent = `${_lbActual + 1} / ${_lbFotos.length}`;
    document.getElementById('lbPrev').style.display = _lbFotos.length > 1 ? 'flex' : 'none';
    document.getElementById('lbNext').style.display = _lbFotos.length > 1 ? 'flex' : 'none';
}

document.addEventListener('keydown', e => {
    if (e.key === 'Escape')      cerrarLightbox();
    if (e.key === 'ArrowLeft')   lbAnterior();
    if (e.key === 'ArrowRight')  lbSiguiente();
});

/* ── CAMBIAR ESTADO ─────────────────────────────────────────── */
function cambiarEstado(id, nuevoEstado, nombre) {
    const msgs = {
        ACTIVA:   `¿Aprobar y activar la cancha "${nombre}"?\nEl propietario podrá publicarla para los clientes.`,
        INACTIVA: `¿Desactivar la cancha "${nombre}"?\nEl propietario NO podrá editarla ni los clientes hacer reservas.`,
    };
    confirmar(msgs[nuevoEstado] || `¿Cambiar estado de "${nombre}"?`, async () => {
        try {
            await api.patch(`/admin/canchas/${id}/estado`, { estado: nuevoEstado });
            const toasts = {
                ACTIVA:   'Cancha aprobada. El propietario puede publicarla.',
                INACTIVA: '🚫 Cancha desactivada.',
            };
            showToast(toasts[nuevoEstado] || 'Estado actualizado', 'success');
            closeModal('modalDetalle');
            await cargarCanchas();
        } catch (e) { showToast('Error: ' + e.message, 'error'); }
    });
}

/* ── ELIMINAR ───────────────────────────────────────────────── */
function eliminarCancha(id, nombre) {
    confirmar(
        `¿Eliminar la cancha "${nombre}"?\nSi el propietario quiere registrar otra, deberá crearla de nuevo.`,
        async () => {
            try {
                await api.delete(`/admin/canchas/${id}`);
                showToast('Cancha eliminada correctamente', 'success');
                await cargarCanchas();
            } catch (e) { showToast('Error: ' + e.message, 'error'); }
        }
    );
}

/* ── Helpers ────────────────────────────────────────────────── */
function setText(id, val) { const el = document.getElementById(id); if (el) el.textContent = val; }
function esc(s) { return (s || '').replace(/'/g, "\\'"); }

document.addEventListener('DOMContentLoaded', () => { cargarCanchas(); });