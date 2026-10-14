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
 * @author JG uniCenta
 */
public class ScaleComm implements Scale, SerialPortDataListener {
    
    private static final byte[] DEFAULT_WEIGHT_REQUEST = new byte[]{0x05};

    private String m_sPortScale;
    private final byte[] m_weightRequest;
    private SerialPort m_CommPortPrinter;      
    private OutputStream m_out;
    private InputStream m_in;

    private static final int SCALE_READY = 0;
    private static final int SCALE_READING = 1;
    
    private double m_dWeightBuffer;
    private int m_iStatusScale;
        
    /** Creates a new instance of ScaleComm
     * @param sPortPrinter */
    public ScaleComm(String sPortPrinter) {
        this(sPortPrinter, DEFAULT_WEIGHT_REQUEST);
    }

    public ScaleComm(String sPortPrinter, byte[] weightRequest) {
        m_sPortScale = sPortPrinter;
        m_weightRequest = weightRequest.clone();
        m_out = null;
        m_in = null;
        
        m_iStatusScale = SCALE_READY; 
        m_dWeightBuffer = 0.0;
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
                    wait(1000);
                } catch (InterruptedException e) {
                }
                if (m_iStatusScale != SCALE_READY) {
                    // bascula tonta.
                    m_iStatusScale = SCALE_READY;
                }
            }
            
            // Ya estamos en SCALE_READY
            m_dWeightBuffer = 0.0;
            write(m_weightRequest);
            flush();             
            
            // Esperamos un ratito
            try {
                wait(1000);
            } catch (InterruptedException e) {
            }
            
            if (m_iStatusScale == SCALE_READY) {
                // a value as been readed.
                double dWeight = m_dWeightBuffer / 1000.0;
                m_dWeightBuffer = 0.0;
                return dWeight;
            } else {
                m_iStatusScale = SCALE_READY;
                m_dWeightBuffer = 0.0;
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

                m_CommPortPrinter.setComPortParameters(4800, 8,

                        SerialPort.ONE_STOP_BIT, SerialPort.ODD_PARITY);

                if (!m_CommPortPrinter.openPort(2000)) {

                    return;

                }

                m_out = m_CommPortPrinter.getOutputStream();

                m_in = m_CommPortPrinter.getInputStream();

                m_CommPortPrinter.addDataListener(this); // Configuramos el puerto
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
                    while (m_in.available() > 0) {
                        int b = m_in.read();

                        if (b == 0x001E) { // RS ASCII
                            // Fin de lectura
                            synchronized (this) {
                                m_iStatusScale = SCALE_READY;
                                notifyAll();
                            }
                        } else if (b > 0x002F && b < 0x003A){
                            synchronized(this) {
                                if (m_iStatusScale == SCALE_READY) {
                                    m_dWeightBuffer = 0.0; // se supone que esto debe estar ya garantizado
                                    m_iStatusScale = SCALE_READING;
                                }
                                m_dWeightBuffer = m_dWeightBuffer * 10.0 + b - 0x0030;
                            }
                        } else {
                            // caracteres invalidos, reseteamos.
                            m_dWeightBuffer = 0.0; // se supone que esto debe estar ya garantizado
                            m_iStatusScale = SCALE_READY;
                        }
                    }

        } catch (IOException eIO) {
        }
    } 
}
