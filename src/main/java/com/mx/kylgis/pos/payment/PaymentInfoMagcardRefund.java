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

public class PaymentInfoMagcardRefund extends PaymentInfoMagcard {

/**
     * Creates a new instance of PaymentInfoMagcardRefund
 */
    public PaymentInfoMagcardRefund(String sHolderName, String sCardNumber, String sExpirationDate, String track1, String track2,
            String track3, String encryptCard, String encryptKey, String sTransactionID, double dTotal) {
        super(sHolderName, sCardNumber, sExpirationDate, track1, track2, track3, encryptCard, encryptKey, sTransactionID, dTotal);
    }
    
    /**
     * Creates a new instance of PaymentInfoMagcard
     */
    public PaymentInfoMagcardRefund(String sHolderName, String sCardNumber, String sExpirationDate, String sTransactionID, String encryptedCard, String encryptionKey, double dTotal) {
        super(sHolderName, sCardNumber, sExpirationDate, sTransactionID, dTotal);
    }
    
    @Override
    public PaymentInfo copyPayment() {
        PaymentInfoMagcard p = new PaymentInfoMagcardRefund(m_sHolderName, m_sCardNumber, m_sExpirationDate,
                track1, track2, track3, encryptedTrack, encryptionKey, m_sTransactionID, m_dTotal);
        p.m_sAuthorization = m_sAuthorization;
        p.m_sErrorMessage = m_sErrorMessage;
        return p;
    }

    @Override
    public String getName() {
//        return "magcardrefund";
        return "ccardrefund";        
    }    
}
