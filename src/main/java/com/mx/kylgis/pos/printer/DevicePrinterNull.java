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

import com.mx.kylgis.pos.forms.AppLocal;

/**
 *
 * @author JG uniCenta
 */
public class DevicePrinterNull implements DevicePrinter {
    
    private String m_sName;
    private String m_sDescription;
    
    /** Creates a new instance of DevicePrinterNull */
    public DevicePrinterNull() {
        this(null);
    }
    
    /** Creates a new instance of DevicePrinterNull
     * @param desc */
    public DevicePrinterNull(String desc) {
        m_sName = AppLocal.getIntString("printer.null");
        m_sDescription = desc;
    }

    /**
     *
     * @return
     */
    @Override
    public String getPrinterName() {
        return m_sName;
    }    

    /**
     *
     * @return
     */
    @Override
    public String getPrinterDescription() {
        return m_sDescription;
    }        

    /**
     *
     * @return
     */
    @Override
    public javax.swing.JComponent getPrinterComponent() {
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
     * @param type
     * @param position
     * @param code
     */
    @Override
    public void printBarCode(String type, String position, String code) {        
    }    

    /**
     *
     * @param image
     */
    @Override
    public void printImage(java.awt.image.BufferedImage image) {
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
    }   

    /**
     *
     */
    @Override
    public void endLine() {
    }

    /**
     *
     */
    @Override
    public void endReceipt() {
    }

    /**
     *
     */
    @Override
     public void openDrawer() {
    }

    /**
     *
     */
    @Override
    public void printLogo() {
    }
}
