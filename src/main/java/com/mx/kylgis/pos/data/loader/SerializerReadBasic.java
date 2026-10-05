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
package com.mx.kylgis.pos.data.loader;

import com.mx.kylgis.pos.basic.BasicException;

/**
 *
 * @author JG uniCenta
 */
public class SerializerReadBasic implements SerializerRead {
    
    private Datas[] m_classes;
    
    /** Creates a new instance of SerializerReadBasic
     * @param classes */
    public SerializerReadBasic(Datas[] classes) {
        m_classes = classes;
    }
    
    /**
     *
     * @param dr
     * @return
     * @throws BasicException
     */
    public Object readValues(DataRead dr) throws BasicException {
        
        Object[] m_values = new Object[m_classes.length];
        for (int i = 0; i < m_classes.length; i++) {
            m_values[i] = m_classes[i].getValue(dr, i + 1);
        }
        return m_values;
    }    
}
