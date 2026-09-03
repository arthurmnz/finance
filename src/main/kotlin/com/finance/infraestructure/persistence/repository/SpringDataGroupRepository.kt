package com.finance.infraestructure.persistence.repository

import com.finance.infraestructure.persistence.entity.GroupJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataGroupRepository : JpaRepository<GroupJpaEntity, UUID>
