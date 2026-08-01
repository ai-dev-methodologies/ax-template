# fixture transcript — fail_tampered_transcript

TAMPERED: these bytes were edited after the manifest's transcript_sha256 was recorded, which is
exactly the P3-108 attack this guard exists to catch. The manifest still names the ORIGINAL
sha256 (copied byte-for-byte from pass_clean's manifest); this file's disk sha256 no longer
matches it.
