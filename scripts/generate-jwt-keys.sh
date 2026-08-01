#!/usr/bin/env bash

set -Eeuo pipefail

KEY_DIRECTORY="secrets/jwt"
PRIVATE_KEY="${KEY_DIRECTORY}/private.pem"
PUBLIC_KEY="${KEY_DIRECTORY}/public.pem"

mkdir -p "$KEY_DIRECTORY"

echo "[JWT] Generating RSA private key"

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "$PRIVATE_KEY"

echo "[JWT] Generating RSA public key"

openssl rsa \
  -pubout \
  -in "$PRIVATE_KEY" \
  -out "$PUBLIC_KEY"

chmod 600 "$PRIVATE_KEY"
chmod 644 "$PUBLIC_KEY"

echo
echo "[JWT] Keys generated successfully"
echo "Private key: $PRIVATE_KEY"
echo "Public key : $PUBLIC_KEY"