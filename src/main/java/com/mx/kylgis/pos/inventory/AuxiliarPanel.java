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
import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.model.*;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.panels.AuxiliarFilter;
import com.mx.kylgis.pos.panels.JPanelTable2;
import com.mx.kylgis.pos.ticket.ProductInfoExt;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author jaroslawwozniak
 * @author adrianromero
 */
public class AuxiliarPanel extends JPanelTable2 {

    private AuxiliarEditor editor;
    private AuxiliarFilter filter;

    /**
     *
     */
    @Override
    protected void init() {  
        
        filter = new AuxiliarFilter();
        filter.init(app);
        filter.addActionListener(new ReloadActionListener());
        
        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field("PRODUCT1", Datas.STRING, Formats.STRING),
                new Field("PRODUCT2", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.prodref"), Datas.STRING, Formats.STRING, true, true, true),
                new Field(AppLocal.getIntString("label.prodbarcode"), Datas.STRING, Formats.STRING, false, true, true),
                new Field(AppLocal.getIntString("label.prodname"), Datas.STRING, Formats.STRING, true, true, true)
        );        
        Table table = new Table(
                "products_com",
                new PrimaryKey("ID"),
                new Column("PRODUCT"),
                new Column("PRODUCT2"));
         
        lpr = row.getListProvider(app.getSession(), 
                "SELECT COM.ID, COM.PRODUCT, COM.PRODUCT2, P.REFERENCE, P.CODE, P.NAME " +
                "FROM products_com COM, products P " +
                "WHERE COM.PRODUCT2 = P.ID AND COM.PRODUCT = ?", filter);
        spr = row.getSaveProvider(app.getSession(), table);              
        
        editor = new AuxiliarEditor(app, dirty);
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        filter.activate();
        
        //super.activate();
        startNavigation();
        reload(filter);
    }

    /**
     *
     * @return
     */
    @Override
    public Component getFilter(){
        return filter.getComponent();
    }
    
    /**
     *
     * @return
     */
    @Override
    public EditorRecord getEditor() {
        return editor;
    }

    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Auxiliar");
    } 
    
    private void reload(AuxiliarFilter filter) throws BasicException {
        ProductInfoExt prod = filter.getProductInfoExt();
        editor.setInsertProduct(prod); // must be set before load
        bd.setEditable(prod != null);
        bd.actionLoad();
    }
            
    private class ReloadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                reload((AuxiliarFilter) e.getSource());
            } catch (BasicException w) {
            }
        }
    }
}
