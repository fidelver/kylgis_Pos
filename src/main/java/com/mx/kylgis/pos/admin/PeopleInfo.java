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
import com.mx.kylgis.pos.data.loader.IKeyed;
import com.mx.kylgis.pos.data.loader.SerializableRead;

/**
 *
 * @author adrianromero
 * Created on 27 de febrero de 2007, 23:27
 *
 */
public class PeopleInfo implements SerializableRead, IKeyed {
    
    private static final long serialVersionUID = 9110127845966L;
    private String m_sID;

    /**
     *
     */
    protected String m_sName;
    
    /** Creates a new instance of PeopleInfo */
    public PeopleInfo() {
        m_sID = null;
        m_sName = null;
    }
   
    /**
     *
     * @return
     */
    @Override
    public Object getKey() {
        return m_sID;
    }

    
    /**
     *
     * @param dr
     * @throws BasicException
     */
    @Override
    public void readValues(DataRead dr) throws BasicException {
        m_sID = dr.getString(1);
        m_sName = dr.getString(2);
    }

    
    /**
     *
     * @return
     */
    public String getID() {
        return m_sID;
    }
    public void setID(String sID) {
        m_sID = sID;
    }
    
    /**
     *
     * @return
     */
    public String getName() {
        return m_sName;
    }
    
    /**
     *
     * @param sValue
     */
    public void setName(String sValue) {
        m_sName = sValue;
    }    
    
    @Override
    public String toString() {
        return m_sName;
    }
}
