# PackBridge agent guidance

PackBridge is a client-side Java/Fabric mod supporting Minecraft 26.1.2 and
26.2. It adapts classic resource-pack textures inside a read-only per-pack view.

## Multi-version architecture

- Keep all active Minecraft targets on `main`.
- Treat `gradle/targets.properties` as the supported target catalog.
- Keep shared implementation and resources under `src/main`.
- Put only compile-time API differences under `src/<minecraft-version>/java`.
- Keep `versions/<target>/build.gradle.kts` as marker files rather than copies
  of the mod.
- All targets share `mod_version`; `releaseManifest` generates publishing
  metadata from the same catalog.
- Build every active target before considering changes complete.

## Resource adaptation

- Preserve the original pack files, namespace boundaries, and pack priority.
- Leave newer packs unchanged. Adapt only packs with format 1–4 and assets.
- Keep explicit modern resources ahead of historic path aliases.
- Keep `getResource` and `listResources` consistent so atlas loading can see
  every generated texture.
- Bound conversion caches and discard them on close; never reuse content from
  a previous pack instance after a reload.
- Test chest UVs and sheet coordinates with synthetic pixel fixtures. Do not
  include third-party pack assets in tests or release JARs.
- Fabric API and Fabric Language Kotlin are not needed. Keep Gradle,
  `fabric.mod.json`, and publishing dependencies synchronized.

## Release policy

- Only pushed `v*` tags trigger `.github/workflows/release.yml`.
- The tag must equal `v<mod_version>` and every target must build successfully.
- The release job uses the repository-scoped labels `self-hosted`, `Linux`,
  `X64`, `wicked-game-01`, and `packbridge`.
- The `release` environment owns the `MODRINTH_TOKEN` secret and
  `MODRINTH_PROJECT_ID` variable. Never commit credentials.
- Normal branch pushes and pull requests do not build or publish in Actions.
- Create local conventional commits at coherent verified checkpoints; push
  only when authorized by the user.

## Verification

Run `./gradlew build releaseManifest` (`.\gradlew.bat` on Windows). Tests run
against both Minecraft targets and cover pack metadata, zip/folder loading,
texture discovery and priority, model references, high-resolution sprites,
chest UVs, malformed images, and reload behavior.
