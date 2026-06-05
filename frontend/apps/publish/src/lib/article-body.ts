/**
 * Article-body cover encoding.
 *
 * The generic CRUD item DTO (ItemResponse) has only `title` + `description` —
 * there is no dedicated cover-image column. Rather than fork the backend domain,
 * the studio encodes the cover as a leading <figure data-ax-cover> wrapping an
 * <img> at the very top of the HTML body stored in `description`. This keeps the
 * cover inside the single body field, round-trips through the editor cleanly, and
 * renders for free in the read view's prose.
 *
 * These helpers are pure string transforms (no DOM) so they run in both the
 * editor (client) and the list cards (server-rendered prose excerpt).
 */

const COVER_RE = /^\s*<figure data-ax-cover[^>]*>\s*<img[^>]*src="([^"]*)"[^>]*>\s*<\/figure>/i;

/** Extract the cover image URL from a body, or null if there is none. */
export function extractCover(body: string | null | undefined): string | null {
  if (!body) return null;
  const match = COVER_RE.exec(body);
  return match ? match[1] : null;
}

/** Return the body with any leading cover figure removed (the editable content). */
export function stripCover(body: string | null | undefined): string {
  if (!body) return '';
  return body.replace(COVER_RE, '').trimStart();
}

/**
 * Compose the persisted body: an optional leading cover figure + the editor's
 * HTML content. The src is attribute-escaped (it comes from our own upload
 * response, but escaping keeps the markup well-formed).
 */
export function composeBody(content: string, coverUrl: string | null): string {
  const inner = content ?? '';
  if (!coverUrl) return inner;
  const safe = coverUrl.replace(/&/g, '&amp;').replace(/"/g, '&quot;');
  return `<figure data-ax-cover><img src="${safe}" alt="" /></figure>${inner}`;
}
