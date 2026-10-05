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
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.*;

/**
 *
 * @author uniCenta
 */
public class DataLogicOrders extends BeanFactoryDataSingle {
    private SentenceExec m_addOrder;
    private SentenceExec m_resetPickup;
    
    /** Creates a new instance of DataLogicOrders */
    public DataLogicOrders() {            
    }
    
    /**
     *
     * @param s
     */
    @Override
    public void init(Session s){
        
        m_addOrder =  new StaticSentence(s
                , "INSERT INTO orders (orderid, qty, details, attributes, "
                        + "notes, ticketid, ordertime, displayid, auxiliary, "
                        + "completetime) " +
                  "VALUES (?, ?, ?, ?, ?, "
                        + "?, ?, ?, ?, ? ) "
                , new SerializerWriteBasic(new Datas[] {
                    Datas.STRING,   // OrderId
                    Datas.INT,      // Qty
                    Datas.STRING,   // Details
                    Datas.STRING,   // Attributes
                    Datas.STRING,   // Notes
                    Datas.STRING,   // TicketId
                    Datas.STRING,   // OrderTime
                    Datas.INT,      // DisplayId
                    Datas.INT,     // Auxiliary
                    Datas.STRING    // CompleteTime
                }));

        m_resetPickup =  new PreparedSentence(s
                , "UPDATE pickup_number SET id=1"
                , SerializerWriteParams.INSTANCE); 
    }


    public final void addOrder(String orderId, Integer qty, 
            String details, String attributes, String notes, String ticketId, 
            String ordertime, Integer displayId, String auxiliary, String completetime
        ) throws BasicException {

        m_addOrder.exec(orderId, qty, details, attributes, notes, ticketId, 
                ordertime, displayId, auxiliary, completetime);    
    } 
    
    public final void resetPickup() throws BasicException {
        m_resetPickup.exec();    
    }      
}
