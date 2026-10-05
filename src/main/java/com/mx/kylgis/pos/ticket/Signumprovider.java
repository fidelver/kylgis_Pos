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
package com.mx.kylgis.pos.ticket;

import java.util.*;

/**
 *
 * @author JG uniCenta
 */
public class Signumprovider {
    
    private Set m_positives = new HashSet();
    private Set m_negatives = new HashSet();
    
    /** Creates a new instance of Signumprovider */
    public Signumprovider() {
    }
    
    /**
     *
     * @param key
     */
    public void addPositive(Object key) {
        m_positives.add(key);
    }
    
    /**
     *
     * @param key
     */
    public void addNegative(Object key) {
        m_negatives.add(key);
    }
    
    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Double correctSignum(Object key, Double value) {
        if (m_positives.contains(key)) {
            return value.doubleValue() < 0.0 ? new Double(-value.doubleValue()) : value;
        } else if (m_negatives.contains(key)) {
            return value.doubleValue() > 0.0 ? new Double(-value.doubleValue()) : value;
        } else {
            return value;
        }        
    }    
}
