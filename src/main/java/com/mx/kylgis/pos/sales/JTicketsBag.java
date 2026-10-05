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

import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurantMap;
import com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurant;
import com.mx.kylgis.pos.sales.shared.JTicketsBagShared;
import com.mx.kylgis.pos.sales.simple.JTicketsBagSimple;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 *
 * @author JG uniCenta
 */
public abstract class JTicketsBag extends JPanel {
    
    /**
     *
     */
    protected AppView m_App;     

    /**
     *
     */
    protected DataLogicSales m_dlSales;

    /**
     *
     */
    protected TicketsEditor m_panelticket;    
    
    /** Creates new form JTicketsBag
     * @param oApp
     * @param panelticket */
    public JTicketsBag(AppView oApp, TicketsEditor panelticket) {        
        m_App = oApp;     
        m_panelticket = panelticket;        
        m_dlSales = (DataLogicSales) m_App.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
    }
    
    public abstract void activate();
    public abstract boolean deactivate();
    public abstract void deleteTicket();
    protected abstract JComponent getBagComponent();
    protected abstract JComponent getNullComponent();
    
    /**
     *
     * @param sName
     * @param app
     * @param panelticket
     * @return
     */
    public static JTicketsBag createTicketsBag(String sName, AppView app, TicketsEditor panelticket) {
        switch (sName) {
            case "standard":
                // return new JTicketsBagMulti(oApp, user, panelticket);
                return new JTicketsBagShared(app, panelticket);
            case "restaurant":
                return new JTicketsBagRestaurantMap(app, panelticket);
            default:
                // "simple"
                return new JTicketsBagSimple(app, panelticket);
        }
    }   
}