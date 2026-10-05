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
package com.mx.kylgis.pos.inventory;

import com.mx.kylgis.pos.panels.ComboItemLocal;

/**
 *
 * @author adrianromero
 */
public class MovementReason extends ComboItemLocal {
    
// IN values
    public static final MovementReason IN_PURCHASE = new MovementReason(+1, "stock.in.purchase");
    public static final MovementReason IN_REFUND = new MovementReason(+2, "stock.in.refund");
    public static final MovementReason IN_MOVEMENT = new MovementReason(+4, "stock.in.movement");    
    public static final MovementReason IN_ADJUST = new MovementReason(+5, "stock.in.adjust");

// OUT values    
    public static final MovementReason OUT_SALE = new MovementReason(-1, "stock.out.sale");
    public static final MovementReason OUT_REFUND = new MovementReason(-2, "stock.out.refund");
    public static final MovementReason OUT_BREAK = new MovementReason(-3, "stock.out.break");
    public static final MovementReason OUT_MOVEMENT = new MovementReason(-4, "stock.out.movement");
    
    public static final MovementReason OUT_SAMPLE = new MovementReason(-5, "stock.out.sample");    
    public static final MovementReason OUT_FREE = new MovementReason(-6, "stock.out.free");    
    public static final MovementReason OUT_USED = new MovementReason(-7, "stock.out.used");
    public static final MovementReason OUT_SUBTRACT = new MovementReason(-8, "stock.out.subtract");
    public static final MovementReason OUT_ADJUST = new MovementReason(-9, "stock.out.adjust");

// TRANSFER
    public static final MovementReason OUT_CROSSING = new MovementReason(1000, "stock.out.crossing");
   
    private MovementReason(Integer iKey, String sKeyValue) {
        super(iKey, sKeyValue);
    }

    public boolean isInput() {
        return m_iKey.intValue() > 0;
    }

    public Double samesignum(Double d) {
        
        if (d == null || m_iKey == null) {
            return d;
        } else if ((m_iKey.intValue() > 0 && d.doubleValue() < 0.0) ||
            (m_iKey.intValue() < 0 && d.doubleValue() > 0.0)) {
            return -d.doubleValue();
        } else {
            return d;
        }            
    }

    /**
     *
     * @param dBuyPrice
     * @param dSellPrice
     * @return
     */
    public Double getPrice(Double dBuyPrice, Double dSellPrice) {
        
        if (this == IN_PURCHASE || this == OUT_REFUND || this == OUT_BREAK) {
            return dBuyPrice;
        } else if (this == OUT_SALE || this == IN_REFUND) {
            return dSellPrice;
        } else {
            return null;
        }
    }
}
