package com.sportspace.service;

import com.sportspace.entity.*;
import com.sportspace.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final UsuarioRepository  usuarioRepository;
    private final ReservaRepository  reservaRepository;
    private final PagoRepository     pagoRepository;
    private final CanchaRepository   canchaRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // USUARIOS

    public byte[] reporteUsuarios(LocalDate inicio, LocalDate fin) throws Exception {
        List<Usuario> lista = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Rol.CLIENTE || u.getRol() == Rol.PROPIETARIO)
                .filter(u -> filtrarPorFecha(u.getCreatedAt() != null
                        ? u.getCreatedAt().toLocalDate() : null, inicio, fin))
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Usuarios");
            String[] headers = { "ID", "Nombres", "Apellidos", "Email",
                    "Nro. Documento", "Telefono", "Rol", "Distrito",
                    "Activo", "Fecha Registro" };

            escribirEncabezado(wb, sheet, headers, "Reporte de Usuarios");

            int fila = 2;
            for (Usuario u : lista) {
                Row row = sheet.createRow(fila++);
                setCelda(row, 0, u.getId());
                setCelda(row, 1, u.getNombres());
                setCelda(row, 2, u.getApellidos());
                setCelda(row, 3, u.getEmail());
                setCelda(row, 4, u.getNumeroDocumento());
                setCelda(row, 5, u.getTelefono());
                setCelda(row, 6, u.getRol() != null ? u.getRol().name() : "");
                setCelda(row, 7, u.getDistrito());
                setCelda(row, 8, Boolean.TRUE.equals(u.getActivo()) ? "SI" : "NO");
                setCelda(row, 9, u.getCreatedAt() != null
                        ? u.getCreatedAt().toLocalDate().format(FMT) : "");
            }

            autosize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // RESERVAS

    public byte[] reporteReservas(LocalDate inicio, LocalDate fin) throws Exception {
        List<Reserva> lista = reservaRepository.findAll().stream()
                .filter(r -> filtrarPorFecha(r.getFecha(), inicio, fin))
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Reservas");
            String[] headers = { "ID", "Cliente", "Email Cliente",
                    "Cancha", "Deporte", "Distrito",
                    "Fecha", "Hora Inicio", "Hora Fin",
                    "Total (S/)", "Estado" };

            escribirEncabezado(wb, sheet, headers, "Reporte de Reservas");

            int fila = 2;
            for (Reserva r : lista) {
                Row row = sheet.createRow(fila++);
                setCelda(row, 0,  r.getId());
                setCelda(row, 1,  r.getCliente().getNombres() + " " + r.getCliente().getApellidos());
                setCelda(row, 2,  r.getCliente().getEmail());
                setCelda(row, 3,  r.getCancha().getNombre());
                setCelda(row, 4,  r.getCancha().getDeporte());
                setCelda(row, 5,  r.getCancha().getDistrito());
                setCelda(row, 6,  r.getFecha().format(FMT));
                setCelda(row, 7,  r.getHoraInicio().toString());
                setCelda(row, 8,  r.getHoraFin().toString());
                setCeldaMonto(row, 9, r.getTotal());
                setCelda(row, 10, r.getEstado() != null ? r.getEstado().name() : "");
            }

            autosize(sheet, headers.length);
            return toBytes(wb);
        }
    }


    // INGRESOS (solo pagos COMPLETADO con reserva NO cancelada)


    public byte[] reporteIngresos(LocalDate inicio, LocalDate fin) throws Exception {
        List<Pago> lista = pagoRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(p -> filtrarPorFecha(
                        p.getFechaPago() != null
                                ? p.getFechaPago().toLocalDate()
                                : (p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : null),
                        inicio, fin))
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Ingresos");
            String[] headers = { "ID Pago", "ID Reserva", "Cliente", "Email",
                    "Cancha", "Deporte", "Fecha Reserva",
                    "Metodo Pago", "Monto (S/)", "Estado Pago", "Estado Reserva" };

            escribirEncabezado(wb, sheet, headers, "Reporte de Ingresos");

            int fila = 2;
            BigDecimal totalIngresos = BigDecimal.ZERO;

            for (Pago p : lista) {
                Row row = sheet.createRow(fila++);
                Reserva r  = p.getReserva();
                Cancha  ca = r.getCancha();

                // Estado para el reporte (igual que en el dashboard)
                boolean cancelada = r.getEstado() == EstadoReserva.CANCELADA;
                String estadoPago;
                if (p.getEstado() == Pago.EstadoPago.REEMBOLSADO) {
                    estadoPago = "REEMBOLSADO";
                } else if (p.getEstado() == Pago.EstadoPago.COMPLETADO && cancelada) {
                    estadoPago = "EN REEMBOLSO";
                } else if (p.getEstado() == Pago.EstadoPago.COMPLETADO) {
                    estadoPago = "APROBADO";
                    totalIngresos = totalIngresos.add(p.getMonto());
                } else {
                    estadoPago = p.getEstado().name();
                }

                setCelda(row, 0,  p.getId());
                setCelda(row, 1,  r.getId());
                setCelda(row, 2,  r.getCliente().getNombres() + " " + r.getCliente().getApellidos());
                setCelda(row, 3,  r.getCliente().getEmail());
                setCelda(row, 4,  ca.getNombre());
                setCelda(row, 5,  ca.getDeporte());
                setCelda(row, 6,  r.getFecha().format(FMT));
                setCelda(row, 7,  p.getMetodo() != null ? p.getMetodo().name() : "—");
                setCeldaMonto(row, 8, p.getMonto());
                setCelda(row, 9,  estadoPago);
                setCelda(row, 10, r.getEstado() != null ? r.getEstado().name() : "");
            }

            // Fila de total
            Row rowTotal = sheet.createRow(fila + 1);
            setCelda(rowTotal, 7, "TOTAL APROBADO:");
            setCeldaMonto(rowTotal, 8, totalIngresos);

            autosize(sheet, headers.length);
            return toBytes(wb);
        }
    }


    // CANCHAS

    public byte[] reporteCanchas(LocalDate inicio, LocalDate fin) throws Exception {
        List<Cancha> lista = canchaRepository.findAll();

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Canchas");
            String[] headers = { "ID", "Nombre", "Deporte", "Propietario",
                    "Precio/Hora (S/)", "Direccion", "Distrito",
                    "Capacidad", "Activa",
                    "Total Reservas", "Ingresos Totales (S/)" };

            escribirEncabezado(wb, sheet, headers, "Reporte de Canchas");

            int fila = 2;
            for (Cancha ca : lista) {
                // Calcular reservas e ingresos de esta cancha en el periodo
                List<Reserva> reservasCa = reservaRepository.findAll().stream()
                        .filter(r -> r.getCancha().getId().equals(ca.getId()))
                        .filter(r -> filtrarPorFecha(r.getFecha(), inicio, fin))
                        .collect(Collectors.toList());

                long totalReservas = reservasCa.size();
                BigDecimal ingresos = reservasCa.stream()
                        .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA)
                        .map(Reserva::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Row row = sheet.createRow(fila++);
                setCelda(row, 0, ca.getId());
                setCelda(row, 1, ca.getNombre());
                setCelda(row, 2, ca.getDeporte());
                setCelda(row, 3, ca.getPropietario() != null
                        ? ca.getPropietario().getNombres() + " " + ca.getPropietario().getApellidos() : "");
                setCeldaMonto(row, 4, ca.getPrecioHora());
                setCelda(row, 5, ca.getDireccion());
                setCelda(row, 6, ca.getDistrito());
                setCelda(row, 7, ca.getCapacidad());
                setCelda(row, 8, Boolean.TRUE.equals(ca.getActiva()) ? "SI" : "NO");
                setCelda(row, 9, totalReservas);
                setCeldaMonto(row, 10, ingresos);
            }

            autosize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // PROPIETARIOS

    public byte[] reportePropietarios(LocalDate inicio, LocalDate fin) throws Exception {
        List<Usuario> propietarios = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() == Rol.PROPIETARIO)
                .collect(Collectors.toList());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Propietarios");
            String[] headers = { "ID", "Nombres", "Apellidos", "Email",
                    "Telefono", "Nro. Documento", "Distrito",
                    "Total Canchas", "Canchas Activas",
                    "Total Reservas", "Ingresos Periodo (S/)" };

            escribirEncabezado(wb, sheet, headers, "Reporte de Propietarios");

            int fila = 2;
            for (Usuario prop : propietarios) {
                List<Cancha> canchas = canchaRepository.findAll().stream()
                        .filter(c -> c.getPropietario() != null
                                && c.getPropietario().getId().equals(prop.getId()))
                        .collect(Collectors.toList());

                long totalCanchas   = canchas.size();
                long canchasActivas = canchas.stream().filter(c -> Boolean.TRUE.equals(c.getActiva())).count();

                List<Long> canchaIds = canchas.stream().map(Cancha::getId).collect(Collectors.toList());

                List<Reserva> reservas = reservaRepository.findAll().stream()
                        .filter(r -> canchaIds.contains(r.getCancha().getId()))
                        .filter(r -> filtrarPorFecha(r.getFecha(), inicio, fin))
                        .collect(Collectors.toList());

                long totalReservas = reservas.size();
                BigDecimal ingresos = reservas.stream()
                        .filter(r -> r.getEstado() == EstadoReserva.CONFIRMADA)
                        .map(Reserva::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                Row row = sheet.createRow(fila++);
                setCelda(row, 0,  prop.getId());
                setCelda(row, 1,  prop.getNombres());
                setCelda(row, 2,  prop.getApellidos());
                setCelda(row, 3,  prop.getEmail());
                setCelda(row, 4,  prop.getTelefono());
                setCelda(row, 5,  prop.getNumeroDocumento());
                setCelda(row, 6,  prop.getDistrito());
                setCelda(row, 7,  totalCanchas);
                setCelda(row, 8,  canchasActivas);
                setCelda(row, 9,  totalReservas);
                setCeldaMonto(row, 10, ingresos);
            }

            autosize(sheet, headers.length);
            return toBytes(wb);
        }
    }

    // HELPERS

    private void escribirEncabezado(XSSFWorkbook wb, Sheet sheet,
                                    String[] headers, String titulo) {
        // Fila 0: título
        Row rowTitulo = sheet.createRow(0);
        Cell cellTitulo = rowTitulo.createCell(0);
        cellTitulo.setCellValue(titulo + "  —  SportSpace");

        CellStyle styleTitulo = wb.createCellStyle();
        Font fontTitulo = wb.createFont();
        fontTitulo.setBold(true);
        fontTitulo.setFontHeightInPoints((short) 13);
        styleTitulo.setFont(fontTitulo);
        cellTitulo.setCellStyle(styleTitulo);

        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

        // Fila 1: headers
        Row rowHeader = sheet.createRow(1);
        CellStyle styleHeader = wb.createCellStyle();
        Font fontHeader = wb.createFont();
        fontHeader.setBold(true);
        styleHeader.setFont(fontHeader);
        styleHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        styleHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styleHeader.setBorderBottom(BorderStyle.THIN);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = rowHeader.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styleHeader);
        }
    }

    private void setCelda(Row row, int col, Object val) {
        Cell cell = row.createCell(col);
        if (val == null)          cell.setCellValue("");
        else if (val instanceof Long)    cell.setCellValue((Long) val);
        else if (val instanceof Integer) cell.setCellValue((Integer) val);
        else if (val instanceof Number)  cell.setCellValue(((Number) val).doubleValue());
        else                             cell.setCellValue(val.toString());
    }

    private void setCeldaMonto(Row row, int col, BigDecimal val) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val.doubleValue() : 0.0);
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            // Ancho mínimo 3000 (unidades POI)
            if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
        }
    }

    private byte[] toBytes(XSSFWorkbook wb) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    /**
     * Retorna true si la fecha está dentro del rango (o si no hay rango definido).
     */
    private boolean filtrarPorFecha(LocalDate fecha, LocalDate inicio, LocalDate fin) {
        if (fecha == null) return true;
        if (inicio != null && fecha.isBefore(inicio)) return false;
        if (fin    != null && fecha.isAfter(fin))     return false;
        return true;
    }
}