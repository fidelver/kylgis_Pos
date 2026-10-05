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
package com.mx.kylgis.pos.util;

import java.awt.Component;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import com.mx.kylgis.pos.beans.JPasswordDialog;
import com.mx.kylgis.pos.forms.AppLocal;

/**
 *
 * @author JG uniCenta
 */
public class Hashcypher {
    
    
    /** Creates a new instance of Hashcypher */
    public Hashcypher() {
    }

    /**
     *
     * @param sPassword
     * @param sHashPassword
     * @return
     */
    public static boolean authenticate(String sPassword, String sHashPassword) {
        if (sHashPassword == null || sHashPassword.equals("") || sHashPassword.startsWith("empty:")) {
            return sPassword == null || sPassword.equals("");
        } else if (sHashPassword.startsWith("sha1:")) {
            return sHashPassword.equals(hashString(sPassword));
        } else if (sHashPassword.startsWith("plain:")) {
            return sHashPassword.equals("plain:" + sPassword);
        } else {
            return sHashPassword.equals(sPassword);
        } 
    }
    
    /**
     *
     * @param sPassword
     * @return
     */
    public static String hashString(String sPassword) {
        
        if (sPassword == null || sPassword.equals("")) {
            return "empty:";
        } else {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-1");
                md.update(sPassword.getBytes("UTF-8"));
                byte[] res = md.digest();
                return "sha1:" + StringUtils.byte2hex(res);
            } catch (NoSuchAlgorithmException e) {
                return "plain:" + sPassword;
            } catch (UnsupportedEncodingException e) {
                return "plain:" + sPassword;
            }
        }
    }
    
    /**
     *
     * @param parent
     * @return
     */
    public static String changePassword(Component parent) {
        // Show the changePassword dialogs but do not check the old password
        
        String sPassword = JPasswordDialog.showEditPassword(parent,                 
                AppLocal.getIntString("label.Password"), 
                AppLocal.getIntString("label.passwordnew"),
                new ImageIcon(Hashcypher.class.getResource("/com/mx/kylgis/pos/images/password.png")));
        if (sPassword != null) {
            String sPassword2 = JPasswordDialog.showEditPassword(parent,                 
                    AppLocal.getIntString("label.Password"), 
                    AppLocal.getIntString("label.passwordrepeat"),
                    new ImageIcon(Hashcypher.class.getResource("/com/mx/kylgis/pos/images/password.png")));
            if (sPassword2 != null) {
                if (sPassword.equals(sPassword2)) {
                    return  Hashcypher.hashString(sPassword);
                } else {
                    JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.changepassworddistinct"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
        }   
        
        return null;
    }

    /**
     *
     * @param parent
     * @param sOldPassword
     * @return
     */
    public static String changePassword(Component parent, String sOldPassword) {
        
        String sPassword = JPasswordDialog.showEditPassword(parent,                 
                AppLocal.getIntString("label.Password"), 
                AppLocal.getIntString("label.passwordold"),
                new ImageIcon(Hashcypher.class.getResource("/com/mx/kylgis/pos/images/password.png")));
        if (sPassword != null) {
            if (Hashcypher.authenticate(sPassword, sOldPassword)) {
                return changePassword(parent);               
            } else {
                JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.BadPassword"), AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
           }
        }
        return null;
    }
}
