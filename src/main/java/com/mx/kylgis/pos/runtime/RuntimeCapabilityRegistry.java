//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.runtime;

import com.mx.kylgis.pos.node.NodeRole;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ServiceLoader;

/** Registry of executable capabilities available in this KylGis artifact. */
public final class RuntimeCapabilityRegistry {
    private final Map<NodeRole, RuntimeCapability> capabilities =
            new EnumMap<>(NodeRole.class);

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
        ClassLoader effective = loader == null
                ? RuntimeCapabilityRegistry.class.getClassLoader() : loader;
        for (RuntimeCapability capability : ServiceLoader.load(RuntimeCapability.class, effective)) {
            register(capability);
        }
        return this;
    }

    public RuntimeCapability get(NodeRole role) { return capabilities.get(role); }

    public Collection<RuntimeCapability> all() {
        return Collections.unmodifiableCollection(capabilities.values());
    }

    public static RuntimeCapabilityRegistry defaults() {
        return new RuntimeCapabilityRegistry()
                .register(new PosCapability())
                .register(new PrintServiceCapability())
                .discover(Thread.currentThread().getContextClassLoader());
    }
}
