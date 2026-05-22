package com.ax.template.authblueprint.favoritesbookmarks;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    Optional<Favorite> findByUserIdAndEntityTypeAndEntityId(String userId,
                                                             String entityType,
                                                             String entityId);

    List<Favorite> findByUserIdOrderByFavoritedAtDesc(String userId);

    List<Favorite> findByUserIdAndEntityTypeOrderByFavoritedAtDesc(String userId, String entityType);

    long countByUserId(String userId);

    long countByEntityTypeAndEntityId(String entityType, String entityId);

    @Modifying
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId AND f.entityType = :entityType AND f.entityId = :entityId")
    int deleteByUserAndEntity(@Param("userId") String userId,
                              @Param("entityType") String entityType,
                              @Param("entityId") String entityId);
}
