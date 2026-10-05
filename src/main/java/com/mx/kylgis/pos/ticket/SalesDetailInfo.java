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

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.DataRead;
import com.mx.kylgis.pos.data.loader.IKeyed;
import com.mx.kylgis.pos.data.loader.SerializerRead;
import com.mx.kylgis.pos.format.Formats;

/**
 *
 * @author  Adrian
 * @version 
 */
public class SalesDetailInfo implements IKeyed {

    private static final long serialVersionUID = 8612449444103L;
    private String productName;
    private int lineNO;

    /**
     *
     * @return
     */
    public int getLineNO() {
        return lineNO;
    }

    /**
     *
     * @param lineNO
     */
    public void setLineNO(int lineNO) {
        this.lineNO = lineNO;
    }
    
    /**
     *
     * @return
     */
    public double getPrice() {
        return price;
    }
    
    /**
     *
     * @return
     */
    public String printPrice() {
        return Formats.CURRENCY.formatValue(price);
    }

    /**
     *
     * @param price
     */
    public void setPrice(double price) {
        this.price = price;
    }

    /**
     *
     * @return
     */
    public String getProductName() {
        return productName;
    }

    /**
     *
     * @param productName
     */
    public void setProductName(String productName) {
        this.productName = productName;
    }
    private double price;

    /** Creates new CategoryInfo
     * @param lineNo
     * @param productName
     * @param price */
    public SalesDetailInfo(int lineNo, String productName, double price) {
        this.lineNO = lineNo;
        this.productName = productName;
        this.price = price;
    }

    /**
     *
     * @return
     */
    public static SerializerRead getSerializerRead() {
        return new SerializerRead() {
            @Override
            public Object readValues(DataRead dr) throws BasicException {
            return new SalesDetailInfo(
                    dr.getInt(1), 
                    dr.getString(2), 
                    dr.getDouble(3));
        }};
    }

    /**
     *
     * @return
     */
    @Override
    public Object getKey() {
       return getLineNO();
    }
}