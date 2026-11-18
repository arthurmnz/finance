package com.finance.application.use_case.category

import com.finance.application.dto.category.CategoryResponse
import com.finance.application.service.CategorySeedService
import com.finance.application.service.GroupService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SeedDefaultCategoriesUseCase(
    private val categorySeedService: CategorySeedService,
    private val listUserCategoriesUseCase: ListUserCategoriesUseCase,
    private val listGroupCategoriesUseCase: ListGroupCategoriesUseCase,
    private val groupService: GroupService
) {
    @Transactional
    fun seedForUser(userId: UUID): List<CategoryResponse> {
        categorySeedService.seedDefaultsForUser(userId)
        return listUserCategoriesUseCase.execute(userId)
    }

    @Transactional
    fun seedForGroup(groupId: UUID, userId: UUID): List<CategoryResponse> {
        groupService.findGroupById(groupId)
        val member = groupService.findMember(groupId, userId)
        groupService.requireOwnerOrAdmin(member)
        categorySeedService.seedDefaultsForGroup(groupId)
        return listGroupCategoriesUseCase.execute(groupId, userId)
    }
}
