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
package com.mx.kylgis.pos.payment;

public class PaymentInfoFree extends PaymentInfo {
    
    private double m_dTotal;
    private double m_dTendered;
    private String m_dCardName =null;
//    private double m_dTip;    
   
    /** Creates a new instance of PaymentInfoFree
     * @param dTotal */
    public PaymentInfoFree(double dTotal) {
        m_dTotal = dTotal;
    }

    public PaymentInfo copyPayment(){
        return new PaymentInfoFree(m_dTotal);
    }    
    public String getTransactionID(){
        return "no ID";
    }
    public String getName() {
        return "free";
    }   
    public double getTotal() {
        return m_dTotal;
    }
    public double getPaid() {
        return (0.0); 
    }

/**
 * 
    public double getTip() {
        return m_dTip;
    }
*/
    
    public double getChange(){
       return (0.00);
   }
    public double getTendered() {
       return m_dTendered;
   }
    public String getCardName() {
       return m_dCardName;
   } 

/**    
    public boolean getIsProcessed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setIsProcessed(boolean value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getReturnMessage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
*/
    
    public String getVoucher() {
        return null;
    }    

}
