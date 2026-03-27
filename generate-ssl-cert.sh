NET::ERR_CERT_AUTHORITY_INVALID#!/bin/bash

# Generate Self-Signed SSL Certificate for Local Development
# This script creates a self-signed certificate valid for 365 days

KEYSTORE_DIR="keystore/ssl"
CERT_FILE="$KEYSTORE_DIR/server.crt"
KEY_FILE="$KEYSTORE_DIR/server.key"

# Create keystore directory if it doesn't exist
mkdir -p "$KEYSTORE_DIR"

# Check if certificates already exist
if [ -f "$CERT_FILE" ] && [ -f "$KEY_FILE" ]; then
    echo "✅ SSL certificates already exist at $KEYSTORE_DIR"
    echo "   Certificate: $CERT_FILE"
    echo "   Private Key: $KEY_FILE"
    exit 0
fi

echo "🔐 Generating self-signed SSL certificate..."

# Generate private key and self-signed certificate
# Valid for 365 days with common name set to localhost
openssl req -x509 -newkey rsa:4096 -keyout "$KEY_FILE" -out "$CERT_FILE" -days 365 -nodes \
    -subj "/C=US/ST=State/L=City/O=Organization/CN=localhost"

if [ $? -eq 0 ]; then
    echo "✅ SSL certificate generated successfully!"
    echo ""
    echo "📁 Files created:"
    echo "   - Private Key: $KEY_FILE"
    echo "   - Certificate: $CERT_FILE"
    echo ""
    echo "⚠️  IMPORTANT for macOS/Windows:"
    echo "   This is a self-signed certificate. Your browser will show a security warning."
    echo "   To trust it locally:"
    echo ""
    echo "   macOS:"
    echo "     sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain $CERT_FILE"
    echo ""
    echo "   Windows (PowerShell as Admin):"
    echo "     Import-Certificate -FilePath '$CERT_FILE' -CertStoreLocation 'Cert:\LocalMachine\Root'"
    echo ""
    echo "   Or simply click 'Proceed' in your browser when accessing https://localhost"
    echo ""
    echo "🚀 You can now start Docker with: docker-compose up"
else
    echo "❌ Error generating SSL certificate"
    exit 1
fi
