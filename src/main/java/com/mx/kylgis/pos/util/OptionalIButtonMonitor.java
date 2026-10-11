//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS and is distributed under the GPL.
package com.mx.kylgis.pos.util;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Optional bridge for the legacy Dallas/Maxim 1-Wire Java API.
 *
 * <p>The 1-Wire SDK is no longer a mandatory build dependency. Installations
 * that still use iButton can provide the SDK JAR either on the application
 * class path or through {@code machine.iButton.library}. The core POS remains
 * buildable and runnable when the legacy SDK is absent.</p>
 */
public final class OptionalIButtonMonitor implements Closeable {

    public interface Listener {
        void deviceArrived(String address);
        void deviceDeparted(String address);
        void monitorError(Throwable error);
    }

    private final Object adapter;
    private final Object monitor;
    private final Thread monitorThread;
    private final Object listenerProxy;
    private final URLClassLoader externalLoader;

    private OptionalIButtonMonitor(Object adapter, Object monitor,
            Thread monitorThread, Object listenerProxy,
            URLClassLoader externalLoader) {
        this.adapter = adapter;
        this.monitor = monitor;
        this.monitorThread = monitorThread;
        this.listenerProxy = listenerProxy;
        this.externalLoader = externalLoader;
    }

    public static OptionalIButtonMonitor start(String libraryPath,
            final Listener listener) throws Exception {
        if (listener == null) {
            throw new IllegalArgumentException("listener is required");
        }

        URLClassLoader ownedLoader = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = OptionalIButtonMonitor.class.getClassLoader();
        }

        if (libraryPath != null && !libraryPath.trim().isEmpty()) {
            File library = new File(libraryPath.trim());
            if (!library.isFile()) {
                throw new IllegalArgumentException("1-Wire library not found: "
                        + library.getAbsolutePath());
            }
            ownedLoader = new URLClassLoader(
                    new URL[]{library.toURI().toURL()}, loader);
            loader = ownedLoader;
        }

        try {
            Class<?> accessProvider = Class.forName(
                    "com.dalsemi.onewire.OneWireAccessProvider", true, loader);
            Class<?> adapterClass = Class.forName(
                    "com.dalsemi.onewire.adapter.DSPortAdapter", true, loader);
            Class<?> monitorClass = Class.forName(
                    "com.dalsemi.onewire.application.monitor.DeviceMonitor", true, loader);
            final Class<?> listenerClass = Class.forName(
                    "com.dalsemi.onewire.application.monitor.DeviceMonitorEventListener",
                    true, loader);

            Object adapter = accessProvider.getMethod("getDefaultAdapter").invoke(null);
            adapterClass.getMethod("setSearchAllDevices").invoke(adapter);
            adapterClass.getMethod("targetFamily", int.class).invoke(adapter, 0x01);
            int regularSpeed = adapterClass.getField("SPEED_REGULAR").getInt(null);
            adapterClass.getMethod("setSpeed", int.class).invoke(adapter, regularSpeed);

            Constructor<?> monitorConstructor = monitorClass.getConstructor(adapterClass);
            Object monitor = monitorConstructor.newInstance(adapter);
            invokeIfPresent(monitorClass, monitor, "setMaxStateCount",
                    new Class<?>[]{int.class}, new Object[]{5});

            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if ("deviceArrival".equals(name) && args != null && args.length > 0) {
                    dispatchAddresses(args[0], true, listener);
                    return null;
                }
                if ("deviceDeparture".equals(name) && args != null && args.length > 0) {
                    dispatchAddresses(args[0], false, listener);
                    return null;
                }
                if ("networkException".equals(name) && args != null && args.length > 0) {
                    Object error = args[0];
                    listener.monitorError(error instanceof Throwable
                            ? (Throwable) error
                            : new IllegalStateException(String.valueOf(error)));
                    return null;
                }
                if ("toString".equals(name)) {
                    return "KylGisOptionalIButtonListener";
                }
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return args != null && args.length == 1 && proxy == args[0];
                }
                return null;
            };

            Object proxy = Proxy.newProxyInstance(loader,
                    new Class<?>[]{listenerClass}, handler);
            monitorClass.getMethod("addDeviceMonitorEventListener", listenerClass)
                    .invoke(monitor, proxy);

            if (!(monitor instanceof Runnable)) {
                throw new IllegalStateException("1-Wire DeviceMonitor is not Runnable");
            }
            Thread thread = new Thread((Runnable) monitor, "kylgis-ibutton-monitor");
            thread.setDaemon(true);
            thread.start();

            return new OptionalIButtonMonitor(adapter, monitor, thread, proxy, ownedLoader);
        } catch (Exception ex) {
            if (ownedLoader != null) {
                try {
                    ownedLoader.close();
                } catch (Exception ignored) {
                    // Preserve the initialization failure.
                }
            }
            throw ex;
        }
    }

    private static void dispatchAddresses(Object event, boolean arrival,
            Listener listener) throws Exception {
        Class<?> eventClass = event.getClass();
        Method countMethod = eventClass.getMethod("getDeviceCount");
        Method addressMethod = eventClass.getMethod("getAddressAsStringAt", int.class);
        int count = ((Number) countMethod.invoke(event)).intValue();
        for (int i = 0; i < count; i++) {
            String address = String.valueOf(addressMethod.invoke(event, i));
            if (arrival) {
                listener.deviceArrived(address);
            } else {
                listener.deviceDeparted(address);
            }
        }
    }

    private static void invokeIfPresent(Class<?> type, Object target,
            String method, Class<?>[] parameterTypes, Object[] args) throws Exception {
        try {
            type.getMethod(method, parameterTypes).invoke(target, args);
        } catch (NoSuchMethodException ignored) {
            // Older SDKs do not necessarily expose every tuning method.
        }
    }

    @Override
    public void close() {
        try {
            monitor.getClass().getMethod("killMonitor").invoke(monitor);
        } catch (Exception ignored) {
            // Best-effort shutdown of an optional legacy peripheral.
        }
        try {
            adapter.getClass().getMethod("freePort").invoke(adapter);
        } catch (Exception ignored) {
            // Best-effort shutdown of an optional legacy peripheral.
        }
        try {
            monitorThread.join(1000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        if (externalLoader != null) {
            try {
                externalLoader.close();
            } catch (Exception ignored) {
                // Nothing else to release.
            }
        }
    }
}
