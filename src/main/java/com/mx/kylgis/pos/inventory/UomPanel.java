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

public class UomPanel extends JPanelTable {

     private TableDefinition tuom;
     private UomEditor jeditor;
     
    @Override
    protected void init() {
        DataLogicSales dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");           
        tuom = dlSales.getTableUom();
        jeditor = new UomEditor(app, dirty);   
    }

    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }

    @Override
    public ListProvider getListProvider() {
         return new ListProviderCreator(tuom);
    }

    @Override
    public SaveProvider getSaveProvider() {
        return new SaveProvider(tuom);  
    }
    
    @Override
    public Vectorer getVectorer() {
        return tuom.getVectorerBasic(new int[]{1});
    }
    

     @Override
    public ListCellRenderer getListCellRenderer() {
        return new ListCellRendererBasic(tuom.getRenderStringBasic(new int[]{1}));
    }
     
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Uom");
    }
    
}
