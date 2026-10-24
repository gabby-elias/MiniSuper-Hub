package com.umoar.minisuperhub.servicios;

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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FacturaExcelService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
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
