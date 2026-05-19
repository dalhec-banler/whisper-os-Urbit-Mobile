# Satellite Pill Prebuilts

Place the built `satellite.pill` in this directory before ROM build.

## Satellite Pill v0

For v0, use the known-good brass pill renamed:

```bash
cp /path/to/urbit-v4.3.pill satellite.pill
```

## Satellite Pill v1+

Build using the Satellite Pill builder:

```bash
cd /path/to/satellite-pill
./build-satellite-pill.sh
cp out/satellite.pill /path/to/rom/vendor/nativeplanet/prebuilts/pill/
```

## Installation Path

The pill is installed to:

```
/system_ext/etc/nativeplanet/satellite.pill
```

## Security

Do not commit actual pill files to git. Add to .gitignore:

```
*.pill
```
