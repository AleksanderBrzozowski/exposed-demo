package com.brzozowski

import org.junit.Assert.assertEquals
import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant

@SpringBootTest
class SpringExposedDemoApplicationTests {


    @Autowired
    lateinit var personRepository: PersonRepository

    @Test
    fun personTest() {
        val timestamp = Instant.now()
        personRepository.insert(Person("abrzozo1", "Aleksander Brzozowski", timestamp))
        assertEquals(personRepository.findAll(), listOf(Person("abrzozo1", "Aleksander Brzozowski", timestamp)))
    }


    companion object {

        @ClassRule
        @JvmField
        val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")
                .apply {
                    withDatabaseName("test")
                    withUsername("test")
                    withPassword("test")
                    withInitScript("init.sql")
                    start()
                }

        @DynamicPropertySource
        @JvmStatic
        fun dbProperties(registry: DynamicPropertyRegistry): Unit {
            registry.add("spring.datasource.url") { postgreSQLContainer.jdbcUrl }
        }
    }
}
