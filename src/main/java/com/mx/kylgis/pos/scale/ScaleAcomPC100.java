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
package com.mx.kylgis.pos.scale;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.*;

/**
*
* @author uniCenta + H Singh
*/
public class ScaleAcomPC100 implements Scale, SerialPortDataListener {

private SerialPort m_CommPortPrinter;

private static final byte[] DEFAULT_WEIGHT_REQUEST = new byte[]{0x57, 0x0D};

private final String m_sPortScale;
private final byte[] m_weightRequest;
private OutputStream m_out;
private InputStream m_in;

private static final int SCALE_READY = 0;
private static final int SCALE_READING = 1;
private static final int SCALE_READINGDECIMALS = 2;

private double m_dWeightBuffer;
private double m_dWeightDecimals;
private int m_iStatusScale;
private String m_sScaleReading;

/** Creates a new instance of ScaleComm
* @param sPortPrinter */
    public ScaleAcomPC100(String sPortPrinter) {
        this(sPortPrinter, DEFAULT_WEIGHT_REQUEST);
    }

    public ScaleAcomPC100(String sPortPrinter, byte[] weightRequest) {
        m_sPortScale = sPortPrinter;
        m_weightRequest = weightRequest.clone();
        m_out = null;
        m_in = null;
        m_iStatusScale = SCALE_READY;
        m_dWeightBuffer = 0.0;
        m_dWeightDecimals = 1.0;
        m_sScaleReading = "";
    }

/**
*
* @return
*/
@Override
public Double readWeight() {

    synchronized(this) {

        if (m_iStatusScale != SCALE_READY) {
        try {
            wait(200);
        } catch (InterruptedException e) {
        }
        if (m_iStatusScale != SCALE_READY) {
            m_iStatusScale = SCALE_READY;
        }
        }

        m_dWeightBuffer = 0.0;
        m_dWeightDecimals = 1.0;
        write(m_weightRequest);
        flush();

        try {
            wait(200);
        } catch (InterruptedException e) {
        }

        if (m_iStatusScale == SCALE_READY) {
            double dWeight = m_dWeightBuffer / m_dWeightDecimals;
            m_dWeightBuffer = 0.0;
            m_dWeightDecimals = 1.0;
            return dWeight;
        } else {
            m_iStatusScale = SCALE_READY;
            m_dWeightBuffer = 0.0;
            m_dWeightDecimals = 1.0;
            return 0.0;
        }
    }
}

private void flush() {
    try {
        m_out.flush();
    } catch (IOException e) {
    }
}

private void write(byte[] data) {
    try {
        if (m_out == null) {
            m_CommPortPrinter = SerialPort.getCommPort(m_sPortScale);

            m_CommPortPrinter.setComPortParameters(9600, 7,

                    SerialPort.ONE_STOP_BIT, SerialPort.EVEN_PARITY);

            if (!m_CommPortPrinter.openPort(2000)) {

                return;

            }

            m_out = m_CommPortPrinter.getOutputStream();

            m_in = m_CommPortPrinter.getInputStream();

            m_CommPortPrinter.addDataListener(this);
        }
        m_out.write(data);
    } catch (IOException e) {
    }
}

/**
*
* @param e
*/
@Override
public int getListeningEvents() {
    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
}

@Override
public void serialEvent(SerialPortEvent e) {
    if (e.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
        return;
    }
    try {
        int i = m_in.available();
        byte[] readBuffer = new byte[i];
        if (i > 0) {
            m_in.read(readBuffer);
        }

        m_sScaleReading = m_sScaleReading + new String(readBuffer);

        try {
            int start = m_sScaleReading.indexOf((char) 10);
            int end = m_sScaleReading.indexOf((char) 3);

            if (start >= 0 && end >= 0) {
                start = m_sScaleReading.indexOf((char) 10);
                end = m_sScaleReading.indexOf((char) 75);
                m_dWeightBuffer = Double.parseDouble(m_sScaleReading.substring(start + 1, end));
                m_sScaleReading = "";
            }
        } catch (IndexOutOfBoundsException ex) {
            System.out.println("IndexOutOfBoundsException, message not complete yet. Waiting for more data.");
        }
    } catch (IOException eIO) {
    }
}
}