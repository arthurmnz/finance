package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.CategoryEntity
import com.finance.domain.enum.CategoryLevel
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

    override fun saveAll(categories: List<CategoryEntity>): List<CategoryEntity> {
        if (categories.isEmpty()) return emptyList()
        val jpaEntities = categories.map { mapper.toJpaEntity(it) }
        return springDataCategoryRepository.saveAll(jpaEntities)
            .map { mapper.toDomain(it) }
    }

    override fun findById(id: UUID): CategoryEntity? {
        return springDataCategoryRepository.findById(id)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByIdIn(ids: Collection<UUID>): List<CategoryEntity> {
        if (ids.isEmpty()) return emptyList()
        return springDataCategoryRepository.findByIdIn(ids)
            .map { mapper.toDomain(it) }
    }

    override fun findByUserId(userId: UUID): List<CategoryEntity> {
        return springDataCategoryRepository.findByUserId(userId)
            .map { mapper.toDomain(it) }
    }

    override fun findByUserIdAndLevel(userId: UUID, level: CategoryLevel): List<CategoryEntity> {
        return springDataCategoryRepository.findByUserIdAndLevel(userId, level)
            .map { mapper.toDomain(it) }
    }

    override fun findByGroupId(groupId: UUID): List<CategoryEntity> {
        return springDataCategoryRepository.findByGroupId(groupId)
            .map { mapper.toDomain(it) }
    }

    override fun findByGroupIdAndLevel(groupId: UUID, level: CategoryLevel): List<CategoryEntity> {
        return springDataCategoryRepository.findByGroupIdAndLevel(groupId, level)
            .map { mapper.toDomain(it) }
    }

    override fun findByParentId(parentId: UUID): List<CategoryEntity> {
        return springDataCategoryRepository.findByParentId(parentId)
            .map { mapper.toDomain(it) }
    }

    override fun existsByParentId(parentId: UUID): Boolean {
        return springDataCategoryRepository.existsByParentId(parentId)
    }

    override fun findByHierarchyMigratedFalse(): List<CategoryEntity> {
        return springDataCategoryRepository.findByHierarchyMigratedFalse()
            .map { mapper.toDomain(it) }
    }

    override fun delete(id: UUID) {
        springDataCategoryRepository.deleteById(id)
    }
}
