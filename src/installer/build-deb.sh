#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd -- "$SCRIPT_DIR/../.." && pwd)"
PROJECT_XML="$SCRIPT_DIR/kylgispos.xml"

resolve_builder() {
    if [[ -n "${INSTALLBUILDER_BUILDER:-}" && -x "${INSTALLBUILDER_BUILDER}" ]]; then
        printf '%s\n' "$INSTALLBUILDER_BUILDER"
        return
    fi
    if [[ -n "${INSTALLBUILDER_HOME:-}" && -x "${INSTALLBUILDER_HOME}/bin/builder" ]]; then
        printf '%s\n' "$INSTALLBUILDER_HOME/bin/builder"
        return
    fi
    if command -v installbuilder-builder >/dev/null 2>&1; then
        command -v installbuilder-builder
        return
    fi
    if command -v builder >/dev/null 2>&1; then
        command -v builder
        return
    fi
    printf 'ERROR: InstallBuilder builder no encontrado. Configure INSTALLBUILDER_HOME o INSTALLBUILDER_BUILDER.\n' >&2
    exit 2
}

BUILDER="$(resolve_builder)"
DPKG_DEB_REAL="$(command -v dpkg-deb || true)"
if [[ -z "$DPKG_DEB_REAL" || ! -x "$DPKG_DEB_REAL" ]]; then
    printf 'ERROR: dpkg-deb no está disponible.\n' >&2
    exit 2
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kylgis-deb-build.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT
mkdir -p "$TMP_DIR/bin"

cat > "$TMP_DIR/bin/dpkg-deb" <<SHIM
#!/usr/bin/env bash
set -euo pipefail
for arg in "\$@"; do
    if [[ -d "\$arg/DEBIAN" ]]; then
        # InstallBuilder conserva modos/ACL del árbol de desarrollo. Normalizar el
        # staging evita paquetes con 670/750 y satisface las reglas de dpkg-deb.
        find "\$arg" -type d -exec chmod 0755 {} +
        find "\$arg" -type f -exec chmod 0644 {} +
        [[ -d "\$arg/opt/bitrock" ]] && find "\$arg/opt/bitrock" -type f -name 'helperBinary*' -exec chmod 0755 {} +
        find "\$arg" -type f -name '*.sh' -exec chmod 0755 {} +
        [[ -d "\$arg/usr/bin" ]] && find "\$arg/usr/bin" -type f -exec chmod 0755 {} +
        [[ -d "\$arg/usr/sbin" ]] && find "\$arg/usr/sbin" -type f -exec chmod 0755 {} +

        control_dir="\$arg/DEBIAN"
        find "\$control_dir" -maxdepth 1 -type f -exec chmod 0644 {} +
        for script in preinst postinst prerm postrm config; do
            [[ -f "\$control_dir/\$script" ]] && chmod 0755 "\$control_dir/\$script"
        done
    fi
done
exec "$DPKG_DEB_REAL" --root-owner-group "\$@"
SHIM
chmod 0755 "$TMP_DIR/bin/dpkg-deb"

cd "$PROJECT_ROOT"
mkdir -p target/installers
PATH="$TMP_DIR/bin:$PATH" "$BUILDER" build "$PROJECT_XML" deb --disable-parallel-compression

DEB_FILE="$(find target/installers -maxdepth 1 -type f -name '*.deb' -print -quit)"
if [[ -z "$DEB_FILE" ]]; then
    printf 'ERROR: InstallBuilder terminó sin producir un .deb.\n' >&2
    exit 3
fi

dpkg-deb --info "$DEB_FILE" >/dev/null
printf 'DEB_OK=%s\n' "$DEB_FILE"
sha256sum "$DEB_FILE"
