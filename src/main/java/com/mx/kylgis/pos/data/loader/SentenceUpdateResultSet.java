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
 * @author  adrian
 */
public class SentenceUpdateResultSet implements DataResultSet {
    
    private int m_iUpdateCount;
    
    /** Creates a new instance of UpdateResultSet
     * @param iUpdateCount */
    public SentenceUpdateResultSet(int iUpdateCount) {
        m_iUpdateCount = iUpdateCount;
    }
    
    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public Integer getInt(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public String getString(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public Double getDouble(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public Boolean getBoolean(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public java.util.Date getTimestamp(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    //public java.io.InputStream getBinaryStream(int columnIndex) throws DataException;

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
        public byte[] getBytes(int columnIndex) throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @param columnIndex
     * @return
     * @throws BasicException
     */
    public Object getObject(int columnIndex) throws BasicException  {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

//    public int getColumnCount() throws DataException;

    /**
     *
     * @return
     * @throws BasicException
     */
        public DataField[] getDataField() throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public Object getCurrent() throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }
    
    /**
     *
     * @return
     * @throws BasicException
     */
    public boolean next() throws BasicException {
        throw new BasicException(LocalRes.getIntString("exception.nodataset"));
    }
     
    /**
     *
     * @throws BasicException
     */
    public void close() throws BasicException {
    }
   
    /**
     *
     * @return
     * @throws BasicException
     */
    public int updateCount() throws BasicException {
        return m_iUpdateCount;
    }
}
