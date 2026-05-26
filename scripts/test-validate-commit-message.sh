#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VALIDATOR="$ROOT_DIR/scripts/validate-commit-message.sh"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

write_message() {
    local name="$1"
    shift
    printf '%s\n' "$@" > "$TMP_DIR/$name"
}

expect_valid() {
    local name="$1"
    if ! "$VALIDATOR" "$TMP_DIR/$name" > "$TMP_DIR/$name.out" 2>&1; then
        echo "Expected valid commit message to pass: $name"
        cat "$TMP_DIR/$name.out"
        return 1
    fi
}

expect_invalid() {
    local name="$1"
    if "$VALIDATOR" "$TMP_DIR/$name" > "$TMP_DIR/$name.out" 2>&1; then
        echo "Expected invalid commit message to fail: $name"
        cat "$TMP_DIR/$name.out"
        return 1
    fi
}

write_message valid_feature "feat: add recording browser"
write_message valid_scope "fix(adapter-jmc): handle missing recording id"
write_message valid_breaking "feat!: require Java 26"
write_message valid_body "docs: add contributor guide" "" "Explain the local hook setup."
write_message valid_merge "Merge branch 'main' into feature/logging"
write_message invalid_type "feature: add recording browser"
write_message invalid_capitalized "Feat: add recording browser"
write_message invalid_missing_colon "fix handle missing recording id"
write_message invalid_empty_subject "docs:"
write_message invalid_period "chore: update build."

expect_valid valid_feature
expect_valid valid_scope
expect_valid valid_breaking
expect_valid valid_body
expect_valid valid_merge
expect_invalid invalid_type
expect_invalid invalid_capitalized
expect_invalid invalid_missing_colon
expect_invalid invalid_empty_subject
expect_invalid invalid_period
