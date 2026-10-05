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

import com.mx.kylgis.pos.ticket.TicketInfo;

import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author JG uniCenta
 */
public interface DevicePrinter {
    
// Font Sizes
    public static final int SIZE_0 = 0;
    public static final int SIZE_1 = 1;
    public static final int SIZE_2 = 2;
    public static final int SIZE_3 = 3;
    
// Font Enhancers
    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_UNDERLINE = 2;
    
// Layout    
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_RIGHT = 1;
    public static final int ALIGN_CENTER = 2;
    
    public static final String POSITION_BOTTOM = "bottom";
    public static final String POSITION_NONE = "none";
    
// Barcodes    
    public static final String BARCODE_EAN8 = "EAN8";
    public static final String BARCODE_EAN13 = "EAN13";
    public static final String BARCODE_UPCA = "UPC-A";
    public static final String BARCODE_UPCE = "UPC-E";    
    public static final String BARCODE_CODE128 = "CODE128";
    public static final String BARCODE_CODE39 = "CODE39";

    public String getPrinterName();
    public String getPrinterDescription();
    public JComponent getPrinterComponent();

    /** Contexto opcional del documento; las impresoras físicas pueden ignorarlo. */
    default void setDocumentContext(TicketInfo ticketInfo) {
    }

// Initialise    
    public void reset();
    public void beginReceipt();

// Graphic renders
    public void printImage(BufferedImage image);
    public void printLogo();
    public void printBarCode(String type, String position, String code);

// Do TextLine
    public void beginLine(int iTextSize);
    public void printText(int iStyle, String sText);
    public void endLine();

// Close
    public void endReceipt();   

// Transact    
    public void openDrawer();        
   
}