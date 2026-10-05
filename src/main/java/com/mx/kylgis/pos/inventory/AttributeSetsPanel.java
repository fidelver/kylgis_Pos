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

import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.model.*;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.panels.JPanelTable2;

/**
 *
 * @author adrianromero
 */
public class AttributeSetsPanel extends JPanelTable2 {

    private EditorRecord editor;

    /** Creates a new instance of JPanelCategories */
    public AttributeSetsPanel() {
    }

    /**
     *
     */
    @Override
    protected void init() {

        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.name"), Datas.STRING, Formats.STRING, true, true, true)
        );

        Table table = new Table(
                "attributeset",
                new PrimaryKey("ID"),
                new Column("NAME"));

        lpr = row.getListProvider(app.getSession(), table);
        spr = row.getSaveProvider(app.getSession(), table);

        editor = new AttributeSetsEditor(dirty);
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
        return AppLocal.getIntString("Menu.AttributeSets");
    }
}
