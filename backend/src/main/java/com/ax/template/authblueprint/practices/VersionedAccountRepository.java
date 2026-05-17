package com.ax.template.authblueprint.practices;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionedAccountRepository extends JpaRepository<VersionedAccount, Long> {
}
