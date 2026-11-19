package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FacturaExcelService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int FILA_ENCABEZADOS = 6;
    private static final Color MORADO = new Color(101, 67, 136);
    private static final Color MORADO_CLARO = new Color(242, 237, 248);
    private static final Color FILA_ALTERNA = new Color(249, 247, 252);

    public byte[] generar(Factura factura) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = workbook.createSheet("Factura");
            hoja.setDisplayGridlines(false);
            hoja.setZoom(90);

            CellStyle titulo = crearEstilo(workbook, 18, true, IndexedColors.WHITE,
                    MORADO, HorizontalAlignment.LEFT, false);
            CellStyle etiqueta = crearEstilo(workbook, 10, true, IndexedColors.DARK_BLUE,
                    MORADO_CLARO, HorizontalAlignment.LEFT, false);
            CellStyle valor = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    Color.WHITE, HorizontalAlignment.LEFT, true);
            CellStyle encabezado = crearEstilo(workbook, 10, true, IndexedColors.WHITE,
                    MORADO, HorizontalAlignment.CENTER, false);
            CellStyle filaPar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    Color.WHITE, HorizontalAlignment.LEFT, true);
            CellStyle filaImpar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    FILA_ALTERNA, HorizontalAlignment.LEFT, true);
            CellStyle cantidadPar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    Color.WHITE, HorizontalAlignment.CENTER, false);
            CellStyle cantidadImpar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    FILA_ALTERNA, HorizontalAlignment.CENTER, false);
            CellStyle monedaPar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    Color.WHITE, HorizontalAlignment.RIGHT, false);
            CellStyle monedaImpar = crearEstilo(workbook, 10, false, IndexedColors.BLACK,
                    FILA_ALTERNA, HorizontalAlignment.RIGHT, false);
            CellStyle totalEtiqueta = crearEstilo(workbook, 11, true, IndexedColors.WHITE,
                    MORADO, HorizontalAlignment.RIGHT, false);
            CellStyle totalMoneda = crearEstilo(workbook, 11, true, IndexedColors.WHITE,
                    MORADO, HorizontalAlignment.RIGHT, false);
            DataFormat formato = workbook.createDataFormat();
            monedaPar.setDataFormat(formato.getFormat("$#,##0.00"));
            monedaImpar.setDataFormat(formato.getFormat("$#,##0.00"));
            totalMoneda.setDataFormat(formato.getFormat("$#,##0.00"));

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.setHeightInPoints(34);
            for (int columna = 0; columna < 4; columna++) {
                filaTitulo.createCell(columna).setCellStyle(titulo);
            }
            filaTitulo.getCell(0).setCellValue("Factura #" + factura.getId());
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            agregarDatoPareado(hoja, 1, "Fecha", factura.getFecha() == null
                            ? "—" : factura.getFecha().format(FORMATO_FECHA),
                    "Estado", estado(factura), etiqueta, valor);
            agregarDatoExtendido(hoja, 2, "Cliente", nombreCliente(factura), etiqueta, valor);
            agregarDatoExtendido(hoja, 3, "Vendedor", factura.getVendedor() == null
                    ? "—" : factura.getVendedor().getNombre(), etiqueta, valor);
            agregarDatoExtendido(hoja, 4, "Documento", documentoCliente(factura), etiqueta, valor);

            Row encabezados = hoja.createRow(FILA_ENCABEZADOS);
            encabezados.setHeightInPoints(24);
            String[] columnas = {"Cantidad", "Producto", "Precio unitario", "Subtotal"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezados.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(encabezado);
            }

            int indiceFila = FILA_ENCABEZADOS + 1;
            List<DetalleFactura> detalles = factura.getDetalles();
            if (detalles != null) {
                for (int i = 0; i < detalles.size(); i++) {
                    DetalleFactura detalle = detalles.get(i);
                    boolean alterna = i % 2 == 1;
                    Row fila = hoja.createRow(indiceFila++);
                    String nombreProducto = detalle.getProducto() == null
                            ? "Producto no disponible" : detalle.getProducto().getNombre();

                    Cell cantidad = fila.createCell(0);
                    cantidad.setCellValue(detalle.getCantidad());
                    cantidad.setCellStyle(alterna ? cantidadImpar : cantidadPar);

                    Cell producto = fila.createCell(1);
                    producto.setCellValue(nombreProducto == null ? "Producto no disponible" : nombreProducto);
                    producto.setCellStyle(alterna ? filaImpar : filaPar);

                    Cell precio = fila.createCell(2);
                    precio.setCellValue(detalle.getPrecioUnitario());
                    precio.setCellStyle(alterna ? monedaImpar : monedaPar);

                    Cell subtotal = fila.createCell(3);
                    subtotal.setCellValue(detalle.getSubtotal());
                    subtotal.setCellStyle(alterna ? monedaImpar : monedaPar);

                    int lineas = Math.max(1, ((producto.getStringCellValue().length() - 1) / 42) + 1);
                    fila.setHeightInPoints(Math.min(48, Math.max(22, lineas * 16)));
                }
            }

            int filaTotalIndice = indiceFila + 1;
            Row filaTotal = hoja.createRow(filaTotalIndice);
            filaTotal.setHeightInPoints(25);
            for (int columna = 0; columna < 4; columna++) {
                filaTotal.createCell(columna).setCellStyle(columna == 2 ? totalEtiqueta : totalMoneda);
            }
            filaTotal.getCell(2).setCellValue("TOTAL A PAGAR");
            filaTotal.getCell(3).setCellValue(factura.getTotal());

            if (indiceFila > FILA_ENCABEZADOS + 1) {
                hoja.setAutoFilter(new CellRangeAddress(FILA_ENCABEZADOS, indiceFila - 1, 0, 3));
            }
            hoja.createFreezePane(0, FILA_ENCABEZADOS + 1);
            hoja.setColumnWidth(0, 13 * 256);
            hoja.setColumnWidth(1, 44 * 256);
            hoja.setColumnWidth(2, 19 * 256);
            hoja.setColumnWidth(3, 17 * 256);
            hoja.setFitToPage(true);
            hoja.setHorizontallyCenter(true);
            hoja.setRepeatingRows(CellRangeAddress.valueOf("7:7"));
            hoja.setMargin(Sheet.LeftMargin, 0.35);
            hoja.setMargin(Sheet.RightMargin, 0.35);
            hoja.setMargin(Sheet.TopMargin, 0.5);
            hoja.setMargin(Sheet.BottomMargin, 0.5);
            PrintSetup impresion = hoja.getPrintSetup();
            impresion.setLandscape(true);
            impresion.setFitWidth((short) 1);
            impresion.setFitHeight((short) 0);

            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo generar el Excel de la factura.", exception);
        }
    }

    private XSSFCellStyle crearEstilo(Workbook workbook, int tamano, boolean negrita,
                                      IndexedColors colorFuente, Color fondo,
                                      HorizontalAlignment alineacion, boolean ajustarTexto) {
        Font fuente = workbook.createFont();
        fuente.setBold(negrita);
        fuente.setFontHeightInPoints((short) tamano);
        fuente.setColor(colorFuente.getIndex());

        XSSFCellStyle estilo = (XSSFCellStyle) workbook.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(alineacion);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        estilo.setWrapText(ajustarTexto);
        estilo.setBorderBottom(BorderStyle.THIN);
        estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        if (fondo != null) {
            estilo.setFillForegroundColor(new XSSFColor(fondo, new DefaultIndexedColorMap()));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return estilo;
    }

    private void agregarDatoPareado(Sheet hoja, int indiceFila, String etiquetaIzquierda,
                                    String valorIzquierda, String etiquetaDerecha, String valorDerecha,
                                    CellStyle estiloEtiqueta, CellStyle estiloValor) {
        Row fila = hoja.createRow(indiceFila);
        fila.setHeightInPoints(23);
        agregarCelda(fila, 0, etiquetaIzquierda, estiloEtiqueta);
        agregarCelda(fila, 1, valorIzquierda, estiloValor);
        agregarCelda(fila, 2, etiquetaDerecha, estiloEtiqueta);
        agregarCelda(fila, 3, valorDerecha, estiloValor);
    }

    private void agregarDatoExtendido(Sheet hoja, int indiceFila, String etiqueta, String valor,
                                      CellStyle estiloEtiqueta, CellStyle estiloValor) {
        Row fila = hoja.createRow(indiceFila);
        fila.setHeightInPoints(23);
        agregarCelda(fila, 0, etiqueta, estiloEtiqueta);
        for (int columna = 1; columna < 4; columna++) {
            fila.createCell(columna).setCellStyle(estiloValor);
        }
        fila.getCell(1).setCellValue(valor == null || valor.isBlank() ? "—" : valor);
        hoja.addMergedRegion(new CellRangeAddress(indiceFila, indiceFila, 1, 3));
    }

    private void agregarCelda(Row fila, int columna, String texto, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(texto == null || texto.isBlank() ? "—" : texto);
        celda.setCellStyle(estilo);
    }

    private String nombreCliente(Factura factura) {
        if (factura.getCliente() == null || factura.getCliente().getNombre() == null
                || factura.getCliente().getNombre().isBlank()) {
            return "Consumidor final";
        }
        return factura.getCliente().getNombre();
    }

    private String documentoCliente(Factura factura) {
        return factura.getCliente() == null || factura.getCliente().getDocumento() == null
                || factura.getCliente().getDocumento().isBlank()
                ? "—" : factura.getCliente().getDocumento();
    }

    private String estado(Factura factura) {
        if (factura.isAnulada()) return "Anulada";
        if (factura.isEntregada()) return "Entregada";
        return "Pendiente";
    }
}
