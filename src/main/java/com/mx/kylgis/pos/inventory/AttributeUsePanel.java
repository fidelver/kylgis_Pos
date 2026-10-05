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
import com.mx.kylgis.pos.panels.JPanelTable2;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 * @author adrianromero
 */
public class AttributeUsePanel extends JPanelTable2 {

    private AttributeUseEditor editor;
    private AttributeSetFilter filter;

    /**
     *
     */
    @Override
    protected void init() {

        filter = new AttributeSetFilter();
        filter.init(app);
        filter.addActionListener(new ReloadActionListener());

        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field("ATRIBUTESET_ID", Datas.STRING, Formats.STRING),
                new Field("ATTRIBUTE_ID", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.order"), Datas.INT, Formats.INT, false, true, true),
                new Field(AppLocal.getIntString("label.name"), Datas.STRING, Formats.STRING, true, true, true)
        );

        Table table = new Table(
                "attributeuse",
                new PrimaryKey("ID"),
                new Column("attributeset_ID"),
                new Column("ATTRIBUTE_ID"),
                new Column("LINENO"));

        lpr = row.getListProvider(app.getSession(),
                "SELECT ATTUSE.ID, ATTUSE.attributeset_ID, ATTUSE.ATTRIBUTE_ID, ATTUSE.LINENO, ATT.NAME " +
                "FROM attributeuse ATTUSE, attribute ATT " +
                "WHERE ATTUSE.ATTRIBUTE_ID = ATT.ID AND ATTUSE.attributeset_ID = ? ORDER BY LINENO", filter);
        spr = row.getSaveProvider(app.getSession(), table);

        editor = new AttributeUseEditor(app, dirty);
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        filter.activate();
        editor.activate();

        //super.activate();
        startNavigation();
        reload();
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

    private void reload() throws BasicException {

        String attsetid = (String) filter.createValue();
        editor.setInsertId(attsetid); // must be set before load
        bd.setEditable(attsetid != null);
        bd.actionLoad();
    }

    /**
     *
     * @return
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.AttributeUse");
    }

    private class ReloadActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                reload();
            } catch (BasicException w) {
            }
        }
    }
}
