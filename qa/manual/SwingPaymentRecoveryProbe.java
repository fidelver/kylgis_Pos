import com.mx.kylgis.pos.forms.*;
import com.mx.kylgis.pos.sales.*;
import com.mx.kylgis.pos.sales.shared.*;
import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.*;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import com.mx.kylgis.pos.data.loader.Session;
import java.awt.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.swing.*;

/** Real Swing components and DataLogicReceipts; app services are synthetic.
 * No login, hardware, bank authorization or operator reconciliation is simulated as real. */
public final class SwingPaymentRecoveryProbe {
    static int checks;
    static final String OP="11111111-1111-4111-8111-111111111111";
    static final long WHEN=1700000000000L;
    static JFrame frame;
    static RecoveryPanel panel;
    static JTicketsBagShared bag;
    static DataLogicReceipts receipts;
    static Session session;
    static void ck(boolean ok,String name) {
        if(!ok) throw new AssertionError(name);
        checks++;System.out.println("PASS "+name);
    }
    static Object field(Object obj,Class<?> type,String name)throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(obj);
    }
    static void set(Object obj,Class<?> type,String name,Object value)throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(obj,value);
    }
    static JButton button(Object o,Class<?> c,String name)throws Exception {
        return (JButton)field(o,c,name);
    }
    static final class RecoveryPanel extends JPanelTicket {
        RecoveryPanel(AppView app)throws Exception {
            m_App=app;
            m_ticketlines=new JTicketLines(null);
            m_jbtnconfig=new JPanelButtons("QA.NoEvents",this);
            set(this,JPanelTicket.class,"taxeslogic",new TaxesLogic(List.of()));
            set(this,JPanelTicket.class,"priceWith00",false);
        }
        public String getTitle(){return "Synthetic payment recovery QA";}
        protected JTicketsBag getJTicketsBag(){return bag;}
        protected Component getSouthComponent(){return new JPanel();}
        protected void resetSouthComponent(){}
        public String getResourceAsXML(String key){return null;}
    }
    static AppView app() {
        AppUser user=new AppUser("QA-USER","Synthetic cashier","","","QA-ROLE",null) {
            public boolean hasPermission(String p) {
                return Set.of("sales.Total","sales.ViewSharedTicket","sales.DeleteTicket").contains(p);
            }
        };
        AppUserView uv=new AppUserView() {
            public AppUser getUser(){return user;}
            public void showTask(String s){throw new AssertionError("UNEXPECTED_TASK");}
            public void executeTask(String s){throw new AssertionError("UNEXPECTED_TASK");}
        };
        AppProperties props=(AppProperties)Proxy.newProxyInstance(
            AppProperties.class.getClassLoader(),new Class[]{AppProperties.class},(p,m,a)->{
                if(m.getName().equals("getProperty"))return switch((String)a[0]){
                    case "machine.ticketsbag" -> "standard";
                    case "override.check" -> "false";
                    default -> null;
                };
                throw new AssertionError("UNEXPECTED_PROPERTIES_"+m.getName());
            });
        return (AppView)Proxy.newProxyInstance(AppView.class.getClassLoader(),
            new Class[]{AppView.class},(p,m,a)->switch(m.getName()){
                case "getBean" -> switch((String)a[0]){
                    case "com.mx.kylgis.pos.sales.DataLogicReceipts" -> receipts;
                    case "com.mx.kylgis.pos.forms.DataLogicSales" -> new DataLogicSales();
                    case "com.mx.kylgis.pos.forms.DataLogicSystem" -> new DataLogicSystem();
                    default -> throw new AssertionError("UNEXPECTED_BEAN");
                };
                case "getAppUserView" -> uv;
                case "getProperties" -> props;
                case "getActiveCashIndex" -> "QA-OTHER-CASH";
                case "getSession" -> session;
                default -> throw new AssertionError("UNEXPECTED_APP_"+m.getName());
            });
    }
    static void ui()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            try {
                panel=new RecoveryPanel(app());
                bag=new JTicketsBagShared(app(),panel);
                frame=new JFrame("KylGis QA isolated payment recovery");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.add(panel);
                frame.add(bag,BorderLayout.NORTH);
                frame.setSize(980,640);frame.setVisible(true);
                button(bag,JTicketsBagShared.class,"m_jNewTicket").doClick();
                ck("QA-OTHER-CASH".equals(panel.getActiveTicket().getActiveCash()),"DRAFT_USES_CURRENT_CASHBOX");
                ck(Math.abs(System.currentTimeMillis()-panel.getActiveTicket().getDate().getTime())<5000,
                   "DRAFT_USES_CURRENT_TIMESTAMP");
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    static TicketInfo specimen() {
        TicketInfo t=panel.getActiveTicket();
        t.addLine(new TicketLineInfo("QA-PRODUCT","Synthetic product","QA-TAX","0",2,15,null));
        t.setPayments(List.of(new PaymentInfoCash(4,8,12,2),
            new PaymentInfoTicket(6,"ccard","QA-BANK-REF",null)));
        t.setVoucherDiscounts(List.of(new VoucherDiscountInfo("V-ID","V-CODE","QA-CUSTOMER",30,20)));
        t.setProperty("kylgis.qa.durableLedger.operationUuid",OP);
        t.setActiveCash("QA-ORIGIN-CASH");t.setDate(new java.util.Date(WHEN));
        return t;
    }
    static String text(Component c) {
        String s=c instanceof JLabel l?l.getText():c instanceof JTextArea t?t.getText():"";
        if(c instanceof Container p)for(Component child:p.getComponents())s+=" "+text(child);
        return s;
    }
    static boolean owned(Window w) {
        for(Window owner=w.getOwner();owner!=null;owner=owner.getOwner())
            if(owner==frame)return true;
        return false;
    }
    static void selectSaved()throws Exception {
        AtomicBoolean selected=new AtomicBoolean();
        AtomicReference<Throwable> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            javax.swing.Timer timer=new javax.swing.Timer(80,null);
            timer.addActionListener(e->{
                try {
                    for(Window w:Window.getWindows())
                        if(w.isShowing()&&owned(w)&&w instanceof JTicketsBagSharedList){
                            JPanel tickets=(JPanel)field(w,JTicketsBagSharedList.class,"m_jtickets");
                            if(tickets.getComponentCount()!=1)throw new AssertionError("ONE_STORED_TICKET");
                            selected.set(true);timer.stop();
                            ((JButton)tickets.getComponent(0)).doClick();return;
                        }
                }catch(Throwable t){error.set(t);timer.stop();}
            });
            timer.start();
            try {button(bag,JTicketsBagShared.class,"m_jListTickets").doClick();}
            catch(Exception e){throw new RuntimeException(e);}
        });
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<deadline) {
            if(error.get()!=null)throw new AssertionError(error.get());
            AtomicBoolean done=new AtomicBoolean();
            SwingUtilities.invokeAndWait(()->done.set(selected.get()
                && OP.equals(panel.getActiveTicket().getProperty("kylgis.qa.durableLedger.operationUuid"))));
            if(done.get())return;
            Thread.sleep(30);
        }
        throw new AssertionError("RECOVERY_LIST_DIALOG_TIMEOUT");
    }
    static void blocked(String action)throws Exception {
        AtomicBoolean seen=new AtomicBoolean();
        AtomicReference<Throwable> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            javax.swing.Timer closer=new javax.swing.Timer(80,null);
            closer.addActionListener(e->{
                for(Window w:Window.getWindows())if(w instanceof JDialog d&&owned(w)&&w.isShowing()){
                    if("Venta pendiente de conciliación".equals(d.getTitle())
                        &&text(d).contains(panel.getActiveTicket().getId())){
                        seen.set(true);d.dispose();closer.stop();return;
                    }
                }
            });
            // An unexpected modal cannot hang or turn into success.
            javax.swing.Timer deadline=new javax.swing.Timer(4000,e->{
                for(Window w:Window.getWindows())if(owned(w)&&w.isShowing())w.dispose();
            });
            deadline.setRepeats(false);closer.start();deadline.start();
            try {
                if(action.equals("SPLIT")) {
                    JButton split=button(panel,JPanelTicket.class,"btnSplit");
                    if(split.isEnabled())split.doClick();
                    else {
                        // Empty tickets may disable the button. Test the handler guard as well.
                        Method m=JPanelTicket.class.getDeclaredMethod("btnSplitActionPerformed",java.awt.event.ActionEvent.class);
                        m.setAccessible(true);m.invoke(panel,new java.awt.event.ActionEvent(split,0,"QA"));
                    }
                }else if(action.equals("DELETE"))bag.deleteTicket();
                else {
                    Method m=JPanelTicket.class.getDeclaredMethod("stateTransition",char.class);
                    m.setAccessible(true);m.invoke(panel,'=');
                }
            }catch(Throwable t){error.set(t);}
            finally{closer.stop();deadline.stop();}
        });
        ck(seen.get()&&error.get()==null,action+"_BLOCKED_BEFORE_PAYMENT_OR_SPLIT_DIALOG");
    }
    static void finance(TicketInfo t,boolean attached,int lines)throws Exception {
        ck(t.requiresPaymentReconciliation(),"RECOVERED_REQUIRES_RECONCILIATION");
        ck(OP.equals(t.getProperty("kylgis.qa.durableLedger.operationUuid")),"OPERATION_ID_PRESERVED");
        ck("QA-ORIGIN-CASH".equals(t.getActiveCash()),"ORIGIN_CASH_PRESERVED_ON_OTHER_CASHBOX");
        ck(t.getDate().getTime()==WHEN,"ORIGIN_TIMESTAMP_PRESERVED_ON_OTHER_CASHBOX");
        ck(t.getLinesCount()==lines&&(lines==0||t.getLine(0).getMultiply()==2),"LINES_PRESERVED");
        ck(t.getPayments().size()==2&&t.getTotalPaid()==10,"MIXED_PAYMENTS_PRESERVED");
        PaymentInfo cash=t.getPayments().get(0),card=t.getPayments().get(1);
        ck(cash.getPaid()==8&&cash.getTendered()==12&&cash.getChange()==4,"CASH_EVIDENCE_PRESERVED");
        ck("QA-BANK-REF".equals(card.getTransactionID())&&!(card instanceof PaymentInfoMagcard),
           "BANK_REFERENCE_PRESERVED_WITH_INERT_PAYMENT");
        ck(t.getVoucherDiscounts().size()==1&&t.getVoucherDiscounts().get(0).getTotal()==20
            &&"V-ID".equals(t.getVoucherDiscounts().get(0).getVoucherId()),"VOUCHER_EVIDENCE_PRESERVED");
        if(attached)ck(t.getUser()!=null&&"QA-USER".equals(t.getUser().getId()),"CURRENT_OPERATOR_ATTACHED");
        else ck(t.getUser()==null,"SERIALIZER_OMITS_SESSION_OPERATOR");
    }
    static String dbSnapshot()throws Exception {
        try(Statement st=session.getConnection().createStatement();
            ResultSet rs=st.executeQuery("SELECT ID,SHA2(CONTENT,256),COALESCE(LOCKED,'') FROM sharedtickets ORDER BY ID")){
            String s="";
            while(rs.next())s+=rs.getString(1)+":"+rs.getString(2)+":"+rs.getString(3)+"\n";
            return s;
        }
    }
    static String storedId()throws Exception {
        ck(receipts.getSharedTicketList().size()==1,"ONE_DURABLE_RECOVERY");
        return receipts.getSharedTicketList().get(0).getId();
    }
    static String lockOf(String id)throws Exception {
        try(PreparedStatement ps=session.getConnection().prepareStatement("SELECT LOCKED FROM sharedtickets WHERE ID=?")){
            ps.setString(1,id);try(ResultSet r=ps.executeQuery()){
                if(!r.next())throw new AssertionError("RECOVERY_ROW_MISSING");
                return r.getString(1);
            }
        }
    }
    static void claimDirect(String id)throws Exception {
        AtomicReference<Throwable> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            try{
                Method m=JTicketsBagShared.class.getDeclaredMethod("setActiveTicket",String.class);
                m.setAccessible(true);m.invoke(bag,id);
            }catch(InvocationTargetException e){error.set(e.getCause());}
            catch(Throwable e){error.set(e);}
        });
        Throwable t=error.get();if(t instanceof Exception e)throw e;
        if(t!=null)throw new AssertionError(t);
    }
    static void suspendFailureAction(String action,TicketInfo t,String id)throws Exception {
        AtomicBoolean dialog=new AtomicBoolean();
        AtomicBoolean negative=new AtomicBoolean();
        AtomicReference<Throwable> error=new AtomicReference<>();
        SwingUtilities.invokeAndWait(()->{
            javax.swing.Timer timer=new javax.swing.Timer(60,null);
            timer.addActionListener(e->{
                for(Window w:Window.getWindows())if(w instanceof JDialog d&&owned(w)&&w.isShowing()
                        &&"No se pudo suspender la venta".equals(d.getTitle())&&text(d).contains(t.getId())){
                    dialog.set(true);d.dispose();timer.stop();
                }
            });
            javax.swing.Timer deadline=new javax.swing.Timer(3000,e->{
                for(Window w:Window.getWindows())if(owned(w)&&w.isShowing())w.dispose();
            });
            deadline.setRepeats(false);timer.start();deadline.start();
            try{
                switch(action){
                    case "DEACTIVATE" -> negative.set(!bag.deactivate());
                    case "NEW" -> button(bag,JTicketsBagShared.class,"m_jNewTicket").doClick();
                    case "HOLD" -> button(bag,JTicketsBagShared.class,"m_jHold").doClick();
                    default -> throw new AssertionError("BAD_FAILURE_ACTION");
                }
            }catch(Throwable e){error.set(e);}
            finally{timer.stop();deadline.stop();}
        });
        ck(error.get()==null&&dialog.get()&&panel.getActiveTicket()==t
            &&id.equals(field(bag,JTicketsBagShared.class,"m_sCurrentTicket")),
            "SUSPEND_FAILURE_RETAINS_ACTIVE_TICKET_"+action);
        if(action.equals("DEACTIVATE"))ck(negative.get(),"FAILED_SUSPEND_RETURNS_FALSE");
    }
    static void suspensionFailure(boolean fromDatabase)throws Exception {
        if(fromDatabase)selectSaved();
        TicketInfo t=fromDatabase?panel.getActiveTicket():specimen();
        List<PaymentInfo> valid=t.getPayments();
        String id=(String)field(bag,JTicketsBagShared.class,"m_sCurrentTicket");
        String before=dbSnapshot();
        if(!fromDatabase)t.setPayments(List.of(new PaymentInfoCash(Double.NaN,0)));
        for(String action:List.of("DEACTIVATE","NEW","HOLD")){
            suspendFailureAction(action,t,id);
            ck(before.equals(dbSnapshot()),"FAILED_SUSPEND_PRESERVES_DB_"+action);
        }
        SwingUtilities.invokeAndWait(()->bag.activate());
        ck(panel.getActiveTicket()==t&&id.equals(field(bag,JTicketsBagShared.class,"m_sCurrentTicket")),
            "REACTIVATE_DOES_NOT_DISCARD_FAILED_SUSPEND");
        ck(t.requiresPaymentReconciliation(),"FAILED_FINANCIAL_SUSPEND_KEEPS_PAYMENT_HOLD");
        if(!fromDatabase){
            t.setPayments(valid);
            SwingUtilities.invokeAndWait(()->ck(bag.deactivate(),"CORRECTED_ENCODING_CAN_SUSPEND_WITH_HOLD"));
            ck(receipts.getSharedTicket(id).requiresPaymentReconciliation(),"CORRECTED_SUSPEND_DOES_NOT_UNLOCK_PAYMENT");
        }
    }
    static void afterCrash()throws Exception {
        String id=storedId(),before=dbSnapshot(),owner=lockOf(id);
        ck(owner!=null&&owner.matches("[0-9a-f]{20}"),"CRASH_LEAVES_DURABLE_OWNER_WITHIN_LEGACY_WIDTH");
        finance(receipts.getSharedTicket(id),false,1);
        ck(!receipts.tryLockSharedTicket(id,"QA-OTHER-OWNER"),"RESTART_CANNOT_STEAL_CRASH_CLAIM");
        TicketInfo draft=panel.getActiveTicket();
        boolean refused=false;try{claimDirect(id);}catch(com.mx.kylgis.pos.basic.BasicException e){refused=true;}
        ck(refused&&panel.getActiveTicket()==draft,"SECOND_GUI_CANNOT_OPEN_CRASH_CLAIM");
        TicketInfo stored=receipts.getSharedTicket(id);
        stored.setUser(new UserInfo("QA-USER","Synthetic cashier"));
        ck(!receipts.suspendSharedTicketIfOwned(id,stored,0,"QA-OTHER-OWNER"),
            "FOREIGN_OWNER_CANNOT_WRITE_OR_RELEASE_RECOVERY");
        ck(before.equals(dbSnapshot()),"CRASH_RESTART_ATTEMPTS_PRESERVE_BYTES_AND_OWNER");
    }
    static void waitFile(java.nio.file.Path p)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20);
        while(!java.nio.file.Files.exists(p)){
            if(System.nanoTime()>deadline)throw new AssertionError("QA_BARRIER_TIMEOUT");
            Thread.sleep(25);
        }
    }
    static void claimRace(String side)throws Exception {
        java.nio.file.Path dir=java.nio.file.Path.of(System.getProperty("kylgis.qa.barrier"));
        String id=storedId();
        java.nio.file.Files.writeString(dir.resolve("race-"+side+".ready"),"READY");
        waitFile(dir.resolve("race.go"));
        boolean won;
        try{claimDirect(id);won=true;}
        catch(com.mx.kylgis.pos.basic.BasicException expected){won=false;}
        if(won){
            ck(panel.getActiveTicket().requiresPaymentReconciliation(),"RACE_WINNER_IS_HELD");
            ck(receipts.getSharedTicket(id)!=null,"RACE_WINNER_KEEPS_DURABLE_COPY");
            ck(lockOf(id).equals(field(bag,JTicketsBagShared.class,"m_recoveryOwner")),"RACE_WINNER_OWNS_CLAIM");
        }else ck(!panel.getActiveTicket().requiresPaymentReconciliation(),"RACE_LOSER_KEEPS_OWN_DRAFT");
        java.nio.file.Files.writeString(dir.resolve("race-"+side+".result"),won?"WIN":"LOSE");
        waitFile(dir.resolve("race-"+(side.equals("A")?"B":"A")+".result"));
        System.out.println("CLAIM_RACE_RESULT "+side+" "+(won?"WIN":"LOSE"));
    }

    static final class SuspensionAckProxy implements AutoCloseable {
        final java.net.ServerSocket listener;
        final int target;
        final AtomicBoolean seen=new AtomicBoolean(),dispatched=new AtomicBoolean(),
            serverOk=new AtomicBoolean(),cut=new AtomicBoolean();
        final Thread service;
        volatile Throwable failure;
        SuspensionAckProxy(int port)throws Exception {
            target=port;
            listener=new java.net.ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"));
            service=new Thread(this::serve,"qa-suspension-ack-proxy");
            service.setDaemon(true);service.start();
        }
        static byte[] packet(java.io.InputStream in)throws java.io.IOException {
            byte[] h=in.readNBytes(4);
            if(h.length==0)return null;
            if(h.length!=4)throw new java.io.EOFException("QA packet header");
            int n=(h[0]&255)|((h[1]&255)<<8)|((h[2]&255)<<16);
            if(n>1048576)throw new java.io.IOException("QA packet bound");
            byte[] data=in.readNBytes(n);
            if(data.length!=n)throw new java.io.EOFException("QA packet body");
            byte[] all=new byte[n+4];
            System.arraycopy(h,0,all,0,4);System.arraycopy(data,0,all,4,n);return all;
        }
        static void send(java.io.OutputStream out,byte[] p)throws java.io.IOException {
            out.write(p);out.flush();
        }
        static boolean release(byte[] p){
            if(p.length<5||(p[4]&255)!=3)return false;
            String q=new String(p,5,p.length-5,java.nio.charset.StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
            return q.contains("UPDATE SHAREDTICKETS SET")&&q.contains("CONTENT =")&&q.contains("LOCKED = NULL");
        }
        void serve(){
            try(java.net.Socket client=listener.accept();
                java.net.Socket server=new java.net.Socket("127.0.0.1",target)){
                client.setTcpNoDelay(true);server.setTcpNoDelay(true);
                Thread request=new Thread(()->{
                    try{
                        byte[] p;
                        while((p=packet(client.getInputStream()))!=null){
                            boolean match=release(p);
                            if(match)seen.set(true);
                            send(server.getOutputStream(),p);
                            if(match)dispatched.set(true);
                        }
                    }catch(java.io.IOException ignored){}
                },"qa-suspension-request");
                request.setDaemon(true);request.start();
                byte[] p;
                while((p=packet(server.getInputStream()))!=null){
                    if(seen.get()&&p.length>=5&&(p[4]&255)==0){
                        serverOk.set(true);cut.set(true);
                        client.setSoLinger(true,0);client.close();return;
                    }
                    send(client.getOutputStream(),p);
                }
            }catch(java.io.IOException e){if(!cut.get()&&!listener.isClosed())failure=e;}
            catch(Throwable e){failure=e;}
        }
        public void close()throws Exception{
            listener.close();service.join(5000);
            if(service.isAlive())throw new AssertionError("SUSPENSION_PROXY_NOT_STOPPED");
            if(failure!=null)throw new AssertionError("SUSPENSION_PROXY_FAILURE",failure);
        }
    }
    static void lostSuspensionAck(int port)throws Exception {
        selectSaved();
        TicketInfo t=panel.getActiveTicket();
        String id=(String)field(bag,JTicketsBagShared.class,"m_sCurrentTicket");
        t.setProperty("qa.suspension.ack.marker","QA-ACK-APPLIED");
        try(SuspensionAckProxy proxy=new SuspensionAckProxy(port)){
            Session viaProxy=new Session("jdbc:mariadb://127.0.0.1:"+proxy.listener.getLocalPort()
                +"/pos_qa_swing?sslMode=disable&useServerPrepStmts=false&connectTimeout=3000&socketTimeout=5000",
                "qa_swing","");
            DataLogicReceipts proxied=new DataLogicReceipts();proxied.init(viaProxy);
            SwingUtilities.invokeAndWait(()->{
                try{set(bag,JTicketsBagShared.class,"dlReceipts",proxied);}
                catch(Exception e){throw new RuntimeException(e);}
            });
            try{suspendFailureAction("DEACTIVATE",t,id);}
            finally{
                SwingUtilities.invokeAndWait(()->{
                    try{set(bag,JTicketsBagShared.class,"dlReceipts",receipts);}
                    catch(Exception e){throw new RuntimeException(e);}
                });
                viaProxy.close();
            }
            ck(proxy.seen.get()&&proxy.dispatched.get()&&proxy.serverOk.get()&&proxy.cut.get(),
               "SUSPEND_UPDATE_SERVER_OK_OBSERVED_ACK_SUPPRESSED");
        }
        ck(lockOf(id)==null,"LOST_ACK_UPDATE_WAS_COMMITTED_AND_CLAIM_RELEASED");
        ck("QA-ACK-APPLIED".equals(receipts.getSharedTicket(id).getProperty("qa.suspension.ack.marker")),
            "LOST_ACK_CONTENT_ACTUALLY_PERSISTED");
        ck(panel.getActiveTicket()==t&&t.requiresPaymentReconciliation(),
            "LOST_ACK_GUI_REMAINS_HELD_WITH_ORIGINAL_TICKET");
        String committed=dbSnapshot();
        for(String action:List.of("NEW","HOLD")){
            suspendFailureAction(action,t,id);
            ck(committed.equals(dbSnapshot()),"LOST_ACK_STALE_OWNER_CANNOT_OVERWRITE_"+action);
        }
    }

    static void logoutRefusal()throws Exception {
        AtomicBoolean safe=new AtomicBoolean();
        SwingUtilities.invokeAndWait(()->{
            class RejectingRoot extends JRootApp {
                int calls;
                @Override public boolean closeAppView(){calls++;return false;}
            }
            RejectingRoot root=new RejectingRoot();
            try{
                root.exitToLogin();
                safe.set(root.calls==1);
            }catch(RuntimeException expectedBaselineFailure){safe.set(false);}
        });
        ck(safe.get(),"REFUSED_DEACTIVATION_CANNOT_SHOW_LOGIN");
    }

    public static void main(String[] args) {
        int rc=0;
        try {
            if(!"ONLY_SYNTHETIC_SWING_RECOVERY".equals(System.getenv("KYLGIS_QA_SCOPE")))
                throw new IllegalArgumentException("REFUSE_SCOPE");
            int port=Integer.parseInt(args[0]);
            if(port<1024||port>65535)throw new IllegalArgumentException("REFUSE_PORT");
            Class.forName("org.mariadb.jdbc.Driver");
            session=new Session("jdbc:mariadb://127.0.0.1:"+port+"/pos_qa_swing","qa_swing","");
            receipts=new DataLogicReceipts();receipts.init(session);
            ui();
            if(args[1].equals("ack-suspend-failure")){
                lostSuspensionAck(port);
            }else if(args[1].equals("logout-refusal")){
                logoutRefusal();
            }else if(args[1].equals("suspend-failure")){
                suspensionFailure(false);
            }else if(args[1].equals("db-suspend-failure")){
                suspensionFailure(true);
            }else if(args[1].equals("claim-crash")){
                selectSaved();
                ck(receipts.getSharedTicketList().size()==1,"RECOVERY_COPY_STAYS_DURABLE_WHILE_OPEN");
                finance(panel.getActiveTicket(),true,1);
                ck(lockOf(storedId()).equals(field(bag,JTicketsBagShared.class,"m_recoveryOwner")),
                   "OPEN_RECOVERY_OWNS_DURABLE_COPY");
                java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("kylgis.qa.barrier")),"READY");
                System.out.println("READY_FOR_FORCED_CRASH");System.out.flush();
                Thread.sleep(120000);
                throw new AssertionError("PARENT_DID_NOT_KILL_OWNED_JVM");
            }else if(args[1].equals("after-crash")){
                afterCrash();
            }else if(args[1].startsWith("claim-race-")){
                claimRace(args[1].substring("claim-race-".length()));
            }else if(args[1].equals("recover-retention-regression")){
                selectSaved();
                ck(receipts.getSharedTicketList().size()==1,"RECOVERY_COPY_STAYS_DURABLE_WHILE_OPEN");
            }else if(args[1].equals("split-regression")){

                SwingUtilities.invokeAndWait(()->{
                    specimen();
                    panel.getActiveTicket().setProperty(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY,"true");
                });
                blocked("SPLIT");
            }else if(args[1].equals("delete-regression")){
                SwingUtilities.invokeAndWait(()->{
                    specimen();
                    panel.getActiveTicket().setProperty(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY,"true");
                });
                blocked("DELETE");
            }else if(args[1].equals("empty-regression")){
                SwingUtilities.invokeAndWait(()->{
                    TicketInfo empty=specimen();
                    empty.setProperty(TicketInfo.PAYMENT_RECONCILIATION_PROPERTY,"true");
                    empty.removeLine(0);bag.deactivate();
                });
                ck(receipts.getSharedTicketList().size()==1,"EMPTY_FINANCIAL_CONTEXT_RETAINED");
            }else if(args[1].equals("operation-only-suspend")){
                SwingUtilities.invokeAndWait(()->{
                    TicketInfo t=panel.getActiveTicket();
                    t.setProperty("kylgis.qa.durableLedger.operationUuid",OP);
                    ck(bag.deactivate(),"PREAUTH_ID_WITHOUT_COMPONENTS_SUSPENDED");
                });
                ck(receipts.getSharedTicketList().size()==1,"PREAUTH_ID_WITHOUT_COMPONENTS_RETAINED");
            }else if(args[1].equals("operation-only-recover")){
                selectSaved();
                TicketInfo t=panel.getActiveTicket();
                ck(t.requiresPaymentReconciliation(),"PREAUTH_ID_WITHOUT_COMPONENTS_HELD_ON_RECOVERY");
                ck(t.getPayments().isEmpty()&&t.getVoucherDiscounts().isEmpty(),"PREAUTH_EMPTY_COMPONENTS_PRESERVED");
                ck(receipts.getSharedTicketList().size()==1,"PREAUTH_COPY_REMAINS_DURABLE");
                blocked("SPLIT");blocked("DELETE");
                boolean held=false;
                try{new DataLogicSales().saveTicket(t,"QA-NO-DB");}
                catch(com.mx.kylgis.pos.basic.BasicException e){held=true;}
                ck(held,"PREAUTH_EMPTY_COMPONENTS_BACKEND_GUARD");
                SwingUtilities.invokeAndWait(()->ck(bag.deactivate(),"PREAUTH_EMPTY_COMPONENTS_RESUSPEND"));
            }else if(args[1].equals("suspend")){

                SwingUtilities.invokeAndWait(()->{
                    specimen();
                    ck(bag.deactivate(),"FIRST_SWING_SUSPEND_SUCCEEDED");
                });
                ck(receipts.getSharedTicketList().size()==1,"SUSPENDED_TICKET_DURABLE");
            }else{
                selectSaved();
                ck(receipts.getSharedTicketList().size()==1,"RECOVERY_COPY_STAYS_DURABLE_WHILE_OPEN");
                ck(lockOf(storedId()).equals(field(bag,JTicketsBagShared.class,"m_recoveryOwner")),
                   "RECOVERY_GUI_OWNS_DURABLE_COPY");
                TicketInfo t=panel.getActiveTicket();
                int lines=args[1].equals("recover-empty")?0:1;
                finance(t,true,lines);
                if(lines>0)blocked("TOTAL");
                blocked("SPLIT");blocked("DELETE");
                ck(panel.getActiveTicket()==t&&t.getLinesCount()==lines&&t.getPayments().size()==2,
                   "BLOCKED_ACTIONS_KEEP_ORIGINAL_TICKET");
                ck(t.requiresPaymentReconciliation(),"BLOCKED_ACTIONS_KEEP_RECONCILIATION_FLAG");
                boolean backend=false;
                try{new DataLogicSales().saveTicket(t,"QA-NO-DB");}
                catch(com.mx.kylgis.pos.basic.BasicException expected){backend=expected.getMessage().contains("reconcile");}
                ck(backend,"RECOVERED_UI_TICKET_BACKEND_GUARD");
                SwingUtilities.invokeAndWait(()->{
                    if(args[1].equals("suspend-empty"))t.removeLine(0);
                    ck(bag.deactivate(),"RECOVERED_TICKET_RESUSPEND_SUCCEEDED");
                });
                ck(receipts.getSharedTicketList().size()==1,"FINANCIAL_CONTEXT_RETAINED_AFTER_SUSPEND");
                ck(lockOf(storedId())==null,"SUCCESSFUL_RESUSPEND_RELEASES_CLAIM");
                TicketInfo saved=receipts.getSharedTicket(receipts.getSharedTicketList().get(0).getId());
                finance(saved,false,args[1].equals("suspend-empty")?0:lines);
                // Same GUI paths must also block an in-memory uncertain COMMIT before serialization.
                SwingUtilities.invokeAndWait(()->{
                    TicketInfo unknown=new TicketInfo();
                    unknown.addLine(new TicketLineInfo("QA-PRODUCT",2,15,null));
                    unknown.setProperty("kylgis.commit.outcome.unknown","true");
                    panel.setActiveTicket(unknown,null);
                });
                blocked("SPLIT");blocked("TOTAL");
            }
            if(Boolean.getBoolean("kylgis.qa.forceFailure"))throw new AssertionError("INTENTIONAL_SWING_QA_FAILURE");
            System.out.println("SWING_PAYMENT_RECOVERY_PASS phase="+args[1]+" checks="+checks);
        }catch(Throwable t){t.printStackTrace();rc=1;}
        finally{
            if(session!=null)session.close();
            try{SwingUtilities.invokeAndWait(()->{
                for(Window w:Window.getWindows())w.dispose();
            });}catch(Exception ignored){}
        }
        System.exit(rc);
    }
}
