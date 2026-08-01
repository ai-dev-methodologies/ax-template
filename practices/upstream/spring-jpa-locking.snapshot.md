# spring-jpa-locking — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:24:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r144`
**Body SHA-256 (below the `---` divider, header excluded):** d555db976b7372d487ca6eb671abb061c05559076c9912fbd1c47e074e4333f9

---

---
snapshot_id: spring-jpa-locking
source: "https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 3872
sha: "dfcd4ee1bcc2838ecc81a69ed78b2e2dc134ce57b1107303cf0f9f5515019f0c"
---

# spring jpa locking — upstream snapshot

Source: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
Fetched: 2026-07-14

Locking :: Spring Data JPA

Why Spring
OverviewMicroservicesReactiveEvent
DrivenCloudWeb
ApplicationsServerlessBatch

Learn
OverviewQuickstartGuidesBlog

Projects
OverviewSpring BootSpring FrameworkSpring CloudSpring Cloud Data FlowSpring DataSpring IntegrationSpring BatchSpring SecurityView all projects
DEVELOPMENT TOOLS
Spring Tools 4Spring Initializr

Academy
CoursesGet Certified

Solutions
OverviewSpring RuntimeSpring ConsultingSpring Academy For TeamsSecurity Advisories

Community
OverviewEventsTeam

light

Spring Data JPA4.1.0
Search

Overview

Upgrading Spring Data

JPA

Getting Started

Core concepts

Defining Repository Interfaces

Configuration

Persisting Entities

Defining Query Methods

JPA Query Methods

Value Expressions Fundamentals

Projections

Stored Procedures

Specifications

Query by Example

Vector Search

Transactionality

Locking

Auditing

Merging persistence units

CDI Integration

Custom Repository Implementations

Publishing Events from Aggregate Roots

Null Handling of Repository Methods

Spring Data Extensions

Repository query keywords

Repository query return types

Ahead of Time Optimizations

Frequently Asked Questions

Glossary

Envers

Introduction

Configuration

Usage

Javadoc

Wiki

Search

Edit this Page

GitHub Project

Stack Overflow

Spring Data JPA

JPA

Locking

# Locking

To specify the lock mode to be used, you can use the @Lock annotation on query methods, as shown in the following example:

Example 1. Defining lock metadata on query methods

interface UserRepository extends Repository<User, Long> {

// Plain query method
@Lock(LockModeType.READ)
List<User> findByLastname(String lastname);
}

This method declaration causes the query being triggered to be equipped with a LockModeType of READ. You can also define locking for CRUD methods by redeclaring them in your repository interface and adding the @Lock annotation, as shown in the following example:

Example 2. Defining lock metadata on CRUD methods

interface UserRepository extends Repository<User, Long> {

// Redeclaration of a CRUD method
@Lock(LockModeType.READ)
List<User> findAll();
}

Spring Data Commons

Stable

4.1.0

4.0.6

3.5.13

Snapshot

4.2.0-SNAPSHOT

4.1.1-SNAPSHOT

4.0.7-SNAPSHOT

3.5.14-SNAPSHOT

Spring Data JPA

Stable

4.1.0

4.0.6

3.5.13

Snapshot

4.2.0-SNAPSHOT

4.1.1-SNAPSHOT

4.0.7-SNAPSHOT

3.5.14-SNAPSHOT

Related Spring Documentation

Spring Framework

Spring Data

Spring Data Cassandra

Spring Data Commons

Spring Data Couchbase

Spring Data Elasticsearch

Spring Data JPA

Spring Data KeyValue

Spring Data LDAP

Spring Data MongoDB

Spring Data Neo4j

Spring Data Redis

Spring Data JDBC & R2DBC

Spring Data REST

Spring GraphQL

All Docs...

Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.

Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings

Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners.

Search in all Spring Docs

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
HTTP status: 200 · extracted bytes: 3797 · sha256: c36e009605e7476af35e738bc4f96e5a18749df2d472f51763096390c275a2c5
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r144`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Locking :: Spring Data JPA Why Spring Overview Microservices Reactive Event Driven Cloud Web Applications Serverless Batch Learn Overview Quickstart Guides Blog Projects Overview Spring Boot Spring Framework Spring Cloud Spring Cloud Data Flow Spring Data Spring Integration Spring Batch Spring Security View all projects DEVELOPMENT TOOLS Spring Tools 4 Spring Initializr Academy Courses Get Certified Solutions Overview Spring Runtime Spring Consulting Spring Academy For Teams Security Advisories Community Overview Events Team light Spring Data JPA 4.1.0 Search Overview Upgrading Spring Data JPA Getting Started Core concepts Defining Repository Interfaces Configuration Persisting Entities Defining Query Methods JPA Query Methods Value Expressions Fundamentals Projections Stored Procedures Specifications Query by Example Vector Search Transactionality Locking Auditing Merging persistence units CDI Integration Custom Repository Implementations Publishing Events from Aggregate Roots Null Handling of Repository Methods Spring Data Extensions Repository query keywords Repository query return types Ahead of Time Optimizations Frequently Asked Questions Glossary Envers Introduction Configuration Usage Javadoc Wiki Search Edit this Page GitHub Project Stack Overflow Spring Data JPA JPA Locking Locking To specify the lock mode to be used, you can use the @Lock annotation on query methods, as shown in the following example: Example 1. Defining lock metadata on query methods interface UserRepository extends Repository<User, Long> { // Plain query method @Lock(LockModeType.READ) List<User> findByLastname(String lastname); } This method declaration causes the query being triggered to be equipped with a LockModeType of READ . You can also define locking for CRUD methods by redeclaring them in your repository interface and adding the @Lock annotation, as shown in the following example: Example 2. Defining lock metadata on CRUD methods interface UserRepository extends Repository<User, Long> { // Redeclaration of a CRUD method @Lock(LockModeType.READ) List<User> findAll(); } Spring Data Commons Stable 4.1.0 4.0.6 3.5.13 Snapshot 4.2.0-SNAPSHOT 4.1.1-SNAPSHOT 4.0.7-SNAPSHOT 3.5.14-SNAPSHOT Spring Data JPA Stable 4.1.0 4.0.6 3.5.13 Snapshot 4.2.0-SNAPSHOT 4.1.1-SNAPSHOT 4.0.7-SNAPSHOT 3.5.14-SNAPSHOT Related Spring Documentation Spring Framework Spring Data Spring Data Cassandra Spring Data Commons Spring Data Couchbase Spring Data Elasticsearch Spring Data JPA Spring Data KeyValue Spring Data LDAP Spring Data MongoDB Spring Data Neo4j Spring Data Redis Spring Data JDBC & R2DBC Spring Data REST Spring GraphQL All Docs... Copyright © 2005 - Broadcom. All Rights Reserved. The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries. Terms of Use • Privacy • Trademark Guidelines • Thank you • Your California Privacy Rights • Cookie Settings Apache®, Apache Tomcat®, Apache Kafka®, Apache Cassandra™, and Apache Geode™ are trademarks or registered trademarks of the Apache Software Foundation in the United States and/or other countries. Java™, Java™ SE, Java™ EE, and OpenJDK™ are trademarks of Oracle and/or its affiliates. Kubernetes® is a registered trademark of the Linux Foundation in the United States and other countries. Linux® is the registered trademark of Linus Torvalds in the United States and other countries. Windows® and Microsoft® Azure are registered trademarks of Microsoft Corporation. “AWS” and “Amazon Web Services” are trademarks or registered trademarks of Amazon.com Inc. or its affiliates. All other trademarks and copyrights are property of their respective owners and are only mentioned for informative purposes. Other names may be trademarks of their respective owners. Search in all Spring Docs
