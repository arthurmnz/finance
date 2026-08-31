package com.finance.infraestructure.persistence.repository

import com.finance.domain.entity.UserEntity
import com.finance.domain.repository.UserRepository
import com.finance.domain.value_object.Email
import com.finance.infraestructure.persistence.mapper.UserMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class UserRepositoryImpl(
    private val springDataUserRepository: SpringDataUserRepository,
    private val userMapper: UserMapper,
) : UserRepository {

    @Transactional
    override fun save(user: UserEntity): UserEntity {
        val jpaEntity = userMapper.toJpaEntity(user)
        val savedEntity = springDataUserRepository.save(jpaEntity)
        return userMapper.toDomain(savedEntity)
    }

    @Transactional(readOnly = true)
    override fun findById(id: UUID): UserEntity? {
        return springDataUserRepository.findById(id)
            .map { userMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: Email): UserEntity? {
        val jpaEntity = springDataUserRepository.findByEmail(email.normalized)
        return jpaEntity?.let { userMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun existsByEmail(email: Email): Boolean {
        return springDataUserRepository.existsByEmail(email.normalized)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<UserEntity> {
        return springDataUserRepository.findAll().map { userMapper.toDomain(it) }
    }

    @Transactional
    override fun deleteById(id: UUID): Boolean {
        return if (springDataUserRepository.existsById(id)) {
            springDataUserRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}
