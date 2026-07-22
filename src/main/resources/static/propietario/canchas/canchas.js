'use strict';

let todasCanchas      = [];
let canchaAEliminar   = null;
let fotosNuevasBase64 = [];

const DEPORTES_LABELS  = { FUTBOL:'Fútbol', BASQUETBOL:'Básquet', VOLEIBOL:'Vóley', TENIS:'Tenis', PADEL:'Pádel' };
const SUPERFICIE_LABELS = { GRASS_SINTETICO:'Grass sint.', LOSA:'Losa', PARQUET:'Parquet', ARENA:'Arena', CEMENTO:'Cemento' };

document.addEventListener('DOMContentLoaded', async () => {
    await initPage('Mis Canchas');
    await cargarCanchas();
    document.getElementById('btnConfirmEliminar').addEventListener('click', confirmarEliminar);
});

/* CARGA  */
async function cargarCanchas() {
    const grid = document.getElementById('canchasGrid');
    grid.innerHTML = '<div style="grid-column:1/-1"><div class="loading-state"><div class="spinner"></div> Cargando...</div></div>';
    try {
        todasCanchas = await api.get('/propietario/canchas');
        renderCanchas(todasCanchas);
    } catch {
        grid.innerHTML = '<div style="grid-column:1/-1"><div class="empty-state"><h4>Error al cargar canchas</h4></div></div>';
    }
}

/*  RENDER CARDS  */
function renderCanchas(lista) {
    const grid = document.getElementById('canchasGrid');
    if (!lista.length) {
        grid.innerHTML = `<div style="grid-column:1/-1"><div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <rect x="2" y="7" width="20" height="14" rx="2"/><path d="M12 7v14M2 14h20"/>
            </svg>
            <h4>Sin canchas registradas</h4>
            <p>Agrega tu primera cancha para comenzar.</p>
            <button class="btn btn-primary" onclick="abrirModalAgregar()">+ Agregar cancha</button>
        </div></div>`;
        return;
    }

    grid.innerHTML = lista.map(c => {
        const estado     = c.estado ?? 'PENDIENTE';
        const publicada  = c.publicada  === true;
        const editable   = c.editable   !== false;
        const puedeCambiarVisibilidad = estado !== 'INACTIVA';
        const horariosDisponibles = c.estado !== 'INACTIVA' && c.pendienteAprobacion !== true;
        const publicable = c.publicable === true;

        /*  badge de estado */
        const badgeEstado = {
            PENDIENTE:  '<span class="badge badge-warning">⏳ Pendiente aprobación</span>',
            ACTIVA:     publicada
                ? '<span class="badge badge-success"> Publicada</span>'
                : '<span class="badge badge-info">👁 Aprobada (no publicada)</span>',
            DESTACADA:  '<span class="badge badge-success"> Destacada</span>',
            INACTIVA:   '<span class="badge badge-danger">🚫 Desactivada por admin</span>',
        }[estado] ?? `<span class="badge">${estado}</span>`;

        /*  aviso cuando admin desactivó  */
        const avisoInactiva = estado === 'INACTIVA' ? `
            <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:10px 12px;font-size:12px;color:#991b1b;margin-top:6px">
                🚫 El administrador desactivó esta cancha. No puedes editarla ni publicarla.
                Si deseas continuar, elimínala y crea una nueva.
            </div>` : '';

        /* ── aviso pendiente ── */
        const avisoPendiente = estado === 'PENDIENTE' ? `
            <div style="background:#fff8e1;border:1px solid #fde047;border-radius:8px;padding:10px 12px;font-size:12px;color:#713f12;margin-top:6px">
                ⏳ En revisión. Una vez que el admin la apruebe podrás publicarla.
                Si no es aprobada, elimínala y crea una nueva.
            </div>` : '';

        /* aviso: ya usó su edición de este mes */
        const avisoEdicionMensual = (estado !== 'INACTIVA' && estado !== 'PENDIENTE' && c.edicionDisponibleDesde) ? `
            <div style="background:#eff6ff;border:1px solid #bfdbfe;border-radius:8px;padding:10px 12px;font-size:12px;color:#1e40af;margin-top:6px">
                🔒 Ya usaste tu edición de este mes en esta cancha. Podrás editarla de nuevo el
                <strong>${c.edicionDisponibleDesde}</strong>.
            </div>` : '';

        return `
        <div class="cancha-card">
            <div class="cancha-img">
                ${c.fotos?.length
            ? `<img src="${c.fotos[0]}" alt="${c.nombre}"/>`
            : `<div class="cancha-img-placeholder">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                            <rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/>
                        </svg>
                        <span style="font-size:11px;opacity:.7">Sin fotos</span>
                       </div>`}
                <div class="cancha-img-badge">${badgeEstado}</div>
            </div>

            <div class="cancha-body">
                <div class="cancha-title">${c.nombre}</div>
                <div class="cancha-location">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                        <circle cx="12" cy="10" r="3"/>
                    </svg>
                    ${[c.distrito, c.provincia, c.departamento].filter(Boolean).join(', ') || '—'}
                </div>
                <div class="cancha-meta">
                    ${SUPERFICIE_LABELS[c.tipoSuperficie] ?? c.tipoSuperficie ?? '—'}
                    · ${DEPORTES_LABELS[c.deporte] ?? c.deporte ?? '—'}
                </div>

                ${avisoInactiva}
                ${avisoPendiente}
                ${avisoEdicionMensual}

                <div class="cancha-stats" style="margin-top:10px">
                    <div class="cancha-stat">
                        <div class="cancha-stat-val">${c.totalReservasMes ?? 0}</div>
                        <div class="cancha-stat-lbl">Res./mes</div>
                    </div>
                    <div class="cancha-stat">
                        <div class="cancha-stat-val" style="color:var(--success)">${fmtMoney(c.precioPorHora)}</div>
                        <div class="cancha-stat-lbl">Por hora</div>
                    </div>
                    <div class="cancha-stat">
                        <div class="cancha-stat-val">${c.ocupacion ?? 0}%</div>
                        <div class="cancha-stat-lbl">Ocupación</div>
                    </div>
                </div>

                <div class="cancha-actions" style="flex-wrap:wrap;gap:6px">
                    <!-- Editar: solo si NO está INACTIVA -->
                    ${editable
            ? `<button class="btn btn-secondary btn-sm" onclick="abrirModalEditar(${c.id})">
                               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                   <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                                   <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                               </svg>
                               Editar
                           </button>`
            : c.pendienteAprobacion
                ? `<button class="btn btn-secondary btn-sm" disabled
                                title="Esta cancha aún no fue aprobada por el administrador. Podrás editarla y configurar horarios una vez aprobada."
                                style="opacity:.45;cursor:not-allowed">
                               ⏳ Editar (pendiente de aprobación)
                           </button>`
                : c.edicionDisponibleDesde
                    ? `<button class="btn btn-secondary btn-sm" disabled
                                    style="opacity:.45;cursor:not-allowed">
                                   🔒 Editar
                               </button>`
                    : `<button class="btn btn-secondary btn-sm" disabled title="Desactivada por admin"
                                    style="opacity:.45;cursor:not-allowed">
                                   Editar (bloqueado)
                               </button>`}

                    <!-- Publicar: solo si aprobada y NO publicada -->
                    ${publicable
            ? `<button class="btn btn-primary btn-sm" onclick="publicar(${c.id})">
                               🌐 Publicar
                           </button>`
            : ''}

                    <!-- Despublicar: solo si publicada y no inactiva (independiente de la cuota mensual de edición) -->
                    ${publicada && puedeCambiarVisibilidad
            ? `<button class="btn btn-secondary btn-sm" onclick="despublicar(${c.id})">
                               👁 Ocultar
                           </button>`
            : ''}

                    <!-- Horarios: solo si no inactiva y ya fue aprobada -->
                    ${horariosDisponibles
            ? `<a class="btn-icon view" href="/propietario/horarios?canchaId=${c.id}" title="Horarios">
                               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                   <circle cx="12" cy="12" r="10"/>
                                   <polyline points="12 6 12 12 16 14"/>
                               </svg>
                           </a>`
            : `<span class="btn-icon view" style="opacity:.35;cursor:not-allowed"
                            title="Disponible cuando el administrador apruebe la cancha">
                               <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                   <circle cx="12" cy="12" r="10"/>
                                   <polyline points="12 6 12 12 16 14"/>
                               </svg>
                           </span>`}

                    <!-- Eliminar: siempre disponible (si no tiene reservas activas) -->
                    <button class="btn-icon del" onclick="pedirEliminar(${c.id},'${(c.nombre||'').replace(/'/g,"\\'")}')">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                            <path d="M10 11v6M14 11v6"/>
                        </svg>
                    </button>
                </div>
            </div>
        </div>`;
    }).join('');
}

function filtrarCanchas(q) {
    const t = q.toLowerCase();
    renderCanchas(t ? todasCanchas.filter(c =>
        c.nombre?.toLowerCase().includes(t) ||
        c.deporte?.toLowerCase().includes(t) ||
        c.distrito?.toLowerCase().includes(t)
    ) : todasCanchas);
}

/* PUBLICAR / DESPUBLICAR  */
async function publicar(id) {
    try {
        const res = await api.patch(`/propietario/canchas/${id}/publicar`, {});
        toast(res?.mensaje ?? 'Cancha publicada. Los clientes ya pueden encontrarla.', 'success');
        await cargarCanchas();
    } catch (e) { toast(e.message || 'Error al publicar', 'error'); }
}

async function despublicar(id) {
    try {
        const res = await api.patch(`/propietario/canchas/${id}/despublicar`, {});
        toast(res?.mensaje ?? 'Cancha ocultada de la búsqueda.', 'info');
        await cargarCanchas();
    } catch (e) { toast(e.message || 'Error al ocultar', 'error'); }
}

/* MODAL AGREGAR */
function abrirModalAgregar() {
    limpiarFormCancha();
    document.getElementById('modalCanchaTitle').textContent = 'Agregar Cancha';
    document.getElementById('canchaId').value = '';
    document.getElementById('fotosActualesWrap').style.display = 'none';
    const avisoModal = document.getElementById('avisoEdicionModal');
    if (avisoModal) avisoModal.style.display = 'none';
    if (document.getElementById('cDepartamento').options.length <= 1) cargarDepartamentos();
    openModal('modalCancha');
}

/*  MODAL EDITAR  */
async function abrirModalEditar(id) {
    limpiarFormCancha();
    document.getElementById('modalCanchaTitle').textContent = 'Editar Cancha';
    try {
        const c = await api.get(`/propietario/canchas/${id}`);

        // Verificar si está bloqueada (con el motivo correcto)
        if (!c.editable) {
            if (c.estado === 'INACTIVA') {
                toast('Esta cancha fue desactivada por el administrador. No puedes editarla.', 'error');
            } else if (c.pendienteAprobacion) {
                toast('Esta cancha aún no fue aprobada por el administrador. Podrás editarla una vez aprobada.', 'error');
            } else if (c.edicionDisponibleDesde) {
                toast(`Ya usaste tu edición de este mes. Podrás editarla de nuevo el ${c.edicionDisponibleDesde}.`, 'error');
            } else {
                toast('Esta cancha no se puede editar en este momento.', 'error');
            }
            return;
        }

        // Aviso corto: solo 1 edición por mes por cancha
        const avisoModal = document.getElementById('avisoEdicionModal');
        if (avisoModal) avisoModal.style.display = 'block';

        document.getElementById('canchaId').value     = c.id;
        document.getElementById('cNombre').value      = c.nombre ?? '';
        document.getElementById('cDeporte').value     = c.deporte ?? '';
        document.getElementById('cSuperficie').value  = c.tipoSuperficie ?? '';
        document.getElementById('cPrecio').value      = c.precioPorHora ?? '';
        document.getElementById('cCapacidad').value   = c.capacidad ?? '';
        document.getElementById('cDescripcion').value = c.descripcion ?? '';
        document.getElementById('cDireccion').value   = c.direccion ?? '';

        if (c.fotos?.length) {
            const wrap = document.getElementById('fotosActualesWrap');
            wrap.style.display = 'flex';
            wrap.style.flexWrap = 'wrap';
            wrap.style.gap = '10px';
            wrap.innerHTML = c.fotos.map((f, i) => `
                <div class="upload-preview" style="width:90px;height:70px">
                    <img src="${f}" alt="Foto ${i+1}"/>
                    <button class="upload-remove" onclick="eliminarFotoActual(${c.id},${i},this)">×</button>
                </div>`).join('');
        }

        document.getElementById('cDepartamento').innerHTML = '<option value="">Seleccionar...</option>';
        await cargarDepartamentosYSeleccionar(c.departamento, c.provincia, c.distrito);
        openModal('modalCancha');
    } catch { toast('Error al cargar datos de la cancha', 'error'); }
}

function limpiarFormCancha() {
    fotosNuevasBase64 = [];
    window.fotosUrls  = [];  // limpiar URLs de Cloudinary al abrir modal
    ['cNombre','cPrecio','cCapacidad','cDescripcion','cDireccion'].forEach(id => {
        document.getElementById(id).value = '';
    });
    ['cDeporte','cSuperficie'].forEach(id => {
        document.getElementById(id).selectedIndex = 0;
    });
    document.getElementById('previewsNuevos').innerHTML = '';
    document.getElementById('fotosActualesWrap').style.display = 'none';
    document.getElementById('fotosActualesWrap').innerHTML = '';
    document.getElementById('fileFotos').value = '';
    document.getElementById('cDepartamento').innerHTML = '<option value="">Seleccionar...</option>';
    document.getElementById('cProvincia').innerHTML = '<option value="">Primero selecciona departamento</option>';
    document.getElementById('cProvincia').disabled = true;
    document.getElementById('cDistrito').innerHTML = '<option value="">Primero selecciona provincia</option>';
    document.getElementById('cDistrito').disabled = true;
}

/*  GUARDAR  */
async function guardarCancha() {
    const id = document.getElementById('canchaId').value;
    const fotosActuales = Array.from(
        document.querySelectorAll('#fotosActualesWrap .upload-preview img')
    ).map(img => img.src);
    const fotosNuevas = window.fotosUrls || [];
    const todasFotos = [...fotosActuales, ...fotosNuevas].slice(0, 3);

    const body = {
        nombre:         document.getElementById('cNombre').value.trim(),
        deporte:        document.getElementById('cDeporte').value,
        tipoSuperficie: document.getElementById('cSuperficie').value,
        precioPorHora:  parseFloat(document.getElementById('cPrecio').value) || 0,
        capacidad:      parseInt(document.getElementById('cCapacidad').value) || null,
        descripcion:    document.getElementById('cDescripcion').value.trim(),
        departamento:   document.getElementById('cDepartamento').value,
        provincia:      document.getElementById('cProvincia').value,
        distrito:       document.getElementById('cDistrito').value,
        direccion:      document.getElementById('cDireccion').value.trim(),
        fotos:          todasFotos,
    };

    if (!body.nombre || !body.deporte || !body.precioPorHora || !body.departamento || !body.distrito || !body.direccion) {
        toast('Completa los campos obligatorios', 'error'); return;
    }

    try {
        const btn = document.getElementById('btnGuardarCancha');
        btn.disabled = true; btn.textContent = 'Guardando...';
        if (id) {
            await api.put(`/propietario/canchas/${id}`, body);
            toast('Cancha actualizada correctamente', 'success');
        } else {
            await api.post('/propietario/canchas', body);
            toast(' Cancha registrada. Espera que el administrador la revise y apruebe. Una vez aprobada podrás publicarla.', 'success');
        }
        closeModal('modalCancha');
        await cargarCanchas();
    } catch (e) { toast(e.message || 'Error al guardar', 'error'); }
    finally {
        const btn = document.getElementById('btnGuardarCancha');
        btn.disabled = false; btn.textContent = 'Guardar cancha';
    }
}

/*  ELIMINAR  */
function pedirEliminar(id, nombre) {
    canchaAEliminar = id;
    document.getElementById('confirmNombreCancha').textContent = nombre;
    openModal('modalConfirm');
}

async function confirmarEliminar() {
    if (!canchaAEliminar) return;
    try {
        const res = await api.del(`/propietario/canchas/${canchaAEliminar}`);
        toast(res?.mensaje ?? 'Cancha eliminada', 'success');
        closeModal('modalConfirm');
        await cargarCanchas();
    } catch (e) { toast(e.message || 'Error al eliminar', 'error'); }
    finally { canchaAEliminar = null; }
}

async function handleFotosCancha(event) {
    const files = Array.from(event.target.files);
    if (!files.length) return;

    // Solo imágenes
    const soloImagenes = files.filter(f => f.type.startsWith('image/'));
    if (soloImagenes.length < files.length)
        toast('Solo se permiten imágenes (JPG, PNG, WebP)', 'error');
    if (!soloImagenes.length) return;

    // Máximo 3 fotos en total
    if (!window.fotosUrls) window.fotosUrls = [];
    const espacioDisponible = 3 - window.fotosUrls.length;
    if (espacioDisponible <= 0) {
        toast('Ya tienes 3 fotos. Quita alguna antes de agregar más.', 'error');
        document.getElementById('fileFotos').value = '';
        return;
    }
    const filesToUpload = soloImagenes.slice(0, espacioDisponible);
    if (filesToUpload.length < soloImagenes.length)
        toast(`Solo se subirán ${filesToUpload.length} foto(s) para no superar el límite de 3`, 'error');

    // Mostrar estado "Subiendo..."
    const wrap = document.getElementById('previewsNuevos');

    for (const file of filesToUpload) {
        if (file.size > 5 * 1024 * 1024) {
            toast(`"${file.name}" supera 5 MB, omitida`, 'error');
            continue;
        }

        // Placeholder mientras sube
        const placeholderId = 'ph_' + Date.now() + Math.random().toString(36).slice(2);
        const ph = document.createElement('div');
        ph.id = placeholderId;
        ph.style.cssText = 'display:inline-flex;align-items:center;justify-content:center;width:90px;height:70px;border:1px dashed var(--border);border-radius:6px;font-size:11px;color:var(--text3);margin:4px';
        ph.textContent = '⏳';
        wrap.appendChild(ph);

        try {
            const url = await uploadToCloudinary(file, 'sportspace/canchas');
            window.fotosUrls.push(url);

            // Reemplazar placeholder con miniatura real + botón quitar
            const idx = window.fotosUrls.length - 1;
            const div = document.createElement('div');
            div.className = 'upload-preview';
            div.style.cssText = 'position:relative;display:inline-block;width:90px;height:70px;margin:4px';
            div.innerHTML = `
                <img src="${url}" style="width:90px;height:70px;object-fit:cover;border-radius:6px;border:1px solid var(--border)"/>
                <button onclick="quitarFotoCloudinary(${idx},this)"
                        style="position:absolute;top:-6px;right:-6px;width:18px;height:18px;border-radius:50%;
                               background:var(--danger);color:#fff;border:none;font-size:11px;
                               cursor:pointer;line-height:1;display:flex;align-items:center;justify-content:center">✕</button>`;
            document.getElementById(placeholderId)?.replaceWith(div);
        } catch (e) {
            document.getElementById(placeholderId)?.remove();
            toast(`Error al subir "${file.name}": ${e.message}`, 'error');
        }
    }

    // Limpiar input para poder volver a seleccionar el mismo archivo
    document.getElementById('fileFotos').value = '';
}

function quitarFotoCloudinary(idx, btn) {
    if (window.fotosUrls) window.fotosUrls.splice(idx, 1);
    btn.parentElement.remove();
    // Re-indexar botones restantes
    document.querySelectorAll('#previewsNuevos .upload-preview button').forEach((b, i) => {
        b.setAttribute('onclick', `quitarFotoCloudinary(${i},this)`);
    });
}

function quitarFotoNueva(idx, btn) {
    fotosNuevasBase64.splice(idx, 1);
    btn.parentElement.remove();
    document.querySelectorAll('#previewsNuevos .upload-remove').forEach((b, i) => {
        b.setAttribute('onclick', `quitarFotoNueva(${i},this)`);
    });
}

async function eliminarFotoActual(canchaId, index, btn) {
    try {
        await api.del(`/propietario/canchas/${canchaId}/fotos/${index}`);
        btn.parentElement.remove();
        toast('Foto eliminada', 'success');
    } catch { toast('Error al eliminar foto', 'error'); }
}

/* UBIGEO */
async function cargarDepartamentos() {
    try {
        const list = await api.get('/ubigeo/departamentos');
        const sel  = document.getElementById('cDepartamento');
        while (sel.options.length > 1) sel.remove(1);
        list.forEach(d => {
            const opt = document.createElement('option');
            opt.value        = d.name;
            opt.dataset.id   = d.id;
            opt.textContent  = d.name;
            sel.appendChild(opt);
        });
    } catch { /* ignora */ }
}

async function cargarProvincias(depName) {
    const sel = document.getElementById('cProvincia');
    sel.disabled = true;
    sel.innerHTML = '<option value="">Seleccionar provincia...</option>';
    document.getElementById('cDistrito').innerHTML = '<option value="">Primero selecciona provincia</option>';
    document.getElementById('cDistrito').disabled  = true;
    if (!depName) { sel.disabled = false; return; }

    // FIX: busqueda case-insensitive para que "LIMA" == "Lima" etc.
    const depSel = document.getElementById('cDepartamento');
    const depOpt = Array.from(depSel.options).find(
        o => o.value.trim().toLowerCase() === depName.trim().toLowerCase()
    );
    const depId = depOpt?.dataset?.id;
    if (!depId) { sel.disabled = false; return; }

    try {
        const list = await api.get(`/ubigeo/provincias/${depId}`);
        list.forEach(p => {
            const opt = document.createElement('option');
            opt.value       = p.name;
            opt.dataset.id  = p.id;
            opt.textContent = p.name;
            sel.appendChild(opt);
        });
        sel.disabled = false;
    } catch { sel.disabled = false; }
}

async function cargarDistritos(provName) {
    const sel = document.getElementById('cDistrito');
    sel.disabled = true;
    sel.innerHTML = '<option value="">Seleccionar distrito...</option>';
    if (!provName) { sel.disabled = false; return; }

    // FIX: busqueda case-insensitive
    const provSel = document.getElementById('cProvincia');
    const provOpt = Array.from(provSel.options).find(
        o => o.value.trim().toLowerCase() === provName.trim().toLowerCase()
    );
    const provId = provOpt?.dataset?.id;
    if (!provId) { sel.disabled = false; return; }

    try {
        const list = await api.get(`/ubigeo/distritos/${provId}`);
        list.forEach(d => {
            const opt = document.createElement('option');
            opt.value       = d.name;
            opt.dataset.id  = d.id;
            opt.textContent = d.name;
            sel.appendChild(opt);
        });
        sel.disabled = false;
    } catch { sel.disabled = false; }
}

function _selectCI(selectId, value) {
    if (!value) return;
    const sel = document.getElementById(selectId);
    const v = value.trim().toLowerCase();
    const found = Array.from(sel.options).find(o => o.value.trim().toLowerCase() === v);
    if (found) sel.value = found.value;
}

// Carga ubigeo en cascada con pre-seleccion (modo EDITAR)
async function cargarDepartamentosYSeleccionar(dep, prov, dist) {
    await cargarDepartamentos();

    if (dep) {
        _selectCI('cDepartamento', dep);
        const depSelVal = document.getElementById('cDepartamento').value;
        await cargarProvincias(depSelVal || dep);

        if (prov) {
            _selectCI('cProvincia', prov);
            const provSelVal = document.getElementById('cProvincia').value;
            await cargarDistritos(provSelVal || prov);
            if (dist) _selectCI('cDistrito', dist);
        }
    }
}