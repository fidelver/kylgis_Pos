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
package com.mx.kylgis.pos.util;

import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

/**
 *
 * @author adrianromero
 */
public class ReportUtils {
    
    private ReportUtils() {
    }
    
    /**
     *
     * @param printername
     * @return
     */
    public static PrintService getPrintService(String printername) {
        
        // Initalize print service
        
        if (printername == null) {
            return PrintServiceLookup.lookupDefaultPrintService();       
        } else {
            
            if ("(Show dialog)".equals(printername)) {
                return null; // null means "you have to show the print dialog"
            } else if ("(Default)".equals(printername)) {
                return PrintServiceLookup.lookupDefaultPrintService(); 
            } else {
                PrintService[] pservices = 
                        PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE , null);
                for (PrintService s : pservices) {    
                    if (printername.equals(s.getName())) {
                        return s;
                    }
                }
                return PrintServiceLookup.lookupDefaultPrintService();       
            }                
        }                 
    }
    
    /**
     *
     * @return
     */
    public static String[] getPrintNames() {
        PrintService[] pservices = 
                PrintServiceLookup.lookupPrintServices(DocFlavor.SERVICE_FORMATTED.PRINTABLE , null);
        
        String printers[] = new String[pservices.length];
        for (int i = 0; i < pservices.length; i++) {    
            printers[i] = pservices[i].getName();
        }
        
        return printers;
    }

}
