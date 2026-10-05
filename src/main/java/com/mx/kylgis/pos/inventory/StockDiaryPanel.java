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
package com.mx.kylgis.pos.inventory;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.data.user.ListProvider;
import com.mx.kylgis.pos.data.user.SaveProvider;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSales;
//import com.mx.kylgis.pos.suppliers.DataLogicSuppliers;
import com.mx.kylgis.pos.panels.JPanelTable;
import com.mx.kylgis.pos.suppliers.DataLogicSuppliers;

/**
 *
 * @author adrianromero
 */
public class StockDiaryPanel extends JPanelTable {
    
    private StockDiaryEditor jeditor;    
    private DataLogicSales m_dlSales;
//    private DataLogicSuppliers m_dlSuppliers;
    
    /** Creates a new instance of JPanelDiaryEditor */
    public StockDiaryPanel() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {
        m_dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
//        DataLogicSuppliers m_dlSuppliers = (DataLogicSuppliers) app.getBean("com.mx.kylgis.pos.suppliers.DataLogicSuppliers");        
        jeditor = new StockDiaryEditor(app, dirty); 
       
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListProvider getListProvider() {
        return null;
    }
    
    /**
     *
     * @return
     */
    @Override
    public SaveProvider getSaveProvider() {
        return  new SaveProvider(null
                , m_dlSales.getStockDiaryInsert()
                , m_dlSales.getStockDiaryDelete());      
    }
    
    /**
     *
     * @return
     */
    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.StockDiary");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        jeditor.activate(); // primero activo el editor 
        super.activate();   // segundo activo el padre        
    } 
}
