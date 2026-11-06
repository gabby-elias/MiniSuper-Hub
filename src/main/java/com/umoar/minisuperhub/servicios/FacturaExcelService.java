package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.dto.ProductoVendidoDTO;
import com.umoar.minisuperhub.dto.VendedorResumenDTO;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FacturaExcelService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_SOLO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int FILA_ENCABEZADOS = 6;

    public byte[] generar(Factura factura) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet("Factura");
            CellStyle titulo = estiloNegrita(workbook, (short) 16);
            CellStyle negrita = estiloNegrita(workbook, (short) 11);
            CellStyle moneda = workbook.createCellStyle();
            DataFormat formato = workbook.createDataFormat();
            moneda.setDataFormat(formato.getFormat("$#,##0.00"));

            Row filaTitulo = hoja.createRow(0);
            Cell celdaTitulo = filaTitulo.createCell(0);
            celdaTitulo.setCellValue("Factura #" + factura.getId());
            celdaTitulo.setCellStyle(titulo);

            agregarDato(hoja, 1, "Fecha", factura.getFecha() == null
                    ? "—" : factura.getFecha().format(FORMATO_FECHA));
            agregarDato(hoja, 2, "Cliente", nombreCliente(factura));
            agregarDato(hoja, 3, "Vendedor", factura.getVendedor() == null
                    ? "—" : factura.getVendedor().getNombre());
            agregarDato(hoja, 4, "Estado", estado(factura));

            Row encabezados = hoja.createRow(FILA_ENCABEZADOS);
            String[] columnas = {"Cantidad", "Producto", "Precio unitario", "Subtotal"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezados.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(negrita);
            }

            int indiceFila = FILA_ENCABEZADOS + 1;
            List<DetalleFactura> detalles = factura.getDetalles();
            if (detalles != null) {
                for (DetalleFactura detalle : detalles) {
                    Row fila = hoja.createRow(indiceFila++);
                    fila.createCell(0).setCellValue(detalle.getCantidad());
                    fila.createCell(1).setCellValue(detalle.getProducto() == null
                            ? "Producto no disponible" : detalle.getProducto().getNombre());
                    Cell precio = fila.createCell(2);
                    precio.setCellValue(detalle.getPrecioUnitario());
                    precio.setCellStyle(moneda);
                    Cell subtotal = fila.createCell(3);
                    subtotal.setCellValue(detalle.getSubtotal());
                    subtotal.setCellStyle(moneda);
                }
            }

            Row filaTotal = hoja.createRow(indiceFila + 1);
            Cell etiquetaTotal = filaTotal.createCell(2);
            etiquetaTotal.setCellValue("Total");
            etiquetaTotal.setCellStyle(negrita);
            Cell total = filaTotal.createCell(3);
            total.setCellValue(factura.getTotal());
            total.setCellStyle(estiloMonedaNegrita(workbook, moneda));

            hoja.createFreezePane(0, FILA_ENCABEZADOS + 1);
            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.setColumnWidth(1, Math.min(hoja.getColumnWidth(1), 60 * 256));

            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de la factura.", exception);
        }
    }

    public byte[] generarReporteVentasPeriodo(LocalDate fechaInicio,
                                             LocalDate fechaFin,
                                             List<Factura> facturas,
                                             double totalVentas,
                                             double ticketPromedio,
                                             long facturasVigentes,
                                             long facturasAnuladas,
                                             List<ProductoVendidoDTO> topProductos,
                                             List<VendedorResumenDTO> ventasPorVendedor) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CellStyle titulo = estiloNegrita(workbook, (short) 16);
            CellStyle subtitulo = estiloNegrita(workbook, (short) 13);
            CellStyle negrita = estiloNegrita(workbook, (short) 11);
            CellStyle moneda = workbook.createCellStyle();
            DataFormat formato = workbook.createDataFormat();
            moneda.setDataFormat(formato.getFormat("$#,##0.00"));
            CellStyle monedaNegrita = estiloMonedaNegrita(workbook, moneda);

            // HOJA 1: RESUMEN Y CUADRE
            Sheet hoja1 = workbook.createSheet("Resumen Ejecutivo");

            Row r0 = hoja1.createRow(0);
            Cell cTitulo = r0.createCell(0);
            cTitulo.setCellValue("Minisúper Hub · Reporte de Ventas y Cuadre");
            cTitulo.setCellStyle(titulo);

            Row r1 = hoja1.createRow(1);
            r1.createCell(0).setCellValue("Período evaluado: " +
                    (fechaInicio != null ? fechaInicio.format(FORMATO_SOLO_FECHA) : "Inicio") +
                    " hasta " +
                    (fechaFin != null ? fechaFin.format(FORMATO_SOLO_FECHA) : "Hoy"));

            // KPIs
            hoja1.createRow(3).createCell(0).setCellValue("Métricas Clave del Período");
            hoja1.getRow(3).getCell(0).setCellStyle(subtitulo);

            agregarDatoMoneda(hoja1, 4, "Total Ventas Vigentes", totalVentas, monedaNegrita);
            agregarDatoMoneda(hoja1, 5, "Ticket Promedio", ticketPromedio, monedaNegrita);
            agregarDato(hoja1, 6, "Facturas Vigentes", String.valueOf(facturasVigentes));
            agregarDato(hoja1, 7, "Facturas Anuladas", String.valueOf(facturasAnuladas));

            int curRow = 9;
            // Ventas por Vendedor
            Row rVendedorHeader = hoja1.createRow(curRow++);
            rVendedorHeader.createCell(0).setCellValue("Desglose por Vendedor / Cajero");
            rVendedorHeader.getCell(0).setCellStyle(subtitulo);

            Row rVendedorCols = hoja1.createRow(curRow++);
            rVendedorCols.createCell(0).setCellValue("Vendedor");
            rVendedorCols.getCell(0).setCellStyle(negrita);
            rVendedorCols.createCell(1).setCellValue("N.º Facturas");
            rVendedorCols.getCell(1).setCellStyle(negrita);
            rVendedorCols.createCell(2).setCellValue("Total Vendido");
            rVendedorCols.getCell(2).setCellStyle(negrita);

            if (ventasPorVendedor != null && !ventasPorVendedor.isEmpty()) {
                for (VendedorResumenDTO v : ventasPorVendedor) {
                    Row f = hoja1.createRow(curRow++);
                    f.createCell(0).setCellValue(v.getNombre());
                    f.createCell(1).setCellValue(v.getCantidadFacturas());
                    Cell cv = f.createCell(2);
                    cv.setCellValue(v.getTotalVendido());
                    cv.setCellStyle(moneda);
                }
            } else {
                hoja1.createRow(curRow++).createCell(0).setCellValue("Sin ventas en el período.");
            }

            curRow += 2;
            // Top Productos
            Row rTopHeader = hoja1.createRow(curRow++);
            rTopHeader.createCell(0).setCellValue("Top Productos con Mayor Rotación");
            rTopHeader.getCell(0).setCellStyle(subtitulo);

            Row rTopCols = hoja1.createRow(curRow++);
            rTopCols.createCell(0).setCellValue("N.º");
            rTopCols.getCell(0).setCellStyle(negrita);
            rTopCols.createCell(1).setCellValue("Producto");
            rTopCols.getCell(1).setCellStyle(negrita);
            rTopCols.createCell(2).setCellValue("Unidades Vendidas");
            rTopCols.getCell(2).setCellStyle(negrita);
            rTopCols.createCell(3).setCellValue("Total Recaudado");
            rTopCols.getCell(3).setCellStyle(negrita);

            if (topProductos != null && !topProductos.isEmpty()) {
                int rank = 1;
                for (ProductoVendidoDTO p : topProductos) {
                    Row f = hoja1.createRow(curRow++);
                    f.createCell(0).setCellValue(rank++);
                    f.createCell(1).setCellValue(p.getNombre());
                    f.createCell(2).setCellValue(p.getCantidad());
                    Cell cp = f.createCell(3);
                    cp.setCellValue(p.getTotalRecaudado());
                    cp.setCellStyle(moneda);
                }
            } else {
                hoja1.createRow(curRow++).createCell(0).setCellValue("Sin productos vendidos en el período.");
            }

            for (int i = 0; i < 4; i++) {
                hoja1.autoSizeColumn(i);
            }

            // HOJA 2: DETALLE DE FACTURAS
            Sheet hoja2 = workbook.createSheet("Detalle de Facturas");
            Row rFTitle = hoja2.createRow(0);
            rFTitle.createCell(0).setCellValue("Listado de Facturas Emitidas");
            rFTitle.getCell(0).setCellStyle(titulo);

            Row rFCols = hoja2.createRow(2);
            String[] colsFacturas = {"N.º", "Fecha", "Cliente", "Vendedor", "Estado", "Total"};
            for (int i = 0; i < colsFacturas.length; i++) {
                Cell c = rFCols.createCell(i);
                c.setCellValue(colsFacturas[i]);
                c.setCellStyle(negrita);
            }

            int rowIdx = 3;
            if (facturas != null && !facturas.isEmpty()) {
                for (Factura f : facturas) {
                    Row row = hoja2.createRow(rowIdx++);
                    row.createCell(0).setCellValue(f.getId());
                    row.createCell(1).setCellValue(f.getFecha() != null ? f.getFecha().format(FORMATO_FECHA) : "—");
                    row.createCell(2).setCellValue(nombreCliente(f));
                    row.createCell(3).setCellValue(f.getVendedor() != null ? f.getVendedor().getNombre() : "—");
                    row.createCell(4).setCellValue(estado(f));
                    Cell cTot = row.createCell(5);
                    cTot.setCellValue(f.getTotal());
                    cTot.setCellStyle(moneda);
                }
            } else {
                hoja2.createRow(rowIdx++).createCell(0).setCellValue("No hay facturas en este rango.");
            }

            for (int i = 0; i < colsFacturas.length; i++) {
                hoja2.autoSizeColumn(i);
            }

            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Error al generar el reporte en Excel.", ex);
        }
    }

    private void agregarDatoMoneda(Sheet hoja, int indiceFila, String etiqueta, double valor, CellStyle estilo) {
        Row fila = hoja.createRow(indiceFila);
        fila.createCell(0).setCellValue(etiqueta);
        Cell celda = fila.createCell(1);
        celda.setCellValue(valor);
        celda.setCellStyle(estilo);
    }

    private CellStyle estiloNegrita(Workbook workbook, short tamano) {
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints(tamano);
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }

    private CellStyle estiloMonedaNegrita(Workbook workbook, CellStyle moneda) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.cloneStyleFrom(moneda);
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        return estilo;
    }

    private void agregarDato(Sheet hoja, int indiceFila, String etiqueta, String valor) {
        Row fila = hoja.createRow(indiceFila);
        fila.createCell(0).setCellValue(etiqueta);
        fila.createCell(1).setCellValue(valor == null || valor.isBlank() ? "—" : valor);
    }

    private String nombreCliente(Factura factura) {
        if (factura.getCliente() == null || factura.getCliente().getNombre() == null
                || factura.getCliente().getNombre().isBlank()) {
            return "Consumidor final";
        }
        return factura.getCliente().getNombre();
    }

    private String estado(Factura factura) {
        if (factura.isAnulada()) return "Anulada";
        if (factura.isEntregada()) return "Entregada";
        return "Pendiente";
    }
}
