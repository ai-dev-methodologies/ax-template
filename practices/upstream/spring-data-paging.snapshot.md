---
snapshot_id: spring-data-paging
source: "https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 3
bytes: 4934
sha: "e5dcbbc3a01984668a4da9f556a92692364afadd5f2ac3a0c637f83d665ab2ef"
---

# spring data paging — upstream snapshot

Source: https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html
Fetched: 2026-07-14

Core concepts :: Spring Data Commons
Edit this Page
 
 
 
 GitHub Project
 
 
 
 Stack Overflow

# Core concepts
The central interface in the Spring Data repository abstraction is Repository.
It takes the domain class to manage as well as the identifier type of the domain class as type arguments.
This interface acts primarily as a marker interface to capture the types to work with and to help you to discover interfaces that extend this one.
Spring Data considers domain types to be entities, more specifically aggregates.
So you will see the term "entity" used throughout the documentation that can be interchanged with the term "domain type" or "aggregate".
As you might have noticed in the introduction it already hinted towards domain-driven concepts.
We consider domain objects in the sense of DDD.
Domain objects have identifiers (otherwise these would be identity-less value objects), and we somehow need to refer to identifiers when working with certain patterns to access data.
Referring to identifiers will become more meaningful as we talk about repositories and query methods.
The CrudRepository and ListCrudRepository interfaces provide sophisticated CRUD functionality for the entity class that is being managed.
CrudRepository Interface
public interface CrudRepository extends Repository {

 S save(S entity); (1)

 Optional findById(ID primaryKey); (2)

 Iterable findAll(); (3)

 long count(); (4)

 void delete(T entity); (5)

 boolean existsById(ID primaryKey); (6)

 // … more functionality omitted.
}
1
Saves the given entity.
2
Returns the entity identified by the given ID.
3
Returns all entities.
4
Returns the number of entities.
5
Deletes the given entity.
6
Indicates whether an entity with the given ID exists.
The methods declared in this interface are commonly referred to as CRUD methods.
ListCrudRepository offers equivalent methods, but they return List where the CrudRepository methods return an Iterable.
The repository interface implies a few reserved methods like findById(ID identifier) that target the domain type identifier property regardless of its property name.
Read more about this in “Defining Query Methods”.
You can annotate your query method with @Query to provide a custom query if a property named Id doesn’t refer to the identifier.
Following that path can easily lead to confusion and is discouraged as you will quickly hit type limits if the ID type and the type of your Id property deviate.
We also provide persistence technology-specific abstractions, such as JpaRepository or MongoRepository.
Those interfaces extend CrudRepository and expose the capabilities of the underlying persistence technology in addition to the rather generic persistence technology-agnostic interfaces such as CrudRepository.
In addition to CrudRepository, there are PagingAndSortingRepository and ListPagingAndSortingRepository which add additional methods to ease paginated access to entities:
PagingAndSortingRepository interface
interface PagingAndSortingRepository extends Repository {

 Iterable findAll(Sort sort);

 Page findAll(Pageable pageable);
}
Extension interfaces are subject to be supported by the actual store module.
While this documentation explains the general scheme, make sure that your store module supports the interfaces that you want to use.
To access the second page of User by a page size of 20, you could do something like the following:
PagingAndSortingRepository repository = // … get access to a bean
Page users = repository.findAll(PageRequest.of(1, 20));
ListPagingAndSortingRepository offers equivalent methods, but returns a List where the PagingAndSortingRepository methods return an Iterable.
In addition to pagination, scrolling provides a more fine-grained access to iterate through chunks of larger result sets.
In addition to query methods, query derivation for both count and delete queries is available.
The following list shows the interface definition for a derived count query:
Derived Count Query
interface UserRepository extends CrudRepository {

 long countByLastname(String lastname);
}
The following listing shows the interface definition for a derived delete query:
Derived Delete Query
interface UserRepository extends CrudRepository {

 long deleteByLastname(String lastname);

 List removeByLastname(String lastname);
}
Spring Data Commons
4.1.0
4.0.6
3.5.13
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
Search in all Spring Docs
