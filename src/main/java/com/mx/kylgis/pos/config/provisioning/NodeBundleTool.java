//    KylGis POS Punto de Venta Táctil
//    Copyright (c) 2026 KylGis POS
package com.mx.kylgis.pos.config.provisioning;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/** Builds a portable KylGis node directory from one runtime distribution. */
public final class NodeBundleTool {
    private NodeBundleTool() { }

    public static void main(String[] args) {
        if (args.length < 3) {
            usage(); System.exit(2); return;
        }
        try {
            File distribution = new File(args[0]).getCanonicalFile();
            File bootstrap = new File(args[1]).getCanonicalFile();
            File output = new File(args[2]).getCanonicalFile();
            List<File> modules = new ArrayList<>();
            for (int i = 3; i < args.length; i++) modules.add(new File(args[i]).getCanonicalFile());
            build(distribution, bootstrap, output, modules);
            System.out.println("KylGis node bundle ready: " + output);
            System.out.println("Runtime modules: " + modules.size());
        } catch (Exception ex) {
            System.err.println("Cannot build KylGis node bundle: " + ex.getMessage());
            System.exit(1);
        }
    }

    public static void build(File distribution, File bootstrap, File output,
            List<File> modules) throws IOException {
        File jar = new File(distribution, "kylgispos.jar");
        File lib = new File(distribution, "lib");
        if (!jar.isFile() || !lib.isDirectory()) {
            throw new IOException("Distribution must contain kylgispos.jar and lib/: " + distribution);
        }
        if (!bootstrap.isFile()) throw new IOException("Bootstrap does not exist: " + bootstrap);
        if (output.exists()) {
            String[] children = output.list();
            if (!output.isDirectory() || (children != null && children.length > 0)) {
                throw new IOException("Output directory must be empty or absent: " + output);
            }
        } else if (!output.mkdirs()) {
            throw new IOException("Cannot create output directory: " + output);
        }

        copyFile(jar.toPath(), new File(output, "kylgispos.jar").toPath());
        copyTree(lib.toPath(), new File(output, "lib").toPath());
        File configDir = new File(output, "config");
        if (!configDir.mkdirs() && !configDir.isDirectory()) throw new IOException("Cannot create config/");
        File nodeConfig = new File(configDir, "node.properties");
        copyFile(bootstrap.toPath(), nodeConfig.toPath());
        restrict(nodeConfig);

        if (modules != null && !modules.isEmpty()) {
            File moduleRoot = new File(output, "modules");
            if (!moduleRoot.mkdirs() && !moduleRoot.isDirectory()) throw new IOException("Cannot create modules/");
            for (File module : modules) {
                if (!module.isDirectory()) throw new IOException("Runtime module is not a directory: " + module);
                copyTree(module.toPath(), new File(moduleRoot, module.getName()).toPath());
            }
        }

        writeLaunchers(output);
        writeCheckLaunchers(output);
        BundleIntegrity.write(output);
    }

    private static void writeLaunchers(File output) throws IOException {
        File sh = new File(output, "run.sh");
        String shell = "#!/usr/bin/env sh\n"
                + "DIR=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\n"
                + "OS=$(uname -s 2>/dev/null || echo unknown)\n"
                + "ARCH=$(uname -m 2>/dev/null || echo unknown)\n"
                + "NATIVE=\n"
                + "case \"$OS/$ARCH\" in\n"
                + "  Linux/i?86) NATIVE=\"$DIR/lib/Linux/i686-unknown-linux-gnu\" ;;\n"
                + "  Linux/x86_64|Linux/amd64) NATIVE=\"$DIR/lib/Linux/x86_64-unknown-linux-gnu\" ;;\n"
                + "  Darwin/i?86|Darwin/x86_64|Darwin/amd64) NATIVE=\"$DIR/lib/Mac_OS_X\" ;;\n"
                + "esac\n"
                + "if [ -n \"$NATIVE\" ] && [ -d \"$NATIVE\" ]; then\n"
                + "  exec java \"-Djava.library.path=$NATIVE\" \"-Ddirname.path=$DIR/\" -jar \"$DIR/kylgispos.jar\" \"$DIR/config/node.properties\" \"$@\"\n"
                + "else\n"
                + "  exec java \"-Ddirname.path=$DIR/\" -jar \"$DIR/kylgispos.jar\" \"$DIR/config/node.properties\" \"$@\"\n"
                + "fi\n";
        Files.write(sh.toPath(), shell.getBytes(StandardCharsets.UTF_8));
        sh.setExecutable(true, true);

        File cmd = new File(output, "run.cmd");
        String windows = "@echo off\r\n"
                + "set \"DIR=%~dp0\"\r\n"
                + "if /I \"%PROCESSOR_ARCHITECTURE%\"==\"x86\" goto native\r\n"
                + "if /I \"%PROCESSOR_ARCHITECTURE%\"==\"AMD64\" goto native\r\n"
                + "goto nonative\r\n"
                + ":native\r\n"
                + "java \"-Djava.library.path=%DIR%lib\\Windows\\i368-mingw32\" \"-Ddirname.path=%DIR%\" -jar \"%DIR%kylgispos.jar\" \"%DIR%config\\node.properties\" %*\r\n"
                + "goto end\r\n"
                + ":nonative\r\n"
                + "java \"-Ddirname.path=%DIR%\" -jar \"%DIR%kylgispos.jar\" \"%DIR%config\\node.properties\" %*\r\n"
                + ":end\r\n";
        Files.write(cmd.toPath(), windows.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeCheckLaunchers(File output) throws IOException {
        File sh = new File(output, "check.sh");
        String shell = "#!/usr/bin/env sh\n"
                + "DIR=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\n"
                + "exec java \"-Ddirname.path=$DIR/\" \"-Dkylgis.bundle.dir=$DIR\" -cp \"$DIR/kylgispos.jar:$DIR/lib/*\" "
                + "com.mx.kylgis.pos.launcher.KylGisCheck \"$DIR/config/node.properties\" \"$@\"\n";
        Files.write(sh.toPath(), shell.getBytes(StandardCharsets.UTF_8));
        sh.setExecutable(true, true);

        File cmd = new File(output, "check.cmd");
        String windows = "@echo off\r\n"
                + "set \"DIR=%~dp0\"\r\n"
                + "java \"-Ddirname.path=%DIR%\" \"-Dkylgis.bundle.dir=%DIR%\" -cp \"%DIR%kylgispos.jar;%DIR%lib/*\" "
                + "com.mx.kylgis.pos.launcher.KylGisCheck \"%DIR%config\\node.properties\" %*\r\n";
        Files.write(cmd.toPath(), windows.getBytes(StandardCharsets.UTF_8));
    }

    private static void copyFile(Path source, Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static void copyTree(final Path source, final Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void restrict(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    private static void usage() {
        System.err.println("Usage: NodeBundleTool <distribution-dir> <bootstrap.properties> <output-dir> [runtime-module-dir ...]");
    }
}
