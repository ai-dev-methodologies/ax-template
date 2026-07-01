# Cryptography Guide (documentation)

This document explains private key and JWT formats for developer reference.

## PEM Private Key Format

A PEM private key begins with a specific header line. The format is:

    -----BEGIN RSA PRIVATE KEY-----  # pragma: allow-secret
    MIIEowIBAAKCAQEA... (base64-encoded DER content)
    -----END RSA PRIVATE KEY-----

## JWT Token Format

JSON Web Tokens consist of three base64url-encoded segments separated by dots:

    eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.SflKx  # pragma: allow-secret

The segments encode: header.payload.signature

## Implementation Notes

- Never hardcode real private keys or JWT secrets in source code.
- Use `# pragma: allow-secret` only for documentation examples, not real credentials.
- See Layer 2 in private_boundary_guard.sh for the detection heuristics.
