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
package com.mx.kylgis.pos.epm;

/**
 *
 * @author Ali Safdar and Aneeqa Baber
 */
public class Break {

    private String m_sId;
    private String m_sName;
    private String m_sNotes;
    private boolean m_sVisible;

    /**
     *
     * @param id
     * @param name
     * @param notes
     * @param visible
     */
    public Break(String id, String name, String notes,  boolean visible) {
        m_sId = id;
        m_sName = name;
        m_sNotes = notes;
        m_sVisible = visible;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return m_sId;
    }

    /**
     *
     * @param Id
     */
    public void setId(String Id) {
        this.m_sId = Id;
    }

    /**
     *
     * @return
     */
    public String getName() {
        return m_sName;
    }

    /**
     *
     * @param Name
     */
    public void setName(String Name) {
        this.m_sName = Name;
    }

    /**
     *
     * @return
     */
    public String getNotes() {
        return m_sNotes;
    }

    /**
     *
     * @param Notes
     */
    public void setNotes(String Notes) {
        this.m_sNotes = Notes;
    }

    /**
     *
     * @return
     */
    public boolean isVisible() {
        return m_sVisible;
    }

    /**
     *
     * @param Visible
     */
    public void setVisible(boolean Visible) {
        this.m_sVisible = Visible;
    }
}
