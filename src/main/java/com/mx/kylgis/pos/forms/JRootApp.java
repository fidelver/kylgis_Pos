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

import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.beans.JFlowPanel;
import com.mx.kylgis.pos.beans.JPasswordDialog;
import com.mx.kylgis.pos.data.gui.JMessageDialog;
import com.mx.kylgis.pos.data.gui.MessageInf;
import com.mx.kylgis.pos.data.loader.BatchSentence;
import com.mx.kylgis.pos.data.loader.BatchSentenceResource;
import com.mx.kylgis.pos.data.loader.Session;
import com.mx.kylgis.pos.format.Formats;
import com.mx.kylgis.pos.printer.DeviceTicket;
import com.mx.kylgis.pos.printer.TicketParser;
import com.mx.kylgis.pos.printer.TicketPrinterException;
import com.mx.kylgis.pos.scale.DeviceScale;
import com.mx.kylgis.pos.scanpal2.DeviceScanner;
import com.mx.kylgis.pos.scanpal2.DeviceScannerFactory;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.swing.*;     
import org.joda.time.DateTime;
import com.mx.kylgis.pos.util.OptionalIButtonMonitor;
import com.mx.kylgis.pos.util.uOWWatch;
import org.joda.time.Instant;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


/**
 *
 * @author adrianromero
 */
// public class JRootApp extends JPanel implements AppView {
public class JRootApp extends JPanel implements AppView {

    private AppProperties m_props;
    private Session session;     
    private DataLogicSystem m_dlSystem;
    
    private Properties m_propsdb = null;
    private String m_sActiveCashIndex;
    private int m_iActiveCashSequence;
    private Date m_dActiveCashDateStart;
    private Date m_dActiveCashDateEnd;
    
    private Double m_dActiveCashNotes;
    private Double m_dActiveCashCoins;
    private Double m_dActiveCashCards;
    
    private String m_sClosedCashIndex;
    private int m_iClosedCashSequence;
    private Date m_dClosedCashDateStart;
    private Date m_dClosedCashDateEnd;
    
//    private Double m_dClosedCashNotes;
//    private Double m_dClosedCashCoins;
//    private Double m_dClosedCashCards;
    
    private String m_sInventoryLocation;
    
    private StringBuilder inputtext;
   
    private DeviceScale m_Scale;
    private DeviceScanner m_Scanner;
    private DeviceTicket m_TP;   
    private TicketParser m_TTP;
    private javax.swing.Timer m_healthMonitorTimer;
    private volatile boolean m_healthProbeRunning;
    private final OperationalHealth m_health = new OperationalHealth();
    private final JLabel m_healthPeripherals = new JLabel();
    private final JLabel m_healthResources = new JLabel();
    
    private final Map<String, BeanFactory> m_aBeanFactories;
    
    private JPrincipalApp m_principalapp = null;
    
    private static HashMap<String, String> m_oldclasses; 

    private String m_clock;
    private String m_date; 
    private Connection con;  
    private ResultSet rs;
    private Statement stmt;
    private String SQL;
    private String sJLVersion;
    private DatabaseMetaData md;
    
    private final int m_rate=0;
    
    static {        
        initOldClasses();
    }

    private String sLaunch;
    private String sMachine;    
                
    private class PrintTimeAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent evt) {
            m_clock = getLineTimer();
            m_date = getLineDate();

            DateTime m_datetime = getDateTime();
        
            m_jLblTitle.setText(m_dlSystem.getResourceAsText("Window.Title"));
            m_jLblTitle.repaint();
            jLabel2.setText("  " + m_date + " " + m_clock);
/*
* JG Note: Arbritary 8 hour cycle for MySQL server ping on chosen port:nnnn
* MySQL default setting is 28800 seconds (8hrs)
* Better than a host ping as need to know if MySQL is alive & kicking
* Be careful though as MySQL could run out of Connections if it's left on default
            

            webProgressBar.setValue(0);
            
            if (getDateTime().getHourOfDay() == 7 
                || getDateTime().getHourOfDay() == 15
                || getDateTime().getHourOfDay() == 23) {

                if (getDateTime().getMinuteOfHour() == 59 && 
                    (getDateTime().getSecondOfMinute() == 59)) {
                    try {
                        if (pingServer()) {
                            webProgressBar.setValue(0);
                        } else {
                            webProgressBar.setString("Server is down!");
                            webProgressBar.setValue(100);
                        }
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
*/                    
        }
    }
    
    private DateTime getDateTime() {
        DateTime dt = DateTime.now();
    return dt;
}
   private String getLineTimer() {
        return Formats.HOURMIN.formatValue(new Date());
    }
    private String getLineDate() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.DEFAULT,getDefaultLocale());
        return df.format(new Date());        
    }
    

    public JRootApp() {
        
        m_aBeanFactories = new HashMap<>();
        
        initComponents ();            
        configureTopBranding();
        configureHealthBar();
        jScrollPane1.getVerticalScrollBar().setPreferredSize(new Dimension(30, 30));
        serverMonitor.setVisible(false);
        webMemoryBar1.setShowMaximumMemory ( true );
    }
    private void configureHealthBar() {
        java.awt.Font font = new java.awt.Font("Arial", java.awt.Font.BOLD, 11);
        panelTask.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 6, 2));
        m_jHost.setFont(font);
        m_healthPeripherals.setFont(font);
        m_healthResources.setFont(font);
        m_jHost.setPreferredSize(new java.awt.Dimension(500, 24));
        m_healthPeripherals.setPreferredSize(new java.awt.Dimension(150, 24));
        m_healthResources.setPreferredSize(new java.awt.Dimension(150, 24));
        webMemoryBar1.setPreferredSize(new java.awt.Dimension(100, 24));
        m_healthPeripherals.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 4));
        m_healthResources.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 4));
        panelTask.add(m_healthPeripherals, 1);
        panelTask.add(m_healthResources, 2);
        refreshHealthBar();
    }

    private void configureTopBranding() {
        java.net.URL logoUrl = getClass().getResource("/com/mx/kylgis/pos/images/kylgis_pos.png");
        if (logoUrl != null) {
            javax.swing.ImageIcon source = new javax.swing.ImageIcon(logoUrl);
            java.awt.Image scaled = source.getImage().getScaledInstance(54, 36, java.awt.Image.SCALE_SMOOTH);
            poweredby.setIcon(new javax.swing.ImageIcon(scaled));
        }
        poweredby.setPreferredSize(new java.awt.Dimension(100, 38));
        poweredby.setMaximumSize(new java.awt.Dimension(100, 38));
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(449, 44));
    }

    private OptionalIButtonMonitor m_iButtonMonitor;

    private void initIButtonMonitor() {
        if (m_iButtonMonitor != null) {
            return;
        }
        try {
            m_iButtonMonitor = OptionalIButtonMonitor.start(
                    m_props.getProperty("machine.iButton.library"),
                    new OptionalIButtonMonitor.Listener() {
                @Override
                public void deviceArrived(String address) {
                    uOWWatch.setIButtonId(address);
                    handleIButtonArrival(address);
                }

                @Override
                public void deviceDeparted(String address) {
                    uOWWatch.clearIButtonId(address);
                    handleIButtonDeparture(address);
                }

                @Override
                public void monitorError(Throwable error) {
                    Logger.getLogger(JRootApp.class.getName()).log(
                            Level.WARNING, "iButton monitor error", error);
                }
            });
        } catch (Exception e) {
            JMessageDialog.showMessage(this,
                    new MessageInf(MessageInf.SGN_WARNING,
                            AppLocal.getIntString("message.ibuttonnotfound"), e));
        }
    }

    private void handleIButtonArrival(final String iButtonId) {
        SwingUtilities.invokeLater(() -> {
            assert m_dlSystem != null;
            AppUser user = null;
            try {
                user = m_dlSystem.findPeopleByCard(iButtonId);
            } catch (BasicException e) {
                Logger.getLogger(JRootApp.class.getName()).log(
                        Level.WARNING, "Unable to resolve iButton user", e);
            }

            if (user == null) {
                JOptionPane.showMessageDialog(this,
                        AppLocal.getIntString("message.ibuttonnotassign"),
                        AppLocal.getIntString("title.editor"),
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                setVisible(false);
                openAppView(user);
                setVisible(true);
            }
        });
    }

    private void handleIButtonDeparture(final String iButtonId) {
        SwingUtilities.invokeLater(() -> {
            if (m_principalapp != null) {
                AppUser currentUser = m_principalapp.getUser();
                if (currentUser != null && Objects.equals(currentUser.getCard(), iButtonId)) {
                    closeAppView();
                }
            }
        });
    }

    private void shutdownIButtonMonitor() {
        if (m_iButtonMonitor != null) {
            m_iButtonMonitor.close();
            m_iButtonMonitor = null;
        }
        uOWWatch.clearIButtonId(null);
    }

    public void releaseResources() {
        shutdownIButtonMonitor();
    }

    /**
     *
     * @param props
     * @return
     */
    public boolean initApp(AppProperties props) {
        
        m_props = props;
        m_jPanelDown.setVisible(!(Boolean.valueOf(m_props.getProperty("till.hideinfo"))));
        setOperationIndicator("Inicializando", new java.awt.Color(184, 134, 11), null);

        applyComponentOrientation(ComponentOrientation.getOrientation(Locale.getDefault()));
        
        try {
            session = AppViewConnection.createSession(m_props);
            
        } catch (BasicException e) {
            setOperationIndicator("Sin conexión", new java.awt.Color(176, 0, 32), null);
            JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_DANGER, e.getMessage(), e));
            return false;
        }

        m_dlSystem = (DataLogicSystem) getBean("com.mx.kylgis.pos.forms.DataLogicSystem");
/**
 * CONDITIONS;
 * if dbversion is null then new createDatabase
 * if dbversion is not null then check the version
 * if appversion equals dbversion then check for updates
 * IF APP_VERSION = existing db version
 * 
*/
        String sDBVersion = readDataBaseVersion();        
        String targetDBVersion = ApplicationIdentity.getVersion();
        if (!targetDBVersion.equals(sDBVersion)
                && DatabaseUpgradeService.supports(sDBVersion, targetDBVersion)) {
            if (JOptionPane.showConfirmDialog(this,
                    AppLocal.getIntString("message.kylgisdbupgrade", sDBVersion, targetDBVersion),
                    AppLocal.getIntString("message.title"),
                    JOptionPane.OK_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
                session.close();
                return false;
            }
            try {
                DatabaseUpgradeService.upgrade(session, m_props, sDBVersion, targetDBVersion);
                sDBVersion = targetDBVersion;
            } catch (BasicException e) {
                JMessageDialog.showMessage(this,
                        new MessageInf(MessageInf.SGN_DANGER,
                        AppLocal.getIntString("database.scripterror"), e));
                session.close();
                return false;
            }
        }

        if (!targetDBVersion.equals(sDBVersion)) {
            String sScript = sDBVersion == null 
            ? m_dlSystem.getInitScript() + "-create.sql"
            : m_dlSystem.getInitScript() + "-upgrade_master.sql";                    

            if (JRootApp.class.getResource(sScript) == null) {
                JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_DANGER, sDBVersion == null
                    ? AppLocal.getIntString("message.databasenotsupported", session.DB.getName() + " " + sDBVersion)
                    : AppLocal.getIntString("message.noupdatescript")));
                session.close();
                return false;
            } else {
                if (JOptionPane.showConfirmDialog(this
                    , AppLocal.getIntString(sDBVersion == null 
                        ? "message.createdatabase" 
                        : "message.eolupdate", session.DB.getName() + " " + sDBVersion)
                    , AppLocal.getIntString("message.title")
                    , JOptionPane.OK_OPTION
                    , JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
                    
                    try {
                        BatchSentence bsentence = new BatchSentenceResource(session, sScript);
                        bsentence.putParameter("APP_ID", Matcher.quoteReplacement(ApplicationIdentity.getDatabaseId(m_props)));
                        bsentence.putParameter("APP_NAME", Matcher.quoteReplacement(ApplicationIdentity.getDatabaseName(m_props)));
                        bsentence.putParameter("APP_VERSION", Matcher.quoteReplacement(ApplicationIdentity.getVersion()));

                        java.util.List l = bsentence.list();

                        if (l.size() > 0) { 
                            JMessageDialog.showMessage(this, new MessageInf(MessageInf.SGN_WARNING
                                , AppLocal.getIntString("database.scriptwarning")
                                , l.toArray(new Throwable[l.size()])));
                        }
                    } catch (BasicException e) {
                        JMessageDialog.showMessage(this
                            , new MessageInf(MessageInf.SGN_DANGER
                            , AppLocal.getIntString("database.scripterror"), e));
                        session.close();
                        return false;
                    } 
                } else {
                    session.close();
                    return false;
                }
            }   
        }

        /*
        * JG 10 Dec 2018
        * Test rig in prep' to track user install/launch success/fails
        * We only need core info' to identify for POC
        * For testing; code and declares are deliberately verbose
        * 
        * FUTURE : 
        * App' error logging 
        * Also auto-Notify users of available app' updates. 
        * To be replaced with our REST API
        */        
        
        try {
            sMachine = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
        }
// create the filename
        String sUserPath = System.getProperty("user.home"); 
        String filePath = sUserPath + "/" + sMachine + ".lau";
        
        Instant machineTimestamp = Instant.now();
        String sContent = sUserPath + "," 
                + machineTimestamp + "," 
                + ApplicationIdentity.getDatabaseId(m_props) + ","
                + ApplicationIdentity.getDatabaseName(m_props) + ","
                + ApplicationIdentity.getVersion() + "\n";
        
        try {
            Files.write(Paths.get(filePath), sContent.getBytes(), 
                    StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try {
            filePath = sUserPath + "/open.db";
            Files.write(Paths.get(filePath), sContent.getBytes(), 
                    StandardOpenOption.CREATE);
        } catch (IOException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
        }        

        m_propsdb = m_dlSystem.getResourceAsProperties(m_props.getHost() + "/properties");

        try {
            String sActiveCashIndex = m_propsdb.getProperty("activecash");
            Object[] valcash = sActiveCashIndex == null
                    ? null
                    : m_dlSystem.findActiveCash(sActiveCashIndex);
            if (valcash == null || !m_props.getHost().equals(valcash[0])) {
                setActiveCash(UUID.randomUUID().toString()
                    , m_dlSystem.getSequenceCash(m_props.getHost()) + 1, new Date(), null);
                m_dlSystem.execInsertCash(
                new Object[] {getActiveCashIndex(), m_props.getHost()
                    , getActiveCashSequence()
                    , getActiveCashDateStart()
                    , getActiveCashDateEnd()});                  
            } else {
                setActiveCash(sActiveCashIndex
                    , (Integer) valcash[1]
                    , (Date) valcash[2]
                    , (Date) valcash[3]);
            }
        } catch (BasicException e) {
            MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE
                , AppLocal.getIntString("message.cannotclosecash"), e);
            msg.show(this);
            session.close();
            return false;
        }  
        
        m_sInventoryLocation = m_propsdb.getProperty("location");
        if (m_sInventoryLocation == null) {
            m_sInventoryLocation = "0";
            m_propsdb.setProperty("location", m_sInventoryLocation);
            m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties"
                , m_propsdb);
        }
        
        m_TP = new DeviceTicket(this, m_props);
        
        m_TTP = new TicketParser(getDeviceTicket(), m_dlSystem);
        printerStart();
        
        m_Scale = new DeviceScale(this, m_props);
               
        m_Scanner = DeviceScannerFactory.createInstance(m_props);
            
        new javax.swing.Timer(250, new PrintTimeAction()).start();
        
        updateOperationIndicator();
        startHealthMonitor();

        String newLogo = m_props.getProperty("start.logo");
        if (newLogo != null) {
           if ("".equals(newLogo)){
                jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/kylgis_pos.png")));
           }else{
                jLabel1.setIcon(new javax.swing.ImageIcon (newLogo));
            }
        }

        String newText = m_props.getProperty("start.text");
        if (newText != null) {
            if (newText.equals("")){
                jLabel1.setText("<html><center>KylGis POS Punto de Venta Táctil<br>" +
                "Copyright \u00A9 2026 KylGis POS<br>" +
                "https://pos.kylgis.com<br>" +
                "<br>" +
                "KylGis POS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.<br>" +
                "<br>" +
                "KylGis POS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>" +
                "<br>" +
                "You should have received a copy of the GNU General Public License along with KylGis POS. If not, see http://www.gnu.org/licenses/<br>" +
                "</center>");
                
            } else {
                try {    
                    String newTextCode = new Scanner(new File(newText)
                        , "UTF-8").useDelimiter("\\A").next();
                    jLabel1.setText(newTextCode);
                }
                    catch (FileNotFoundException e) {
                }
            
                    jLabel1.setAlignmentX(0.5F);
                    jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
                    jLabel1.setMaximumSize(new java.awt.Dimension(800, 1024));
                    jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
            }
        }
           
        showLogin();
        
        String ibutton = m_props.getProperty("machine.iButton");        
        if(ibutton.equals("true")) {
            initIButtonMonitor();
            updateOperationIndicator();
        }    
        return true;
    }
   
    private void updateOperationIndicator() {
        m_health.clear(OperationalHealth.Category.DATABASE);
        diagnosePeripherals();
        diagnoseResources();
        refreshHealthBar();
    }

    private void setOperationIndicator(String state, java.awt.Color color, String dbLastOctet) {
        m_health.clear(OperationalHealth.Category.DATABASE);
        OperationalHealth.Severity severity = "Sin conexión".equals(state)
                ? OperationalHealth.Severity.RED : OperationalHealth.Severity.AMBER;
        m_health.report(OperationalHealth.Category.DATABASE, "database.startup",
                severity, state, OperationalHealth.CAP_STARTUP);
        refreshHealthBar();
    }

    private void refreshHealthBar() {
        updateHealthLabel(m_jHost, OperationalHealth.Category.DATABASE,
                "Host: " + escapeHtml(safeStatusText(m_props == null ? null : m_props.getHost(), "POS"))
                + databaseSuffix());
        updateHealthLabel(m_healthPeripherals, OperationalHealth.Category.PERIPHERALS, null);
        updateHealthLabel(m_healthResources, OperationalHealth.Category.RESOURCES, null);
        panelTask.revalidate();
        panelTask.repaint();
    }

    private String databaseSuffix() {
        String lastOctet = getDatabaseIpv4LastOctet();
        return lastOctet == null ? "" : " · IP: …" + escapeHtml(lastOctet);
    }

    private void updateHealthLabel(JLabel label, OperationalHealth.Category category, String suffix) {
        OperationalHealth.Severity severity = m_health.getSeverity(category);
        String state;
        java.awt.Color color;
        switch (severity) {
            case RED:
                state = "Corrección necesaria";
                color = new java.awt.Color(176, 0, 32);
                break;
            case AMBER:
                state = "Atención";
                color = new java.awt.Color(184, 134, 11);
                break;
            default:
                state = "OK";
                color = new java.awt.Color(0, 128, 0);
                break;
        }
        StringBuilder text = new StringBuilder("<html><b>● ")
                .append(escapeHtml(category.getLabel()))
                .append(": ").append(escapeHtml(state)).append("</b>");
        if (suffix != null && !suffix.isEmpty()) {
            text.append(" · ").append(suffix);
        }
        text.append("</html>");
        label.setIcon(null);
        label.setForeground(color);
        label.setText(text.toString());
        label.setToolTipText(buildHealthTooltip(category));
    }

    private String buildHealthTooltip(OperationalHealth.Category category) {
        java.util.List<OperationalHealth.Issue> issues = m_health.getIssues(category);
        if (issues.isEmpty()) {
            return category.getLabel() + ": sin incidencias detectadas";
        }
        StringBuilder tooltip = new StringBuilder("<html>");
        for (OperationalHealth.Issue issue : issues) {
            tooltip.append("• ").append(escapeHtml(issue.getMessage())).append("<br>");
        }
        return tooltip.append("</html>").toString();
    }

    private void diagnosePeripherals() {
        m_health.clear(OperationalHealth.Category.PERIPHERALS);

        String receiptIndex = m_TP == null ? "1" : m_TP.getReceiptPrinterIndex();
        for (int i = 1; i <= 6; i++) {
            String key = i == 1 ? "machine.printer" : "machine.printer." + i;
            String configured = normalizedConfig(m_props.getProperty(key));
            if (!isConfiguredDevice(configured)) {
                continue;
            }
            boolean unavailable = m_TP == null
                    || m_TP.getDevicePrinter(Integer.toString(i)) instanceof com.mx.kylgis.pos.printer.DevicePrinterNull
                    || configuredEndpointUnavailable(configured);
            if (unavailable) {
                String capability = Integer.toString(i).equals(receiptIndex)
                        ? OperationalHealth.CAP_PRINT_RECEIPT : null;
                m_health.report(OperationalHealth.Category.PERIPHERALS,
                        "printer." + i, OperationalHealth.Severity.AMBER,
                        "Impresora " + i + " configurada pero no disponible", capability);
            }
        }

        String scale = normalizedConfig(m_props.getProperty("machine.scale"));
        if (isConfiguredDevice(scale) && (m_Scale == null || !m_Scale.existsScale()
                || configuredEndpointUnavailable(scale))) {
            m_health.report(OperationalHealth.Category.PERIPHERALS, "scale",
                    OperationalHealth.Severity.AMBER,
                    "Báscula configurada pero no disponible", OperationalHealth.CAP_SCALE);
        }

        String scanner = normalizedConfig(m_props.getProperty("machine.scanner"));
        if (isConfiguredDevice(scanner) && m_Scanner == null) {
            m_health.report(OperationalHealth.Category.PERIPHERALS, "scanner",
                    OperationalHealth.Severity.AMBER,
                    "Escáner configurado pero no disponible", OperationalHealth.CAP_SCANNER);
        }

        String display = normalizedConfig(m_props.getProperty("machine.display"));
        if (isConfiguredDevice(display) && (m_TP == null
                || m_TP.getDeviceDisplay() instanceof com.mx.kylgis.pos.printer.DeviceDisplayNull)) {
            m_health.report(OperationalHealth.Category.PERIPHERALS, "display",
                    OperationalHealth.Severity.AMBER,
                    "Display configurado pero no disponible", OperationalHealth.CAP_DISPLAY);
        }

        if (Boolean.parseBoolean(m_props.getProperty("machine.iButton")) && m_iButtonMonitor == null) {
            m_health.report(OperationalHealth.Category.PERIPHERALS, "ibutton",
                    OperationalHealth.Severity.AMBER,
                    "iButton configurado pero no disponible", OperationalHealth.CAP_IBUTTON);
        }
    }

    private void startHealthMonitor() {
        if (m_healthMonitorTimer != null) {
            m_healthMonitorTimer.stop();
        }
        m_healthMonitorTimer = new javax.swing.Timer(5000, evt -> refreshPeripheralHealthAsync());
        m_healthMonitorTimer.setInitialDelay(5000);
        m_healthMonitorTimer.start();
    }

    private void refreshPeripheralHealthAsync() {
        if (m_healthProbeRunning || m_props == null) {
            return;
        }
        m_healthProbeRunning = true;
        new javax.swing.SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                diagnosePeripherals();
                return null;
            }

            @Override
            protected void done() {
                m_healthProbeRunning = false;
                refreshHealthBar();
            }
        }.execute();
    }

    private boolean configuredEndpointUnavailable(String configured) {
        String endpoint = extractDevicePath(configured);
        if (endpoint == null) {
            return false;
        }
        java.nio.file.Path path = java.nio.file.Paths.get(endpoint);
        if (!java.nio.file.Files.exists(path)
                || !java.nio.file.Files.isReadable(path)
                || !java.nio.file.Files.isWritable(path)) {
            return true;
        }
        // RFCOMM printers are expected to stay disconnected while idle and
        // establish the Bluetooth link only when a print job writes to the port.
        // Treating the idle RFCOMM state as a fault creates a false amber health
        // condition. Runtime print failures are reported by the print paths.
        return false;
    }

    private static String extractDevicePath(String configured) {
        if (configured == null) {
            return null;
        }
        int start = configured.indexOf("/dev/");
        if (start < 0) {
            return null;
        }
        int end = start;
        while (end < configured.length()) {
            char ch = configured.charAt(end);
            if (Character.isWhitespace(ch) || ch == ',' || ch == ';') {
                break;
            }
            end++;
        }
        return end > start ? configured.substring(start, end) : null;
    }

    private boolean isRfcommConnected(String endpoint) {
        String index = endpoint.substring("/dev/rfcomm".length());
        Process process = null;
        try {
            process = new ProcessBuilder("rfcomm", "show", index)
                    .redirectErrorStream(true).start();
            if (!process.waitFor(1200, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8).toLowerCase(java.util.Locale.ROOT);
            return process.exitValue() == 0 && output.contains("connected");
        } catch (IOException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(Level.FINE,
                    "No se pudo consultar estado RFCOMM de " + endpoint, ex);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private void diagnoseResources() {
        m_health.clear(OperationalHealth.Category.RESOURCES);
        checkPrinterResource("Printer.Ticket", OperationalHealth.CAP_PRINT_RECEIPT);
        checkPrinterResource("Printer.Ticket2", OperationalHealth.CAP_PRINT_RECEIPT_ALT);
        checkPrinterResource("Printer.TicketTotal", OperationalHealth.CAP_PRINT_TOTAL);
        checkPrinterResource("Printer.TicketLine", OperationalHealth.CAP_PRINT_LINE);
        checkPrinterResource("Printer.CustomerPaid", OperationalHealth.CAP_PRINT_CUSTOMER);
        checkPrinterResource("Printer.CustomerPaid2", OperationalHealth.CAP_PRINT_CUSTOMER);
        checkPrinterResource("Printer.TicketPreview", OperationalHealth.CAP_PRINT_PREVIEW);
        checkPrinterResource("Printer.TicketPreview2", OperationalHealth.CAP_PRINT_PREVIEW);
        checkPrinterResource("Printer.Inventory", OperationalHealth.CAP_PRINT_INVENTORY);
        checkPrinterResource("Printer.CloseCash", OperationalHealth.CAP_PRINT_CASH);
        checkPrinterResource("Printer.PartialCash", OperationalHealth.CAP_PRINT_CASH);
        checkPrinterResource("Printer.CloseCash.Preview", OperationalHealth.CAP_PRINT_PREVIEW);
        checkPrinterResource("Printer.ReprintTicket", OperationalHealth.CAP_PRINT_REPRINT);
        checkPrinterResource("Printer.ReprintLastTicket", OperationalHealth.CAP_PRINT_REPRINT);
        checkPrinterResource("Printer.Start", null);
    }

    private void checkPrinterResource(String name, String capability) {
        String template = m_dlSystem == null ? null : m_dlSystem.getResourceAsXML(name);
        if (template == null || template.trim().isEmpty()) {
            m_health.report(OperationalHealth.Category.RESOURCES, name,
                    OperationalHealth.Severity.AMBER,
                    "Plantilla ausente: " + name, capability);
            return;
        }
        try {
            com.mx.kylgis.pos.printer.TicketTemplateValidator.validateForRuntimeHealth(name, template);
        } catch (BasicException ex) {
            m_health.report(OperationalHealth.Category.RESOURCES, name,
                    OperationalHealth.Severity.AMBER,
                    "Plantilla inválida: " + name, capability);
        }
    }

    private static String normalizedConfig(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isConfiguredDevice(String value) {
        return value != null && !value.isEmpty()
                && !"Not defined".equalsIgnoreCase(value)
                && !"none".equalsIgnoreCase(value);
    }

    private String getDatabaseIpv4LastOctet() {
        if (session == null) {
            return null;
        }
        try {
            String jdbcUrl = session.getURL();
            if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) {
                return null;
            }
            java.net.URI uri = new java.net.URI(jdbcUrl.substring("jdbc:".length()));
            String host = uri.getHost();
            if (host == null) {
                return null;
            }
            String[] octets = host.split("\\.");
            if (octets.length != 4) {
                return null;
            }
            for (String octet : octets) {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return null;
                }
            }
            return octets[3];
        } catch (SQLException | java.net.URISyntaxException | NumberFormatException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(
                    Level.FINE, "Unable to derive redacted database endpoint for status bar.", ex);
            return null;
        }
    }

    private static String safeStatusText(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String readDataBaseVersion() {
        try {
            return m_dlSystem.findVersion();
        } catch (BasicException ed) {
            return null;
        }
    }
    
    public void tryToClose() {   
        
        if (closeAppView()) {
                if (m_healthMonitorTimer != null) {
                    m_healthMonitorTimer.stop();
                }
                m_TP.getDeviceDisplay().clearVisor();
                m_TP.close();
                shutdownIButtonMonitor();

// delete the open.db tracking file
                String sUserPath = System.getProperty("user.home");
//                String filePath = sUserPath + "\\open.db";
                String filePath = sUserPath + "/open.db";
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException ex) {
                Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            session.close();
            SwingUtilities.getWindowAncestor(this).dispose();            
        }
    }
    
    @Override
    public DeviceTicket getDeviceTicket(){
        return m_TP;
    }
    
    @Override
    public DeviceScale getDeviceScale() {
        return m_Scale;
    }

    @Override
    public DeviceScanner getDeviceScanner() {
        return m_Scanner;
    }
    
    @Override
    public Session getSession() {
        return session;
    } 

    @Override
    public String getInventoryLocation() {
        return m_sInventoryLocation;
    }   

    @Override
    public String getActiveCashIndex() {
        return m_sActiveCashIndex;
    }

    @Override
    public int getActiveCashSequence() {
        return m_iActiveCashSequence;
    }

    @Override
    public Date getActiveCashDateStart() {
        return m_dActiveCashDateStart;
    }

    @Override
    public Date getActiveCashDateEnd(){
        return m_dActiveCashDateEnd;
    }

    @Override
    public void setActiveCash(String sIndex, int iSeq, Date dStart, Date dEnd) {
        m_sActiveCashIndex = sIndex;
        m_iActiveCashSequence = iSeq;
        m_dActiveCashDateStart = dStart;
        m_dActiveCashDateEnd = dEnd;
        
        m_propsdb.setProperty("activecash", m_sActiveCashIndex);
        m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties", m_propsdb);
    }
    
    @Override
    public String getClosedCashIndex() {
        return m_sClosedCashIndex;
    }

    @Override
    public int getClosedCashSequence() {
        return m_iClosedCashSequence;
    }

    @Override
    public Date getClosedCashDateStart() {
        return m_dClosedCashDateStart;
    }

    @Override
    public Date getClosedCashDateEnd(){
        return m_dClosedCashDateEnd;
    }
    
    @Override
    public void setClosedCash(String sIndex, int iSeq, Date dStart, Date dEnd) {
        m_sClosedCashIndex = sIndex;
        m_iClosedCashSequence = iSeq;
        m_dClosedCashDateStart = dStart;
        m_dClosedCashDateEnd = dEnd;

        m_dlSystem.setResourceAsProperties(m_props.getHost() + "/properties", m_propsdb);
    }    

    @Override
    public AppProperties getProperties() {
        return m_props;
    }

    @Override
    public OperationalHealth getOperationalHealth() {
        return m_health;
    }

    @Override
    public void refreshOperationalHealthIndicators() {
        refreshHealthBar();
    }
    
    @Override
    public Object getBean(String beanfactory) throws BeanFactoryException {
        
        beanfactory = mapNewClass(beanfactory);
        BeanFactory bf = m_aBeanFactories.get(beanfactory);

        if (bf == null) {   

            if (beanfactory.startsWith("/")) {
                bf = new BeanFactoryScript(beanfactory);               
            } else {
                try {
                    Class bfclass = Class.forName(beanfactory);

                    if (BeanFactory.class.isAssignableFrom(bfclass)) {
                        bf = (BeanFactory) bfclass.getDeclaredConstructor().newInstance();             
                    } else {
                        Constructor constMyView = bfclass.getConstructor(new Class[] {AppView.class});
                        Object bean = constMyView.newInstance(new Object[] {this});
                        bf = new BeanFactoryObj(bean);
                    }

                } catch (ClassNotFoundException | InstantiationException 
                        | IllegalAccessException | NoSuchMethodException 
                        | SecurityException | IllegalArgumentException | InvocationTargetException e) {
                    throw new BeanFactoryException(e);
                }
            }
            
            m_aBeanFactories.put(beanfactory, bf);         
            
            if (bf instanceof BeanFactoryApp) {
                ((BeanFactoryApp) bf).init(this);
            }
        }
        return bf.getBean();
    }
    
    private static String mapNewClass(String classname) {
        String newclass = m_oldclasses.get(classname);
        return newclass == null 
                ? classname 
                : newclass;
    }
    
    private static void initOldClasses() {
        m_oldclasses = new HashMap<>();
        
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportCustomers", "/com/mx/kylgis/pos/reports/customers.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportCustomersB", "/com/mx/kylgis/pos/reports/customers_b.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportClosedPos", "/com/mx/kylgis/pos/reports/sales_closedpos.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportClosedProducts", "/com/mx/kylgis/pos/reports/sales_closedproducts.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JChartSales", "/com/mx/kylgis/pos/reports/sales_chart_sales.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportInventory", "/com/mx/kylgis/pos/reports/inventory.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportInventory2", "/com/mx/kylgis/pos/reports/inventory_b.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportInventoryBroken", "/com/mx/kylgis/pos/reports/inventory_broken.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportInventoryDiff", "/com/mx/kylgis/pos/reports/inventory_diff.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportPeople", "/com/mx/kylgis/pos/reports/users.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportTaxes", "/com/mx/kylgis/pos/reports/sales_taxes.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportUserSales", "/com/mx/kylgis/pos/reports/users_sales.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportProducts", "/com/mx/kylgis/pos/reports/products.bs");
        m_oldclasses.put("com.mx.kylgis.pos.reports.JReportCatalog", "/com/mx/kylgis/pos/reports/products_catalog.bs");
        
        m_oldclasses.put("com.mx.kylgis.pos.panels.JPanelTax", "com.mx.kylgis.pos.inventory.TaxPanel");
       
    }
    
    @Override
    public void waitCursorBegin() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }
    
    @Override
    public void waitCursorEnd(){
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
    @Override
    public AppUserView getAppUserView() {
        return m_principalapp;
    }

    private void printerStart() {
        
        String sresource = m_dlSystem.getResourceAsXML("Printer.Start");
        if (sresource == null) {
            m_TP.getDeviceDisplay().writeVisor(ApplicationIdentity.getDisplayName(m_props), ApplicationIdentity.getVersion());
        } else {
            try {
                m_TTP.printTicket(sresource);
            } catch (TicketPrinterException eTP) {
                m_TP.getDeviceDisplay().writeVisor(ApplicationIdentity.getDisplayName(m_props), ApplicationIdentity.getVersion());
            }
        }        
    }
  
    private void listPeople() {
        
        try {
           
            jScrollPane1.getViewport().setView(null);

            JFlowPanel jPeople = new JFlowPanel();
            jPeople.applyComponentOrientation(getComponentOrientation());
           
            java.util.List people = m_dlSystem.listPeopleVisible();
                     
            for (Object people1 : people) {
                AppUser user = (AppUser) people1;
                JButton btn = new JButton(new AppUserAction(user));
                btn.applyComponentOrientation(getComponentOrientation());
                btn.setFocusPainted(false);
                btn.setFocusable(false);
                btn.setRequestFocusEnabled(false);
                btn.setMaximumSize(new Dimension(110, 60));
                btn.setPreferredSize(new Dimension(110, 60));
                btn.setMinimumSize(new Dimension(110, 60));
                btn.setHorizontalAlignment(SwingConstants.CENTER);
                btn.setHorizontalTextPosition(AbstractButton.CENTER);                 
                btn.setVerticalTextPosition(AbstractButton.BOTTOM);
                jPeople.add(btn);                    
            }

            jScrollPane1.getViewport().setView(jPeople);
            
        } catch (BasicException ee) {
        }
    }

    class AppUserAction extends AbstractAction {
      
        private final AppUser m_actionuser;
        
        public AppUserAction(AppUser user) {
            m_actionuser = user;
            putValue(Action.SMALL_ICON, m_actionuser.getIcon());
            putValue(Action.NAME, m_actionuser.getName());
        }
        
        public AppUser getUser() {
            return m_actionuser;
        }
        
        @Override
        public void actionPerformed(ActionEvent evt) {

            if (m_actionuser.authenticate()) {
                openAppView(m_actionuser);         
            } else {
                String sPassword = JPasswordDialog.showEditPassword(JRootApp.this, 
                    AppLocal.getIntString("label.Password"),
                    m_actionuser.getName(),
                    m_actionuser.getIcon());
                if (sPassword != null) {

                    if (m_actionuser.authenticate(sPassword)) {
                        openAppView(m_actionuser);                
                    } else {
                        MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                                AppLocal.getIntString("message.BadPassword"));
                        msg.show(JRootApp.this);                        
                    }
                }   
            }
        }
    }
    
    private void showView(String view) {
        CardLayout cl = (CardLayout)(m_jPanelContainer.getLayout());
        cl.show(m_jPanelContainer, view);  
    }
    
    private void openAppView(AppUser user) {
        
        if (closeAppView()) {

            m_principalapp = new JPrincipalApp(this, user);

            jPanel3.add(m_principalapp.getNotificator());
            jPanel3.revalidate();
            
            m_jPanelContainer.add(m_principalapp
                , "_" + m_principalapp.getUser().getId());
            showView("_" + m_principalapp.getUser().getId());

            m_principalapp.activate();
        }
    }

    public void exitToLogin() {
        closeAppView();
        showLogin();
    }
    
    public boolean closeAppView() {
        
        if (m_principalapp == null) {
            return true;
        } else if (!m_principalapp.deactivate()) {
            return false;
        } else {
            jPanel3.remove(m_principalapp.getNotificator());
            jPanel3.revalidate();
            jPanel3.repaint();

            m_jPanelContainer.remove(m_principalapp);
            m_principalapp = null;

            showLogin();
            
            return true;
        }
    }
    
    private void showLogin() {
        
        listPeople();
        showView("login");     

        printerStart();
 
        inputtext = new StringBuilder();
        m_txtKeys.setText(null);       
        java.awt.EventQueue.invokeLater(() -> {
            m_txtKeys.requestFocus();
        }); 
    }
    
    private void processKey(char c) {
        
        if ((c == '\n') || (c =='?')) {
            AppUser user = null;
            try {
                user = m_dlSystem.findPeopleByCard(inputtext.toString());
            } catch (BasicException e) {
            }
            
            if (user == null)  {
                MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, 
                        AppLocal.getIntString("message.nocard"));
                msg.show(this);                
            } else {
                openAppView(user);   
            }

            inputtext = new StringBuilder();
        } else {
            inputtext.append(c);
        }
    }
    
    private int getProgressBar() {
        int rate = serverMonitor.getValue();        
        return rate;
    }
    
    private boolean pingServer() throws UnknownHostException {
    /* 
     * This method is for the future. Connects and will include both servers + backup server
     * Tested locally on JG machine and unicenta-server   
    */
        serverMonitor.setString("Checking...");
        
        InetAddress addr = InetAddress.getByName(AppLocal.getIntString("db.ip"));
        int port = 3306;

        SocketAddress sockaddr = new InetSocketAddress(addr, port);
        Socket sock = new Socket();
        try {
            sock.connect(sockaddr, 2000);    
            serverMonitor.setString("Server is alive!");                    
            serverMonitor.setValue(0);
            return true;
        } catch (IOException ex) {
            Logger.getLogger(JRootApp.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
        
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jPanelTitle = new javax.swing.JPanel();
        m_jLblTitle = new javax.swing.JLabel();
        poweredby = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        m_jPanelContainer = new javax.swing.JPanel();
        m_jPanelLogin = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 0));
        jPanel5 = new javax.swing.JPanel();
        m_jLogonName = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        m_txtKeys = new javax.swing.JTextField();
        m_jClose = new javax.swing.JButton();
        m_jPanelDown = new javax.swing.JPanel();
        panelTask = new javax.swing.JPanel();
        m_jHost = new javax.swing.JLabel();
        webMemoryBar1 = new com.alee.extended.statusbar.WebMemoryBar();
        serverMonitor = new com.alee.laf.progressbar.WebProgressBar();
        jPanel3 = new javax.swing.JPanel();

        setEnabled(false);
        setPreferredSize(new java.awt.Dimension(1024, 768));
        setLayout(new java.awt.BorderLayout());

        m_jPanelTitle.setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")));
        m_jPanelTitle.setPreferredSize(new java.awt.Dimension(449, 40));
        m_jPanelTitle.setLayout(new java.awt.BorderLayout());

        m_jLblTitle.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        m_jLblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        m_jLblTitle.setText("Window.Title");
        m_jPanelTitle.add(m_jLblTitle, java.awt.BorderLayout.CENTER);

        poweredby.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        poweredby.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/kylgis_pos.png"))); // NOI18N
        poweredby.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        poweredby.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        poweredby.setMaximumSize(new java.awt.Dimension(180, 34));
        poweredby.setPreferredSize(new java.awt.Dimension(180, 34));
        m_jPanelTitle.add(poweredby, java.awt.BorderLayout.LINE_END);

        jLabel2.setFont(new java.awt.Font("Arial", 1, 16)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(102, 102, 102));
        jLabel2.setPreferredSize(new java.awt.Dimension(180, 34));
        m_jPanelTitle.add(jLabel2, java.awt.BorderLayout.LINE_START);

        add(m_jPanelTitle, java.awt.BorderLayout.NORTH);

        m_jPanelContainer.setLayout(new java.awt.CardLayout());

        m_jPanelLogin.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

        jLabel1.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/kylgis_pos.png"))); // NOI18N
        jLabel1.setText("<html><center>KylGis POS Punto de Venta Táctil<br>" +
            "Copyright \u00A9 2026 KylGis POS<br>" +
            "https://pos.kylgis.com<br>" +
            "<br>" +
            "KylGis POS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.<br>" +
            "<br>" +
            "KylGis POS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.<br>" +
            "<br>" +
            "You should have received a copy of the GNU General Public License along with KylGis POS. If not, see http://www.gnu.org/licenses/<br>" +
            "</center>");
        jLabel1.setAlignmentX(0.5F);
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setMaximumSize(new java.awt.Dimension(800, 1024));
        jLabel1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jPanel4.add(jLabel1);
        jPanel4.add(filler2);

        m_jPanelLogin.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel5.setPreferredSize(new java.awt.Dimension(300, 400));

        m_jLogonName.setLayout(new java.awt.BorderLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
        jPanel2.setPreferredSize(new java.awt.Dimension(100, 100));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new java.awt.GridLayout(0, 1, 5, 5));
        jPanel2.add(jPanel8, java.awt.BorderLayout.NORTH);

        m_jLogonName.add(jPanel2, java.awt.BorderLayout.LINE_END);

        jScrollPane1.setBorder(null);
        jScrollPane1.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane1.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        m_txtKeys.setPreferredSize(new java.awt.Dimension(0, 0));
        m_txtKeys.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                m_txtKeysKeyTyped(evt);
            }
        });

        m_jClose.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/exit.png"))); // NOI18N
        m_jClose.setText(AppLocal.getIntString("button.close")); // NOI18N
        m_jClose.setFocusPainted(false);
        m_jClose.setFocusable(false);
        m_jClose.setPreferredSize(new java.awt.Dimension(100, 50));
        m_jClose.setRequestFocusEnabled(false);
        m_jClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jCloseActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1Layout.createSequentialGroup()
                        .add(m_txtKeys, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(0, 0, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .add(m_jClose, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(m_txtKeys, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(m_jClose, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jScrollPane1))
                .add(104, 104, 104)
                .add(m_jLogonName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .add(15, 15, 15)
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(m_jLogonName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(434, 434, 434))
                    .add(jPanel5Layout.createSequentialGroup()
                        .add(jScrollPane1)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())))
        );

        m_jPanelLogin.add(jPanel5, java.awt.BorderLayout.EAST);

        m_jPanelContainer.add(m_jPanelLogin, "login");

        add(m_jPanelContainer, java.awt.BorderLayout.CENTER);

        m_jPanelDown.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")));
        m_jPanelDown.setLayout(new java.awt.BorderLayout());

        m_jHost.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        m_jHost.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/display.png"))); // NOI18N
        m_jHost.setText("*Hostname");
        panelTask.add(m_jHost);

        webMemoryBar1.setBackground(new java.awt.Color(153, 153, 153));
        webMemoryBar1.setText("");
        webMemoryBar1.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        webMemoryBar1.setPreferredSize(new java.awt.Dimension(150, 30));
        webMemoryBar1.setUsedBorderColor(new java.awt.Color(0, 204, 204));
        webMemoryBar1.setUsedFillColor(new java.awt.Color(0, 204, 255));
        panelTask.add(webMemoryBar1);

        serverMonitor.setToolTipText("");
        serverMonitor.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        serverMonitor.setMaximumSize(new java.awt.Dimension(50, 18));
        serverMonitor.setPreferredSize(new java.awt.Dimension(150, 30));
        serverMonitor.setProgressBottomColor(new java.awt.Color(76, 197, 237));
        serverMonitor.setRound(2);
        serverMonitor.setString("Keep Alive");
        serverMonitor.setStringPainted(true);
        panelTask.add(serverMonitor);

        m_jPanelDown.add(panelTask, java.awt.BorderLayout.LINE_START);
        m_jPanelDown.add(jPanel3, java.awt.BorderLayout.LINE_END);

        add(m_jPanelDown, java.awt.BorderLayout.SOUTH);
    }// </editor-fold>//GEN-END:initComponents


    private void m_txtKeysKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_m_txtKeysKeyTyped

        m_txtKeys.setText("0");
        processKey(evt.getKeyChar());

    }//GEN-LAST:event_m_txtKeysKeyTyped

    private void m_jCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jCloseActionPerformed
        tryToClose();
    }//GEN-LAST:event_m_jCloseActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton m_jClose;
    private javax.swing.JLabel m_jHost;
    private javax.swing.JLabel m_jLblTitle;
    private javax.swing.JPanel m_jLogonName;
    private javax.swing.JPanel m_jPanelContainer;
    private javax.swing.JPanel m_jPanelDown;
    private javax.swing.JPanel m_jPanelLogin;
    private javax.swing.JPanel m_jPanelTitle;
    private javax.swing.JTextField m_txtKeys;
    private javax.swing.JPanel panelTask;
    private javax.swing.JLabel poweredby;
    private com.alee.laf.progressbar.WebProgressBar serverMonitor;
    private com.alee.extended.statusbar.WebMemoryBar webMemoryBar1;
    // End of variables declaration//GEN-END:variables
}