//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//    Portions Copyright (c) 2015-2021 John Lewis (Chromis POS / ChromisKitchenScreen)
//    Portions Copyright (c) 2010-2021 Hugh Clayson / uniCenta (https://unicenta.com)
//    Portions Copyright (c) 2006-2010 Adrián Romero / Openbravo S.L.
//
//    This file is part of KylGis POS
//
//    KylGis POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    KylGis POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with KylGis POS.  If not, see <http://www.gnu.org/licenses/>.
package com.mx.kylgis.pos.scripting;

import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.AppLocal;
// import com.mx.kylgis.pos.util.ScriptletUtil;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;

/**
 *
 * @author adrianromero
 * Created on 5 de marzo de 2007, 19:57
 *
 */
class ScriptEngineVelocity implements ScriptEngine {
    
    private static VelocityEngine m_ve = null;

    private VelocityContext c = null;
    private AppView m_App;
    
    
    /** Creates a new instance of ScriptEngineVelocity */
    public ScriptEngineVelocity() throws ScriptException {
        
        if (m_ve == null) {
            // Inicializo Velocity
            m_ve = new VelocityEngine();
            // ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);

    //        m_ve.setProperty(VelocityEngine.RESOURCE_LOADER, "class");
    //        // m_ve.setProperty("class.resource.loader.description", "Velocity Classpath Resource Loader");
    //        // m_ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    //        m_ve.setProperty("class.resource.loader.class", "com.mx.kylgis.pos.forms.SystemResourceLoader");
    //        m_ve.setProperty("class.resource.loader.description", "Velocity Resource Loader");
    //        m_ve.setProperty("class.resource.loader.appresources", this);

            // Velocity 2.x uses SLF4J and UTF-8 defaults; the legacy NullLogSystem
            // and encoding constants were removed from the public API.
            try {
                m_ve.init();
            } catch (Exception e) {
                throw new ScriptException("Cannot initialize Velocity Engine", e);
            } 
        }
         c = new VelocityContext();
         c.put("config", AppConfig.getInstance());
         c.put("labels", new LocalizedLabels());

// Add JG uniCenta - see com.mx.kylgis.pos.ScriptletUtil
//         this.put("scriptletutil", new ScriptletUtil());                        

    }
    
    @Override
    public void put(String key, Object value) {
        c.put(key, value);
    }
    @Override
    public Object get(String key) {
        return c.get(key);
    }
    
    @Override
    public Object eval(String src) throws ScriptException {
        if (m_ve == null) {
            throw new ScriptException("Velocity engine not initialized.");
        } else {
            Writer w = new StringWriter();
            try {
                String localizedSrc = localizeStaticText(src);
                 if (m_ve.evaluate(c, w, "log", new StringReader(localizedSrc))) {
                    return w.toString();
                } else {
                    throw new ScriptException("Velocity engine unexpected error.");
                }
            } catch (ParseErrorException e) {
                throw new ScriptException(e.getMessage(), e);
            } catch (MethodInvocationException e) {
                throw new ScriptException(e.getMessage(), e);
            } catch (Exception e) {
                throw new ScriptException(e.getMessage(), e);
            }
        }
    }

    private static final Pattern STATIC_TEXT = Pattern.compile("(<text\\b[^>]*>)([^<\\r\\n]*)(</text>)");

    private static String localizeStaticText(String source) {
        Matcher matcher = STATIC_TEXT.matcher(source);
        StringBuffer localized = new StringBuffer();
        while (matcher.find()) {
            String literal = matcher.group(2);
            String trimmed = literal.trim();
            if (trimmed.isEmpty() || literal.indexOf('$') >= 0 || trimmed.matches("[\\s\\-_=*+!]+")) {
                matcher.appendReplacement(localized, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String escaped = trimmed.replace("\\", "\\\\").replace("\"", "\\\"");
            String replacement = matcher.group(1) + "${labels.text(\"" + escaped + "\")}" + matcher.group(3);
            matcher.appendReplacement(localized, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(localized);
        return localized.toString();
    }

    public static final class LocalizedLabels {

        public String text(String literal) {
            if (literal == null) {
                return "";
            }
            String leading = literal.substring(0, literal.length() - literal.stripLeading().length());
            String trimmed = literal.trim();
            String originalTrimmed = trimmed;
            String trailing = trimmed.isEmpty() ? "" : literal.substring(literal.stripTrailing().length());
            String punctuation = "";
            if (trimmed.endsWith(":")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
                punctuation = ":";
            }
            String key = keyFor(trimmed);
            if (key == null) {
                key = generatedStaticKey(originalTrimmed);
            }
            if (key == null) {
                return literal;
            }
            String translated = AppLocal.getIntString(key);
            if (translated == null || translated.startsWith("** ")) {
                return literal;
            }
            if (!punctuation.isEmpty() && translated.endsWith(punctuation)) {
                punctuation = "";
            }
            return leading + translated + punctuation + trailing;
        }

        public String label(String key) {
            if (key == null || key.isBlank()) {
                return "";
            }
            String resourceKey = key.startsWith("label.") || key.startsWith("Menu.") ? key : keyFor(key);
            if (resourceKey == null) {
                resourceKey = "label." + key;
            }
            String translated = AppLocal.getIntString(resourceKey);
            return translated == null || translated.startsWith("** ") ? key : translated;
        }

        public String get(String key) {
            return label(key);
        }

        private static String generatedStaticKey(String value) {
            String normalized = Normalizer.normalize(value == null ? "" : value.trim(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .toLowerCase(Locale.ROOT)
                    .replace("#", "_num")
                    .replace(":", "_colon")
                    .replace(".", "_dot")
                    .replace("/", "_slash")
                    .replace("+", "_plus")
                    .replace("?", "_question")
                    .replace("[", "_lb")
                    .replace("]", "_rb")
                    .replaceAll("[^a-z0-9]+", "_")
                    .replaceAll("^_+|_+$", "");
            return normalized.isEmpty() ? null : "label.print.static." + normalized;
        }

        private static String keyFor(String value) {
            switch (value) {
                case "Name": case "Nombre": return "label.name";
                case "Price": case "Precio": return "label.price";
                case "Price+Tax": return "label.pricetax";
                case "Unit": case "Qty": case "QTY": case "Cant.": case "Cantidad": case "Unidades": return "label.units";
                case "Total": case "MontoTotal": return "label.total";
                case "Subtotal": case "SubTotal": return "label.subtotalcash";
                case "Taxes": case "Impuestos": case "Tax": return "label.taxes";
                case "Cash": case "Efectivo": return "label.cash";
                case "Card": case "Tarjeta": return "label.card";
                case "Customer": case "Cliente": return "label.customer";
                case "Date": case "Fecha": return "label.date";
                case "User": case "Usuario": return "label.user";
                case "Terminal": return "label.terminal";
                case "Sequence": return "label.sequence";
                case "Sales": case "Ventas": return "label.sales";
                case "Refund": case "Reembolso": return "label.refunds";
                case "Voucher": case "Vale": return "label.voucher";
                case "Supplier": case "Proveedor": return "label.supplier";
                case "Warehouse": case "Almacen": return "label.warehouse";
                case "Description": case "Descripción": return "label.description";
                case "Amount": case "Importe": case "Monto": return "label.amount";
                case "Invoice": case "Factura": return "label.invoice";
                case "Receipt": case "Recibo": return "label.receipt";
                case "Ticket": case "Ticket #": return "label.ticketid";
                case "Table": case "Mesa": case "Mesa/Salón": return "label.table";
                case "Phone": case "Teléfono": return "label.phone";
                case "Address": case "Dirección": return "label.address";
                case "Category": case "Categoría": return "label.category";
                case "Ticket Type": case "Tipo": return "label.tickettype";
                case "Printer": case "Impresora": return "label.receiptprinter";
                case "Sales report": return "label.salestitle";
                case "Gracias por su preferencia": return "label.print.thanks";
                case "Gracias": return "label.print.thanksShort";
                case "Gracias.": return "label.print.thanksShortPeriod";
                case "Arqueo de Caja": case "Partial Cash Report": case "Cash Report": return "label.print.cashReport";
                case "CÓDIGO DE ORDEN": case "ORDER CODE": return "label.print.orderCode";
                case "Pago de cliente": case "Customer payment": return "label.print.customerPayment";
                case "Movimiento de Inventario": case "Inventory movement": return "label.print.inventory";
                case "Conserva Este Comprobante": case "Keep this receipt": return "label.print.keepReceipt";
                case "Card number": case "Número": case "Núm. tarjeta": return "label.cardnumber";
                case "Expiration": case "Vencimiento": return "label.cardexpdate";
                case "Operation": case "Operación": return "label.operation";
                case "Authorization": case "Autorización": return "label.authorization";
                case "Change": case "Cambio": return "label.change";
                case "Received": case "Recibido": return "label.received";
                case "Free": case "Gratis": return "label.free";
                case "Account": case "A cuenta": return "label.account";
                default: return null;
            }
        }
    }
}
