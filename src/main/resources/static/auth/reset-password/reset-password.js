'use strict';

const API = 'http://localhost:8080/api';

const ICONS = {
    error: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>`,
    success: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>`,
};

function setAlert(id, msg, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerHTML = msg
        ? `<div class="alert alert-${type}">${ICONS[type] || ''}<span>${msg}</span></div>`
        : '';
}

function mostrar(id) {
    ['view-loading', 'view-invalid', 'view-form', 'view-ok'].forEach(v => {
        document.getElementById(v).style.display = (v === id) ? 'block' : 'none';
    });
}

// Leer token de la URL
const params = new URLSearchParams(window.location.search);
const TOKEN  = params.get('token') || '';

// Paso 1: Validar token al cargar la página
(async () => {
    if (!TOKEN) {
        document.getElementById('invalid-msg').textContent =
            'No se encontró ningún token en el enlace. Solicita uno nuevo desde el inicio de sesión.';
        mostrar('view-invalid');
        return;
    }
    try {
        const res = await fetch(`${API}/auth/validar-token?token=${encodeURIComponent(TOKEN)}`);
        if (res.ok) {
            mostrar('view-form');
        } else {
            const data = await res.json().catch(() => ({}));
            document.getElementById('invalid-msg').textContent =
                data.mensaje || data.message || 'Este enlace ha expirado o ya fue utilizado.';
            mostrar('view-invalid');
        }
    } catch {
        document.getElementById('invalid-msg').textContent =
            'No se pudo verificar el enlace. Verifica tu conexión e intenta de nuevo.';
        mostrar('view-invalid');
    }
})();

// Indicador de fuerza de contraseña
document.getElementById('rp-password').addEventListener('input', function () {
    const val  = this.value;
    const fill = document.getElementById('strength-fill');
    const lbl  = document.getElementById('strength-label');
    let score = 0;
    if (val.length >= 6)           score++;
    if (val.length >= 10)          score++;
    if (/[A-Z]/.test(val))         score++;
    if (/[0-9]/.test(val))         score++;
    if (/[^A-Za-z0-9]/.test(val))  score++;
    const levels = [
        { pct: '0%',   color: '',        label: '' },
        { pct: '25%',  color: '#e02424', label: 'Muy débil' },
        { pct: '50%',  color: '#d97706', label: 'Débil' },
        { pct: '70%',  color: '#f59e0b', label: 'Moderada' },
        { pct: '88%',  color: '#10b981', label: 'Fuerte' },
        { pct: '100%', color: '#0e9f6e', label: 'Muy fuerte' },
    ];
    const lvl = val.length === 0 ? levels[0] : levels[Math.min(score, 5)];
    fill.style.width      = lvl.pct;
    fill.style.background = lvl.color;
    lbl.textContent       = lvl.label;
    lbl.style.color       = lvl.color;
});

// Ojitos
function togglePwd(inputId, offId, onId) {
    const input  = document.getElementById(inputId);
    const eyeOff = document.getElementById(offId);
    const eyeOn  = document.getElementById(onId);
    const isPass = input.type === 'password';
    input.type           = isPass ? 'text' : 'password';
    eyeOff.style.display = isPass ? 'none'  : 'block';
    eyeOn.style.display  = isPass ? 'block' : 'none';
}

// Paso 2: Enviar nueva contraseña
document.getElementById('btn-resetear').addEventListener('click', async () => {
    const nuevaPassword     = document.getElementById('rp-password').value;
    const confirmarPassword = document.getElementById('rp-confirm').value;

    if (!nuevaPassword || !confirmarPassword)
        return setAlert('rp-alert', 'Completa ambos campos.');
    if (nuevaPassword.length < 6)
        return setAlert('rp-alert', 'La contraseña debe tener al menos 6 caracteres.');
    if (nuevaPassword !== confirmarPassword)
        return setAlert('rp-alert', 'Las contraseñas no coinciden.');

    const btn = document.getElementById('btn-resetear');
    btn.innerHTML = '<span class="spinner"></span> Guardando...';
    btn.disabled  = true;
    setAlert('rp-alert', '');

    try {
        const res  = await fetch(`${API}/auth/reset-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ token: TOKEN, nuevaPassword, confirmarPassword }),
        });
        const data = await res.json().catch(() => ({}));

        if (res.ok) {
            // Personalizar mensaje con el primer nombre si el backend lo devuelve
            if (data.nombre) {
                const primerNombre = data.nombre.split(' ')[0];
                document.getElementById('ok-nombre-msg').textContent =
                    `${primerNombre}, tu contraseña fue actualizada correctamente.`;
            }
            // Mostrar vista de éxito — SIN mensaje de bloqueo ni botón de bloquear.
            // El correo de seguridad ya fue enviado automáticamente por el backend.
            mostrar('view-ok');
        } else {
            throw new Error(data.mensaje || data.message || 'No se pudo restablecer la contraseña.');
        }
    } catch (e) {
        setAlert('rp-alert', e.message || 'Error de conexión. Intenta de nuevo.');
        btn.innerHTML = 'Restablecer contraseña';
        btn.disabled  = false;
    }
});

// Enter en campos
['rp-password', 'rp-confirm'].forEach(id => {
    document.getElementById(id).addEventListener('keydown', e => {
        if (e.key === 'Enter') document.getElementById('btn-resetear').click();
    });
});