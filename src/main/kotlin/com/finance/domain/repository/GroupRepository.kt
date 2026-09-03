package com.finance.domain.repository

import com.finance.domain.entity.GroupEntity
import java.util.UUID

interface GroupRepository {
    fun save(group: GroupEntity): GroupEntity
    fun findById(id: UUID): GroupEntity?
    fun deleteById(id: UUID)
}
