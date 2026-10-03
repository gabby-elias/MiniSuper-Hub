package com.umoar.minisuperhub.servicios;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class FacturaPdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(30, 45, 65));
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font ENCABEZADO_TABLA = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombreMinisuper;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccionMinisuper;

    @Value("${app.minisuper.telefono:Teléfono del minisúper}")
    private String telefonoMinisuper;

    public byte[] generar(Factura factura) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(PageSize.LETTER, 42, 42, 42, 42);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph titulo = new Paragraph(nombreMinisuper, TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);
            Paragraph contacto = new Paragraph(direccionMinisuper + " · Tel. " + telefonoMinisuper, NORMAL);
            contacto.setAlignment(Element.ALIGN_CENTER);
            documento.add(contacto);
            documento.add(new Paragraph(" "));

            Paragraph numero = new Paragraph("FACTURA #" + factura.getId(), new Font(Font.HELVETICA, 14, Font.BOLD));
            numero.setAlignment(Element.ALIGN_RIGHT);
            documento.add(numero);
            documento.add(new Paragraph("Fecha: " + (factura.getFecha() == null
                    ? "—" : factura.getFecha().format(FORMATO_FECHA)), NORMAL));
            documento.add(new Paragraph("Cliente: " + (factura.getCliente() == null
                    || factura.getCliente().getNombre() == null
                    || factura.getCliente().getNombre().isBlank()
                    ? "Consumidor final" : factura.getCliente().getNombre()), NORMAL));
            documento.add(new Paragraph("Atendió: " + (factura.getVendedor() == null
                    ? "—" : factura.getVendedor().getNombre()), NORMAL));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{1, 4, 1.5f, 1.5f});
            tabla.setWidthPercentage(100);
            agregarCeldaEncabezado(tabla, "Cantidad");
            agregarCeldaEncabezado(tabla, "Descripción");
            agregarCeldaEncabezado(tabla, "Precio unitario");
            agregarCeldaEncabezado(tabla, "Total por línea");

            BigDecimal subtotal = BigDecimal.ZERO;
            if (factura.getDetalles() != null) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    BigDecimal totalLinea = BigDecimal.valueOf(detalle.getSubtotal()).setScale(2, RoundingMode.HALF_UP);
                    subtotal = subtotal.add(totalLinea);
                    tabla.addCell(celda(String.valueOf(detalle.getCantidad()), Element.ALIGN_RIGHT));
                    tabla.addCell(celda(detalle.getProducto() == null ? "Producto eliminado"
                            : detalle.getProducto().getNombre(), Element.ALIGN_LEFT));
                    tabla.addCell(celda(moneda(detalle.getPrecioUnitario()), Element.ALIGN_RIGHT));
                    tabla.addCell(celda(moneda(totalLinea.doubleValue()), Element.ALIGN_RIGHT));
                }
            }
            documento.add(tabla);

            BigDecimal total = BigDecimal.valueOf(factura.getTotal()).setScale(2, RoundingMode.HALF_UP);
            documento.add(new Paragraph(" "));
            PdfPTable totales = new PdfPTable(new float[]{3, 1});
            totales.setWidthPercentage(42);
            totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
            agregarTotal(totales, "Subtotal", subtotal);
            agregarTotal(totales, "Total", total);
            documento.add(totales);
            documento.add(new Paragraph("Total en letras: " + totalEnLetras(total), NORMAL));
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura.", exception);
        }
    }

    private void agregarCeldaEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, ENCABEZADO_TABLA));
        celda.setBackgroundColor(new Color(30, 45, 65));
        celda.setPadding(7);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private PdfPCell celda(String texto, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "—" : texto, NORMAL));
        celda.setPadding(6);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private void agregarTotal(PdfPTable tabla, String etiqueta, BigDecimal cantidad) {
        tabla.addCell(celda(etiqueta, Element.ALIGN_RIGHT));
        PdfPCell importe = celda(moneda(cantidad.doubleValue()), Element.ALIGN_RIGHT);
        importe.setPhrase(new Phrase(moneda(cantidad.doubleValue()), new Font(Font.HELVETICA, 10, Font.BOLD)));
        tabla.addCell(importe);
    }

    private String moneda(double cantidad) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.US);
        formato.setMinimumFractionDigits(2);
        formato.setMaximumFractionDigits(2);
        return "$" + formato.format(cantidad);
    }

    private String totalEnLetras(BigDecimal total) {
        long unidades = total.setScale(2, RoundingMode.HALF_UP).longValue();
        int centavos = total.remainder(BigDecimal.ONE).abs().movePointRight(2).intValue();
        String letras = numeroEnLetras(unidades);
        if (letras.endsWith("veintiuno")) {
            letras = letras.substring(0, letras.length() - "veintiuno".length()) + "veintiún";
        } else if (letras.endsWith("uno")) {
            letras = letras.substring(0, letras.length() - 3) + "un";
        }
        return letras + (unidades == 1 ? " dólar" : " dólares")
                + String.format(Locale.ROOT, " con %02d/100", centavos);
    }

    private String numeroEnLetras(long numero) {
        if (numero == 0) return "cero";
        if (numero < 0) return "menos " + numeroEnLetras(-numero);
        if (numero < 30) {
            String[] especiales = {"", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve",
                    "diez", "once", "doce", "trece", "catorce", "quince", "dieciséis", "diecisiete",
                    "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidós", "veintitrés",
                    "veinticuatro", "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"};
            return especiales[(int) numero];
        }
        if (numero < 100) {
            String[] decenas = {"", "", "", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa"};
            long resto = numero % 10;
            return decenas[(int) (numero / 10)] + (resto == 0 ? "" : " y " + numeroEnLetras(resto));
        }
        if (numero == 100) return "cien";
        if (numero < 1000) {
            String[] centenas = {"", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos",
                    "seiscientos", "setecientos", "ochocientos", "novecientos"};
            return centenas[(int) (numero / 100)] + (numero % 100 == 0 ? "" : " " + numeroEnLetras(numero % 100));
        }
        if (numero < 1_000_000) {
            long miles = numero / 1000;
            return (miles == 1 ? "mil" : numeroEnLetras(miles) + " mil")
                    + (numero % 1000 == 0 ? "" : " " + numeroEnLetras(numero % 1000));
        }
        if (numero < 1_000_000_000) {
            long millones = numero / 1_000_000;
            String prefijo = millones == 1 ? "un millón" : numeroEnLetras(millones) + " millones";
            return prefijo + (numero % 1_000_000 == 0 ? "" : " " + numeroEnLetras(numero % 1_000_000));
        }
        return Long.toString(numero);
    }
}
