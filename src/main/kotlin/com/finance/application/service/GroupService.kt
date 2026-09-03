package com.finance.application.service

import com.finance.domain.entity.GroupEntity
import com.finance.domain.entity.GroupMemberEntity
import com.finance.domain.exception.AccessDeniedException
import com.finance.domain.exception.GroupMemberNotFoundException
import com.finance.domain.exception.GroupNotFoundException
import com.finance.domain.repository.GroupMemberRepository
import com.finance.domain.repository.GroupRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class GroupService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
) {

    fun findGroupById(id: UUID): GroupEntity {
        return groupRepository.findById(id)
            ?: throw GroupNotFoundException("Grupo não encontrado com o ID: $id")
    }

    fun findMember(groupId: UUID, userId: UUID): GroupMemberEntity {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw GroupMemberNotFoundException("Usuário não é membro deste grupo.")
    }

    fun requireOwner(member: GroupMemberEntity) {
        if (!member.isOwner()) {
            throw AccessDeniedException("Apenas o dono do grupo pode realizar esta ação.")
        }
    }

    fun requireOwnerOrAdmin(member: GroupMemberEntity) {
        if (!member.canManageMembers()) {
            throw AccessDeniedException("Apenas o dono ou administradores podem realizar esta ação.")
        }
    }

    fun requireCanRename(member: GroupMemberEntity) {
        if (!member.canRenameGroup()) {
            throw AccessDeniedException("Apenas o dono ou administradores podem renomear o grupo.")
        }
    }
}
