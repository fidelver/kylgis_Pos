//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.launcher;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.config.provisioning.BundleIntegrity;
import com.mx.kylgis.pos.node.NodeContext;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.runtime.RuntimeCapability;
import com.mx.kylgis.pos.runtime.RuntimeCapabilityRegistry;
import com.mx.kylgis.pos.runtime.RuntimeLaunchContext;
import com.mx.kylgis.pos.runtime.JavaRuntimeSupport;
import java.io.File;
import java.util.List;

/** Side-effect-free preflight for one KylGis node/bootstrap. */
public final class KylGisCheck {
    private KylGisCheck() { }

    public static void main(String[] args) {
        int exit = check(args);
        if (exit != 0) System.exit(exit);
    }

    static int check(String[] args) {
        try {
            AppConfig config = new AppConfig(args);
            config.load();
            AppConfig.setActiveInstance(config);
            NodeContext node = NodeContext.from(config);
            RuntimeLaunchContext context = new RuntimeLaunchContext(config, node);

            System.out.println("KYLGIS_NODE_CHECK");
            line("node.id", node.getNodeId());
            line("node.roles", node.getRoles().toString());
            line("node.profile", value(node.getProfile()));
            line("os.name", System.getProperty("os.name"));
            line("os.arch", System.getProperty("os.arch"));
            line("java.version", System.getProperty("java.version"));
            int javaMajor = JavaRuntimeSupport.currentMajor();
            String javaCompatibility = JavaRuntimeSupport.classification(javaMajor);
            line("java.compatibility", javaCompatibility);
            if ("UNSUPPORTED".equals(javaCompatibility)) {
                System.out.println("result=FAIL:java-8-or-newer-required");
                return 1;
            }
            line("provisioning", provisioning(config));

            String bundleDir = System.getProperty("kylgis.bundle.dir");
            if (bundleDir != null && !bundleDir.trim().isEmpty()) {
                BundleIntegrity.Verification integrity = BundleIntegrity.verify(new File(bundleDir.trim()));
                if (!integrity.isPresent()) {
                    line("bundle.integrity", "MISSING");
                    System.out.println("result=FAIL:bundle-manifest-missing");
                    return 1;
                }
                if (!integrity.isValid()) {
                    line("bundle.integrity", "FAIL:" + integrity.getProblems());
                    System.out.println("result=FAIL:bundle-integrity");
                    return 1;
                }
                line("bundle.integrity", "OK");
            }

            RuntimeCapabilityRegistry registry = RuntimeCapabilityRegistry.defaults();
            File modules = KylGisRuntime.resolveModulesDirectory(config);
            line("runtime.modules", modules == null ? "none" : modules.getAbsolutePath());
            if (modules != null) registry.discoverModules(modules);

            boolean warning = "UNVERIFIED_NEWER".equals(javaCompatibility)
                    || !registry.getDiscoveryErrors().isEmpty();
            int moduleError = 0;
            for (String error : registry.getDiscoveryErrors()) {
                System.out.println("runtime.module.error." + (++moduleError) + "=" + error);
            }
            for (NodeRole role : node.getRoles()) {
                RuntimeCapability capability = registry.get(role);
                if (capability == null) {
                    if (role == NodeRole.MASTER) {
                        System.out.println("capability." + role.getPropertyValue() + "=ADMINISTRATIVE");
                    } else {
                        System.out.println("capability." + role.getPropertyValue() + "=MISSING");
                        warning = true;
                    }
                }
            }

            List<RuntimeCapability> selected = KylGisRuntime.selectCapabilities(node, registry);
            if (selected.isEmpty()) {
                System.out.println("result=FAIL:no-runnable-capability");
                return 2;
            }
            for (RuntimeCapability capability : selected) {
                try {
                    capability.validate(context);
                    System.out.println("capability." + capability.getRole().getPropertyValue() + "=OK");
                } catch (Exception ex) {
                    System.out.println("capability." + capability.getRole().getPropertyValue()
                            + "=INVALID:" + safe(ex));
                    System.out.println("result=FAIL");
                    return 1;
                }
            }
            System.out.println(warning ? "result=WARN" : "result=OK");
            return 0;
        } catch (Throwable ex) {
            System.out.println("result=FAIL:" + safe(ex));
            return 1;
        }
    }

    private static String provisioning(AppConfig config) {
        if (config.isRemoteProvisioned()) {
            return config.isRemoteCacheFallback() ? "remote-cache" : "remote-live";
        }
        return config.isProvisioned() ? "local-master" : "legacy-local";
    }

    private static void line(String key, Object value) {
        System.out.println(key + "=" + (value == null ? "" : value));
    }
    private static String value(String value) { return value == null ? "" : value; }
    private static String safe(Throwable ex) {
        String message = ex.getMessage();
        if (message == null || message.trim().isEmpty()) message = ex.getClass().getSimpleName();
        message = message.replace('\n', ' ').replace('\r', ' ').trim();
        return message.length() > 240 ? message.substring(0, 240) : message;
    }
}
