'use strict';

const API = '/api';

// Estado global
let tipoDocumento    = 'DNI';   // "DNI" | "CE"
let numeroConsultado = '';
let emailVerificado  = false;
let emailEnVerificacion = false;
let telefonoValido   = false;   // false si el número ya está registrado

// Alertas principales
const alertIcons = {
    error:  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    success:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
};
function setAlert(msg, type = 'error') {
    document.getElementById('reg-alert').innerHTML = msg
        ? `<div class="alert alert-${type}">${alertIcons[type] || ''}<span>${msg}</span></div>`
        : '';
}

// Hints de campo
const hintIcons = {
    error:  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
    success:'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    info:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
};
function setHint(id, msg, type = '') {
    const el = document.getElementById(id);
    if (!el) return;
    if (!msg) { el.className = 'field-hint'; el.innerHTML = ''; return; }
    el.className = `field-hint hint-${type} show`;
    el.innerHTML = `${hintIcons[type] || ''}<span>${msg}</span>`;
}
function limpiarHint(id) { setHint(id, ''); }

// HTTP helpers
async function apiGet(url) {
    const res  = await fetch(API + url, { headers: { 'Accept': 'application/json' } });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.mensaje || data.message || 'Error');
    return data;
}
async function apiPost(url, body) {
    const res  = await fetch(API + url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify(body),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.mensaje || data.message || 'Error');
    return data;
}


// TIPO DE DOCUMENTO

/**
 * Cambia entre DNI y C.E.
 * IMPORTANTE: hace un reset TOTAL del formulario, incluyendo email, verificación
 * de correo, códigos y todos los datos previamente consultados.
 */
function seleccionarTipoDoc(tipo) {
    if (tipoDocumento === tipo) return;
    tipoDocumento = tipo;

    const input  = document.getElementById('reg-dni');
    const label  = document.getElementById('label-numero-doc');
    const badge  = document.getElementById('badge-api');
    const btnDni = document.getElementById('btn-tipo-dni');
    const btnCe  = document.getElementById('btn-tipo-ce');

    // Limpiar TODO el formulario
    resetFormularioCompleto();

    // Ajustar campo de documento según tipo
    if (tipo === 'DNI') {
        label.textContent   = 'Número de DNI';
        badge.textContent   = 'Autocompletado RENIEC';
        input.placeholder   = 'Ingresa tus 8 dígitos';
        input.maxLength     = 8;
        input.inputMode     = 'numeric';
        input.oninput = function () {
            this.value = this.value.replace(/\D/g, '');
            onDocInput();
        };
        btnDni.classList.add('active');
        btnCe.classList.remove('active');
    } else {
        label.textContent   = 'Número de Carnet de Extranjería';
        badge.textContent   = 'Autocompletado';
        input.placeholder   = 'Ej: 001077238';
        input.maxLength     = 12;
        input.inputMode     = 'numeric';
        input.oninput = function () { this.value = this.value.replace(/\D/g, ''); onDocInput(); };
        btnDni.classList.remove('active');
        btnCe.classList.add('active');
    }

    document.getElementById('btn-reniec').disabled = true;
    input.focus();
}

/**
 * Reset TOTAL del formulario:
 * - Datos del documento y personales
 * - Correo electrónico + estado de verificación
 * - Códigos enviados y UI de verificación
 * - Campos bloqueados / selects de ubigeo
 * - Nacionalidad
 * - Botón de registro
 */
function resetFormularioCompleto() {
    // Estado interno
    numeroConsultado    = '';
    emailVerificado     = false;
    emailEnVerificacion = false;
    telefonoValido      = false;

    // Campo documento
    const docInput = document.getElementById('reg-dni');
    docInput.value     = '';
    docInput.readOnly  = false;
    docInput.disabled  = false;
    docInput.classList.remove('doc-locked', 'field-error', 'valid');
    const lapiz = document.getElementById('btn-edit-doc');
    if (lapiz) lapiz.style.display = 'none';
    document.getElementById('dni-info').classList.remove('show');

    // Datos personales
    ['reg-nombres', 'reg-apellidos', 'reg-fecha-nac',
        'reg-departamento', 'reg-provincia', 'reg-distrito', 'reg-direccion']
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });

    // Restaurar inputs de ubigeo visibles
    ['reg-fecha-nac', 'reg-departamento', 'reg-provincia', 'reg-distrito']
        .forEach(id => {
            const el = document.getElementById(id);
            if (el) el.style.display = '';
        });

    // Resetear combos de ubigeo
    ['sel-departamento', 'sel-provincia', 'sel-distrito'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.innerHTML = '<option value="">Selecciona...</option>'; el.style.display = 'none'; }
    });

    // Date picker
    const picker = document.getElementById('reg-fecha-nac-picker');
    if (picker) { picker.value = ''; picker.style.display = 'none'; }

    // Dirección bloqueada y vacía
    const dir = document.getElementById('reg-direccion');
    if (dir) { dir.readOnly = true; dir.classList.add('field-locked'); dir.placeholder = ''; }

    // Nacionalidad: vuelve a input bloqueado vacío (SIEMPRE VISIBLE)
    resetNacionalidad();

    // Correo electrónico
    const emailInput = document.getElementById('reg-email');
    emailInput.value     = '';
    emailInput.readOnly  = false;
    emailInput.disabled  = false;
    emailInput.classList.remove('doc-locked', 'valid', 'field-error');

    const lapizEmail = document.getElementById('btn-edit-email');
    if (lapizEmail) lapizEmail.style.display = 'none';

    limpiarHint('hint-email');

    // Ocultar el bloque de verificación email y resetear su UI interna
    const verifyRow  = document.getElementById('email-verify-row');
    const emailPaso1 = document.getElementById('email-paso1');
    const emailPaso2 = document.getElementById('email-paso2');
    const btnPedir   = document.getElementById('btn-pedir-codigo-email');
    const codigoInput= document.getElementById('codigo-email');
    const btnEnviar  = document.getElementById('btn-enviar-codigo-email');
    const btnReenviar= document.getElementById('btn-reenviar-email');

    if (verifyRow)  verifyRow.style.display   = 'none';
    if (emailPaso1) emailPaso1.style.display  = 'block';
    if (emailPaso2) emailPaso2.style.display  = 'none';

    if (btnPedir) {
        btnPedir.disabled  = false;
        btnPedir.innerHTML =
            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> Pedir código';
    }
    if (codigoInput) {
        codigoInput.value    = '';
        codigoInput.disabled = false;
    }
    if (btnEnviar) {
        btnEnviar.disabled   = false;
        btnEnviar.innerHTML  =
            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Enviar código';
        btnEnviar.classList.remove('btn-verified');
        btnEnviar.disabled   = true;
    }
    if (btnReenviar) { btnReenviar.style.display = 'none'; btnReenviar.disabled = false; }

    limpiarHint('hint-codigo-email');

    // Alerta geneneral
    setAlert('');

    // Contraseña
    const pwdInput = document.getElementById('reg-password');
    if (pwdInput) {
        pwdInput.value = '';
        pwdInput.type  = 'password';
    }
    document.getElementById('eye-off').style.display = 'block';
    document.getElementById('eye-on').style.display  = 'none';
    const pwdStrength = document.getElementById('pwd-strength');
    if (pwdStrength) pwdStrength.classList.remove('show');

    // Teléfono
    const telInput = document.getElementById('reg-telefono');
    if (telInput) {
        telInput.value = '';
        telInput.classList.remove('valid', 'field-error');
    }
    limpiarHint('hint-telefono');

    // Botón de registro — siempre deshabilitado al resetear
    const btnReg = document.getElementById('btn-registro');
    btnReg.disabled = true;
    btnReg.classList.add('btn-disabled');
    btnReg.title = 'Verifica tu correo primero';
}

// Resetear solo la sección de nacionalidad
function resetNacionalidad() {
    const inputNac = document.getElementById('reg-nacionalidad');
    const selNac   = document.getElementById('sel-nacionalidad');

    // Siempre mostramos el input (bloqueado), ocultamos el select
    inputNac.value       = '';
    inputNac.placeholder = '';
    inputNac.style.display = '';
    inputNac.readOnly    = true;
    inputNac.classList.add('field-locked');

    if (selNac) { selNac.value = ''; selNac.style.display = 'none'; }
}

// CONSULTA DE DOCUMENTO

function onDocInput() {
    const val    = document.getElementById('reg-dni').value.trim();
    const minLen = tipoDocumento === 'DNI' ? 8 : 7;
    document.getElementById('reg-dni').classList.remove('field-error', 'valid');
    if (val !== numeroConsultado) {
        document.getElementById('btn-reniec').disabled = val.length < minLen;
        if (val.length < minLen) { limpiarCamposPersonales(); setAlert(''); }
    }
}

function onDniInput() { onDocInput(); }

function limpiarCamposPersonales() {
    ['reg-nombres', 'reg-apellidos', 'reg-fecha-nac', 'reg-departamento',
        'reg-provincia', 'reg-distrito', 'reg-direccion'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });
    ['sel-departamento', 'sel-provincia', 'sel-distrito'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.innerHTML = '<option value="">Selecciona...</option>'; el.style.display = 'none'; }
    });
    ['reg-fecha-nac', 'reg-departamento', 'reg-provincia', 'reg-distrito'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = '';
    });
    const picker = document.getElementById('reg-fecha-nac-picker');
    if (picker) { picker.value = ''; picker.style.display = 'none'; }
    const dir = document.getElementById('reg-direccion');
    if (dir) { dir.readOnly = true; dir.classList.add('field-locked'); dir.placeholder = ''; }
    const lapizDoc = document.getElementById('btn-edit-doc');
    if (lapizDoc) lapizDoc.style.display = 'none';
    document.getElementById('dni-info').classList.remove('show');
    resetNacionalidad();
}

function bloquearCampoDoc() {
    const input = document.getElementById('reg-dni');
    const btn   = document.getElementById('btn-reniec');
    const lapiz = document.getElementById('btn-edit-doc');
    input.readOnly = true;
    input.classList.add('doc-locked');
    btn.disabled = true;
    if (lapiz) lapiz.style.display = 'flex';
}

function habilitarEdicionDoc() {
    const input = document.getElementById('reg-dni');
    const btn   = document.getElementById('btn-reniec');
    const lapiz = document.getElementById('btn-edit-doc');
    input.readOnly = false;
    input.disabled = false;
    input.classList.remove('doc-locked', 'valid', 'field-error');
    if (lapiz) lapiz.style.display = 'none';
    btn.disabled     = true;
    numeroConsultado = '';
    limpiarCamposPersonales();
    setAlert('');
    input.focus();
}

async function consultarDocumento() {
    const numero = document.getElementById('reg-dni').value.trim();
    const btn    = document.getElementById('btn-reniec');

    if (tipoDocumento === 'DNI' && !/^\d{8}$/.test(numero))
        return setAlert('El DNI debe tener exactamente 8 dígitos numéricos.');
    if (tipoDocumento === 'CE' && !/^\d{7,12}$/.test(numero))
        return setAlert('El Carnet de Extranjería debe tener entre 7 y 12 dígitos.');

    btn.innerHTML = '<span style="display:inline-block;width:12px;height:12px;border:2px solid rgba(255,255,255,0.4);border-top-color:#fff;border-radius:50%;animation:spin .6s linear infinite"></span>';
    btn.disabled  = true;
    limpiarCamposPersonales();

    try {
        // Verificar que el documento no esté ya registrado
        const check = await apiGet('/usuarios/publico/existe-documento/' + numero).catch(() => null);
        if (check && check.existe) {
            const tipoLabel = tipoDocumento === 'DNI' ? 'DNI' : 'Carnet de Extranjería';
            setAlert(`Este ${tipoLabel} ya está registrado. Si ya tienes cuenta, inicia sesión.`);
            document.getElementById('reg-dni').classList.add('field-error');
            btn.innerHTML = '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Consultar';
            btn.disabled  = false;
            return;
        }

        // Llamar al endpoint correcto según tipo
        const endpoint = tipoDocumento === 'DNI'
            ? `/reniec/dni/${numero}`
            : `/reniec/ce/${numero}`;
        const d = await apiGet(endpoint);

        // Nombres y apellidos (siempre vienen)
        document.getElementById('reg-nombres').value   = d.nombres   || '';
        document.getElementById('reg-apellidos').value = d.apellidos || '';

        // Campos que pueden venir vacíos
        aplicarCampoReniec('reg-fecha-nac', 'reg-fecha-nac-picker', d.fechaNacimiento);
        aplicarCampoUbigeo('reg-departamento', 'sel-departamento', d.departamento);
        aplicarCampoUbigeo('reg-provincia',    'sel-provincia',    d.provincia);
        aplicarCampoUbigeo('reg-distrito',     'sel-distrito',     d.distrito);
        aplicarDireccion(d.direccion);

        // Nacionalidad según tipo de documento
        await aplicarNacionalidad(d.nacionalidad);

        if (!d.departamento) await cargarDepartamentos();

        const tipoLabel = tipoDocumento === 'DNI' ? 'DNI' : 'C.E.';
        document.getElementById('dni-info-text').textContent = `${tipoLabel} verificado: ${d.nombreCompleto}`;
        document.getElementById('dni-info').classList.add('show');
        document.getElementById('reg-dni').classList.add('valid');
        setAlert('');
        numeroConsultado = numero;
        bloquearCampoDoc();
        actualizarBotonRegistro();

    } catch (e) {
        const msg = (e.message || '').toLowerCase();
        if (msg.includes('no encontrado') || msg.includes('404')) {
            const tipoLabel = tipoDocumento === 'DNI' ? 'DNI' : 'Carnet de Extranjería';
            setAlert(`${tipoLabel} no encontrado. Verifica el número ingresado.`);
        } else {
            setAlert('Error al consultar la API. Intenta nuevamente en unos segundos.');
        }
        document.getElementById('dni-info').classList.remove('show');
        limpiarCamposPersonales();
    } finally {
        document.getElementById('btn-reniec').innerHTML =
            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg> Consultar';
        if (!numeroConsultado) document.getElementById('btn-reniec').disabled = false;
    }
}

async function consultarReniec() { await consultarDocumento(); }

// NACIONALIDAD


async function aplicarNacionalidad(valor) {
    const inputNac = document.getElementById('reg-nacionalidad');
    const selNac   = document.getElementById('sel-nacionalidad');

    if (tipoDocumento === 'DNI') {
        // DNI → PERUANA, bloqueado
        inputNac.value         = 'PERUANA';
        inputNac.placeholder   = '';
        inputNac.style.display = '';
        inputNac.readOnly      = true;
        inputNac.classList.add('field-locked');
        selNac.style.display   = 'none';

    } else if (valor) {
        // C.E. y la API devuelve la nacionalidad → bloqueado con ese valor
        inputNac.value         = valor;
        inputNac.placeholder   = '';
        inputNac.style.display = '';
        inputNac.readOnly      = true;
        inputNac.classList.add('field-locked');
        selNac.style.display   = 'none';

    } else {
        // C.E. y la API NO devolvió la nacionalidad → select editable
        inputNac.value         = '';
        inputNac.style.display = 'none';
        selNac.style.display   = '';
        selNac.value           = '';
        selNac.disabled        = false;
        await cargarNacionalidades();
    }
}

async function cargarNacionalidades() {
    // No recargar si ya están cargadas
    if (document.getElementById('sel-nacionalidad').options.length > 1) return;
    try {
        const lista = await apiGet('/nacionalidades');
        const sel   = document.getElementById('sel-nacionalidad');
        sel.innerHTML = '<option value="">Selecciona tu nacionalidad</option>';
        lista.forEach(n => {
            const opt = document.createElement('option');
            opt.value = n.gentilicio;
            // Muestra: "Japón — JAPONÉS"
            opt.text  = `${n.pais} — ${n.gentilicio}`;
            sel.appendChild(opt);
        });
    } catch (e) {
        console.error('Error cargando nacionalidades:', e);
    }
}

function onNacionalidadChange(valor) {
    // "valor" es el gentilicio (ej: "JAPONÉS")
    // Se muestra el gentilicio en el input bloqueado para confirmación visual
    document.getElementById('reg-nacionalidad').value = valor;
}

// EMAIL

/**
 * Evalúa si todos los campos obligatorios están completos y habilita/deshabilita
 * el botón "Crear mi cuenta" en consecuencia.
 *
 * Requisitos para habilitar el botón:
 *  1. Documento consultado y bloqueado (numeroConsultado no vacío)
 *  2. Nombres y apellidos (vienen de la API, pero se comprueba que no estén vacíos)
 *  3. Fecha de nacimiento (input o picker)
 *  4. Departamento, provincia, distrito
 *  5. Dirección
 *  6. Nacionalidad (input bloqueado o combo)
 *  7. Email verificado con código
 *  8. Contraseña con al menos 6 caracteres
 *  9. Teléfono con 9 dígitos
 */
function actualizarBotonRegistro() {
    const btn = document.getElementById('btn-registro');

    const v = {
        documento:      !!numeroConsultado,
        nombres:        !!document.getElementById('reg-nombres').value.trim(),
        apellidos:      !!document.getElementById('reg-apellidos').value.trim(),
        fechaNac:       !!(document.getElementById('reg-fecha-nac').value.trim() ||
            document.getElementById('reg-fecha-nac-picker').value.trim()),
        departamento:   !!(document.getElementById('reg-departamento').value.trim() ||
            (() => { const s = document.getElementById('sel-departamento'); return s.style.display !== 'none' && s.value; })()),
        provincia:      !!(document.getElementById('reg-provincia').value.trim() ||
            (() => { const s = document.getElementById('sel-provincia'); return s.style.display !== 'none' && s.value; })()),
        distrito:       !!(document.getElementById('reg-distrito').value.trim() ||
            (() => { const s = document.getElementById('sel-distrito'); return s.style.display !== 'none' && s.value; })()),
        direccion:      !!document.getElementById('reg-direccion').value.trim(),
        nacionalidad:   !!((() => {
            const sel = document.getElementById('sel-nacionalidad');
            const inp = document.getElementById('reg-nacionalidad');
            return sel.style.display !== 'none' ? sel.value : inp.value;
        })().trim()),
        email:          emailVerificado,
        password:       document.getElementById('reg-password').value.length >= 6,
        telefono:       telefonoValido,
    };

    const todosOk = Object.values(v).every(Boolean);

    btn.disabled = !todosOk;
    if (todosOk) {
        btn.classList.remove('btn-disabled');
        btn.title = '';
    } else {
        btn.classList.add('btn-disabled');
        // Construir mensaje de ayuda con los campos faltantes
        const etiquetas = {
            documento:    'consultar documento',
            nombres:      'nombres',
            apellidos:    'apellidos',
            fechaNac:     'fecha de nacimiento',
            departamento: 'departamento',
            provincia:    'provincia',
            distrito:     'distrito',
            direccion:    'dirección',
            nacionalidad: 'nacionalidad',
            email:        'verificar correo',
            password:     'contraseña (mín. 6 caracteres)',
            telefono:     'teléfono (9 dígitos)',
        };
        const faltantes = Object.entries(v)
            .filter(([, ok]) => !ok)
            .map(([k]) => etiquetas[k])
            .join(', ');
        btn.title = `Falta: ${faltantes}`;
    }
}

function onEmailInput() {
    if (emailVerificado) return;
    if (!emailEnVerificacion) limpiarHint('hint-email');
}

async function validarEmailBlur() {
    if (emailVerificado)     return;
    if (emailEnVerificacion) return;
    const email = document.getElementById('reg-email').value.trim();
    if (!email) return setHint('hint-email', '');
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
        return setHint('hint-email', 'Formato de correo inválido', 'error');

    setHint('hint-email', 'Verificando...', 'info');
    try {
        const check = await apiGet('/usuarios/publico/existe-email/' + encodeURIComponent(email)).catch(() => null);
        if (check && check.existe) {
            setHint('hint-email', 'Este correo ya está registrado. Usa otro o inicia sesión.', 'error');
            document.getElementById('reg-email').classList.add('field-error');
            document.getElementById('reg-email').classList.remove('valid');
        } else {
            setHint('hint-email', 'Correo disponible', 'success');
            document.getElementById('reg-email').classList.add('valid');
            document.getElementById('reg-email').classList.remove('field-error');
            bloquearEmail();
            document.getElementById('email-verify-row').style.display = 'block';
            setHint('hint-codigo-email', 'Haz clic en "Pedir código" para recibir el código en tu correo.', 'info');
        }
    } catch (e) { setHint('hint-email', ''); }
}

function bloquearEmail() {
    const input = document.getElementById('reg-email');
    const lapiz = document.getElementById('btn-edit-email');
    input.readOnly = true;
    input.classList.add('doc-locked');
    if (lapiz) lapiz.style.display = 'flex';
    emailEnVerificacion = true;
}

function habilitarEdicionEmail() {
    if (emailVerificado) return;
    const input    = document.getElementById('reg-email');
    const lapiz    = document.getElementById('btn-edit-email');
    const btnPedir = document.getElementById('btn-pedir-codigo-email');

    input.readOnly = false;
    input.classList.remove('doc-locked', 'valid', 'field-error');
    if (lapiz) lapiz.style.display = 'none';
    emailEnVerificacion = false;

    document.getElementById('email-verify-row').style.display  = 'none';
    document.getElementById('email-paso1').style.display       = 'block';
    document.getElementById('email-paso2').style.display       = 'none';
    document.getElementById('codigo-email').value              = '';
    document.getElementById('codigo-email').disabled           = false;
    document.getElementById('btn-enviar-codigo-email').disabled = true;
    document.getElementById('btn-enviar-codigo-email').innerHTML =
        '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Enviar código';
    document.getElementById('btn-enviar-codigo-email').classList.remove('btn-verified');

    if (btnPedir) {
        btnPedir.disabled  = false;
        btnPedir.innerHTML =
            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> Pedir código';
    }

    limpiarHint('hint-email');
    limpiarHint('hint-codigo-email');
    input.focus();
}

async function pedirCodigoEmail() {
    const email    = document.getElementById('reg-email').value.trim();
    const btnPedir = document.getElementById('btn-pedir-codigo-email');
    if (!email) return setHint('hint-codigo-email', 'Ingresa tu correo primero', 'error');
    if (btnPedir) { btnPedir.disabled = true; btnPedir.innerHTML = '<span class="spinner-sm"></span> Enviando...'; }
    try {
        await apiPost('/auth/pre-registro/enviar-codigo-email', { email });
        document.getElementById('email-paso1').style.display = 'none';
        document.getElementById('email-paso2').style.display = 'flex';
        setHint('hint-codigo-email', 'Código enviado. Revisa tu bandeja de ENTRADA o SPAM', 'info');
        document.getElementById('codigo-email').focus();
        iniciarCountdownReenvio('btn-reenviar-email', 60);
    } catch (e) {
        setHint('hint-codigo-email', e.message || 'Error al enviar código', 'error');
        if (btnPedir) {
            btnPedir.disabled  = false;
            btnPedir.innerHTML =
                '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg> Pedir código';
        }
    }
}

function habilitarEnvioEmail() {
    document.getElementById('btn-enviar-codigo-email').disabled =
        document.getElementById('codigo-email').value.length !== 6;
}

async function verificarCodigoEmail() {
    const email  = document.getElementById('reg-email').value.trim();
    const codigo = document.getElementById('codigo-email').value.trim();
    if (codigo.length !== 6) return setHint('hint-codigo-email', 'El código debe tener 6 dígitos', 'error');
    const btn = document.getElementById('btn-enviar-codigo-email');
    btn.disabled  = true;
    btn.innerHTML = '<span class="spinner-sm"></span> Verificando...';
    try {
        await apiPost('/auth/pre-registro/verificar-email', { email, codigo });
        emailVerificado     = true;
        emailEnVerificacion = false;
        setHint('hint-codigo-email', 'Correo verificado ✓', 'success');
        document.getElementById('codigo-email').disabled = true;
        document.getElementById('btn-reenviar-email').style.display = 'none';
        btn.innerHTML = '✓ Verificado';
        btn.classList.add('btn-verified');
        const lapiz = document.getElementById('btn-edit-email');
        if (lapiz) lapiz.style.display = 'none';
        actualizarBotonRegistro();    } catch (e) {
        setHint('hint-codigo-email', e.message || 'Código incorrecto o expirado', 'error');
        btn.disabled  = false;
        btn.innerHTML =
            '<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg> Enviar código';
    }
}

// TELÉFONO  — validación básica

async function validarTelefonoBlur() {
    const tel = document.getElementById('reg-telefono').value.trim();
    telefonoValido = false;   // resetear mientras se valida
    if (!tel) return setHint('hint-telefono', '');
    if (tel.length !== 9) {
        setHint('hint-telefono', 'El teléfono debe tener 9 dígitos', 'error');
        document.getElementById('reg-telefono').classList.add('field-error');
        document.getElementById('reg-telefono').classList.remove('valid');
        actualizarBotonRegistro();
        return;
    }
    setHint('hint-telefono', 'Verificando...', 'info');
    try {
        const check = await apiGet('/usuarios/publico/existe-telefono/' + tel).catch(() => null);
        if (check && check.existe) {
            telefonoValido = false;
            setHint('hint-telefono', 'Número ya registrado, usa otro', 'error');
            document.getElementById('reg-telefono').classList.add('field-error');
            document.getElementById('reg-telefono').classList.remove('valid');
        } else {
            telefonoValido = true;
            setHint('hint-telefono', 'Teléfono disponible', 'success');
            document.getElementById('reg-telefono').classList.add('valid');
            document.getElementById('reg-telefono').classList.remove('field-error');
        }
    } catch (e) {
        // Si falla la verificación, permitir continuar (el backend lo rechazará si hay duplicado)
        telefonoValido = true;
        setHint('hint-telefono', '');
    }
    actualizarBotonRegistro();
}

// CONTRASEÑA

function togglePwd() {
    const input = document.getElementById('reg-password');
    const isPass = input.type === 'password';
    input.type = isPass ? 'text' : 'password';
    document.getElementById('eye-off').style.display = isPass ? 'none'  : 'block';
    document.getElementById('eye-on').style.display  = isPass ? 'block' : 'none';
}

function calcularFuerza() {
    const pwd  = document.getElementById('reg-password').value;
    const bar  = document.getElementById('pwd-strength');
    const fill = document.getElementById('pwd-fill');
    const lbl  = document.getElementById('pwd-label');
    if (!pwd) { bar.classList.remove('show'); return; }
    bar.classList.add('show');
    let score = 0;
    if (pwd.length >= 6)          score++;
    if (pwd.length >= 10)         score++;
    if (/[A-Z]/.test(pwd))        score++;
    if (/[0-9]/.test(pwd))        score++;
    if (/[^A-Za-z0-9]/.test(pwd)) score++;
    const n = [{w:'20%',bg:'#ef4444',t:'Muy débil'},{w:'40%',bg:'#f97316',t:'Débil'},
        {w:'60%',bg:'#eab308',t:'Regular'},{w:'80%',bg:'#22c55e',t:'Buena'},
        {w:'100%',bg:'#16a34a',t:'Excelente'}][Math.min(score, 4)];
    fill.style.width = n.w; fill.style.background = n.bg;
    lbl.style.color  = n.bg; lbl.textContent = n.t;
}

// REGISTRO FINAL

document.getElementById('btn-registro').onclick = async () => {
    setAlert('');
    const nombres         = document.getElementById('reg-nombres').value.trim();
    const apellidos       = document.getElementById('reg-apellidos').value.trim();
    const email           = document.getElementById('reg-email').value.trim();
    const password        = document.getElementById('reg-password').value;
    const telefono        = document.getElementById('reg-telefono').value.trim();
    const dni             = document.getElementById('reg-dni').value.trim();
    const rol             = document.getElementById('reg-rol').value;
    const fechaNacimiento = document.getElementById('reg-fecha-nac').value.trim();
    const departamento    = document.getElementById('reg-departamento').value.trim();
    const provincia       = document.getElementById('reg-provincia').value.trim();
    const distrito        = document.getElementById('reg-distrito').value.trim();
    const direccion       = document.getElementById('reg-direccion').value.trim();

    // Leer nacionalidad de donde esté visible (input o sel)
    const selNac    = document.getElementById('sel-nacionalidad');
    const inputNac  = document.getElementById('reg-nacionalidad');
    const nacionalidad = (selNac.style.display !== 'none' ? selNac.value : inputNac.value)
        .trim().toUpperCase();

    const btn = document.getElementById('btn-registro');
    btn.innerHTML = '<span class="spinner"></span> Creando cuenta...';
    btn.disabled  = true;

    try {
        await apiPost('/auth/registro', {
            nombres, apellidos, email, password, rol,
            tipoDocumento,
            numeroDocumento: dni,
            nacionalidad:    nacionalidad    || null,
            telefono:        telefono        || null,
            fechaNacimiento: fechaNacimiento || null,
            departamento:    departamento    || null,
            provincia:       provincia       || null,
            distrito:        distrito        || null,
            direccion:       direccion       || null,
        });

        setAlert('¡Cuenta creada correctamente!', 'success');
        document.getElementById('redirect-msg').style.display = 'flex';
        setTimeout(() => window.location.href = '/login', 3000);

    } catch (e) {
        let msg = e.message;
        if (msg.toLowerCase().includes('email')) {
            msg = 'Este correo ya está registrado. Usa otro o inicia sesión.';
            setHint('hint-email', msg, 'error');
        } else if (msg.toLowerCase().includes('dni')) {
            msg = 'Este DNI ya está registrado en el sistema.';
        } else if (msg.toLowerCase().includes('telefono')) {
            msg = 'Este teléfono ya está registrado. Usa otro número.';
            setHint('hint-telefono', 'Número ya registrado, usa otro', 'error');
        }
        setAlert(msg);
    } finally {
        btn.innerHTML = 'Crear mi cuenta';
        btn.disabled  = false;
    }
};

// UBIGEO

function aplicarCampoReniec(inputId, pickerId, valor) {
    const input  = document.getElementById(inputId);
    const picker = document.getElementById(pickerId);
    if (valor) {
        input.value = valor; input.style.display = ''; picker.style.display = 'none';
    } else {
        input.value = ''; input.style.display = 'none';
        picker.style.display = ''; picker.value = '';
    }
}

function aplicarCampoUbigeo(inputId, selectId, valor) {
    const input  = document.getElementById(inputId);
    const select = document.getElementById(selectId);
    if (valor) {
        input.value = valor; input.style.display = ''; select.style.display = 'none';
    } else {
        input.value = ''; input.style.display = 'none';
        select.style.display = ''; select.value = '';
    }
}

function aplicarDireccion(valor) {
    const input = document.getElementById('reg-direccion');
    if (valor) {
        input.value = valor; input.readOnly = true;
        input.classList.add('field-locked'); input.placeholder = '';
    } else {
        input.value = ''; input.readOnly = false;
        input.classList.remove('field-locked');
        input.placeholder = 'Escribe tu dirección completa';
        input.style.background = ''; input.style.cursor = '';
    }
}

async function cargarDepartamentos() {
    try {
        const lista = await apiGet('/ubigeo/departamentos');
        const sel   = document.getElementById('sel-departamento');
        sel.innerHTML = '<option value="">Selecciona departamento</option>';
        lista.forEach(dep => {
            const opt = document.createElement('option');
            opt.value = dep.id; opt.text = dep.name;
            sel.appendChild(opt);
        });
    } catch (e) { console.error('Error cargando departamentos:', e); }
}

async function onDepartamentoChange(departamentoId) {
    const sel = document.getElementById('sel-departamento');
    document.getElementById('reg-departamento').value = sel.options[sel.selectedIndex]?.text || '';
    const selProv = document.getElementById('sel-provincia');
    const selDist = document.getElementById('sel-distrito');
    selProv.innerHTML = '<option value="">Selecciona provincia</option>';
    selDist.innerHTML = '<option value="">Selecciona distrito</option>';
    document.getElementById('reg-provincia').value = '';
    document.getElementById('reg-distrito').value  = '';
    actualizarBotonRegistro();
    if (!departamentoId) return;
    await cargarProvincias(departamentoId);
}

async function cargarProvincias(departamentoId) {
    try {
        const lista = await apiGet(`/ubigeo/provincias/${departamentoId}`);
        const sel   = document.getElementById('sel-provincia');
        sel.innerHTML = '<option value="">Selecciona provincia</option>';
        if (sel.style.display === 'none' && !document.getElementById('reg-provincia').value) {
            document.getElementById('reg-provincia').style.display = 'none';
            sel.style.display = '';
        }
        lista.forEach(prov => {
            const opt = document.createElement('option');
            opt.value = prov.id; opt.text = prov.name;
            sel.appendChild(opt);
        });
    } catch (e) { console.error('Error cargando provincias:', e); }
}

async function onProvinciaChange(provinciaId) {
    const sel = document.getElementById('sel-provincia');
    document.getElementById('reg-provincia').value = sel.options[sel.selectedIndex]?.text || '';
    const selDist = document.getElementById('sel-distrito');
    selDist.innerHTML = '<option value="">Selecciona distrito</option>';
    document.getElementById('reg-distrito').value = '';
    actualizarBotonRegistro();
    if (!provinciaId) return;
    try {
        const lista = await apiGet(`/ubigeo/distritos/${provinciaId}`);
        if (selDist.style.display === 'none' && !document.getElementById('reg-distrito').value) {
            document.getElementById('reg-distrito').style.display = 'none';
            selDist.style.display = '';
        }
        lista.forEach(dist => {
            const opt = document.createElement('option');
            opt.value = dist.id; opt.text = dist.name;
            selDist.appendChild(opt);
        });
    } catch (e) { console.error('Error cargando distritos:', e); }
}

// COUNTDOWN REENVÍO

function iniciarCountdownReenvio(btnId, segundos) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.style.display = 'inline-flex';
    btn.disabled      = true;
    let restante      = segundos;
    btn.textContent   = `Reenviar en ${restante}s`;
    const timer = setInterval(() => {
        restante--;
        if (restante <= 0) {
            clearInterval(timer);
            btn.disabled    = false;
            btn.textContent = 'Reenviar';
        } else {
            btn.textContent = `Reenviar en ${restante}s`;
        }
    }, 1000);
}

// Enter en campo de documento
document.getElementById('reg-dni').addEventListener('keydown', e => {
    if (e.key === 'Enter') consultarDocumento();
});