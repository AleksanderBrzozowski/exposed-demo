package com.brzozowski

import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer

@Profile("test")
@SpringBootTest
class SpringExposedDemoApplicationTests {


    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Test
    fun contextLoads() {
        transactionTemplate.transactionManager
    }


    companion object {

        @ClassRule
        @JvmField
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")
                .apply {
                    withDatabaseName("test")
                    withUsername("test")
                    withPassword("test")
                    start()
                }

        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry): Unit {
            registry.add("spring.datasource.url") { postgreSQLContainer.jdbcUrl }
        }
    }
}
