# 08 — Asset System

**Layer**: Kernel  
**Status**: 🎯 CRITICAL — Content Management  
**Dependencies**: None (pure kernel)

---

## Overview

**Everything is an Asset.**

The Asset System is Aperture's content management foundation. Assets are reusable data resources:
- **Profiles** — Cross-section shapes (frame profiles, mullion profiles)
- **Materials** — Surface appearance (oak wood, clear glass, iron hardware)
- **Opening Types** — Opening family definitions (fixed_window, single_door)
- **Presets** — Pre-configured parameter sets (standard sizes, common combinations)
- **Templates** — Complete opening configurations
- **Geometry** — (Future) Reusable 3D shapes (handles, hinges, decorative elements)

**Design Principle**: Assets are **versioned, cataloged, and hot-reloadable**. An asset is referenced by ID, never by direct import.

**This is NOT hardcoded resources. This IS a dynamic content system.**

---

## Why Asset System?

### Without Asset System

```java
// Hardcoded profiles
class FrameGenerator {
    private static final Profile STANDARD_FRAME = new Profile(...);
    private static final Profile DOOR_FRAME = new Profile(...);
    // ...50 hardcoded profiles
}

// Hardcoded materials
class MaterialResolver {
    if (id.equals("oak")) return new Material(...);
    if (id.equals("glass")) return new Material(...);
    // ...100 if statements
}
```

**Problems**:
- Can't add new content without code changes
- Can't hot-reload during development
- Can't version content separately from code
- Can't share content between mods
- Can't let users create custom content

### With Asset System

```json
// aperture-data/aperture/profiles/frame_standard_50.json
{
  "id": "aperture:frame_standard_50",
  "type": "L-profile",
  "width": 50,
  "depth": 80,
  "vertices": [...]
}

// aperture-data/aperture/materials/oak_frame.json
{
  "id": "aperture:oak_frame",
  "blocks": ["minecraft:oak_planks"],
  "textures": {
    "all": "minecraft:block/oak_planks"
  }
}
```

**Benefits**:
- ✅ Add content via JSON (no code)
- ✅ Hot-reload during development
- ✅ Version content independently
- ✅ Data packs / addon mods extend catalogs
- ✅ Users can create custom profiles/materials

---

## Asset Types

### 1. Profile Assets

**Purpose**: Define 2D cross-section shapes for extrusion.

**Schema**:
```json
{
  "id": "aperture:frame_l_50x80",
  "type": "profile",
  "category": "frame",
  "shape": "L-profile",
  "dimensions": {
    "width": 50,
    "depth": 80,
    "thickness": 10
  },
  "vertices": [
    { "x": 0, "y": 0 },
    { "x": 50, "y": 0 },
    { "x": 50, "y": 10 },
    { "x": 10, "y": 10 },
    { "x": 10, "y": 80 },
    { "x": 0, "y": 80 }
  ],
  "closed": true
}
```

**Catalog**: `aperture-data/aperture/profiles/`

**Reference**:
```json
{
  "components": [
    { "kind": "frame", "profile": "aperture:frame_l_50x80" }
  ]
}
```

**Current Status**: ✅ Schema exists, ⏳ Loader partial

---

### 2. Material Assets

**Purpose**: Define surface appearance (blocks, textures, colors).

**Schema**:
```json
{
  "id": "aperture:oak_frame",
  "type": "material",
  "category": "wood",
  "display_name": "Oak Wood",
  "blocks": ["minecraft:oak_planks"],
  "textures": {
    "all": "minecraft:block/oak_planks"
  },
  "properties": {
    "color": "#c9945f",
    "roughness": 0.7,
    "metallic": 0.0
  }
}
```

**Catalog**: `aperture-data/aperture/materials/`

**Reference**:
```json
{
  "parameters": {
    "frame_material": { "type": "material", "default": "aperture:oak_frame" }
  }
}
```

**Current Status**: ✅ Schema exists, ✅ Loader complete

---

### 3. Opening Type Assets

**Purpose**: Define opening family (door, window, etc.).

**Schema**:
```json
{
  "id": "aperture:fixed_window",
  "type": "opening_type",
  "category": "window",
  "display_name": "Fixed Window",
  "parameters": {
    "width": { "type": "length", "default": 1200, "min": 300, "max": 6000 },
    "height": { "type": "length", "default": 1500, "min": 300, "max": 4000 }
  },
  "constraints": [...],
  "generator": "aperture:rectangular_window_v1",
  "components": [...],
  "materialSlots": ["frame", "glazing"]
}
```

**Catalog**: `aperture-data/aperture/opening_types/`

**Reference**: By ID in placement/editor

**Current Status**: ✅ Schema exists, ✅ Loader complete

---

### 4. Preset Assets

**Purpose**: Pre-configured parameter sets (standard sizes, common configs).

**Schema**:
```json
{
  "id": "aperture:window_standard_1200x1500",
  "type": "preset",
  "for_type": "aperture:fixed_window",
  "display_name": "Standard Window (1200×1500)",
  "parameters": {
    "width": 1200,
    "height": 1500,
    "frame_material": "aperture:oak_frame",
    "glass_material": "aperture:glazing_clear"
  }
}
```

**Catalog**: `aperture-data/aperture/presets/`

**Reference**: In catalog browser UI

**Current Status**: ⏸️ Planned

---

### 5. Template Assets

**Purpose**: Complete opening configurations (placement + parameters + materials).

**Schema**:
```json
{
  "id": "aperture:entrance_door_oak",
  "type": "template",
  "display_name": "Oak Entrance Door",
  "opening_type": "aperture:single_door",
  "parameters": {
    "width": 900,
    "height": 2100,
    "frame_material": "aperture:oak_frame",
    "panel_material": "aperture:oak_panel",
    "handle": "lever"
  },
  "preview_image": "aperture:textures/template/entrance_door_oak.png"
}
```

**Catalog**: `aperture-data/aperture/templates/`

**Reference**: In catalog browser UI

**Current Status**: ⏸️ Planned

---

### 6. Geometry Assets (Future)

**Purpose**: Reusable 3D geometry (handles, hinges, decorative elements).

**Schema**:
```json
{
  "id": "aperture:handle_lever_modern",
  "type": "geometry",
  "category": "hardware",
  "format": "obj",
  "file": "aperture:models/hardware/lever_modern.obj",
  "scale": 1.0,
  "anchor": { "x": 0, "y": 0, "z": 0 }
}
```

**Catalog**: `aperture-data/aperture/geometry/`

**Status**: ⏸️ Planned for Phase 4+

---

## Asset Identification

### Asset ID

**Format**: `namespace:path`

**Examples**:
- `aperture:frame_l_50x80`
- `aperture:oak_frame`
- `mymod:custom_window`

**Rules**:
1. Namespace: `[a-z0-9_-]+`
2. Path: `[a-z0-9_/-]+`
3. Case-sensitive (conventionally lowercase)

### Namespaces

**Built-in**:
- `aperture:` — Core assets shipped with Aperture
- `minecraft:` — Vanilla Minecraft blocks/textures

**Third-party**:
- `mymod:` — Addon mod assets
- `datapack_name:` — Data pack assets

**Conflict Resolution**:
- Later-loaded namespaces override earlier (data pack > mod > core)
- Within same namespace, last definition wins

---

## Asset Catalog

### Catalog Interface

```java
public interface AssetCatalog<T extends Asset> {
    /**
     * Register an asset.
     */
    void register(AssetId id, T asset);
    
    /**
     * Get asset by ID.
     */
    Optional<T> get(AssetId id);
    
    /**
     * Get all assets.
     */
    Collection<T> getAll();
    
    /**
     * Get assets by category.
     */
    Collection<T> getByCategory(String category);
    
    /**
     * Check if asset exists.
     */
    boolean contains(AssetId id);
}
```

### Asset Registry

**Central registry for all asset types**:

```java
public class AssetRegistry {
    private final AssetCatalog<ProfileDefinition> profiles = new SimpleCatalog<>();
    private final AssetCatalog<MaterialDefinition> materials = new SimpleCatalog<>();
    private final AssetCatalog<OpeningTypeDefinition> openingTypes = new SimpleCatalog<>();
    private final AssetCatalog<PresetDefinition> presets = new SimpleCatalog<>();
    
    public AssetCatalog<ProfileDefinition> profiles() {
        return profiles;
    }
    
    public AssetCatalog<MaterialDefinition> materials() {
        return materials;
    }
    
    public AssetCatalog<OpeningTypeDefinition> openingTypes() {
        return openingTypes;
    }
    
    public AssetCatalog<PresetDefinition> presets() {
        return presets;
    }
}
```

---

## Asset Loading

### Loader Pipeline

```
Discover Assets (scan directories)
    ↓
Parse JSON (validate schema)
    ↓
Resolve References (link to other assets)
    ↓
Register (add to catalog)
    ↓
Notify Listeners (hot-reload hooks)
```

### Asset Loader

```java
public class AssetLoader {
    private final AssetRegistry registry;
    private final Map<String, AssetCodec<?>> codecs;
    
    /**
     * Load all assets from a directory.
     */
    public void loadFromDirectory(Path directory) {
        try (var stream = Files.walk(directory)) {
            stream.filter(path -> path.toString().endsWith(".json"))
                  .forEach(this::loadAssetFile);
        }
    }
    
    private void loadAssetFile(Path path) {
        try {
            var json = Files.readString(path);
            var node = parseJson(json);
            var type = node.get("type").asText();
            
            var codec = codecs.get(type);
            if (codec == null) {
                logger.warn("Unknown asset type: {}", type);
                return;
            }
            
            var asset = codec.decode(node);
            registerAsset(asset);
            
        } catch (Exception e) {
            logger.error("Failed to load asset: {}", path, e);
        }
    }
    
    private void registerAsset(Asset asset) {
        switch (asset) {
            case ProfileDefinition profile -> 
                registry.profiles().register(profile.id(), profile);
            case MaterialDefinition material -> 
                registry.materials().register(material.id(), material);
            case OpeningTypeDefinition openingType -> 
                registry.openingTypes().register(openingType.id(), openingType);
            // ...
        }
    }
}
```

---

## Asset References

### Reference Types

**Direct Reference** (most common):
```json
{
  "profile": "aperture:frame_l_50x80"
}
```

**Reference with Fallback**:
```json
{
  "profile": {
    "id": "mymod:custom_frame",
    "fallback": "aperture:frame_standard_50"
  }
}
```

**Reference with Override** (future):
```json
{
  "profile": {
    "base": "aperture:frame_l_50x80",
    "overrides": {
      "width": 60
    }
  }
}
```

### Reference Resolution

```java
public class AssetReferenceResolver {
    private final AssetRegistry registry;
    
    public <T extends Asset> T resolve(AssetRef<T> ref, Class<T> assetClass) {
        var catalog = getCatalog(assetClass);
        
        // Try primary ID
        var primary = catalog.get(ref.id());
        if (primary.isPresent()) {
            return primary.get();
        }
        
        // Try fallback
        if (ref.hasFallback()) {
            var fallback = catalog.get(ref.fallback());
            if (fallback.isPresent()) {
                logger.warn("Asset {} not found, using fallback {}", 
                    ref.id(), ref.fallback());
                return fallback.get();
            }
        }
        
        throw new AssetNotFoundException("Asset not found: " + ref.id());
    }
}
```

---

## Asset Versioning

### Schema Version

**Every asset carries a schema version**:

```json
{
  "schemaVersion": 1,
  "id": "aperture:fixed_window",
  ...
}
```

### Migration

**When schema changes, migrate old assets**:

```java
public interface AssetMigration<T extends Asset> {
    int fromVersion();
    int toVersion();
    T migrate(T oldAsset);
}

public class ProfileMigrationV1toV2 implements AssetMigration<ProfileDefinition> {
    public int fromVersion() { return 1; }
    public int toVersion() { return 2; }
    
    public ProfileDefinition migrate(ProfileDefinition old) {
        // Example: V2 adds "closed" field, default to true
        return new ProfileDefinition(
            old.id(),
            2,  // new schema version
            old.shape(),
            old.vertices(),
            true  // closed (new field)
        );
    }
}
```

**Migration Chain**:
```
Asset v1 → Migration v1→v2 → Asset v2 → Migration v2→v3 → Asset v3
```

**Current Status**: ⏸️ Migration framework planned, not implemented

---

## Hot Reload

**Purpose**: Reload assets without restarting the game (developer QOL).

### File Watcher

```java
public class AssetHotReloader {
    private final WatchService watchService;
    private final AssetLoader loader;
    private final AssetRegistry registry;
    
    public void startWatching(Path directory) throws IOException {
        directory.register(watchService, ENTRY_MODIFY, ENTRY_CREATE, ENTRY_DELETE);
        
        while (true) {
            var key = watchService.take();
            for (var event : key.pollEvents()) {
                if (event.kind() == ENTRY_MODIFY || event.kind() == ENTRY_CREATE) {
                    var path = (Path) event.context();
                    reloadAsset(directory.resolve(path));
                }
            }
            key.reset();
        }
    }
    
    private void reloadAsset(Path path) {
        logger.info("Reloading asset: {}", path);
        loader.loadAssetFile(path);
        notifyListeners(path);
    }
}
```

### Reload Listeners

```java
public interface AssetReloadListener {
    void onAssetReloaded(AssetId id, Asset newAsset);
}

// Example: Invalidate pipeline cache when opening type reloaded
public class PipelineCacheInvalidator implements AssetReloadListener {
    @Override
    public void onAssetReloaded(AssetId id, Asset newAsset) {
        if (newAsset instanceof OpeningTypeDefinition) {
            pipelineCache.invalidate(id);
        }
    }
}
```

**Current Status**: ⏸️ Planned for development tooling

---

## Asset Validation

### Schema Validation

**Validate JSON against schema**:

```java
public class AssetValidator {
    private final Map<String, JsonSchema> schemas;
    
    public ValidationResult validate(String json) {
        var node = parseJson(json);
        var type = node.get("type").asText();
        
        var schema = schemas.get(type);
        if (schema == null) {
            return ValidationResult.invalid("Unknown asset type: " + type);
        }
        
        var errors = schema.validate(node);
        return errors.isEmpty() 
            ? ValidationResult.valid()
            : ValidationResult.invalid(errors);
    }
}
```

**Schemas**: `docs/schemas/*.schema.json`

**Current Status**: ✅ Schemas exist, ⏳ Validator partial

---

### Reference Validation

**Check that all asset references are valid**:

```java
public class AssetReferenceValidator {
    private final AssetRegistry registry;
    
    public ValidationResult validateReferences(Asset asset) {
        var issues = new ArrayList<ValidationIssue>();
        
        // Extract all asset references
        var refs = extractReferences(asset);
        
        for (var ref : refs) {
            if (!registry.contains(ref.id())) {
                issues.add(ValidationIssue.error(
                    "Referenced asset not found: " + ref.id()
                ));
            }
        }
        
        return new ValidationResult(issues.isEmpty(), issues);
    }
}
```

---

## Current Status

| Asset Type | Schema | Loader | Catalog | Validation | Hot Reload |
|------------|--------|--------|---------|------------|------------|
| Profile | ✅ | ⏳ | ✅ | ⏳ | ❌ |
| Material | ✅ | ✅ | ✅ | ⏳ | ❌ |
| Opening Type | ✅ | ✅ | ✅ | ✅ | ❌ |
| Preset | ⏸️ | ❌ | ❌ | ❌ | ❌ |
| Template | ⏸️ | ❌ | ❌ | ❌ | ❌ |
| Geometry | ⏸️ | ❌ | ❌ | ❌ | ❌ |

---

## Acceptance Criteria

### For Kernel V1 (Week 2)
- [x] Profile schema defined
- [x] Material schema defined
- [x] Opening Type schema defined
- [ ] Profile loader complete
- [ ] AssetRegistry unified
- [ ] Reference resolution works

### For Platform V1 (Phase B)
- [ ] Preset assets (standard sizes)
- [ ] Template assets (catalog browser)
- [ ] Asset reference validation
- [ ] Schema validation on load

### For Production (Phase C+)
- [ ] Hot reload for development
- [ ] Asset migration framework
- [ ] Geometry assets (handles, hinges)
- [ ] User-generated content support

---

## Integration Examples

### Example 1: Load Profile in Generator

```java
public class FrameComponentGenerator {
    private final AssetRegistry registry;
    
    public GeometrySolid generateFrame(FrameComponent component) {
        var profileId = component.properties().getString("profile");
        var profile = registry.profiles()
            .get(AssetId.of(profileId))
            .orElseThrow(() -> new AssetNotFoundException(profileId));
        
        // Use profile to generate frame geometry
        return extrudeProfile(profile, frameBounds);
    }
}
```

### Example 2: Resolve Material

```java
public class MaterialBindingService {
    private final AssetRegistry registry;
    
    public MaterialInstance resolveMaterial(String materialId) {
        var material = registry.materials()
            .get(AssetId.of(materialId))
            .orElseGet(() -> registry.materials().get(AssetId.of("aperture:default")).get());
        
        return new MaterialInstance(
            material.blocks(),
            material.textures(),
            material.properties()
        );
    }
}
```

### Example 3: Catalog Browser UI

```java
public class CatalogBrowserScreen {
    private final AssetRegistry registry;
    
    public void renderOpeningTypes() {
        var openingTypes = registry.openingTypes().getAll();
        
        for (var type : openingTypes) {
            var button = new Button(
                type.displayName(),
                () -> selectOpeningType(type)
            );
            addButton(button);
        }
    }
}
```

---

## Testing Strategy

### Asset Loading Tests

```java
@Test
void assetLoader_validProfile_loadsSuccessfully() {
    var loader = new AssetLoader(registry);
    var path = Path.of("test_assets/profile_standard.json");
    
    loader.loadAssetFile(path);
    
    assertTrue(registry.profiles().contains(AssetId.of("test:profile_standard")));
}

@Test
void assetLoader_invalidJson_logsError() {
    var loader = new AssetLoader(registry);
    var path = Path.of("test_assets/invalid.json");
    
    loader.loadAssetFile(path);  // Should not throw
    
    // Check logs for error message
}
```

### Reference Resolution Tests

```java
@Test
void referenceResolver_existingAsset_resolves() {
    registry.profiles().register(
        AssetId.of("test:profile"), 
        testProfile
    );
    var resolver = new AssetReferenceResolver(registry);
    
    var resolved = resolver.resolve(
        AssetRef.of("test:profile"), 
        ProfileDefinition.class
    );
    
    assertEquals(testProfile, resolved);
}

@Test
void referenceResolver_missingAsset_throwsException() {
    var resolver = new AssetReferenceResolver(registry);
    
    assertThrows(AssetNotFoundException.class, () -> 
        resolver.resolve(AssetRef.of("test:nonexistent"), ProfileDefinition.class)
    );
}
```

---

## Related Documents

- [kernel/02-parameter-engine.md](02-parameter-engine.md) — Material parameters reference assets
- [kernel/03-component-system.md](03-component-system.md) — Components reference profiles
- [kernel/04-generation-pipeline.md](04-generation-pipeline.md) — Pipeline uses assets

---

**Document Status**: ✅ Complete  
**Last Updated**: 2026-07-16  
**Implementation**: ~40% (material/opening type complete, others partial)  
**Next Review**: After Platform V1
