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
package com.mx.kylgis.pos.editor;

import com.mx.kylgis.pos.basic.BasicException;

/**
 *
 * @author JG uniCenta
 */
public class JEditorPassword extends JEditorText {
    
    private static final char ECHO_CHAR = '*';
    
    /** Creates a new instance of JEditorPassword */
    public JEditorPassword() {
        super();
    }
    
    /**
     *
     * @return
     */
    protected final int getMode() {
        return EditorKeys.MODE_STRING;
    }
        
    /**
     *
     * @return
     */
    protected int getStartMode() {
        return MODE_Abc1;
    }
    
    /**
     *
     * @return
     */
    protected String getTextEdit() {
        
        StringBuilder s = new StringBuilder();
        s.append("<html>");
        s.append(getEcho(m_svalue));
        if (m_cLastChar != '\u0000') {
            s.append("<font color=\"#a0a0a0\">");
            s.append(ECHO_CHAR);
            s.append("</font>");
        }
        s.append("<font color=\"#a0a0a0\">_</font>");

        return s.toString(); 
    }
    
    /**
     *
     * @return
     */
    public final String getPassword() {
        
        // como clave nunca devolvemos null
        String sPassword = getText();
        return sPassword == null ? "" : sPassword;     
    }

    /**
     *
     * @return
     * @throws BasicException
     */
    protected String getTextFormat() throws BasicException {
        return "<html>" + getEcho(m_svalue);
    }    
    
    private String getEcho(String sValue) {
        
        if (sValue == null) {
            return "";
        } else {
            char[] c = new char[sValue.length()];
            for(int i = 0; i < sValue.length(); i++) {
                c[i] = ECHO_CHAR;
            }
            return new String(c);
        }
    }
}
