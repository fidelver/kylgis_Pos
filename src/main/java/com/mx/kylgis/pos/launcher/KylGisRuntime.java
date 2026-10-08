//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.launcher;

import com.mx.kylgis.pos.forms.AppConfig;
import com.mx.kylgis.pos.node.NodeContext;
import com.mx.kylgis.pos.node.NodeRole;
import com.mx.kylgis.pos.runtime.CapabilityType;
import com.mx.kylgis.pos.runtime.RuntimeCapability;
import com.mx.kylgis.pos.runtime.RuntimeCapabilityRegistry;
import com.mx.kylgis.pos.runtime.RuntimeHandle;
import com.mx.kylgis.pos.runtime.RuntimeLaunchContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Canonical role-aware entry point for the KylGis POS JAR.
 *
 * Legacy properties without node.roles still resolve to POS through NodeContext.
 * Executable roles are discovered through RuntimeCapabilityRegistry; passive or
 * not-yet-implemented roles never leak implementation details into this class.
 */
public final class KylGisRuntime {

    private static final Logger LOG = Logger.getLogger(KylGisRuntime.class.getName());

    private KylGisRuntime() { }

    public static void main(String[] args) {
        AppConfig config = new AppConfig(args);
        config.load();
        AppConfig.setActiveInstance(config);

        NodeContext nodeContext = NodeContext.from(config);
        RuntimeLaunchContext launchContext = new RuntimeLaunchContext(config, nodeContext);
        RuntimeCapabilityRegistry registry = RuntimeCapabilityRegistry.defaults();
        File modulesDirectory = resolveModulesDirectory(config);
        if (modulesDirectory != null) {
            registry.discoverModules(modulesDirectory);
        }
        LOG.log(Level.INFO, "Starting KylGis runtime for {0}", nodeContext);

        List<RuntimeCapability> selected = selectCapabilities(nodeContext, registry);
        if (selected.isEmpty()) {
            logNonRunnableRoles(nodeContext, registry);
            LOG.severe("Node has no runnable role in this KylGis artifact: " + nodeContext.getRoles());
            System.exit(2);
            return;
        }

        try {
            for (RuntimeCapability capability : selected) {
                capability.validate(launchContext);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "KylGis node capability validation failed before startup", ex);
            System.exit(1);
            return;
        }

        final List<RuntimeHandle> handles = new ArrayList<>();
        try {
            boolean hasUi = hasUiCapability(selected);
            for (RuntimeCapability capability : selected) {
                boolean daemon = capability.getType() == CapabilityType.SERVICE && hasUi;
                LOG.log(Level.INFO, "Starting capability {0} ({1})",
                        new Object[]{capability.getRole().getPropertyValue(), capability.getType()});
                handles.add(capability.start(launchContext, daemon));
            }
        } catch (Exception ex) {
            closeReverse(handles);
            LOG.log(Level.SEVERE, "Cannot start KylGis node capabilities", ex);
            System.exit(1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override public void run() { closeReverse(handles); }
        }, "kylgis-runtime-shutdown"));

        logNonRunnableRoles(nodeContext, registry);
    }

    static List<RuntimeCapability> selectCapabilities(NodeContext context,
            RuntimeCapabilityRegistry registry) {
        List<RuntimeCapability> selected = new ArrayList<>();
        for (NodeRole role : context.getRoles()) {
            RuntimeCapability capability = registry.get(role);
            if (capability != null) selected.add(capability);
        }
        Collections.sort(selected, new Comparator<RuntimeCapability>() {
            @Override public int compare(RuntimeCapability left, RuntimeCapability right) {
                int leftOrder = left.getType() == CapabilityType.SERVICE ? 0 : 1;
                int rightOrder = right.getType() == CapabilityType.SERVICE ? 0 : 1;
                if (leftOrder != rightOrder) return leftOrder - rightOrder;
                return left.getRole().ordinal() - right.getRole().ordinal();
            }
        });
        return selected;
    }

    static File resolveModulesDirectory(AppConfig config) {
        String configured = config.getProperty("runtime.modules.dir");
        File runtimeBase = resolveRuntimeBaseDirectory(config);
        if (configured == null || configured.trim().isEmpty()) {
            File conventional = new File(runtimeBase, "modules");
            return conventional.isDirectory() ? conventional : null;
        }
        String path = configured.trim();
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            return new File(System.getProperty("user.home"), path.substring(2)).getAbsoluteFile();
        }
        File candidate = new File(path);
        if (candidate.isAbsolute()) return candidate;
        return new File(runtimeBase, path).getAbsoluteFile();
    }

    private static File resolveRuntimeBaseDirectory(AppConfig config) {
        String configuredBase = config.getProperty("runtime.base.dir");
        if (configuredBase != null && !configuredBase.trim().isEmpty()) {
            String value = configuredBase.trim();
            if (value.startsWith("~/") || value.startsWith("~\\")) {
                return new File(System.getProperty("user.home"), value.substring(2)).getAbsoluteFile();
            }
            File candidate = new File(value);
            if (candidate.isAbsolute()) return candidate;
        }
        try {
            File location = new File(KylGisRuntime.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getAbsoluteFile();
            return location.isFile() ? location.getParentFile() : location;
        } catch (Exception ex) {
            return new File(".").getAbsoluteFile();
        }
    }

    private static boolean hasUiCapability(List<RuntimeCapability> capabilities) {
        for (RuntimeCapability capability : capabilities) {
            if (capability.getType() == CapabilityType.UI) return true;
        }
        return false;
    }

    private static void logNonRunnableRoles(NodeContext context,
            RuntimeCapabilityRegistry registry) {
        for (NodeRole role : context.getRoles()) {
            if (registry.get(role) != null) continue;
            switch (role) {
                case MASTER:
                    LOG.info("Role master is an administrative capability; it does not start a process by itself.");
                    break;
                case KITCHEN:
                    LOG.warning("Role kitchen has no available runtime capability; install or repair the Kitchen module.");
                    break;
                case SERVER:
                case REMOTE_SESSIONS:
                    LOG.log(Level.WARNING, "Role {0} has no runtime capability registered yet.", role.getPropertyValue());
                    break;
                default:
                    LOG.log(Level.WARNING, "Role {0} is not executable in this artifact.", role.getPropertyValue());
                    break;
            }
        }
    }

    private static void closeReverse(List<RuntimeHandle> handles) {
        for (int i = handles.size() - 1; i >= 0; i--) {
            try {
                handles.get(i).close();
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Error stopping KylGis capability", ex);
            }
        }
    }
}
