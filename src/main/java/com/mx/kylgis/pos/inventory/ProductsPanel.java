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
import com.mx.kylgis.pos.data.user.EditorListener;
import com.mx.kylgis.pos.data.user.EditorRecord;
import com.mx.kylgis.pos.data.user.ListProviderCreator;
import com.mx.kylgis.pos.data.user.SaveProvider;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.panels.JPanelTable2;
import com.mx.kylgis.pos.ticket.ProductFilter;

import java.awt.Component;
import javax.swing.JButton;

/**
 *
 * @author JG uniCenta
 *
 */
public class ProductsPanel extends JPanelTable2 implements EditorListener {

    private ProductsEditor jeditor;
    private ProductFilter jproductfilter;         
    
    private DataLogicSales m_dlSales = null;
    
    public ProductsPanel() {
    }
    
    /**
     *
     */
    @Override
    protected void init() {   
        m_dlSales = (DataLogicSales) app.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        
        jproductfilter = new ProductFilter();     
        jproductfilter.init(app);

        row = m_dlSales.getProductsRow();

        lpr =  new ListProviderCreator(m_dlSales.getProductCatQBF(), jproductfilter);

        spr = new SaveProvider(
            m_dlSales.getProductCatUpdate(),
            m_dlSales.getProductCatInsert(),
            m_dlSales.getProductCatDelete());
        
        jeditor = new ProductsEditor(app, dirty);       
    }
    
    /**
     *
     * @return value
     */
    @Override
    public EditorRecord getEditor() {
        return jeditor;
    }
    
    /**
     *
     * @return value
     */
    @Override
    public Component getFilter() {
        return jproductfilter.getComponent();
    }

    /**
     *
     * @return btnScanPal
     */
    @Override
    public Component getToolbarExtras() {
        
        JButton btnScanPal = new JButton();
        btnScanPal.setText("ScanPal");
        btnScanPal.setVisible(app.getDeviceScanner() != null);
        btnScanPal.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnScanPalActionPerformed(evt);
            }
        });      
        
        return btnScanPal;
    }
    
    private void btnScanPalActionPerformed(java.awt.event.ActionEvent evt) {                                           
  
        JDlgUploadProducts.showMessage(this, app.getDeviceScanner(), bd);
    }

    /**
     *
     * @return value
     */
    @Override
    public String getTitle() {
        return AppLocal.getIntString("Menu.Products");
    }

    /**
     *
     * @throws BasicException
     */
    @Override
    public void activate() throws BasicException {
        
        jeditor.activate(); 
        jproductfilter.activate();
        
        super.activate();
    }

    /**
     *
     * @param value
     */
    @Override
    public void updateValue(Object value) {
    }    
}