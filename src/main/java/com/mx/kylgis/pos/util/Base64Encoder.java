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
package com.mx.kylgis.pos.util;

import java.io.UnsupportedEncodingException;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author JG uniCenta
 */
public class Base64Encoder {
    
    /**
     *
     * @param base64
     * @return
     */
    public static byte[] decode(String base64) {

        try {
            return Base64.decodeBase64(base64.getBytes("ASCII"));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     *
     * @param raw
     * @return
     */
    public static String encode(byte[] raw) {
        try {
            return new String(Base64.encodeBase64(raw), "ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
    
    /**
     *
     * @param raw
     * @return
     */
    public static String encodeChunked(byte[] raw) {
        try {
            return new String(Base64.encodeBase64Chunked(raw), "ASCII");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}