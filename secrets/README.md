# Secrets Directory

This directory is for local-only secrets that should **NEVER** be committed to git.

## Contents (local only)

Place the following here for local development:

- `keys/` - ROM signing keys
- `avb/` - AVB key material
- `urbit/` - Test ship keys and +codes
- `bootpackages/` - Real BootPackages

## Security

- This entire directory is gitignored
- Do not remove the `.gitignore` file
- Do not force-add files from this directory
- Treat contents as highly sensitive

## For CI/CD

Secrets for automated builds should be:
- Stored in secure CI secret storage
- Injected at build time
- Never written to repository

## If You Need to Share Keys

- Use encrypted transfer (age, GPG)
- Use secure channels (Signal, in-person)
- Never email or message in plaintext
- Rotate keys if compromise suspected
