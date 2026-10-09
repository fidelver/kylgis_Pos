#!/bin/sh
set -eu

root=${1:-}
if [ -z "$root" ] || [ ! -d "$root" ]; then
    echo "Usage: $0 INSTALL_DIR" >&2
    exit 2
fi

# KylGis POS data must be readable by regular users while only launch scripts
# need an executable bit. Shared libraries are loaded as data and remain 0644.
find "$root" -type d -exec chmod 0755 {} +
find "$root" -type f -exec chmod 0644 {} +
find "$root" -type f -name '*.sh' -exec chmod 0755 {} +
