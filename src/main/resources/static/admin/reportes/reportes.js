'use strict';

/* reportes */
const REPORTES = {
    usuarios:     { endpoint: '/admin/reportes/usuarios',     archivo: 'reporte_usuarios.xlsx',     btn: 'btnUsuarios'     },
    reservas:     { endpoint: '/admin/reportes/reservas',     archivo: 'reporte_reservas.xlsx',     btn: 'btnReservas'     },
    ingresos:     { endpoint: '/admin/reportes/ingresos',     archivo: 'reporte_ingresos.xlsx',     btn: 'btnIngresos'     },
    canchas:      { endpoint: '/admin/reportes/canchas',      archivo: 'reporte_canchas.xlsx',      btn: 'btnCanchas'      },
    propietarios: { endpoint: '/admin/reportes/propietarios', archivo: 'reporte_propietarios.xlsx', btn: 'btnPropietarios' },
};

/* descargar reporte */
async function descargarReporte(tipo) {
    const config = REPORTES[tipo];
    if (!config) return;

    const fechaInicio = document.getElementById('fechaInicio').value;
    const fechaFin    = document.getElementById('fechaFin').value;

    // Validar fechas
    if (fechaInicio && fechaFin && fechaInicio > fechaFin) {
        showToast('La fecha inicio no puede ser mayor a la fecha fin', 'error');
        return;
    }

    // Deshabilitar boton y mostrar estado
    const btn = document.getElementById(config.btn);
    const textoOriginal = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `
    <span class="spinner"></span>
    Generando...`;

    showToast(`Generando reporte de ${tipo}...`, 'info');

    try {

        let url = config.endpoint;
        const params = [];
        if (fechaInicio) params.push(`fechaInicio=${fechaInicio}`);
        if (fechaFin)    params.push(`fechaFin=${fechaFin}`);
        if (params.length > 0) url += '?' + params.join('&');

        // Llama al backend para descargar el archivo
        const token = getToken();
        const res   = await fetch('/api' + url, {
            headers: { 'Authorization': 'Bearer ' + token },
        });

        if (!res.ok) {
            const data = await res.json().catch(() => ({}));
            throw new Error(data.mensaje || data.message || 'Error al generar reporte');
        }

        // Descargar el archivo
        const blob     = await res.blob();
        const urlBlob  = URL.createObjectURL(blob);
        const link     = document.createElement('a');
        link.href      = urlBlob;
        link.download  = generarNombreArchivo(tipo, fechaInicio, fechaFin);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(urlBlob);

        showToast(`Reporte de ${tipo} descargado correctamente`, 'success');

    } catch (e) {
        showToast('Error al generar reporte: ' + e.message, 'error');
    } finally {
        btn.disabled  = false;
        btn.innerHTML = textoOriginal;
    }
}

/* nombre del archivo con fecha */
function generarNombreArchivo(tipo, fechaInicio, fechaFin) {
    const hoy = new Date().toISOString().split('T')[0];
    if (fechaInicio && fechaFin) {
        return `reporte_${tipo}_${fechaInicio}_${fechaFin}.xlsx`;
    }
    return `reporte_${tipo}_${hoy}.xlsx`;
}

/* limpiar fechaas */
function limpiarFechas() {
    document.getElementById('fechaInicio').value = '';
    document.getElementById('fechaFin').value    = '';
    showToast('Filtro de fechas limpiado', 'info');
}

/* fecha maxima hoy */
function setFechaMax() {
    const hoy = new Date().toISOString().split('T')[0];
    const fi  = document.getElementById('fechaInicio');
    const ff  = document.getElementById('fechaFin');
    if (fi) fi.max = hoy;
    if (ff) ff.max = hoy;

    if (fi) {
        fi.addEventListener('change', () => {
            if (ff) ff.min = fi.value;
        });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    setFechaMax();
});