package com.finance.application.service

import com.finance.domain.enum.CategoryType
import java.text.Normalizer

object DefaultCategoryCatalog {
    const val OUTROS_SYSTEM_KEY = "OUTROS"

    data class SpecificCategorySpec(
        val systemKey: String,
        val name: String,
        val type: CategoryType,
        val icon: String,
        val color: String,
        val description: String? = null
    )

    data class GeneralCategorySpec(
        val systemKey: String,
        val name: String,
        val type: CategoryType,
        val icon: String,
        val color: String,
        val description: String? = null,
        val specifics: List<SpecificCategorySpec>
    )

    val generals: List<GeneralCategorySpec> = listOf(
        GeneralCategorySpec(
            systemKey = "MORADIA",
            name = "Moradia",
            type = CategoryType.EXPENSE,
            icon = "🏠",
            color = "#7D39EB",
            specifics = listOf(
                SpecificCategorySpec("MORADIA_ALUGUEL", "Aluguel", CategoryType.EXPENSE, "🔑", "#7D39EB"),
                SpecificCategorySpec("MORADIA_CONDOMINIO", "Condomínio", CategoryType.EXPENSE, "🏢", "#7D39EB"),
                SpecificCategorySpec("MORADIA_AGUA", "Água", CategoryType.EXPENSE, "💧", "#7D39EB"),
                SpecificCategorySpec("MORADIA_ENERGIA", "Energia", CategoryType.EXPENSE, "⚡", "#7D39EB"),
                SpecificCategorySpec("MORADIA_GAS", "Gás", CategoryType.EXPENSE, "🔥", "#7D39EB"),
                SpecificCategorySpec("MORADIA_IPTU", "IPTU", CategoryType.EXPENSE, "🏛️", "#7D39EB"),
                SpecificCategorySpec("MORADIA_MANUTENCAO", "Manutenção & Reformas", CategoryType.EXPENSE, "🛠️", "#7D39EB"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "GASTOS_FIXOS",
            name = "Gastos Fixos",
            type = CategoryType.EXPENSE,
            icon = "📌",
            color = "#B285FF",
            specifics = listOf(
                SpecificCategorySpec("GASTOS_FIXOS_INTERNET", "Internet", CategoryType.EXPENSE, "🌐", "#B285FF"),
                SpecificCategorySpec("GASTOS_FIXOS_CELULAR", "Celular / Telefonia", CategoryType.EXPENSE, "📱", "#B285FF"),
                SpecificCategorySpec("GASTOS_FIXOS_ASSINATURAS", "Assinaturas & Streaming", CategoryType.EXPENSE, "📺", "#B285FF"),
                SpecificCategorySpec("GASTOS_FIXOS_SEGUROS", "Seguros", CategoryType.EXPENSE, "🛡️", "#B285FF"),
                SpecificCategorySpec("GASTOS_FIXOS_MENSALIDADES", "Mensalidades", CategoryType.EXPENSE, "📄", "#B285FF"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "ALIMENTACAO",
            name = "Alimentação",
            type = CategoryType.EXPENSE,
            icon = "🍽️",
            color = "#E4FF8F",
            specifics = listOf(
                SpecificCategorySpec("ALIMENTACAO_SUPERMERCADO", "Supermercado", CategoryType.EXPENSE, "🛒", "#E4FF8F"),
                SpecificCategorySpec("ALIMENTACAO_RESTAURANTES", "Restaurantes", CategoryType.EXPENSE, "🍴", "#E4FF8F"),
                SpecificCategorySpec("ALIMENTACAO_DELIVERY", "Delivery / iFood", CategoryType.EXPENSE, "🛵", "#E4FF8F"),
                SpecificCategorySpec("ALIMENTACAO_PADARIA_CAFE", "Padaria & Café", CategoryType.EXPENSE, "☕", "#E4FF8F"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "TRANSPORTE",
            name = "Transporte",
            type = CategoryType.EXPENSE,
            icon = "🚗",
            color = "#8E8EA0",
            specifics = listOf(
                SpecificCategorySpec("TRANSPORTE_COMBUSTIVEL", "Combustível", CategoryType.EXPENSE, "⛽", "#8E8EA0"),
                SpecificCategorySpec("TRANSPORTE_APP_TAXI", "Transporte por App / Táxi", CategoryType.EXPENSE, "🚕", "#8E8EA0"),
                SpecificCategorySpec("TRANSPORTE_PUBLICO", "Transporte Público", CategoryType.EXPENSE, "🚌", "#8E8EA0"),
                SpecificCategorySpec("TRANSPORTE_ESTACIONAMENTO_PEDAGIO", "Estacionamento & Pedágio", CategoryType.EXPENSE, "🅿️", "#8E8EA0"),
                SpecificCategorySpec("TRANSPORTE_MANUTENCAO", "Manutenção do Veículo", CategoryType.EXPENSE, "🔧", "#8E8EA0"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "SAUDE",
            name = "Saúde",
            type = CategoryType.EXPENSE,
            icon = "🏥",
            color = "#F4F4F6",
            specifics = listOf(
                SpecificCategorySpec("SAUDE_PLANO", "Plano de Saúde", CategoryType.EXPENSE, "🩺", "#F4F4F6"),
                SpecificCategorySpec("SAUDE_FARMACIA", "Farmácia", CategoryType.EXPENSE, "💊", "#F4F4F6"),
                SpecificCategorySpec("SAUDE_CONSULTAS_EXAMES", "Consultas & Exames", CategoryType.EXPENSE, "🩻", "#F4F4F6"),
                SpecificCategorySpec("SAUDE_ACADEMIA_BEM_ESTAR", "Academia & Bem-estar", CategoryType.EXPENSE, "🏋️", "#F4F4F6"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "EDUCACAO",
            name = "Educação",
            type = CategoryType.EXPENSE,
            icon = "📚",
            color = "#4A159D",
            specifics = listOf(
                SpecificCategorySpec("EDUCACAO_CURSOS_FACULDADE", "Cursos & Faculdade", CategoryType.EXPENSE, "🎓", "#4A159D"),
                SpecificCategorySpec("EDUCACAO_LIVROS_MATERIAL", "Livros & Material", CategoryType.EXPENSE, "📖", "#4A159D"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "LAZER",
            name = "Lazer & Estilo de Vida",
            type = CategoryType.EXPENSE,
            icon = "🎉",
            color = "#B285FF",
            specifics = listOf(
                SpecificCategorySpec("LAZER_ENTRETENIMENTO_CINEMA", "Entretenimento & Cinema", CategoryType.EXPENSE, "🎬", "#B285FF"),
                SpecificCategorySpec("LAZER_VIAGENS", "Viagens", CategoryType.EXPENSE, "✈️", "#B285FF"),
                SpecificCategorySpec("LAZER_COMPRAS_VESTUARIO", "Compras & Vestuário", CategoryType.EXPENSE, "🛍️", "#B285FF"),
                SpecificCategorySpec("LAZER_PRESENTES", "Presentes", CategoryType.EXPENSE, "🎁", "#B285FF"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "FINANCEIRO",
            name = "Serviços Financeiros",
            type = CategoryType.EXPENSE,
            icon = "💳",
            color = "#38384A",
            specifics = listOf(
                SpecificCategorySpec("FINANCEIRO_TARIFAS_ANUIDADES", "Tarifas & Anuidades", CategoryType.EXPENSE, "🏦", "#38384A"),
                SpecificCategorySpec("FINANCEIRO_JUROS_MULTAS", "Juros & Multas", CategoryType.EXPENSE, "⚠️", "#38384A"),
                SpecificCategorySpec("FINANCEIRO_EMPRESTIMOS", "Empréstimos & Financiamentos", CategoryType.EXPENSE, "💸", "#38384A"),
                SpecificCategorySpec("FINANCEIRO_IMPOSTOS", "Impostos", CategoryType.EXPENSE, "🧾", "#38384A"),
            )
        ),
        GeneralCategorySpec(
            systemKey = "RENDA",
            name = "Renda",
            type = CategoryType.INCOME,
            icon = "💰",
            color = "#C6FF33",
            specifics = listOf(
                SpecificCategorySpec("RENDA_SALARIO", "Salário", CategoryType.INCOME, "💼", "#C6FF33"),
                SpecificCategorySpec("RENDA_FREELANCE", "Freelance & Extras", CategoryType.INCOME, "💻", "#C6FF33"),
                SpecificCategorySpec("RENDA_RENDIMENTOS", "Rendimentos & Dividendos", CategoryType.INCOME, "📈", "#C6FF33"),
                SpecificCategorySpec("RENDA_REEMBOLSOS", "Reembolsos", CategoryType.INCOME, "🔄", "#C6FF33"),
                SpecificCategorySpec("RENDA_BONUS", "Bônus & 13º", CategoryType.INCOME, "🎊", "#C6FF33"),
                SpecificCategorySpec("RENDA_OUTRAS", "Outras Receitas", CategoryType.INCOME, "💵", "#C6FF33"),
            )
        ),
        GeneralCategorySpec(
            systemKey = OUTROS_SYSTEM_KEY,
            name = "Outros",
            type = CategoryType.BOTH,
            icon = "📦",
            color = "#8E8EA0",
            specifics = listOf(
                SpecificCategorySpec("OUTROS_DESPESAS_DIVERSAS", "Despesas Diversas", CategoryType.EXPENSE, "📌", "#8E8EA0"),
                SpecificCategorySpec("OUTROS_TRANSFERENCIAS_AJUSTES", "Transferências & Ajustes", CategoryType.BOTH, "🔁", "#8E8EA0"),
            )
        )
    )

    val outrosSpec: GeneralCategorySpec by lazy {
        generals.first { it.systemKey == OUTROS_SYSTEM_KEY }
    }

    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val whitespaceRegex = Regex("\\s+")

    fun normalizeName(name: String): String {
        val decomposed = Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
        return diacriticsRegex.replace(decomposed, "").replace(whitespaceRegex, " ")
    }
}
