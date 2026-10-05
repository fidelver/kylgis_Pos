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
package com.mx.kylgis.pos.inventory;

import com.mx.kylgis.pos.data.loader.IKeyed;

/**
 *
 * @author adrianromero
 */
public class CodeType implements IKeyed {
    
    /**
     *
     */
    public static final CodeType EAN13 = new CodeType("EAN13", "EAN13");

    /**
     *
     */
    public static final CodeType CODE128 = new CodeType("CODE128", "CODE128");

    /**
     *
     */
    protected String m_sKey;

    /**
     *
     */
    protected String m_sValue;
    
    private CodeType(String key, String value) {
        m_sKey = key;
        m_sValue = value;
    }

    /**
     *
     * @return
     */
    @Override
    public Object getKey() {
        return m_sKey;
    }

    /**
     *
     * @return
     */
    public String getValue() {
        return m_sValue;
    }
    @Override
    public String toString() {
        return m_sValue;
    }   
}