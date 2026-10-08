//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.node;

import java.util.Set;

/** Cross-role constraints that must hold before any node starts or is saved. */
public final class NodeRolePolicy {
    private NodeRolePolicy() { }

    public static void validate(Set<NodeRole> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("A KylGis node requires at least one role");
        }
        if (roles.contains(NodeRole.SERVER) && !roles.contains(NodeRole.MASTER)) {
            throw new IllegalArgumentException(
                    "Role server requires role master on the same node");
        }
        if (roles.contains(NodeRole.POS) && roles.contains(NodeRole.KITCHEN)) {
            throw new IllegalArgumentException(
                    "Roles pos and kitchen cannot share one KylGis JVM yet; configure separate nodes or processes");
        }
    }
}
