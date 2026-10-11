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
package com.mx.kylgis.pos.printer.escpos;

import com.mx.kylgis.pos.hardware.serial.SerialPortConnection;
import com.mx.kylgis.pos.printer.TicketPrinterException;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Serial ESC/POS writer.
 *
 * The historical class name is preserved for configuration compatibility, but
 * the transport is now jSerialComm through SerialPortConnection instead of
 * architecture-specific RXTX binaries.
 */
public class PrinterWritterRXTX extends PrinterWritter {

    private final String m_sPortPrinter;
    private SerialPortConnection connection;
    private OutputStream m_out;

    /** Creates a new serial printer writer.
     * @param sPortPrinter serial endpoint (COMx, /dev/tty*, /dev/rfcomm*, ...)
     * @throws com.mx.kylgis.pos.printer.TicketPrinterException */
    public PrinterWritterRXTX(String sPortPrinter) throws TicketPrinterException {
        m_sPortPrinter = sPortPrinter;
        m_out = null;
    }

    @Override
    protected void internalWrite(byte[] data) {
        try {
            ensureOpen();
            m_out.write(data);
        } catch (IOException e) {
            System.err.println(e);
            closeQuietly();
        }
    }

    private void ensureOpen() throws IOException {
        if (connection != null && connection.isOpen() && m_out != null) {
            return;
        }
        connection = SerialPortConnection.open(
                m_sPortPrinter,
                9600,
                8,
                1,
                SerialPortConnection.Parity.NONE,
                SerialPortConnection.FlowControl.RTS_CTS,
                0);
        m_out = connection.getOutputStream();
    }

    @Override
    protected void internalFlush() {
        try {
            if (m_out != null) {
                m_out.flush();
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    @Override
    protected void internalClose() {
        closeQuietly();
    }

    private void closeQuietly() {
        SerialPortConnection current = connection;
        connection = null;
        m_out = null;
        if (current != null) {
            try {
                current.close();
            } catch (IOException e) {
                System.err.println(e);
            }
        }
    }
}
