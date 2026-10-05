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
package com.mx.kylgis.pos.data.user;

import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.beans.*;

/**
 *
 * @author  adrian
 */
public class DirtyManager implements DocumentListener, ChangeListener, ActionListener, PropertyChangeListener {
    
    private boolean m_bDirty;    

    /**
     *
     */
    protected Vector listeners = new Vector();
    
    /** Creates a new instance of DirtyManager */
    public DirtyManager() {
        m_bDirty = false;
    }
    
    /**
     *
     * @param l
     */
    public void addDirtyListener(DirtyListener l) {
        listeners.add(l);
    }

    /**
     *
     * @param l
     */
    public void removeDirtyListener(DirtyListener l) {
        listeners.remove(l);
    }

    /**
     *
     */
    protected void fireChangedDirty() {
        
        Enumeration e = listeners.elements();
        while (e.hasMoreElements()) {
            DirtyListener l = (DirtyListener) e.nextElement();
            l.changedDirty(m_bDirty);
        }
    }
    
    /**
     *
     * @param bValue
     */
    public void setDirty(boolean bValue) {
        
        if (m_bDirty != bValue) {
            m_bDirty = bValue;
            fireChangedDirty();
        }
    }

    /**
     *
     * @return
     */
    public boolean isDirty() {
        return m_bDirty;
    }
    
    @Override
    public void changedUpdate(DocumentEvent e) {
        setDirty(true);
    }
    @Override
    public void insertUpdate(DocumentEvent e) {
        setDirty(true);
    }    
    @Override
    public void removeUpdate(DocumentEvent e) {
        setDirty(true);
    }    
    
    @Override
    public void stateChanged(ChangeEvent e) {
        setDirty(true);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        setDirty(true);
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        //if ("image".equals(evt.getPropertyName())) {
            setDirty(true);
        //}
    }
    
}
