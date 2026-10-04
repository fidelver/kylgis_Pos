//    uniCenta oPOS  - Touch Friendly Point Of Sale
//    Copyright (c) 2009-2018 uniCenta & previous Openbravo POS works
//    https://unicenta.com
//
//    This file is part of uniCenta oPOS
//
//    uniCenta oPOS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//   uniCenta oPOS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with uniCenta oPOS.  If not, see <http://www.gnu.org/licenses/>.

package com.mx.kylgis.pos.admin;

import com.mx.kylgis.pos.data.gui.ListCellRendererBasic;
import com.mx.kylgis.pos.data.loader.ComparatorCreator;
import com.mx.kylgis.pos.data.loader.TableDefinition;
import com.mx.kylgis.pos.data.loader.Vectorer;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.data.user.ListProvider;
import com.mx.kylgis.pos.data.user.ListProviderCreator;
import com.mx.kylgis.pos.data.user.SaveProvider;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSystem;
import com.mx.kylgis.pos.panels.JPanelTable;
import javax.swing.ListCellRenderer;

/**
 *
 * @author adrianromero
 */
public class ResourcesPanel extends JPanelTable {

    private TableDefinition tresources;
    private ResourcesView jeditor;
    
    /** Creates a new instance of JPanelResources */
    public ResourcesPanel() {
    }
    
    /**
     *
     */
    protected void init() {
        DataLogicAdmin dlAdmin = (DataLogicAdmin) app.getBean("com.mx.kylgis.pos.admin.DataLogicAdmin"); 
        tresources = dlAdmin.getTableResources();         
        jeditor = new ResourcesView(dirty);           
    }

    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {
        if (super.deactivate()) {
            DataLogicSystem dlSystem = (DataLogicSystem) app.getBean("com.mx.kylgis.pos.forms.DataLogicSystem");            
            dlSystem.resetResourcesCache();
            return true;
        } else {
            return false;
        }    
    }
    
    /**
     *
     * @return
     */
    public ListProvider getListProvider() {
        return new ListProviderCreator(tresources);
    }
    
    /**
     *
     * @return
     */
    public SaveProvider getSaveProvider() {
        return new SaveProvider(tresources);        
    }
    
    /**
     *
     * @return
     */
    @Override
    public Vectorer getVectorer() {
        return tresources.getVectorerBasic(new int[] {1});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ComparatorCreator getComparatorCreator() {
        return tresources.getComparatorCreator(new int[] {1, 2});
    }
    
    /**
     *
     * @return
     */
    @Override
    public ListCellRenderer getListCellRenderer() {
        return new ListCellRendererBasic(tresources.getRenderStringBasic(new int[] {1}));
    }
    
    /**
     *
     * @return
     */
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     *
     * @return
     */
    public String getTitle() {
        return AppLocal.getIntString("Menu.Resources");
    }        
}
