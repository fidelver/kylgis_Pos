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

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.monitor.*;


/**
 * @version    0.00, 25 September 2000
 * @author     DS,BA,SH
 * @modified   JG uniCenta October 2017
 */
public class uOWWatch
    implements DeviceMonitorEventListener {

    public static String ibuttonid;
   
    /**
     * Method main
    */
    public static void iButtonOn () {
        OneWireContainer owd;

        try {
            DSPortAdapter adapter = OneWireAccessProvider.getDefaultAdapter();

            adapter.setSearchAllDevices();
            adapter.targetAllFamilies();
            adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

            uOWWatch nw = new uOWWatch(adapter);
        } catch (Exception e) {

        }

        return;
    }

   /** Network Monitor instance */
    private DeviceMonitor dm;

    /**
     * Create a 1-Wire Network Watcher
     * @param  adapter for 1-Wire Network to monitor
    */
    public uOWWatch (DSPortAdapter adapter) {

        dm = new DeviceMonitor(adapter);

        try {
            dm.addDeviceMonitorEventListener(this);
        } catch (Exception e){ }

        Thread t = new Thread(dm);
        t.start();
    }

    /**
     *
    */
    public void killWatch() {
        dm.killMonitor();
    }

    /**
     * Arrival event as a NetworkMonitorEventListener
     * @param devt
    */
   @Override
    public void deviceArrival (DeviceMonitorEvent devt) {
        int i;
        
        for(i=0; i<devt.getDeviceCount(); i++) {
            ibuttonid = devt.getAddressAsStringAt(i);
        }        
    }

    /**
     * Depart event as a NetworkMonitorEventListener
     * @param devt
    */
    @Override
    public void deviceDeparture (DeviceMonitorEvent devt) {
        int i;
        
        for(i=0; i<devt.getDeviceCount(); i++)
            ibuttonid = "";
    }

    /**
     * Depart event as a NetworkMonitorEventListener
     * @param dexc
    */
   @Override
   public void networkException (DeviceMonitorException dexc) {

    }
   
    public static String getibuttonid (){
       return ibuttonid;
    }   
  
}
