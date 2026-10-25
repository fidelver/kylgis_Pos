package com.mx.kylgis.pos.fiscal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * Standalone no-dependency fiscal regression executable:
 * javac -d out SatTaxProfile.java SatConceptMetadata.java
 *     SatTaxCalculator.java SatTaxPhase2SelfTest.java
 * java -cp out com.mx.kylgis.pos.fiscal.SatTaxPhase2SelfTest
 */
public final class SatTaxPhase2SelfTest {
    private static int checks;
    private static final LocalDate DAY = LocalDate.of(2026,10,19);

    private static SatTaxProfile make(String id, String impuesto,
            SatTaxProfile.Factor f, SatTaxProfile.Nature n,
            String rate, String unit) {
        return new SatTaxProfile(id,impuesto,f,n,rate==null?null:new BigDecimal(rate),
                unit,null,null,true);
    }
    private static void eq(String description, BigDecimal actual, String expected) {
        checks++;
        if (actual.compareTo(new BigDecimal(expected)) != 0) {
            throw new AssertionError(description + " expected=" + expected + " actual=" + actual);
        }
    }
    private static void ok(String description,boolean value) {
        checks++;
        if (!value) throw new AssertionError(description);
    }
    private static void invalid(String description,Runnable run) {
        checks++;
        try { run.run(); }
        catch (IllegalArgumentException | IllegalStateException e) { return; }
        throw new AssertionError(description + ": exception expected");
    }
    public static void main(String[] args) {
        SatTaxProfile iva16=make("IVA16","002",SatTaxProfile.Factor.Tasa,
                SatTaxProfile.Nature.Traslado,"0.160000",null);
        SatTaxProfile iva0=make("IVA0","002",SatTaxProfile.Factor.Tasa,
                SatTaxProfile.Nature.Traslado,"0.000000",null);
        SatTaxProfile exento=make("IVAEX","002",SatTaxProfile.Factor.Exento,
                SatTaxProfile.Nature.Traslado,null,null);
        SatTaxProfile ret=make("RET","001",SatTaxProfile.Factor.Tasa,
                SatTaxProfile.Nature.Retencion,"0.100000",null);
        SatTaxProfile ieps=make("IEPS","003",SatTaxProfile.Factor.Cuota,
                SatTaxProfile.Nature.Traslado,"1.250000","litro");
        SatTaxCalculator.Line a=SatTaxCalculator.calculate(iva16,new BigDecimal("100.00"),null,DAY);
        SatTaxCalculator.Line b=SatTaxCalculator.calculate(iva0,new BigDecimal("100.00"),null,DAY);
        SatTaxCalculator.Line c=SatTaxCalculator.calculate(exento,new BigDecimal("100.00"),null,DAY);
        SatTaxCalculator.Line d=SatTaxCalculator.calculate(ret,new BigDecimal("100.00"),null,DAY);
        SatTaxCalculator.Line e=SatTaxCalculator.calculate(ieps,new BigDecimal("100.00"),new BigDecimal("3"),DAY);
        eq("IVA 16",a.importe(),"16.00");
        eq("IVA 0",b.importe(),"0");
        eq("exento importe",c.importe(),"0");
        ok("0 and exento differ",b.profile().getFactor()!=c.profile().getFactor());
        ok("exento has no TasaOCuota",c.profile().getTasaCuota()==null);
        eq("retencion positive",d.importe(),"10.00");
        eq("retencion signed",d.signedEffect(),"-10.00");
        eq("IEPS cuota 3 litros",e.importe(),"3.75");
        eq("aggregated",SatTaxCalculator.aggregate(Arrays.asList(a,d)).netTax(),"6.00");
        eq("totales traslados",SatTaxCalculator.aggregate(Arrays.asList(a,d)).traslados(),"16");
        eq("totales retenciones",SatTaxCalculator.aggregate(Arrays.asList(a,d)).retenciones(),"10");
        eq("redondeo",SatTaxCalculator.rounded(new BigDecimal("1.005"),2),"1.01");
        ok("quota carries quantity",e.cantidadFiscal().compareTo(new BigDecimal("3"))==0);
        invalid("exento rate forbidden",()->make("bad","002",SatTaxProfile.Factor.Exento,SatTaxProfile.Nature.Traslado,"0",null));
        invalid("retencion exento forbidden",()->make("bad","002",SatTaxProfile.Factor.Exento,SatTaxProfile.Nature.Retencion,null,null));
        invalid("cuota without unit",()->make("bad","003",SatTaxProfile.Factor.Cuota,SatTaxProfile.Nature.Traslado,"0.5",null));
        invalid("negative SAT rate forbidden",()->make("bad","001",SatTaxProfile.Factor.Tasa,SatTaxProfile.Nature.Retencion,"-0.1",null));
        invalid("negative base egreso",()->SatTaxCalculator.calculate(iva16,new BigDecimal("-10"),null,DAY));
        invalid("quota requires quantity",()->SatTaxCalculator.calculate(ieps,new BigDecimal("100"),null,DAY));
        invalid("dates reversed",()->new SatTaxProfile("id","002",SatTaxProfile.Factor.Tasa,
                SatTaxProfile.Nature.Traslado,new BigDecimal("0.16"),null,
                DAY,DAY.minusDays(1),true));
        SatTaxProfile expired=new SatTaxProfile("old","002",SatTaxProfile.Factor.Tasa,
                SatTaxProfile.Nature.Traslado,new BigDecimal("0.16"),null,
                DAY.minusDays(4),DAY.minusDays(1),true);
        invalid("expired",()->SatTaxCalculator.calculate(expired,BigDecimal.TEN,null,DAY));
        SatTaxProfile legacy=new SatTaxProfile("legacy",null,null,null,null,null,null,null,false);
        invalid("legacy cannot calculate",()->SatTaxCalculator.calculate(legacy,BigDecimal.TEN,null,DAY));
        SatConceptMetadata product=new SatConceptMetadata("p","02","50181905","H87",
                new BigDecimal("0.750000"),"litro");
        eq("cuota conversion",product.cantidadFiscal(new BigDecimal("4"),"litro"),"3.00");
        invalid("wrong quota unit",()->product.cantidadFiscal(BigDecimal.ONE,"kg"));
        invalid("invalid object code",()->new SatConceptMetadata("p","abc",null,null));
        invalid("missing rate",()->make("bad","002",SatTaxProfile.Factor.Tasa,SatTaxProfile.Nature.Traslado,null,null));
        ok("original RATE never inferred",legacy.getFactor()==null && legacy.getTasaCuota()==null);
        System.out.println("SAT_PHASE2_CHECKS_PASSED="+checks);
    }
}