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
package com.mx.kylgis.pos.admin;

import com.mx.kylgis.pos.data.loader.*;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.BeanFactoryDataSingle;

/**
 *
 * @author adrianromero
 */
public class DataLogicAdmin extends BeanFactoryDataSingle {
    
    private Session s;
    private TableDefinition m_tpeople;
    private TableDefinition m_troles;
    private TableDefinition m_tresources;    
    
    
    /** Creates a new instance of DataLogicAdmin */
    public DataLogicAdmin() {
    }
    
    /**
     *
     * @param s
     */
    @Override
    public void init(Session s){
        this.s = s;
        
        m_tpeople = new TableDefinition(s,
            "people"
            , new String[] {"ID", "NAME", "APPPASSWORD", "ROLE", "VISIBLE", "CARD", "IMAGE"}
            , new String[] {"ID", AppLocal.getIntString("label.peoplename"), AppLocal.getIntString("label.Password"), AppLocal.getIntString("label.role"), AppLocal.getIntString("label.peoplevisible"), AppLocal.getIntString("label.card"), AppLocal.getIntString("label.peopleimage")}
            , new Datas[] {Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.BOOLEAN, Datas.STRING, Datas.IMAGE}
            , new Formats[] {Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.BOOLEAN, Formats.STRING, Formats.NULL}
            , new int[] {0}
        );   
                        
        m_troles = new TableDefinition(s,
            "roles"
            , new String[] {"ID", "NAME", "PERMISSIONS"}
            , new String[] {"ID", AppLocal.getIntString("label.name"), "PERMISSIONS"}
            , new Datas[] {Datas.STRING, Datas.STRING, Datas.BYTES}
            , new Formats[] {Formats.STRING, Formats.STRING, Formats.NULL}
            , new int[] {0}
        );  
        
        m_tresources = new TableDefinition(s,
            "resources"
            , new String[] {
                "ID", "NAME", "RESTYPE", "CONTENT"}
            , new String[] {
                "ID", 
                AppLocal.getIntString("label.name"), 
                AppLocal.getIntString("label.type"), 
                "CONTENT"}
            , new Datas[] {
                Datas.STRING, Datas.STRING, Datas.INT, Datas.BYTES}
            , new Formats[] {
                Formats.STRING, Formats.STRING, Formats.INT, Formats.NULL}
            , new int[] {0}
        );           
    }
       
    /**
     *
     * @return
     */
    public final SentenceList getRolesList() {
        return new StaticSentence(s
            , "SELECT ID, NAME FROM roles ORDER BY NAME"
            , null
            , new SerializerReadClass(RoleInfo.class));
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTablePeople() {
        return m_tpeople;
    }    

    /**
     *
     * @return
     */
    public final TableDefinition getTableRoles() {
        return m_troles;
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTableResources() {
        return m_tresources;
    }
    
    /**
     *
     * @return
     */
    public final SentenceList getPeopleList() {
        return new StaticSentence(s
                , "SELECT ID, NAME FROM people ORDER BY NAME"
                , null
                , new SerializerReadClass(PeopleInfo.class));
    }
}
