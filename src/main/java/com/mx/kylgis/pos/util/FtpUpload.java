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
 
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FtpUpload extends Thread {

    private static final int BUFFER_SIZE = 1024;
    private static String sMachine;

    @Override
    public void run() {
        String ftpUrl = "";
        String host = "";
        String user = "";
        String pass = "";

        try {
            sMachine = InetAddress.getLocalHost().getHostName();

            String filePath = new File(System.getProperty("user.home"), sMachine + ".lau").getPath();
            String uploadPath = sMachine + ".lau";

            ftpUrl = String.format(ftpUrl, user, pass, host, uploadPath);

            URL url = new URL(ftpUrl);
            URLConnection conn = url.openConnection();
            try (OutputStream outputStream = conn.getOutputStream(); 
                    FileInputStream inputStream = new FileInputStream(filePath)) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
            }

        }
        catch (IOException ex) {
            Logger.getLogger(this.getName()).log(Level.SEVERE, null, ex);
        }
    }
}