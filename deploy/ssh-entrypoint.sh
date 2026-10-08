#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

base=$(cd -- "$(dirname -- "$0")" && pwd -P)
if [[ ! ${SSH_ORIGINAL_COMMAND:-} =~ ^deploy\ ([0-9a-f]{40}-[0-9]+-[0-9]+)$ ]]; then
    echo 'This key only accepts: deploy <commit SHA>-<run ID>-<attempt>' >&2
    exit 1
fi
release_id=${BASH_REMATCH[1]}
release="$base/releases/$release_id"
mkdir -p -- "$base/releases"
# A repeated run must use a new attempt number, never overwrite an active release.
mkdir -- "$release"
tar -xz --no-same-owner --no-same-permissions -C "$release"
cd -- "$release"
sha256sum --check --quiet SHA256SUMS
[[ $(cat REVISION) == "${release_id%%-*}" ]]
exec bash "$release/deploy/deploy.sh" "$base" "$release"
