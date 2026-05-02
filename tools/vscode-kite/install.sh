#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

npx --yes @vscode/vsce package --out ../../build/kite-language-0.1.0.vsix --allow-missing-repository
code --install-extension ../../build/kite-language-0.1.0.vsix --force
