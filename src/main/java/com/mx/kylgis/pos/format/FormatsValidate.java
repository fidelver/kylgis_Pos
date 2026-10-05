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
package com.mx.kylgis.pos.format;

import java.text.ParseException;

/**
 *
 * @author JG uniCenta
 */
public class FormatsValidate extends Formats {
    
    private Formats m_fmt;
    private FormatsConstrain[] m_aConstrains;
    
    /** Creates a new instance of FormatsValidate
     * @param fmt
     * @param constrains */
    public FormatsValidate(Formats fmt, FormatsConstrain[] constrains) {
        m_fmt = fmt;
        m_aConstrains = constrains;
    }
    /** Creates a new instance of FormatsValidate
     * @param fmt */
    public FormatsValidate(Formats fmt) {
        this(fmt, new FormatsConstrain[0]);
    }
    /** Creates a new instance of FormatsValidate
     * @param fmt
     * @param constrain */
    public FormatsValidate(Formats fmt, FormatsConstrain constrain) {
        this(fmt, new FormatsConstrain[]{constrain});
    }
    
    /**
     *
     * @param value
     * @return
     */
    @Override
    protected String formatValueInt(Object value) {
        return m_fmt.formatValueInt(value);
    }

    /**
     *
     * @param value
     * @return
     * @throws ParseException
     */
    @Override
    protected Object parseValueInt(String value) throws ParseException {
        // Primero obtenemos el valor        
        Object val = m_fmt.parseValueInt(value);        
        for (int i = 0; i < m_aConstrains.length; i++) {
            val = m_aConstrains[i].check(val);
        }
        
        return val;
    }

    /**
     *
     * @return
     */
    @Override
    public int getAlignment() {
        return m_fmt.getAlignment();
    }
}
