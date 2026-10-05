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
package com.mx.kylgis.pos.catalog;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.DataRead;
import com.mx.kylgis.pos.data.loader.SerializerRead;

/**
 *
 * @author JG uniCenta Dec 17
 * Used in Categories to display all this Categories Products
 */

public class CategoryStock {

    String productId;
    String productName;
    String productCode;
    String categoryId;   

    /**
     * Main method to return all customer's transactions 
     */
    public CategoryStock() {
    }

    /**
     *
     * @param productId
     * @param productName
     * @param cId
     */
    public CategoryStock(String productId, String productName, String productCode, String pId) {
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.categoryId = pId;        
    }

    /**
     *
     * @return product string
     */
    public String getProductId() {
        return productId;
    }
    public void setProductId(String productId) {
        this.productId = productId;
    }

    /**
     *
     * @return product name string 
     */
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    /**
     *
     * @return product barcode string 
     */
    public String getProductCode() {
        return productCode;
    }
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }    

    /**
     *
     * @return category name string
     */
    public String getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    /**
     *
     * @return products for this category
     */
    public static SerializerRead getSerializerRead() {
        return new SerializerRead() {
            @Override
            public Object readValues(DataRead dr) throws BasicException {
                String productId = dr.getString(1);
                String productName = dr.getString(2);
                String productCode = dr.getString(3);                
                String categoryId = dr.getString(4);
                return new CategoryStock(productId, productName, productCode, categoryId);
            }
        };
    }
}