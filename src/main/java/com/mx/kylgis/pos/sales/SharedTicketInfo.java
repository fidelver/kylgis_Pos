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
package com.mx.kylgis.pos.sales;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.DataRead;
import com.mx.kylgis.pos.data.loader.DataWrite;
import com.mx.kylgis.pos.data.loader.SerializableRead;
import com.mx.kylgis.pos.data.loader.SerializableWrite;

public class SharedTicketInfo implements SerializableRead, SerializableWrite {
    
    private static final long serialVersionUID = 7640633837719L;
    private String id;
    private String name;
    private String UserName;
    private String status;    
/*
  * For     : RickyO - display Customer Name | Phone | PickupId
  * Change  : JG uniCenta
  * Date    : May 2017          
*/            
    private String phone;
    private String pickupid;
    
    /** Creates a new instance of SharedTicketInfo */
    public SharedTicketInfo() {
    }
    
    /**
     *
     * @param dr
     * @throws BasicException
     */
    @Override
    public void readValues(DataRead dr) throws BasicException {
        id = dr.getString(1);
        name = dr.getString(2);
        UserName = dr.getString(3);
        status = dr.getString(4);  
/*
  * For     : RickyO - display Customer Name | Phone | PickupId
  * Change  : JG uniCenta
  * Date    : May 2017          
*/            
        pickupid = dr.getString(5);        
        phone = dr.getString(6);

    }   

    /**
     *
     * @param dp
     * @throws BasicException
     */
    @Override
    public void writeValues(DataWrite dp) throws BasicException {
        dp.setString(1, id);
        dp.setString(2, name);
        dp.setString(3, UserName);
        dp.setString(4, status);        
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public String getAppUser() {
        return UserName;
    }
    
    public String getStatus() {
        return status;  
    }

/*
  * For     : RickyO - display Customer Name | Phone | PickupId
  * Change  : JG uniCenta
  * Date    : May 2017          
*/            
    public String getPhone() {
        return phone;
    }
/*
  * For     : RickyO - display Customer Name | Phone | PickupId
  * Change  : JG uniCenta
  * Date    : May 2017          
*/                
    public String getPickupId() {
        return pickupid;
    }
}
