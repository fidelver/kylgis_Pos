package com.mx.kylgis.pos.instance;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Owns one independent RMI registry/slot for a running POS instance.
 * Each APP_ID receives its own deterministic range of local ports.
 */
public class InstanceManager {

    public static final String BINDING_NAME = "AppMessage";
    private final AppMessage message;
    private Registry registry;
    private int slot;
    private int port;

    public InstanceManager(AppMessage message, String appId, int maxInstances)
            throws RemoteException, AlreadyBoundException {
        this.message = message;
        AppMessage stub = (AppMessage) UnicastRemoteObject.exportObject(this.message, 0);

        for (int candidateSlot = 1; candidateSlot <= maxInstances; candidateSlot++) {
            int candidatePort = InstanceQuery.getPort(appId, candidateSlot);
            try {
                Registry candidateRegistry = LocateRegistry.createRegistry(candidatePort);
                candidateRegistry.bind(BINDING_NAME, stub);
                registry = candidateRegistry;
                slot = candidateSlot;
                port = candidatePort;
                return;
            } catch (RemoteException | AlreadyBoundException occupied) {
                // Another live process owns this slot/port. Try the next one.
            }
        }

        // No free slot: restore the first live instance and reject this process.
        InstanceQuery.restoreFirst(appId, maxInstances);
        try {
            UnicastRemoteObject.unexportObject(this.message, true);
        } catch (Exception ignored) {
        }
        throw new AlreadyBoundException("Maximum number of instances reached for " + appId);
    }

    public int getSlot() {
        return slot;
    }

    public int getPort() {
        return port;
    }
}
