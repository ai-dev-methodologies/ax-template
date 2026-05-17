package fixtures.base_repository.pass;

// FIXTURE: pass
// PATTERN: *Repository interface extends BaseRepository (JpaRepository) — PASSES PRACTICES-TEST-004

// CORRECT: interface extending BaseRepository<T, ID> → satisfies ArchUnit rule
public interface ItemRepository extends com.example.app.repositories.BaseRepository<Item, Long> {
    // Spring Data query methods and custom @Query methods go here
}
