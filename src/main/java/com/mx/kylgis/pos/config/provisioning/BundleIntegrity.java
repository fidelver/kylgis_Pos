//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.provisioning;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/** SHA-256 inventory for immutable portable bundle artifacts. */
public final class BundleIntegrity {
    public static final String MANIFEST_NAME = "bundle.sha256";

    private BundleIntegrity() { }

    public static void write(File bundleRoot) throws IOException {
        File root = canonicalDirectory(bundleRoot);
        List<String> paths = immutableFiles(root);
        File manifest = new File(root, MANIFEST_NAME);
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(manifest), StandardCharsets.UTF_8))) {
            out.write("# KylGis portable bundle SHA-256 inventory\n");
            out.write("# Mutable config/ files are intentionally excluded.\n");
            for (String relative : paths) {
                out.write(sha256(new File(root, relative)));
                out.write("  ");
                out.write(relative);
                out.write('\n');
            }
        }
    }

    public static Verification verify(File bundleRoot) throws IOException {
        File root = canonicalDirectory(bundleRoot);
        File manifest = new File(root, MANIFEST_NAME);
        if (!manifest.isFile()) return Verification.notPresent();

        Map<String, String> expected = readManifest(root, manifest);
        Set<String> actual = new LinkedHashSet<>(immutableFiles(root));
        List<String> problems = new ArrayList<>();

        for (Map.Entry<String, String> entry : expected.entrySet()) {
            String relative = entry.getKey();
            File file = new File(root, relative);
            if (!file.isFile()) {
                problems.add("missing:" + relative);
                continue;
            }
            String found = sha256(file);
            if (!entry.getValue().equalsIgnoreCase(found)) {
                problems.add("modified:" + relative);
            }
        }
        for (String relative : actual) {
            if (!expected.containsKey(relative)) problems.add("unexpected:" + relative);
        }
        for (String relative : expected.keySet()) {
            if (!actual.contains(relative) && new File(root, relative).isFile()) {
                problems.add("untracked:" + relative);
            }
        }
        return new Verification(true, problems);
    }

    private static Map<String, String> readManifest(File root, File manifest)
            throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(manifest), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int sep = line.indexOf("  ");
                if (sep != 64) throw new IOException("Invalid bundle manifest line");
                String hash = line.substring(0, sep);
                String relative = normalizeRelative(line.substring(sep + 2));
                if (!hash.matches("[0-9a-fA-F]{64}")) throw new IOException("Invalid SHA-256 in bundle manifest");
                File target = new File(root, relative).getCanonicalFile();
                if (!isInside(root, target)) throw new IOException("Bundle manifest path escapes bundle: " + relative);
                if (result.put(relative, hash.toLowerCase()) != null) throw new IOException("Duplicate bundle manifest path: " + relative);
            }
        }
        return result;
    }

    private static List<String> immutableFiles(File root) throws IOException {
        final List<String> result = new ArrayList<>();
        final Path base = root.toPath();
        try (Stream<Path> paths = Files.walk(base)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                String relative = base.relativize(path).toString().replace(File.separatorChar, '/');
                if (MANIFEST_NAME.equals(relative) || relative.startsWith("config/")) return;
                result.add(relative);
            });
        }
        Collections.sort(result);
        return result;
    }

    private static String sha256(File file) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new IOException("SHA-256 unavailable", ex); }
        byte[] buffer = new byte[32768];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int read;
            while ((read = in.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest.digest()) hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
    }

    private static File canonicalDirectory(File dir) throws IOException {
        if (dir == null) throw new IllegalArgumentException("bundleRoot is required");
        File root = dir.getCanonicalFile();
        if (!root.isDirectory()) throw new IOException("Bundle root does not exist: " + root);
        return root;
    }

    private static String normalizeRelative(String value) throws IOException {
        String relative = value == null ? "" : value.trim().replace('\\', '/');
        if (relative.isEmpty() || relative.startsWith("/") || relative.contains("../") || relative.equals("..")) {
            throw new IOException("Invalid bundle manifest path: " + relative);
        }
        return relative;
    }

    private static boolean isInside(File root, File target) {
        String base = root.getPath() + File.separator;
        return target.getPath().startsWith(base);
    }

    public static final class Verification {
        private final boolean present;
        private final List<String> problems;
        private Verification(boolean present, List<String> problems) {
            this.present = present;
            this.problems = Collections.unmodifiableList(new ArrayList<>(problems));
        }
        static Verification notPresent() { return new Verification(false, Collections.<String>emptyList()); }
        public boolean isPresent() { return present; }
        public boolean isValid() { return present && problems.isEmpty(); }
        public List<String> getProblems() { return problems; }
    }
}
