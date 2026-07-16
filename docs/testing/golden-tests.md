# Golden Test Documentation

## Overview

Golden tests verify that pipeline output remains stable across code changes. They capture "known good" mesh outputs as JSON files and compare future runs against them.

## Purpose

- **Regression Detection**: Catch unintended changes to geometry generation
- **Refactoring Safety**: Confidently refactor knowing output hasn't changed
- **Documentation**: Golden files serve as concrete examples of expected output
- **Performance Baseline**: Track mesh complexity over time

## Structure

### Test Files

- **GenerateGoldenMeshes.java**: Generates golden mesh JSON files from verified pipeline runs
- **PipelineGoldenTest.java**: Compares current pipeline output against golden files
- **GoldenMeshSupport.java**: JSON serialization and comparison utilities

### Golden File Format

```json
{
  "vertexCount": 24,
  "faceCount": 12,
  "vertices": [
    {"x": 0.0, "y": 0.0, "z": 0.0},
    {"x": 1200.0, "y": 0.0, "z": 0.0}
  ],
  "faces": [
    {"indices": [0, 1, 2], "materialSlot": 0}
  ]
}
```

### Storage Location

```
aperture-opening/src/test/resources/golden/
├── fixed_window_1200x1500_frame_bottom.json
├── fixed_window_1200x1500_frame_top.json
├── door_single_900x2100_frame_bottom.json
├── door_single_900x2100_door_leaf_0_bottom.json
└── ...
```

## Usage

### Generating Golden Files

Run when you've verified that pipeline output is correct:

```bash
# Generate all golden files
./scripts/generate-golden-meshes.sh

# Or run specific test
./gradlew :aperture-opening:test \
  --tests "GenerateGoldenMeshes.generateFixedWindowGolden"
```

### Running Golden Tests

```bash
# Run all golden tests
./gradlew :aperture-opening:test \
  --tests "PipelineGoldenTest"

# Run specific golden test
./gradlew :aperture-opening:test \
  --tests "PipelineGoldenTest.fixedWindow_1200x1500_matchesGolden"
```

### Test Coverage

Current golden test coverage:

1. **fixed_window_1200x1500**: Standard fixed window
   - Frame: bottom, top, left, right
   - Glazing: main glass pane
   - Sill: bottom trim

2. **fixed_window_600x800**: Small fixed window
   - Verifies scaling works correctly

3. **door_single_900x2100**: Single panel door
   - Frame: all four sides
   - Door leaf: panel with glass insert
   - Hardware: hinges, handle
   - Threshold: floor transition

4. **door_double_1800x2300**: Double panel door
   - Two door leaves
   - Paired hardware

5. **door_solid_1000x2100**: Solid door (no glass)
   - Panel without glazing

6. **curtain_wall_3000x2700**: Multi-unit curtain wall
   - Multiple frames and glazing units

## Comparison Logic

### Vertex Comparison

Vertices match if within epsilon (0.001mm):

```java
|v1.x - v2.x| < 0.001 && 
|v1.y - v2.y| < 0.001 && 
|v1.z - v2.z| < 0.001
```

### Face Comparison

Faces match if:
- Same vertex indices
- Same material slot

### Tolerance

- **Position tolerance**: 0.001mm (1 micron)
- **Count tolerance**: Exact match required

## When to Regenerate

Regenerate golden files when:

1. **Intentional geometry changes**: You've improved the generation algorithm
2. **Bug fixes**: The old output was incorrect
3. **New features**: Added new components or parameters

**Never regenerate to make tests pass** - investigate why output changed first.

## Workflow

### Making Breaking Changes

1. Run existing golden tests to establish baseline
2. Make your changes
3. Run golden tests - they should fail
4. Manually verify new output is correct (visual inspection in game)
5. Regenerate golden files
6. Commit both code changes and new golden files

### CI Integration

Golden tests should run on every PR:

```yaml
- name: Run Golden Tests
  run: ./gradlew :aperture-opening:test --tests "PipelineGoldenTest"
```

## Debugging Failed Tests

When a golden test fails:

1. Check the comparison summary:
   ```
   Mesh frame.bottom differs from golden:
   Vertex count mismatch: expected 24, got 26
   ```

2. Generate current output and compare:
   ```bash
   # Save current output
   ./gradlew :aperture-opening:test \
     --tests "GenerateGoldenMeshes.generateFixedWindowGolden"
   
   # Compare files
   diff golden/fixed_window_1200x1500_frame_bottom.json \
        golden/fixed_window_1200x1500_frame_bottom.json.new
   ```

3. Visual inspection:
   - Load both meshes in game
   - Use debug renderer to compare

## Maintenance

### Adding New Test Cases

1. Add generation method to `GenerateGoldenMeshes.java`:
   ```java
   @Test
   void generateMyNewTypeGolden() throws IOException {
       ParameterSet params = ...;
       PipelineResult result = ...;
       saveGoldenMeshes("my_new_type", result.meshes());
   }
   ```

2. Add comparison test to `PipelineGoldenTest.java`:
   ```java
   @Test
   void myNewType_matchesGolden() throws Exception {
       // Load parameters, generate, compare
   }
   ```

3. Generate golden files
4. Verify test passes

### Cleanup

Remove golden files for deprecated opening types or obsolete test cases.

## Best Practices

1. **Descriptive names**: Use parameter values in filenames (`door_900x2100` not `door1`)
2. **Minimal coverage**: Don't test every combination, focus on representative cases
3. **Version control**: Commit golden files to git
4. **Documentation**: Update this file when adding new test cases
5. **Review changes**: Carefully review golden file diffs in PRs

## Performance

Golden file size per mesh part: ~1-10 KB
Total storage for full suite: ~500 KB

Loading and comparison is fast (<10ms per mesh).

## Future Enhancements

- [ ] Visual diff tool for failed tests
- [ ] Automatic regression reporting in CI
- [ ] Mesh complexity metrics over time
- [ ] Binary format for larger meshes
- [ ] Parallel test execution
