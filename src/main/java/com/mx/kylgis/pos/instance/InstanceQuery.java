package com.mx.kylgis.pos.instance;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

/** Discovers independent RMI instance slots belonging to one APP_ID. */
public class InstanceQuery {

    private static final int BASE_PORT = 20000;
    private static final int APP_BUCKETS = 1000;
    private static final int PORTS_PER_APP = 20;

    private InstanceQuery() {
    }

    public static int getPort(String appId, int slot) {
        int bucket = (appId.hashCode() & 0x7fffffff) % APP_BUCKETS;
        return BASE_PORT + (bucket * PORTS_PER_APP) + (slot - 1);
    }

    public static List<AppMessage> getLiveInstances(String appId, int maxInstances) {
        List<AppMessage> instances = new ArrayList<>();
        for (int slot = 1; slot <= maxInstances; slot++) {
            try {
                Registry registry = LocateRegistry.getRegistry("127.0.0.1", getPort(appId, slot));
                AppMessage message = (AppMessage) registry.lookup(InstanceManager.BINDING_NAME);
                if (message.isAlive()) {
                    instances.add(message);
                }
            } catch (NotBoundException | RemoteException ignored) {
            }
        }
        return instances;
    }

    public static boolean restoreFirst(String appId, int maxInstances) {
        for (AppMessage message : getLiveInstances(appId, maxInstances)) {
            try {
                message.restoreWindow();
                return true;
            } catch (RemoteException ignored) {
            }
        }
        return false;
    }
}
