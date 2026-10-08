//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.printer;

import com.mx.kylgis.pos.basic.BasicException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validates editable printer resources before they are persisted.
 * Printer templates mix XML and Velocity, so bare ampersands used by Velocity
 * are escaped only in the validation copy; the stored template is not changed.
 */
public final class TicketTemplateValidator {

    public static final String DYNAMIC_RECEIPT_PRINTER = "$config.getProperty('ticket.printer')";

    private TicketTemplateValidator() {
    }

    public static void validateForSave(String resourceName, String template) throws BasicException {
        if (template == null) {
            return;
        }
        if (resourceName != null && !resourceName.startsWith("Printer.")) {
            return;
        }

        validateVelocitySyntax(template);

        String validationXml = template.replaceAll(
                "&(?!amp;|lt;|gt;|quot;|apos;|#\\d+;|#x[0-9A-Fa-f]+;)", "&amp;");

        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (ParserConfigurationException | SAXException ignored) {
                // Best-effort hardening. The input is an in-memory local template.
            }

            factory.newSAXParser().parse(new InputSource(new StringReader(validationXml)), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {
                    if (!"ticket".equals(qName)) {
                        return;
                    }
                    String printer = attributes.getValue("printer");
                    if (printer == null || printer.trim().isEmpty()) {
                        throw new SAXException("El elemento <ticket> requiere el atributo printer.");
                    }
                    printer = printer.trim();
                    if (printer.matches("[1-6]") || DYNAMIC_RECEIPT_PRINTER.equals(printer)) {
                        return;
                    }
                    throw new SAXException("Valor printer inválido: " + printer
                            + ". Use Printer 1..6 o la ruta dinámica oficial del recibo.");
                }
            });
        } catch (SAXParseException ex) {
            throw new BasicException("Plantilla no guardada. XML inválido en línea "
                    + ex.getLineNumber() + ", columna " + ex.getColumnNumber() + ": "
                    + ex.getMessage(), ex);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new BasicException("Plantilla no guardada. " + ex.getMessage(), ex);
        }
    }

    private static void validateVelocitySyntax(String template) throws BasicException {
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.NullLogSystem");
        engine.setProperty(VelocityEngine.ENCODING_DEFAULT, "UTF-8");
        engine.setProperty(VelocityEngine.INPUT_ENCODING, "UTF-8");
        try {
            engine.init();
            engine.evaluate(new VelocityContext(), new StringWriter(),
                    "kylgis-template-validation", new StringReader(template));
        } catch (ParseErrorException ex) {
            throw new BasicException("Plantilla no guardada. Sintaxis Velocity inválida: "
                    + ex.getMessage(), ex);
        } catch (Exception ex) {
            // Missing runtime objects ($ticket, $config, etc.) are intentionally
            // not supplied here. Any other engine failure must still block save.
            throw new BasicException("Plantilla no guardada. No se pudo validar Velocity: "
                    + ex.getMessage(), ex);
        }
    }
}
