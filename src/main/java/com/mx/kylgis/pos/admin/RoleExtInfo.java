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
package com.mx.kylgis.pos.admin;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.DataRead;

/**
 *
 * @author adrianromero
 * Created on 27 de febrero de 2007, 23:46
 *
 */
public class RoleExtInfo extends RoleInfo {
    
    /**
     *
     */
    protected byte[] m_aPermissions;
    
    /** Creates a new instance of RoleExtInfo */
    public RoleExtInfo() {
        super();
        m_aPermissions = null;
    }
    
    /**
     *
     * @param dr
     * @throws BasicException
     */
    @Override
    public void readValues(DataRead dr) throws BasicException {
        m_sName = dr.getString(1);
        m_aPermissions = dr.getBytes(2);
    }   
    
    //  implements Vectorer, ComparatorCreator

    /**
     *
     * @return
     */
        public static String[] getHeaders() {
        return new String[] {"Name"};
    }
    
    /**
     *
     * @return
     */
    public String[] toStringArray() {
        return new String[] {m_sName};
    } 

    /**
     *
     * @return
     */
    public Comparable[] toComparableArray() {
        return new Comparable[] {m_sName};
    }
}
