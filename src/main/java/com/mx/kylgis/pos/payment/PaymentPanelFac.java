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

/**
 *
 * @author adrianromero
 */
public class PaymentPanelFac {
    
    /** Creates a new instance of PaymentPanelFac */
    private PaymentPanelFac() {
    }
    
    public static PaymentPanel getPaymentPanel(String sReader, JPaymentNotifier notifier) {
// JG 16 May 12 use switch
        switch (sReader) {
            case "Intelligent":
                return new PaymentPanelMagCard(new MagCardReaderIntelligent(), notifier);
            case "Generic":
                return new PaymentPanelMagCard(new MagCardReaderGeneric(), notifier);
            case "Keyboard":
                return new PaymentPanelType(notifier);
            case "EMV":
                return new PaymentPanelEMV(notifier);
            default:
           return new PaymentPanelBasic(notifier);
        }
    }      
}
