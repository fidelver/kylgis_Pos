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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Creation and Editing of stored settings
 * @author JG uniCenta
 */
public class AppConfig implements AppProperties {

    private static final Logger logger = Logger.getLogger("com.mx.kylgis.pos.forms.AppConfig");
     
    private static AppConfig m_instance = null;
    private Properties m_propsconfig;
    private File configfile;  
      
    /**
     * Set configuration array
     * @param args array strings
     */
    public AppConfig(String[] args) {
        if (args.length == 0) {
            init(getDefaultConfig());
        } else {
            init(new File(args[0]));
        }
    }
      
    /**
     * unicenta resources file
     * @param configfile resource file
     */
    public AppConfig(File configfile) {
        init(configfile);
        this.configfile = configfile;
        m_propsconfig = new Properties();
        load();
        logger.log(Level.INFO, "Reading configuration file: {0}", configfile.getAbsolutePath());        
    }
    
    private void init(File configfile) {
        this.configfile = configfile;
        m_propsconfig = new Properties();

        logger.log(Level.INFO, "Reading configuration file: {0}", configfile.getAbsolutePath());
    }
    
    private File getDefaultConfig() {
        return new File(new File(System.getProperty("user.home"))
            , AppLocal.APP_ID + ".properties");            
    }
    
    /**
     * Get key pair value from properties resource
     * @param sKey key pair value
     * @return key pair from .properties filename
     */
    @Override
    public String getProperty(String sKey) {
        return m_propsconfig.getProperty(sKey);
    }
    
    /**
     *
     * @return Machine name
     */
    @Override
    public String getHost() {
        return getProperty("machine.hostname");
    }

    /**
     *
     * @return .properties filename
     */
    @Override
    public File getConfigFile() {
        return configfile;
    }
    
   
    public String getTicketHeaderLine1() {
        return getProperty("tkt.header1");
    }

    public String getTicketHeaderLine2() {
        return getProperty("tkt.header2");
    }
    
    public String getTicketHeaderLine3() {
        return getProperty("tkt.header3");
    }
    
    public String getTicketHeaderLine4() {
        return getProperty("tkt.header4");
    }
   
    public String getTicketHeaderLine5() {
        return getProperty("tkt.header5");
    }

    public String getTicketHeaderLine6() {
        return getProperty("tkt.header6");
    }
    
    public String getTicketFooterLine1() {
        return getProperty("tkt.footer1");
    }

    public String getTicketFooterLine2() {
        return getProperty("tkt.footer2");
    }

    public String getTicketFooterLine3() {
        return getProperty("tkt.footer3");
    }

    public String getTicketFooterLine4() {
        return getProperty("tkt.footer4");
    }

    public String getTicketFooterLine5() {
        return getProperty("tkt.footer5");
    }

    public String getTicketFooterLine6() {
        return getProperty("tkt.footer6");
    }    
    
    /**
     * Update .properties resource key pair values
     * @param sKey  key pair left side
     * @param sValue key pair right side value
     */
    public void setProperty(String sKey, String sValue) {
        if (sValue == null) {
            m_propsconfig.remove(sKey);
        } else {
            m_propsconfig.setProperty(sKey, sValue);
        }
    }
    
   /**
     * Local machine identity
     * @return Machine name from OS
     */
    private String getLocalHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException eUH) {
            return "localhost";
        }
    }
   
    public static AppConfig getInstance() {

        if (m_instance == null) {
            m_instance = new AppConfig(new File(System.getProperty("user.home")
                , AppLocal.APP_ID + ".properties"));
        }
        return m_instance;
    }
    
    public Boolean getBoolean(String sKey) {
        return Boolean.valueOf(m_propsconfig.getProperty(sKey));
    }
    
    public void setBoolean(String sKey, Boolean sValue) {
        if (sValue == null) {
            m_propsconfig.remove(sKey);
        } else if (sValue) {
            m_propsconfig.setProperty(sKey, "true");
        } else {
            m_propsconfig.setProperty(sKey, "false");
        }
    }
    
    /**
     *
     * @return Delete .properties filename
     */
    public boolean delete() {
        loadDefault();
        return configfile.delete();
    }

    /**
     * Get instance settings
     */
    public void load() {

        loadDefault();

        try {
            InputStream in = new FileInputStream(configfile);
            if (in != null) {
                m_propsconfig.load(in);
                in.close();
            }
        } catch (IOException e){
            loadDefault();
        }
   
    }

    /**
     *
     * @return 0 or 00 number keypad boolean true/false
     */
    public Boolean isPriceWith00() {
        String prop = getProperty("pricewith00");
        if (prop == null) {
            return false;
        } else {
            return prop.equals("true");
        }
    }

    /**
     * Save values to properties file
     * @throws java.io.IOException explicit on OS
     */
    public void save() throws IOException {
        
        OutputStream out = new FileOutputStream(configfile);
        if (out != null) {
            m_propsconfig.store(out, AppLocal.APP_NAME + ". Configuration file.");
            out.close();
        }
    }
    
    
    private void loadDefault() {
        
        m_propsconfig = new Properties();
        
        String dirname = System.getProperty("dirname.path");
        dirname = dirname == null ? "./" : dirname;
             
        m_propsconfig.setProperty("db.multi", "false");
        m_propsconfig.setProperty("override.check", "false");
        m_propsconfig.setProperty("override.pin", "");        
        
        m_propsconfig.setProperty("db.driverlib", new File(new File(dirname)
            , "mysql-connector-java-5.1.39.jar").getAbsolutePath());
        m_propsconfig.setProperty("db.engine", "MySQL");
        m_propsconfig.setProperty("db.driver", "com.mysql.jdbc.Driver");

// primary DB
        m_propsconfig.setProperty("db.name", "Main DB");
        m_propsconfig.setProperty("db.URL", "jdbc:mysql://localhost:3306/"); 
        m_propsconfig.setProperty("db.schema", "kylgispos");
        m_propsconfig.setProperty("db.options", "?zeroDateTimeBehavior=convertToNull");        
        m_propsconfig.setProperty("db.user", "username");
        m_propsconfig.setProperty("db.password", "password");     

// secondary DB        
        m_propsconfig.setProperty("db1.name", "");        
        m_propsconfig.setProperty("db1.URL", "jdbc:mysql://localhost:3306/"); 
        m_propsconfig.setProperty("db1.schema", "kylgispos");
        m_propsconfig.setProperty("db1.options", "?zeroDateTimeBehavior=convertToNull");        
        m_propsconfig.setProperty("db1.user", "");
        m_propsconfig.setProperty("db1.password", ""); 

        m_propsconfig.setProperty("machine.hostname", getLocalHostName());
        
        Locale l = Locale.getDefault();
        m_propsconfig.setProperty("user.language", l.getLanguage());
        m_propsconfig.setProperty("user.country", l.getCountry());
        m_propsconfig.setProperty("user.variant", l.getVariant());
        
        m_propsconfig.setProperty("swing.defaultlaf", 
                System.getProperty("swing.defaultlaf", 
                        "javax.swing.plaf.metal.MetalLookAndFeel"));                
        
        m_propsconfig.setProperty("machine.printer", "Not defined");
        m_propsconfig.setProperty("machine.printer.2", "Not defined");
        m_propsconfig.setProperty("machine.printer.3", "Not defined");
        m_propsconfig.setProperty("machine.printer.4", "Not defined");
        m_propsconfig.setProperty("machine.printer.5", "Not defined");
        m_propsconfig.setProperty("machine.printer.6", "Not defined");
                
        m_propsconfig.setProperty("machine.display", "screen");
        m_propsconfig.setProperty("machine.scale", "Not defined");
        m_propsconfig.setProperty("machine.screenmode", "fullscreen");
        m_propsconfig.setProperty("machine.ticketsbag", "standard");
        m_propsconfig.setProperty("machine.scanner", "Not defined");
        m_propsconfig.setProperty("machine.iButton", "false");  
        m_propsconfig.setProperty("machine.iButtonResponse","5");
        m_propsconfig.setProperty("machine.uniqueinstance", "true");
        m_propsconfig.setProperty("machine.maxinstances", "2");
        
        m_propsconfig.setProperty("payment.gateway", "external");
        m_propsconfig.setProperty("payment.magcardreader", "Not defined");
        m_propsconfig.setProperty("payment.testmode", "true");
        m_propsconfig.setProperty("payment.commerceid", "");
        m_propsconfig.setProperty("payment.commercepassword", "password");
        
        m_propsconfig.setProperty("machine.printername", "(Default)");
        m_propsconfig.setProperty("screen.receipt.columns", "31");        
        m_propsconfig.setProperty("ticket.paper.size", "58");
        m_propsconfig.setProperty("ticket.width", "32");
        m_propsconfig.setProperty("screen.ticket.enabled", "true");
        m_propsconfig.setProperty("digital.ticket.enabled", "false");
        m_propsconfig.setProperty("digital.ticket.path", new File(System.getProperty("user.home"), "KylGisPOS").getPath());
        m_propsconfig.setProperty("digital.ticket.format", "jpg");

        // Receipt printer paper set to 72mmx200mm
        m_propsconfig.setProperty("paper.receipt.x", "2");
        m_propsconfig.setProperty("paper.receipt.y", "2");
        m_propsconfig.setProperty("paper.receipt.width", "142");
        m_propsconfig.setProperty("paper.receipt.height", "546");
        m_propsconfig.setProperty("paper.receipt.mediasizename", "A4");

        // Normal printer paper for A4
        m_propsconfig.setProperty("paper.standard.x", "72");
        m_propsconfig.setProperty("paper.standard.y", "72");
        m_propsconfig.setProperty("paper.standard.width", "451");
        m_propsconfig.setProperty("paper.standard.height", "698");
        m_propsconfig.setProperty("paper.standard.mediasizename", "A4");

        m_propsconfig.setProperty("tkt.header1", "Nombre o Razón Social");
        m_propsconfig.setProperty("tkt.header2", "RFC");
        m_propsconfig.setProperty("tkt.header3", "Régimen Fiscal");
        m_propsconfig.setProperty("tkt.header4", "Dirección 1");
        m_propsconfig.setProperty("tkt.header5", "Dirección 2");
        m_propsconfig.setProperty("tkt.header6", "Dirección 3");

        m_propsconfig.setProperty("tkt.footer1", "Nombre del Negocio");
        m_propsconfig.setProperty("tkt.footer2", "Gracias por su preferencia");
        m_propsconfig.setProperty("tkt.footer3", "Quejas o sugerencias:");
        m_propsconfig.setProperty("tkt.footer4", "Teléfono");
        m_propsconfig.setProperty("tkt.footer5", "Correo Electrónico");
        m_propsconfig.setProperty("tkt.footer6", "Página Web");

        m_propsconfig.setProperty("table.showcustomerdetails", "true");
        m_propsconfig.setProperty("table.customercolour", "#58B000");        
        m_propsconfig.setProperty("table.showwaiterdetails", "true");
        m_propsconfig.setProperty("table.waitercolour", "#258FB0");                
        m_propsconfig.setProperty("table.tablecolour", "#D62E52"); 
        m_propsconfig.setProperty("till.amountattop", "true");        
        m_propsconfig.setProperty("till.hideinfo", "true");                
        // Sales toolbar visibility. Essential navigation buttons are not configurable here.
        m_propsconfig.setProperty("sales.button.platformorder", "false");
        m_propsconfig.setProperty("sales.button.scale", "true");
        m_propsconfig.setProperty("sales.button.remoteprint", "true");
        m_propsconfig.setProperty("sales.button.split", "true");
        m_propsconfig.setProperty("sales.button.reprint", "true");
        m_propsconfig.setProperty("sales.button.ticketpreview", "true");
        m_propsconfig.setProperty("sales.platforms.custom", "");

    }
}