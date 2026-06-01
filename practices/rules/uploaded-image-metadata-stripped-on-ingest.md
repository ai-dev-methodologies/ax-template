---
title: Strip embedded metadata from accepted raster images by re-encoding on ingest
impact: HIGH
impactDescription: "User-uploaded photos carry EXIF/XMP/IPTC blocks — GPS coordinates, device make/model/serial, capture timestamp — that leak personal-location and device PII the instant the image is served publicly; an in-place tag delete is fragile and the only durable fix is to re-encode the pixels and drop every metadata segment"
tags:
  - file-upload
  - privacy
  - exif
  - metadata
  - pii
  - image-processing
spec_ref: "specs/file-storage-l0.yaml#FILE-UPLOAD-004"
verification:
  type: review
  source: "specs/file-storage-l0.yaml#FILE-UPLOAD-004"
  pattern: "Accepted raster image (image/jpeg, image/png) is decoded to a raw raster and re-encoded via ImageIO with no metadata segment before persistence; download bytes contain no EXIF/XMP/IPTC block (no GPSLatitude/GPSLongitude, Make/Model/SerialNumber, DateTimeOriginal)"
upstream:
  - "https://cwe.mitre.org/data/definitions/212.html"
  - "https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html"
evidence:
  - source_type: external
    citation: "CWE-212: Improper Removal of Sensitive Information Before Storage or Transfer"
    url: "https://cwe.mitre.org/data/definitions/212.html"
    quote: "Some formats have well-defined fields that could contain private data, such as Exchangeable image file format (Exif), which can contain potentially sensitive metadata such as geolocation, date, and time."
    quoted_at: "2026-06-01"
---

## Strip embedded metadata from accepted raster images by re-encoding on ingest

**Impact: HIGH — a single accepted photo can leak the uploader's home GPS coordinates and exact device the moment the image is served**

When a fork-receiver accepts user image uploads, the bytes that arrive are not just pixels. Cameras and phones embed EXIF, XMP, and IPTC blocks: GPS latitude/longitude of where the shot was taken, device make/model/serial number, lens, and the original-capture timestamp. CWE-212 calls this out by name — Exif "can contain potentially sensitive metadata such as geolocation, date, and time." If the server stores and re-serves the uploaded file verbatim, every one of those fields ships to anyone who can fetch the public download URL. A profile avatar can disclose the uploader's home address; an attached product photo can disclose a warehouse location and the exact camera used.

The wrong fix is to enumerate and delete known EXIF tags in place. Metadata can live in multiple containers (EXIF, XMP, IPTC, maker-notes, thumbnails that carry their own EXIF copy — see CVE-2005-0406), and an in-place editor that misses one container leaves the PII intact. The durable fix is to **re-encode**: decode the accepted raster to a raw pixel buffer and write a fresh stream that has no metadata segment at all. The OWASP File Upload Cheat Sheet recommends exactly this image-rewriting approach, and it doubles as the polyglot/payload neutralizer — re-encoding destroys injected content as a side effect.

Do this only for raster types ImageIO can both READ and WRITE: `image/jpeg`, `image/png`. WebP is deliberately excluded — stock JDK ImageIO ships no WebP writer (the common TwelveMonkeys plugin is read-only for WebP), so `ImageIO.write(pixels, "webp", out)` returns false; a fork-receiver that accepts WebP uploads MUST strip its metadata with a dedicated libwebp / webp-imageio path or reject WebP, never assume ImageIO round-trips it. Anything not in the raster allowlist is rejected upstream and never reaches the stripper. Run the re-encode on ingest — before the file is persisted or made downloadable — so no path serves the original bytes.

**Incorrect — store the uploaded bytes verbatim; EXIF GPS + device serial ship on every public download:**

```java
// FileStorageService.store(...)
byte[] raw = multipartFile.getBytes();
String key = storageBackend.put(raw, contentType);   // ❌ EXIF/XMP/IPTC persisted intact
StoredFile saved = repository.save(StoredFile.of(key, contentType, raw.length));
// GET /api/files/{id}/download returns raw → GPSLatitude, Make/Model, DateTimeOriginal all exposed
```

**Correct — re-encode accepted raster images to a fresh metadata-free stream on ingest:**

```java
// ImageReEncodeService — invoked by FileStorageService BEFORE persistence
private static final Set<String> RASTER_TYPES =
    Set.of("image/jpeg", "image/png");

byte[] strip(byte[] uploaded, String contentType) throws IOException {
  if (!RASTER_TYPES.contains(contentType)) {
    // out-of-allowlist types never reach here (FILE-UPLOAD-001 rejects them upstream)
    throw new UnsupportedMediaTypeException(contentType);
  }
  BufferedImage pixels = ImageIO.read(new ByteArrayInputStream(uploaded));
  if (pixels == null) {
    throw new InvalidImageException("not a decodable raster image");
  }
  String format = contentType.substring("image/".length());   // jpeg | png
  ByteArrayOutputStream out = new ByteArrayOutputStream();
  // ImageIO.write emits ONLY the pixel data — no EXIF/XMP/IPTC segment survives the decode→encode
  if (!ImageIO.write(pixels, format, out)) {
    throw new UnsupportedMediaTypeException(contentType);
  }
  return out.toByteArray();   // ✅ GPS, Make/Model/Serial, DateTimeOriginal all gone
}
```

Verify positively: upload a JPEG whose EXIF carries real `GPSLatitude`/`GPSLongitude`, `Make`/`Model`/`SerialNumber`, and `DateTimeOriginal`, download it back, and assert the returned bytes contain no EXIF/XMP/IPTC block. This is FILE-UPLOAD-004's acceptance test.

Verification: Decode-and-re-encode every accepted raster image (image/jpeg, image/png) to a metadata-free stream on ingest; download bytes must contain no EXIF/XMP/IPTC fields (no GPSLatitude/GPSLongitude, Make/Model/SerialNumber, DateTimeOriginal) — see specs/file-storage-l0.yaml#FILE-UPLOAD-004.

Reference: [CWE-212: Improper Removal of Sensitive Information Before Storage or Transfer](https://cwe.mitre.org/data/definitions/212.html), [CWE-200: Exposure of Sensitive Information to an Unauthorized Actor](https://cwe.mitre.org/data/definitions/200.html), [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
