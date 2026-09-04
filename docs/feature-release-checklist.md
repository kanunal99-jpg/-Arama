# Feature Release Checklist

For every ARAMA product feature:

- [ ] Feature implemented in the real product path
- [ ] Existing functionality preserved
- [ ] Backend/API integration verified where applicable
- [ ] Database contract verified where applicable
- [ ] Android/web integration verified where applicable
- [ ] Build passes
- [ ] Release APK signature verified with `apksigner`
- [ ] SHA-256 verified after publication
- [ ] `release/latest.json` points to the published APK
- [ ] OTA detects the new version
- [ ] Live device validation completed

Do not mark a feature complete until the new APK is available for live validation.
