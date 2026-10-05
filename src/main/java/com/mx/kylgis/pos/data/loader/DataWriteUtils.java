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

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 * @author  adrian
 */
public class DataWriteUtils {
    
    private static DateFormat tsf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
    
    
    /** Creates a new instance of DataWriteUtils */
    public DataWriteUtils() {
    }
    
    /**
     *
     * @param obj
     * @return
     */
    public static String getSQLValue(Object obj) {
        if (obj == null) {
            return "NULL";
        } else if (obj instanceof Double) {
            return getSQLValue((Double) obj);
        } else if (obj instanceof Integer) {
            return getSQLValue((Integer) obj);
        } else if (obj instanceof Boolean) {
            return getSQLValue((Boolean) obj);
        } else if (obj instanceof String) {
            return getSQLValue((String) obj);
        } else if (obj instanceof Date) {
            return getSQLValue((Date) obj);
        } else {
            return getSQLValue(obj.toString());
        }            
    }
    
    /**
     *
     * @param iValue
     * @return
     */
    public static String getSQLValue(Integer iValue) {
        if (iValue == null) {
            return "NULL";
        } else {
            return iValue.toString();
        }
    }
    
    /**
     *
     * @param dValue
     * @return
     */
    public static String getSQLValue(Double dValue) {
        if (dValue == null) {
            return "NULL";
        } else {
            return dValue.toString();
        }
    }
    
    /**
     *
     * @param bValue
     * @return
     */
    public static String getSQLValue(Boolean bValue) {
        if (bValue == null) {
            return "NULL";
        } else {
            return bValue.booleanValue() ? "TRUE" : "FALSE";
        }
    }
    
    /**
     *
     * @param sValue
     * @return
     */
    public static String getSQLValue(String sValue) {
        if (sValue == null) {
            return "NULL";
        } else {
            return '\'' + getEscaped(sValue) + '\'';
        }
    }
    
    /**
     *
     * @param dValue
     * @return
     */
    public static String getSQLValue(Date dValue) {
        if (dValue == null) {
            return "NULL";
        } else {
            return "{ts '" + tsf.format(dValue) + "'}";
        }
    }
    
    /**
     *
     * @param sValue
     * @return
     */
    public static String getEscaped(String sValue) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sValue.length(); i++) {
            switch (sValue.charAt(i)) {
                case '\\':
                    sb.append("\\\\");
                    break;
                 case '\'':
                    sb.append("\\'");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default: 
                    sb.append(sValue.charAt(i));
                    break;
            }
        }
        return sb.toString();
    }
 }
