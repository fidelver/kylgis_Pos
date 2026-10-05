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

import com.mx.kylgis.pos.data.gui.ListCellRendererBasic;
import com.mx.kylgis.pos.data.loader.ComparatorCreator;
import com.mx.kylgis.pos.data.loader.TableDefinition;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.data.user.ListProvider;
import com.mx.kylgis.pos.data.user.ListProviderCreator;
import com.mx.kylgis.pos.data.user.SaveProvider;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.panels.JPanelTable;
import javax.swing.ListCellRenderer;

/**
 *
 * @author adrianromero
 */
public class LocationsPanel extends JPanelTable {
    
    private TableDefinition tlocations;
    private LocationsView jeditor;
    
    /** Creates a new instance of LocationsPanel */
    public LocationsPanel() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {   
        DataLogicSales dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");          
        tlocations = dlSales.getTableLocations();
        jeditor = new LocationsView(dirty);
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListProvider getListProvider() {
        return new ListProviderCreator(tlocations);
    }
    
    /**
     *
     * @return
     */
    @Override
    public SaveProvider getSaveProvider() {
        return new SaveProvider(tlocations);        
    }
    
    /**
     *
     * @return
     */
    @Override
    public Vectorer getVectorer() {
        return tlocations.getVectorerBasic(new int[]{1, 2});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ComparatorCreator getComparatorCreator() {
        return tlocations.getComparatorCreator(new int[] {1, 2});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new ListCellRendererBasic(tlocations.getRenderStringBasic(new int[]{1}));
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
        return AppLocal.getIntString("Menu.Locations");
    }      
}
