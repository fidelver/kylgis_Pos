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
package com.mx.kylgis.pos.mant;
import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.gui.ListCellRendererBasic;
import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.loader.TableDefinition;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.data.user.ListProvider;
import com.mx.kylgis.pos.data.user.ListProviderCreator;
import com.mx.kylgis.pos.data.user.SaveProvider;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.panels.JPanelTable;
import javax.swing.ListCellRenderer;

/**
 *
 * @author adrianromero
 */
public class JPanelPlaces extends JPanelTable {
    
    private TableDefinition tplaces;
    private PlacesEditor jeditor;
    
    /** Creates a new instance of JPanelPlaces */
    public JPanelPlaces() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {
        DataLogicSales dlSales = null;
        dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        
        tplaces = new TableDefinition(app.getSession(),
            "places"
            , new String[] {"ID", "NAME", "SEATS", "X", "Y", "FLOOR", "WIDTH", "HEIGHT"}
            , new String[] {"ID", AppLocal.getIntString("label.name"), 
                AppLocal.getIntString("label.seats"), "X", "Y", 
                AppLocal.getIntString("label.placefloor")}
            , new Datas[] {
                Datas.STRING, Datas.STRING, Datas.STRING, 
                Datas.INT, Datas.INT, 
                Datas.STRING,
                Datas.INT, Datas.INT }
            , new Formats[] {
                Formats.STRING, Formats.STRING, Formats.STRING, 
                Formats.INT, Formats.INT, 
                Formats.NULL,
                Formats.INT, Formats.INT }
            , new int[] {0}
        ); 
        jeditor = new PlacesEditor(dlSales, dirty); 
    }
        
    /**
     *
     * @return
     */
    @Override
    public ListProvider getListProvider() {
        return new ListProviderCreator(tplaces);
    }
    
    /**
     *
     * @return
     */
    @Override
    public SaveProvider getSaveProvider() {
        return new SaveProvider(tplaces);      
    }
    
    /**
     *
     * @return
     */
    @Override
    public Vectorer getVectorer() {
        return tplaces.getVectorerBasic(new int[]{1});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new ListCellRendererBasic(tplaces.getRenderStringBasic(new int[]{1}));
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
        return AppLocal.getIntString("Menu.Tables");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        jeditor.activate();
        super.activate();        
    }     
}