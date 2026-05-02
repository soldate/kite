#!/usr/bin/env bash
set -euo pipefail

rm -rf build/classes
mkdir -p build/classes
javac -d build/classes $(find src/main/java -name '*.java' | sort)
