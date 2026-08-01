# caffeine-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://github.com/ben-manes/caffeine/wiki/Eviction (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:23:42Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://github.com/ben-manes/caffeine/wiki/Eviction`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r112`
**Body SHA-256 (below the `---` divider, header excluded):** c93c5685f848fb90b2f4a7064c19dcb34b79fdad182552e40d56ff46b4168989

---

---
snapshot_id: caffeine-2026-05
source: "https://github.com/ben-manes/caffeine/wiki/Eviction"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 6372
sha: "02c344fa21cb7b6798956bed366b4df5d6d2d249bd417f44957719df840d2338"
---

# caffeine 2026 05 — upstream snapshot

Source: https://github.com/ben-manes/caffeine/wiki/Eviction
Fetched: 2026-07-14

Eviction · ben-manes/caffeine Wiki · GitHub
Skip to content

## Navigation Menu
Sign in
Appearance settings

# Search code, repositories, users, issues, pull requests...
Search syntax tips

# Provide feedback

# Saved searches

## Use saved searches to filter your results more quickly
Sign in
//wiki/show;ref_cta:Sign up;ref_loc:header logged out"}"
 >
 Sign up
Appearance settings
You signed in with another tab or window. Reload to refresh your session.
 You signed out in another tab or window. Reload to refresh your session.
 You switched accounts on another tab or window. Reload to refresh your session.

 Dismiss alert
{{ message }}
ben-manes
 
 /
 
 caffeine
 

 Public
Notifications
 You must be signed in to change notification settings
Fork
 1.7k
Star
 17.7k

# Eviction
Jump to bottom
Ben Manes edited this page Jan 18, 2025
 ·
 20 revisions
Caffeine provides three types of eviction: size-based eviction, time-based eviction, and reference-based eviction.

### Size-based
// Evict based on the number of entries in the cache
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .maximumSize(10_000)
 .build(key -> createExpensiveGraph(key));

// Evict based on the number of vertices in the cache
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .maximumWeight(10_000)
 .weigher((Key key, Graph graph) -> graph.vertices().size())
 .build(key -> createExpensiveGraph(key));
If your cache should not grow beyond a certain size, use Caffeine.maximumSize(long). The cache will try to evict entries that have not been used recently or very often.
Alternately, if different cache entries have different "weights" -- for example, if your cache values have radically different memory footprints -- you may specify a weight function with Caffeine.weigher(Weigher) and a maximum cache weight with Caffeine.maximumWeight(long). In addition to the same caveats as maximumSize requires, be aware that weights are computed at entry creation and update time, are static thereafter, and that relative weights are not used when making eviction selections.

### Time-based
// Evict based on a fixed expiration policy
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .expireAfterAccess(5, TimeUnit.MINUTES)
 .build(key -> createExpensiveGraph(key));
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .expireAfterWrite(10, TimeUnit.MINUTES)
 .build(key -> createExpensiveGraph(key));

// Evict based on a varying expiration policy
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .expireAfter(Expiry.creating((Key key, Graph graph) -> 
 Duration.between(Instant.now(), graph.creationDate().plusHours(5))))
 .build(key -> createExpensiveGraph(key));
Caffeine provides three approaches to timed eviction:
expireAfterAccess(long, TimeUnit): Expire entries after the specified duration has passed since the entry was last accessed by a read or a write. This could be desirable if the cached data is bound to a session and expires due to inactivity.
expireAfterWrite(long, TimeUnit): Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value. This could be desirable if cached data grows stale after a certain amount of time.
expireAfter(Expiry): Expires entries after the variable duration has passed. This could be desirable if the expiration time of an entry is determined by an external resource.
Expiration is performed with periodic maintenance during writes and occasionally during reads. Scheduling and firing of an expiration event is executed in amortized O(1) time.
For prompt expiration, rather than relying on other cache activity to trigger routine maintenance, use the Scheduler interface and the Caffeine.scheduler(Scheduler) method to specify a scheduling thread in your cache builder. Java 9+ users may prefer using Scheduler.systemScheduler() to leverage the dedicated, system-wide scheduling thread.
Testing timed eviction does not require that tests wait until the wall clock time has elapsed. Use the Ticker interface and the Caffeine.ticker(Ticker) method to specify a time source in your cache builder, rather than having to wait for the system clock. Guava's testlib provides a convenient FakeTicker for this purpose.

### Reference-based
// Evict when neither the key nor value are strongly reachable
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .weakKeys()
 .weakValues()
 .build(key -> createExpensiveGraph(key));

// Evict when the garbage collector needs to free memory
LoadingCache<Key, Graph> graphs = Caffeine.newBuilder()
 .softValues()
 .build(key -> createExpensiveGraph(key));
Caffeine allows you to set up your cache to allow the garbage collection of entries, by using weak references for keys or values, and by using soft references for values. Note that weak and soft value references are not supported by AsyncCache.
Caffeine.weakKeys() stores keys using weak references. This allows entries to be garbage-collected if there are no other strong references to the keys. Since garbage collection depends only on identity equality, this causes the whole cache to use identity (==) equality to compare keys, instead of equals().
Caffeine.weakValues() stores values using weak references. This allows entries to be garbage-collected if there are no other strong references to the values. Since garbage collection depends only on identity equality, this causes the whole cache to use identity (==) equality to compare values, instead of equals().
Caffeine.softValues() stores values using soft references. Softly referenced objects are garbage-collected in a globally least-recently-used manner, in response to memory demand. Because of the performance implications of using soft references, we generally recommend using the more predictable maximum cache size instead. Use of softValues() will cause values to be compared using identity (==) equality instead of equals().
Home
Caches
Population
Eviction
Removal
Refresh
Compute
Interner
Statistics
Specification
Cleanup
Policy
Testing
Faq
Extensions
Simulator
JCache
Guava
Migrating
Guava
CLHM
Performance
Design
Efficiency
Benchmarks
Memory Overhead
Roadmap
How to Contribute

### Clone this wiki locally
You can’t perform that action at this time.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://github.com/ben-manes/caffeine/wiki/Eviction
HTTP status: 200 · extracted bytes: 13082 · sha256: 19ddba736f96628532a99f5a13c9dd431d56960d01ae4899c664d2a9f8d7774c
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r112`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Eviction · ben-manes/caffeine Wiki · GitHub Skip to content Navigation Menu Toggle navigation Sign in Appearance settings Platform AI CODE CREATION GitHub Copilot Write better code with AI GitHub Copilot app Direct agents from issue to merge MCP Registry Integrate external tools DEVELOPER WORKFLOWS Actions Automate any workflow Codespaces Instant dev environments Issues Plan and track work Code Review Manage code changes Code Quality Enforce quality at merge APPLICATION SECURITY GitHub Advanced Security Find and fix vulnerabilities Code security Secure your code as you build Secret protection Stop leaks before they start EXPLORE Why GitHub Documentation Blog Changelog Marketplace View all features Solutions BY COMPANY SIZE Enterprises Small and medium teams Startups Nonprofits BY USE CASE App Modernization DevSecOps DevOps CI/CD View all use cases BY INDUSTRY Healthcare Financial services Manufacturing Government View all industries View all solutions Resources EXPLORE BY TOPIC AI Software Development DevOps Security View all topics EXPLORE BY TYPE Customer stories Events & webinars Ebooks & reports Business insights GitHub Skills SUPPORT & SERVICES Documentation Customer support Community forum Trust center Partners View all resources Open Source COMMUNITY GitHub Sponsors Fund open source developers PROGRAMS Security Lab Maintainer Community Accelerator GitHub Stars Archive Program REPOSITORIES Topics Trending Collections Enterprise ENTERPRISE SOLUTIONS Enterprise platform AI-powered developer platform AVAILABLE ADD-ONS GitHub Advanced Security Enterprise-grade security features Copilot for Business Enterprise-grade AI features Premium Support Enterprise-grade 24/7 support Pricing Search or jump to... Search code, repositories, users, issues, pull requests... --> Search Clear Search syntax tips Provide feedback --> We read every piece of feedback, and take your input very seriously. Include my email address so I can be contacted Cancel Submit feedback Saved searches Use saved searches to filter your results more quickly --> Name Query To see all available qualifiers, see our documentation . Cancel Create saved search Sign in Sign up Appearance settings Resetting focus You signed in with another tab or window. Reload to refresh your session. You signed out in another tab or window. Reload to refresh your session. You switched accounts on another tab or window. Reload to refresh your session. Dismiss alert {{ message }} ben-manes / caffeine Public Notifications You must be signed in to change notification settings Fork 1.7k Star 17.8k Code Issues 2 Pull requests 0 Discussions Actions Wiki Security and quality 0 Insights Additional navigation options Code Issues Pull requests Discussions Actions Wiki Security and quality Insights Eviction Jump to bottom Ben Manes edited this page Jan 18, 2025 · 20 revisions Caffeine provides three types of eviction: size-based eviction, time-based eviction, and reference-based eviction. Size-based // Evict based on the number of entries in the cache LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . maximumSize ( 10_000 ) . build ( key -> createExpensiveGraph ( key )); // Evict based on the number of vertices in the cache LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . maximumWeight ( 10_000 ) . weigher (( Key key , Graph graph ) -> graph . vertices (). size ()) . build ( key -> createExpensiveGraph ( key )); If your cache should not grow beyond a certain size, use Caffeine.maximumSize(long) . The cache will try to evict entries that have not been used recently or very often . Alternately, if different cache entries have different "weights" -- for example, if your cache values have radically different memory footprints -- you may specify a weight function with Caffeine.weigher(Weigher) and a maximum cache weight with Caffeine.maximumWeight(long) . In addition to the same caveats as maximumSize requires, be aware that weights are computed at entry creation and update time, are static thereafter, and that relative weights are not used when making eviction selections. Time-based // Evict based on a fixed expiration policy LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . expireAfterAccess ( 5 , TimeUnit . MINUTES ) . build ( key -> createExpensiveGraph ( key )); LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . expireAfterWrite ( 10 , TimeUnit . MINUTES ) . build ( key -> createExpensiveGraph ( key )); // Evict based on a varying expiration policy LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . expireAfter ( Expiry . creating (( Key key , Graph graph ) -> Duration . between ( Instant . now (), graph . creationDate (). plusHours ( 5 )))) . build ( key -> createExpensiveGraph ( key )); Caffeine provides three approaches to timed eviction: expireAfterAccess(long, TimeUnit): Expire entries after the specified duration has passed since the entry was last accessed by a read or a write. This could be desirable if the cached data is bound to a session and expires due to inactivity. expireAfterWrite(long, TimeUnit): Expire entries after the specified duration has passed since the entry was created, or the most recent replacement of the value. This could be desirable if cached data grows stale after a certain amount of time. expireAfter(Expiry): Expires entries after the variable duration has passed. This could be desirable if the expiration time of an entry is determined by an external resource. Expiration is performed with periodic maintenance during writes and occasionally during reads. Scheduling and firing of an expiration event is executed in amortized O(1) time. For prompt expiration, rather than relying on other cache activity to trigger routine maintenance, use the Scheduler interface and the Caffeine.scheduler(Scheduler) method to specify a scheduling thread in your cache builder. Java 9+ users may prefer using Scheduler.systemScheduler() to leverage the dedicated, system-wide scheduling thread. Testing timed eviction does not require that tests wait until the wall clock time has elapsed. Use the Ticker interface and the Caffeine.ticker(Ticker) method to specify a time source in your cache builder, rather than having to wait for the system clock. Guava's testlib provides a convenient FakeTicker for this purpose. Reference-based // Evict when neither the key nor value are strongly reachable LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . weakKeys () . weakValues () . build ( key -> createExpensiveGraph ( key )); // Evict when the garbage collector needs to free memory LoadingCache < Key , Graph > graphs = Caffeine . newBuilder () . softValues () . build ( key -> createExpensiveGraph ( key )); Caffeine allows you to set up your cache to allow the garbage collection of entries, by using weak references for keys or values, and by using soft references for values. Note that weak and soft value references are not supported by AsyncCache . Caffeine.weakKeys() stores keys using weak references. This allows entries to be garbage-collected if there are no other strong references to the keys. Since garbage collection depends only on identity equality, this causes the whole cache to use identity (==) equality to compare keys, instead of equals() . Caffeine.weakValues() stores values using weak references. This allows entries to be garbage-collected if there are no other strong references to the values. Since garbage collection depends only on identity equality, this causes the whole cache to use identity (==) equality to compare values, instead of equals() . Caffeine.softValues() stores values using soft references. Softly referenced objects are garbage-collected in a globally least-recently-used manner, in response to memory demand. Because of the performance implications of using soft references, we generally recommend using the more predictable maximum cache size instead. Use of softValues() will cause values to be compared using identity (==) equality instead of equals() . Wiki pages Pages 52 Loading Home Uh oh! There was an error while loading. Please reload this page . Loading Benchmarks Uh oh! There was an error while loading. Please reload this page . Loading Benchmarks zh CN Uh oh! There was an error while loading. Please reload this page . Loading Cache Uh oh! There was an error while loading. Please reload this page . Loading Cleanup Uh oh! There was an error while loading. Please reload this page . Loading Cleanup zh CN Uh oh! There was an error while loading. Please reload this page . Loading Compute Uh oh! There was an error while loading. Please reload this page . Loading Compute zh CN Uh oh! There was an error while loading. Please reload this page . Loading ConcurrentLinkedHashMap Uh oh! There was an error while loading. Please reload this page . Loading ConcurrentLinkedHashMap zh CN Uh oh! There was an error while loading. Please reload this page . Loading ConcurrentLinkedStack Uh oh! There was an error while loading. Please reload this page . Loading Contribute Uh oh! There was an error while loading. Please reload this page . Loading Contribute zh CN Uh oh! There was an error while loading. Please reload this page . Loading Design Uh oh! There was an error while loading. Please reload this page . Loading Design zh CN Uh oh! There was an error while loading. Please reload this page . Loading Efficiency Uh oh! There was an error while loading. Please reload this page . Loading Efficiency zh CN Uh oh! There was an error while loading. Please reload this page . Loading Ehcache Uh oh! There was an error while loading. Please reload this page . Loading Eviction Size-based Time-based Reference-based Loading Eviction zh CN Uh oh! There was an error while loading. Please reload this page . Loading Faq Uh oh! There was an error while loading. Please reload this page . Loading Faq zh CN Uh oh! There was an error while loading. Please reload this page . Loading Guava Uh oh! There was an error while loading. Please reload this page . Loading Guava zh CN Uh oh! There was an error while loading. Please reload this page . Loading Home zh CN Uh oh! There was an error while loading. Please reload this page . Loading Interner Uh oh! There was an error while loading. Please reload this page . Loading Interner zh CN Uh oh! There was an error while loading. Please reload this page . Loading JCache Uh oh! There was an error while loading. Please reload this page . Loading JCache zh CN Uh oh! There was an error while loading. Please reload this page . Loading Memory overhead Uh oh! There was an error while loading. Please reload this page . Loading Memory overhead zh CN Uh oh! There was an error while loading. Please reload this page . Loading Policy Uh oh! There was an error while loading. Please reload this page . Loading Policy zh CN Uh oh! There was an error while loading. Please reload this page . Loading Population Uh oh! There was an error while loading. Please reload this page . Loading Population zh CN Uh oh! There was an error while loading. Please reload this page . Loading Refresh Uh oh! There was an error while loading. Please reload this page . Loading Refresh zh CN Uh oh! There was an error while loading. Please reload this page . Loading Removal Uh oh! There was an error while loading. Please reload this page . Loading Removal zh CN Uh oh! There was an error while loading. Please reload this page . Loading Roadmap Uh oh! There was an error while loading. Please reload this page . Loading Roadmap zh CN Uh oh! There was an error while loading. Please reload this page . Loading Simulator Uh oh! There was an error while loading. Please reload this page . Loading Simulator zh CN Uh oh! There was an error while loading. Please reload this page . Loading SingleConsumerQueue Uh oh! There was an error while loading. Please reload this page . Loading Specification Uh oh! There was an error while loading. Please reload this page . Loading Specification zh CN Uh oh! There was an error while loading. Please reload this page . Loading Statistics Uh oh! There was an error while loading. Please reload this page . Loading Statistics zh CN Uh oh! There was an error while loading. Please reload this page . Loading Testing Uh oh! There was an error while loading. Please reload this page . Loading Testing zh CN Uh oh! There was an error while loading. Please reload this page . Loading Tracing Uh oh! There was an error while loading. Please reload this page . Loading Writer Uh oh! There was an error while loading. Please reload this page . Show 37 more pages… Home Caches Population Eviction Removal Refresh Compute Interner Statistics Specification Cleanup Policy Testing Faq Extensions Simulator JCache Guava Migrating Guava CLHM Performance Design Efficiency Benchmarks Memory Overhead Roadmap How to Contribute Clone this wiki locally Footer © 2026 GitHub, Inc. Footer navigation Terms Privacy Security Status Community Docs Contact Manage cookies Do not share my personal information You can’t perform that action at this time.
