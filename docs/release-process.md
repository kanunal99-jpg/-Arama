# ARAMA Release Process

Every completed product feature is released as a new Android version for live-device validation.

## Rule

1. Implement one coherent feature.
2. Validate the changed code and its integration points.
3. Commit to `main`.
4. GitHub Actions builds and signs a new APK.
5. The release workflow publishes the APK and updates `release/latest.json`.
6. The Android OTA client detects the new version.
7. Live-device validation is performed before the next feature starts.

## Versioning

The Android workflow derives `versionCode` from the GitHub Actions run number and `versionName` as `0.1.<run number>`. Therefore each feature release receives a distinct Android version.

## Acceptance gate

A feature is not considered complete merely because its code exists. It must pass build/release validation and be available through OTA so it can be tested on a real device.
