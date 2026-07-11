'use strict';

// ── Cloudinary upload ──────────────────────────────────────────
const CLOUDINARY_CLOUD_NAME    = 'dnwetom28';
const CLOUDINARY_UPLOAD_PRESET = 'sportspace_unsigned';

async function uploadToCloudinary(file, carpeta = 'sportspace') {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
    fd.append('folder', carpeta);
    const r = await fetch(
        `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`,
        { method: 'POST', body: fd }
    );
    if (!r.ok) throw new Error('Error al subir imagen');
    const d = await r.json();
    return d.secure_url;
}
// ───────────────────────────────────────────────────────────────
/**
 * Sube un File al CDN de Cloudinary y devuelve la URL segura (https).
 *
 * @param {File}   file     - El archivo elegido por el usuario
 * @param {string} carpeta  - Subcarpeta en Cloudinary (ej: 'vouchers', 'canchas')
 * @param {function} onProgress - Callback opcional: recibe % de progreso (0-100)
 * @returns {Promise<string>} URL pública del archivo subido
 *
 * Uso:
 *   const url = await uploadToCloudinary(file, 'vouchers');
 *   // url = "https://res.cloudinary.com/tucloud/image/upload/v.../vouchers/abc.jpg"
 */
async function uploadToCloudinary(file, carpeta = 'sportspace', onProgress = null) {
    if (!file) throw new Error('No se proporcionó ningún archivo');
    if (file.size > 5 * 1024 * 1024) throw new Error('El archivo supera el límite de 5 MB');

    const formData = new FormData();
    formData.append('file',          file);
    formData.append('upload_preset', CLOUDINARY_UPLOAD_PRESET);
    formData.append('folder',        carpeta);

    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();

        // Progreso de subida
        if (onProgress) {
            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    onProgress(Math.round((e.loaded / e.total) * 100));
                }
            });
        }

        xhr.addEventListener('load', () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const data = JSON.parse(xhr.responseText);
                    // secure_url = URL con HTTPS desde el CDN de Cloudinary
                    resolve(data.secure_url);
                } catch {
                    reject(new Error('Respuesta inválida de Cloudinary'));
                }
            } else {
                let msg = 'Error al subir el archivo';
                try {
                    const err = JSON.parse(xhr.responseText);
                    msg = err.error?.message || msg;
                } catch { /* ignora */ }
                reject(new Error(msg));
            }
        });

        xhr.addEventListener('error',  () => reject(new Error('Error de red al subir el archivo')));
        xhr.addEventListener('abort',  () => reject(new Error('Subida cancelada')));

        xhr.open('POST', `https://api.cloudinary.com/v1_1/${CLOUDINARY_CLOUD_NAME}/image/upload`);
        xhr.send(formData);
    });
}

/**
 * Helper: muestra un estado de carga mientras sube y luego el nombre del archivo.
 * Se usa en cualquier zona de drag&drop de la app.
 *
 * @param {File}     file         - Archivo a subir
 * @param {string}   carpeta      - Carpeta en Cloudinary
 * @param {string}   previewId    - ID del elemento HTML donde mostrar el estado
 * @param {function} onSuccess    - Callback con la URL final cuando termina
 * @param {function} onError      - Callback con el mensaje de error si falla
 */
async function subirArchivoConUI(file, carpeta, previewId, onSuccess, onError) {
    const el = document.getElementById(previewId);

    const setUI = (html) => { if (el) el.innerHTML = html; };
    const progressBar = (pct) => setUI(`
        <div style="font-size:12.5px;color:var(--text2);margin-bottom:6px">Subiendo… ${pct}%</div>
        <div style="background:var(--border);border-radius:4px;height:4px;overflow:hidden">
            <div style="background:var(--accent);height:100%;width:${pct}%;transition:width .2s"></div>
        </div>`);

    setUI(`<div style="font-size:12.5px;color:var(--text3)">Preparando…</div>`);

    try {
        const url = await uploadToCloudinary(file, carpeta, progressBar);
        setUI(`<strong style="color:var(--success)">${file.name}</strong>
               <br/><small style="color:var(--success)">✓ Subido correctamente</small>`);
        if (onSuccess) onSuccess(url);
    } catch (e) {
        setUI(`<span style="color:var(--danger)">${e.message || 'Error al subir'}</span>
               <br/><small style="color:var(--text3)">Toca para intentar de nuevo</small>`);
        if (onError) onError(e);
    }
}