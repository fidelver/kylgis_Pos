//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
//
//    This file is part of KylGis POS.
package com.mx.kylgis.pos.config.provisioning;

import com.mx.kylgis.pos.config.DatabaseSettings;
import com.mx.kylgis.pos.config.remote.ConfigServiceConfig;
import com.mx.kylgis.pos.config.remote.RemoteConfigClient;
import com.mx.kylgis.pos.config.remote.RemoteConfigCache;
import com.mx.kylgis.pos.config.remote.RemoteConfigException;
import com.mx.kylgis.pos.forms.AppProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves KylGis configuration layers for one node.
 *
 * Legacy mode (no config.master): DEFAULT + LOCAL.
 * Provisioned mode: DEFAULT + MASTER + ORDERED NODE MODULES + LOCAL BOOTSTRAP.
 *
 * The local file remains the bootstrap identity of the node. The MASTER owns
 * the topology and points to the ordered module stack associated with each node.
 */
public final class NodeProvisioner {

    private static final Logger LOGGER = Logger.getLogger(NodeProvisioner.class.getName());

    public static final String MASTER_KEY = "config.master";
    public static final String NODE_ID_KEY = "node.id";
    public static final String NODE_MODULE_PREFIX = "node.";
    public static final String NODE_MODULE_SUFFIX = ".module";
    public static final String NODE_MODULES_SUFFIX = ".modules";
    public static final String SECRETS_KEY = "config.secrets";

    private NodeProvisioner() {
    }

    public static ProvisioningResult resolve(File localFile, Properties defaults)
            throws IOException {
        if (localFile == null) {
            throw new IllegalArgumentException("localFile is required");
        }

        Properties local = loadIfPresent(localFile);
        String masterReference = trimToNull(local.getProperty(MASTER_KEY));
        String remoteReference = trimToNull(local.getProperty(
                ConfigServiceConfig.REMOTE_SERVICE_KEY));

        if (masterReference != null && remoteReference != null) {
            throw new IOException("Node bootstrap cannot define both "
                    + MASTER_KEY + " and "
                    + ConfigServiceConfig.REMOTE_SERVICE_KEY);
        }

        if (remoteReference != null) {
            return resolveRemote(localFile, local, defaults);
        }

        if (masterReference == null) {
            Properties effective = merge(defaults, local);
            return new ProvisioningResult(false, localFile, null,
                    Collections.<File>emptyList(), null, local, new Properties(),
                    Collections.<Properties>emptyList(), effective, effective);
        }

        if (!localFile.isFile()) {
            throw new IOException("Provisioned node bootstrap does not exist: "
                    + localFile.getAbsolutePath());
        }

        String nodeId = trimToNull(local.getProperty(NODE_ID_KEY));
        if (nodeId == null) {
            throw new IOException("Provisioned node requires explicit " + NODE_ID_KEY
                    + " in " + localFile.getAbsolutePath());
        }

        File masterFile = resolveRelative(localFile.getParentFile(), masterReference);
        Properties master = loadRequired(masterFile, "MASTER");

        List<String> moduleReferences = nodeModuleReferences(master, nodeId);
        if (moduleReferences.isEmpty()) {
            throw new IOException("MASTER does not define " + nodeModulesKey(nodeId)
                    + " or legacy " + nodeModuleKey(nodeId) + " for node " + nodeId);
        }

        List<File> nodeModuleFiles = new ArrayList<>();
        List<Properties> nodeModules = new ArrayList<>();
        for (String moduleReference : moduleReferences) {
            File nodeModuleFile = resolveRelative(masterFile.getParentFile(), moduleReference);
            Properties nodeModule = loadRequired(nodeModuleFile, "node module");
            nodeModuleFiles.add(nodeModuleFile);
            nodeModules.add(nodeModule);
        }

        Properties rawEffective = merge(defaults, master);
        for (Properties nodeModule : nodeModules) {
            rawEffective.putAll(nodeModule);
        }
        rawEffective.putAll(local);
        stripRestrictedServiceCredentials(rawEffective);
        DatabaseSettings.applyLegacyCompatibility(rawEffective);
        File secretsFile = null;
        Properties effective = rawEffective;
        if (SecretResolver.containsSecretReferences(rawEffective)) {
            String secretsReference = trimToNull(master.getProperty(SECRETS_KEY));
            if (secretsReference == null) {
                throw new IOException("MASTER must define " + SECRETS_KEY
                        + " because the effective configuration contains !secret references");
            }
            secretsFile = resolveRelative(masterFile.getParentFile(), secretsReference);
            Properties secrets = loadRequired(secretsFile, "secrets");
            effective = SecretResolver.resolve(rawEffective, secrets);
        }

        return new ProvisioningResult(true, localFile, masterFile, nodeModuleFiles,
                secretsFile, local, master, nodeModules, rawEffective, effective);
    }

    private static ProvisioningResult resolveRemote(File localFile,
            Properties local, Properties defaults) throws IOException {
        if (!localFile.isFile()) {
            throw new IOException("Remote node bootstrap does not exist: "
                    + localFile.getAbsolutePath());
        }
        String nodeId = trimToNull(local.getProperty(NODE_ID_KEY));
        if (nodeId == null) {
            throw new IOException("Remote node requires explicit " + NODE_ID_KEY
                    + " in " + localFile.getAbsolutePath());
        }

        BootstrapProperties bootstrap = new BootstrapProperties(local, localFile);
        ConfigServiceConfig.RemoteEndpoint endpoint =
                ConfigServiceConfig.remoteEndpoint(bootstrap);
        boolean cacheEnabled = ConfigServiceConfig.remoteCacheEnabled(bootstrap);
        boolean cacheFallback = false;
        Properties remote;
        try {
            remote = RemoteConfigClient.fetch(endpoint, nodeId);
            if (cacheEnabled) {
                try {
                    RemoteConfigCache.store(ConfigServiceConfig.remoteCacheFile(bootstrap),
                            endpoint.getServiceId(), nodeId, endpoint.getToken(), remote);
                } catch (IOException cacheEx) {
                    LOGGER.log(Level.WARNING,
                            "Remote configuration received but cache could not be updated", cacheEx);
                }
            }
        } catch (RemoteConfigException authOrProtocolFailure) {
            // Explicit rejection/authentication/protocol failures must never be
            // bypassed using an older cached authorization.
            throw authOrProtocolFailure;
        } catch (IOException transportFailure) {
            if (!cacheEnabled) throw transportFailure;
            try {
                RemoteConfigCache.CachedConfiguration cached = RemoteConfigCache.load(
                        ConfigServiceConfig.remoteCacheFile(bootstrap),
                        endpoint.getServiceId(), nodeId, endpoint.getToken(),
                        ConfigServiceConfig.remoteCacheMaxAgeMs(bootstrap));
                remote = cached.getProperties();
                cacheFallback = true;
                LOGGER.log(Level.WARNING,
                        "Remote MASTER unavailable; using encrypted cached configuration from {0}",
                        new java.util.Date(cached.getTimestamp()));
            } catch (IOException cacheFailure) {
                transportFailure.addSuppressed(cacheFailure);
                throw transportFailure;
            }
        }
        Properties effective = merge(defaults, remote);
        effective.setProperty(NODE_ID_KEY, nodeId);
        DatabaseSettings.applyLegacyCompatibility(effective);

        // Keep endpoint metadata for diagnostics but never retain the
        // bootstrap authentication token in ProvisioningResult/AppConfig.
        Properties safeLocal = new Properties();
        for (String key : local.stringPropertyNames()) {
            if (!ConfigServiceConfig.REMOTE_TOKEN_KEY.equals(key)) {
                safeLocal.setProperty(key, local.getProperty(key));
            }
        }

        return new ProvisioningResult(true, true, cacheFallback, localFile, null,
                Collections.<File>emptyList(), null, safeLocal,
                new Properties(), Collections.<Properties>emptyList(),
                effective, effective);
    }

    /**
     * Builds the effective configuration for one node directly from a MASTER.
     * This form is used by the remote provisioning service and deliberately
     * excludes topology metadata and secret-store locations from the payload.
     */
    public static Properties resolveEffectiveNode(File masterFile, String nodeId)
            throws IOException {
        if (masterFile == null) {
            throw new IllegalArgumentException("masterFile is required");
        }
        String normalizedNodeId = trimToNull(nodeId);
        if (normalizedNodeId == null) {
            throw new IllegalArgumentException("nodeId is required");
        }

        Properties master = loadRequired(masterFile, "MASTER");
        List<String> moduleReferences = nodeModuleReferences(master, normalizedNodeId);
        if (moduleReferences.isEmpty()) {
            throw new IOException("MASTER does not define node modules for " + normalizedNodeId);
        }

        Properties rawEffective = merge(master);
        for (String moduleReference : moduleReferences) {
            File nodeModuleFile = resolveRelative(masterFile.getParentFile(), moduleReference);
            rawEffective.putAll(loadRequired(nodeModuleFile, "node module"));
        }
        rawEffective.setProperty(NODE_ID_KEY, normalizedNodeId);
        stripRestrictedServiceCredentials(rawEffective);
        DatabaseSettings.applyLegacyCompatibility(rawEffective);

        Properties effective = rawEffective;
        if (SecretResolver.containsSecretReferences(rawEffective)) {
            String secretsReference = trimToNull(master.getProperty(SECRETS_KEY));
            if (secretsReference == null) {
                throw new IOException("MASTER must define " + SECRETS_KEY
                        + " because the node configuration contains !secret references");
            }
            File secretsFile = resolveRelative(masterFile.getParentFile(), secretsReference);
            effective = SecretResolver.resolve(rawEffective,
                    loadRequired(secretsFile, "secrets"));
        }

        Properties sanitized = new Properties();
        sanitized.putAll(effective);
        sanitizeRemotePayload(sanitized);
        return sanitized;
    }

    /**
     * Removes service credentials that the effective node is not authorized to
     * consume. Filtering happens before SecretResolver so foreign aliases are
     * never resolved from the secret store.
     */
    private static void stripRestrictedServiceCredentials(Properties properties) {
        List<String> remove = new ArrayList<>();
        boolean configServer = hasRole(properties, "master") && hasRole(properties, "server");
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith("config.service.") && key.contains(".token.")) {
                if (!configServer) remove.add(key);
                continue;
            }
            if (key.startsWith("print.service.") && key.endsWith(".token")) {
                String id = serviceIdFromTokenKey(key, "print.service.");
                if (!canUsePrintService(properties, id)) remove.add(key);
                continue;
            }
            if (key.startsWith("scale.service.") && key.endsWith(".token")) {
                String id = serviceIdFromTokenKey(key, "scale.service.");
                if (!canUseScaleService(properties, id)) remove.add(key);
            }
        }
        for (String key : remove) properties.remove(key);
    }

    private static String serviceIdFromTokenKey(String key, String prefix) {
        return key.substring(prefix.length(), key.length() - ".token".length());
    }

    private static boolean canUsePrintService(Properties properties, String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) return false;
        if (hasRole(properties, "printer_service")
                && serviceId.equals(properties.getProperty("service.id"))) return true;
        for (int i = 1; i <= 6; i++) {
            String mapped = properties.getProperty("device.printer." + i + ".service");
            if (serviceId.equals(mapped)) return true;
        }
        return false;
    }

    private static boolean canUseScaleService(Properties properties, String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) return false;
        if (hasRole(properties, "scale_service")
                && serviceId.equals(properties.getProperty("service.id"))) return true;
        return serviceId.equals(properties.getProperty("device.scale.service"));
    }

    private static boolean hasRole(Properties properties, String expected) {
        String roles = trimToNull(properties.getProperty("node.roles"));
        if (roles == null) return false;
        for (String token : roles.split("[,;\\s]+")) {
            if (expected.equalsIgnoreCase(token.trim())) return true;
        }
        return false;
    }

    private static void sanitizeRemotePayload(Properties properties) {
        List<String> remove = new ArrayList<>();
        for (String key : properties.stringPropertyNames()) {
            if ((key.startsWith(NODE_MODULE_PREFIX)
                    && (key.endsWith(NODE_MODULE_SUFFIX) || key.endsWith(NODE_MODULES_SUFFIX)))
                    || SECRETS_KEY.equals(key)
                    || key.startsWith("config.service.")
                    || key.startsWith("config.remote.")) {
                remove.add(key);
            }
        }
        for (String key : remove) properties.remove(key);
    }

    public static void provisionBootstrap(File localFile, File masterFile,
            String nodeId, boolean overwrite) throws IOException {
        if (localFile == null || masterFile == null) {
            throw new IllegalArgumentException("localFile and masterFile are required");
        }
        String normalizedNodeId = trimToNull(nodeId);
        if (normalizedNodeId == null) {
            throw new IllegalArgumentException("nodeId is required");
        }
        if (localFile.exists() && !overwrite) {
            throw new IOException("Bootstrap already exists: " + localFile.getAbsolutePath());
        }

        Properties master = loadRequired(masterFile, "MASTER");
        List<String> moduleReferences = nodeModuleReferences(master, normalizedNodeId);
        if (moduleReferences.isEmpty()) {
            throw new IOException("MASTER does not define node modules for " + normalizedNodeId);
        }
        for (String moduleReference : moduleReferences) {
            File nodeModule = resolveRelative(masterFile.getParentFile(), moduleReference);
            loadRequired(nodeModule, "node module");
        }

        File parent = localFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create bootstrap directory: " + parent);
        }

        Properties bootstrap = new Properties();
        bootstrap.setProperty(NODE_ID_KEY, normalizedNodeId);
        bootstrap.setProperty(MASTER_KEY, relativizeIfPossible(parent, masterFile));

        try (OutputStream out = new FileOutputStream(localFile)) {
            bootstrap.store(out, "KylGis POS node bootstrap. Managed by provisioning.");
        }
    }

    public static void provisionRemoteBootstrap(File localFile, String nodeId,
            String serviceId, String host, int port, String token,
            boolean overwrite) throws IOException {
        if (localFile == null) {
            throw new IllegalArgumentException("localFile is required");
        }
        String normalizedNodeId = trimToNull(nodeId);
        String normalizedServiceId = trimToNull(serviceId);
        String normalizedHost = trimToNull(host);
        String normalizedToken = trimToNull(token);
        if (normalizedNodeId == null) throw new IllegalArgumentException("nodeId is required");
        if (normalizedServiceId == null || !normalizedServiceId.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new IllegalArgumentException("Invalid remote service id");
        }
        if (!normalizedNodeId.matches("[A-Za-z0-9_.-]{1,128}")) {
            throw new IllegalArgumentException("Invalid nodeId");
        }
        if (normalizedHost == null) throw new IllegalArgumentException("remote host is required");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("remote port out of range");
        if (normalizedToken == null || normalizedToken.length() < 16) {
            throw new IllegalArgumentException("remote token must contain at least 16 characters");
        }
        if (localFile.exists() && !overwrite) {
            throw new IOException("Bootstrap already exists: " + localFile.getAbsolutePath());
        }

        File parent = localFile.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Cannot create bootstrap directory: " + parent);
        }

        Properties bootstrap = new Properties();
        bootstrap.setProperty(NODE_ID_KEY, normalizedNodeId);
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_SERVICE_KEY, normalizedServiceId);
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_HOST_KEY, normalizedHost);
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_PORT_KEY, Integer.toString(port));
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_TOKEN_KEY, normalizedToken);
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_CACHE_ENABLED_KEY, "true");
        bootstrap.setProperty(ConfigServiceConfig.REMOTE_CACHE_MAXAGE_HOURS_KEY, "168");
        try (OutputStream out = new FileOutputStream(localFile)) {
            bootstrap.store(out, "KylGis remote node bootstrap. Contains enrolment credential.");
        }
        restrictBootstrapPermissions(localFile);
    }

    private static void restrictBootstrapPermissions(File file) {
        // Best effort and portable: on POSIX this becomes owner read/write only;
        // on platforms with different ACL semantics the OS may ignore part of it.
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    public static String nodeModuleKey(String nodeId) {
        return NODE_MODULE_PREFIX + nodeId + NODE_MODULE_SUFFIX;
    }

    public static String nodeModulesKey(String nodeId) {
        return NODE_MODULE_PREFIX + nodeId + NODE_MODULES_SUFFIX;
    }

    /**
     * Returns the ordered module stack for one node. The plural key is the
     * canonical form. The legacy singular key remains a transparent fallback.
     */
    public static List<String> nodeModuleReferences(Properties master, String nodeId) {
        if (master == null) {
            return Collections.emptyList();
        }
        String id = trimToNull(nodeId);
        if (id == null) {
            return Collections.emptyList();
        }

        String configured = trimToNull(master.getProperty(nodeModulesKey(id)));
        if (configured == null) {
            configured = trimToNull(master.getProperty(nodeModuleKey(id)));
        }
        if (configured == null) {
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String token : configured.split("[,;]+")) {
            String reference = trimToNull(token);
            if (reference != null) {
                result.add(reference);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static Properties loadIfPresent(File file) throws IOException {
        if (!file.isFile()) {
            return new Properties();
        }
        return load(file);
    }

    private static Properties loadRequired(File file, String layerName) throws IOException {
        if (!file.isFile()) {
            throw new IOException("KylGis " + layerName + " file does not exist: "
                    + file.getAbsolutePath());
        }
        return load(file);
    }

    private static Properties load(File file) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            properties.load(in);
        }
        return properties;
    }

    private static Properties merge(Properties... layers) {
        Properties merged = new Properties();
        if (layers != null) {
            for (Properties layer : layers) {
                if (layer != null) {
                    merged.putAll(layer);
                }
            }
        }
        return merged;
    }

    private static File resolveRelative(File baseDirectory, String reference) {
        File candidate = new File(reference);
        if (candidate.isAbsolute()) {
            return candidate.getAbsoluteFile();
        }
        File base = baseDirectory == null ? new File(".") : baseDirectory;
        return new File(base, reference).getAbsoluteFile();
    }

    private static String relativizeIfPossible(File baseDirectory, File target) {
        if (baseDirectory == null) {
            return target.getAbsolutePath();
        }
        try {
            Path base = baseDirectory.getCanonicalFile().toPath();
            Path destination = target.getCanonicalFile().toPath();
            return base.relativize(destination).toString();
        } catch (IOException | IllegalArgumentException ex) {
            return target.getAbsolutePath();
        }
    }

    private static final class BootstrapProperties implements AppProperties {
        private final Properties properties;
        private final File file;

        BootstrapProperties(Properties properties, File file) {
            this.properties = properties;
            this.file = file;
        }

        @Override public File getConfigFile() { return file; }
        @Override public String getHost() {
            return properties.getProperty("machine.hostname");
        }
        @Override public String getProperty(String key) {
            return properties.getProperty(key);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
