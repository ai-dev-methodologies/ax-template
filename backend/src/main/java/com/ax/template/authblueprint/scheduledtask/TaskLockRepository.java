package com.ax.template.authblueprint.scheduledtask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskLockRepository extends JpaRepository<TaskLock, String> {
    Optional<TaskLock> findByTaskName(String taskName);
}
