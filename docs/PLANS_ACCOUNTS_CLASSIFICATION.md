# Planejamento Arquitetural: Classificação de Contas (Individuais vs. Conjuntas de Grupo)

**Status:** Proposta de Arquitetura (Planning / RFC)  
**Autor:** Planner Agent  
**Data:** 12 de Novembro de 2026  
**Contexto:** Evolução do modelo de contas no sistema financeiro (`finance` backend Kotlin + `front_finance` React).

---

## 1. Sumário Executivo & Diagnóstico da Modelagem Atual

### 1.1. Contexto e Motivação
Atualmente, o usuário identificou uma lacuna estrutural na forma como grupos e contas convivem:
> *"para dar a possibilidade de um casal ou grupo de amigos manter a sua individualidade e o fator compartilhado, quero criar uma classificação pra contas. Conta Conjunta pertencente ao grupo e os participantes são donos, podendo escolher as pessoas do grupo que fazem parte dessa conta, e a Conta Individual que já existe."*

### 1.2. Diagnóstico da Modelagem Vigente

1. **Titularidade Estritamente Individual (`owner_id NOT NULL`)**:
   - A tabela `accounts` e a entidade de domínio `AccountEntity` possuem o campo `val ownerId: UUID`.
   - Toda e qualquer conta pertence rigidamente a um único usuário criador (`UserEntity`).
2. **Vínculo Superficial ao Grupo (`groupId: UUID?`)**:
   - A relação atual com grupos ocorre via campo opcional `groupId` na própria conta, acionado por `LinkAccountToGroupUseCase` (`POST /api/v1/accounts/{id}/group/{groupId}`).
   - O fluxo existente assume que o usuário "oferece" ou "vincula" sua conta pessoal ao grupo através de um `AccountGroupRequest`.
3. **Ausência de Seleção de Participantes (Co-titularidade)**:
   - Uma vez que `groupId != null`, a regra de permissão em `CreateTransactionUseCase` concede acesso indiscriminado a **todos** os membros do grupo:
     ```kotlin
     val hasAccess = acc.ownerId == userId || (acc.groupId != null && groupMemberRepository.existsByGroupIdAndUserId(acc.groupId!!, userId))
     ```
   - Em um grupo de 4 pessoas (ex: família com pais e 2 filhos, ou república de amigos), não há como criar uma conta conjunta apenas entre o casal ou entre 2 amigos específicos. Se estiver no grupo, todos do grupo têm acesso à conta.
4. **Distorção no Patrimônio e Balanço Pessoal (`GetTotalBalanceUseCase`)**:
   - O cálculo do balanço total pessoal consulta unicamente `accountRepository.findAllByOwnerId(authenticatedUserId)`.
   - Se uma conta conjunta foi cadastrada por um dos parceiros, **100% do saldo** dessa conta entra no balanço pessoal desse parceiro, e **0%** entra no balanço pessoal do outro co-titular, mesmo que ambos depositem dinheiro e usem a conta diariamente.
5. **Invisibilidade em "Minhas Contas" (`ListMyAccountsUseCase`)**:
   - O endpoint `/api/v1/accounts` (`GET`) retorna apenas as contas com `ownerId == userId`.
   - O participante que não é o `ownerId` original nem sequer enxerga a conta na tela principal de Contas (`/contas`), sendo obrigado a navegar até a tela do Grupo (`/grupos/{id}` -> aba Contas) para poder consultá-la.

---

## 2. Modelo Conceitual Proposto

Propõe-se introduzir explicitamente uma classificação binária para contas financeiras:

```
                      ┌────────────────────────────────────────┐
                      │             AccountEntity              │
                      └───────────────────┬────────────────────┘
                                          │
            ┌─────────────────────────────┴─────────────────────────────┐
            ▼                                                           ▼
┌───────────────────────┐                                   ┌───────────────────────┐
│   Conta INDIVIDUAL    │                                   │    Conta CONJUNTA     │
│ (Individual Account)  │                                   │    (Joint Account)    │
├───────────────────────┤                                   ├───────────────────────┤
│ • Dono único (ownerId)│                                   │ • Pertence ao Grupo   │
│ • Privada e pessoal   │                                   │ • N Co-titulares      │
│ • groupId = null      │                                   │ • groupId obrigatório │
│ • 100% no balanço     │                                   │ • Participantes       │
│   pessoal do dono     │                                   │   selecionados        │
└───────────────────────┘                                   └───────────────────────┘
```

### 2.1. Conta Individual (`INDIVIDUAL`)
- **Propósito**: Manter a individualidade e a privacidade financeira do indivíduo (salário próprio, investimentos pessoais, despesas particulares).
- **Propriedade**: Possui um único `ownerId`.
- **Escopo**: `groupId` é obrigatoriamente `null`.
- **Privacidade**: Inacessível para qualquer outro usuário ou grupo.
- **Balanço**: Compõe integralmente o saldo e fluxo de caixa pessoal.

### 2.2. Conta Conjunta (`JOINT`)
- **Propósito**: Gestão de patrimônio e despesas compartilhadas (casal gerindo despesas do lar, amigos gerindo caixinha da viagem ou custos da casa).
- **Propriedade**: Pertence formalmente a um `Group` (`groupId NOT NULL`).
- **Co-propriedade**: Não possui um "dono individual soberano". Em vez disso, possui uma coleção de **Participantes / Co-titulares** (`account_members`).
- **Seleção Granular**: Ao criar ou editar a conta no grupo, seleciona-se exatamente quais membros do grupo são os participantes dessa conta (no mínimo 1 ou 2 membros).
- **Controle de Acesso**: Somente os membros do grupo explicitamente vinculados como participantes têm permissão de visualização, extrato e movimentação. Membros do grupo que não forem participantes não possuem acesso.

---

## 3. Arquitetura de Dados & Schema Evolution

### 3.1. Novo Enum de Domínio
```kotlin
package com.finance.domain.enum

enum class AccountClassification {
    INDIVIDUAL,
    JOINT
}
```

### 3.2. Nova Entidade de Domínio: `AccountMemberEntity`
```kotlin
package com.finance.domain.entity

import com.finance.domain.enum.AccountMemberRole
import java.time.LocalDateTime
import java.util.UUID

class AccountMemberEntity(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val userId: UUID,
    val role: AccountMemberRole = AccountMemberRole.CO_OWNER,
    val joinedAt: LocalDateTime = LocalDateTime.now()
)
```

Onde `AccountMemberRole`:
- `CO_OWNER`: Co-titular com plenos poderes (visualizar, movimentar, editar dados da conta, gerenciar outros participantes).
- `MEMBER` (opcional para expansão futura): Permissão apenas para visualizar e lançar transações, sem poder de edição de dados bancários ou exclusão.

### 3.3. Evolução da `AccountEntity`
```kotlin
class AccountEntity(
    val id: UUID,
    val classification: AccountClassification,
    val ownerId: UUID?, // Obrigatório para INDIVIDUAL; opcional ou criador histórico para JOINT
    val groupId: UUID?, // Obrigatório para JOINT; nulo para INDIVIDUAL
    name: String,
    startBalance: Int,
    balance: Int,
    type: AccountType,
    tax: Int?,
    includeInTotal: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    updatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        when (classification) {
            AccountClassification.INDIVIDUAL -> {
                requireNotNull(ownerId) { "Contas individuais devem possuir um ownerId." }
                require(groupId == null) { "Contas individuais não podem estar vinculadas a um grupo." }
            }
            AccountClassification.JOINT -> {
                requireNotNull(groupId) { "Contas conjuntas devem estar vinculadas a um grupo (groupId obrigatório)." }
            }
        }
    }
}
```

### 3.4. DDL no Banco de Dados (PostgreSQL)

```sql
-- 1. Adicionar coluna de classificação na tabela accounts
ALTER TABLE accounts 
ADD COLUMN classification VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL';

-- 2. Permitir owner_id nulo para contas puramente conjuntas (se desejado)
ALTER TABLE accounts ALTER COLUMN owner_id DROP NOT NULL;

-- 3. Criar tabela de participantes/co-titulares da conta
CREATE TABLE account_members (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'CO_OWNER',
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_account_member UNIQUE (account_id, user_id)
);

CREATE INDEX idx_account_members_user ON account_members(user_id);
CREATE INDEX idx_account_members_account ON account_members(account_id);

-- 4. Constraint condicional para integridade
ALTER TABLE accounts ADD CONSTRAINT chk_account_classification 
CHECK (
    (classification = 'INDIVIDUAL' AND group_id IS NULL) OR
    (classification = 'JOINT' AND group_id IS NOT NULL)
);
```

---

## 4. Impactos nos Subsistemas & Regras de Negócio

### 4.1. Visualização e Listagem de Contas
- **`ListMyAccountsUseCase`**:
  - Nova lógica: Retornar todas as contas individuais do usuário (`ownerId == userId`) **mais** todas as contas conjuntas em que o usuário está cadastrado em `account_members`.
  - No DTO de resposta (`AccountResponse`), incluir:
    - `classification: AccountClassification`
    - `groupId: UUID?`, `groupName: String?`
    - `participants: List<GroupUserSummary>` (para contas conjuntas)
    - `isCoOwner: Boolean`

### 4.2. Balanço Total Pessoal & Patrimônio (`GetTotalBalanceUseCase`)
Este é um dos pontos arquiteturais mais críticos quando se trata de contas conjuntas:
- **Dilema**: Se uma conta conjunta possui R$ 20.000 e 2 participantes (o casal), qual valor entra no totalizador pessoal de cada um?
- **Estratégia Recomendada**:
  1. **Flag / Configuração por Membro**: Permitir no modelo de membro da conta um toggle `includeInPersonalBalance: Boolean` (padrão: `true`).
  2. **Regra de Apresentação no Frontend**:
     - O backend fornece os números segregados no `TotalBalanceResponse`:
       ```json
       {
         "personalAccountsBalance": 15000,
         "jointAccountsBalance": 20000,
         "total": 35000,
         "monthlyIncome": 5000,
         "monthlyExpense": 3200
       }
       ```
     - A UI oferece um seletor no topo da tela: `[Todos os Saldos | Somente Individuais | Somente Conjuntas]`.

### 4.3. Lançamento e Autorização de Transações (`CreateTransactionUseCase`, `UpdateTransactionUseCase`)
- A verificação de permissão para movimentar a conta passa a ser:
  ```kotlin
  fun checkAccountAccess(account: AccountEntity, userId: UUID) {
      if (account.classification == AccountClassification.INDIVIDUAL) {
          if (account.ownerId != userId) {
              throw AccessDeniedException("Você não tem acesso a esta conta individual.")
          }
      } else if (account.classification == AccountClassification.JOINT) {
          val isParticipant = accountMemberRepository.existsByAccountIdAndUserId(account.id, userId)
          if (!isParticipant) {
              throw AccessDeniedException("Você não é participante desta conta conjunta do grupo.")
          }
      }
  }
  ```
- **Rastreabilidade**: A transação continua gravando `responsibleId` (quem realizou a despesa/receita), permitindo que o extrato da conta conjunta mostre quem comprou cada item.

### 4.4. Transferências entre Contas
Com a introdução formal de contas conjuntas, surgem 4 fluxos de transferência:
1. **Individual (A) ➔ Conjunta (Grupo)**: Aporte / Contribuição para a despesa do casal ou do grupo.
2. **Conjunta (Grupo) ➔ Individual (A)**: Retirada / Reembolso.
3. **Conjunta (Grupo) ➔ Conjunta (Grupo)**: Movimentação entre caixinhas/reservas compartilhadas.
4. **Individual (A) ➔ Individual (B)**: Transferência pessoal direta.

### 4.5. Integração com o Módulo de Dívidas do Grupo (`CalculateGroupDebtsUseCase`)
- **Regra Fundamental**: Quando uma despesa é paga diretamente utilizando uma **Conta Conjunta**, ela **NÃO gera dívida** entre os participantes da conta conjunta no módulo de acerto de contas (`settlements`), pois o recurso financeiro já era compartilhado entre eles.
- Apenas pagamentos feitos com contas **Individuais** ou cartões pessoais em benefício de outros membros do grupo geram lançamentos de débito/crédito a compensar.

### 4.6. Ciclo de Vida do Grupo e dos Membros
1. **Saída de Membro (`LeaveGroupUseCase` / `RemoveMemberUseCase`)**:
   - Se um membro sai do grupo, ele deve ser automaticamente desvinculado de todas as contas conjuntas daquele grupo em que era participante.
   - Caso ele seja o **único participante restante** em uma conta conjunta, o sistema deve impedir a saída até que ele transfira a conta para outro membro ou encerre/transfira os saldos da conta.
2. **Exclusão de Grupo (`DeleteGroupUseCase`)**:
   - A exclusão de um grupo deleta (em cascata ou mediante validação de saldo zero) as contas conjuntas associadas ao grupo.

---

## 5. Arquitetura de Frontend & Experiência do Usuário (UI/UX)

### 5.1. Tela de Contas (`/contas`)
- **Segmentação Visual**:
  - Seção 1: **"Minhas Contas Individuais"** (privadas, ícone de cadeado sutil, saldo pessoal).
  - Seção 2: **"Contas Conjuntas"** (badge do Grupo correspondente, avatares ou fotos dos co-titulares da conta).
- **Criação de Conta (`AccountFormModal.tsx`)**:
  - Campo seletor: `Tipo de Titularidade`:
    - `(•) Individual (Apenas eu)`
    - `( ) Conjunta de Grupo`
  - Se selecionado "Conjunta de Grupo":
    - Dropdown para selecionar o Grupo.
    - Componente de seleção de participantes: lista com avatar e nome de cada membro do grupo, permitindo marcar via checkbox quem são os co-titulares da conta (com o usuário atual marcado por padrão).

### 5.2. Tela do Grupo (`/grupos/:id` -> Aba `Contas`)
- Botão de ação: **"Nova Conta Conjunta"** (abre o modal já contextualizado com aquele grupo pré-selecionado).
- Cada card de conta na lista exibe os avatares dos membros que participam dela.
- Opção para os co-titulares da conta: "Gerenciar Participantes" (adicionar/remover membros do grupo nessa conta específica).

### 5.3. Seletor de Contas em Transações (`TransactionFormModal.tsx`)
- O dropdown de seleção de conta deve agrupar as opções por optgroup ou separador visual claro:
  - **Suas Contas Individuais**:
    - NuBank (Individual)
    - Carteira Física
  - **Contas Conjuntas (Família)**:
    - Itaú Casal (Você, Maria)
  - **Contas Conjuntas (Viagem Amigos)**:
    - Caixinha da Praia (Você, Carlos, Pedro)

---

## 6. Estratégia de Migração de Dados & Compatibilidade Retroativa

1. **Classificação das Contas Existentes**:
   - Todas as contas com `group_id IS NULL` recebem `classification = 'INDIVIDUAL'`.
   - Todas as contas existentes que possuem `group_id IS NOT NULL` (fruto do vínculo antigo) recebem `classification = 'JOINT'`.
2. **Migração para `account_members`**:
   - Para cada conta que virou `JOINT`:
     - Insere um registro em `account_members` para o `owner_id` com role `CO_OWNER`.
     - Insere registros em `account_members` para os demais membros daquele grupo (garantindo que nada que hoje é acessível se torne inacessível abruptamente).
3. **Depreciação dos Endpoints Antigos de Vínculo**:
   - O endpoint `/api/v1/accounts/{id}/group/{groupId}` e a tabela `account_group_requests` serão gradualmente descontinuados, substituídos pela criação direta de contas conjuntas no grupo.

---

## 7. Roadmap de Implementação em 4 Fases

```
┌────────────────────────────────────────────────────────────────────────┐
│                      ROADMAP DE IMPLEMENTAÇÃO                          │
├───────────────────┬────────────────────────────────────────────────────┤
│ Fase 1: Fundação  │ Migração de banco de dados, entidades JPA/Domínio, │
│ & Schema          │ Enum AccountClassification e AccountMemberEntity.  │
├───────────────────┼────────────────────────────────────────────────────┤
│ Fase 2: Regras &  │ UseCases de CRUD de Conta Conjunta, controle de    │
│ APIs              │ acesso granular em transações e listagens.         │
├───────────────────┼────────────────────────────────────────────────────┤
│ Fase 3: Frontend  │ UI de Contas Individuais vs Conjuntas, seleção de  │
│ & Gestão Membros  │ participantes no form, badges e aglutinadores.     │
├───────────────────┼────────────────────────────────────────────────────┤
│ Fase 4: Relatórios│ Balanço patrimonial segregado, relatórios e script │
│ & Go-Live         │ de migração de dados em produção.                  │
└───────────────────┴────────────────────────────────────────────────────┘
```

---

## 8. Riscos, Trade-offs e Decisões Arquiteturais em Aberto

1. **Critério de Governança de Administradores do Grupo**:
   - *Decisão*: Se um usuário é ADMIN do grupo, ele pode visualizar/gerenciar uma conta conjunta da qual não é participante direto?
   - *Recomendação*: **Não**. O isolamento de privacidade da conta conjunta deve prevalecer. Apenas os co-titulares da conta devem ver saldos e extratos da conta. O admin do grupo pode apenas excluir a conta do grupo se necessário para moderação administrativa.
2. **Proporção de Saldo Pessoal**:
   - *Decisão*: Devemos permitir divisão percentual customizada do saldo de contas conjuntas (ex: 60/40) para o cálculo de patrimônio pessoal?
   - *Recomendação*: Para o MVP, manter divisão proporcional simétrica ou toggle individual de visualização (100% vs 0%), postergando divisões percentuais para iterações futuras.
3. **Exclusão de Conta Conjunta**:
   - *Decisão*: Qualquer participante pode excluir a conta conjunta ou apenas quem a criou?
   - *Recomendação*: Exclusão de conta conjunta deve requerer papel `OWNER` na conta ou confirmação de todos os co-titulares caso possua saldo positivo.
