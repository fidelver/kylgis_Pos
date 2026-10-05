//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
package com.mx.kylgis.pos.printer.digital;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.printer.DevicePrinter;
import com.mx.kylgis.pos.printer.ticket.BasicTicket;
import com.mx.kylgis.pos.printer.ticket.BasicTicketForPrinter;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JComponent;

/**
 * Impresora virtual que genera una imagen JPG con ancho de rollo y altura dinámica.
 */
public class DevicePrinterDigital implements DevicePrinter {

    private static final Logger LOGGER = Logger.getLogger(DevicePrinterDigital.class.getName());
    private static final String[] MONTHS = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };
    private static final double RENDER_SCALE = 3.0; // ~216 dpi partiendo de puntos PDF (72 dpi)
    private static final float MARGIN_POINTS = 6.0f;

    private final AppProperties props;
    private final File baseDirectory;
    private BasicTicket ticket;
    private TicketInfo ticketInfo;

    public DevicePrinterDigital(String directory, AppProperties props) {
        this.props = props;
        this.baseDirectory = resolveBaseDirectory(directory, props);
    }

    private static File resolveBaseDirectory(String directory, AppProperties props) {
        String path = directory;
        if (path == null || path.trim().isEmpty()) {
            path = props == null ? null : props.getProperty("digital.ticket.path");
        }
        if (path == null || path.trim().isEmpty()) {
            path = new File(System.getProperty("user.home"), "KylGisPOS/Tickets").getPath();
        }
        path = path.trim();
        if (path.equals("~")) {
            path = System.getProperty("user.home");
        } else if (path.startsWith("~/") || path.startsWith("~" + File.separator)) {
            path = System.getProperty("user.home") + path.substring(1);
        }
        return new File(path);
    }

    @Override
    public void setDocumentContext(TicketInfo ticketInfo) {
        this.ticketInfo = ticketInfo;
    }

    @Override
    public String getPrinterName() {
        return "Digital";
    }

    @Override
    public String getPrinterDescription() {
        return baseDirectory.getAbsolutePath();
    }

    @Override
    public JComponent getPrinterComponent() {
        return null;
    }

    @Override
    public void reset() {
        ticket = null;
        ticketInfo = null;
    }

    @Override
    public void beginReceipt() {
        ticket = new BasicTicketForPrinter();
    }

    @Override
    public void printImage(BufferedImage image) {
        if (ticket != null) ticket.printImage(image);
    }

    @Override
    public void printLogo() {
        // Los tickets KylGis usan normalmente <image>Printer.Ticket.Logo</image>.
    }

    @Override
    public void printBarCode(String type, String position, String code) {
        if (ticket != null) ticket.printBarCode(type, position, code);
    }

    @Override
    public void beginLine(int iTextSize) {
        if (ticket != null) ticket.beginLine(iTextSize);
    }

    @Override
    public void printText(int iStyle, String sText) {
        if (ticket != null) ticket.printText(iStyle, sText);
    }

    @Override
    public void endLine() {
        if (ticket != null) ticket.endLine();
    }

    @Override
    public void endReceipt() {
        if (ticket == null) return;
        try {
            File target = buildTargetFile();
            File parent = target.getParentFile();
            // Las carpetas Devoluciones/Previos nacen sólo cuando existe el primer archivo.
            if (!parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IllegalStateException("No se pudo crear la carpeta " + parent);
            }
            writeJpeg(target);
            LOGGER.log(Level.INFO, "Ticket digital creado: {0}", target.getAbsolutePath());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "No se pudo crear el ticket digital", ex);
        } finally {
            ticket = null;
            ticketInfo = null;
        }
    }

    private File buildTargetFile() {
        Date date = ticketInfo != null && ticketInfo.getDate() != null ? ticketInfo.getDate() : new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String year = String.format("%04d", cal.get(Calendar.YEAR));
        int month = cal.get(Calendar.MONTH) + 1;
        String monthFolder = String.format("%02d-%s", month, MONTHS[month - 1]);
        String day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH));

        // Cada tipo de documento vive en una carpeta hermana bajo el mismo día.
        // Ejemplo: /ruta/2026/10-Octubre/05/{Tickets,Devoluciones,Previos}
        File dayFolder = new File(new File(new File(baseDirectory, year), monthFolder), day);
        File folder = new File(dayFolder, "Tickets");
        String prefix = "ticket";
        int id = 0;

        if (ticketInfo != null) {
            id = ticketInfo.getTicketId();
            if (ticketInfo.getTicketType() == TicketInfo.RECEIPT_REFUND) {
                prefix = "reembolso";
                folder = new File(dayFolder, "Devoluciones");
            } else if (id <= 0) {
                prefix = "previo";
                folder = new File(dayFolder, "Previos");
            }
        } else {
            prefix = "documento";
        }

        String suffix = id > 0 ? Integer.toString(id) : new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(date);
        File target = new File(folder, prefix + "_" + suffix + ".jpg");

        // Nunca sobrescribir el archivo digital original de una venta/reembolso.
        // Si se vuelve a imprimir el mismo documento, conservar una copia separada.
        if (id > 0 && target.exists()) {
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
            target = new File(folder, prefix + "_" + suffix + "_reimpresion_" + stamp + ".jpg");
        }

        return target;
    }

    private void writeJpeg(File file) throws Exception {
        float pageWidthPoints = getPaperWidthPoints();
        float contentWidthPoints = getLogicalContentWidthPoints(pageWidthPoints);
        float contentXPoints = Math.max(MARGIN_POINTS, (pageWidthPoints - contentWidthPoints) / 2.0f);
        float pageHeightPoints = Math.max(24.0f, ticket.getHeight() + MARGIN_POINTS * 2.0f);

        // Renderizar primero en coordenadas lógicas sin escalar Graphics2D.
        // PrintItemLine calcula el avance entre segmentos con FontMetrics; si el
        // Graphics2D ya está escalado, esas métricas cambian y las líneas con
        // varios <text> (p. ej. Total + importe) se desplazan hacia la derecha.
        int logicalWidth = Math.max(1, (int) Math.ceil(pageWidthPoints));
        int logicalHeight = Math.max(1, (int) Math.ceil(pageHeightPoints));
        BufferedImage logicalImage = new BufferedImage(logicalWidth, logicalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D logical = logicalImage.createGraphics();
        try {
            logical.setColor(Color.WHITE);
            logical.fillRect(0, 0, logicalWidth, logicalHeight);
            logical.setColor(Color.BLACK);
            logical.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            logical.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            ticket.draw(logical,
                    Math.round(contentXPoints),
                    Math.round(MARGIN_POINTS),
                    Math.round(contentWidthPoints));
        } finally {
            logical.dispose();
        }

        // Escalar el raster ya compuesto para obtener un JPG de mayor resolución
        // sin alterar la métrica ni el espaciado lógico del ticket.
        int imageWidth = Math.max(1, (int) Math.ceil(pageWidthPoints * RENDER_SCALE));
        int imageHeight = Math.max(1, (int) Math.ceil(pageHeightPoints * RENDER_SCALE));
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D scaled = image.createGraphics();
        try {
            scaled.setColor(Color.WHITE);
            scaled.fillRect(0, 0, imageWidth, imageHeight);
            scaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            scaled.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            scaled.drawImage(logicalImage, 0, 0, imageWidth, imageHeight, null);
        } finally {
            scaled.dispose();
        }
        writeJpegQuality(image, file, 0.94f);
    }

    /**
     * Centra el bloque lógico de caracteres dentro del ancho físico del rollo.
     * Las líneas de BasicTicket se dibujan desde X y no usan width para el texto,
     * por eso el centrado debe hacerse sobre el ancho real de 32/42/64 caracteres.
     */
    private float getLogicalContentWidthPoints(float pageWidthPoints) {
        int chars = getTicketWidthChars();
        Font font = new Font("Monospaced", Font.PLAIN, 7)
                .deriveFont(AffineTransform.getScaleInstance(1.0, 1.50));
        FontRenderContext frc = new FontRenderContext(null, true, true);
        float charWidth = (float) font.getStringBounds("M", frc).getWidth();
        float logical = charWidth * chars;
        float maximum = Math.max(40.0f, pageWidthPoints - MARGIN_POINTS * 2.0f);
        return Math.max(40.0f, Math.min(logical, maximum));
    }

    private int getTicketWidthChars() {
        int width = 32;
        if (props != null) {
            try {
                width = Integer.parseInt(props.getProperty("ticket.width"));
            } catch (Exception ex) {
                width = 32;
            }
        }
        return Math.max(16, Math.min(width, 120));
    }

    private float getPaperWidthPoints() {
        int mm = 58;
        if (props != null) {
            try {
                int value = Integer.parseInt(props.getProperty("ticket.paper.size"));
                if (value == 58 || value == 80 || value == 120) mm = value;
            } catch (Exception ex) {
                mm = 58;
            }
        }
        return mm * 72.0f / 25.4f;
    }

    private static void writeJpegQuality(BufferedImage image, File file, float quality) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No hay escritor JPEG disponible");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        try (ImageOutputStream output = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(output);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    @Override
    public void openDrawer() {
        // No aplica a una salida digital.
    }
}
