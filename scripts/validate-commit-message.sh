#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <commit-message-file>" >&2
    exit 2
fi

message_file="$1"

if [ ! -f "$message_file" ]; then
    echo "Commit message file not found: $message_file" >&2
    exit 2
fi

subject="$(sed -n '1p' "$message_file")"

case "$subject" in
    Merge\ *|Revert\ \"*|revert:\ *)
        exit 0
        ;;
esac

type_pattern='(feat|fix|docs|style|refactor|test|build|ci|chore|perf|revert)'
scope_pattern='(\([a-z0-9][a-z0-9._-]*\))?'
breaking_pattern='!?'
subject_pattern='[^[:space:]].*[^.]'
commit_pattern="^${type_pattern}${scope_pattern}${breaking_pattern}: ${subject_pattern}$"

if [[ "$subject" =~ $commit_pattern ]]; then
    exit 0
fi

cat >&2 <<'EOF'
Invalid commit message.

Use Conventional Commits:
  <type>(optional-scope): <subject>

Allowed types:
  feat, fix, docs, style, refactor, test, build, ci, chore, perf, revert

Examples:
  feat: add JVM browser
  fix(adapter-jmc): handle missing recording id
  docs!: rewrite contributor guide

Rules:
  - type is lowercase and from the allowed list
  - optional scope uses lowercase letters, digits, '.', '_' or '-'
  - subject is required
  - subject must not end with a period
EOF
exit 1
