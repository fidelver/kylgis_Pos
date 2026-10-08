//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.remote;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

/** Client for one encrypted effective node configuration. */
public final class RemoteConfigClient {
    private RemoteConfigClient() { }

    public static Properties fetch(ConfigServiceConfig.RemoteEndpoint endpoint,
            String nodeId) throws IOException, RemoteConfigException {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint is required");
        }
        RemoteConfigProtocol.Request request =
                RemoteConfigProtocol.createRequest(endpoint.getServiceId(),
                        nodeId, endpoint.getToken());
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(endpoint.getHost(),
                    endpoint.getPort()), 5000);
            socket.setSoTimeout(10000);
            try (DataOutputStream out = new DataOutputStream(
                        socket.getOutputStream());
                    DataInputStream in = new DataInputStream(
                        socket.getInputStream())) {
                RemoteConfigProtocol.writeRequest(out, request);
                return RemoteConfigProtocol.readResponse(in, request,
                        endpoint.getToken());
            }
        }
    }
}
