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
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.data.loader.*;
import com.mx.kylgis.pos.data.model.Field;
import com.mx.kylgis.pos.data.model.Row;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.catalog.CategoryStock;
import com.mx.kylgis.pos.customers.CustomerInfoExt;
import com.mx.kylgis.pos.customers.CustomerTransaction;
import com.mx.kylgis.pos.inventory.*;
import com.mx.kylgis.pos.mant.FloorsInfo;
import com.mx.kylgis.pos.payment.PaymentInfo;
import com.mx.kylgis.pos.payment.PaymentInfoTicket;
import com.mx.kylgis.pos.sales.ReprintTicketInfo;
import com.mx.kylgis.pos.suppliers.SupplierInfo;
import com.mx.kylgis.pos.suppliers.SupplierInfoExt;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import com.mx.kylgis.pos.voucher.VoucherInfo;
import com.mx.kylgis.pos.voucher.VoucherEditContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Logger;
import java.io.File;

/**
 *
 * @author adrianromero
 * @author jackgerrard
 */
public class DataLogicSales extends BeanFactoryDataSingle {

    protected Session s;

    protected Datas[] auxiliarDatas;
    protected Datas[] stockdiaryDatas;
    protected Datas[] paymenttabledatas;
    protected Datas[] stockdatas;
    protected Datas[] stockAdjustDatas;
    
    protected Row productsRow;
    protected Row customersRow;

    private String pName;
    private Double getTotal;
    private Double getTendered;
    private String getRetMsg;    
    private String getVoucher;    

    public static final String DEBT = "debt";
    public static final String DEBT_PAID = "debtpaid";
    protected static final String PREPAY = "prepay";
    private static final Logger logger = Logger.getLogger("com.mx.kylgis.pos.forms.DataLogicSales");

    private String getCardName;
    protected SentenceExec m_createCat;
    protected SentenceExec m_createSupp;

    protected AppView m_App;    
    
    private AppConfig m_config;
        
    public DataLogicSales() {
        AppView app = null;        
        m_config = AppConfig.getInstance();
        m_App = app;        
        
        stockdiaryDatas = new Datas[] {
            Datas.STRING, Datas.TIMESTAMP, Datas.INT, Datas.STRING, 
            Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.DOUBLE, 
            Datas.STRING, Datas.STRING, Datas.STRING};
        
        paymenttabledatas = new Datas[] {
            Datas.STRING, Datas.STRING, Datas.TIMESTAMP, 
            Datas.STRING, Datas.STRING, Datas.DOUBLE, 
            Datas.STRING};
        
        stockdatas = new Datas[] {
            Datas.STRING, Datas.STRING, Datas.STRING, 
            Datas.DOUBLE, Datas.DOUBLE, Datas.DOUBLE};
        
        stockAdjustDatas = new Datas[] {
            Datas.STRING,
            Datas.STRING,
            Datas.STRING,
            Datas.DOUBLE};
        
        auxiliarDatas = new Datas[] {
            Datas.STRING, Datas.STRING, Datas.STRING, 
            Datas.STRING, Datas.STRING, Datas.STRING};

        productsRow = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.prodref"), Datas.STRING, Formats.STRING, true, true, true),
                new Field(AppLocal.getIntString("label.prodbarcode"), Datas.STRING, Formats.STRING, false, true, true),
                new Field(AppLocal.getIntString("label.prodbarcodetype"), Datas.STRING, Formats.STRING, false, true, true),
                new Field(AppLocal.getIntString("label.prodname"), Datas.STRING, Formats.STRING, true, true, true),
                new Field(AppLocal.getIntString("label.prodpricebuy"), Datas.DOUBLE, Formats.CURRENCY, false, true, true),
                new Field(AppLocal.getIntString("label.prodpricesell"), Datas.DOUBLE, Formats.CURRENCY, false, true, true),
                new Field(AppLocal.getIntString("label.prodcategory"), Datas.STRING, Formats.STRING, false, false, true),
                new Field(AppLocal.getIntString("label.taxcategory"), Datas.STRING, Formats.STRING, false, false, true),
                new Field(AppLocal.getIntString("label.attributeset"), Datas.STRING, Formats.STRING, false, false, true),
                new Field("STOCKCOST", Datas.DOUBLE, Formats.CURRENCY),
                new Field("STOCKVOLUME", Datas.DOUBLE, Formats.DOUBLE),
                new Field("IMAGE", Datas.IMAGE, Formats.NULL),
                new Field("ISCOM", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("ISSCALE", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("ISCONSTANT", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("PRINTKB", Datas.BOOLEAN, Formats.BOOLEAN),                                                     
                new Field("SENDSTATUS", Datas.BOOLEAN, Formats.BOOLEAN),                                                  
                new Field("ISSERVICE", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("PROPERTIES", Datas.BYTES, Formats.NULL),
                new Field(AppLocal.getIntString("label.display"), Datas.STRING, Formats.STRING, false, true, true),
                new Field("ISVPRICE", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("ISVERPATRIB", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("TEXTTIP", Datas.STRING, Formats.STRING),
                new Field("WARRANTY", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field(AppLocal.getIntString("label.stockunits"), Datas.DOUBLE, Formats.DOUBLE),                  
                new Field("PRINTTO", Datas.STRING, Formats.STRING),
                new Field(AppLocal.getIntString("label.prodsupplier"), Datas.STRING, Formats.STRING, false, false, true),
                new Field(AppLocal.getIntString("label.UOM"), Datas.STRING, Formats.STRING),
                new Field("MEMODATE", Datas.TIMESTAMP, Formats.DATE),
                
                new Field("ISCATALOG", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("CATORDER", Datas.INT, Formats.INT)
        );
        
// creating customers object here for now for future global reuse
// LOYALTY, MEMBERSHIP & etc as will be more system centric than customer  
        
        customersRow = new Row(
                new Field("ID", Datas.STRING, Formats.STRING),
                new Field("SEARCHKEY", Datas.STRING, Formats.STRING),
                new Field("TAXID", Datas.STRING, Formats.STRING),
                new Field("NAME", Datas.STRING, Formats.STRING),
                new Field("TAXCATEGORY", Datas.STRING, Formats.STRING),
                new Field("CARD", Datas.STRING, Formats.STRING),
                new Field("MAXDEBT", Datas.DOUBLE, Formats.CURRENCY),
                new Field("ADDRESS", Datas.STRING, Formats.STRING),
                new Field("ADDRESS2", Datas.STRING, Formats.STRING),
                new Field("POSTAL", Datas.STRING, Formats.STRING),
                new Field("CITY", Datas.STRING, Formats.STRING),
                new Field("REGION", Datas.STRING, Formats.STRING),
                new Field("COUNTRY", Datas.STRING, Formats.STRING),
                new Field("FIRSTNAME", Datas.STRING, Formats.STRING),
                new Field("LASTNAME", Datas.STRING, Formats.STRING),
                new Field("EMAIL", Datas.STRING, Formats.STRING),
                new Field("PHONE", Datas.STRING, Formats.STRING),                                                     
                new Field("PHONE2", Datas.STRING, Formats.STRING),
                new Field("FAX", Datas.STRING, Formats.STRING),
                new Field("NOTES", Datas.STRING, Formats.STRING),
                new Field("VISIBLE", Datas.BOOLEAN, Formats.BOOLEAN),                
                new Field("CURDATE", Datas.STRING, Formats.TIMESTAMP),
                new Field("CURDEBT", Datas.DOUBLE, Formats.CURRENCY),                
                new Field("IMAGE", Datas.BYTES, Formats.NULL),
                new Field("ISVIP", Datas.BOOLEAN, Formats.BOOLEAN),
                new Field("DISCOUNT", Datas.DOUBLE, Formats.CURRENCY),             
                new Field("MEMODATE", Datas.STRING, Formats.TIMESTAMP)                
        );        
        
    }

    /**
     *
     * @param s
     */
    @Override
    public void init(Session s){
        this.s = s;

        m_createCat = new StaticSentence(s, 
                "INSERT INTO categories ( ID, NAME, CATSHOWNAME ) "
                + "VALUES (?, ?, ?)"
                , new SerializerWriteBasic(new Datas[]{
                    Datas.STRING,
                    Datas.STRING,
                Datas.BOOLEAN})
        );    
        
        m_createSupp = new StaticSentence(s,
                "INSERT INTO suppliers ( ID, NAME, SEARCHKEY, VISIBLE ) "
                + "VALUES (?, ?, ?, ?)"
                , new SerializerWriteBasic(new Datas[]{
                    Datas.STRING,
                    Datas.STRING,
                    Datas.STRING,                    
                    Datas.BOOLEAN})
        ); 
    }
    
    /**
     * Creates an isolated read-only data-logic instance for background product
     * searches. Its JDBC connection is never shared with the POS session.
     */
    public final ProductSearchContext openProductSearchContext() throws BasicException {
        try {
            Session searchSession = s.forkReadOnly();
            DataLogicSales searchSales = new DataLogicSales();
            searchSales.init(searchSession);
            return new ProductSearchContext(searchSession, searchSales);
        } catch (SQLException ex) {
            throw new BasicException("Unable to open read-only product search session.", ex);
        }
    }

    public static final class ProductSearchContext implements AutoCloseable {
        private final Session session;
        private final DataLogicSales sales;

        private ProductSearchContext(Session session, DataLogicSales sales) {
            this.session = session;
            this.sales = sales;
        }

        public DataLogicSales getSales() {
            return sales;
        }

        @Override
        public void close() {
            session.close();
        }
    }

// Import Creates
    public final void createCategory(Object[] category) throws BasicException {
        m_createCat.exec(category);
    }

    public final void createSupplier(Object[] supplier) throws BasicException {
        m_createSupp.exec(supplier);
    } 
// End Import Creates
    
    public final Row getProductsRow() {
        return productsRow;
    }
    
    public final Row getCustomersRow() {
        return customersRow;
    }    
    
    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public final ProductInfoExt getProductInfo(String id) throws BasicException {
	return (ProductInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "
                + "UOM, "
                + "MEMODATE "        
                + "FROM products WHERE ID = ?"
		, SerializerWriteString.INSTANCE
		, ProductInfoExt.getSerializerRead()).find(id);
    }

    public final ProductInfoExt getProductInfoByCode(String sCode) throws BasicException {
//        if (sCode.length() == 13 && (sCode.startsWith("2") || sCode.startsWith("02"))) 
//            return  getProductInfoByShortCode(sCode);
//        else {        
            return (ProductInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "
                + "UOM, "
                + "MEMODATE "
		+ "FROM products WHERE CODE = ?"
		, SerializerWriteString.INSTANCE
		, ProductInfoExt.getSerializerRead()).find(sCode);
//        }
    }

    public final ProductInfoExt getProductInfoByShortCode(String sCode) throws BasicException {

        return (ProductInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "
                + "UOM, "
                + "MEMODATE "
		+ "FROM products "
                + "WHERE SUBSTRING( CODE, 3, 6 ) = ?"                
            , SerializerWriteString.INSTANCE
                , ProductInfoExt.getSerializerRead()).find(sCode.substring(2, 8));
    } 
       
/*
*  Important Note: 
*  Deliberately extracted from other code to force strict UPC-A (full 12 digits)
*  Why? Because other manf' or in-store codes may exist and we just need a single
*  record returned. Also, handling things this way will allow use (future) of a
*  COUPON code (5 or 9 normally used) in-store
*
*/    
    public final ProductInfoExt getProductInfoByUShortCode(String sCode) throws BasicException {

        return (ProductInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "
                + "UOM, "
                + "MEMODATE "
                + "FROM products "
                + "WHERE LEFT( CODE, 7 ) = ? AND CODETYPE = 'UPC-A' "                
//  selection of 7 digits ie: 2123456 specific to allow for other 12 digit
//  codes that may be in use at positions 234567
//  last digit (position 7) can be used to identify COUPON (5 or 9) - FUTURE              
            , SerializerWriteString.INSTANCE
                , ProductInfoExt.getSerializerRead())
                .find(sCode.substring(0, 7));
    } 
    
    /**
     *
     * @param sReference
     * @return
     * @throws BasicException
     */
    public final ProductInfoExt getProductInfoByReference(String sReference) throws BasicException {
	return (ProductInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "
                + "UOM, "
                + "MEMODATE "
		+ "FROM products WHERE REFERENCE = ?"
		, SerializerWriteString.INSTANCE
		, ProductInfoExt.getSerializerRead()).find(sReference);
    }

    /**
     *
     * @return
     * @throws BasicException
     */
        public final List<CategoryInfo> getRootCategories() throws BasicException {
        return new PreparedSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, "
                + "CATORDER "                    
                + "FROM categories "
                + "WHERE PARENTID IS NULL AND CATSHOWNAME = " + s.DB.TRUE() + " "
                + "ORDER BY CATORDER, NAME"
            , null
            , CategoryInfo.getSerializerRead()).list();
    }

    /** Categories available to inventory/warehouse, independent of sales catalog visibility. */
    public final List<CategoryInfo> getRootCategoriesInventory() throws BasicException {
        return new PreparedSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, "
                + "CATORDER "
                + "FROM categories "
                + "WHERE PARENTID IS NULL "
                + "ORDER BY CATORDER, NAME"
            , null
            , CategoryInfo.getSerializerRead()).list();
    }

    /**
     *
     * @param category
     * @return
     * @throws BasicException
     */
    public final List<CategoryInfo> getSubcategories(String category) throws BasicException  {
        return new PreparedSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, " 
                + "CATORDER "                    
                + "FROM categories WHERE PARENTID = ? "
                + "ORDER BY CATORDER, NAME"
            , SerializerWriteString.INSTANCE
            , CategoryInfo.getSerializerRead()).list(category);
    }

    /**
     *
     * @param category
     * @return
     * @throws BasicException
     */
    public List<ProductInfoExt> getProductCatalog(String category) throws BasicException  {
	return new PreparedSentence(s
		, "SELECT "
                + "P.ID, "
                + "P.REFERENCE, "
                + "P.CODE, "
                + "P.CODETYPE, "
                + "P.NAME, "
                + "P.PRICEBUY, "
                + "P.PRICESELL, "
                + "P.CATEGORY, "
                + "P.TAXCAT, "
                + "P.ATTRIBUTESET_ID, "
                + "P.STOCKCOST, "
                + "P.STOCKVOLUME, "                        
                + "P.IMAGE, "
                + "P.ISCOM, "
                + "P.ISSCALE, "
                + "P.ISCONSTANT, "
                + "P.PRINTKB, "
                + "P.SENDSTATUS, "
                + "P.ISSERVICE, "
                + "P.ATTRIBUTES, "
                + "P.DISPLAY, "
                + "P.ISVPRICE, "
                + "P.ISVERPATRIB, "
                + "P.TEXTTIP, "
                + "P.WARRANTY, "
                + "P.STOCKUNITS, " 
                + "P.PRINTTO, "
                + "P.SUPPLIER, "        
                + "P.UOM, "
		+ "P.MEMODATE "
                + "FROM products P, products_cat O "
                + "WHERE P.ID = O.PRODUCT AND P.CATEGORY = ? " 
                + "ORDER BY O.CATORDER, P.NAME "                
		, SerializerWriteString.INSTANCE
		, ProductInfoExt.getSerializerRead()).list(category);
    }

    /** Products manageable by inventory, independent of products_cat. */
    public List<ProductInfoExt> getProductCatalogInventory(String category) throws BasicException {
        return new PreparedSentence(s
                , "SELECT "
                + "P.ID, "
                + "P.REFERENCE, "
                + "P.CODE, "
                + "P.CODETYPE, "
                + "P.NAME, "
                + "P.PRICEBUY, "
                + "P.PRICESELL, "
                + "P.CATEGORY, "
                + "P.TAXCAT, "
                + "P.ATTRIBUTESET_ID, "
                + "P.STOCKCOST, "
                + "P.STOCKVOLUME, "
                + "P.IMAGE, "
                + "P.ISCOM, "
                + "P.ISSCALE, "
                + "P.ISCONSTANT, "
                + "P.PRINTKB, "
                + "P.SENDSTATUS, "
                + "P.ISSERVICE, "
                + "P.ATTRIBUTES, "
                + "P.DISPLAY, "
                + "P.ISVPRICE, "
                + "P.ISVERPATRIB, "
                + "P.TEXTTIP, "
                + "P.WARRANTY, "
                + "P.STOCKUNITS, "
                + "P.PRINTTO, "
                + "P.SUPPLIER, "
                + "P.UOM, "
                + "P.MEMODATE "
                + "FROM products P "
                + "WHERE P.CATEGORY = ? AND P.ISSERVICE = " + s.DB.FALSE() + " "
                + "ORDER BY P.NAME "
                , SerializerWriteString.INSTANCE
                , ProductInfoExt.getSerializerRead()).list(category);
    }

    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public List<ProductInfoExt> getProductComments(String id) throws BasicException {
	return new PreparedSentence(s
		, "SELECT "
                + "P.ID, "
                + "P.REFERENCE, "
                + "P.CODE, "
                + "P.CODETYPE, "
                + "P.NAME, "
                + "P.PRICEBUY, "
                + "P.PRICESELL, "
                + "P.CATEGORY, "
                + "P.TAXCAT, "
                + "P.ATTRIBUTESET_ID, "
                + "P.STOCKCOST, "
                + "P.STOCKVOLUME, "                        
                + "P.IMAGE, "
                + "P.ISCOM, "
                + "P.ISSCALE, "
                + "P.ISCONSTANT, "
                + "P.PRINTKB, "
                + "P.SENDSTATUS, "
                + "P.ISSERVICE, "
                + "P.ATTRIBUTES, "
                + "P.DISPLAY, "
                + "P.ISVPRICE, "
                + "P.ISVERPATRIB, "
                + "P.TEXTTIP, "
                + "P.WARRANTY, "
                + "P.STOCKUNITS, " 
                + "P.PRINTTO, "
                + "P.SUPPLIER, "         
                + "P.UOM, "        
                + "P.MEMODATE "
                + "FROM products P, "
                + "products_cat O, products_com M "
                + "WHERE P.ID = O.PRODUCT AND P.ID = M.PRODUCT2 AND M.PRODUCT = ? "
		+ "AND P.ISCOM = " + s.DB.TRUE() + " " +
		  "ORDER BY O.CATORDER, P.NAME"
		, SerializerWriteString.INSTANCE
		, ProductInfoExt.getSerializerRead()).list(id);
    }
    
    // JG uniCenta June 2014 includes StockUnits  
    /**
     *
     * @return
     * @throws BasicException
     */
    public List<ProductInfoExt> getProductConstant() throws BasicException {
        return new PreparedSentence(s
		, "SELECT "
                    + "products.ID, "
                    + "products.REFERENCE, "
                    + "products.CODE, "
                    + "products.CODETYPE, "
                    + "products.NAME, "
                    + "products.PRICEBUY, "
                    + "products.PRICESELL, "
                    + "products.CATEGORY, "
                    + "products.TAXCAT, "
                    + "products.ATTRIBUTESET_ID, "
                    + "products.STOCKCOST, "
                    + "products.STOCKVOLUME, "                        
                    + "products.IMAGE, "
                    + "products.ISCOM, "
                    + "products.ISSCALE, "
                    + "products.ISCONSTANT, "
                    + "products.PRINTKB, "
                    + "products.SENDSTATUS, "
                    + "products.ISSERVICE, "
                    + "products.ATTRIBUTES, "
                    + "products.DISPLAY, "
                    + "products.ISVPRICE, "
                    + "products.ISVERPATRIB, "
                    + "products.TEXTTIP, "
                    + "products.WARRANTY, "
                    + "products.STOCKUNITS, " 
                    + "products.PRINTTO, "
                    + "products.SUPPLIER, "
                    + "products.UOM, "
                    + "products.MEMODATE "
                + "FROM categories INNER JOIN products ON (products.CATEGORY = categories.ID) "
                + "WHERE products.ISCONSTANT = " +s.DB.TRUE()+ " "
                + "AND EXISTS (SELECT 1 FROM products_cat PC WHERE PC.PRODUCT = products.ID) "
                + "ORDER BY categories.NAME, products.NAME", 
                null,
                ProductInfoExt.getSerializerRead()).list();
        
 
    }    

    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public final CategoryInfo getCategoryInfo(String id) throws BasicException {
        return (CategoryInfo) new PreparedSentence(s
        , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, "
                + "CATORDER "                
                + "FROM categories "
                + "WHERE ID = ? "
                + "ORDER BY CATORDER, NAME"
        , SerializerWriteString.INSTANCE
        , CategoryInfo.getSerializerRead()).find(id);
    }
    
        /**
     * JG Dec 2017
     * @param pId
     * @return
     * @throws BasicException
     */
        @SuppressWarnings("unchecked")
    public final List<CategoryStock> getCategorysProductList(String pId) throws BasicException {
        return new PreparedSentence(s,               
                "SELECT products.ID, " +
                    "products.NAME AS Name, " +
                    "products.CODE AS Barcode, " +
                    "categories.ID AS Category " +                        
                "FROM products products " +
                    "INNER JOIN categories categories ON (products.CATEGORY = categories.ID) " +
                "WHERE products.category = ? " +
                "ORDER BY products.NAME ASC",
                    SerializerWriteString.INSTANCE,                
                        CategoryStock.getSerializerRead()).list(pId);
    }
    
    /**
     *
     * @return
     */
      
    public final SentenceList getProductList() {
	return new StaticSentence(s
		, new QBFBuilder(
		  "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "            
                + "UOM, "
                + "MEMODATE "
                + "FROM products "
                + "WHERE ?(QBF_FILTER) "
                + "ORDER BY REFERENCE", 
                new String[] {"NAME", "PRICEBUY", "PRICESELL", "CATEGORY", "CODE|REFERENCE"})
		, new SerializerWriteBasic(new Datas[] {
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.STRING})
		, ProductInfoExt.getSerializerRead());
    }
    
    /**
     *
     * @return
     */
    public SentenceList getProductListNormal() {
	return new StaticSentence(s
		, new QBFBuilder(
		  "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "           
                + "UOM, "
                + "MEMODATE "
		+ "FROM products "
                + "WHERE ISCOM = " + s.DB.FALSE() + " AND ?(QBF_FILTER) ORDER BY REFERENCE",
                new String[] {"NAME", "PRICEBUY", "PRICESELL", "CATEGORY", "CODE|REFERENCE"})
		, new SerializerWriteBasic(new Datas[] {
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.STRING})
		, ProductInfoExt.getSerializerRead());
    }

    /**
     *
     * @return
     */
    public SentenceList getProductsList() {
	return new StaticSentence(s
		, "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "          
                + "UOM, "
                + "MEMODATE "
		+ "FROM products "
                + "ORDER BY NAME"
                , null
		, ProductInfo.getSerializerRead());
    }
    
    public SentenceList getProductList2() {
	return new StaticSentence(s
		, new QBFBuilder(
		  "SELECT "
                + "products.id, "
                + "products.name, "
                + "stockcurrent.units, "
                + "locations.name, "
                + "products.pricesell, "
                + "taxes.rate, "
                + "products.pricesell + (products.pricesell * taxes.rate) AS SellIncTax "
            + " FROM (((stockcurrent stockcurrent "
                + "INNER JOIN locations locations "
                    + "ON (stockcurrent.location = locations.id)) "
                + "INNER JOIN products products "
                    + "ON (stockcurrent.product = products.id)) "
                + "INNER JOIN taxcategories taxcategories "
                    + "ON (products.taxcat = taxcategories.id)) "
                + "INNER JOIN taxes taxes "
                    + "ON (taxes.category = taxcategories.id) "
                + "WHERE ?(QBF_FILTER) "
                + "GROUP BY products.name ",
                new String[] {"NAME", "UNITS", "SellIncTax", "LOCATION",})
		, new SerializerWriteBasic(new Datas[] {
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.STRING})
		, ProductInfoExt.getSerializerRead());                                      
    }
   
    /**
     *
     * @return
     */
    public SentenceList getProductListAuxiliar() {
 	 return new StaticSentence(s
		, new QBFBuilder(
		  "SELECT "
                + "ID, "
                + "REFERENCE, "
                + "CODE, "
                + "CODETYPE, "
                + "NAME, "
                + "PRICEBUY, "
                + "PRICESELL, "
                + "CATEGORY, "
                + "TAXCAT, "
                + "ATTRIBUTESET_ID, "
                + "STOCKCOST, "
                + "STOCKVOLUME, "                        
                + "IMAGE, "
                + "ISCOM, "
                + "ISSCALE, "
                + "ISCONSTANT, "
                + "PRINTKB, "
                + "SENDSTATUS, "                          
                + "ISSERVICE, "
                + "ATTRIBUTES, "
                + "DISPLAY, "
                + "ISVPRICE, "
                + "ISVERPATRIB, "
                + "TEXTTIP, "
                + "WARRANTY, "
                + "STOCKUNITS, "
                + "PRINTTO, "
                + "SUPPLIER, "            
                + "UOM, "
		+ "MEMODATE "
                + "FROM products "
                + "WHERE ISCOM = " + s.DB.TRUE() + " AND ?(QBF_FILTER) "
                + "ORDER BY REFERENCE", new String[] {"NAME", "PRICEBUY", "PRICESELL", "CATEGORY", "CODE|REFERENCE"})
		, new SerializerWriteBasic(new Datas[] {
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.DOUBLE, 
                    Datas.OBJECT, Datas.STRING, 
                    Datas.OBJECT, Datas.STRING})
		, ProductInfoExt.getSerializerRead());
    }

    /**
     * 
     * @param productId The product id to look for bundle
     * @return List of products part of the searched product
     * @throws BasicException 
     */
    public final List<ProductsBundleInfo> getProductsBundle(String productId) throws BasicException {
        return new PreparedSentence(s
            , "SELECT "
                + "ID, "
                + "PRODUCT, "
                + "PRODUCT_BUNDLE, "
                + "QUANTITY "
                + "FROM products_bundle WHERE PRODUCT = ?"
            , SerializerWriteString.INSTANCE
            , ProductsBundleInfo.getSerializerRead()).list(productId);
    }
    
    /**                
     * JG Oct 2016
     * Called from JPanelTicket
     * @param pId
     * @param location
     * @return
     * @throws BasicException
     */
    public final ProductStock getProductStockState(String pId, String location) throws BasicException {

        PreparedSentence preparedSentence = new PreparedSentence(s,
                "SELECT " +
                        "products.id, " +
                        "locations.id as Location, " +
                        "stockcurrent.units AS Current, " +
                        "stocklevel.stocksecurity AS Minimum, " +
                        "stocklevel.stockmaximum AS Maximum, " +
                        "products.pricebuy, " +
                        "products.pricesell, " +
                        "products.memodate " +
                        "FROM locations " +
                        "INNER JOIN ((products " +
                        "INNER JOIN stockcurrent " +
                        "ON products.id = stockcurrent.product) " +
                        "LEFT JOIN stocklevel ON products.id = stocklevel.product) " +
                        "ON locations.id = stockcurrent.location " +
                        "WHERE products.id = ? " +
                        "AND locations.id = ?"
                , SerializerWriteString.INSTANCE
                , ProductStock.getSerializerRead());

        ProductStock productStock = (ProductStock) preparedSentence.find(pId, location);

        return productStock;
    }     
    
    /**
     * JG May 2016
     * Called from StockManagement
     * @param pId
     * @return
     * @throws BasicException
     */
        @SuppressWarnings("unchecked")
    public final List<ProductStock> getProductStockList(String pId) throws BasicException {                    
        return new PreparedSentence(s,               
            "SELECT products.id, " +
                "locations.name AS Location, " +
                "stockcurrent.units AS Current, " +
                "stocklevel.stocksecurity AS Minimum, " +
                "stocklevel.stockmaximum AS Maximum, " +
                "Round(products.pricebuy,2) AS PriceBuy, " +
                "Round((products.pricesell * taxes.rate) + products.pricesell,2) AS PriceSell, " +
                "products.memodate " +
            "FROM ((((taxcategories TC " +
                "INNER JOIN taxes taxes " +
                "ON (TC.id = taxes.category)) " +
                "RIGHT OUTER JOIN products products " +
                "ON (products.TAXCAT = TC.id)) " +
                "LEFT OUTER JOIN stocklevel stocklevel " +
                "ON (stocklevel.product = products.ID)) " +
                "LEFT OUTER JOIN stockcurrent stockcurrent " +
                "ON (products.ID = stockcurrent.product)) " +
                "INNER JOIN locations locations " +
                "ON (stockcurrent.location = locations.id) " +
            "WHERE products.id= ? " +
            "GROUP BY locations.name",
                    SerializerWriteString.INSTANCE,                
                        ProductStock.getSerializerRead()).list(pId);
    }  
    
    /**
     * JG Sept 2017
     * @return
     * @throws BasicException
     */
    @SuppressWarnings("unchecked")
 public final List<ReprintTicketInfo> getReprintTicketList() throws BasicException {                   
        return (List<ReprintTicketInfo>) new StaticSentence(s
                , "SELECT "
                + "T.TICKETID, "
                + "T.TICKETTYPE, "
                + "R.DATENEW, "
                + "P.NAME, "
                + "C.NAME, "
                + "SUM(PM.TOTAL), "
                + "T.STATUS "                   
            + "FROM receipts "
                + "R JOIN tickets T ON R.ID = T.ID LEFT OUTER JOIN payments PM "
                + "ON R.ID = PM.RECEIPT LEFT OUTER JOIN customers C "
                + "ON C.ID = T.CUSTOMER LEFT OUTER JOIN people P ON T.PERSON = P.ID "
            + "GROUP BY "
                + "T.ID, "
                + "T.TICKETID, "
                + "T.TICKETTYPE, "
                + "R.DATENEW, "
                + "P.NAME, "
                + "C.NAME "
            + "ORDER BY R.DATENEW DESC, T.TICKETID "
                + "LIMIT 10 "
                , null
                , new SerializerReadClass(ReprintTicketInfo.class)).list();
    }
 
     /**
     *
     * @param Id
     * @return
     * @throws BasicException
     */
    public final TicketInfo getReprintTicket(String Id) throws BasicException {
        
        if (Id == null) {
            return null; 
        } else {
            Object[]record = (Object[]) new StaticSentence(s
                    , "SELECT "
                + "T.TICKETID, "
                + "SUM(PM.TOTAL), "
                + "R.DATENEW, "
                + "P.NAME, "
                + "T.TICKETTYPE, "
                + "C.NAME, "
                + "T.STATUS "                   
            + "FROM receipts "
                + "R JOIN tickets T ON R.ID = T.ID LEFT OUTER JOIN payments PM "
                + "ON R.ID = PM.RECEIPT LEFT OUTER JOIN customers C "
                + "ON C.ID = T.CUSTOMER LEFT OUTER JOIN people P ON T.PERSON = P.ID "
            + "WHERE T.TICKETID = ?"
                    , SerializerWriteString.INSTANCE
                    , new SerializerReadBasic(new Datas[] {Datas.SERIALIZABLE})).find(Id);
            return record == null ? null : (TicketInfo) record[0];
        }
    }
    
    //Tickets and Receipt list
        public SentenceList getTicketsList() {
         return new StaticSentence(s
            , new QBFBuilder(
            "SELECT "
                + "T.TICKETID, "
                + "T.TICKETTYPE, "
                + "R.DATENEW, "
                + "P.NAME, "
                + "C.NAME, "
                + "SUM(PM.TOTAL), "
                + "T.STATUS "                   
            + "FROM receipts "
                + "R JOIN tickets T ON R.ID = T.ID LEFT OUTER JOIN payments PM "
                + "ON R.ID = PM.RECEIPT LEFT OUTER JOIN customers C "
                + "ON C.ID = T.CUSTOMER LEFT OUTER JOIN people P ON T.PERSON = P.ID "
            + "WHERE ?(QBF_FILTER) "
                + "GROUP BY "
                + "T.ID, "
                + "T.TICKETID, "
                + "T.TICKETTYPE, "
                + "R.DATENEW, "
                + "P.NAME, "
                + "C.NAME "
                + "ORDER BY R.DATENEW DESC, T.TICKETID", 
                 new String[] {
                   "T.TICKETID", "T.TICKETTYPE", "PM.TOTAL", "R.DATENEW", "R.DATENEW", "P.NAME", "C.NAME"})                     
            , new SerializerWriteBasic(new Datas[] {
                Datas.OBJECT, Datas.INT, 
                Datas.OBJECT, Datas.INT, 
                Datas.OBJECT, Datas.DOUBLE, 
                Datas.OBJECT, Datas.TIMESTAMP, 
                Datas.OBJECT, Datas.TIMESTAMP, 
                Datas.OBJECT, Datas.STRING, 
                Datas.OBJECT, Datas.STRING})
            , new SerializerReadClass(FindTicketsInfo.class));
    }
    
    //User list

    /**
     *
     * @return
     */
        public final SentenceList getUserList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME "
                + "FROM people "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new TaxCategoryInfo(
                    dr.getString(1),
                    dr.getString(2)));
    }

    /**
     *
     * @return
     */
        public final SentenceList getTaxList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "CATEGORY, "
                + "CUSTCATEGORY, "
                + "PARENTID, "
                + "RATE, "
                + "RATECASCADE, "
                + "RATEORDER "
                + "FROM taxes "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new TaxInfo(
                    dr.getString(1),
                    dr.getString(2),
                    dr.getString(3),
                    dr.getString(4),
                    dr.getString(5),
                    dr.getDouble(6),
                    dr.getBoolean(7),
                    dr.getInt(8)));
    }

    /**
     *
     * @return
     */
        public final SentenceList getCategoriesList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, "
                + "CATORDER "                    
                + "FROM categories "
                + "ORDER BY NAME"
            , null
            , CategoryInfo.getSerializerRead());
    }
    /**
     * JG Feb 2017
     * Returns all PARENT categories
     * @return
     */
        public final SentenceList getCategoriesList_1() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "IMAGE, "
                + "TEXTTIP, "
                + "CATSHOWNAME, "
                + "CATORDER "                    
                + "FROM categories "
                + "WHERE PARENTID IS NULL "
                + "ORDER BY NAME"
            , null
            , CategoryInfo.getSerializerRead());
    }           
    /**
     *
     * @return
     */
    public final SentenceList getSuppList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "SEARCHKEY, "                    
                + "NAME "
                + "FROM suppliers "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new SupplierInfo(
                dr.getString(1),
                dr.getString(2),
                dr.getString(3)));
    }
    
    /**
     *
     * @return
     */
    public final SentenceList getTaxCustCategoriesList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME "
                + "FROM taxcustcategories "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new TaxCustCategoryInfo(
                    dr.getString(1),
                    dr.getString(2)));
    }

        /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public final CustomerInfoExt getCustomerInfo(String id) throws BasicException {
	return (CustomerInfoExt) new PreparedSentence(s
		, "SELECT "
                + "ID, "
                + "SEARCHKEY, "
                + "TAXID, "
                + "NAME, "
                + "TAXCATEGORY, "
                + "CARD, "
                + "MAXDEBT, "
                + "ADDRESS, "
                + "ADDRESS2, "
                + "POSTAL, "                        
                + "CITY, "
                + "REGION, "
                + "COUNTRY, "
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "                          
                + "PHONE, "
                + "PHONE2, "
                + "FAX, "
                + "NOTES, "
                + "VISIBLE, "
                + "CURDATE, "
                + "CURDEBT, "
                + "IMAGE, "
                + "ISVIP, "
                + "DISCOUNT, "
                + "MEMODATE "        
                + "FROM customers WHERE ID = ?"
		, SerializerWriteString.INSTANCE
		, new CustomerExtRead()).find(id);
    }
    
    /**
     * JG Apr 2017 - Revised to return Customer Id - cId param
     * @param cId
     * @return
     * @throws BasicException
     */
        @SuppressWarnings("unchecked")
    public final List<CustomerTransaction> getCustomersTransactionList(String cId) throws BasicException {
                    
        return new PreparedSentence(s,               
                "SELECT tickets.TICKETID, " +
                    "products.NAME AS PNAME, " +
                    "SUM(ticketlines.UNITS) AS UNITS, " +
                    "SUM(ticketlines.UNITS * ticketlines.PRICE) AS AMOUNT, " +
                    "SUM(ticketlines.UNITS * ticketlines.PRICE * (1.0 + taxes.RATE)) AS TOTAL, " +
                    "receipts.DATENEW, " +
                    "customers.ID AS CID " +
                "FROM ((((ticketlines ticketlines " +
                    "CROSS JOIN taxes taxes ON (ticketlines.TAXID = taxes.ID)) " +
                    "INNER JOIN tickets tickets ON (tickets.ID = ticketlines.TICKET)) " +
                    "INNER JOIN customers customers ON (customers.ID = tickets.CUSTOMER)) " +
                    "INNER JOIN receipts receipts ON (tickets.ID = receipts.ID)) " +
                    "LEFT OUTER JOIN products products ON (ticketlines.PRODUCT = products.ID) " +
                "WHERE tickets.CUSTOMER = ? " +
                    "GROUP BY customers.ID, receipts.DATENEW, tickets.TICKETID, " +
                    "products.NAME, tickets.TICKETTYPE " +
                "ORDER BY receipts.DATENEW DESC",
                    SerializerWriteString.INSTANCE,                
                        CustomerTransaction.getSerializerRead()).list(cId);
    }

    /**
     *
     * @return
     */
    public final SentenceList getTaxCategoriesList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME "
                + "FROM taxcategories "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new TaxCategoryInfo(dr.getString(1), dr.getString(2)));
    }

    /**
     *
     * @return
     */
    public final SentenceList getAttributeSetList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME "
                + "FROM attributeset "
                + "ORDER BY NAME"
            , null
            , (DataRead dr) -> new AttributeSetInfo(dr.getString(1), dr.getString(2)));
    }

    /**
     *
     * @return
     */
    public final SentenceList getLocationsList() {
        return new StaticSentence(s
            , "SELECT "
                + "ID, "
                + "NAME, "
                + "ADDRESS FROM locations "
                + "ORDER BY NAME"
            , null
            , new SerializerReadClass(LocationInfo.class));
    }

    /**
     *
     * @return
     */
    public final SentenceList getFloorsList() {
        return new StaticSentence(s
            , "SELECT ID, NAME FROM floors ORDER BY NAME"
            , null
            , new SerializerReadClass(FloorsInfo.class));
    }
        /**
     *
     * @return
     */
    public final SentenceList getFloorTablesList() {
        return new StaticSentence(s
            , "SELECT ID, NAME, SEATS, width, height FROM places ORDER BY NAME"                
            , null
            , new SerializerReadClass(FloorsInfo.class));
    }
    

    /**
     *
     * @param card
     * @return
     * @throws BasicException
     */
    public CustomerInfoExt findCustomerExt(String card) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s
                , "SELECT "
                + "ID, "
                + "TAXID, "
                + "SEARCHKEY, "
                + "NAME, "
                + "TAXCATEGORY, "
                + "CARD, "
                + "MAXDEBT, "
                + "ADDRESS, "
                + "ADDRESS2, "
                + "POSTAL, "
                + "CITY, "
                + "REGION, "
                + "COUNTRY, "
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "
                + "PHONE, "
                + "PHONE2, "
                + "FAX, "                        
                + "NOTES, "
                + "VISIBLE, "
                + "CURDATE, "
                + "CURDEBT, " 
                + "IMAGE, "
                + "ISVIP, "
                + "DISCOUNT, "                        
                + "MEMODATE "
                + "FROM customers "
                + "WHERE CARD = ? AND VISIBLE = " + s.DB.TRUE() + " "
                + "ORDER BY NAME"
                , SerializerWriteString.INSTANCE
                , new CustomerExtRead()).find(card);
    }
        /**
     *
     * @param name
     * @return
     * @throws BasicException
     */
    public CustomerInfoExt findCustomerName(String name) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s
                , "SELECT "
                + "ID, "
                + "SEARCHKEY, "
                + "TAXID, "
                + "NAME, "
                + "TAXCATEGORY, "
                + "CARD, "
                + "MAXDEBT, "
                + "ADDRESS, "
                + "ADDRESS2, "
                + "POSTAL, "
                + "CITY, "
                + "REGION, "
                + "COUNTRY, "
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "
                + "PHONE, "
                + "PHONE2, "
                + "FAX, "
                + "NOTES, "
                + "VISIBLE, "
                + "CURDATE, "
                + "CURDEBT, " 
                + "IMAGE, "
                + "ISVIP, "
                + "DISCOUNT, "
                + "MEMODATE "
                + "FROM customers "
                + "WHERE NAME = ? AND VISIBLE = " + s.DB.TRUE() + " "
                + "ORDER BY NAME"
                , SerializerWriteString.INSTANCE
                , new CustomerExtRead()).find(name);
    }

    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public CustomerInfoExt loadCustomerExt(String id) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s
                , "SELECT "
                + "ID, "
                + "SEARCHKEY, "
                + "TAXID, " 
                + "NAME, "
                + "TAXCATEGORY, "
                + "CARD, "
                + "MAXDEBT, "
                + "ADDRESS, "
                + "ADDRESS2, "
                + "POSTAL, "
                + "CITY, "
                + "REGION, "
                + "COUNTRY, "
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "
                + "PHONE, "
                + "PHONE2, "
                + "FAX, "
                + "NOTES, "
                + "VISIBLE, "
                + "CURDATE, "
                + "CURDEBT, "
                + "IMAGE, "
                + "ISVIP, "        
                + "DISCOUNT, "
                + "MEMODATE "
                + "FROM customers WHERE ID = ?"
                , SerializerWriteString.INSTANCE
                , new CustomerExtRead()).find(id);
    }
    
    /**
     * Quick Customer create
     * @param id
     * @return
     * @throws BasicException
     */
    public CustomerInfoExt qCustomerExt(String id) throws BasicException {
        return (CustomerInfoExt) new PreparedSentence(s
                , "SELECT "
                + "ID, "
                + "SEARCHKEY, "
                + "TAXID, " 
                + "NAME, "
                + "TAXCATEGORY, "
                + "MAXDEBT, "        
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "
                + "PHONE, "
                + "PHONE2, "
                + "ISVIP, "        
                + "DISCOUNT "
                + "FROM customers WHERE ID = ?"
                , SerializerWriteString.INSTANCE
                , new CustomerExtRead()).find(id);
    }
    
    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public final boolean isCashActive(String id) throws BasicException {

        return new PreparedSentence(s,
                "SELECT MONEY FROM closedcash WHERE DATEEND IS NULL AND MONEY = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE).find(id)
            != null;
    }

    /**
     *
     * @param tickettype
     * @param ticketid
     * @return
     * @throws BasicException
     */
    public final TicketInfo loadTicket(final int tickettype, final int ticketid) throws BasicException {
        TicketInfo ticket = (TicketInfo) new PreparedSentence(s
                , "SELECT "
                + "T.ID, "
                + "T.TICKETTYPE, "
                + "T.TICKETID, "
                + "R.DATENEW, "
                + "R.MONEY, "
                + "R.ATTRIBUTES, "
                + "P.ID, "
                + "P.NAME, "
                + "T.CUSTOMER, "
                + "T.STATUS "            
                + "FROM receipts R "
                + "JOIN tickets T ON R.ID = T.ID "
                + "LEFT OUTER JOIN people P ON T.PERSON = P.ID "
                + "WHERE T.TICKETTYPE = ? AND T.TICKETID = ? "
                + "ORDER BY R.DATENEW DESC"
                , SerializerWriteParams.INSTANCE
                , new SerializerReadClass(TicketInfo.class))
                .find(new DataParams() {
                @Override
                    public void writeValues() throws BasicException {
                    setInt(1, tickettype);
                    setInt(2, ticketid);
                    }});
        
        if (ticket != null) {

            String customerid = ticket.getCustomerId();
            ticket.setCustomer(customerid == null
                    ? null
                    : loadCustomerExt(customerid));

            ticket.setLines(new PreparedSentence(s
                , "SELECT L.TICKET, L.LINE, L.PRODUCT, L.ATTRIBUTESETINSTANCE_ID, " +
                        "L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.CUSTCATEGORY, " +
                        "T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES " +
                "FROM ticketlines L, taxes T " +
                "WHERE L.TAXID = T.ID AND L.TICKET = ? ORDER BY L.LINE"
                , SerializerWriteString.INSTANCE
                , new SerializerReadClass(TicketLineInfo.class)).list(ticket.getId()));

            ticket.setPayments(new PreparedSentence(s
                , "SELECT PAYMENT, TOTAL, TRANSID, TENDERED, CARDNAME, VOUCHER FROM payments WHERE RECEIPT = ?"                    
                , SerializerWriteString.INSTANCE
                , new SerializerReadClass(PaymentInfoTicket.class)).list(ticket.getId()));
            ticket.setVoucherDiscounts(loadVoucherDiscounts(ticket.getId()));
        }
        return ticket;
    }
    
    /**
     *
     * @param ticket
     * @param location
     * @throws BasicException
     */
    private TicketInfo loadNormalTicketByReceiptId(final String receiptId) throws BasicException {
        TicketInfo ticket = (TicketInfo) new PreparedSentence(s,
                "SELECT T.ID, T.TICKETTYPE, T.TICKETID, R.DATENEW, R.MONEY, R.ATTRIBUTES, "
                + "P.ID, P.NAME, T.CUSTOMER, T.STATUS "
                + "FROM receipts R JOIN tickets T ON R.ID = T.ID "
                + "LEFT OUTER JOIN people P ON T.PERSON = P.ID "
                + "WHERE T.ID = ? AND T.TICKETTYPE = 0",
                SerializerWriteString.INSTANCE,
                new SerializerReadClass(TicketInfo.class)).find(receiptId);

        if (ticket != null) {
            String customerId = ticket.getCustomerId();
            ticket.setCustomer(customerId == null ? null : loadCustomerExt(customerId));
            ticket.setLines(new PreparedSentence(s,
                    "SELECT L.TICKET, L.LINE, L.PRODUCT, L.ATTRIBUTESETINSTANCE_ID, "
                    + "L.UNITS, L.PRICE, T.ID, T.NAME, T.CATEGORY, T.CUSTCATEGORY, "
                    + "T.PARENTID, T.RATE, T.RATECASCADE, T.RATEORDER, L.ATTRIBUTES "
                    + "FROM ticketlines L, taxes T "
                    + "WHERE L.TAXID = T.ID AND L.TICKET = ? ORDER BY L.LINE",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadClass(TicketLineInfo.class)).list(ticket.getId()));
            ticket.setPayments(new PreparedSentence(s,
                    "SELECT PAYMENT, TOTAL, TRANSID, TENDERED, CARDNAME, VOUCHER "
                    + "FROM payments WHERE RECEIPT = ?",
                    SerializerWriteString.INSTANCE,
                    new SerializerReadClass(PaymentInfoTicket.class)).list(ticket.getId()));
            ticket.setVoucherDiscounts(loadVoucherDiscounts(ticket.getId()));
        }
        return ticket;
    }

    private List<VoucherDiscountInfo> loadVoucherDiscounts(final String receiptId) throws BasicException {
        return new PreparedSentence(s,
                "SELECT R.VOUCHER_ID, R.VOUCHER_NUMBER, R.VOUCHER_CUSTOMER_BEFORE, "
                + "V.AMOUNT, R.DISCOUNT_AMOUNT FROM voucher_redemptions R "
                + "JOIN vouchers V ON V.ID = R.VOUCHER_ID "
                + "WHERE R.RECEIPT_ID = ? AND R.STATUS = 'ACTIVE' ORDER BY R.REDEEMED_AT, R.ID",
                SerializerWriteString.INSTANCE,
                new SerializerRead() {
                    @Override
                    public Object readValues(DataRead dr) throws BasicException {
                        Double faceValue = dr.getDouble(4);
                        Double appliedAmount = dr.getDouble(5);
                        return new VoucherDiscountInfo(dr.getString(1), dr.getString(2), dr.getString(3),
                                faceValue == null ? 0.0 : faceValue,
                                appliedAmount == null ? 0.0 : appliedAmount);
                    }
                }).list(receiptId);
    }

    public final boolean hasRecordedRefund(final int saleTicketId) throws BasicException {
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM tickets WHERE TICKETTYPE = 1 AND STATUS = ?",
                SerializerWriteInteger.INSTANCE, SerializerReadInteger.INSTANCE).find(saleTicketId);
        return count != null && count > 0;
    }

    public final void saveTicket(final TicketInfo ticket, final String location) throws BasicException {

    Transaction t;
    t = new Transaction(s) {
    @Override
    public Object transact() throws BasicException {

        if (ticket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
            final int sourceTicketId = ticket.getTicketStatus();
            if (sourceTicketId <= 0) {
                throw new BasicException("La devolución no tiene una venta original válida.");
            }
            // Lock the source sale until commit so another cashier cannot refund it concurrently.
            Integer sourceStatus = (Integer) new PreparedSentence(s,
                    "SELECT STATUS FROM tickets WHERE TICKETTYPE = 0 AND TICKETID = ? FOR UPDATE",
                    SerializerWriteInteger.INSTANCE, SerializerReadInteger.INSTANCE).find(sourceTicketId);
            if (sourceStatus == null || sourceStatus != 0 || hasRecordedRefund(sourceTicketId)) {
                throw new BasicException("Esta venta ya tiene una devolución o no está disponible para devolver.");
            }
        }

        Set<String> vouchersToConsume = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (VoucherDiscountInfo discount : ticket.getVoucherDiscounts()) {
            if (discount != null) {
                String customerId = discount.getCustomerId();
                String voucherNumber = discount.getVoucher();

                if (customerId != null && !customerId.isBlank()
                        && !customerId.equals(ticket.getCustomerId())) {
                    throw new BasicException("Voucher belongs to another customer.");
                }
                if (voucherNumber == null || voucherNumber.isBlank()) {
                    throw new BasicException("Voucher redemption requires a valid voucher number.");
                }
                if (!vouchersToConsume.add(voucherNumber.trim())) {
                    throw new BasicException("The same voucher cannot be used more than once in a sale.");
                }
            }
        }

        String sourceReceiptId = ticket.getProperty(VoucherEditContext.SOURCE_RECEIPT_ID);
        String authorizationUserId = ticket.getProperty(VoucherEditContext.AUTH_USER_ID);
        String operatorUserId = ticket.getProperty(VoucherEditContext.OPERATOR_ID);
        String authorizationAt = ticket.getProperty(VoucherEditContext.AUTH_AT);
        boolean editedNormalSale = ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL && ticket.getOldTicket();
        int sourceVoucherCount = 0;
        boolean voucherEditAuthorized = false;
        TicketInfo sourceVoucherEditTicket = null;

        if (editedNormalSale) {
            if (sourceReceiptId == null || sourceReceiptId.isBlank()
                    || !lockSourceNormalReceiptForVoucherEdit(sourceReceiptId)) {
                throw new BasicException("Edited sale is missing or has already finalized its source receipt context.");
            }
            sourceVoucherCount = countVoucherUsages(sourceReceiptId);
            if (sourceVoucherCount > 0) {
                voucherEditAuthorized = isAuthorizedVoucherEditUser(authorizationUserId);
                if (!voucherEditAuthorized) {
                    throw new BasicException("Administrator or Manager authorization is required to edit a sale that consumed vouchers.");
                }
                if (!isVisibleVoucherEditOperator(operatorUserId)) {
                    throw new BasicException("Voucher edit audit requires a valid visible POS operator.");
                }
                parseVoucherAuthorizationTime(authorizationAt);
                sourceVoucherEditTicket = loadNormalTicketByReceiptId(sourceReceiptId);
                if (sourceVoucherEditTicket == null) {
                    throw new BasicException("Unable to load the locked source sale for atomic voucher edit finalization.");
                }
            }
        }

        for (VoucherDiscountInfo discount : ticket.getVoucherDiscounts()) {
            int affected = consumeVoucher(discount, ticket.getCustomerId());
            if (affected != 1) {
                boolean reusedFromEditedSale = editedNormalSale
                        && sourceVoucherCount > 0
                        && voucherEditAuthorized
                        && isReusableVoucherFromSourceReceipt(sourceReceiptId, discount.getVoucher());
                if (!reusedFromEditedSale) {
                    throw new BasicException("Voucher is invalid, belongs to another customer, is already redeemed, expired, or duplicated in the voucher table.");
                }
            }
        }

        // Set Receipt Id
        if (ticket.getTicketId() == 0) {
            switch (ticket.getTicketType()) {
                case TicketInfo.RECEIPT_NORMAL:
                    ticket.setTicketId(getNextTicketIndex());
                    break;
                case TicketInfo.RECEIPT_REFUND:
                    ticket.setTicketId(getNextTicketRefundIndex());
                    break;
                case TicketInfo.RECEIPT_PAYMENT:
                    ticket.setTicketId(getNextTicketPaymentIndex());
                    break;
                case TicketInfo.RECEIPT_NOSALE:
                    ticket.setTicketId(getNextTicketPaymentIndex());
                    break;
                default:
                    throw new BasicException();
            }
        }

        new PreparedSentence(s
            , "INSERT INTO receipts (ID, MONEY, DATENEW, ATTRIBUTES, PERSON) VALUES (?, ?, ?, ?, ?)"
                , SerializerWriteParams.INSTANCE )
            .exec(new DataParams() {

            @Override
            public void writeValues() throws BasicException {
                setString(1, ticket.getId());
                setString(2, ticket.getActiveCash());
                setTimestamp(3, ticket.getDate());

                try {
                    ByteArrayOutputStream o = new ByteArrayOutputStream();
                    ticket.getProperties().storeToXML(o, AppLocal.APP_NAME, "UTF-8");
                    setBytes(4, o.toByteArray());
                } catch (IOException e) {
                    setBytes(4, null);
                }
                setString(5, ticket.getProperty("person"));
            }
            });

        for (VoucherDiscountInfo discount : ticket.getVoucherDiscounts()) {
            insertVoucherRedemption(discount, ticket);
        }

    // new ticket
        new PreparedSentence(s
            , "INSERT INTO tickets (ID, TICKETTYPE, TICKETID, PERSON, CUSTOMER, STATUS) "
                + "VALUES (?, ?, ?, ?, ?, ?)"
            , SerializerWriteParams.INSTANCE )
                .exec(new DataParams() {

            @Override
            public void writeValues() throws BasicException {
                setString(1, ticket.getId());
                setInt(2, ticket.getTicketType());
                setInt(3, ticket.getTicketId());
                setString(4, ticket.getUser().getId());
                setString(5, ticket.getCustomerId());
                setInt(6, ticket.getTicketStatus());                
            }
        });
        
    // Complete the locked sale only when saving a refund.
        if (ticket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
        int updated = new PreparedSentence(s
            , "UPDATE tickets SET STATUS = ? "
                + "WHERE TICKETTYPE = 0 AND TICKETID = ? AND STATUS = 0"
            , SerializerWriteParams.INSTANCE )
                .exec(new DataParams() {

            @Override
            public void writeValues() throws BasicException {
                setInt(1, ticket.getTicketId());
                setInt(2, ticket.getTicketStatus());                
            }
        });
        if (updated != 1) {
            throw new BasicException("La venta original cambió; se canceló la devolución sin generar un egreso.");
        }
        }

        SentenceExec ticketlineinsert = new PreparedSentence(s
            , "INSERT INTO ticketlines (TICKET, LINE, "
                    + "PRODUCT, ATTRIBUTESETINSTANCE_ID, "
                    + "UNITS, PRICE, TAXID, ATTRIBUTES) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            , SerializerWriteBuilder.INSTANCE);
  
        for (TicketLineInfo l : ticket.getLines()) {
            ticketlineinsert.exec(l);

            if (l.getProductID() != null && l.isProductService() != true)  {
                getStockDiaryInsert().exec(new Object[] {
                UUID.randomUUID().toString(),
                ticket.getDate(),
                l.getMultiply() < 0.0
                    ? MovementReason.IN_REFUND.getKey()
                    : MovementReason.OUT_SALE.getKey(),
                location,
                l.getProductID(),
                l.getProductAttSetInstId(), -l.getMultiply(), l.getPrice(),
                ticket.getUser().getName()                         
                });
            }
        }

        SentenceExec paymentinsert = new PreparedSentence(s
            , "INSERT INTO payments (ID, RECEIPT, PAYMENT, TOTAL, TRANSID, RETURNMSG, "
                + "TENDERED, CARDNAME, VOUCHER) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                , SerializerWriteParams.INSTANCE);

        for (final PaymentInfo payment : ticket.getPayments()) {
            paymentinsert.exec(new DataParams() {
                @Override
                public void writeValues() throws BasicException {
                    setString(1, UUID.randomUUID().toString());
                    setString(2, ticket.getId());
                    setString(3, payment.getName());
                    setDouble(4, payment.getTotal());
                    setString(5, ticket.getTransactionID());
                    setBytes(6, (byte[]) Formats.BYTEA.parseValue(ticket.getReturnMessage()));
                    // Preserve the historical meaning of TENDERED used by this code path.
                    setDouble(7, payment.getPaid());
                    setString(8, null);
                    setString(9, payment.getVoucher());
                }
            });

            if ("debt".equals(payment.getName()) || "debtpaid".equals(payment.getName())) {
                ticket.getCustomer().updateCurDebt(payment.getTotal(), ticket.getDate());
                getDebtUpdate().exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setDouble(1, ticket.getCustomer().getAccdebt());
                        setTimestamp(2, ticket.getCustomer().getCurdate());
                        setString(3, ticket.getCustomer().getId());
                    }
                });
            }
        }

                if (sourceVoucherEditTicket != null) {
                    finalizeVoucherEditedSaleAtomic(sourceVoucherEditTicket, ticket, location,
                            operatorUserId, authorizationUserId, authorizationAt);
                }

                SentenceExec taxlinesinsert = new PreparedSentence(s
                    , "INSERT INTO taxlines (ID, RECEIPT, TAXID, BASE, AMOUNT)  "
                    + "VALUES (?, ?, ?, ?, ?)"
                    , SerializerWriteParams.INSTANCE);
                
                if (ticket.getTaxes() != null) {
                    for (final TicketTaxInfo tickettax: ticket.getTaxes()) {
                        taxlinesinsert.exec(new DataParams() {
                        @Override
                        public void writeValues() throws BasicException {
                            setString(1, UUID.randomUUID().toString());
                            setString(2, ticket.getId());
                            setString(3, tickettax.getTaxInfo().getId());
                            setDouble(4, tickettax.getSubTotal());
                            setDouble(5, tickettax.getTax());
                        }
                        });
                    }
                }
            return null;
        }
    };

        t.execute();
}

    /**
     *
     * @param ticket
     * @param location
     * @throws BasicException
     */
    private void finalizeVoucherEditedSaleAtomic(final TicketInfo sourceTicket,
            final TicketInfo replacementTicket, final String location,
            final String operatorUserId, final String authorizationUserId,
            final String authorizationAt) throws BasicException {

        if (!lockSourceNormalReceiptForVoucherEdit(sourceTicket.getId())) {
            throw new BasicException("The source sale is no longer available for atomic voucher edit finalization.");
        }

        Set<String> sourceVoucherNumbers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sourceVoucherNumbers.addAll(getVoucherPaymentsForReceipt(sourceTicket.getId()));
        if (sourceVoucherNumbers.isEmpty()) {
            throw new BasicException("The source sale no longer contains the vouchers expected by this authorized edit.");
        }
        if (!isAuthorizedVoucherEditUser(authorizationUserId)) {
            throw new BasicException("Administrator or Manager authorization is required to reactivate vouchers from an edited sale.");
        }
        if (!isVisibleVoucherEditOperator(operatorUserId)) {
            throw new BasicException("Voucher edit audit requires a valid visible POS operator.");
        }
        Date voucherAuthorizationTime = parseVoucherAuthorizationTime(authorizationAt);
        if (replacementTicket == null || replacementTicket.getId() == null
                || replacementTicket.getId().isBlank() || !receiptExists(replacementTicket.getId())) {
            throw new BasicException("Replacement receipt is missing while finalizing voucher reactivation.");
        }

        Set<String> replacementVoucherNumbers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        replacementVoucherNumbers.addAll(getVoucherPaymentsForReceipt(replacementTicket.getId()));

        Date reversalTime = new Date();
        for (int i = 0; i < sourceTicket.getLinesCount(); i++) {
            if (sourceTicket.getLine(i).getProductID() != null) {
                getStockDiaryInsert().exec(new Object[]{
                    UUID.randomUUID().toString(),
                    reversalTime,
                    sourceTicket.getLine(i).getMultiply() >= 0.0
                            ? MovementReason.IN_REFUND.getKey()
                            : MovementReason.OUT_SALE.getKey(),
                    location,
                    sourceTicket.getLine(i).getProductID(),
                    sourceTicket.getLine(i).getProductAttSetInstId(),
                    sourceTicket.getLine(i).getMultiply(),
                    sourceTicket.getLine(i).getPrice(),
                    sourceTicket.getUser().getName()
                });
            }

            List<ProductsBundleInfo> bundle = sourceTicket.getLine(i).getProductID() == null
                    ? java.util.Collections.emptyList()
                    : getProductsBundle((String) sourceTicket.getLine(i).getProductID());
            if (bundle.size() > 0) {
                for (ProductsBundleInfo bundleComponent : bundle) {
                    ProductInfoExt bundleProduct = getProductInfo(bundleComponent.getProductBundleId());
                    getStockDiaryInsert().exec(new Object[]{
                        UUID.randomUUID().toString(),
                        reversalTime,
                        sourceTicket.getLine(i).getMultiply() * bundleComponent.getQuantity() >= 0.0
                                ? MovementReason.IN_REFUND.getKey()
                                : MovementReason.OUT_SALE.getKey(),
                        location,
                        bundleComponent.getProductBundleId(),
                        null,
                        sourceTicket.getLine(i).getMultiply() * bundleComponent.getQuantity(),
                        bundleProduct.getPriceSell(),
                        sourceTicket.getUser().getName()
                    });
                }
            }
        }

        CustomerInfoExt currentDebtCustomer = null;
        for (PaymentInfo payment : sourceTicket.getPayments()) {
            if ("debt".equals(payment.getName()) || "debtpaid".equals(payment.getName())) {
                if (sourceTicket.getCustomerId() == null) {
                    throw new BasicException("Edited source sale has debt movements without a customer.");
                }
                if (currentDebtCustomer == null) {
                    currentDebtCustomer = loadCustomerExt(sourceTicket.getCustomerId());
                    if (currentDebtCustomer == null) {
                        throw new BasicException("Unable to load customer while reversing edited sale debt.");
                    }
                }
                currentDebtCustomer.updateCurDebt(-payment.getTotal(), sourceTicket.getDate());
                final CustomerInfoExt debtCustomer = currentDebtCustomer;
                getDebtUpdate().exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setDouble(1, debtCustomer.getAccdebt());
                        setTimestamp(2, debtCustomer.getCurdate());
                        setString(3, debtCustomer.getId());
                    }
                });
            }
        }

        reverseVoucherRedemptions(sourceTicket.getId(), replacementTicket.getId());

        new StaticSentence(s, "DELETE FROM taxlines WHERE RECEIPT = ?",
                SerializerWriteString.INSTANCE).exec(sourceTicket.getId());
        new StaticSentence(s, "DELETE FROM payments WHERE RECEIPT = ?",
                SerializerWriteString.INSTANCE).exec(sourceTicket.getId());
        new StaticSentence(s, "DELETE FROM ticketlines WHERE TICKET = ?",
                SerializerWriteString.INSTANCE).exec(sourceTicket.getId());
        new StaticSentence(s, "DELETE FROM tickets WHERE ID = ?",
                SerializerWriteString.INSTANCE).exec(sourceTicket.getId());
        new StaticSentence(s, "DELETE FROM receipts WHERE ID = ?",
                SerializerWriteString.INSTANCE).exec(sourceTicket.getId());

        for (String voucherNumber : sourceVoucherNumbers) {
            if (replacementVoucherNumbers.contains(voucherNumber)) {
                insertVoucherEditAudit(voucherNumber, sourceTicket.getId(), replacementTicket.getId(),
                        operatorUserId, authorizationUserId, voucherAuthorizationTime, "EDIT_REUSED");
            } else {
                int affected = reactivateVoucherAfterEdit(voucherNumber);
                if (affected != 1) {
                    throw new BasicException("Unable to reactivate voucher from edited sale.");
                }
                insertVoucherEditAudit(voucherNumber, sourceTicket.getId(), replacementTicket.getId(),
                        operatorUserId, authorizationUserId, voucherAuthorizationTime, "EDIT_REACTIVATED");
            }
        }
    }

    public final void deleteTicket(final TicketInfo ticket, final String location) throws BasicException {
        deleteTicketInternal(ticket, location, null, null, null, null, false);
    }

    private void deleteEditedTicket(final TicketInfo sourceTicket, final String location,
            final String replacementReceiptId, final String operatorUserId,
            final String authorizationUserId, final String authorizationAt) throws BasicException {
        deleteTicketInternal(sourceTicket, location, replacementReceiptId, operatorUserId,
                authorizationUserId, authorizationAt, true);
    }

    private void deleteTicketInternal(final TicketInfo ticket, final String location,
            final String replacementReceiptId, final String operatorUserId,
            final String authorizationUserId, final String authorizationAt,
            final boolean reactivateUnusedVouchers) throws BasicException {

        Transaction t;
        t = new Transaction(s) {
            @Override
            public Object transact() throws BasicException {

                Set<String> sourceVoucherNumbers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                Set<String> replacementVoucherNumbers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                Date voucherAuthorizationTime = null;
                if (reactivateUnusedVouchers) {
                    if (!lockSourceNormalReceiptForVoucherEdit(ticket.getId())) {
                        throw new BasicException("The source sale is no longer available for voucher edit finalization.");
                    }
                    sourceVoucherNumbers.addAll(getVoucherPaymentsForReceipt(ticket.getId()));
                    if (sourceVoucherNumbers.isEmpty()) {
                        throw new BasicException("The source sale no longer contains the vouchers expected by this authorized edit.");
                    }
                    if (!sourceVoucherNumbers.isEmpty()) {
                        if (!isAuthorizedVoucherEditUser(authorizationUserId)) {
                            throw new BasicException("Administrator or Manager authorization is required to reactivate vouchers from an edited sale.");
                        }
                        if (!isVisibleVoucherEditOperator(operatorUserId)) {
                            throw new BasicException("Voucher edit audit requires a valid visible POS operator.");
                        }
                        voucherAuthorizationTime = parseVoucherAuthorizationTime(authorizationAt);
                        if (replacementReceiptId == null || replacementReceiptId.isBlank()
                                || !receiptExists(replacementReceiptId)) {
                            throw new BasicException("Replacement receipt is missing while finalizing voucher reactivation.");
                        }
                        replacementVoucherNumbers.addAll(getVoucherPaymentsForReceipt(replacementReceiptId));
                    }
                }

                // update the inventory
                Date d = new Date();
                for (int i = 0; i < ticket.getLinesCount(); i++) {
                    if (ticket.getLine(i).getProductID() != null)  {
                        // Hay que actualizar el stock si el hay producto
                        getStockDiaryInsert().exec( new Object[] {
                            UUID.randomUUID().toString(),
                            d,
                            ticket.getLine(i).getMultiply() >= 0.0
                                    ? MovementReason.IN_REFUND.getKey()
                                    : MovementReason.OUT_SALE.getKey(),
                            location,
                            ticket.getLine(i).getProductID(),
                            ticket.getLine(i).getProductAttSetInstId(), ticket.getLine(i).getMultiply(), ticket.getLine(i).getPrice(),
                            ticket.getUser().getName() 
                        });
                    }
// For productBundle
                    List<ProductsBundleInfo> bundle = ticket.getLine(i).getProductID() == null
                            ? java.util.Collections.emptyList()
                            : getProductsBundle((String) ticket.getLine(i).getProductID());

                    if (bundle.size() > 0) {
                        for (ProductsBundleInfo bundleComponent : bundle) {
                            ProductInfoExt bundleProduct = getProductInfo(bundleComponent.getProductBundleId());
        
                            getStockDiaryInsert().exec(new Object[]{
                            UUID.randomUUID().toString(),
                            d,
                            ticket.getLine(i).getMultiply() * bundleComponent.getQuantity()  >= 0.0
                                ? MovementReason.IN_REFUND.getKey()
                                : MovementReason.OUT_SALE.getKey(),
                            location,
                            bundleComponent.getProductBundleId(),
                            null, ticket.getLine(i).getMultiply() * bundleComponent.getQuantity()
                            , bundleProduct.getPriceSell(),
                            ticket.getUser().getName()});
                        }
                    }
                }
                
                // update customer debts
                for (PaymentInfo p : ticket.getPayments()) {
                    if ("debt".equals(p.getName()) || "debtpaid".equals(p.getName())) {

                        // udate customer fields...
                        ticket.getCustomer().updateCurDebt(-p.getTotal(), ticket.getDate());
                        
                        // save customer fields...
                        getDebtUpdate().exec(new DataParams() {
                            @Override
                            public void writeValues() throws BasicException {
                                setDouble(1, ticket.getCustomer().getAccdebt());
                                setTimestamp(2, ticket.getCustomer().getCurdate());
                                setString(3, ticket.getCustomer().getId());
                            }});
                    }
                }

                // and delete the receipt
                if (reactivateUnusedVouchers) {
                    reverseVoucherRedemptions(ticket.getId(), replacementReceiptId);
                }
                new StaticSentence(s
                        , "DELETE FROM taxlines WHERE RECEIPT = ?"
                        , SerializerWriteString.INSTANCE).exec(ticket.getId());
                new StaticSentence(s
                        , "DELETE FROM payments WHERE RECEIPT = ?"
                        , SerializerWriteString.INSTANCE).exec(ticket.getId());
                new StaticSentence(s
                        , "DELETE FROM ticketlines WHERE TICKET = ?"
                        , SerializerWriteString.INSTANCE).exec(ticket.getId());
                new StaticSentence(s
                        , "DELETE FROM tickets WHERE ID = ?"
                        , SerializerWriteString.INSTANCE).exec(ticket.getId());
                new StaticSentence(s
                        , "DELETE FROM receipts WHERE ID = ?"
                        , SerializerWriteString.INSTANCE).exec(ticket.getId());

                if (reactivateUnusedVouchers) {
                    for (String voucherNumber : sourceVoucherNumbers) {
                        if (replacementVoucherNumbers.contains(voucherNumber)) {
                            insertVoucherEditAudit(voucherNumber, ticket.getId(), replacementReceiptId,
                                    operatorUserId, authorizationUserId, voucherAuthorizationTime,
                                    "EDIT_REUSED");
                        }
                        if (!replacementVoucherNumbers.contains(voucherNumber)) {
                            int affected = reactivateVoucherAfterEdit(voucherNumber);
                            if (affected != 1) {
                                throw new BasicException("Unable to reactivate voucher from edited sale.");
                            }
                            insertVoucherEditAudit(voucherNumber, ticket.getId(), replacementReceiptId,
                                    operatorUserId, authorizationUserId, voucherAuthorizationTime,
                                    "EDIT_REACTIVATED");
                        }
                    }
                }
                return null;
            }
        };
        t.execute();
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public final Integer getNextPickupIndex() throws BasicException {
        return (Integer) s.DB.getSequenceSentence(s, "pickup_number").find();
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public final Integer getNextTicketIndex() throws BasicException {
        return (Integer) s.DB.getSequenceSentence(s, "ticketsnum").find();
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public final Integer getNextTicketRefundIndex() throws BasicException {
        return (Integer) s.DB.getSequenceSentence(s, "ticketsnum_refund").find();
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    public final Integer getNextTicketPaymentIndex() throws BasicException {
        return (Integer) s.DB.getSequenceSentence(s, "ticketsnum_payment").find();
    }

// JG 3 Feb 16 - Product load speedup
    public final SentenceFind getProductImage() {
        return new PreparedSentence(s,
        "SELECT IMAGE FROM products WHERE ID = ?",
        SerializerWriteString.INSTANCE
                , (DataRead dr) -> ImageUtils.readImage(dr.getBytes(1)));
    }
    
    /**
     * Loads on ProductsEditor
     * @return
     */
    public final SentenceList getProductCatQBF() {
 	return new StaticSentence(s
	, new QBFBuilder(
            "SELECT "
                    + "P.ID, "
                    + "P.REFERENCE, "
                    + "P.CODE, "
                    + "P.CODETYPE, "
                    + "P.NAME, "
                    + "P.PRICEBUY, "
                    + "P.PRICESELL, "
                    + "P.CATEGORY, "
                    + "P.TAXCAT, "
                    + "P.ATTRIBUTESET_ID, "
                    + "P.STOCKCOST, "
                    + "P.STOCKVOLUME, "
// JG 3 feb 16 speedup  + "P.IMAGE, "
                    + s.DB.CHAR_NULL() + "," 
                    + "P.ISCOM, "
                    + "P.ISSCALE, "
                    + "P.ISCONSTANT, "
                    + "P.PRINTKB, "
                    + "P.SENDSTATUS, "
                    + "P.ISSERVICE, "
                    + "P.ATTRIBUTES, "
                    + "P.DISPLAY, "
                    + "P.ISVPRICE, "
                    + "P.ISVERPATRIB, "
                    + "P.TEXTTIP, "
                    + "P.WARRANTY, "
                    + "P.STOCKUNITS, "
                    + "P.PRINTTO, "
                    + "P.SUPPLIER, "
                    + "P.UOM, "
                    + "P.MEMODATE, "        
                        + "CASE WHEN "
                            + "C.PRODUCT IS NULL "
                            + "THEN " + s.DB.FALSE() 
                            + " ELSE " + s.DB.TRUE() 
                        + " END, "
                    + "C.CATORDER "
                    + "FROM products P LEFT OUTER JOIN products_cat C "
                    + "ON P.ID = C.PRODUCT "
                    + "WHERE ?(QBF_FILTER) "
                    + "ORDER BY P.REFERENCE", 
                    new String[] {
                        "P.NAME", "P.PRICEBUY", "P.PRICESELL", "P.CATEGORY", "P.CODE|P.REFERENCE"})
	, new SerializerWriteBasic(new Datas[] {
            Datas.OBJECT, Datas.STRING, 
            Datas.OBJECT, Datas.DOUBLE, 
            Datas.OBJECT, Datas.DOUBLE, 
            Datas.OBJECT, Datas.STRING,             
            Datas.OBJECT, Datas.STRING})
	, productsRow.getSerializerRead());
    }

    /**
     *
     * @return
     */
    
    /**
     * Keeps products.supplier and the N:M association table synchronized without
     * removing secondary suppliers. During the compatibility phase, the legacy
     * supplier combo changes only the preferred association.
     */
    private void syncPreferredProductSupplier(Object params) throws BasicException {
        Object[] values = (Object[]) params;
        String supplierId = values[27] == null ? null : values[27].toString().trim();

        new PreparedSentence(s,
                "UPDATE product_suppliers SET is_preferred = " + s.DB.FALSE()
                + " WHERE product_id = ?",
                new SerializerWriteBasicExt(productsRow.getDatas(), new int[]{0}))
                .exec(params);

        if (supplierId == null || supplierId.isEmpty()) {
            return;
        }

        int updated = new PreparedSentence(s,
                "UPDATE product_suppliers SET is_preferred = " + s.DB.TRUE()
                + ", active = " + s.DB.TRUE()
                + " WHERE product_id = ? AND supplier_id = ?",
                new SerializerWriteBasicExt(productsRow.getDatas(), new int[]{0, 27}))
                .exec(params);

        if (updated == 0) {
            new PreparedSentence(s,
                    "INSERT INTO product_suppliers "
                    + "(product_id, supplier_id, is_preferred, active) VALUES (?, ?, "
                    + s.DB.TRUE() + ", " + s.DB.TRUE() + ")",
                    new SerializerWriteBasicExt(productsRow.getDatas(), new int[]{0, 27}))
                    .exec(params);
        }
    }

    public final SentenceExec getProductCatInsert() {
	return new SentenceExecTransaction(s) {
        @Override
	public int execInTransaction(Object params) throws BasicException {
            Object[] values = (Object[]) params;
                int i = new PreparedSentence(s
                , "INSERT INTO products ("
                    + "ID, "
                    + "REFERENCE, "
                    + "CODE, "
                    + "CODETYPE, "
                    + "NAME, "
                    + "PRICEBUY, "
                    + "PRICESELL, "
                    + "CATEGORY, "
                    + "TAXCAT, "
                    + "ATTRIBUTESET_ID, "
                    + "STOCKCOST, "
                    + "STOCKVOLUME, "
                    + "IMAGE, "
                    + "ISCOM, "
                    + "ISSCALE, "
                    + "ISCONSTANT, "
                    + "PRINTKB, "
                    + "SENDSTATUS, "
                    + "ISSERVICE, "
                    + "ATTRIBUTES, "
                    + "DISPLAY, "
                    + "ISVPRICE, "
                    + "ISVERPATRIB, "
                    + "TEXTTIP, "
                    + "WARRANTY, "
                    + "STOCKUNITS, "
                    + "PRINTTO, "
                    + "SUPPLIER, "
                    + "UOM, "
                    + "MEMODATE ) "                        
                    + "VALUES ("
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?)"
		, new SerializerWriteBasicExt(productsRow.getDatas(), 
                new int[]{0, 
                    1, 2, 3, 4, 5, 6, 
                    7, 8, 9, 10, 11, 12, 
                    13, 14, 15, 16, 17, 18,
                    19, 20, 21, 22, 23, 24, 
                    25, 26, 27, 28, 29}))
                    
                    .exec(params);

                if (i > 0) {
                    syncPreferredProductSupplier(params);
                }

                if (i > 0 && ((Boolean)values[30])) {
                    return new PreparedSentence(s
                    , "INSERT INTO products_cat (PRODUCT, CATORDER) VALUES (?, ?)"
                    , new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0, 31}))
                    
                    .exec(params);
                } else {
                    return i;
                }
	}
	};
    }

    /**
     *
     * @return
     */
    public final SentenceExec getProductCatUpdate() {
	return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            Object[] values = (Object[]) params;
		int i = new PreparedSentence(s
                , "UPDATE products SET "
                        + "ID = ?, "
                        + "REFERENCE = ?, "
                        + "CODE = ?, "
                        + "CODETYPE = ?, "
                        + "NAME = ?, "
                        + "PRICEBUY = ?, "
                        + "PRICESELL = ?, "
                        + "CATEGORY = ?, "
                        + "TAXCAT = ?, "
                        + "ATTRIBUTESET_ID = ?, "
                        + "STOCKCOST = ?, "
                        + "STOCKVOLUME = ?, "
                        + "IMAGE = ?, "
                        + "ISCOM = ?, "
                        + "ISSCALE = ?, "
                        + "ISCONSTANT = ?, "
                        + "PRINTKB = ?, "
                        + "SENDSTATUS = ?, "                        
                        + "ISSERVICE = ?,  "
                        + "ATTRIBUTES = ?,"
                        + "DISPLAY = ?, "
                        + "ISVPRICE = ?, "
                        + "ISVERPATRIB = ?, "
                        + "TEXTTIP = ?, "
                        + "WARRANTY = ?, "
                        + "STOCKUNITS = ?, "
                        + "PRINTTO = ?, "
                        + "SUPPLIER = ?, "
                        + "UOM = ?, "
                        + "MEMODATE = ? "
                    + "WHERE ID = ?"
		, new SerializerWriteBasicExt(productsRow.getDatas(), 
                        new int[]{0, 
                            1, 2, 3, 4, 5, 
                            6, 7, 8, 9, 10, 
                            11, 12, 13, 14, 15,
                            16, 17, 18, 19, 20, 
                            21, 22, 23, 24, 25, 
                            26, 27, 28, 29, 0}))
                        .exec(params);
            	if (i > 0) {
                    syncPreferredProductSupplier(params);
                    if (((Boolean)values[30])) {
			if (new PreparedSentence(s
                                , "UPDATE products_cat SET CATORDER = ? WHERE PRODUCT = ?"
                                , new SerializerWriteBasicExt(productsRow.getDatas()
                                , new int[] {31, 0})).exec(params) == 0) {
                            new PreparedSentence(s
				, "INSERT INTO products_cat (PRODUCT, CATORDER) VALUES (?, ?)"
                                , new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0, 31})).exec(params);
                            }
			} else {
                            new PreparedSentence(s
				, "DELETE FROM products_cat WHERE PRODUCT = ?"
				, new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0})).exec(params);
			}
		}
		return i;
            }
	};
    }

    /**
     *
     * @return
     */
    public final SentenceExec getProductCatDelete() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            new PreparedSentence(s
                , "DELETE FROM products_cat WHERE PRODUCT = ?"
                , new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0})).exec(params);
            return new PreparedSentence(s
                , "DELETE FROM products WHERE ID = ?"
                , new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0})).exec(params);
            }
        };
    }

    /**
     *
     * @return
     */
    public final SentenceExec getDebtUpdate() {

        return new PreparedSentence(s
                , "UPDATE customers SET CURDEBT = ?, CURDATE = ? WHERE ID = ?"
                , SerializerWriteParams.INSTANCE);
    }

    /**
     * ProductBundle version
     * @return
     */
    public final SentenceExec getStockDiaryInsert() {
        return new SentenceExecTransaction(s) {
            @Override
            /**
             * @param params[0] String     STOCKDIARY.ID
             * @param params[1] Date       Timestamp
             * @param params[2] Integer    Reason
             * @param params[3] String     Location
             * @param params[4] String     Product ID
             * @param params[5] String     Attribute instance ID
             * @param params[6] Double     Units
             * @param params[7] Double     Price
             * @param params[8] String     Application User
             */            
            public int execInTransaction(Object params) throws BasicException {

                Object[] adjustParams = new Object[4];
                Object[] paramsArray = (Object[]) params;
                adjustParams[0] = paramsArray[4];                               //product ->Location
                adjustParams[1] = paramsArray[3];                               //location -> Product
                adjustParams[2] = paramsArray[5];                               //attributesetinstance
                adjustParams[3] = paramsArray[6];                               //units
                adjustStock(adjustParams);
                
                return new PreparedSentence(s
                    , "INSERT INTO stockdiary (ID, DATENEW, REASON, LOCATION, "
                            + "PRODUCT, ATTRIBUTESETINSTANCE_ID, "
                            + "UNITS, PRICE, AppUser) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    , new SerializerWriteBasicExt(stockdiaryDatas
                            , new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}))
                        .exec(params);
            }
        };
    }    
    
    /**
     * 
     * @return
     */
    public final SentenceExec getStockDiaryInsert1() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
        
            int updateresult = ((Object[]) params)[5] == null 
            ? new PreparedSentence(s
                , "UPDATE stockcurrent SET UNITS = (UNITS + ?) "
                        + "WHERE LOCATION = ? AND PRODUCT = ? "
                        + "AND ATTRIBUTESETINSTANCE_ID IS NULL"
                , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {6, 3, 4})).exec(params)
                : new PreparedSentence(s
                , "UPDATE stockcurrent SET UNITS = (UNITS + ?) "
                        + "WHERE LOCATION = ? AND PRODUCT = ? "
                        + "AND ATTRIBUTESETINSTANCE_ID = ?"
                , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {6, 3, 4, 5})).exec(params);

                if (updateresult == 0) {
                    new PreparedSentence(s
                    , "INSERT INTO stockcurrent (LOCATION, PRODUCT, "
                            + "ATTRIBUTESETINSTANCE_ID, UNITS) "
                            + "VALUES (?, ?, ?, ?)"
                    , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {3, 4, 5, 6})).exec(params);
                }
                learnSupplierFromPurchase((Object[]) params);
                return new PreparedSentence(s
                    , "INSERT INTO stockdiary (ID, DATENEW, REASON, LOCATION, PRODUCT, "
                            + "ATTRIBUTESETINSTANCE_ID, UNITS, PRICE, AppUser, "
                            + "SUPPLIER, SUPPLIERDOC) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                    , new SerializerWriteBasicExt(stockdiaryDatas, 
                            new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10})).exec(params);

            }
        };
    }

    /**
     *
     * @return
     */
    private void learnSupplierFromPurchase(Object[] params) throws BasicException {
        if (params == null || params.length < 10 || !(params[2] instanceof Number)
                || ((Number) params[2]).intValue() != 1) {
            return;
        }

        String productId = params[4] == null ? null : params[4].toString().trim();
        String supplierId = params[9] == null ? null : params[9].toString().trim();
        if (productId == null || productId.isEmpty()
                || supplierId == null || supplierId.isEmpty()) {
            return;
        }

        Double lastCost = params[7] instanceof Number
                ? ((Number) params[7]).doubleValue() : null;
        Timestamp lastPurchase = params[1] instanceof java.util.Date
                ? new Timestamp(((java.util.Date) params[1]).getTime()) : null;

        try {
            Connection connection = s.getConnection();
            // Use the product row as the serialization point shared with the
            // product-supplier editor. Without this lock, a concurrent editor
            // save could delete an association learned by this purchase.
            try (PreparedStatement lockProduct = connection.prepareStatement(
                    "SELECT id FROM products WHERE id = ? FOR UPDATE")) {
                lockProduct.setString(1, productId);
                try (ResultSet product = lockProduct.executeQuery()) {
                    if (!product.next()) {
                        throw new SQLException("Unknown product while learning supplier: " + productId);
                    }
                }
            }
            boolean associationExists;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM product_suppliers WHERE product_id = ? AND supplier_id = ?")) {
                statement.setString(1, productId);
                statement.setString(2, supplierId);
                try (ResultSet rows = statement.executeQuery()) {
                    associationExists = rows.next();
                }
            }

            if (associationExists) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE product_suppliers SET active = ?, last_cost = ?, last_purchase = ? "
                        + "WHERE product_id = ? AND supplier_id = ?")) {
                    statement.setBoolean(1, true);
                    if (lastCost == null) {
                        statement.setNull(2, java.sql.Types.DOUBLE);
                    } else {
                        statement.setDouble(2, lastCost);
                    }
                    statement.setTimestamp(3, lastPurchase);
                    statement.setString(4, productId);
                    statement.setString(5, supplierId);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO product_suppliers "
                        + "(product_id, supplier_id, is_preferred, active, last_cost, last_purchase) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    statement.setString(1, productId);
                    statement.setString(2, supplierId);
                    statement.setBoolean(3, false);
                    statement.setBoolean(4, true);
                    if (lastCost == null) {
                        statement.setNull(5, java.sql.Types.DOUBLE);
                    } else {
                        statement.setDouble(5, lastCost);
                    }
                    statement.setTimestamp(6, lastPurchase);
                    statement.executeUpdate();
                }

            }

            // Learn new suppliers without replacing an existing preference.
            // This conditional update serializes concurrent first purchases.
            int becamePreferred;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE products SET supplier = ? WHERE id = ? "
                    + "AND (supplier IS NULL OR TRIM(supplier) = '')")) {
                statement.setString(1, supplierId);
                statement.setString(2, productId);
                becamePreferred = statement.executeUpdate();
            }

            if (becamePreferred == 1) {
                try (PreparedStatement clear = connection.prepareStatement(
                        "UPDATE product_suppliers SET is_preferred = ? WHERE product_id = ?");
                        PreparedStatement prefer = connection.prepareStatement(
                                "UPDATE product_suppliers SET is_preferred = ?, active = ? "
                                + "WHERE product_id = ? AND supplier_id = ?")) {
                    clear.setBoolean(1, false);
                    clear.setString(2, productId);
                    clear.executeUpdate();

                    prefer.setBoolean(1, true);
                    prefer.setBoolean(2, true);
                    prefer.setString(3, productId);
                    prefer.setString(4, supplierId);
                    if (prefer.executeUpdate() != 1) {
                        throw new SQLException("Purchase supplier association was not updated exactly once.");
                    }
                }
            }
        } catch (SQLException ex) {
            throw new BasicException("Unable to learn product supplier from purchase.", ex);
        }
    }

    /**
     *
     * @return
     */

    public final SentenceExec getStockDiaryDelete() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            int updateresult = ((Object[]) params)[5] == null // if ATTRIBUTESETINSTANCE_ID is null
            ? new PreparedSentence(s
                , "UPDATE stockcurrent SET UNITS = (UNITS - ?) "
                        + "WHERE LOCATION = ? AND PRODUCT = ? "
                        + "AND ATTRIBUTESETINSTANCE_ID IS NULL"
            , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {6, 3, 4})).exec(params)
            : new PreparedSentence(s
                , "UPDATE stockcurrent SET UNITS = (UNITS - ?) "
                        + "WHERE LOCATION = ? AND PRODUCT = ? "
                        + "AND ATTRIBUTESETINSTANCE_ID = ?"
            , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {6, 3, 4, 5})).exec(params);

            if (updateresult == 0) {
                new PreparedSentence(s
                , "INSERT INTO stockcurrent (LOCATION, PRODUCT, "
                        + "ATTRIBUTESETINSTANCE_ID, UNITS) "
                        + "VALUES (?, ?, ?, -(?))"
                , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {3, 4, 5, 6})).exec(params);
            }
            return new PreparedSentence(s
                , "DELETE FROM stockdiary WHERE ID = ?"
                , new SerializerWriteBasicExt(stockdiaryDatas, new int[] {0})).exec(params);
            }
        };
    }
    

    private void adjustStock(Object params) throws BasicException {

        List<ProductsBundleInfo> bundle = getProductsBundle((String) ((Object[])params)[0]);

        if (bundle.size() > 0) {

            for (ProductsBundleInfo component : bundle) {
                Object[] adjustParams = new Object[4];
                adjustParams[0] = component.getProductBundleId();
                adjustParams[1] = ((Object[])params)[1];                
                adjustParams[2] = ((Object[])params)[2];
                adjustParams[3] = ((Double)((Object[])params)[3]) * component.getQuantity();
                adjustStock(adjustParams);
            }
        } else {

            int updateresult = ((Object[]) params)[2] == null
                ? new PreparedSentence(s
                    , "UPDATE stockcurrent SET UNITS = (UNITS + ?) "
                           + "WHERE LOCATION = ? AND PRODUCT = ? "
                           + "AND ATTRIBUTESETINSTANCE_ID IS NULL"
                    , new SerializerWriteBasicExt(stockAdjustDatas
                           , new int[] {3, 1, 0}))
                        .exec(params)
                : new PreparedSentence(s
                    , "UPDATE stockcurrent SET UNITS = (UNITS + ?) "
                           + "WHERE LOCATION = ? AND PRODUCT = ? "
                           + "AND ATTRIBUTESETINSTANCE_ID = ?"
                    , new SerializerWriteBasicExt(stockAdjustDatas
                           , new int[] {3, 1, 0, 2}))
                        .exec(params);

            if (updateresult == 0) {

                new PreparedSentence(s
                    , "INSERT INTO stockcurrent (LOCATION, PRODUCT, "
                            + "ATTRIBUTESETINSTANCE_ID, UNITS) "
                            + "VALUES (?, ?, ?, ?)"
                    , new SerializerWriteBasicExt(stockAdjustDatas
                            , new int[] {1, 0, 2, 3}))
                        .exec(params);
            }
        }
    }

    /**
     *
     * @return
     */
    public final SentenceExec getPaymentMovementInsert() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            new PreparedSentence(s
            , "INSERT INTO receipts (ID, MONEY, DATENEW) "
                    + "VALUES (?, ?, ?)"
            , new SerializerWriteBasicExt(paymenttabledatas, 
                    new int[] {0, 1, 2})).exec(params);
            return new PreparedSentence(s
            , "INSERT INTO payments (ID, RECEIPT, PAYMENT, TOTAL, NOTES) "
                    + "VALUES (?, ?, ?, ?, ?)"
            , new SerializerWriteBasicExt(paymenttabledatas, 
                    new int[] {3, 0, 4, 5, 6})).exec(params);
            }
        };
    }

    /**
     *
     * @return
     */
    public final SentenceExec getPaymentMovementDelete() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            new PreparedSentence(s
            , "DELETE FROM payments WHERE ID = ?"
            , new SerializerWriteBasicExt(paymenttabledatas, new int[] {3})).exec(params);
            return new PreparedSentence(s
            , "DELETE FROM receipts WHERE ID = ?"
            , new SerializerWriteBasicExt(paymenttabledatas, new int[] {0})).exec(params);
            }
        };
    }

    /**
     *
     * @param warehouse
     * @param id
     * @param attsetinstid
     * @return
     * @throws BasicException
     */
    public final double findProductStock(String warehouse, String id, String attsetinstid) throws BasicException {

        PreparedSentence p = attsetinstid == null
                ? new PreparedSentence(s, "SELECT UNITS FROM stockcurrent "
                        + "WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID IS NULL"
                    , new SerializerWriteBasic(Datas.STRING, Datas.STRING)
                    , SerializerReadDouble.INSTANCE)
                : new PreparedSentence(s, "SELECT UNITS FROM stockcurrent "
                        + "WHERE LOCATION = ? AND PRODUCT = ? AND ATTRIBUTESETINSTANCE_ID = ?"
                    , new SerializerWriteBasic(Datas.STRING, Datas.STRING, Datas.STRING)
                    , SerializerReadDouble.INSTANCE);

        Double d = (Double) p.find(warehouse, id, attsetinstid);
        return d == null ? 0.0 : d;
    }

    /**
     *
     * @return
     */
    public final SentenceExec getCatalogCategoryAdd() {
        return new StaticSentence(s
                , "INSERT INTO products_cat(PRODUCT, CATORDER) "
                + "SELECT P.ID, " + s.DB.INTEGER_NULL() + " FROM products P "
                + "WHERE P.CATEGORY = ? AND NOT EXISTS "
                + "(SELECT 1 FROM products_cat C WHERE C.PRODUCT = P.ID)"
                , SerializerWriteString.INSTANCE);
    }

    /** Returns {total products in category, products currently in sales catalog}. */
    public final int[] getCatalogCategoryCounts(String category) throws BasicException {
        Integer total = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM products WHERE CATEGORY = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(category);
        Integer inCatalog = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM products P INNER JOIN products_cat C ON P.ID = C.PRODUCT WHERE P.CATEGORY = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(category);
        return new int[] {total == null ? 0 : total, inCatalog == null ? 0 : inCatalog};
    }

    /** Number of catalog rows in a category with an explicit sales-button order. */
    public final int getCatalogCategoryOrderedCount(String category) throws BasicException {
        Integer ordered = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM products P INNER JOIN products_cat C ON P.ID = C.PRODUCT "
                + "WHERE P.CATEGORY = ? AND C.CATORDER IS NOT NULL",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(category);
        return ordered == null ? 0 : ordered;
    }

    /**
     *
     * @return
     */
    public final SentenceExec getCatalogCategoryDel() {
        return new StaticSentence(s
                , "DELETE FROM products_cat WHERE PRODUCT = ANY (SELECT ID "
                        + "FROM products WHERE CATEGORY = ?)"
                , SerializerWriteString.INSTANCE);
    }

    /**
     *
     * @return
     */
        public final TableDefinition getTableCategories() {
        return new TableDefinition(s,
            "categories"
            , new String[] {"ID", "NAME", "PARENTID", "IMAGE", "TEXTTIP", "CATSHOWNAME", "CATORDER"}
            , new String[] {"ID", AppLocal.getIntString("label.name"), "", AppLocal.getIntString("label.image")}
            , new Datas[] {Datas.STRING, Datas.STRING, Datas.STRING, 
                Datas.IMAGE, Datas.STRING, Datas.BOOLEAN, Datas.STRING}
            , new Formats[] {Formats.STRING, Formats.STRING, Formats.STRING, 
                Formats.NULL, Formats.STRING, Formats.BOOLEAN, Formats.STRING}
            , new int[] {0}
        );
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTableTaxes() {
        return new TableDefinition(s,
            "taxes"
            , new String[] {"ID", "NAME", "CATEGORY", "CUSTCATEGORY", "PARENTID", "RATE", "RATECASCADE", "RATEORDER"}
            , new String[] {"ID", AppLocal.getIntString("label.name"), AppLocal.getIntString("label.taxcategory"), AppLocal.getIntString("label.custtaxcategory"), AppLocal.getIntString("label.taxparent"), AppLocal.getIntString("label.dutyrate"), AppLocal.getIntString("label.cascade"), AppLocal.getIntString("label.order")}
            , new Datas[] {Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.STRING, Datas.DOUBLE, Datas.BOOLEAN, Datas.INT}
            , new Formats[] {Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.STRING, Formats.PERCENT, Formats.BOOLEAN, Formats.INT}
            , new int[] {0}
        );
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTableTaxCustCategories() {
        return new TableDefinition(s,
            "taxcustcategories"
            , new String[] {"ID", "NAME"}
            , new String[] {"ID", AppLocal.getIntString("label.name")}
            , new Datas[] {Datas.STRING, Datas.STRING}
            , new Formats[] {Formats.STRING, Formats.STRING}
            , new int[] {0}
        );
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTableTaxCategories() {
        return new TableDefinition(s,
            "taxcategories"
            , new String[] {"ID", "NAME"}
            , new String[] {"ID", AppLocal.getIntString("label.name")}
            , new Datas[] {Datas.STRING, Datas.STRING}
            , new Formats[] {Formats.STRING, Formats.STRING}
            , new int[] {0}
        );
    }

    /**
     *
     * @return
     */
    public final TableDefinition getTableLocations() {
        return new TableDefinition(s,
            "locations"
            , new String[] {"ID", "NAME", "ADDRESS"}
            , new String[] {"ID", AppLocal.getIntString("label.locationname"), 
                AppLocal.getIntString("label.locationaddress")}
            , new Datas[] {Datas.STRING, Datas.STRING, Datas.STRING}
            , new Formats[] {Formats.STRING, Formats.STRING, Formats.STRING}
            , new int[] {0}
        );
    }
        

    /**
     *
     */
    protected static class CustomerExtRead implements SerializerRead {

        /**
         *
         * @param dr
         * @return
         * @throws BasicException
         */
        @Override
        public Object readValues(DataRead dr) throws BasicException {
            CustomerInfoExt c = new CustomerInfoExt(dr.getString(1));
            c.setSearchkey(dr.getString(2));
            c.setTaxid(dr.getString(3));
            c.setTaxCustomerID(dr.getString(3));            
            c.setName(dr.getString(4));
            c.setTaxCustCategoryID(dr.getString(5));
            c.setCard(dr.getString(6));
            c.setMaxdebt(dr.getDouble(7));
            c.setAddress(dr.getString(8));
            c.setAddress2(dr.getString(9));
            c.setPcode(dr.getString(10));
            c.setCity(dr.getString(11));
            c.setRegion(dr.getString(12));
            c.setCountry(dr.getString(13));
            c.setFirstname(dr.getString(14));
            c.setLastname(dr.getString(15));
            c.setCemail(dr.getString(16));
            c.setPhone1(dr.getString(17));
            c.setPhone2(dr.getString(18));
            c.setFax(dr.getString(19));
            c.setNotes(dr.getString(20));
            c.setVisible(dr.getBoolean(21));
            c.setCurdate(dr.getTimestamp(22));
            c.setAccdebt(dr.getDouble(23));
            c.setImage(ImageUtils.readImage(dr.getBytes(24)));
            c.setisVIP(dr.getBoolean(25));
            c.setDiscount(dr.getDouble(26));
            c.setMemoDate(dr.getString(27));
            
            return c;
        }
    }
    
    public final UomInfo getUomInfoById(String id) throws BasicException {
        return (UomInfo) new PreparedSentence(s, 
                "SELECT " +
                    "id, name " +
                "FROM uom " +
                "WHERE id = ?"
            , SerializerWriteString.INSTANCE, UomInfo.getSerializerRead()).find(id);
    } 

    public final TableDefinition getTableUom() {
        return new TableDefinition(s,
                "uom", 
                new String[]{"id", "name"}, 
                new String[]{"id", 
                    AppLocal.getIntString("Label.Name")}, 
                new Datas[]{
                    Datas.STRING, Datas.STRING}, 
                new Formats[]{
                    Formats.STRING, Formats.STRING}, 
                new int[]{0}
        );
    }    

    public final SentenceList getUomList() {
        return new StaticSentence(s, "SELECT ID, NAME  FROM uom ORDER BY NAME", null, UomInfo.getSerializerRead());
    }       
    
    public final SentenceList getVoucherList() {
        return new StaticSentence(s,
            "SELECT " +
                "vouchers.ID,vouchers.VOUCHER_NUMBER,vouchers.CUSTOMER, " +
                "customers.NAME,AMOUNT, STATUS, EXPIRES_AT " +
              "FROM vouchers   " +
                "JOIN customers ON customers.id = vouchers.CUSTOMER  " +
              "WHERE STATUS='A' " +
              "AND (EXPIRES_AT IS NULL OR EXPIRES_AT > CURRENT_TIMESTAMP) " +
              "ORDER BY vouchers.VOUCHER_NUMBER ASC"
            , null, VoucherInfo.getSerializerRead());
    }

    public final SentenceList getVoucherListByCustomer() {
        return new PreparedSentence(s,
            "SELECT " +
                "vouchers.ID,vouchers.VOUCHER_NUMBER,vouchers.CUSTOMER, " +
                "customers.NAME,AMOUNT, STATUS, EXPIRES_AT " +
              "FROM vouchers " +
                "JOIN customers ON customers.id = vouchers.CUSTOMER " +
              "WHERE vouchers.STATUS='A' AND vouchers.CUSTOMER = ? " +
              "AND (vouchers.EXPIRES_AT IS NULL OR vouchers.EXPIRES_AT > CURRENT_TIMESTAMP) " +
              "ORDER BY vouchers.VOUCHER_NUMBER ASC",
            SerializerWriteString.INSTANCE,
            VoucherInfo.getSerializerRead());
    }

    public final List<VoucherInfo> getVoucherListByCustomerForEdit(final String customerId,
            final String sourceReceiptId) throws BasicException {
        return new PreparedSentence(s,
                "SELECT V.ID, V.VOUCHER_NUMBER, V.CUSTOMER, C.NAME, V.AMOUNT, V.STATUS, V.EXPIRES_AT "
                + "FROM vouchers V JOIN customers C ON C.ID = V.CUSTOMER "
                + "WHERE V.CUSTOMER = ? "
                + "AND (V.EXPIRES_AT IS NULL OR V.EXPIRES_AT > CURRENT_TIMESTAMP) "
                + "AND (V.STATUS = 'A' OR (V.STATUS = 'D' AND (EXISTS ("
                + "SELECT 1 FROM payments P WHERE P.RECEIPT = ? "
                + "AND P.PAYMENT = 'voucherin' AND P.VOUCHER = V.VOUCHER_NUMBER) "
                + "OR EXISTS (SELECT 1 FROM voucher_redemptions R WHERE R.RECEIPT_ID = ? "
                + "AND R.VOUCHER_NUMBER = V.VOUCHER_NUMBER AND R.STATUS = 'ACTIVE')))) "
                + "ORDER BY V.VOUCHER_NUMBER ASC",
                SerializerWriteParams.INSTANCE,
                VoucherInfo.getSerializerRead()).list(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, customerId);
                        setString(2, sourceReceiptId);
                        setString(3, sourceReceiptId);
                    }
                });
    }

    private boolean receiptExists(String receiptId) throws BasicException {
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM receipts WHERE ID = ?",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(receiptId);
        return count != null && count == 1;
    }

    private boolean sourceNormalReceiptExists(String sourceReceiptId) throws BasicException {
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM tickets WHERE ID = ? AND TICKETTYPE = 0",
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(sourceReceiptId);
        return count != null && count == 1;
    }

    private int countVoucherUsages(String receiptId) throws BasicException {
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT (SELECT COUNT(*) FROM payments WHERE RECEIPT = ? "
                + "AND PAYMENT = 'voucherin' AND VOUCHER IS NOT NULL AND VOUCHER <> '') "
                + "+ (SELECT COUNT(*) FROM voucher_redemptions WHERE RECEIPT_ID = ? AND STATUS = 'ACTIVE')",
                SerializerWriteParams.INSTANCE,
                SerializerReadInteger.INSTANCE).find(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, receiptId);
                        setString(2, receiptId);
                    }
                });
        return count == null ? 0 : count;
    }

    private boolean isAuthorizedVoucherEditUser(String userId) throws BasicException {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM people WHERE ID = ? AND ROLE IN ('0','1') "
                + "AND VISIBLE = " + s.DB.TRUE(),
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(userId);
        return count != null && count == 1;
    }

    private boolean lockSourceNormalReceiptForVoucherEdit(String sourceReceiptId) throws BasicException {
        if (sourceReceiptId == null || sourceReceiptId.isBlank()) {
            return false;
        }
        String lockedId = (String) new PreparedSentence(s,
                "SELECT ID FROM tickets WHERE ID = ? AND TICKETTYPE = 0 FOR UPDATE",
                SerializerWriteString.INSTANCE,
                SerializerReadString.INSTANCE).find(sourceReceiptId);
        return sourceReceiptId.equals(lockedId);
    }

    private boolean isVisibleVoucherEditOperator(String userId) throws BasicException {
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM people WHERE ID = ? AND VISIBLE = " + s.DB.TRUE(),
                SerializerWriteString.INSTANCE,
                SerializerReadInteger.INSTANCE).find(userId);
        return count != null && count == 1;
    }

    private Date parseVoucherAuthorizationTime(String value) throws BasicException {
        if (value == null || value.isBlank()) {
            throw new BasicException("Voucher edit audit is missing its authorization timestamp.");
        }
        try {
            return Date.from(Instant.parse(value));
        } catch (RuntimeException ex) {
            throw new BasicException("Voucher edit audit has an invalid authorization timestamp.", ex);
        }
    }

    private void insertVoucherEditAudit(final String voucherNumber,
            final String originalReceiptId, final String newReceiptId,
            final String operatorUserId, final String authorizationUserId,
            final Date authorizationTime, final String reason) throws BasicException {
        if (voucherNumber == null || voucherNumber.isBlank()
                || originalReceiptId == null || originalReceiptId.isBlank()
                || newReceiptId == null || newReceiptId.isBlank()
                || operatorUserId == null || operatorUserId.isBlank()
                || authorizationUserId == null || authorizationUserId.isBlank()
                || authorizationTime == null || reason == null || reason.isBlank()) {
            throw new BasicException("Incomplete voucher edit audit context.");
        }
        new PreparedSentence(s,
                "INSERT INTO voucher_reactivation_audit "
                + "(ID, VOUCHER_NUMBER, ORIGINAL_RECEIPT, NEW_RECEIPT, OPERATOR_ID, "
                + "AUTHORIZED_BY, AUTHORIZED_AT, REASON) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                SerializerWriteParams.INSTANCE).exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, UUID.randomUUID().toString());
                        setString(2, voucherNumber);
                        setString(3, originalReceiptId);
                        setString(4, newReceiptId);
                        setString(5, operatorUserId);
                        setString(6, authorizationUserId);
                        setTimestamp(7, authorizationTime);
                        setString(8, reason);
                    }
                });
    }

    public boolean isReusableVoucherFromSourceReceipt(String sourceReceiptId,
            String voucherNumber) throws BasicException {
        if (sourceReceiptId == null || voucherNumber == null) {
            return false;
        }
        Integer count = (Integer) new PreparedSentence(s,
                "SELECT COUNT(*) FROM vouchers V WHERE V.VOUCHER_NUMBER = ? AND V.STATUS = 'D' "
                + "AND (V.EXPIRES_AT IS NULL OR V.EXPIRES_AT > CURRENT_TIMESTAMP) AND ("
                + "EXISTS (SELECT 1 FROM payments P WHERE P.RECEIPT = ? "
                + "AND P.PAYMENT = 'voucherin' AND P.VOUCHER = V.VOUCHER_NUMBER) "
                + "OR EXISTS (SELECT 1 FROM voucher_redemptions R WHERE R.RECEIPT_ID = ? "
                + "AND R.VOUCHER_NUMBER = V.VOUCHER_NUMBER AND R.STATUS = 'ACTIVE'))",
                SerializerWriteParams.INSTANCE,
                SerializerReadInteger.INSTANCE).find(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, voucherNumber);
                        setString(2, sourceReceiptId);
                        setString(3, sourceReceiptId);
                    }
                });
        return count != null && count == 1;
    }

    private List<String> getVoucherPaymentsForReceipt(String receiptId) throws BasicException {
        return new PreparedSentence(s,
                "SELECT VOUCHER FROM payments WHERE RECEIPT = ? "
                + "AND PAYMENT = 'voucherin' AND VOUCHER IS NOT NULL AND VOUCHER <> '' "
                + "UNION SELECT VOUCHER_NUMBER FROM voucher_redemptions "
                + "WHERE RECEIPT_ID = ? AND STATUS = 'ACTIVE'",
                SerializerWriteParams.INSTANCE,
                SerializerReadString.INSTANCE).list(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, receiptId);
                        setString(2, receiptId);
                    }
                });
    }

    private int reactivateVoucherAfterEdit(final String voucherNumber)
            throws BasicException {
        return new PreparedSentence(s,
                "UPDATE vouchers SET STATUS = 'A' WHERE VOUCHER_NUMBER = ? AND STATUS = 'D'",
                SerializerWriteString.INSTANCE).exec(voucherNumber);
    }

    private int consumeVoucher(final VoucherDiscountInfo discount, final String customerId)
            throws BasicException {
        return new PreparedSentence(s,
                "UPDATE vouchers SET STATUS = 'D' "
                + "WHERE ID = ? AND VOUCHER_NUMBER = ? AND STATUS = 'A' "
                + "AND (CUSTOMER = ? OR CUSTOMER IS NULL) "
                + "AND (EXPIRES_AT IS NULL OR EXPIRES_AT > CURRENT_TIMESTAMP)",
                SerializerWriteParams.INSTANCE).exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, discount.getVoucherId());
                        setString(2, discount.getVoucher());
                        setString(3, customerId);
                    }
                });
    }

    private void insertVoucherRedemption(final VoucherDiscountInfo discount, final TicketInfo ticket)
            throws BasicException {
        new PreparedSentence(s,
                "INSERT INTO voucher_redemptions "
                + "(ID, VOUCHER_ID, VOUCHER_NUMBER, RECEIPT_ID, CUSTOMER_ID, "
                + "VOUCHER_CUSTOMER_BEFORE, DISCOUNT_AMOUNT, REDEEMED_AT, OPERATOR_ID, STATUS, "
                + "REVERSED_AT, REPLACEMENT_RECEIPT_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NULL, NULL)",
                SerializerWriteParams.INSTANCE).exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, UUID.randomUUID().toString());
                        setString(2, discount.getVoucherId());
                        setString(3, discount.getVoucher());
                        setString(4, ticket.getId());
                        setString(5, ticket.getCustomerId());
                        setString(6, discount.getCustomerId());
                        setDouble(7, discount.getTotal());
                        setTimestamp(8, ticket.getDate());
                        setString(9, ticket.getUser().getId());
                    }
                });
    }

    private void reverseVoucherRedemptions(final String sourceReceiptId,
            final String replacementReceiptId) throws BasicException {
        new PreparedSentence(s,
                "UPDATE voucher_redemptions SET STATUS = 'REVERSED', REVERSED_AT = CURRENT_TIMESTAMP, "
                + "REPLACEMENT_RECEIPT_ID = ? WHERE RECEIPT_ID = ? AND STATUS = 'ACTIVE'",
                SerializerWriteParams.INSTANCE).exec(new DataParams() {
                    @Override
                    public void writeValues() throws BasicException {
                        setString(1, replacementReceiptId);
                        setString(2, sourceReceiptId);
                    }
                });
    }

    public final SentenceExec resetPickupId() {

        return new PreparedSentence(s, 
                "UPDATE pickup_number SET ID=1 " 
                , SerializerWriteString.INSTANCE);

    }     
    
    /**
     *
     * @param id
     * @return
     * @throws BasicException
     */
    public SupplierInfoExt loadSupplierExt(String id) throws BasicException {
        return (SupplierInfoExt) new PreparedSentence(s
                , "SELECT "
                + "ID, "
                + "SEARCHKEY, "
                + "TAXID, " 
                + "NAME, "
                + "MAXDEBT, "
                + "ADDRESS, "
                + "ADDRESS2, "
                + "POSTAL, "
                + "CITY, "
                + "REGION, "
                + "COUNTRY, "
                + "FIRSTNAME, "
                + "LASTNAME, "
                + "EMAIL, "
                + "PHONE, "
                + "PHONE2, "
                + "FAX, "
                + "NOTES, "
                + "VISIBLE, "
                + "CURDATE, "
                + "CURDEBT, "
                + "VATID "
                + "FROM suppliers WHERE ID = ?"
                , SerializerWriteString.INSTANCE
                , new SupplierExtRead()).find(id);
    } 
    
    /**
     *
     */
    protected static class SupplierExtRead implements SerializerRead {

        /**
         *
         * @param dr
         * @return
         * @throws BasicException
         */
        @Override
        public Object readValues(DataRead dr) throws BasicException {
            SupplierInfoExt s = new SupplierInfoExt(dr.getString(1));
            s.setSearchkey(dr.getString(2));
            s.setTaxid(dr.getString(3));
            s.setName(dr.getString(4));
            s.setMaxdebt(dr.getDouble(5));
            s.setAddress(dr.getString(6));
            s.setAddress2(dr.getString(7));
            s.setPostal(dr.getString(8));
            s.setCity(dr.getString(9));
            s.setRegion(dr.getString(10));
            s.setCountry(dr.getString(11));
            s.setFirstname(dr.getString(12));
            s.setLastname(dr.getString(13));
            s.setEmail(dr.getString(14));
            s.setPhone(dr.getString(15));
            s.setPhone2(dr.getString(16));
            s.setFax(dr.getString(17));
            s.setNotes(dr.getString(18));
            s.setVisible(dr.getBoolean(19));
            s.setCurdate(dr.getTimestamp(20));
            s.setCurdebt(dr.getDouble(21));
            s.setSupplierVATID(dr.getString(22));

            return s;
        }
    }
    
    /**
     *
     * @return
     */
    
    public final SentenceExec getCustomerInsert() {
	return new SentenceExecTransaction(s) {
        @Override
	public int execInTransaction(Object params) throws BasicException {
            Object[] values = (Object[]) params;
                int i = new PreparedSentence(s
                , "INSERT INTO customers ("
                    + "ID, "
                    + "SEARCHKEY, "
                    + "TAXID, "
                    + "NAME, "
                    + "TAXCATEGORY, "
                    + "CARD, "
                    + "MAXDEBT, "
                    + "ADDRESS, "
                    + "ADDRESS2, "
                    + "POSTAL, "
                    + "CITY, "
                    + "REGION, "
                    + "COUNTRY, "
                    + "FIRSTNAME, "
                    + "LASTNAME, "
                    + "EMAIL, "
                    + "PHONE, "
                    + "PHONE2, "
                    + "FAX, "
                    + "NOTES, "
                    + "VISIBLE, "
                    + "CURDATE, "
                    + "CURDEBT, "
                    + "IMAGE, "
                    + "ISVIP, "
                    + "DISCOUNT, "
                    + "MEMODATE ) "                        
                    + "VALUES ("
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?, ?, ?, ?, "
                    + "?, ?, ?)"
		, new SerializerWriteBasicExt(customersRow.getDatas(), 
                new int[]{0, 
                    1, 2, 3, 4, 5, 6, 
                    7, 8, 9, 10, 11, 12, 
                    13, 14, 15, 16, 17, 18,
                    19, 20, 21, 22, 23, 24, 
                    25, 26}))
                    
                    .exec(params);
                    return i;
	}
	};
    }

    /**
     *
     * @return
     */
    public final SentenceExec getCustomerUpdate() {
	return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            Object[] values = (Object[]) params;
		int i = new PreparedSentence(s
                , "UPDATE customers SET "
                        + "ID = ?, "
                        + "SEARCHKEY = ?, "
                        + "TAXID = ?, "
                        + "NAME = ?, "
                        + "TAXCATEGORY = ?, "
                        + "CARD = ?, "
                        + "MAXDEBT = ?, "
                        + "ADDRESS = ?, "
                        + "ADDRESS2 = ?, "
                        + "POSTAL = ?, "
                        + "CITY = ?, "
                        + "REGION = ?, "
                        + "COUNTRY = ?, "
                        + "FIRSTNAME = ?, "
                        + "LASTNAME = ?, "
                        + "EMAIL = ?, "
                        + "PHONE = ?, "
                        + "PHONE2 = ?, "                        
                        + "FAX = ?,  "
                        + "NOTES = ?,"
                        + "VISIBLE = ?, "
                        + "CURDATE = ?, "
                        + "CURDEBT = ?, "
                        + "IMAGE = ?, "
                        + "ISVIP = ?, "
                        + "DISCOUNT = ?, "
                        + "MEMODATE = ? "                        
                    + "WHERE ID = ?"
		, new SerializerWriteBasicExt(customersRow.getDatas(), 
                        new int[]{0, 
                            1, 2, 3, 4, 5, 
                            6, 7, 8, 9, 10, 
                            11, 12, 13, 14, 15,
                            16, 17, 18, 19, 20, 
                            21, 22, 23, 24, 25, 
                            26, 0}))
                        .exec(params);

/*                
 * Use this block workflow as template to pump LOYALTY, MEMBERSHIP & etc
 * updates to internal or external DB table               
                if (i > 0) {
                    if (((Boolean)values[n0])) {
			if (new PreparedSentence(s
                                , "UPDATE tablename SET FIELD = ? WHERE CUSTOMER = ?"
                                , new SerializerWriteBasicExt(customersRow.getDatas()
                                , new int[] {n1, 0})).exec(params) == 0) {
                            new PreparedSentence(s
				, "INSERT INTO other_tablename (CUSTOMER, FIELD) VALUES (?, ?)"
                                , new SerializerWriteBasicExt(productsRow.getDatas(), new int[] {0, n1})).exec(params);
                            }
			} else {
                            new PreparedSentence(s
				, "DELETE FROM FIELD WHERE CUSTOMER = ?"
				, new SerializerWriteBasicExt(customersRow.getDatas(), new int[] {0})).exec(params);
			}
		}
*/                
		return i;
            }
	};
    }

    /**
     *
     * @return
     */
    public final SentenceExec getCustomerDelete() {
        return new SentenceExecTransaction(s) {
        @Override
        public int execInTransaction(Object params) throws BasicException {
            return new PreparedSentence(s
                , "DELETE FROM customers WHERE ID = ?"
                , new SerializerWriteBasicExt(customersRow.getDatas(), 
                        new int[] {0}
                )).exec(params);
            }
        };
    }
  
    
}