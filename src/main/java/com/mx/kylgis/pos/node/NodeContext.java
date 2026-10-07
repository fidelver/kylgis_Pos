//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.node;

import com.mx.kylgis.pos.forms.AppProperties;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime identity and capabilities of one KylGis node.
 *
 * Legacy installations do not need node.* properties: node.id falls back to
 * machine.hostname and node.roles falls back to POS. This keeps the current
 * behaviour while the provisioning model is introduced incrementally.
 */
public final class NodeContext {

    private static final Logger LOGGER = Logger.getLogger(NodeContext.class.getName());

    private final String nodeId;
    private final String profile;
    private final EnumSet<NodeRole> roles;
    private final boolean explicitNodeId;
    private final boolean explicitRoles;

    private NodeContext(String nodeId, String profile, EnumSet<NodeRole> roles,
            boolean explicitNodeId, boolean explicitRoles) {
        this.nodeId = nodeId;
        this.profile = profile;
        this.roles = roles.clone();
        this.explicitNodeId = explicitNodeId;
        this.explicitRoles = explicitRoles;
    }

    public static NodeContext from(AppProperties properties) {
        String configuredId = trimToNull(properties.getProperty("node.id"));
        String legacyHostname = trimToNull(properties.getProperty("machine.hostname"));
        boolean hasExplicitId = configuredId != null;
        String resolvedId = hasExplicitId ? configuredId : legacyHostname;

        if (resolvedId == null) {
            resolvedId = "local";
            LOGGER.log(Level.WARNING,
                    "No node.id or machine.hostname configured; using legacy fallback: {0}",
                    resolvedId);
        }

        String profile = trimToNull(properties.getProperty("node.profile"));
        String configuredRoles = trimToNull(properties.getProperty("node.roles"));
        boolean hasExplicitRoles = configuredRoles != null;
        EnumSet<NodeRole> resolvedRoles = EnumSet.noneOf(NodeRole.class);

        if (configuredRoles != null) {
            for (String token : configuredRoles.split("[,;\\s]+")) {
                String normalized = trimToNull(token);
                if (normalized == null) {
                    continue;
                }
                NodeRole role = NodeRole.fromPropertyValue(normalized);
                if (role == null) {
                    LOGGER.log(Level.WARNING,
                            "Ignoring unknown KylGis node role: {0}", normalized);
                } else {
                    resolvedRoles.add(role);
                }
            }
        }

        if (resolvedRoles.isEmpty()) {
            resolvedRoles.add(NodeRole.POS);
        }

        return new NodeContext(resolvedId, profile, resolvedRoles, hasExplicitId, hasExplicitRoles);
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getProfile() {
        return profile;
    }

    public Set<NodeRole> getRoles() {
        return Collections.unmodifiableSet(EnumSet.copyOf(roles));
    }

    public boolean hasRole(NodeRole role) {
        return roles.contains(role);
    }

    public boolean hasExplicitNodeId() {
        return explicitNodeId;
    }

    public boolean hasExplicitRoles() {
        return explicitRoles;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Override
    public String toString() {
        return "NodeContext{"
                + "nodeId='" + nodeId + '\''
                + ", profile='" + profile + '\''
                + ", roles=" + roles
                + ", explicitNodeId=" + explicitNodeId
                + ", explicitRoles=" + explicitRoles
                + '}';
    }
}
