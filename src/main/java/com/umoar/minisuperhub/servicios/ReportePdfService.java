package com.umoar.minisuperhub.servicios;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.umoar.minisuperhub.dto.ProductoMasVendido;
import com.umoar.minisuperhub.dto.ReporteVentas;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReportePdfService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(ReporteVentas reporte) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();

            Font titulo = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL);
            document.add(new Paragraph("Reporte de ventas - MiniSuper Hub", titulo));
            document.add(new Paragraph("Periodo: " + reporte.fechaInicio().format(FORMATO_FECHA)
                    + " al " + reporte.fechaFin().format(FORMATO_FECHA), subtitulo));
            document.add(new Paragraph(" "));

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(100);
            resumen.addCell("Indicador");
            resumen.addCell("Resultado");
            resumen.addCell("Total de ventas");
            resumen.addCell(String.format(Locale.US, "$%.2f", reporte.totalVentas()));
            resumen.addCell("Facturas emitidas");
            resumen.addCell(String.valueOf(reporte.totalFacturas()));
            resumen.addCell("Ingresos registrados");
            resumen.addCell(String.format(Locale.US, "$%.2f", reporte.ingresos()));
            document.add(resumen);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Productos más vendidos", new Font(Font.HELVETICA, 13, Font.BOLD)));

            PdfPTable ranking = new PdfPTable(new float[]{1, 5, 2});
            ranking.setWidthPercentage(100);
            ranking.addCell("#");
            ranking.addCell("Producto");
            ranking.addCell("Unidades");
            int posicion = 1;
            for (ProductoMasVendido producto : reporte.productosMasVendidos()) {
                ranking.addCell(String.valueOf(posicion++));
                ranking.addCell(String.valueOf(producto.nombre()));
                ranking.addCell(String.valueOf(producto.unidades()));
            }
            if (reporte.productosMasVendidos().isEmpty()) {
                ranking.addCell("—");
                ranking.addCell("Sin ventas de productos en este periodo");
                ranking.addCell("0");
            }
            document.add(ranking);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Los ingresos se calculan a partir de facturas no anuladas; "
                    + "el sistema no registra cobros por separado.", subtitulo));
            document.close();
            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", exception);
        }
    }
}
