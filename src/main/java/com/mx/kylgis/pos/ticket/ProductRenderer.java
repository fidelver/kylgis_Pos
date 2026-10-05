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

import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.util.ThumbNailBuilder;
import java.awt.*;
import javax.swing.*;

/**
 *
 * @author uniCenta 2017
 *
 */
public class ProductRenderer extends DefaultListCellRenderer {
                
    ThumbNailBuilder tnbprod;

    /** Creates a new instance of ProductRenderer */
    public ProductRenderer() {   
//        tnbprod = new ThumbNailBuilder(48,48, "com/mx/kylgis/pos/images/package.png");
        tnbprod = new ThumbNailBuilder(48,48, "com/mx/kylgis/pos/images/null.png");        
      
       
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, null, index, isSelected, cellHasFocus);
        
        ProductInfoExt prod = (ProductInfoExt) value;
       
        if (prod != null) {
                        
            setText("<html><center>" 
                    + prod.getReference() 
                    + " - " 
                    + prod.getName() + " - " + Formats.CURRENCY.formatValue(prod.getPriceSell())
//                    + "<br>" + prod.getStockUnits() + " | " + Formats.DATE.formatValue(prod.getMemoDate()) + " | " + prod.getPriceSellTax(tax)
                    );
            
            Image img = tnbprod.getThumbNail(prod.getImage());
            setIcon(img == null ? null :new ImageIcon(img));
        }
        return this;
    }      
}
