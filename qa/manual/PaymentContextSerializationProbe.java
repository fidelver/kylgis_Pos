import com.mx.kylgis.pos.ticket.*;
import com.mx.kylgis.pos.payment.*;
import com.mx.kylgis.pos.voucher.VoucherDiscountInfo;
import java.io.*;
import java.net.*;
import java.util.*;

public final class PaymentContextSerializationProbe {
    static int checks;
    static void ck(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
    static byte[] encode(Object ticket)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(ObjectOutputStream out=new ObjectOutputStream(bytes)){out.writeObject(ticket);out.writeObject("AFTER_TICKET");}
        return bytes.toByteArray();
    }
    static TicketInfo decode(byte[] bytes)throws Exception{
        try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes))){
            TicketInfo t=(TicketInfo)in.readObject();ck("AFTER_TICKET".equals(in.readObject()),"PAYMENT_STREAM_BOUNDARY");return t;
        }
    }
    public static void main(String[] args)throws Exception{
        TicketInfo t=new TicketInfo();
        t.setActiveCash("QA-CASH");
        t.setProperty("kylgis.qa.durableLedger.operationUuid",UUID.randomUUID().toString());
        t.setPayments(List.of(new PaymentInfoCash(4,8,12,2),new PaymentInfoTicket(6,"ccard","QA-BANK-REF",null)));
        t.setVoucherDiscounts(List.of(new VoucherDiscountInfo("V-ID","V-CODE","CUSTOMER",30,20)));
        TicketInfo copy=t.copyTicket();
        ck(copy.getPayments().get(0).getPaid()==8&&copy.getPayments().get(0).getTendered()==12,"PAYMENT_CASH_COPY_PAID_TENDERED_NOT_SWAPPED");
        ck(copy.requiresPaymentReconciliation(),"PAYMENT_CORRELATED_COPY_BLOCKED");
        ck(t.getId().equals(copy.getProperty("kylgis.payment.context.sourceReceiptId")),"PAYMENT_COPY_SOURCE_ID_PRESERVED");
        TicketInfo restored=decode(encode(t));
        ck(t.getId().equals(restored.getId()),"PAYMENT_RECEIPT_ID_STABLE");
        ck(t.getProperty("kylgis.qa.durableLedger.operationUuid").equals(restored.getProperty("kylgis.qa.durableLedger.operationUuid")),"PAYMENT_OPERATION_ID_STABLE");
        ck(restored.getPayments().size()==2&&restored.getTotalPaid()==10,"PAYMENT_MIXED_COMPONENTS_PRESERVED");
        PaymentInfo cash=restored.getPayments().get(0),bank=restored.getPayments().get(1);
        ck(cash.getTotal()==4&&cash.getPaid()==8&&cash.getTendered()==12&&cash.getChange()==4,"PAYMENT_CASH_VALUES_PRESERVED");
        ck("QA-BANK-REF".equals(bank.getTransactionID()),"PAYMENT_PROVIDER_REFERENCE_PRESERVED");
        ck(!(bank instanceof PaymentInfoMagcard),"PAYMENT_RECOVERED_BANK_IS_INERT");
        ck(restored.requiresPaymentReconciliation(),"PAYMENT_RESTORE_REQUIRES_RECONCILIATION");
        VoucherDiscountInfo v=restored.getVoucherDiscounts().get(0);
        ck("V-ID".equals(v.getVoucherId())&&"V-CODE".equals(v.getVoucher())&&"CUSTOMER".equals(v.getCustomerId())&&v.getFaceValue()==30&&v.getTotal()==20,"PAYMENT_VOUCHER_IDENTITY_AMOUNTS_PRESERVED");
        ck("QA-CASH".equals(restored.getActiveCash()),"PAYMENT_TERMINAL_PRESERVED");
        TicketInfo second=decode(encode(restored));
        ck(second.requiresPaymentReconciliation()&&second.getPayments().size()==2,"PAYMENT_SECOND_SUSPEND_STAYS_BLOCKED");
        TicketInfo empty=decode(encode(new TicketInfo()));
        ck(!empty.requiresPaymentReconciliation(),"PAYMENT_EMPTY_DRAFT_REMAINS_USABLE");
        // Real pre-extension implementation from the sealed CORE jar, isolated loader.
        URL[] jars={new File("/tmp/qa-core13.jar").toURI().toURL(),new File("/tmp/qa-commons-lang.jar").toURI().toURL(),new File("/tmp/qa-mysql.jar").toURI().toURL()};
        try(URLClassLoader old=new URLClassLoader(jars,ClassLoader.getPlatformClassLoader())){
            Class<?> legacy=old.loadClass("com.mx.kylgis.pos.ticket.TicketInfo");
            Object oldTicket=legacy.getConstructor().newInstance();
            TicketInfo oldRead=decode(encode(oldTicket));
            ck(oldRead.getPayments().isEmpty(),"PAYMENT_OLD_FORMAT_READABLE");
            try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(encode(t))){
                protected Class<?> resolveClass(ObjectStreamClass desc)throws IOException,ClassNotFoundException{
                    try{return Class.forName(desc.getName(),false,old);}catch(ClassNotFoundException ignored){return super.resolveClass(desc);}
                }
            }){
                Object legacyRead=in.readObject();
                ck(legacy.isInstance(legacyRead),"PAYMENT_NEW_FORMAT_LEGACY_READER_COMPATIBLE");
                ck("AFTER_TICKET".equals(in.readObject()),"PAYMENT_LEGACY_READER_SKIPS_EXTENSION");
            }
        }
        boolean directBlocked=false;
        try {new com.mx.kylgis.pos.forms.DataLogicSales().saveTicket(restored,"0");}
        catch(com.mx.kylgis.pos.basic.BasicException expected){directBlocked=expected.getMessage().contains("reconcile");}
        ck(directBlocked,"PAYMENT_DIRECT_BACKEND_GUARD_BEFORE_DB_OR_GATEWAY");
        java.lang.reflect.Method reader=Class.forName("com.mx.kylgis.pos.ticket.TicketPaymentContext")
                .getDeclaredMethod("read",ObjectInput.class,TicketInfo.class);
        reader.setAccessible(true);
        for(int badCase=0;badCase<4;badCase++){
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(ObjectOutputStream out=new ObjectOutputStream(bytes)){
                if(badCase==0)out.writeByte(0x4b);
                else if(badCase==1){out.writeInt(0x4b504331);out.writeInt(262145);}
                else if(badCase==2){out.writeInt(0x4b504331);out.writeInt(4);out.writeInt(99);}
                else out.writeInt(0x12345678);
            }
            boolean failed=false;
            try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))){
                reader.invoke(null,in,new TicketInfo());
            }catch(java.lang.reflect.InvocationTargetException expected){failed=expected.getCause() instanceof IOException;}
            ck(failed,"PAYMENT_CORRUPT_EXTENSION_REJECTED_"+badCase);
        }
        // Invalid number and oversized collections must not produce a restorable ticket.
        TicketInfo bad=new TicketInfo();bad.setPayments(List.of(new PaymentInfoCash(Double.NaN,0)));
        boolean rejected=false;try{encode(bad);}catch(IOException expected){rejected=true;}
        ck(rejected,"PAYMENT_NONFINITE_REJECTED");
        bad.setPayments(Collections.nCopies(257,new PaymentInfoCash(1,1)));
        rejected=false;try{encode(bad);}catch(IOException expected){rejected=true;}
        ck(rejected,"PAYMENT_COMPONENT_BOUND_ENFORCED");
        System.out.println("PAYMENT_CONTEXT_SERIALIZATION_PASS checks="+checks);
    }
}
