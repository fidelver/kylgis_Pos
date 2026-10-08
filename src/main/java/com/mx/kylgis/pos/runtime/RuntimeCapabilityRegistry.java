//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.node.NodeRole;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Registry of executable capabilities available in this KylGis artifact. */
public final class RuntimeCapabilityRegistry {
    private static final Logger LOG = Logger.getLogger(RuntimeCapabilityRegistry.class.getName());
    private final Map<NodeRole, RuntimeCapability> capabilities = new EnumMap<>(NodeRole.class);
    private final List<ClassLoader> moduleLoaders = new ArrayList<>();

    public RuntimeCapabilityRegistry register(RuntimeCapability capability) {
        if (capability == null) throw new IllegalArgumentException("capability is required");
        NodeRole role = capability.getRole();
        if (role == null) throw new IllegalArgumentException("capability role is required");
        RuntimeCapability previous = capabilities.get(role);
        if (previous != null && previous.getClass() != capability.getClass()) {
            throw new IllegalStateException("Capability already registered for role "
                    + role.getPropertyValue() + ": " + previous.getClass().getName());
        }
        capabilities.put(role, capability);
        return this;
    }

    public RuntimeCapabilityRegistry discover(ClassLoader loader) {
        ClassLoader effective = loader == null ? RuntimeCapabilityRegistry.class.getClassLoader() : loader;
        for (RuntimeCapability capability : ServiceLoader.load(RuntimeCapability.class, effective)) {
            register(capability);
            LOG.log(Level.INFO, "Discovered runtime capability {0} from {1}",
                    new Object[]{capability.getRole().getPropertyValue(), capability.getClass().getName()});
        }
        return this;
    }

    /**
     * Loads each immediate child directory as an isolated capability module.
     * Every JAR inside that module directory (including lib/) shares one loader,
     * while different modules do not see one another's private dependencies.
     */
    public RuntimeCapabilityRegistry discoverModules(File root) {
        if (root == null || !root.isDirectory()) return this;
        File[] children = root.listFiles();
        if (children == null) return this;
        Arrays.sort(children, new Comparator<File>() {
            @Override public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); }
        });
        for (File child : children) {
            if (child.isDirectory()) discoverModule(child);
            else if (isJar(child)) discoverModule(child);
        }
        return this;
    }

    private void discoverModule(File module) {
        List<File> jars = new ArrayList<>();
        collectJars(module, jars);
        if (jars.isEmpty()) return;
        Collections.sort(jars, new Comparator<File>() {
            @Override public int compare(File a, File b) { return a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath()); }
        });
        try {
            URL[] urls = new URL[jars.size()];
            for (int i = 0; i < jars.size(); i++) urls[i] = jars.get(i).toURI().toURL();
            URLClassLoader loader = new URLClassLoader(urls, RuntimeCapabilityRegistry.class.getClassLoader());
            moduleLoaders.add(loader);
            LOG.log(Level.INFO, "Loading KylGis runtime module {0} with {1} JAR(s)",
                    new Object[]{module.getAbsolutePath(), jars.size()});
            discover(loader);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Invalid runtime module path: " + module, ex);
        }
    }

    private static void collectJars(File file, List<File> jars) {
        if (file == null) return;
        if (file.isFile()) {
            if (isJar(file)) jars.add(file);
            return;
        }
        File[] children = file.listFiles();
        if (children == null) return;
        for (File child : children) collectJars(child, jars);
    }

    private static boolean isJar(File file) {
        return file != null && file.isFile() && file.getName().toLowerCase().endsWith(".jar");
    }

    public RuntimeCapability get(NodeRole role) { return capabilities.get(role); }
    public Collection<RuntimeCapability> all() { return Collections.unmodifiableCollection(capabilities.values()); }

    public static RuntimeCapabilityRegistry defaults() {
        return new RuntimeCapabilityRegistry()
                .register(new PosCapability())
                .register(new PrintServiceCapability())
                .register(new ScaleServiceCapability())
                .register(new ScannerServiceCapability())
                .register(new DisplayServiceCapability())
                .register(new ConfigServiceCapability())
                .discover(Thread.currentThread().getContextClassLoader());
    }
}
