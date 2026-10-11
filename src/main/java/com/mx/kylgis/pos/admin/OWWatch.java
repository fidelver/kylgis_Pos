/*---------------------------------------------------------------------------
 * Copyright (c) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
 * Portions adapted for KylGis POS.
 *---------------------------------------------------------------------------
 */

import com.mx.kylgis.pos.util.OptionalIButtonMonitor;

/**
 * Minimal command-line diagnostic for the optional 1-Wire/iButton integration.
 *
 * <p>Usage: {@code OWWatch [milliseconds] [OneWireApi.jar]}. The legacy SDK is
 * loaded only at runtime, so it is no longer a mandatory dependency of the
 * core KylGis POS build.</p>
 */
public final class OWWatch {

    private OWWatch() {
    }

    public static void main(String[] args) {
        long delay = 20000L;
        if (args.length >= 1) {
            delay = Long.decode(args[0]);
        }
        String library = args.length >= 2 ? args[1] : null;

        try (OptionalIButtonMonitor monitor = OptionalIButtonMonitor.start(
                library, new OptionalIButtonMonitor.Listener() {
            @Override
            public void deviceArrived(String address) {
                System.out.println("ADD: " + address);
            }

            @Override
            public void deviceDeparted(String address) {
                System.out.println("REMOVE: " + address);
            }

            @Override
            public void monitorError(Throwable error) {
                System.err.println("ERROR: " + error);
            }
        })) {
            System.out.println("Monitor run for: " + delay + "ms");
            Thread.sleep(delay);
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
