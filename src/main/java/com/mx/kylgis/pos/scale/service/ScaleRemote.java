//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.scale.service;

import com.mx.kylgis.pos.scale.Scale;
import com.mx.kylgis.pos.scale.ScaleException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Scale client that delegates one weight reading to the hardware-owning node. */
public final class ScaleRemote implements Scale {
    private final ScaleServiceConfig.Endpoint endpoint;

    public ScaleRemote(ScaleServiceConfig.Endpoint endpoint) {
        if (endpoint == null) throw new IllegalArgumentException("endpoint is required");
        this.endpoint = endpoint;
    }

    @Override
    public Double readWeight() throws ScaleException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()),
                    endpoint.getConnectTimeoutMs());
            socket.setSoTimeout(endpoint.getReadTimeoutMs());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            ScaleProtocol.writeRequest(out, endpoint.getServiceId(), endpoint.getToken());
            ScaleProtocol.Response response = ScaleProtocol.readResponse(in);
            if (!response.ok) throw new IOException(response.message);
            return response.weight;
        } catch (IOException ex) {
            throw new ScaleException("KylGis scale service " + endpoint.getServiceId()
                    + " failed: " + ex.getMessage());
        }
    }
}
