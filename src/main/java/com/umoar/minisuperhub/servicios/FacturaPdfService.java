package com.umoar.minisuperhub.servicios;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
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

    private static final float ANCHO_TICKET = 226.77f;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font NOMBRE_TIENDA = new Font(Font.HELVETICA, 17, Font.BOLD);
    private static final Font TITULO = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 8);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 7);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font SEPARADOR = new Font(Font.COURIER, 7, Font.NORMAL, Color.DARK_GRAY);

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombreMinisuper;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccionMinisuper;

    @Value("${app.minisuper.telefono:Teléfono del minisúper}")
    private String telefonoMinisuper;

    public byte[] generar(Factura factura) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Document documento = new Document(new Rectangle(ANCHO_TICKET, calcularAltura(factura)), 16, 16, 16, 16);
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarParrafo(documento, nombreMinisuper, NOMBRE_TIENDA, Element.ALIGN_CENTER, 0, 1);
            agregarParrafo(documento, direccionMinisuper, PEQUENA, Element.ALIGN_CENTER, 0, 1);
            agregarParrafo(documento, "Tel. " + telefonoMinisuper, PEQUENA, Element.ALIGN_CENTER, 0, 4);
            agregarParrafo(documento, "TICKET DE COMPRA", TITULO, Element.ALIGN_CENTER, 0, 3);
            agregarSeparador(documento);

            agregarParrafo(documento, "Factura: #" + factura.getId(), NEGRITA, Element.ALIGN_LEFT, 0, 2);
            agregarParrafo(documento, "Fecha: " + (factura.getFecha() == null
                    ? "—" : factura.getFecha().format(FORMATO_FECHA)), NORMAL, Element.ALIGN_LEFT, 0, 2);
            agregarParrafo(documento, "Cliente: " + nombreCliente(factura), NORMAL, Element.ALIGN_LEFT, 0, 2);
            if (factura.getCliente() != null && tieneTexto(factura.getCliente().getDocumento())) {
                agregarParrafo(documento, "Documento: " + factura.getCliente().getDocumento(), NORMAL, Element.ALIGN_LEFT, 0, 2);
            }
            if (factura.getCliente() != null && tieneTexto(factura.getCliente().getDireccion())) {
                agregarParrafo(documento, "Dirección: " + factura.getCliente().getDireccion(), NORMAL, Element.ALIGN_LEFT, 0, 2);
            }
            agregarParrafo(documento, "Atendió: " + (factura.getVendedor() == null
                    ? "—" : factura.getVendedor().getNombre()), NORMAL, Element.ALIGN_LEFT, 0, 3);
            agregarSeparador(documento);

            PdfPTable productos = new PdfPTable(new float[]{3.3f, 0.55f, 1.05f, 1.15f});
            productos.setWidthPercentage(100);
            productos.setWidths(new float[]{3.3f, 0.55f, 1.05f, 1.15f});
            productos.setSpacingBefore(2);
            productos.setSpacingAfter(2);
            agregarCelda(productos, "Descripción", NEGRITA, Element.ALIGN_LEFT);
            agregarCelda(productos, "Cant.", NEGRITA, Element.ALIGN_CENTER);
            agregarCelda(productos, "Precio", NEGRITA, Element.ALIGN_RIGHT);
            agregarCelda(productos, "Total", NEGRITA, Element.ALIGN_RIGHT);

            if (factura.getDetalles() != null) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    agregarCelda(productos, detalle.getProducto() == null
                            ? "Producto no disponible" : detalle.getProducto().getNombre(), NORMAL, Element.ALIGN_LEFT);
                    agregarCelda(productos, String.valueOf(detalle.getCantidad()), PEQUENA, Element.ALIGN_CENTER);
                    agregarCelda(productos, moneda(detalle.getPrecioUnitario()), PEQUENA, Element.ALIGN_RIGHT);
                    agregarCelda(productos, moneda(detalle.getSubtotal()), NORMAL, Element.ALIGN_RIGHT);
                }
            }
            documento.add(productos);
            agregarSeparador(documento);

            BigDecimal subtotal = factura.getDetalles() == null ? BigDecimal.ZERO : factura.getDetalles().stream()
                    .map(detalle -> BigDecimal.valueOf(detalle.getSubtotal()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = BigDecimal.valueOf(factura.getTotal()).setScale(2, RoundingMode.HALF_UP);
            PdfPTable totales = new PdfPTable(new float[]{1, 1});
            totales.setWidthPercentage(100);
            agregarCelda(totales, "Subtotal:", NORMAL, Element.ALIGN_RIGHT);
            agregarCelda(totales, moneda(subtotal.doubleValue()), NORMAL, Element.ALIGN_RIGHT);
            agregarCelda(totales, "Total:", NEGRITA, Element.ALIGN_RIGHT);
            agregarCelda(totales, moneda(total.doubleValue()), NEGRITA, Element.ALIGN_RIGHT);
            documento.add(totales);

            agregarParrafo(documento, "Total en letras: " + totalEnLetras(total), PEQUENA, Element.ALIGN_LEFT, 5, 0);
            agregarSeparador(documento);
            agregarParrafo(documento, "¡Gracias por su compra!", TITULO, Element.ALIGN_CENTER, 3, 0);
            documento.close();
            return salida.toByteArray();
        } catch (DocumentException | java.io.IOException exception) {
            throw new IllegalStateException("No se pudo generar el PDF de la factura.", exception);
        }
    }

    private float calcularAltura(Factura factura) {
        float altura = 350;
        if (factura.getDetalles() != null) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                String nombre = detalle.getProducto() == null ? "Producto no disponible" : detalle.getProducto().getNombre();
                altura += 28 + Math.max(0, nombre == null ? 0 : nombre.length() - 20) / 24f * 9;
            }
        }
        if (factura.getCliente() != null) {
            if (tieneTexto(factura.getCliente().getDocumento())) altura += 12;
            if (tieneTexto(factura.getCliente().getDireccion())) altura += 20;
        }
        return Math.max(altura, 430);
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto == null ? "—" : texto, fuente));
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(2);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_TOP);
        tabla.addCell(celda);
    }

    private void agregarParrafo(Document documento, String texto, Font fuente, int alineacion,
                                float antes, float despues) throws DocumentException {
        Paragraph parrafo = new Paragraph(texto == null ? "—" : texto, fuente);
        parrafo.setAlignment(alineacion);
        parrafo.setSpacingBefore(antes);
        parrafo.setSpacingAfter(despues);
        documento.add(parrafo);
    }

    private void agregarSeparador(Document documento) throws DocumentException {
        agregarParrafo(documento, "-------------------------------------", SEPARADOR, Element.ALIGN_CENTER, 2, 2);
    }

    private String nombreCliente(Factura factura) {
        return factura.getCliente() == null || !tieneTexto(factura.getCliente().getNombre())
                ? "Consumidor final" : factura.getCliente().getNombre();
    }

    private boolean tieneTexto(String texto) {
        return texto != null && !texto.isBlank();
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
