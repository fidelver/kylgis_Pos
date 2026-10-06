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
package com.mx.kylgis.pos.printer;

import com.mx.kylgis.pos.forms.AppProperties;
import com.mx.kylgis.pos.printer.escpos.*;
import com.mx.kylgis.pos.printer.digital.DevicePrinterDigital;
import com.mx.kylgis.pos.printer.javapos.DeviceDisplayJavaPOS;
import com.mx.kylgis.pos.printer.javapos.DeviceFiscalPrinterJavaPOS;
import com.mx.kylgis.pos.printer.javapos.DevicePrinterJavaPOS;
import com.mx.kylgis.pos.printer.printer.DevicePrinterPrinter;
import com.mx.kylgis.pos.printer.screen.DeviceDisplayPanel;
import com.mx.kylgis.pos.printer.screen.DeviceDisplayWindow;
import com.mx.kylgis.pos.printer.screen.DevicePrinterPanel;
import com.mx.kylgis.pos.util.StringParser;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JG uniCenta
 */
public class DeviceTicket {

    private static final Logger logger = Logger.getLogger("com.mx.kylgis.pos.printer.DeviceTicket");

    private DeviceFiscalPrinter m_deviceFiscal;
    private DeviceDisplay m_devicedisplay;
    private DevicePrinter m_nullprinter;
    private DevicePrinter m_previewprinter;
    private DevicePrinter m_currentticketprinter;
    private Map<String, DevicePrinter> m_deviceprinters;
    private List<DevicePrinter> m_deviceprinterslist;

/** 
 * 
 * Creates a new instance of DeviceTicket 
 */
    public DeviceTicket() {
        // Una impresora solo de pantalla.

        m_deviceFiscal = new DeviceFiscalPrinterNull();

        m_devicedisplay = new DeviceDisplayNull();

        m_nullprinter = new DevicePrinterNull();
        m_deviceprinters = new HashMap<>();
        m_deviceprinterslist = new ArrayList<>();

        DevicePrinter p = new DevicePrinterPanel();
        m_previewprinter = p;
        m_currentticketprinter = p;
        m_deviceprinters.put("1", p);
        m_deviceprinterslist.add(p);
    }

    /**
     *
     * @param parent
     * @param props
     */
    public DeviceTicket(Component parent, AppProperties props) {

        PrinterWritterPool pws = new PrinterWritterPool();

        // La impresora fiscal
        StringParser sf = new StringParser(props.getProperty("machine.fiscalprinter"));
        String sFiscalType = sf.nextToken(':');
        String sFiscalParam1 = sf.nextToken(',');
        try {
            if ("javapos".equals(sFiscalType)) {
                m_deviceFiscal = new DeviceFiscalPrinterJavaPOS(sFiscalParam1);
            } else {
                m_deviceFiscal = new DeviceFiscalPrinterNull();
            }
        } catch (TicketPrinterException e) {
            m_deviceFiscal = new DeviceFiscalPrinterNull(e.getMessage());
        }

        StringParser sd = new StringParser(props.getProperty("machine.display"));
        String sDisplayType = sd.nextToken(':');
        String sDisplayParam1 = sd.nextToken(',');
        String sDisplayParam2 = sd.nextToken(',');

        if ("serial".equals(sDisplayType) 
                || "rxtx".equals(sDisplayType) 
                || "file".equals(sDisplayType)) {
            sDisplayParam2 = sDisplayParam1;
            sDisplayParam1 = sDisplayType;
            sDisplayType = "epson";
        }

        try {
         
            switch (sDisplayType) {
                case "screen":
                    m_devicedisplay = new DeviceDisplayPanel();
                    break;
                case "window":
                    m_devicedisplay = new DeviceDisplayWindow();
                    break;
                case "epson":
                    m_devicedisplay = new DeviceDisplayESCPOS(
                            pws.getPrinterWritter(sDisplayParam1, sDisplayParam2)
                            , new UnicodeTranslatorInt());
                    break;
                case "surepos":
                    m_devicedisplay = new DeviceDisplaySurePOS(
                            pws.getPrinterWritter(sDisplayParam1, sDisplayParam2));
                    break;
                case "ld200":
                    m_devicedisplay = new DeviceDisplayESCPOS(
                            pws.getPrinterWritter(sDisplayParam1, sDisplayParam2)
                            , new UnicodeTranslatorEur());
                    break;
                case "javapos":
                    m_devicedisplay = new DeviceDisplayJavaPOS(sDisplayParam1);
                    break;
                default:
                    m_devicedisplay = new DeviceDisplayNull();
                    break;
            }
        } catch (TicketPrinterException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            m_devicedisplay = new DeviceDisplayNull(e.getMessage());
        }

        m_nullprinter = new DevicePrinterNull();
        m_previewprinter = m_nullprinter;
        m_currentticketprinter = m_nullprinter;

        m_deviceprinters = new HashMap<>();
        m_deviceprinterslist = new ArrayList<>();

        // Salidas espejo de la impresora principal. También reconocemos
        // configuraciones antiguas donde screen/digital ocupaban una posición.
        boolean screenMirrorEnabled = Boolean.parseBoolean(props.getProperty("screen.ticket.enabled"));
        boolean digitalMirrorEnabled = Boolean.parseBoolean(props.getProperty("digital.ticket.enabled"));
        String digitalMirrorPath = props.getProperty("digital.ticket.path");

        // Ancho raster real del recibo. En impresoras térmicas de 58 mm con
        // ticket.width=32 son 384 puntos (12 puntos por carácter). Puede
        // ajustarse explícitamente con printer.image.width para otro hardware.
        int receiptRasterWidth = 256;
        try {
            String configuredRasterWidth = props.getProperty("printer.image.width");
            if (configuredRasterWidth != null && !configuredRasterWidth.trim().isEmpty()) {
                receiptRasterWidth = Integer.parseInt(configuredRasterWidth.trim());
            } else {
                String ticketWidth = props.getProperty("ticket.width");
                if (ticketWidth != null && !ticketWidth.trim().isEmpty()) {
                    receiptRasterWidth = Integer.parseInt(ticketWidth.trim()) * 12;
                }
            }
        } catch (NumberFormatException ex) {
            receiptRasterWidth = 256;
        }
        receiptRasterWidth = Math.max(64, Math.min(2048, ((receiptRasterWidth + 7) / 8) * 8));

        // Empezamos a iterar por las impresoras...
        int iPrinterIndex = 1;
        String sPrinterIndex = Integer.toString(iPrinterIndex);
        String sprinter = props.getProperty("machine.printer");

        while (sprinter != null && !"".equals(sprinter)) {

            StringParser sp = new StringParser(sprinter);
            String sPrinterType = sp.nextToken(':');
            String sPrinterParam1 = sp.nextToken(',');
            String sPrinterParam2 = sp.nextToken(',');


            if ("serial".equals(sPrinterType) 
                    || "rxtx".equals(sPrinterType) 
                    || "file".equals(sPrinterType)) {
                sPrinterParam2 = sPrinterParam1;
                sPrinterParam1 = sPrinterType;
                sPrinterType = "epson";
            }

            try {
        
                switch (sPrinterType) {
                    case "screen":
                        // Compatibilidad: screen deja de consumir una posición y pasa
                        // a ser espejo de la impresora principal.
                        screenMirrorEnabled = true;
                        break;
                    case "digital":
                        // Compatibilidad: digital deja de consumir una posición.
                        digitalMirrorEnabled = true;
                        if (sPrinterParam1 != null && !sPrinterParam1.trim().isEmpty()) {
                            digitalMirrorPath = sPrinterParam1.trim();
                        }
                        break;
                    case "printer":
                        // backward compatibility
                        if (sPrinterParam2 == null || sPrinterParam2.equals("") 
                                || sPrinterParam2.equals("true")) {
                            sPrinterParam2 = "receipt";
                        } else if (sPrinterParam2.equals("false")) {
                            sPrinterParam2 = "standard";
                        }
                        addPrinter(sPrinterIndex, new DevicePrinterPrinter(parent, sPrinterParam1,
                                Integer.parseInt(props.getProperty("paper." + sPrinterParam2 + ".x")),
                                Integer.parseInt(props.getProperty("paper." + sPrinterParam2 + ".y")),
                                Integer.parseInt(props.getProperty("paper." + sPrinterParam2 + ".width")),
                                Integer.parseInt(props.getProperty("paper." + sPrinterParam2 + ".height")),
                                props.getProperty("paper." + sPrinterParam2 + ".mediasizename")
                                ));
                        break;
                    case "epson":
                        CodesEpson epsonCodes = new CodesEpson();
                        epsonCodes.setImageWidthOverride(receiptRasterWidth);
                        addPrinter(sPrinterIndex, new DevicePrinterESCPOS(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)
                                , epsonCodes, new UnicodeTranslatorInt()));
                        break;
                    case "tmu220":
                        addPrinter(sPrinterIndex, new DevicePrinterESCPOS(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)
                                , new CodesTMU220(), new UnicodeTranslatorInt()));
                        break;
                    case "star":
                        addPrinter(sPrinterIndex, new DevicePrinterESCPOS(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)
                                , new CodesStar(), new UnicodeTranslatorStar()));
                        break;
                    case "ithaca":
                        addPrinter(sPrinterIndex, new DevicePrinterESCPOS(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)
                                , new CodesIthaca(), new UnicodeTranslatorInt()));
                        break;
                    case "surepos":
                        addPrinter(sPrinterIndex, new DevicePrinterESCPOS(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)
                                , new CodesSurePOS(), new UnicodeTranslatorSurePOS()));
                        break;
                    case "plain":
                        addPrinter(sPrinterIndex, new DevicePrinterPlain(
                                pws.getPrinterWritter(sPrinterParam1, sPrinterParam2)));
                        break;
                    case "javapos":
                        addPrinter(sPrinterIndex, new DevicePrinterJavaPOS(
                                sPrinterParam1, sPrinterParam2));
                        break;
                }
            } catch (TicketPrinterException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            iPrinterIndex++;
            sPrinterIndex = Integer.toString(iPrinterIndex);
            sprinter = props.getProperty("machine.printer." + sPrinterIndex);
        }

        // Printer 1 es la cola principal de recibos. Las salidas espejo no
        // consumen posiciones 2-6 ni requieren cambios en las plantillas.
        DevicePrinter primary = m_deviceprinters.get("1");
        if (primary == null) {
            primary = m_nullprinter;
        }
        m_currentticketprinter = primary;

        if (screenMirrorEnabled || digitalMirrorEnabled) {
            int screenTicketColumns = 42;
            try {
                screenTicketColumns = Integer.parseInt(props.getProperty("ticket.width"));
            } catch (Exception ex) {
                screenTicketColumns = 42;
            }
            DevicePrinter screenMirror = screenMirrorEnabled ? new DevicePrinterPanel(screenTicketColumns) : null;
            if (screenMirror != null) {
                m_previewprinter = screenMirror;
            }
            DevicePrinter digitalMirror = digitalMirrorEnabled
                    ? new DevicePrinterDigital(digitalMirrorPath, props) : null;
            DevicePrinter currentTicketDigitalMirror = digitalMirrorEnabled
                    ? new DevicePrinterDigital(digitalMirrorPath, props, true) : null;

            // El boton de ticket actual usa las tres salidas del recibo:
            // virtual -> pantalla -> fisica. KitchenScreen sigue siendo una
            // accion independiente y no forma parte de esta cadena.
            m_currentticketprinter = (screenMirror != null || currentTicketDigitalMirror != null)
                    ? new DevicePrinterMirror(primary, screenMirror, currentTicketDigitalMirror)
                    : primary;

            DevicePrinter mirror = new DevicePrinterMirror(primary, screenMirror, digitalMirror);

            int primaryIndex = m_deviceprinterslist.indexOf(primary);
            if (primaryIndex >= 0) {
                m_deviceprinterslist.set(primaryIndex, mirror);
            } else {
                m_deviceprinterslist.add(0, mirror);
            }
            m_deviceprinters.put("1", mirror);
        }
    }

    private void addPrinter(String sPrinterIndex, DevicePrinter p) {
        m_deviceprinters.put(sPrinterIndex, p);
        m_deviceprinterslist.add(p);
    }

    private static class PrinterWritterPool {

        private final Map<String, PrinterWritter> m_apool = new HashMap<>();

        public PrinterWritter getPrinterWritter(String con, String port) throws TicketPrinterException {

            String skey = con + "-->" + port;
            PrinterWritter pw = (PrinterWritter) m_apool.get(skey);
            if (pw == null) {
            
                switch (con) {
                    case "serial":
                    case "rxtx":
                        pw = new PrinterWritterRXTX(port);
                        m_apool.put(skey, pw);
                        break;
                    case "file":
                        pw = new PrinterWritterFile(port);
                        m_apool.put(skey, pw);
                        break;
                    default:
                        throw new TicketPrinterException();
                }
            }
            return pw;
        }
    }
    

    /**
     *
     * @return Fiscal printer
     */
        public DeviceFiscalPrinter getFiscalPrinter() {
        return m_deviceFiscal;
    }
    
    /**
     *
     * @return Device display
     */
        public DeviceDisplay getDeviceDisplay() {
        return m_devicedisplay;
    }
    
    /**
     *
     * @param key
     * @return Device printer
     */
        public DevicePrinter getDevicePrinter(String key) {
        DevicePrinter printer = m_deviceprinters.get(key);
        return printer == null ? m_nullprinter : printer;
    }

    /**
     * Returns the screen-only printer used by ticket preview. Preview output
     * must never flow through the physical/digital mirror chain.
     *
     * @return preview printer
     */
    public DevicePrinter getPreviewPrinter() {
        return m_previewprinter == null ? m_nullprinter : m_previewprinter;
    }

    /**
     * Printer used by the current-ticket print/preview action. It includes the
     * virtual archive, on-screen receipt and physical receipt printer when each
     * is enabled. KitchenScreen remains a separate sales action.
     */
    public DevicePrinter getCurrentTicketPrinter() {
        return m_currentticketprinter == null ? m_nullprinter : m_currentticketprinter;
    }

    /**
     *
     * @return Device printer list
     */
    public List<DevicePrinter> getDevicePrinterAll() {
        return m_deviceprinterslist;
    }
    
    /**
     *
     * @param iSize
     * @param cWhiteChar
     * @return Spacing string length
     */
        public static String getWhiteString(int iSize, char cWhiteChar) {

        char[] cFill = new char[iSize];
        for (int i = 0; i < iSize; i++) {
            cFill[i] = cWhiteChar;
        }
        return new String(cFill);
    }

    /**
     *
     * @param iSize
     * @return Space sizing
     */
    public static String getWhiteString(int iSize) {

        return getWhiteString(iSize, ' ');
    }

    /**
     *
     * @param sLine
     * @param iSize
     * @return Barcode bar inter-spacing
     */
    public static String alignBarCode(String sLine, int iSize) {

        if (sLine.length() > iSize) {
            return sLine.substring(sLine.length() - iSize);
        } else {
            return getWhiteString(iSize - sLine.length(), '0') + sLine;
        }
    }

    /**
     *
     * @param sLine
     * @param iSize
     * @return Reduce spacing
     */
    public static String alignLeft(String sLine, int iSize) {

        if (sLine.length() > iSize) {
            return sLine.substring(0, iSize);
        } else {
            return sLine + getWhiteString(iSize - sLine.length());
        }
    }

    /**
     *
     * @param sLine
     * @param iSize
     * @return Add spacing
     */
    public static String alignRight(String sLine, int iSize) {

        if (sLine.length() > iSize) {
            return sLine.substring(sLine.length() - iSize);
        } else {
            return getWhiteString(iSize - sLine.length()) + sLine;
        }
    }

    /**
     *
     * @param sLine
     * @param iSize
     * @return Adjusts Left/Right spacing
     */
    public static String alignCenter(String sLine, int iSize) {

        if (sLine.length() > iSize) {
            return alignRight(sLine.substring(0, (sLine.length() + iSize) / 2), iSize);
        } else {
            return alignRight(sLine + getWhiteString((iSize - sLine.length()) / 2), iSize);
        }
    }

    /**
     *
     * @param sLine
     * @return Equalise Left/Right spacing 
     */
    public static String alignCenter(String sLine) {
        return alignCenter(sLine, 28);
    }

// JG 16 May 12     public static final byte[] transNumber(String sCad) {

    /**
     *
     * @param sCad
     * @return Convert number to string
     */
        public static byte[] transNumber(String sCad) {

        if (sCad == null) {
            return null;
        } else {
            byte bAux[] = new byte[sCad.length()];
            for( int i = 0; i < sCad.length(); i++) {
                bAux[i] = transNumberChar(sCad.charAt(i));
            }
            return bAux;
        }
    }

    /**
     *
     * @param sChar
     * @return Convert hex to character
     */
    public static byte transNumberChar(char sChar) {
        switch (sChar) {
        case '0' : return 0x30;
        case '1' : return 0x31;
        case '2' : return 0x32;
        case '3' : return 0x33;
        case '4' : return 0x34;
        case '5' : return 0x35;
        case '6' : return 0x36;
        case '7' : return 0x37;
        case '8' : return 0x38;
        case '9' : return 0x39;
        default: return 0x30;
        }
    }
}
