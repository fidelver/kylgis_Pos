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
package com.mx.kylgis.pos.printer.escpos;

import com.mx.kylgis.pos.printer.DevicePrinter;
import com.mx.kylgis.pos.printer.TicketPrinterException;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author JG uniCenta
 */
public class DevicePrinterPlain implements DevicePrinter  {
    
    private static final byte[] NEW_LINE = {0x0D, 0x0A}; // Print and carriage return
      
    private final PrinterWritter out;
    private final UnicodeTranslator trans;
    
    // Creates new TicketPrinter

    /**
     *
     * @param CommOutputPrinter
     * @throws TicketPrinterException
     */
        public DevicePrinterPlain(PrinterWritter CommOutputPrinter) throws TicketPrinterException {

        out = CommOutputPrinter;
        trans = new UnicodeTranslatorStar(); // The star translator stands for the 437 int char page
    }
   
    /**
     *
     * @return
     */
    @Override
    public String getPrinterName() {
        return "Plain";
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterDescription() {
        return null;
    }   

    /**
     *
     * @return
     */
    @Override
    public JComponent getPrinterComponent() {
        return null;
    }

    /**
     *
     */
    @Override
    public void reset() {
    }
    
    /**
     *
     */
    @Override
    public void beginReceipt() {
    }
    
    /**
     *
     * @param image
     */
    @Override
    public void printImage(BufferedImage image) {
    }
    
    /**
     *
     */
    @Override
    public void printLogo(){
        
    }
    
    /**
     *
     * @param type
     * @param position
     * @param code
     */
    @Override
    public void printBarCode(String type, String position, String code) {        
        if (! DevicePrinter.POSITION_NONE.equals(position)) {                
            out.write(code);
            out.write(NEW_LINE);
        }
    }
    
    /**
     *
     * @param iTextSize
     */
    @Override
    public void beginLine(int iTextSize) {
    }
    
    /**
     *
     * @param iStyle
     * @param sText
     */
    @Override
    public void printText(int iStyle, String sText) {
        out.write(trans.transString(sText));
    }
    
    /**
     *
     */
    @Override
    public void endLine() {
        out.write(NEW_LINE);
    }
    
    /**
     *
     */
    @Override
    public void endReceipt() {       
        out.write(NEW_LINE);
        out.write(NEW_LINE);
        out.write(NEW_LINE);
        out.write(NEW_LINE);
        out.write(NEW_LINE);
        out.flush();
    }
    
    /**
     *
     */
    @Override
       public void openDrawer() {
    }
}

