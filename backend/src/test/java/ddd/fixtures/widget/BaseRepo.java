package ddd.fixtures.widget;
/** FIXTURE — custom base repository interface (NOT named *Repository) with a mutator. */
public interface BaseRepo<T, ID> { T save(T t); }
