package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.repository.CategoryRepository
import com.finance.infraestructure.persistence.mapper.CategoryMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class CategoryRepositoryImpl(
    private val springDataCategoryRepository: SpringDataCategoryRepository,
    private val mapper: CategoryMapper
) : CategoryRepository {

    override fun save(category: CategoryEntity): CategoryEntity {
        val jpaEntity = mapper.toJpaEntity(category)
        val savedEntity = springDataCategoryRepository.save(jpaEntity)
        return mapper.toDomain(savedEntity)
    }

    override fun findById(id: UUID): CategoryEntity? {
        return springDataCategoryRepository.findById(id)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByUserId(userId: UUID): List<CategoryEntity> {
        return springDataCategoryRepository.findByUserId(userId)
            .map { mapper.toDomain(it) }
    }

    override fun findByGroupId(groupId: UUID): List<CategoryEntity> {
        return springDataCategoryRepository.findByGroupId(groupId)
            .map { mapper.toDomain(it) }
    }

    override fun delete(id: UUID) {
        springDataCategoryRepository.deleteById(id)
    }
}
