//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.ui;

import com.alee.extended.time.ClockType;
import com.alee.extended.time.WebClock;
import com.alee.managers.notification.NotificationIcon;
import com.alee.managers.notification.NotificationManager;
import com.alee.managers.notification.WebNotification;
import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single boundary for transient, non-critical desktop notifications.
 *
 * Business flows must not depend directly on WebLaF notification classes. This
 * adapter intentionally keeps the current WebLaF visual behavior while making
 * replacement by another Swing implementation an isolated future change.
 */
public final class KylGisNotifications {

    private static final Logger LOGGER = Logger.getLogger(KylGisNotifications.class.getName());
    private static final int DISPLAY_MILLIS = 4000;
    private static final int TIMER_MILLIS = 5000;

    private KylGisNotifications() {
    }

    public static void showInformation(Component owner, String message) {
        try {
            WebNotification notification = createInformationNotification();
            notification.setContent(message);
            if (owner == null) {
                NotificationManager.showNotification(notification);
            } else {
                NotificationManager.showNotification(owner, notification);
            }
        } catch (RuntimeException ex) {
            logFailure(ex);
        }
    }

    public static void showTimedInformation(String message) {
        try {
            WebNotification notification = createInformationNotification();
            WebClock clock = new WebClock();
            clock.setClockType(ClockType.timer);
            clock.setTimeLeft(TIMER_MILLIS);
            clock.setTimePattern(message);
            notification.setContent(clock);
            NotificationManager.showNotification(notification);
            clock.start();
        } catch (RuntimeException ex) {
            logFailure(ex);
        }
    }

    private static WebNotification createInformationNotification() {
        WebNotification notification = new WebNotification();
        notification.setIcon(NotificationIcon.information);
        notification.setDisplayTime(DISPLAY_MILLIS);
        return notification;
    }

    private static void logFailure(RuntimeException ex) {
        LOGGER.log(Level.WARNING,
                "Unable to show non-critical desktop notification", ex);
    }
}
