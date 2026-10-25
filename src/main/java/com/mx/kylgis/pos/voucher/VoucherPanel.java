

package com.mx.kylgis.pos.voucher;

import com.mx.kylgis.pos.data.loader.Datas;
import com.mx.kylgis.pos.data.model.*;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.panels.JPanelTable2;
import java.awt.Component;
import javax.swing.JButton;

public class VoucherPanel extends JPanelTable2 {

    private VoucherEditor editor;    

    @Override
    protected void init() {  
        row = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.Number"), Datas.STRING, Formats.STRING, true, true, true),
                new Field(AppLocal.getIntString("label.customer"), Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.paymenttotal"), Datas.DOUBLE, Formats.DOUBLE),
                new Field(AppLocal.getIntString("label.status"), Datas.STRING, Formats.STRING),
                new Field("EXPIRES_AT", Datas.TIMESTAMP, Formats.TIMESTAMP)
                
        );        
        Table table = new Table(
                "vouchers",
                new PrimaryKey("ID"),
                new Column("VOUCHER_NUMBER"),
                new Column("CUSTOMER"),
                new Column("AMOUNT"),
                new Column("STATUS"),
                new Column("EXPIRES_AT"));
        lpr = row.getListProvider(app.getSession(), table);
        spr = row.getSaveProvider(app.getSession(), table);              
        
        editor = new VoucherEditor(dirty,app);
    }

   
    @Override
    public EditorRecord getEditor() {
        return editor;
    }  
    
    @Override
    public Component getToolbarExtras() {
        JButton campaigns = new JButton("Campañas");
        campaigns.setFocusable(false);
        campaigns.addActionListener(e -> VoucherCampaignDialog.showDialog(app, this));
        return campaigns;
    }

    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Vouchers");
    } 
}
