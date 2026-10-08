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
package com.mx.kylgis.pos.forms;

import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.instance.InstanceQuery;
import com.mx.kylgis.pos.node.NodeContext;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.io.IOException;
import javax.swing.SwingUtilities;

import org.pushingpixels.substance.api.SubstanceLookAndFeel;
import org.pushingpixels.substance.api.SubstanceSkin;


public class StartPOS {

    private static final Logger logger = Logger.getLogger("com.mx.kylgis.pos.forms.StartPOS");

    private StartPOS() {
    }

    public static int getMaxInstances(AppProperties config) {
        if ("true".equals(config.getProperty("machine.uniqueinstance"))) {
            return 1;
        }
        try {
            return Math.max(2, Integer.parseInt(config.getProperty("machine.maxinstances")));
        } catch (NumberFormatException | NullPointerException ex) {
            return 2;
        }
    }

    /**
     * Local identity used only for instance/RMI separation. By default it is
     * the application APP_ID, but test installations can use a distinct
     * machine.instanceid without changing the database application identity.
     */
    public static String getInstanceAppId(AppProperties config) {
        String instanceId = config.getProperty("machine.instanceid");
        if (instanceId != null && !instanceId.trim().isEmpty()) {
            return instanceId.trim();
        }
        String nodeId = config.getProperty("node.id");
        if (nodeId != null && !nodeId.trim().isEmpty()) {
            return ApplicationIdentity.getDatabaseId(config) + ":" + nodeId.trim();
        }
        return ApplicationIdentity.getDatabaseId(config);
    }

    private static boolean instanceLimitReached(AppProperties config) {
        int maxInstances = getMaxInstances(config);
        String instanceAppId = getInstanceAppId(config);
        if (InstanceQuery.getLiveInstances(instanceAppId, maxInstances).size() >= maxInstances) {
            InstanceQuery.restoreFirst(instanceAppId, maxInstances);
            return true;
        }
        return false;
    }

    public static void main (final String args[]) {

        SwingUtilities.invokeLater (() -> {
            AppConfig config = new AppConfig(args);
            config.load();
            AppConfig.setActiveInstance(config);

            NodeContext nodeContext = NodeContext.from(config);
            logger.log(Level.INFO, "KylGis node context: {0}", nodeContext);

            // Fast path: if all slots are already occupied, activate an existing
            // instance before initializing the database and the rest of the UI.
            if (instanceLimitReached(config)) {
                System.exit(1);
                return;
            }

            String slang = config.getProperty("user.language");
            String scountry = config.getProperty("user.country");
            String svariant = config.getProperty("user.variant");
            if (slang != null
                    && !slang.equals("")
                    && scountry != null
                    && svariant != null) {
                Locale.setDefault(new Locale(slang, scountry, svariant));
            }
            
            Formats.setIntegerPattern(config.getProperty("format.integer"));
            Formats.setDoublePattern(config.getProperty("format.double"));
            Formats.setCurrencyPattern(config.getProperty("format.currency"));
            Formats.setPercentPattern(config.getProperty("format.percent"));
            Formats.setDatePattern(config.getProperty("format.date"));
            Formats.setTimePattern(config.getProperty("format.time"));
            Formats.setDateTimePattern(config.getProperty("format.datetime"));
            
            // Set the look and feel.
            try {
                
                Object laf = Class.forName(config.getProperty("swing.defaultlaf")).newInstance();
                if (laf instanceof LookAndFeel){
                    UIManager.setLookAndFeel((LookAndFeel) laf);
                } else if (laf instanceof SubstanceSkin) {
                    SubstanceLookAndFeel.setSkin((SubstanceSkin) laf);
                }
// JG 6 May 2013 to multicatch
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                logger.log(Level.WARNING, "Cannot set Look and Feel", e);
            }
            
// JG July 2014 Hostname for Tickets
        String hostname = config.getProperty("machine.hostname");
        TicketInfo.setHostname(hostname);

        String screenmode = config.getProperty("machine.screenmode");

        if ("fullscreen".equals(screenmode)) {
            JRootKiosk rootkiosk = new JRootKiosk();
            try {
                rootkiosk.initFrame(config);
            } catch (IOException ex) {
                Logger.getLogger(StartPOS.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            JRootFrame rootframe = new JRootFrame();
            try {
                rootframe.initFrame(config);
            } catch (Exception ex) {
                Logger.getLogger(StartPOS.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        });    
    }    
}