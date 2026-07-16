#!/bin/bash
# Performance benchmark suite for Aperture pipeline

echo "=== Aperture Pipeline Performance Benchmark ==="
echo ""

cd "$(dirname "$0")/.."

echo "Running comprehensive performance benchmarks..."
echo "This may take several minutes..."
echo ""

# Run all benchmark tests
./gradlew :aperture-opening-geometry:test \
  --tests "dev.aperture.opening.geometry.performance.*" \
  --info

echo ""
echo "=== Benchmark Complete ==="
echo ""
echo "Performance Targets:"
echo "  Cold generation: < 150ms"
echo "  Cached generation: < 5ms"
echo ""
echo "Review test output above for detailed statistics."
