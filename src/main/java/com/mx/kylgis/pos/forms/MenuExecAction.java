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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;

/**
 *
 * @author adrianromero
 */
public class MenuExecAction extends AbstractAction {

    private final AppView m_App;
    private final String m_sMyView;
    
    /** Creates a new instance of MenuExecAction
     * @param app
     * @param icon
     * @param keytext
     * @param sMyView */
    public MenuExecAction(AppView app, String icon, String keytext, String sMyView) {
        putValue(Action.SMALL_ICON, new ImageIcon(JPrincipalApp.class.getResource(icon)));
        putValue(Action.NAME, AppLocal.getIntString(keytext));
        putValue(AppUserView.ACTION_TASKNAME, sMyView);
        m_App = app;
        m_sMyView = sMyView;
    }
    @Override
    public void actionPerformed(ActionEvent evt) {
        m_App.getAppUserView().executeTask(m_sMyView);            
    }     
}
