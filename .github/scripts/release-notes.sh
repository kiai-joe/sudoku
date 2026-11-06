#!/usr/bin/env bash
# Prints a release's notes: its version line, what changed since the
# previous release, and how to install.
#
#   release-notes.sh "VERSION LINE" SHA [PREVIOUS_SHA]
#
# The changes are the commits on the first-parent line from PREVIOUS_SHA to
# SHA. On main each is a squash-merged pull request, titled like
# "Scan a puzzle from a photo (#35)". Screenshot-bot commits and merges are
# left out. With no PREVIOUS_SHA this is the first release.
set -euo pipefail

version_line=$1
sha=$2
prev=${3:-}

echo "$version_line"
echo
if [[ -z "$prev" ]]; then
  echo "The first build of Sumi."
else
  changes=$(git log --first-parent --format='%s' "$prev..$sha" \
    | grep -v -e '^Update screenshots' -e '^Merge ' || true)
  if [[ -z "$changes" ]]; then
    echo "No changes since the last build; built again from the same code."
  else
    echo "## What's new"
    echo
    while IFS= read -r line; do echo "- $line"; done <<< "$changes"
  fi
fi
echo
echo '**Install:** open this page on your Android phone, tap **Sumi.apk**, and allow "Install unknown apps" for your browser when asked. Newer builds install over older ones and keep your saved game and stats.'
