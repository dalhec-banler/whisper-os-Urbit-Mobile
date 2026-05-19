# Security Policy

## Sensitive Material

**DO NOT SUBMIT the following to this repository:**

### Identity & Keys
- `+code` outputs from any urbit
- Private keys (`.key` files, key jams)
- Moon or comet keys
- Planet master tickets
- Azimuth wallet seeds or private keys

### Build Signing
- ROM signing keys (release keys)
- AVB (Android Verified Boot) keys
- `avb_pkmd.bin` from production builds
- Any key material from `keys/` directories

### Urbit Data
- Real pier contents (`/data/nativeplanet/pier/`)
- Event logs from production ships
- `.urb/` directories from real ships
- BootPackages containing real identities

### Build Artifacts
- `target_files.zip` archives
- Factory images (`*-factory-*.zip`)
- OTA update packages
- Any file from `releases/` directory
- `out/` directory contents

### Credentials
- API keys or tokens
- Passwords or passphrases
- OAuth secrets
- Firebase/GCM tokens

## What IS Safe to Commit

- Source code
- Documentation
- Configuration templates (without secrets)
- Test fixtures using fake ships (`~zod`, `~nec`, etc.)
- Build scripts that reference paths (not contents)
- SELinux policy source
- Init service definitions

## If You Accidentally Commit Secrets

1. **Do not push** if you haven't already
2. Remove from git history: `git filter-branch` or `git-filter-repo`
3. If pushed, consider the secret compromised
4. Rotate any exposed keys immediately
5. Contact maintainers if unsure

## For Urbit Identity Exposure

If urbit keys are exposed:
1. Breach your ship immediately from a secure location
2. Rotate to new keys via bridge
3. Notify any affected parties

## Reporting Security Issues

For security vulnerabilities in the codebase:
- Email: security@nativeplanet.io (if available)
- Or open a private security advisory on GitHub

Do not open public issues for security vulnerabilities.

## .gitignore Enforcement

This repository includes `.gitignore` rules to prevent accidental commits of:
- Build outputs
- Key material
- Pier data
- Large binaries

Always verify `git status` before committing.
