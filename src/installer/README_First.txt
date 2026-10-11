Project: KylGis POS 1.0
Topic: First installation notes
Maintainer: KylGis POS Project
Date: 11 October 2026

KylGis POS requires Java 21 or newer. Java 21 LTS is the baseline runtime for
this release line. Use a Java build matching the operating system and CPU
architecture. Eclipse Temurin 21 LTS and equivalent OpenJDK-compatible builds
are supported targets.

The portable launchers resolve Java in this order:
1. an application-local runtime in runtime/;
2. KYLGIS_JAVA_HOME;
3. JAVA_HOME;
4. java/javaw from PATH.

A runtime older than Java 21 is rejected before KylGis POS starts. This avoids
accidentally launching the Java 21 bytecode with a legacy Java 8 installation.

Do not copy Java executables into Windows system directories. Configure
KYLGIS_JAVA_HOME or JAVA_HOME normally, or let a future KylGis package provide
its own runtime/ directory.

JavaFX is required only by capabilities that explicitly depend on it, such as
the separate Kitchen application; the core KylGis POS runtime does not require
JavaFX merely to start the POS.

For current setup, multinode provisioning and database guidance, read the main
README and KylGis project documentation.
