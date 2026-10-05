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
package com.mx.kylgis.pos.data.loader;

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.forms.AppViewConnection;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.AppLocal;
import java.io.*;

public class TicketHeader {
    private String header_line1;
    private String header_line2;       
    private String header_line3;
    private String header_line4;
    private String header_line5;
    private String header_line6;
    private File m_config;
    private Session session;

    public TicketHeader() {          
        AppConfig config = new AppConfig(m_config);
        // AppViewConnection Session = new AppViewConnection();

    }

    public void loadProperties(AppConfig config) {
        header_line1=(config.getProperty("till.header1"));
        header_line2=(config.getProperty("till.header2"));
        header_line3=(config.getProperty("till.header3"));         
        header_line4=(config.getProperty("till.header4"));         
        header_line5=(config.getProperty("till.header5"));         
        header_line6=(config.getProperty("till.header6"));         
    }
    
    public String getTicketHeaderLine1() {
        return header_line1;
    }
    
    public String getTicketHeaderLine2() {
        return header_line2;
    }
    
    public String getTicketHeaderLine3() {
        return header_line3;
    }
    
    public String getTicketHeaderLine4() {
        return header_line4;
    }
    
    public String getTicketHeaderLine5() {
        return header_line5;
    }
    
    public String getTicketHeaderLine6() {
        return header_line6;
    }
}





