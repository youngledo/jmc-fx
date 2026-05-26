#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <base-ref> <head-ref>" >&2
    exit 2
fi

base_ref="$1"
head_ref="$2"
root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
validator="$root_dir/scripts/validate-commit-message.sh"
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

commits_file="$tmp_dir/commits.txt"
git rev-list --no-merges --reverse "${base_ref}..${head_ref}" > "$commits_file"

if [ ! -s "$commits_file" ]; then
    echo "No non-merge commits to validate."
    exit 0
fi

while IFS= read -r commit; do
    message_file="$tmp_dir/$commit.txt"
    git log --format=%B -n 1 "$commit" > "$message_file"
    if ! "$validator" "$message_file"; then
        echo "Invalid commit: $commit" >&2
        exit 1
    fi
done < "$commits_file"
