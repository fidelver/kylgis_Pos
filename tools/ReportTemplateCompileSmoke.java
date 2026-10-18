import java.nio.file.*;
import java.util.*;
import net.sf.jasperreports.engine.*;
public final class ReportTemplateCompileSmoke {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]);
        int ok=0;
        List<String> failed=new ArrayList<>();
        try(var stream=Files.list(root)) {
            for(Path file: stream.filter(p->p.toString().endsWith(".jrxml")).sorted().toList()) {
                try(var in=Files.newInputStream(file)) {
                    JasperReport report=JasperCompileManager.compileReport(in);
                    if(report==null)throw new IllegalStateException("Null Jasper report");
                    ok++;
                }catch(Throwable ex) {
                    failed.add(file.getFileName()+": "+ex.getMessage());
                }
            }
        }
        for(String msg:failed)System.out.println("JRXML_FAIL "+msg);
        System.out.println("KYLGIS_JRXML_COMPILE_OK="+ok+" FAIL="+failed.size());
        if(!failed.isEmpty()||ok!=67)System.exit(2);
    }
}
