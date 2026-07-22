'use strict';
/*  BUSCAR CANCHAS  */

let todasLasCanchas = [];   // caché completo
let modalReady      = false;

document.addEventListener('DOMContentLoaded', async () => {
    if (!await initPage()) return;
    await Promise.all([cargarUbigeo(), cargarCanchas()]);
});

/* UBIGEO EN CASCADA  */
async function cargarUbigeo() {
    try {
        const deptos = await api.get('/ubigeo/departamentos');
        const sel = document.getElementById('selDepartamento');
        deptos.forEach(d => {
            const o = document.createElement('option');
            o.value = d.id; o.textContent = d.name || d.nombre || d.id;
            sel.appendChild(o);
        });
    } catch(e) { console.error('Ubigeo:', e); }
}

async function onFiltroCascada(nivel) {
    if (nivel === 'dep') {
        // Resetear provincia y distrito
        resetSelect('selProvincia', 'Provincia');
        resetSelect('selDistrito', 'Distrito');
        const depId = document.getElementById('selDepartamento').value;
        if (!depId) { filtrar(); return; }
        try {
            const provs = await api.get(`/ubigeo/provincias/${depId}`);
            const sel = document.getElementById('selProvincia');
            provs.forEach(p => {
                const o = document.createElement('option');
                o.value = p.id; o.textContent = p.name || p.nombre || p.id;
                sel.appendChild(o);
            });
        } catch(e) { console.error(e); }
    } else if (nivel === 'prov') {
        resetSelect('selDistrito', 'Distrito');
        const provId = document.getElementById('selProvincia').value;
        if (!provId) { filtrar(); return; }
        try {
            const dists = await api.get(`/ubigeo/distritos/${provId}`);
            const sel = document.getElementById('selDistrito');
            dists.forEach(d => {
                const o = document.createElement('option');
                o.value = d.name || d.nombre || d.id;
                o.textContent = d.name || d.nombre || d.id;
                sel.appendChild(o);
            });
        } catch(e) { console.error(e); }
    }
    filtrar();
}

function resetSelect(id, placeholder) {
    const s = document.getElementById(id);
    s.innerHTML = `<option value="">${placeholder}</option>`;
}

/*  CARGA INICIAL */
async function cargarCanchas() {
    const grid = document.getElementById('canchasGrid');
    grid.innerHTML = `<div class="loading-state" style="grid-column:1/-1"><div class="spinner"></div> Cargando canchas…</div>`;
    try {
        const data = await api.get('/canchas/publico');
        todasLasCanchas = Array.isArray(data) ? data : (data.content || []);
        renderGrid(todasLasCanchas);
    } catch(e) {
        grid.innerHTML = `<div class="empty-state" style="grid-column:1/-1">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <h4>Error al cargar canchas</h4><p>Revisa tu conexión e intenta refrescar.</p>
        </div>`;
        console.error(e);
    }
}

/* FILTRAR */
function filtrar() {
    const txt     = (document.getElementById('searchInput').value || '').toLowerCase().trim();
    const deporte = document.getElementById('selDeporte').value;
    const distrito= document.getElementById('selDistrito').value;

    const lista = todasLasCanchas.filter(c => {
        const matchNombre  = !txt      || (c.nombre || '').toLowerCase().includes(txt);
        const matchDeporte = !deporte  || (c.deporte || '') === deporte;
        const matchDist    = !distrito || (c.distrito || '') === distrito;
        return matchNombre && matchDeporte && matchDist;
    });
    renderGrid(lista);
}

function quickFilter(btn, deporte) {
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('selDeporte').value = deporte;
    filtrar();
}

function resetFiltros() {
    document.getElementById('searchInput').value = '';
    ['selDepartamento','selProvincia','selDistrito','selDeporte']
        .forEach(id => document.getElementById(id).value = '');
    resetSelect('selProvincia', 'Provincia');
    resetSelect('selDistrito', 'Distrito');
    document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
    document.querySelector('.chip').classList.add('active');
    renderGrid(todasLasCanchas);
}

/* RENDER GRID */
const PIN = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:11px;height:11px"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>`;
const PLACEHOLDER_SVG = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="width:34px;height:34px;color:rgba(0,0,0,.25)"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="M21 15l-5-5L5 21"/></svg>`;

function renderGrid(lista) {
    const grid   = document.getElementById('canchasGrid');
    const noRes  = document.getElementById('noResultados');
    const label  = document.getElementById('countLabel');
    label.textContent = `${lista.length} cancha${lista.length !== 1 ? 's' : ''}`;

    if (!lista.length) {
        grid.innerHTML = '';
        noRes.style.display = 'flex';
        return;
    }
    noRes.style.display = 'none';
    grid.innerHTML = lista.map(c => renderCanchaCard(c)).join('');
}

function renderCanchaCard(c) {
    const { theme, label: deporteLabel } = iconoDeporte(c.deporte || '');
    const fotoUrl = c.fotos && c.fotos.length ? c.fotos[0] : null;
    const thumbContent = fotoUrl
        ? `<img src="${fotoUrl}" alt="${c.nombre}" style="width:100%;height:100%;object-fit:cover;position:absolute;inset:0"/>`
        : `<div style="position:relative;z-index:1;display:flex;align-items:center;justify-content:center;width:100%;height:100%">${PLACEHOLDER_SVG}</div>`;

    return `
    <div class="cancha-card ${theme}" onclick="abrirModal(${c.id})" data-id="${c.id}">
        <div class="card-thumb" style="position:relative">
            <span class="card-sport-badge">${deporteLabel}</span>
            ${thumbContent}
        </div>
        <div class="card-name">${c.nombre}</div>
        <div class="card-location">${PIN} ${c.distrito || ''}, ${c.departamento || ''}</div>
        <div class="card-footer">
            <div class="card-price">S/. ${Number(c.precioPorHora || 0).toFixed(2)} <span>/ hora</span></div>
            <button class="btn btn-primary btn-sm"
                    onclick="event.stopPropagation(); abrirModal(${c.id})">Reservar</button>
        </div>
    </div>`;
}

/* MODAL (lazy-load) */
async function abrirModal(canchaId) {
    if (!modalReady) {
        const res  = await fetch('/cliente/shared/modal-reserva.html');
        const html = await res.text();
        document.getElementById('modal-reserva-container').innerHTML = html;
        await new Promise((ok, fail) => {
            const s = document.createElement('script');
            s.src = '/cliente/shared/modal-reserva.js';
            s.onload = ok; s.onerror = fail;
            document.body.appendChild(s);
        });
        modalReady = true;
    }
    // La cancha ya está en caché
    const cancha = todasLasCanchas.find(c => c.id === canchaId);
    abrirModalReserva(canchaId, cancha || null);
}