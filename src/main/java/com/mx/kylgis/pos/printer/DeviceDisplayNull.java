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
public class DeviceDisplayNull implements DeviceDisplay {
    
    private String m_sName;
    private String m_sDescription;
    
    /** Creates a new instance of DeviceDisplayNull */
    public DeviceDisplayNull() {
        this(null);
    }
    
    /** Creates a new instance of DeviceDisplayNull
     * @param desc */
    public DeviceDisplayNull(String desc) {
        m_sName = AppLocal.getIntString("display.null");
        m_sDescription = desc;
    }

    /**
     *
     * @return
     */
    @Override
    public String getDisplayName() {
        return m_sName;
    }    

    /**
     *
     * @return
     */
    @Override
    public String getDisplayDescription() {
        return m_sDescription;
    }        

    /**
     *
     * @return
     */
    @Override
    public javax.swing.JComponent getDisplayComponent() {
        return null;
    }
    
    /**
     *
     */
    @Override
    public void clearVisor() {
    }      

    /**
     *
     * @param sLine1
     * @param sLine2
     */
    @Override
    public void writeVisor(String sLine1, String sLine2) {
    } 

    /**
     *
     * @param animation
     * @param sLine1
     * @param sLine2
     */
    @Override
    public void writeVisor(int animation, String sLine1, String sLine2) {
    } 
}
