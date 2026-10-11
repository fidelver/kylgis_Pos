//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.hardware.serial;

import com.fazecast.jSerialComm.SerialPort;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Small platform-neutral serial transport used by KylGis hardware adapters.
 *
 * jSerialComm selects the matching native implementation from its own JAR, so
 * callers do not need architecture-specific RXTX files in java.library.path.
 */
public final class SerialPortConnection implements Closeable {

    public enum Parity {
        NONE,
        ODD,
        EVEN,
        MARK,
        SPACE
    }

    public enum FlowControl {
        NONE,
        RTS_CTS
    }

    private final SerialPort port;
    private InputStream input;
    private OutputStream output;

    private SerialPortConnection(SerialPort port) {
        this.port = port;
    }

    public static SerialPortConnection open(String portName, int baudRate,
            int dataBits, int stopBits, Parity parity, FlowControl flowControl,
            int readTimeoutMillis) throws IOException {
        if (portName == null || portName.trim().isEmpty()) {
            throw new IOException("Serial port is required");
        }

        SerialPort port = SerialPort.getCommPort(portName.trim());
        port.setComPortParameters(baudRate, dataBits, mapStopBits(stopBits), mapParity(parity));
        port.setFlowControl(mapFlowControl(flowControl));
        if (readTimeoutMillis > 0) {
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    readTimeoutMillis, 0);
        } else {
            port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        }

        if (!port.openPort()) {
            throw new IOException("Unable to open serial port " + portName);
        }
        return new SerialPortConnection(port);
    }

    public InputStream getInputStream() {
        if (input == null) {
            input = port.getInputStreamWithSuppressedTimeoutExceptions();
        }
        return input;
    }

    public OutputStream getOutputStream() {
        if (output == null) {
            output = port.getOutputStream();
        }
        return output;
    }

    public boolean isOpen() {
        return port.isOpen();
    }

    public String getSystemPortName() {
        return port.getSystemPortName();
    }

    @Override
    public void close() throws IOException {
        IOException problem = null;
        if (input != null) {
            try {
                input.close();
            } catch (IOException ex) {
                problem = ex;
            } finally {
                input = null;
            }
        }
        if (output != null) {
            try {
                output.flush();
                output.close();
            } catch (IOException ex) {
                if (problem == null) {
                    problem = ex;
                }
            } finally {
                output = null;
            }
        }
        if (port.isOpen() && !port.closePort() && problem == null) {
            problem = new IOException("Unable to close serial port " + port.getSystemPortName());
        }
        if (problem != null) {
            throw problem;
        }
    }

    private static int mapStopBits(int stopBits) {
        switch (stopBits) {
            case 2:
                return SerialPort.TWO_STOP_BITS;
            default:
                return SerialPort.ONE_STOP_BIT;
        }
    }

    private static int mapParity(Parity parity) {
        if (parity == null) {
            return SerialPort.NO_PARITY;
        }
        switch (parity) {
            case ODD:
                return SerialPort.ODD_PARITY;
            case EVEN:
                return SerialPort.EVEN_PARITY;
            case MARK:
                return SerialPort.MARK_PARITY;
            case SPACE:
                return SerialPort.SPACE_PARITY;
            default:
                return SerialPort.NO_PARITY;
        }
    }

    private static int mapFlowControl(FlowControl flowControl) {
        if (FlowControl.RTS_CTS.equals(flowControl)) {
            return SerialPort.FLOW_CONTROL_RTS_ENABLED | SerialPort.FLOW_CONTROL_CTS_ENABLED;
        }
        return SerialPort.FLOW_CONTROL_DISABLED;
    }
}
