package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.GroupEntity
import com.finance.domain.repository.GroupRepository
import com.finance.infraestructure.persistence.mapper.GroupMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class GroupRepositoryImpl(
    private val springDataGroupRepository: SpringDataGroupRepository,
    private val groupMapper: GroupMapper,
) : GroupRepository {

    @Transactional
    override fun save(group: GroupEntity): GroupEntity {
        val jpaEntity = groupMapper.toJpaEntity(group)
        val saved = springDataGroupRepository.save(jpaEntity)
        return groupMapper.toDomain(saved)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): GroupEntity? {
        return springDataGroupRepository.findById(id)
            .map { groupMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional
    override fun deleteById(id: UUID) {
        springDataGroupRepository.deleteById(id)
    }
}
