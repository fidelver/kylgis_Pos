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

/**
 *
 * @author JG uniCenta
 */
public class TicketTaxInfo {
    
    private TaxInfo tax;
    
    private double subtotal;
    private double taxtotal;
            
    /** Creates a new instance of TicketTaxInfo
     * @param tax */
    public TicketTaxInfo(TaxInfo tax) {
        this.tax = tax;
        
        subtotal = 0.0;
        taxtotal = 0.0;
    }
    
    /**
     *
     * @return
     */
    public TaxInfo getTaxInfo() {
        return tax;
    }
    
    /**
     *
     * @param dValue
     */
    public void add(double dValue) {
        subtotal += dValue;
        taxtotal = subtotal * tax.getRate();
    }
    
    /**
     *
     * @return
     */
    public double getSubTotal() {    
        return subtotal;
    }
    
    /**
     *
     * @return
     */
    public double getTax() {       
        return taxtotal;
    }
    
    /**
     *
     * @return
     */
    public double getTotal() {         
        return subtotal + taxtotal;
    }
    
    /**
     *
     * @return
     */
    public String printSubTotal() {
        return Formats.CURRENCY.formatValue(getSubTotal());
    }

    /**
     *
     * @return
     */
    public String printTax() {
        return Formats.CURRENCY.formatValue(getTax());
    }    

    /**
     *
     * @return
     */
    public String printTotal() {
        return Formats.CURRENCY.formatValue(getTotal());
    }    
}
