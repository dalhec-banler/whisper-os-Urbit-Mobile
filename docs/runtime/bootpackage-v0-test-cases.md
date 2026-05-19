# BootPackage v0 Test Cases

Test cases for the BootPackage parser in `nativeplanet-vere-launch.c`.

## Running Tests

Compile and run the host-side test harness:

```bash
cd rom/vendor/nativeplanet/src
gcc -Wall -Werror -o test-bootpackage-parser test-bootpackage-parser.c
./test-bootpackage-parser
```

## Test Categories

### Valid Input

| Test | Description | Expected |
|------|-------------|----------|
| valid_bootpackage | Complete valid BootPackage JSON | Parse OK |
| valid_moon_ship_name | Ship name with `~` prefix | Parse OK |

### Missing Required Fields

| Test | Description | Expected |
|------|-------------|----------|
| missing_ship | No "ship" field | Fail: missing 'ship' |
| missing_pillPath | No "pillPath" field | Fail: missing 'pillPath' |
| missing_pierPath | No "pierPath" field | Fail: missing 'pierPath' |
| missing_bootMode | No "bootMode" field | Fail: missing 'bootMode' |
| missing_keyMaterialRef | No "keyMaterialRef" field | Fail: missing 'keyMaterialRef' |
| missing_packageVersion | No "packageVersion" field | Fail: missing 'packageVersion' |

### Boot Mode Validation

| Test | Description | Expected |
|------|-------------|----------|
| unsupported_bootMode_MOON | bootMode = "MOON" | Fail: MOON not supported in v0 |
| unknown_bootMode | bootMode = "INVALID" | Fail: unknown bootMode |

### Key Material Validation

| Test | Description | Expected |
|------|-------------|----------|
| keyMaterialRef_not_none_in_FAKE_TEST | FAKE_TEST with keyMaterialRef != "none" | Fail: FAKE_TEST requires none |
| keyMaterialRef_looks_like_secret | 64-char hex string as keyMaterialRef | Fail: raw key material detected |

### Path Security

| Test | Description | Expected |
|------|-------------|----------|
| path_traversal_pierPath | `../` in pierPath | Fail: invalid characters |
| path_traversal_pillPath | `../` in pillPath | Fail: invalid characters |
| pillPath_wrong_prefix | pillPath outside /system_ext/etc/nativeplanet/ | Fail: wrong prefix |
| pierPath_wrong_prefix | pierPath outside /data/nativeplanet/ships/ | Fail: wrong prefix |
| control_char_in_path | Tab character in path | Fail: invalid characters |

### Ship Name Validation

| Test | Description | Expected |
|------|-------------|----------|
| invalid_ship_uppercase | Uppercase ship name | Fail: invalid characters |
| invalid_ship_spaces | Space in ship name | Fail: invalid characters |

### Version Validation

| Test | Description | Expected |
|------|-------------|----------|
| unsupported_packageVersion | packageVersion = 99 | Fail: not supported |

### Malformed JSON

| Test | Description | Expected |
|------|-------------|----------|
| malformed_json_no_quotes | JSON without quoted keys | Fail: missing field |
| empty_json | `{}` | Fail: missing field |

## Security Properties

The parser enforces:

1. **Fail closed**: Any validation error stops boot, no defaults guessed
2. **Path allowlist**: Only specific prefixes allowed for pill/pier paths
3. **No path traversal**: `..` rejected in all paths
4. **No control characters**: Chars < 0x20 or 0x7f rejected in paths
5. **Secret detection**: Hex strings > 50 chars, `~` sigils, `0x` prefixes rejected
6. **No raw keys**: keyMaterialRef must be "none" for FAKE_TEST
7. **MOON blocked**: bootMode=MOON rejected in v0
8. **Version check**: Only packageVersion=1 accepted

## V0 Schema

```json
{
  "ship": "zod",
  "parent": null,
  "pillPath": "/system_ext/etc/nativeplanet/satellite.pill",
  "pierPath": "/data/nativeplanet/ships/zod",
  "bootMode": "FAKE_TEST",
  "keyMaterialRef": "none",
  "networkConfig": {},
  "delegationConfig": {},
  "createdAtMs": 0,
  "packageVersion": 1
}
```

Required fields: `ship`, `pillPath`, `pierPath`, `bootMode`, `keyMaterialRef`, `packageVersion`

Optional fields (ignored in v0): `parent`, `networkConfig`, `delegationConfig`, `createdAtMs`
