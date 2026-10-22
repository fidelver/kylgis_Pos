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
            if(args[1].equals("split-regression")){
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
            }else if(args[1].equals("suspend")){
                SwingUtilities.invokeAndWait(()->{
                    specimen();
                    ck(bag.deactivate(),"FIRST_SWING_SUSPEND_SUCCEEDED");
                });
                ck(receipts.getSharedTicketList().size()==1,"SUSPENDED_TICKET_DURABLE");
            }else{
                selectSaved();
                ck(receipts.getSharedTicketList().isEmpty(),"RECOVERY_FROM_REAL_SHARED_LIST");
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
