'use strict';

const API = '/api';

/* ruta segun el rol */
const RUTAS = {
    ADMIN:       '/admin/dashboard',
    PROPIETARIO: '/propietario/canchas',
    CLIENTE:     '/cliente/buscar',
};

/* alerta icono */
const ALERT_ICONS = {
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

/* alerta */
function setAlert(id, msg, type = 'error') {
    const el = document.getElementById(id);
    if (!el) return;
    el.innerHTML = msg
        ? `<div class="alert alert-${type}">${ALERT_ICONS[type] || ''}<span>${msg}</span></div>`
        : '';
}

async function apiPost(url, body) {
    const res  = await fetch(API + url, {
        method:  'POST',
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body:    JSON.stringify(body),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.mensaje || data.message || 'Credenciales incorrectas');
    return data;
}

/* contraseña ojito */
function togglePwd() {
    const input  = document.getElementById('login-password');
    const eyeOff = document.getElementById('eye-off');
    const eyeOn  = document.getElementById('eye-on');
    const isPass = input.type === 'password';
    input.type           = isPass ? 'text'  : 'password';
    eyeOff.style.display = isPass ? 'none'  : 'block';
    eyeOn.style.display  = isPass ? 'block' : 'none';
}

function getStoredToken() {
    return localStorage.getItem('ss_token') || sessionStorage.getItem('ss_token');
}
function getStoredUser() {
    try {
        const raw = localStorage.getItem('ss_user') || sessionStorage.getItem('ss_user');
        return JSON.parse(raw || 'null');
    } catch { return null; }
}

/* recordar credenciales */
function cargarCredencialesGuardadas() {
    const emailGuardado = localStorage.getItem('ss_remember_email');
    const pwdGuardada   = localStorage.getItem('ss_remember_pwd');
    if (emailGuardado && pwdGuardada) {
        document.getElementById('login-email').value    = emailGuardado;
        document.getElementById('login-password').value = pwdGuardada;
        document.getElementById('remember-me').checked  = true;
    }
}

function guardarCredenciales(email, password) {
    localStorage.setItem('ss_remember_email', email);
    localStorage.setItem('ss_remember_pwd',   password);
}

function borrarCredenciales() {
    localStorage.removeItem('ss_remember_email');
    localStorage.removeItem('ss_remember_pwd');
}

/* helpers de hint por campo  */
function setHint(id, msg, type = '') {
    const el = document.getElementById(id);
    if (!el) return;
    if (!msg) { el.className = 'field-hint'; el.innerHTML = ''; return; }
    const icons = {
        error:   '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>',
        success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
        info:    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
    };
    el.className = `field-hint hint-${type} show`;
    el.innerHTML = `${icons[type] || ''}<span>${msg}</span>`;
}

/* validación email en tiempo real */
document.getElementById('login-email').addEventListener('blur', async function () {
    const email = this.value.trim();
    if (!email) {
        setHint('hint-email', 'Ingresa tu correo electrónico', 'error');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setHint('hint-email', 'Formato de correo inválido', 'error');
        return;
    }
    // Verificar si existe en el sistema
    try {
        const res  = await fetch(`${API}/usuarios/publico/existe-email/${encodeURIComponent(email)}`);
        const data = await res.json().catch(() => ({}));
        if (!data.existe) {
            setHint('hint-email', 'Correo no encontrado. Verifica o crea una cuenta', 'error');
        } else {
            setHint('hint-email', '');  // existe → limpiar hint
        }
    } catch (_) {
        setHint('hint-email', '');  // error de red: no bloquear al usuario
    }
});

document.getElementById('login-email').addEventListener('input', function () {
    setHint('hint-email', '');  // limpiar hint mientras escribe
});

/* validación password en tiempo real */
document.getElementById('login-password').addEventListener('blur', function () {
    const pwd = this.value;
    if (!pwd) {
        setHint('hint-password', 'Ingresa tu contraseña', 'error');
    } else if (pwd.length < 6) {
        setHint('hint-password', 'La contraseña debe tener al menos 6 caracteres', 'error');
    } else {
        setHint('hint-password', '');
    }
});

document.getElementById('login-password').addEventListener('input', function () {
    setHint('hint-password', '');  // limpiar hint mientras escribe
});

/* login */
document.getElementById('btn-login').onclick = async () => {
    const email    = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;
    const remember = document.getElementById('remember-me').checked;

    // Validaciones con hints por campo
    if (!email) {
        setHint('hint-email', 'Ingresa tu correo electrónico', 'error');
        return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
        setHint('hint-email', 'Formato de correo inválido', 'error');
        return;
    }
    if (!password) {
        setHint('hint-password', 'Ingresa tu contraseña', 'error');
        return;
    }
    if (password.length < 6) {
        setHint('hint-password', 'La contraseña debe tener al menos 6 caracteres', 'error');
        return;
    }

    // Limpiar hints antes de intentar login
    setHint('hint-email', '');
    setHint('hint-password', '');

    const btn = document.getElementById('btn-login');
    btn.innerHTML = '<span class="spinner"></span> Ingresando...';
    btn.disabled  = true;
    setAlert('login-alert', '');

    try {
        const data = await apiPost('/auth/login', { email, password });

        // DOBLE ROL: pedir con cuál desea ingresar
        if (data.requiereSeleccion) {
            btn.innerHTML = 'Iniciar sesion';
            btn.disabled  = false;
            mostrarSeleccionRol(data, remember);
            return;
        }

        guardarSesionYRedirigir(data, remember, email, password);

    } catch (e) {
        const msg = e.message || 'Credenciales incorrectas';

        // Mensajes que vienen del backend y deben mostrarse tal cual en el alert,
        // sin pasar por la lógica de "¿el email existe?".
        const esMensajeEspecial =
            msg.includes('bloqueada') ||
            msg.includes('bloqueado') ||
            msg.includes('inactiva') ||
            msg.includes('pendiente de aprobación');

        if (esMensajeEspecial) {
            // Mostrar el mensaje exacto del backend en el alert principal
            setAlert('login-alert', msg);
            setHint('hint-email', '');
            setHint('hint-password', '');
        } else {
            // Error genérico: verificar si el correo existe para dar hint preciso
            try {
                const res  = await fetch(`${API}/usuarios/publico/existe-email/${encodeURIComponent(email)}`);
                const data = await res.json().catch(() => ({}));
                if (!data.existe) {
                    setHint('hint-email', 'Correo no encontrado. Verifica o crea una cuenta', 'error');
                    setHint('hint-password', '');
                } else {
                    // Correo sí existe → el error es de contraseña
                    setHint('hint-password', 'Contraseña incorrecta', 'error');
                    setHint('hint-email', '');
                }
            } catch (_) {
                // Sin conexión al backend → mostrar mensaje genérico
                setAlert('login-alert', msg);
            }
        }

        btn.innerHTML = 'Iniciar sesion';
        btn.disabled  = false;
    }
};

/* contraseña */
document.getElementById('login-password').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('btn-login').click();
});

/* guarda la sesión (token/user) y redirige según el rol*/
function guardarSesionYRedirigir(data, remember, email, password) {
    if (remember) {
        localStorage.setItem('ss_token', data.token);
        localStorage.setItem('ss_user',  JSON.stringify(data));
        sessionStorage.removeItem('ss_token');
        sessionStorage.removeItem('ss_user');
        if (email && password) guardarCredenciales(email, password);
    } else {
        sessionStorage.setItem('ss_token', data.token);
        sessionStorage.setItem('ss_user',  JSON.stringify(data));
        localStorage.removeItem('ss_token');
        localStorage.removeItem('ss_user');
        borrarCredenciales();
    }
    window.location.href = RUTAS[data.rol] || '/login';
}

/* pantalla "¿Cómo deseas ingresar?" para doble rol*/
function mostrarSeleccionRol(data, remember) {
    document.getElementById('rol-bienvenida').textContent = `Bienvenido, ${data.nombreMostrar || ''}`;
    document.getElementById('rol-overlay-error').textContent = '';
    document.getElementById('rol-overlay').style.display = 'flex';

    const elegir = async (rol) => {
        document.getElementById('rol-overlay-error').textContent = '';
        try {
            const resp = await apiPost('/auth/seleccionar-rol', { preToken: data.preToken, rol });
            document.getElementById('rol-overlay').style.display = 'none';
            guardarSesionYRedirigir(resp, remember);
        } catch (e) {
            document.getElementById('rol-overlay-error').textContent =
                e.message || 'No se pudo completar el ingreso. Intenta de nuevo.';
        }
    };

    document.getElementById('btn-rol-cliente').onclick     = () => elegir('CLIENTE');
    document.getElementById('btn-rol-propietario').onclick = () => elegir('PROPIETARIO');
}

function checkSession() {
    try {
        if (window.location.search.includes('logout=true')) {
            localStorage.removeItem('ss_token');
            localStorage.removeItem('ss_user');
            sessionStorage.removeItem('ss_token');
            sessionStorage.removeItem('ss_user');
            window.history.replaceState({}, '', '/login');
            return;
        }

        const token = getStoredToken();
        const user  = getStoredUser();
        if (!token || !user) return;

        // Verificar token con el backend antes de redirigir
        fetch(API + '/auth/verificar', {
            headers: { 'Authorization': 'Bearer ' + token },
        })
            .then(res => {
                if (res.ok) {
                    window.location.href = RUTAS[user.rol] || '/login';
                } else {
                    // Token expirado  solo borra sesion
                    localStorage.removeItem('ss_token');
                    localStorage.removeItem('ss_user');
                    sessionStorage.removeItem('ss_token');
                    sessionStorage.removeItem('ss_user');
                }
            })
            .catch(() => {
            });

    } catch {
        // Solo borra sesion
        localStorage.removeItem('ss_token');
        localStorage.removeItem('ss_user');
        sessionStorage.removeItem('ss_token');
        sessionStorage.removeItem('ss_user');
    }
}

cargarCredencialesGuardadas();
checkSession();