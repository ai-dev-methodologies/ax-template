---
title: A chat system MUST carry a typed message envelope with delivery/read receipts, room lifecycle, presence, paginated offline-catchup history, and moderation
impact: MEDIUM
impactDescription: "A chat without delivery/read receipts cannot tell the sender whether a message arrived; without per-room offline catch-up, a user who was offline loses messages or re-reads the whole history; without a typed envelope (id, sender, room, timestamp), dedup and ordering are impossible; without moderation + reporting, abuse has no remedy. Each gap degrades a messaging product from reliable to lossy."
tags:
  - chat
  - messaging
  - delivery-receipt
  - presence
  - moderation
  - realtime
spec_ref: "specs/chat-messaging-l0.yaml#CHAT-DELIVERY-001"
verification:
  type: review
  source: "specs/chat-messaging-l0.yaml#CHAT-DELIVERY-001"
  pattern: "A chat system MUST define a typed message envelope — stable message id, sender, room/conversation id, server timestamp (RFC 3339), body, type — so messages dedup and order deterministically (CHAT-MESSAGE-001). Rooms have an explicit lifecycle and type (1:1 / group / broadcast) with membership controlling visibility (CHAT-ROOM-001). Delivery guarantees are explicit: a sender can request a delivery receipt (message reached a recipient client) and a read receipt, distinct states (sent / delivered / read) (CHAT-DELIVERY-001). Presence + typing indicators communicate whether a participant is active/composing/away (CHAT-PRESENCE-001). Message history is paginated and supports offline catch-up — a reconnecting client fetches exactly the messages it missed via a cursor/since token, never the whole history and never gaps (CHAT-HISTORY-001). Moderation supports reporting, takedown, and youth-protection handling of abusive content (CHAT-MODERATION-001). Reject a chat with no message id (undedupable), a single global stream with no room scoping, fire-and-forget delivery with no receipt, and history with no pagination cursor."
upstream:
  - "https://xmpp.org/extensions/xep-0184.html"
  - "https://xmpp.org/extensions/xep-0085.html"
evidence:
  - source_type: external
    citation: "XMPP XEP-0184: Message Delivery Receipts (Abstract)"
    url: "https://xmpp.org/extensions/xep-0184.html"
    quote: "This specification defines an XMPP protocol extension for message delivery receipts, whereby the sender of a message can request notification that the message has been delivered to a client controlled by the intended recipient."
    quoted_at: "2026-06-06"
  - source_type: external
    citation: "XMPP XEP-0085: Chat State Notifications (Abstract)"
    url: "https://xmpp.org/extensions/xep-0085.html"
    quote: "This document defines an XMPP protocol extension for communicating the status of a user in a chat session, thus indicating whether a chat partner is actively engaged in the chat, composing a message, temporarily paused, inactive, or gone."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A chat system MUST carry a typed envelope with delivery/read receipts, rooms, presence, paginated offline-catchup history, and moderation

**Impact: MEDIUM — A chat product lives or dies on reliability the user can see. Delivery and read receipts are a defined protocol concern — XEP-0184 *defines an XMPP protocol extension for message delivery receipts, whereby the sender of a message can request notification that the message has been delivered to a client controlled by the intended recipient* — and presence/typing likewise — XEP-0085 communicates *whether a chat partner is actively engaged in the chat, composing a message, temporarily paused, inactive, or gone*. Skip the typed envelope and messages cannot be deduped or ordered; skip offline catch-up and a reconnecting user silently loses messages or re-downloads everything; skip receipts and the sender never knows if anything arrived; skip moderation and abuse has no remedy.**

There are six load-bearing requirements — the items of `specs/chat-messaging-l0.yaml`, all governed by this rule.

**1. Typed message envelope (CHAT-MESSAGE-001).** Every message carries a stable id, sender, room/conversation id, a server-assigned RFC 3339 timestamp, body, and type. The id makes redelivery dedupable; the server timestamp makes ordering deterministic (never the client clock).

**2. Room lifecycle (CHAT-ROOM-001).** Conversations are rooms with an explicit type (1:1, group, broadcast) and lifecycle (create/join/leave/close); membership controls who sees the messages — there is no single global stream.

**3. Delivery + read receipts (CHAT-DELIVERY-001).** A message moves through explicit, distinct states — sent → delivered (reached a recipient client) → read — and the sender can request receipts, so "did it arrive?" has an answer instead of fire-and-forget.

**4. Presence + typing (CHAT-PRESENCE-001).** Presence (online/away) and typing indicators (composing/paused) are communicated per the chat-state model, so participants see live engagement.

**5. Paginated history + offline catch-up (CHAT-HISTORY-001).** History is paginated by a cursor/since token; a reconnecting client requests exactly the messages it missed since its last seen id — never the entire history, never with gaps.

**6. Moderation (CHAT-MODERATION-001).** Reporting, takedown of abusive content, and youth-protection (청소년보호) handling are supported, so abuse has an enforced remedy.

**Incorrect — no message id, one global stream, fire-and-forget, full-history fetch on reconnect:**

```java
// VIOLATION: no id (undedupable), no room (CHAT-MESSAGE/ROOM); broadcast to everyone
void send(String from, String text) { bus.publishToAll(new Msg(from, text, clientClock.now())); }
// VIOLATION: reconnect pulls the ENTIRE history, no cursor (CHAT-HISTORY-001); no receipts (CHAT-DELIVERY-001)
List<Msg> onReconnect(String user) { return store.findAll(); }
```

**Correct — typed envelope, room-scoped, receipts, cursor-based offline catch-up:**

```java
record ChatMessage(String id, String roomId, String senderId,   // typed envelope (CHAT-MESSAGE-001)
                   Instant serverTs, String type, String body) {}

ChatMessage send(String roomId, String senderId, String body) {
    requireMember(roomId, senderId);                            // room membership (CHAT-ROOM-001)
    ChatMessage m = new ChatMessage(UUID.randomUUID().toString(), roomId, senderId,
                                    clock.now(), "text", body); // server timestamp
    store.append(m);
    receipts.markSent(m.id());                                  // sent → delivered → read (CHAT-DELIVERY-001)
    return m;
}
Page<ChatMessage> catchUp(String roomId, String sinceMessageId, int limit) { // offline catch-up (CHAT-HISTORY-001)
    return store.findInRoomAfter(roomId, sinceMessageId, limit);             // cursor, not findAll
}
```

Verification: review-tier. Messaging reliability is a protocol property — a fire-and-forget chat works in a demo and loses messages under real reconnects. Verify by review against `specs/chat-messaging-l0.yaml`: a typed envelope with id + server timestamp; room-scoped membership; explicit sent/delivered/read receipts; presence + typing; cursor-paginated offline catch-up; moderation/reporting/youth-protection. When a fork-receiver wires a real IT (offline client reconnects and receives exactly the missed messages; a delivery receipt transitions state), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [XMPP XEP-0184 — Message Delivery Receipts](https://xmpp.org/extensions/xep-0184.html)

Reference: [XMPP XEP-0085 — Chat State Notifications](https://xmpp.org/extensions/xep-0085.html)
