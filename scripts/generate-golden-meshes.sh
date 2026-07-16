#!/bin/bash
# Script to generate golden mesh files for pipeline stability tests

echo "=== Generating Golden Mesh Files ==="
echo ""

cd "$(dirname "$0")/.."

# Run the golden mesh generation test
./gradlew :aperture-opening-geometry:test --tests "dev.aperture.opening.geometry.golden.GenerateGoldenMeshes.generateAllGoldenMeshes"

echo ""
echo "=== Golden Mesh Generation Complete ==="
echo "Files saved to: aperture-opening-geometry/src/test/resources/golden/"
