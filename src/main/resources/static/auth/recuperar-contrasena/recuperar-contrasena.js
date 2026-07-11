'use strict';

const API = 'http://localhost:8080/api';

/* ── Helpers de alerta ─────────────────────────────────────────────────── */
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

/* ── Enviar solicitud de recuperación ──────────────────────────────────── */
document.getElementById('btn-enviar').addEventListener('click', async () => {

    const email = document.getElementById('rc-email').value.trim();

    /* Validación mínima en frontend */
    if (!email) {
        return setAlert('rc-alert', 'Ingresa tu correo electrónico.');
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        return setAlert('rc-alert', 'Ingresa un correo electrónico válido.');
    }

    const btn = document.getElementById('btn-enviar');
    btn.innerHTML = '<span class="spinner"></span> Enviando...';
    btn.disabled  = true;
    setAlert('rc-alert', '');

    try {
        const res  = await fetch(`${API}/auth/recuperar-password`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify({ email }),
        });

        // El backend siempre devuelve 200 (incluso si el email no existe).
        // No importa el contenido, mostramos la vista de éxito.
        if (res.ok || res.status === 200) {
            document.getElementById('view-form').style.display = 'none';
            document.getElementById('view-ok').style.display   = 'block';
        } else {
            // Solo en caso de error técnico real (500, red, etc.)
            const data = await res.json().catch(() => ({}));
            throw new Error(data.mensaje || data.message || 'Error al procesar la solicitud.');
        }

    } catch (e) {
        setAlert('rc-alert', e.message || 'No se pudo conectar con el servidor. Intenta de nuevo.');
        btn.innerHTML = 'Enviar enlace de recuperación';
        btn.disabled  = false;
    }
});

/* ── Enviar con Enter ───────────────────────────────────────────────────── */
document.getElementById('rc-email').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('btn-enviar').click();
});