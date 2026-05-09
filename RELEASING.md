# Releasing

## Generate A Keystore

Create a release keystore and keep it outside source control:

```bash
keytool -genkeypair \
  -v \
  -keystore teledrive-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias teledrive
```

## Local Signing

For local release builds, create `keystore.properties` at the project root:

```properties
KEYSTORE_FILE=/absolute/path/to/teledrive-release.jks
KEY_ALIAS=teledrive
KEY_PASSWORD=your-key-password
STORE_PASSWORD=your-store-password
```

`keystore.properties`, `*.jks`, and `*.keystore` are ignored by Git.

## GitHub Secrets

In GitHub, open the repository settings and add these Actions secrets:

- `KEYSTORE_FILE`: path used by the CI workflow after it materializes the keystore.
- `KEY_ALIAS`: keystore alias, for example `teledrive`.
- `KEY_PASSWORD`: password for the key alias.
- `STORE_PASSWORD`: password for the keystore.

If the workflow stores the keystore bytes as a separate secret, keep that secret name aligned with the existing release workflow.

## Trigger A Release

Create and push a version tag:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The existing release workflow handles the signed release build when a `v*` tag is pushed.
