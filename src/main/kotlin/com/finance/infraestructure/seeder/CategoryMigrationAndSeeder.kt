package com.finance.infraestructure.seeder

import com.finance.application.service.CategorySeedService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(2)
class CategoryMigrationAndSeeder(
    private val categorySeedService: CategorySeedService
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String) {
        logger.info("Iniciando verificação de seeding e migração hierárquica de categorias...")
        try {
            categorySeedService.migrateLegacyCategoriesAndTransactions()
            logger.info("Seeding e migração hierárquica de categorias concluídos com sucesso.")
        } catch (ex: Exception) {
            logger.error("Erro ao executar seeding e migração de categorias: ${ex.message}", ex)
            throw ex
        }
    }
}
