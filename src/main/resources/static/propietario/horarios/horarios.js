'use strict';

// Lun=0 … Dom=6
const DIAS_NOMBRES = ['Lunes','Martes','Miércoles','Jueves','Viernes','Sábado','Domingo'];
const DIAS_CORTOS  = ['Lun','Mar','Mié','Jue','Vie','Sáb','Dom'];

let canchaIdActual = null;
// estructura: { diaSemana: { activo, apertura, cierre } }
let horarioData = {};

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Horarios');
    await cargarCanchas();
    const params = new URLSearchParams(location.search);
    if (params.get('canchaId')) {
        document.getElementById('selectorCancha').value = params.get('canchaId');
        await cambiarCancha(params.get('canchaId'));
    }
});

/* CARGAR LISTA DE CANCHAS */
async function cargarCanchas() {
    try {
        const list = await api.get('/propietario/canchas');
        const sel  = document.getElementById('selectorCancha');
        if (!list.length) {
            sel.innerHTML = '<option value="">Sin canchas registradas</option>';
            document.getElementById('contenidoHorarios').innerHTML =
                '<div class="empty-state"><h4>No tienes canchas registradas</h4></div>';
            return;
        }
        sel.innerHTML = list.map(c => `<option value="${c.id}">${c.nombre}</option>`).join('');
        canchaIdActual = list[0].id;
        await cambiarCancha(canchaIdActual);
    } catch { toast('Error al cargar canchas', 'error'); }
}

async function cambiarCancha(id) {
    if (!id) return;
    canchaIdActual = id;
    inicializarHorario();
    await cargarHorarioDesdeAPI();
    renderHorario();
}


function inicializarHorario() {
    horarioData = {};
    for (let d = 0; d < 7; d++) {
        horarioData[d] = {
            activo:   false,   // <-- FIX: desactivado por defecto
            apertura: 8,
            cierre:   22,
        };
    }
}

/* CARGAR DESDE API  */
async function cargarHorarioDesdeAPI() {
    if (!canchaIdActual) return;
    try {
        const data = await api.get(`/propietario/horarios/${canchaIdActual}`);
        const slots = data.slots ?? [];
        if (!slots.length) return; // usar defaults

        // Reconstruir horarioData desde los slots guardados
        // Slots con estado DISPONIBLE definen el rango de apertura/cierre
        const porDia = {};
        slots.forEach(s => {
            if (!porDia[s.diaSemana]) porDia[s.diaSemana] = [];
            if (s.estado === 'DISPONIBLE') porDia[s.diaSemana].push(s.hora);
        });

        for (let d = 0; d < 7; d++) {
            const horas = porDia[d] ?? [];
            if (horas.length) {
                horarioData[d] = {
                    activo:   true,
                    apertura: Math.min(...horas),
                    cierre:   Math.max(...horas) + 1,
                };
            } else {
                horarioData[d] = { activo: false, apertura: 8, cierre: 22 };
            }
        }
    } catch { /* usar defaults */ }
}

/* RENDER PRINCIPAL */
function renderHorario() {
    const contenido = document.getElementById('contenidoHorarios');

    const horasOpts = (desde, hasta, sel) => {
        let html = '';
        for (let h = desde; h <= hasta; h++) {
            const label = `${String(h).padStart(2,'0')}:00`;
            html += `<option value="${h}" ${h === sel ? 'selected' : ''}>${label}</option>`;
        }
        return html;
    };

    const diasHTML = Array.from({ length: 7 }, (_, d) => {
        const info   = horarioData[d];
        const activo = info.activo;
        return `
        <div class="dia-row ${activo ? 'dia-activo' : ''}" id="diaRow${d}">
            <div class="dia-toggle-wrap">
                <label class="toggle-switch">
                    <input type="checkbox" ${activo ? 'checked' : ''}
                           onchange="toggleDia(${d}, this.checked)"/>
                    <span class="toggle-slider"></span>
                </label>
                <span class="dia-nombre">${DIAS_NOMBRES[d]}</span>
                <span class="dia-corto">${DIAS_CORTOS[d]}</span>
            </div>
            <div class="dia-horas ${activo ? '' : 'dia-horas-disabled'}" id="diaHoras${d}">
                <div class="hora-grupo">
                    <label>Apertura</label>
                    <select class="form-select hora-sel" id="ap${d}"
                            onchange="cambiarHora(${d},'apertura',this.value)"
                            ${activo ? '' : 'disabled'}>
                        ${horasOpts(0, 23, info.apertura)}
                    </select>
                </div>
                <div class="hora-separador">→</div>
                <div class="hora-grupo">
                    <label>Cierre</label>
                    <select class="form-select hora-sel" id="ci${d}"
                            onchange="cambiarHora(${d},'cierre',this.value)"
                            ${activo ? '' : 'disabled'}>
                        ${horasOpts(1, 24, info.cierre)}
                    </select>
                </div>
                <div class="dia-resumen" id="resumen${d}">
                    ${activo ? renderResumen(d) : '—'}
                </div>
            </div>
            ${activo ? '' : '<div class="dia-cerrado-label">Cerrado</div>'}
        </div>`;
    }).join('');

    contenido.innerHTML = `
        <div class="horario-card card">
            <div class="card-header">
                <h3>Horario semanal</h3>
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <button class="btn btn-secondary btn-sm" onclick="aplicarATodos()">
                        ↕ Copiar L-V a todos
                    </button>
                    <button class="btn btn-primary" onclick="guardarHorario()">
                         Guardar horario
                    </button>
                </div>
            </div>
            <div class="card-body">
                <div class="dias-list">${diasHTML}</div>
                <p class="horario-tip">
                    💡 Cada franja representa una hora de atención. Ej: Apertura 08:00 → Cierre 22:00 crea horarios de 08:00 a 21:00 (la última hora disponible es 21:00-22:00).
                </p>
            </div>
        </div>`;
}

/*  TOGGLE DÍA  */
function toggleDia(d, activo) {
    horarioData[d].activo = activo;
    const row   = document.getElementById(`diaRow${d}`);
    const horas = document.getElementById(`diaHoras${d}`);
    row.classList.toggle('dia-activo', activo);
    horas.classList.toggle('dia-horas-disabled', !activo);
    horas.querySelectorAll('select').forEach(s => { s.disabled = !activo; });
    const cerrado = row.querySelector('.dia-cerrado-label');
    if (cerrado) cerrado.remove();
    if (!activo) {
        row.insertAdjacentHTML('beforeend', '<div class="dia-cerrado-label">Cerrado</div>');
    }
    actualizarResumen(d);
}

/*  CAMBIAR HORA  */
function cambiarHora(d, tipo, val) {
    const v = parseInt(val);
    horarioData[d][tipo] = v;
    // Validar: cierre debe ser > apertura
    if (tipo === 'apertura' && v >= horarioData[d].cierre) {
        horarioData[d].cierre = v + 1;
        const sel = document.getElementById(`ci${d}`);
        if (sel) sel.value = horarioData[d].cierre;
    }
    if (tipo === 'cierre' && v <= horarioData[d].apertura) {
        horarioData[d].apertura = v - 1;
        const sel = document.getElementById(`ap${d}`);
        if (sel) sel.value = horarioData[d].apertura;
    }
    actualizarResumen(d);
}

function actualizarResumen(d) {
    const el = document.getElementById(`resumen${d}`);
    if (el) el.innerHTML = horarioData[d].activo ? renderResumen(d) : '—';
}

function renderResumen(d) {
    const { apertura, cierre } = horarioData[d];
    const horas = cierre - apertura;
    return `<span class="resumen-horas">${String(apertura).padStart(2,'0')}:00 – ${String(cierre).padStart(2,'0')}:00</span>
            <span class="resumen-cant">${horas}h disponible${horas !== 1 ? 's' : ''}</span>`;
}

/* COPIAR L-V A TODOS */
function aplicarATodos() {
    // Toma el horario del Lunes como referencia
    const ref = horarioData[0];
    for (let d = 0; d < 7; d++) {
        horarioData[d] = { ...ref };
        // Actualizar los selects en el DOM
        const ap = document.getElementById(`ap${d}`);
        const ci = document.getElementById(`ci${d}`);
        const cb = document.querySelector(`#diaRow${d} input[type=checkbox]`);
        if (ap) ap.value = ref.apertura;
        if (ci) ci.value = ref.cierre;
        if (cb) {
            cb.checked = ref.activo;
            toggleDia(d, ref.activo);
        }
    }
    toast('Horario del Lunes copiado a todos los días', 'success');
}

/*  GUARDAR  */
async function guardarHorario() {
    if (!canchaIdActual) return;

    // Construir slots: un slot por hora dentro del rango de cada día activo
    const slots = [];
    for (let d = 0; d < 7; d++) {
        const { activo, apertura, cierre } = horarioData[d];
        if (!activo) continue;
        for (let h = apertura; h < cierre; h++) {
            slots.push({ diaSemana: d, hora: h, estado: 'DISPONIBLE' });
        }
    }

    if (!slots.length) {
        toast('Debes tener al menos un día activo', 'error');
        return;
    }

    try {
        const btn = document.querySelector('[onclick="guardarHorario()"]');
        if (btn) { btn.disabled = true; btn.textContent = 'Guardando...'; }
        await api.post(`/propietario/horarios/${canchaIdActual}`, { slots });
        toast(`Horario guardado — ${slots.length} franjas configuradas`, 'success');
    } catch (e) {
        toast(e.message || 'Error al guardar horario', 'error');
    } finally {
        const btn = document.querySelector('[onclick="guardarHorario()"]');
        if (btn) { btn.disabled = false; btn.textContent = 'Guardar horario'; }
    }
}