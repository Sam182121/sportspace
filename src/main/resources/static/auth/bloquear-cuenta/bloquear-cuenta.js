'use strict';

const API = 'http://localhost:8080/api';

const params = new URLSearchParams(window.location.search);
const TOKEN  = params.get('token') || '';
const EMAIL  = params.get('email') || '';

// ── Helpers de UI ──────────────────────────────────────────────────────────

function mostrarVista(id) {
    ['view-bloqueo', 'view-bloqueado', 'view-invalido'].forEach(v => {
        const el = document.getElementById(v);
        if (el) el.style.display = (v === id) ? 'block' : 'none';
    });
}

function setAlert(id, msg, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    const iconError = `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
        width="18" height="18" style="flex-shrink:0">
        <circle cx="12" cy="12" r="10"/>
        <line x1="12" y1="8" x2="12" y2="12"/>
        <line x1="12" y1="16" x2="12.01" y2="16"/>
    </svg>`;
    el.innerHTML = msg
        ? `<div class="alert alert-${type}" style="display:flex;align-items:center;gap:8px;">
               ${type === 'error' ? iconError : ''}
               <span>${msg}</span>
           </div>`
        : '';
}

// ── Validar token al cargar la página ─────────────────────────────────────
// Si el token ya fue usado o expiró mostramos la vista de enlace inválido
// ANTES de que el usuario vea el formulario de bloqueo.
async function inicializar() {

    // Sin token en la URL → inválido directamente
    if (!TOKEN) {
        document.getElementById('invalido-msg').textContent =
            'El enlace de bloqueo no es válido. Si necesitas bloquear tu cuenta, escríbenos directamente.';
        mostrarVista('view-invalido');
        return;
    }

    // Consultar al backend si el token sigue siendo válido
    try {
        const res  = await fetch(`${API}/auth/validar-token-bloqueo?token=${encodeURIComponent(TOKEN)}`);
        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            // Token usado, expirado o inválido → mostrar vista de enlace inválido
            const msg = data.mensaje || data.message || 'Este enlace ya fue utilizado o ha expirado.';
            document.getElementById('invalido-msg').textContent = msg;
            mostrarVista('view-invalido');
            return;
        }
    } catch (_) {
        // Si hay error de red, dejar pasar y mostrar el formulario
        // (el backend validará de nuevo al confirmar)
    }

    // Token válido → mostrar formulario y personalizar nombre
    mostrarVista('view-bloqueo');
    personalizarNombre();
}

// ── Personalizar mensaje con el primer nombre del usuario ─────────────────
async function personalizarNombre() {
    if (!EMAIL) return;
    try {
        const res  = await fetch(`${API}/usuarios/publico/nombre-por-email/${encodeURIComponent(EMAIL)}`);
        const data = await res.json().catch(() => ({}));
        if (data.nombre) {
            const primerNombre = data.nombre.split(' ')[0];
            const el = document.getElementById('bloqueo-nombre-msg');
            if (el) el.textContent =
                `${primerNombre}, si crees que otra persona accedió a tu cuenta, bloquéala ahora.`;
        }
    } catch (_) { /* sin personalización si falla */ }
}

// ── Confirmar bloqueo ──────────────────────────────────────────────────────
async function bloquearCuenta() {

    const btn = document.getElementById('btn-bloquear-ahora');
    btn.innerHTML = `
        <span class="spinner"></span> Bloqueando...`;
    btn.disabled = true;
    setAlert('bloqueo-alert', '');

    try {
        const res  = await fetch(`${API}/auth/bloquear-cuenta`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ token: TOKEN }),
        });
        const data = await res.json().catch(() => ({}));

        if (res.ok) {
            // Éxito — mostrar vista de cuenta bloqueada (sin botón Cancelar)
            mostrarVista('view-bloqueado');
            if (data.nombre) {
                const primerNombre = data.nombre.split(' ')[0];
                const msg = document.getElementById('bloqueado-msg');
                if (msg) msg.textContent =
                    `${primerNombre}, tu cuenta ha sido bloqueada temporalmente. ` +
                    `Nadie podrá ingresar hasta que te comuniques con nosotros.`;
            }

        } else {
            // Error del backend — si el token ya fue usado o expiró, ir a vista inválido
            const mensaje = data.mensaje || data.message || 'No se pudo bloquear la cuenta.';
            const esTokenInvalido =
                mensaje.toLowerCase().includes('ya fue utilizado') ||
                mensaje.toLowerCase().includes('expirado')         ||
                mensaje.toLowerCase().includes('no es válido');

            if (esTokenInvalido) {
                const el = document.getElementById('invalido-msg');
                if (el) el.textContent = mensaje;
                mostrarVista('view-invalido');
            } else {
                setAlert('bloqueo-alert', mensaje);
                btn.innerHTML = `
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                         stroke-width="2.5" style="vertical-align:middle;margin-right:6px;margin-top:-2px;">
                        <rect x="3" y="11" width="18" height="11" rx="2"/>
                        <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    Bloquear ahora`;
                btn.disabled = false;
            }
        }

    } catch (e) {
        setAlert('bloqueo-alert', 'Error de conexión. Verifica tu internet e intenta de nuevo.');
        btn.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2.5" style="vertical-align:middle;margin-right:6px;margin-top:-2px;">
                <rect x="3" y="11" width="18" height="11" rx="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            Bloquear ahora`;
        btn.disabled = false;
    }
}

// Arrancar al cargar
inicializar();