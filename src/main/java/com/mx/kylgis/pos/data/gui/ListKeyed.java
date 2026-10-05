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
package com.mx.kylgis.pos.data.gui;

import com.mx.kylgis.pos.data.loader.IKeyed;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author adrianromero
 * @param <K>
 */
public class ListKeyed<K extends IKeyed> extends ArrayList<K> {
    
    /**
     *
     * @param list
     */
    public ListKeyed(List<K> list) {
        this.addAll(list);
    }

    /**
     *
     * @param key
     * @return
     */
    public K get(Object key) {

        for (K elem : this) {
            if ((key == null && elem.getKey() == null) || (key != null && key.equals(elem.getKey()))) {
                return elem;
            }
        }
        return null;
    }
}
