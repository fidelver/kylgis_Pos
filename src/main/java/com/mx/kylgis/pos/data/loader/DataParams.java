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
import java.util.Date;

/**
 *
 * @author adrianromero
 */
public abstract class DataParams implements DataWrite {
    
    /**
     *
     */
    protected DataWrite dw;
    
    /**
     *
     * @throws BasicException
     */
    public abstract void writeValues() throws BasicException;

    /**
     *
     * @param paramIndex
     * @param iValue
     * @throws BasicException
     */
    public void setInt(int paramIndex, Integer iValue) throws BasicException {
        dw.setInt(paramIndex, iValue);
    }

    public void setString(int paramIndex, String sValue) throws BasicException {
        dw.setString(paramIndex, sValue);
    }

    /**
     *
     * @param paramIndex
     * @param dValue
     * @throws BasicException
     */
    public void setDouble(int paramIndex, Double dValue) throws BasicException {
        dw.setDouble(paramIndex, dValue);
    }

    /**
     *
     * @param paramIndex
     * @param bValue
     * @throws BasicException
     */
    public void setBoolean(int paramIndex, Boolean bValue) throws BasicException {
        dw.setBoolean(paramIndex, bValue);
    }

    /**
     *
     * @param paramIndex
     * @param dValue
     * @throws BasicException
     */
    public void setTimestamp(int paramIndex, Date dValue) throws BasicException {
        dw.setTimestamp(paramIndex, dValue);
    }

    /**
     *
     * @param paramIndex
     * @param value
     * @throws BasicException
     */
    public void setBytes(int paramIndex, byte[] value) throws BasicException {
        dw.setBytes(paramIndex, value);
    }

    /**
     *
     * @param paramIndex
     * @param value
     * @throws BasicException
     */
    public void setObject(int paramIndex, Object value) throws BasicException {
        dw.setObject(paramIndex, value);
    }

    /**
     *
     * @return
     */
    public DataWrite getDataWrite() {
        return dw;
    }

    /**
     *
     * @param dw
     */
    public void setDataWrite(DataWrite dw) {
        this.dw = dw;
    }
}
