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

import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.printer.DeviceTicket;
import com.mx.kylgis.pos.scale.DeviceScale;
import com.mx.kylgis.pos.scanpal2.DeviceScanner;
import java.util.Date;

/**
 *
 * @author adrianromero
 */
public interface AppView {
    
    public DeviceScale getDeviceScale();
    public DeviceTicket getDeviceTicket();
    public DeviceScanner getDeviceScanner();
    public Session getSession();
    public AppProperties getProperties();

    /**
     *
     * @param beanfactory
     * @return
     * @throws BeanFactoryException
     */
    public Object getBean(String beanfactory) throws BeanFactoryException;
     
    /**
     *
     * @param value
     * @param iSeq
     * @param dStart
     * @param dEnd
     */
    public void setActiveCash(String value, int iSeq, Date dStart, Date dEnd);
    public String getActiveCashIndex();
    public int getActiveCashSequence();
    public Date getActiveCashDateStart();
    public Date getActiveCashDateEnd();

    public void setClosedCash(String value, int iSeq, Date dStart, Date dEnd);    
    public String getClosedCashIndex();    
    public int getClosedCashSequence();    
    public Date getClosedCashDateStart();
    public Date getClosedCashDateEnd();

    public String getInventoryLocation();
    
    public void waitCursorBegin();
    public void waitCursorEnd();
    public AppUserView getAppUserView();

}

