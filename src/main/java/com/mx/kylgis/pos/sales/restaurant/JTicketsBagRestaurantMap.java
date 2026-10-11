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
package com.mx.kylgis.pos.sales.restaurant;

import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import com.mx.kylgis.pos.basic.BasicException;
import com.mx.kylgis.pos.beans.JGuestsPop;
import com.mx.kylgis.pos.data.gui.MessageInf;
import com.mx.kylgis.pos.data.gui.NullIcon;
import com.mx.kylgis.pos.data.loader.SentenceList;
import com.mx.kylgis.pos.data.loader.SerializerReadClass;
import com.mx.kylgis.pos.data.loader.StaticSentence;
import com.mx.kylgis.pos.customers.CustomerInfo;
import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.forms.AppLocal;
import com.mx.kylgis.pos.forms.AppView;
import com.mx.kylgis.pos.forms.DataLogicSales;
import com.mx.kylgis.pos.forms.DataLogicSystem;
import com.mx.kylgis.pos.sales.DataLogicReceipts;
import com.mx.kylgis.pos.sales.JTicketsBag;
import com.mx.kylgis.pos.sales.restaurant.JTicketsBagRestaurant;
import com.mx.kylgis.pos.sales.TicketsEditor;
import com.mx.kylgis.pos.ticket.TicketInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 *
 * @author JG uniCenta
 */
public class JTicketsBagRestaurantMap extends JTicketsBag {

    private static final Logger LOGGER = Logger.getLogger(JTicketsBagRestaurantMap.class.getName());
    private static final int MIN_AUTO_REFRESH_SECONDS = 5;
    private static final int MAX_IDLE_AUTO_REFRESH_DELAY_MS = 15000;
    private static final int MAX_ERROR_AUTO_REFRESH_DELAY_MS = 60000;

    private enum RefreshOutcome {
        CHANGED,
        UNCHANGED,
        FAILED
    }

    private static class ServerCurrent {

        public ServerCurrent() {
        }
    }

    private java.util.List<Place> m_aplaces;
    private java.util.List<Floor> m_afloors;
    
    private JTicketsBagRestaurant m_restaurantmap;
        
    private final JTicketsBagRestaurantRes m_jreservations;   
    private Place m_PlaceCurrent;
    private ServerCurrent m_ServerCurrent;
    private Place m_PlaceClipboard;  
    private CustomerInfo customer;

    private DataLogicReceipts dlReceipts = null;
    private DataLogicSales dlSales = null;
    private DataLogicSystem dlSystem = null;    
    private final RestaurantDBUtils restDB;
    private static final Icon ICO_OCU_SM = new ImageIcon(Place.class.getResource("/com/mx/kylgis/pos/images/edit_group_sm.png"));
    private static final Icon ICO_WAITER = new NullIcon(1, 1);  
    private static final Icon ICO_FRE = new NullIcon(22, 22);
    private String waiterDetails;
    private String customerDetails;
    private String tableName;
    private Boolean transBtns;
    private Boolean actionEnabled = true;    
    private int newX;
    private int newY;    
    private AppView m_app;
    private Boolean showLayout = false;
    private Timer autoRefreshTimer;
    private int autoRefreshBaseDelayMs = MIN_AUTO_REFRESH_SECONDS * 1000;
    private Map<String, RestaurantDBUtils.TableState> lastAutoRefreshSnapshot;
    private final String sharedTicketLockOwner;
    private boolean staleLocksRecovered;
    
        
    private static String createSharedTicketLockOwner(AppView app) {
        String nodeId = app.getProperties().getProperty("node.id");
        String hostname = app.getProperties().getProperty("machine.hostname");
        String instanceId = app.getProperties().getProperty("machine.instanceid");
        StringBuilder identity = new StringBuilder();
        appendIdentityPart(identity, nodeId);
        appendIdentityPart(identity, hostname);
        appendIdentityPart(identity, instanceId);
        if (identity.length() == 0) {
            identity.append("local");
        }
        String uuid = UUID.nameUUIDFromBytes(identity.toString().getBytes(StandardCharsets.UTF_8))
                .toString().replace("-", "");
        return "K" + uuid.substring(0, 19);
    }

    private static void appendIdentityPart(StringBuilder identity, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (identity.length() > 0) {
            identity.append('|');
        }
        identity.append(value.trim());
    }

    private void recoverOwnStaleLocksOnce() {
        if (staleLocksRecovered) {
            return;
        }
        staleLocksRecovered = true;
        try {
            int released = dlReceipts.releaseSharedTicketLocksOwnedBy(sharedTicketLockOwner);
            if (released > 0) {
                LOGGER.log(Level.INFO,
                        "Recovered {0} stale restaurant table lock(s) for this POS instance",
                        released);
            }
        } catch (BasicException ex) {
            staleLocksRecovered = false;
            LOGGER.log(Level.WARNING,
                    "Could not recover stale restaurant table locks for this POS instance", ex);
        }
    }

    private boolean ownsSharedTicketLock(String tableId) throws BasicException {
        return sharedTicketLockOwner.equals(dlReceipts.getLockState(tableId, null));
    }

    private void showForeignTableLock() {
        JOptionPane.showMessageDialog(this,
                AppLocal.getIntString("message.restaurant.locked.otherterminal"),
                AppLocal.getIntString("title.editor"),
                JOptionPane.WARNING_MESSAGE);
    }

    private void recoverMissingActiveTable() {
        recoverActiveTableConflict("message.restaurant.table.removedremote");
    }

    private void recoverLostActiveTableLock() {
        recoverActiveTableConflict("message.restaurant.lock.lost");
    }

    private void recoverActiveTableConflict(String messageKey) {
        Map<String, RestaurantDBUtils.TableState> tableStates = restDB.getPlacesStateSnapshot();
        recoverActiveTableConflict(messageKey, tableStates);
    }

    private void recoverActiveTableConflict(String messageKey,
            Map<String, RestaurantDBUtils.TableState> tableStates) {
        if (m_PlaceCurrent != null) {
            m_PlaceCurrent.setPeople(false);
        }
        m_PlaceCurrent = null;
        m_PlaceClipboard = null;
        customer = null;
        m_panelticket.setActiveTicket(null, null);
        if (tableStates != null) {
            applyTicketPresence(tableStates);
            printState(tableStates);
            lastAutoRefreshSnapshot = new HashMap<>(tableStates);
        }
        showView("map");
        JOptionPane.showMessageDialog(this,
                AppLocal.getIntString(messageKey),
                AppLocal.getIntString("title.editor"),
                JOptionPane.WARNING_MESSAGE);
    }

    private boolean ensureActiveTableOwned() {
        if (m_PlaceCurrent == null) {
            return false;
        }
        try {
            if (dlReceipts.getSharedTicket(m_PlaceCurrent.getId()) == null) {
                recoverMissingActiveTable();
                return false;
            }
            if (!ownsSharedTicketLock(m_PlaceCurrent.getId())) {
                recoverLostActiveTableLock();
                return false;
            }
            return true;
        } catch (BasicException ex) {
            new MessageInf(ex).show(this);
            return false;
        }
    }

    private boolean acquireSharedTicketLock(Place place, boolean allowOverride) {
        try {
            if (dlReceipts.tryLockSharedTicket(place.getId(), sharedTicketLockOwner)) {
                return true;
            }
            if (dlReceipts.getSharedTicket(place.getId()) == null) {
                place.setPeople(false);
                return false;
            }
            showForeignTableLock();
            if (allowOverride
                    && m_App.getAppUserView().getUser().hasPermission("sales.Override")) {
                int result = JOptionPane.showConfirmDialog(this,
                        AppLocal.getIntString("message.restaurant.lock.override"),
                        AppLocal.getIntString("title.editor"),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.YES_OPTION) {
                    dlReceipts.lockSharedTicket(place.getId(), sharedTicketLockOwner);
                    return true;
                }
            }
        } catch (BasicException ex) {
            new MessageInf(ex).show(this);
        }
        return false;
    }

    /** Creates new form JTicketsBagRestaurant
     * @param app
     * @param panelticket */

    public JTicketsBagRestaurantMap(AppView app, TicketsEditor panelticket) {
        
        super(app, panelticket);
      
        restDB = new  RestaurantDBUtils(app);        
        sharedTicketLockOwner = createSharedTicketLockOwner(app);
        transBtns = AppConfig.getInstance().getBoolean("table.transbtn");
        
        dlReceipts = (DataLogicReceipts) app.getBean("com.mx.kylgis.pos.sales.DataLogicReceipts");
        dlSales = (DataLogicSales) m_App.getBean("com.mx.kylgis.pos.forms.DataLogicSales");
        dlSystem = (DataLogicSystem) m_App.getBean("com.mx.kylgis.pos.forms.DataLogicSystem");        
        
        m_restaurantmap = new JTicketsBagRestaurant(app, this);
        m_PlaceCurrent = null;
        m_PlaceClipboard = null;
        customer = null;
            
        try {
            SentenceList sent = new StaticSentence(
                app.getSession(), 
                "SELECT ID, NAME, IMAGE FROM floors ORDER BY NAME", 
                null, 
                new SerializerReadClass(Floor.class));
            m_afloors = sent.list();
               
        } catch (BasicException eD) {
            m_afloors = new ArrayList<>();
        }
        try {
            SentenceList sent = new StaticSentence(
                app.getSession(), 
                "SELECT ID, NAME, SEATS, X, Y, FLOOR, CUSTOMER, WAITER, " 
                    + "TICKETID, TABLEMOVED, WIDTH, HEIGHT, GUESTS, OCCUPIED "
                    + "FROM places "
                    + "ORDER BY FLOOR ",
                null, 
                new SerializerReadClass(Place.class));
            m_aplaces = sent.list();
        } catch (BasicException eD) {
            m_aplaces = new ArrayList<>();
        } 
        
        initComponents(); 
        
        m_jbtnSave.setVisible(false);
        
        if (m_afloors.size() > 1) {
            JTabbedPane jTabFloors = new JTabbedPane();
            jTabFloors.applyComponentOrientation(getComponentOrientation());
            jTabFloors.setBorder(new javax.swing.border.EmptyBorder(new Insets(5, 5, 5, 5)));
            jTabFloors.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            jTabFloors.setFocusable(false);
            jTabFloors.setRequestFocusEnabled(false);
            m_jPanelMap.add(jTabFloors, BorderLayout.CENTER);
            
            m_afloors.stream().map((f) -> {
                f.getContainer().applyComponentOrientation(getComponentOrientation());
                return f;                
            }).forEach((f) -> {
                JScrollPane jScrCont = new JScrollPane();
                jScrCont.applyComponentOrientation(getComponentOrientation());
                JPanel jPanCont = new JPanel();  
                jPanCont.applyComponentOrientation(getComponentOrientation());
                
                jTabFloors.addTab(f.getName(), f.getIcon(), jScrCont);     
                jScrCont.setViewportView(jPanCont);
                jPanCont.add(f.getContainer());
            });
        } else if (m_afloors.size() == 1) {
            Floor f = m_afloors.get(0);
            f.getContainer().applyComponentOrientation(getComponentOrientation());
            
            JPanel jPlaces = new JPanel();
            jPlaces.applyComponentOrientation(getComponentOrientation());
            jPlaces.setLayout(new BorderLayout());
            jPlaces.setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.EmptyBorder(new Insets(5, 5, 5, 5)),
                new javax.swing.border.TitledBorder(f.getName())));
            
            JScrollPane jScrCont = new JScrollPane();
            jScrCont.applyComponentOrientation(getComponentOrientation());
            JPanel jPanCont = new JPanel();
            jPanCont.applyComponentOrientation(getComponentOrientation());
            
            m_jPanelMap.add(jPlaces, BorderLayout.CENTER);
            jPlaces.add(jScrCont, BorderLayout.CENTER);
            jScrCont.setViewportView(jPanCont);            
            jPanCont.add(f.getContainer());
        }   
        
        Floor currfloor = null;
        
        for (Place pl : m_aplaces) {
            int iFloor = 0;
            
            if (currfloor == null || !currfloor.getID().equals(pl.getFloor())) {
                do {
                    currfloor = m_afloors.get(iFloor++);
                } while (!currfloor.getID().equals(pl.getFloor()));
            }

            currfloor.getContainer().add(pl.getButton());
            pl.setButtonBounds();

            if (transBtns) {
                pl.getButton().setOpaque(false);
                pl.getButton().setContentAreaFilled(false);
                pl.getButton().setBorderPainted(false);
            }
            
            pl.getButton().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent E) {
                if (!actionEnabled) {
                    if (pl.getDiffX() == 0) {
                        pl.setDiffX(pl.getButton().getX() - pl.getX());
                        pl.setDiffY(pl.getButton().getY() - pl.getY());
                    }
                    newX = E.getX() + pl.getButton().getX();
                    newY = E.getY() + pl.getButton().getY();
                    pl.getButton().setBounds(newX + pl.getDiffX(), newY + pl.getDiffY(),
                        pl.getButton().getWidth(), pl.getButton().getHeight());
                        pl.setX(newX);
                        pl.setY(newY);
                    }
                }
            }
            );

            pl.getButton().addActionListener(new MyActionListener(pl));
        }
        
        m_jreservations = new JTicketsBagRestaurantRes(app, this);
        add(m_jreservations, "res");

        showLayout = m_App.getAppUserView().getUser().hasPermission("sales.Layout");
        if (showLayout) {
            m_jbtnLayout.setVisible(true);
            m_jbtnSave.setVisible(false);            
        } else {
            m_jbtnLayout.setVisible(false);
            m_jbtnSave.setVisible(false);            
        }        
        
        if ("true".equalsIgnoreCase(m_App.getProperties().getProperty("till.autoRefreshTableMap"))) {
            webLblautoRefresh.setText(
                    AppLocal.getIntString("label.autoRefreshTableMapTimerON"));

            int refreshSeconds = MIN_AUTO_REFRESH_SECONDS;
            try {
                refreshSeconds = Math.max(MIN_AUTO_REFRESH_SECONDS, Integer.parseInt(
                        m_App.getProperties().getProperty("till.autoRefreshTimer")));
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING,
                        "Invalid till.autoRefreshTimer; using {0} seconds", refreshSeconds);
            }
            autoRefreshBaseDelayMs = refreshSeconds * 1000;
            autoRefreshTimer = new Timer(autoRefreshBaseDelayMs, new tableMapRefresh());
            autoRefreshTimer.setCoalesce(true);
        } else {
            webLblautoRefresh.setText(
                    AppLocal.getIntString("label.autoRefreshTableMapTimerOFF"));
        }

}

    class tableMapRefresh implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateAutoRefreshDelay(refreshRestaurantState());
        }
    }

    private void updateAutoRefreshDelay(RefreshOutcome outcome) {
        if (autoRefreshTimer == null) {
            return;
        }

        if (outcome == RefreshOutcome.CHANGED) {
            autoRefreshTimer.setDelay(autoRefreshBaseDelayMs);
            return;
        }

        int current = Math.max(autoRefreshBaseDelayMs, autoRefreshTimer.getDelay());
        int maxDelay = outcome == RefreshOutcome.FAILED
                ? Math.max(MAX_ERROR_AUTO_REFRESH_DELAY_MS, autoRefreshBaseDelayMs)
                : Math.max(MAX_IDLE_AUTO_REFRESH_DELAY_MS, autoRefreshBaseDelayMs);
        long doubled = (long) current * 2L;
        autoRefreshTimer.setDelay((int) Math.min((long) maxDelay, doubled));
    }

    /**
     *
     */
    @Override
    public void activate() {

        showLayout = m_App.getAppUserView().getUser().hasPermission("sales.Layout");
        if (showLayout) {
            m_jbtnLayout.setVisible(true);
            m_jbtnSave.setVisible(false);
        } else {
            m_jbtnLayout.setVisible(false);
            m_jbtnSave.setVisible(false);            
        } 
        
        m_PlaceClipboard = null;
        customer = null;
        recoverOwnStaleLocksOnce();
        refreshRestaurantState();
        if (autoRefreshTimer != null && !autoRefreshTimer.isRunning()) {
            autoRefreshTimer.setDelay(autoRefreshBaseDelayMs);
            autoRefreshTimer.start();
        }

        m_panelticket.setActiveTicket(null, null); 
        m_restaurantmap.activate();
       
        showView("map");      
    }
    
    /**
     *
     * @return
     */
    @Override
    public boolean deactivate() {

        if (viewTables()) {
            m_PlaceClipboard = null;
            customer = null;

            if (m_PlaceCurrent != null) {
                try {
                    TicketInfo activeTicket = m_panelticket.getActiveTicket();
                    if (activeTicket != null && ownsSharedTicketLock(m_PlaceCurrent.getId())) {
                        dlReceipts.updateSharedTicketIfOwned(m_PlaceCurrent.getId(),
                                activeTicket, activeTicket.getPickupId(), sharedTicketLockOwner);
                        dlReceipts.unlockSharedTicketIfOwned(m_PlaceCurrent.getId(), sharedTicketLockOwner);
                    }
                } catch (BasicException e) {
                    new MessageInf(e).show(this);
                }
                m_PlaceCurrent = null;
            }
            printState();
            m_panelticket.setActiveTicket(null, null);
            if (autoRefreshTimer != null) {
                autoRefreshTimer.stop();
            }
            return true;
        }
        return false;
    }

    /**
     *
     * @return
     */
    @Override
    protected JComponent getBagComponent() {
        return m_restaurantmap;
    }

    /**
     *
     * @return
     */
    @Override
    protected JComponent getNullComponent() {
        return this;
    }

    /**
     *
     * @return
     */
    public TicketInfo getActiveTicket() {
        return m_panelticket.getActiveTicket();
    }

    public boolean validateActiveTableOwnership() {
        return ensureActiveTableOwned();
    }

    /**
     *
     */
    public void moveTicket() {
        if (m_PlaceCurrent == null) {
            return;
        }
        if (!ensureActiveTableOwned()) {
            return;
        }

        TicketInfo activeTicket = m_panelticket.getActiveTicket();
        if (activeTicket == null) {
            recoverMissingActiveTable();
            return;
        }
        try {
            if (!dlReceipts.updateRSharedTicketIfOwned(m_PlaceCurrent.getId(),
                    activeTicket, activeTicket.getPickupId(), sharedTicketLockOwner)) {
                recoverLostActiveTableLock();
                return;
            }
        } catch (BasicException e) {
            new MessageInf(e).show(this);
            return;
        }

        m_PlaceClipboard = m_PlaceCurrent;
        customer = null;
        printState();
        m_panelticket.setActiveTicket(null, null);
    }

    /**
     *
     * @param c
     * @return
     */
    public boolean viewTables(CustomerInfo c) {
        if (m_jreservations.deactivate()) {
            showView("map");
            m_PlaceClipboard = null;    
            customer = c;     
            printState();
            return true;
        } else {
            return false;
        }        
    }
    
    /**
     *
     * @return
     */
    public boolean viewTables() {
        return viewTables(null);
    }
        
    /**
     *
     */
    public void newTicket() {
        if (m_PlaceCurrent != null) {
            if (!ensureActiveTableOwned()) {
                return;
            }
            TicketInfo activeTicket = m_panelticket.getActiveTicket();
            try {
                if (activeTicket == null
                        || !dlReceipts.updateSharedTicketIfOwned(m_PlaceCurrent.getId(),
                                activeTicket, activeTicket.getPickupId(), sharedTicketLockOwner)) {
                    recoverLostActiveTableLock();
                    return;
                }
                dlReceipts.unlockSharedTicketIfOwned(m_PlaceCurrent.getId(), sharedTicketLockOwner);
                m_PlaceCurrent = null;
            } catch (BasicException ex) {
                new MessageInf(ex).show(this);
                return;
            }
        }

        printState();
        m_panelticket.setActiveTicket(null, null);
    }

    /**
     *
     * @return
     */
    public String getTable() {
        String id = null;
        if (m_PlaceCurrent != null) {
            id = m_PlaceCurrent.getId();
    }
        return(id);
    }
    
    /**
     *
     * @return
     */
    public String getTableName() {
        String stableName = null;
        if (m_PlaceCurrent != null) {
            stableName = m_PlaceCurrent.getName();
    }
        return(stableName);
    }

    /**
     *
     */
    @Override
    public void deleteTicket() {
        if (m_PlaceCurrent == null) {
            return;
        }
        if (!ensureActiveTableOwned()) {
            return;
        }

        String id = m_PlaceCurrent.getId();
        boolean transactionStarted = false;
        try {
            beginRestaurantMoveTransaction();
            transactionStarted = true;
            if (!dlReceipts.deleteSharedTicketIfOwned(id, sharedTicketLockOwner)) {
                throw new BasicException("Restaurant table lock was lost before delete");
            }
            if (!restDB.clearTableState(id)) {
                throw new BasicException("Could not clear restaurant table metadata");
            }
            commitRestaurantMoveTransaction();
            transactionStarted = false;

            dlSystem.execTicketRemoved(new Object[] {
                m_App.getAppUserView().getUser().getName(),
                "Void",
                "Ticket Deleted",
                0.0
            });
        } catch (BasicException e) {
            if (transactionStarted) {
                rollbackRestaurantMoveTransaction();
            }
            if (!ensureActiveTableOwned()) {
                return;
            }
            new MessageInf(e).show(this);
            refreshRestaurantState();
            return;
        }

        m_PlaceCurrent.setPeople(false);
        m_PlaceCurrent = null;
        printState();
        m_panelticket.setActiveTicket(null, null);
    }

    /**
     *
     */
    public void changeServer() {

        if (m_ServerCurrent != null) {
        }
    }
    
    /**
     *
     */
    public boolean loadTickets() {
        Map<String, RestaurantDBUtils.TableState> tableStates = restDB.getPlacesStateSnapshot();
        if (tableStates == null) {
            LOGGER.warning("Unable to refresh restaurant table state; keeping previous UI state");
            return false;
        }
        applyTicketPresence(tableStates);
        return true;
    }

    private RefreshOutcome refreshRestaurantState() {
        Map<String, RestaurantDBUtils.TableState> tableStates = restDB.getPlacesStateSnapshot();
        if (tableStates == null) {
            LOGGER.warning("Unable to refresh restaurant table state; keeping previous UI state");
            return RefreshOutcome.FAILED;
        }

        boolean changed = lastAutoRefreshSnapshot == null
                || !lastAutoRefreshSnapshot.equals(tableStates);

        if (m_PlaceCurrent != null) {
            RestaurantDBUtils.TableState activeState = tableStates.get(m_PlaceCurrent.getId());
            if (activeState == null || !activeState.hasTicket()) {
                recoverActiveTableConflict("message.restaurant.table.removedremote", tableStates);
                return RefreshOutcome.CHANGED;
            }
            if (!sharedTicketLockOwner.equals(activeState.getLockOwner())) {
                recoverActiveTableConflict("message.restaurant.lock.lost", tableStates);
                return RefreshOutcome.CHANGED;
            }
        }

        applyTicketPresence(tableStates);
        if (!printState(tableStates)) {
            return RefreshOutcome.FAILED;
        }
        lastAutoRefreshSnapshot = new HashMap<>(tableStates);
        return changed ? RefreshOutcome.CHANGED : RefreshOutcome.UNCHANGED;
    }

    private void applyTicketPresence(Map<String, RestaurantDBUtils.TableState> tableStates) {
        for (Place table : m_aplaces) {
            RestaurantDBUtils.TableState state = tableStates.get(table.getId());
            table.setPeople(state != null && state.hasTicket());
        }
    }

/*
 *  Populate the floor plans and tables
 */
    private boolean printState() {
        Map<String, RestaurantDBUtils.TableState> tableStates = restDB.getPlacesStateSnapshot();
        if (tableStates == null) {
            LOGGER.warning("Unable to refresh restaurant table state; keeping previous UI state");
            return false;
        }
        applyTicketPresence(tableStates);
        boolean rendered = printState(tableStates);
        if (rendered) {
            lastAutoRefreshSnapshot = new HashMap<>(tableStates);
        }
        return rendered;
    }

    private boolean printState(Map<String, RestaurantDBUtils.TableState> tableStates) {
        if (m_PlaceClipboard == null) {
            if (customer == null) {
                m_jText.setText(null);

                m_aplaces.stream()
                        .map((place) -> {
                            place.getButton().setEnabled(true);
                            RestaurantDBUtils.TableState state = tableStates.get(place.getId());
                            if (state != null) {
                                place.setGuests(state.getGuests());
                                place.setOccupied(state.getOccupied());
                            }
                            return place;
                        })

                        .map((place) -> {
                            if (m_App.getProperties().getProperty("table.tablecolour")== null){
                                tableName="<style=font-size:9px;font-weight:bold;><font color = black>"
                                + place.getName()+"</font></style>";  
                            }else{
                                if (place.getOccupied() != null) {
                                    Date date = new java.util.Date();
                                    Timestamp t1 = new Timestamp(date.getTime());
                                    Timestamp t2 = new Timestamp(place.getOccupied().getTime());    
 
                                    long milliseconds = t1.getTime() - t2.getTime();
                                    int seconds = (int) milliseconds / 1000;
                                    int hours = seconds / 3600;
                                    int minutes = (seconds % 3600) / 60;

Integer count = place.getGuests();
        
                                    tableName="<style=font-size:9px;font-weight:bold;><font color ="
                                        + m_App.getProperties().getProperty("table.tablecolour")+ ">"
                                        + place.getName() + " / " + place.getSeats() + "<br>"
//                                        + AppLocal.getIntString("button.guest") + place.getGuests() + "<br>"
                                        + AppLocal.getIntString("button.guest") + count + "<br>"                                            
                                        + AppLocal.getIntString("button.occupied") 
                                            + hours + "h" + ":" + minutes + "m" 
                                            + "<br>"
                                        + "</font></style>";      
                                } else {
                                    tableName="<style=font-size:9px;font-weight:bold;><font color ="
                                    + m_App.getProperties().getProperty("table.tablecolour")+ ">"
                                    + place.getName() + " / " + place.getSeats() + "<br>"
                                    + "</font></style>";
                                }
                            }
                            return place;            
                        })

                        .map((place) -> {
                            if (Boolean.parseBoolean(m_App.getProperties().getProperty("table.showwaiterdetails"))){
                                RestaurantDBUtils.TableState state = tableStates.get(place.getId());
                                String waiter = state == null ? place.getWaiter() : state.getWaiter();
                                if (m_App.getProperties().getProperty("table.waitercolour")== null){
                                    waiterDetails = (waiter == null || waiter.isEmpty()) ? ""
                                    :"<style=font-size:9px;font-weight:bold;><font color = red>"
                                    + waiter + "</font></style><br>";
                                }else{
                                    waiterDetails = (waiter == null || waiter.isEmpty()) ? ""
                                    :"<style=font-size:9px;font-weight:bold;><font color ="
                                    + m_App.getProperties().getProperty("table.waitercolour")+ ">"
                                    + waiter + "</font></style><br>";
                                }
                                place.getButton().setIcon(ICO_OCU_SM);
                            } else {
                                waiterDetails = ""; 
                            }
                            return place;           
                        })
                        
                        .map((place) -> {
                            if (Boolean.parseBoolean(
                                    m_App.getProperties().getProperty("table.showcustomerdetails"))){
                                RestaurantDBUtils.TableState state = tableStates.get(place.getId());
                                String tableCustomer = state == null ? place.getCustomer() : state.getCustomer();
                                place.getButton().setIcon((Boolean.parseBoolean(
                                        m_App.getProperties().getProperty("table.showwaiterdetails"))
                                        && tableCustomer != null && !tableCustomer.isEmpty())
                                        ? ICO_WAITER:ICO_OCU_SM);
                                if (m_App.getProperties().getProperty("table.customercolour")== null){
                                    customerDetails = (tableCustomer == null || tableCustomer.isEmpty()) ? ""
                                    :"<style=font-size:9px;font-weight:bold;><font color = blue>"
                                    + tableCustomer + "</font></style><br>";
                                }else{
                                    customerDetails = (tableCustomer == null || tableCustomer.isEmpty()) ? ""
                                            :"<style=font-size:9px;font-weight:bold;><font color ="
                                        + m_App.getProperties().getProperty("table.customercolour")+ ">"
                                        + tableCustomer + "</font></style><br>";
                                }
                            } else {
                                customerDetails = ""; 
                            }
                            return place;
                        })
                        
                        .map((place) -> { 
                            if ((Boolean.parseBoolean(
                                    m_App.getProperties().getProperty("table.showwaiterdetails")))
                                    || (Boolean.parseBoolean(
                                            m_App.getProperties().getProperty("table.showcustomerdetails")))) {
                                place.getButton().setText("<html><center>"
                                + customerDetails + waiterDetails  +tableName+"</html>" );
                            }else{
                                if (m_App.getProperties().getProperty("table.tablecolour")== null){
                                    tableName="<style=font-size:10px;font-weight:bold;><font color = black>"
                                    + place.getName()+"</font></style>";  
                                }else{
                                    tableName="<style=font-size:10px;font-weight:bold;><font color ="
                                    + m_App.getProperties().getProperty("table.tablecolour")+ ">"
                                    + place.getName()+"</font></style>";     
                                }
                                place.getButton().setText("<html><center>"+tableName+"</html>");
                            }
                            return place;
                        })
                        
                        .filter((place) -> (!place.hasPeople()))

                        .forEach((place) -> {
                            place.getButton().setIcon(ICO_FRE);
                        });

                        m_jbtnReservations.setEnabled(true);
// places here

            } else {
                m_jText.setText(AppLocal.getIntString("label.restaurantcustomer"
                        , new Object[] {
                            customer.getName()
                        }
                )
                );

                m_aplaces.stream()
                        .forEach((place) -> {
                            place.getButton().setEnabled(!place.hasPeople());
                        });
                m_jbtnReservations.setEnabled(false);
            }
        } else {
            m_jText.setText(AppLocal.getIntString("label.restaurantmove"
                    , new Object[] {
                    m_PlaceClipboard.getName()
                }
            ));

            m_aplaces.stream()
                .forEach((place) -> {
                    RestaurantDBUtils.TableState state = tableStates.get(place.getId());
                    if (state != null) {
                        place.setGuests(state.getGuests());
                        place.setOccupied(state.getOccupied());
                    }
                    place.getButton().setEnabled(true);
                });

            m_jbtnReservations.setEnabled(false);
        }
        return true;
    }

    private TicketInfo getTicketInfo(Place place) {

        try {
            return dlReceipts.getSharedTicket(place.getId());
        } catch (BasicException e) {
            new MessageInf(e).show(JTicketsBagRestaurantMap.this);
            return null;
        }
    }
    
    private void beginRestaurantMoveTransaction() throws BasicException {
        try {
            if (m_App.getSession().isTransaction()) {
                throw new BasicException("A database transaction is already active during table move");
            }
            m_App.getSession().begin();
        } catch (SQLException ex) {
            throw new BasicException("Could not start restaurant table move transaction", ex);
        }
    }

    private void commitRestaurantMoveTransaction() throws BasicException {
        try {
            m_App.getSession().commit();
        } catch (SQLException ex) {
            throw new BasicException("Could not commit restaurant table move transaction", ex);
        }
    }

    private void rollbackRestaurantMoveTransaction() {
        if (!m_App.getSession().isTransaction()) {
            return;
        }
        try {
            m_App.getSession().rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.WARNING, "Could not roll back restaurant table move transaction", ex);
        }
    }

    private void setActivePlace(Place place, TicketInfo ticket) {
        m_PlaceCurrent = place;
        m_panelticket.setActiveTicket(ticket, m_PlaceCurrent.getName());
        m_restaurantmap.updateGuestCount();
    }

    private void showView(String view) {
        CardLayout cl = (CardLayout)(getLayout());
        cl.show(this, view);  
    }

    private class MyActionListener implements ActionListener {

        private final Place m_place;

        MyActionListener(Place place) {
            m_place = place;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            m_App.getAppUserView().getUser();

            if (!actionEnabled) {
                m_place.setDiffX(0);
                return;
            }

            if (m_PlaceClipboard == null) {
                TicketInfo ticket = getTicketInfo(m_place);
                if (ticket == null) {
                    ticket = new TicketInfo();
                    ticket.setUser(m_App.getAppUserView().getUser().getUserInfo());
                    try {
                        dlReceipts.insertSharedTicketLocked(m_place.getId(), ticket,
                                ticket.getPickupId(), sharedTicketLockOwner);
                    } catch (BasicException e) {
                        new MessageInf(e).show(JTicketsBagRestaurantMap.this);
                        refreshRestaurantState();
                        return;
                    }
                    m_place.setPeople(true);
                    m_place.setGuests(restDB.updateGuestsInTable(m_place.getId()));
                    setActivePlace(m_place, ticket);
                    return;
                }

                try {
                    String currentUser = m_App.getAppUserView().getUser().getId();
                    String ticketUser = dlReceipts.getServer(m_place.getId(), currentUser);
                    if (!m_App.getAppUserView().getUser().hasPermission("sales.Override")
                            && !currentUser.equals(ticketUser)) {
                        JOptionPane.showMessageDialog(JTicketsBagRestaurantMap.this,
                                AppLocal.getIntString("message.sharedticket"),
                                AppLocal.getIntString("title.editor"),
                                JOptionPane.OK_OPTION);
                        return;
                    }
                } catch (BasicException ex) {
                    new MessageInf(ex).show(JTicketsBagRestaurantMap.this);
                    return;
                }

                if (!acquireSharedTicketLock(m_place, true)) {
                    refreshRestaurantState();
                    return;
                }
                m_place.setPeople(true);
                m_PlaceClipboard = null;
                setActivePlace(m_place, ticket);
                return;
            }

            // Move / merge. The source remains locked by this POS while the
            // operator chooses a destination.
            Place sourcePlace = m_PlaceClipboard;
            TicketInfo ticketclip = getTicketInfo(sourcePlace);
            if (ticketclip == null) {
                recoverMissingActiveTable();
                return;
            }
            try {
                if (!ownsSharedTicketLock(sourcePlace.getId())) {
                    recoverLostActiveTableLock();
                    return;
                }
            } catch (BasicException ex) {
                new MessageInf(ex).show(JTicketsBagRestaurantMap.this);
                return;
            }

            if (sourcePlace == m_place) {
                m_PlaceClipboard = null;
                customer = null;
                printState();
                setActivePlace(sourcePlace, ticketclip);
                return;
            }

            // Never trust only the cached hasPeople flag. Another host may have
            // changed the destination since the last map refresh.
            TicketInfo destinationTicket = getTicketInfo(m_place);
            if (destinationTicket != null) {
                try {
                    String currentUser = m_App.getAppUserView().getUser().getId();
                    String destinationUser = dlReceipts.getServer(m_place.getId(), currentUser);
                    if (!m_App.getAppUserView().getUser().hasPermission("sales.Override")
                            && !currentUser.equals(destinationUser)) {
                        JOptionPane.showMessageDialog(JTicketsBagRestaurantMap.this,
                                AppLocal.getIntString("message.sharedticket"),
                                AppLocal.getIntString("title.editor"),
                                JOptionPane.OK_OPTION);
                        return;
                    }
                } catch (BasicException ex) {
                    new MessageInf(ex).show(JTicketsBagRestaurantMap.this);
                    return;
                }
                if (!acquireSharedTicketLock(m_place, false)) {
                    // Keep move mode active so another destination can be chosen.
                    refreshRestaurantState();
                    return;
                }

                boolean destinationLockHeld = true;
                if (JOptionPane.showConfirmDialog(JTicketsBagRestaurantMap.this,
                        AppLocal.getIntString("message.mergetablequestion"),
                        AppLocal.getIntString("message.mergetable"),
                        JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    try {
                        dlReceipts.unlockSharedTicketIfOwned(m_place.getId(), sharedTicketLockOwner);
                    } catch (BasicException ex) {
                        new MessageInf(ex).show(JTicketsBagRestaurantMap.this);
                    }
                    m_PlaceClipboard = null;
                    customer = null;
                    printState();
                    setActivePlace(sourcePlace, ticketclip);
                    return;
                }

                boolean transactionStarted = false;
                boolean mergeSucceeded = false;
                int mergedGuests = Math.max(0, m_place.getGuests())
                        + Math.max(0, sourcePlace.getGuests());
                try {
                    beginRestaurantMoveTransaction();
                    transactionStarted = true;
                    if (!ownsSharedTicketLock(sourcePlace.getId())
                            || !ownsSharedTicketLock(m_place.getId())) {
                        throw new BasicException("Restaurant table lock changed during merge");
                    }
                    if (destinationTicket.getCustomer() == null) {
                        destinationTicket.setCustomer(ticketclip.getCustomer());
                    }
                    for (com.mx.kylgis.pos.ticket.TicketLineInfo line : ticketclip.getLines()) {
                        destinationTicket.addLine(line);
                    }
                    if (!dlReceipts.updateRSharedTicketIfOwned(m_place.getId(),
                            destinationTicket, destinationTicket.getPickupId(), sharedTicketLockOwner)) {
                        throw new BasicException("Destination restaurant table lock was lost");
                    }
                    if (!dlReceipts.deleteSharedTicketIfOwned(sourcePlace.getId(), sharedTicketLockOwner)) {
                        throw new BasicException("Source restaurant table lock was lost");
                    }
                    if (!restDB.mergeTableState(sourcePlace.getId(), m_place.getId())) {
                        throw new BasicException("Could not merge restaurant table metadata");
                    }
                    commitRestaurantMoveTransaction();
                    transactionStarted = false;
                    destinationLockHeld = false; // keep lock: destination becomes active
                    m_place.setGuests(mergedGuests);
                    sourcePlace.setPeople(false);
                    mergeSucceeded = true;
                } catch (BasicException e) {
                    if (transactionStarted) {
                        rollbackRestaurantMoveTransaction();
                    }
                    new MessageInf(e).show(JTicketsBagRestaurantMap.this);
                } finally {
                    if (!mergeSucceeded && destinationLockHeld) {
                        try {
                            dlReceipts.unlockSharedTicketIfOwned(m_place.getId(), sharedTicketLockOwner);
                        } catch (BasicException ex) {
                            LOGGER.log(Level.WARNING, "Unable to release destination table lock", ex);
                        }
                    }
                }

                if (mergeSucceeded) {
                    m_PlaceClipboard = null;
                    customer = null;
                    printState();
                    setActivePlace(m_place, destinationTicket);
                } else {
                    sourcePlace.setPeople(true);
                    refreshRestaurantState();
                }
                return;
            }

            // Destination is empty. Insert and acquire its lock inside the same
            // DB transaction before deleting the source.
            boolean transactionStarted = false;
            boolean moveSucceeded = false;
            try {
                beginRestaurantMoveTransaction();
                transactionStarted = true;
                if (!ownsSharedTicketLock(sourcePlace.getId())) {
                    throw new BasicException("Source restaurant table lock was lost");
                }
                dlReceipts.insertRSharedTicket(m_place.getId(), ticketclip, ticketclip.getPickupId());
                if (!dlReceipts.tryLockSharedTicket(m_place.getId(), sharedTicketLockOwner)) {
                    throw new BasicException("Destination restaurant table could not be locked");
                }
                if (!dlReceipts.deleteSharedTicketIfOwned(sourcePlace.getId(), sharedTicketLockOwner)) {
                    throw new BasicException("Source restaurant table lock was lost");
                }
                if (!restDB.moveTableState(sourcePlace.getId(), m_place.getId())) {
                    throw new BasicException("Could not transfer restaurant table metadata");
                }
                commitRestaurantMoveTransaction();
                transactionStarted = false;
                m_place.setPeople(true);
                m_place.setGuests(sourcePlace.getGuests());
                sourcePlace.setPeople(false);
                moveSucceeded = true;
            } catch (BasicException e) {
                if (transactionStarted) {
                    rollbackRestaurantMoveTransaction();
                }
                new MessageInf(e).show(JTicketsBagRestaurantMap.this);
            }

            if (moveSucceeded) {
                printState();
                setActivePlace(m_place, ticketclip);
                m_PlaceClipboard = null;
                customer = null;
            } else {
                sourcePlace.setPeople(true);
                refreshRestaurantState();
            }
        }
    }

    /**
     *
     * @param btnText
     */
    public void setButtonTextBags(String btnText){
      m_PlaceClipboard.setButtonText(btnText);
  } 

    
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        m_jPanelMap = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        m_jbtnReservations = new javax.swing.JButton();
        m_jbtnRefresh = new javax.swing.JButton();
        m_jbtnLayout = new javax.swing.JButton();
        m_jbtnSave = new javax.swing.JButton();
        m_jText = new javax.swing.JLabel();
        webLblautoRefresh = new com.alee.laf.label.WebLabel();

        setLayout(new java.awt.CardLayout());

        m_jPanelMap.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jPanelMap.setLayout(new java.awt.BorderLayout());

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        m_jbtnReservations.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jbtnReservations.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/date.png"))); // NOI18N
        m_jbtnReservations.setText(AppLocal.getIntString("button.reservations")); // NOI18N
        m_jbtnReservations.setToolTipText("Open Reservations screen");
        m_jbtnReservations.setFocusPainted(false);
        m_jbtnReservations.setFocusable(false);
        m_jbtnReservations.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnReservations.setMaximumSize(new java.awt.Dimension(133, 40));
        m_jbtnReservations.setMinimumSize(new java.awt.Dimension(133, 40));
        m_jbtnReservations.setPreferredSize(new java.awt.Dimension(133, 45));
        m_jbtnReservations.setRequestFocusEnabled(false);
        m_jbtnReservations.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnReservationsActionPerformed(evt);
            }
        });
        jPanel2.add(m_jbtnReservations);

        m_jbtnRefresh.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jbtnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/reload.png"))); // NOI18N
        m_jbtnRefresh.setText(AppLocal.getIntString("button.reloadticket")); // NOI18N
        m_jbtnRefresh.setToolTipText("Reload table information");
        m_jbtnRefresh.setFocusPainted(false);
        m_jbtnRefresh.setFocusable(false);
        m_jbtnRefresh.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnRefresh.setMaximumSize(new java.awt.Dimension(100, 40));
        m_jbtnRefresh.setMinimumSize(new java.awt.Dimension(100, 40));
        m_jbtnRefresh.setPreferredSize(new java.awt.Dimension(100, 45));
        m_jbtnRefresh.setRequestFocusEnabled(false);
        m_jbtnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnRefreshActionPerformed(evt);
            }
        });
        jPanel2.add(m_jbtnRefresh);

        m_jbtnLayout.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jbtnLayout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/movetable.png"))); // NOI18N
        m_jbtnLayout.setText(AppLocal.getIntString("button.layout")); // NOI18N
        m_jbtnLayout.setToolTipText("");
        m_jbtnLayout.setFocusPainted(false);
        m_jbtnLayout.setFocusable(false);
        m_jbtnLayout.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnLayout.setMaximumSize(new java.awt.Dimension(100, 40));
        m_jbtnLayout.setMinimumSize(new java.awt.Dimension(100, 40));
        m_jbtnLayout.setPreferredSize(new java.awt.Dimension(100, 45));
        m_jbtnLayout.setRequestFocusEnabled(false);
        m_jbtnLayout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnLayoutActionPerformed(evt);
            }
        });
        jPanel2.add(m_jbtnLayout);

        m_jbtnSave.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        m_jbtnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/mx/kylgis/pos/images/filesave.png"))); // NOI18N
        m_jbtnSave.setText(AppLocal.getIntString("button.save")); // NOI18N
        m_jbtnSave.setToolTipText("");
        m_jbtnSave.setFocusPainted(false);
        m_jbtnSave.setFocusable(false);
        m_jbtnSave.setMargin(new java.awt.Insets(8, 14, 8, 14));
        m_jbtnSave.setMaximumSize(new java.awt.Dimension(100, 40));
        m_jbtnSave.setMinimumSize(new java.awt.Dimension(100, 40));
        m_jbtnSave.setPreferredSize(new java.awt.Dimension(100, 45));
        m_jbtnSave.setRequestFocusEnabled(false);
        m_jbtnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                m_jbtnSaveActionPerformed(evt);
            }
        });
        jPanel2.add(m_jbtnSave);

        m_jText.setBackground(new java.awt.Color(255, 255, 255));
        m_jText.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        m_jText.setForeground(new java.awt.Color(0, 153, 255));
        m_jText.setOpaque(true);
        jPanel2.add(m_jText);

        jPanel1.add(jPanel2, java.awt.BorderLayout.LINE_START);

        webLblautoRefresh.setBackground(new java.awt.Color(255, 51, 51));
        webLblautoRefresh.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("pos_messages"); // NOI18N
        webLblautoRefresh.setText(bundle.getString("label.autoRefreshTableMapTimerON")); // NOI18N
        webLblautoRefresh.setFont(new java.awt.Font("Arial", 0, 14)); // NOI18N
        jPanel1.add(webLblautoRefresh, java.awt.BorderLayout.CENTER);

        m_jPanelMap.add(jPanel1, java.awt.BorderLayout.NORTH);

        add(m_jPanelMap, "map");
    }// </editor-fold>//GEN-END:initComponents

    private void m_jbtnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnRefreshActionPerformed

        refreshRestaurantState();
    }//GEN-LAST:event_m_jbtnRefreshActionPerformed

    private void m_jbtnReservationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnReservationsActionPerformed
        showView("res");
        m_jreservations.activate();
    }//GEN-LAST:event_m_jbtnReservationsActionPerformed

    private void m_jbtnLayoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnLayoutActionPerformed
        if (java.util.ResourceBundle.getBundle("pos_messages")
                .getString("button.layout").equals(m_jbtnLayout.getText())) {
            actionEnabled = false;
            m_jbtnSave.setVisible(true);
            m_jbtnLayout.setText(java.util.ResourceBundle
                    .getBundle("pos_messages").getString("button.disablelayout"));
            
            m_aplaces.stream().filter((pl) -> (transBtns)).map((pl) -> {
                pl.getButton().setOpaque(true);
                return pl;
            }).map((pl) -> {
                pl.getButton().setContentAreaFilled(true);
                return pl;
            }).forEachOrdered((pl) -> {
                pl.getButton().setBorderPainted(true);
            });
        } else {
            actionEnabled = true;
            m_jbtnSave.setVisible(false);
            m_jbtnLayout.setText(java.util.ResourceBundle
                    .getBundle("pos_messages").getString("button.layout"));

            m_aplaces.stream().filter((pl) -> (transBtns)).map((pl) -> {
                pl.getButton().setOpaque(false);
                return pl;
            }).map((pl) -> {
                pl.getButton().setContentAreaFilled(false);
                return pl;
            }).forEachOrdered((pl) -> {
                pl.getButton().setBorderPainted(false);
            });
        }
    }//GEN-LAST:event_m_jbtnLayoutActionPerformed

    private void m_jbtnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_m_jbtnSaveActionPerformed
        m_aplaces.forEach((pl) -> {
            try {
                dlSystem.updatePlaces(pl.getX(), pl.getY(), pl.getId());
            } catch (BasicException ex) {
                Logger.getLogger(JTicketsBagRestaurantMap.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }//GEN-LAST:event_m_jbtnSaveActionPerformed
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel m_jPanelMap;
    private javax.swing.JLabel m_jText;
    private javax.swing.JButton m_jbtnLayout;
    private javax.swing.JButton m_jbtnRefresh;
    private javax.swing.JButton m_jbtnReservations;
    private javax.swing.JButton m_jbtnSave;
    private com.alee.laf.label.WebLabel webLblautoRefresh;
    // End of variables declaration//GEN-END:variables
    
}
