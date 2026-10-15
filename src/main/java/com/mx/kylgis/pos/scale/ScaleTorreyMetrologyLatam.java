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
package com.mx.kylgis.pos.scale;

import com.fazecast.jSerialComm.SerialPort;
import com.mx.kylgis.pos.forms.AppLocal;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Torrey / Metrology LATAM request-response scale.
 *
 * Uses jSerialComm so the driver remains functional on the Java 21 runtime.
 * Protocol validated physically on a BPP-style device: 9600 8N1, request
 * {@code P} (0x50), response such as {@code +  1.000 kg\r}.
 *
 * @author JG uniCenta
 */
public class ScaleTorreyMetrologyLatam implements Scale {

    private static final int BAUD_RATE = 9600;
    private static final int READ_TIMEOUT_MS = 1200;
    private static final int MAX_FRAME = 40;
    private static final Pattern WEIGHT_FRAME = Pattern.compile("([+-])\\s*(\\d{1,2}\\.\\d{3})\\s+kg");
    private static final byte[] DEFAULT_WEIGHT_REQUEST = new byte[]{0x50};

    private final String portName;
    private final byte[] weightRequest;
    private SerialPort serialPort;
    private InputStream input;
    private OutputStream output;

    /**
     * @param portName serial device, for example /dev/ttyUSB0
     */
    public ScaleTorreyMetrologyLatam(String portName) {
        this(portName, DEFAULT_WEIGHT_REQUEST);
    }

    public ScaleTorreyMetrologyLatam(String portName, byte[] weightRequest) {
        this.portName = portName;
        if (weightRequest == null || weightRequest.length == 0) {
            throw new IllegalArgumentException("weightRequest is required");
        }
        this.weightRequest = weightRequest.clone();
    }

    @Override
    public synchronized Double readWeight() throws ScaleException {
        ensureOpen();
        try {
            discardPendingInput();
            output.write(weightRequest);
            output.flush();
            return readResponse();
        } catch (IOException ex) {
            resetPort();
            throw new ScaleException(AppLocal.getIntString("scale.ioerror") + " "
                    + portName + ": " + ex.getMessage());
        }
    }

    private void ensureOpen() throws ScaleException {
        if (serialPort != null && serialPort.isOpen() && input != null && output != null) {
            return;
        }

        resetPort();
        SerialPort candidate = SerialPort.getCommPort(portName);
        candidate.setComPortParameters(
                BAUD_RATE,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY);
        candidate.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                READ_TIMEOUT_MS,
                READ_TIMEOUT_MS);

        if (!candidate.openPort(2000)) {
            throw new ScaleException(AppLocal.getIntString("scale.portnotaccessible") + " " + portName);
        }

        try {
            serialPort = candidate;
            input = serialPort.getInputStream();
            output = serialPort.getOutputStream();
        } catch (RuntimeException ex) {
            try {
                candidate.closePort();
            } catch (RuntimeException ignored) {
                // Best effort while reporting the original open failure.
            }
            resetPort();
            throw new ScaleException(AppLocal.getIntString("scale.portnotaccessible") + " " + portName);
        }
    }

    private void discardPendingInput() throws IOException {
        while (input.available() > 0) {
            input.read();
        }
    }

    private Double readResponse() throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        while (true) {
            int value = input.read();
            if (value < 0) {
                throw new IOException("Incomplete scale response");
            }
            if (frame.size() >= MAX_FRAME) {
                throw new IOException("Scale response too long");
            }
            frame.write(value);
            if (value == '\r') {
                return parseWeight(frame.toByteArray());
            }
        }
    }

    static Double parseWeight(byte[] raw) throws IOException {
        if (raw == null || raw.length < 2 || raw.length > MAX_FRAME
                || raw[raw.length - 1] != '\r') {
            throw new IOException("Incomplete scale response");
        }
        for (byte value : raw) {
            if (value != '\r' && (value < 32 || value > 126)) {
                throw new IOException("Invalid non-ASCII scale response");
            }
        }
        String text = new String(raw, 0, raw.length - 1, StandardCharsets.US_ASCII);
        Matcher match = WEIGHT_FRAME.matcher(text);
        if (!match.matches()) {
            throw new IOException("Unrecognized scale response format");
        }
        if ("-".equals(match.group(1))) {
            throw new IOException("Negative scale response");
        }
        try {
            return new BigDecimal(match.group(2)).doubleValue();
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid scale response", ex);
        }
    }

    @Override
    public synchronized void close() {
        resetPort();
    }

    private void resetPort() {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ignored) {
                // Best effort cleanup.
            }
        }
        if (output != null) {
            try {
                output.close();
            } catch (IOException ignored) {
                // Best effort cleanup.
            }
        }
        if (serialPort != null) {
            try {
                serialPort.closePort();
            } catch (RuntimeException ignored) {
                // Best effort cleanup.
            }
        }
        input = null;
        output = null;
        serialPort = null;
    }
}
