package com.brzozowski

import org.junit.ClassRule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    fun insertTest() {
        val timestamp = Instant.now()
        val person = Person("abrzozo1", "Aleksander Brzozowski", timestamp, null)
        personRepository.insert(person)
        val find = personRepository.find("abrzozo1")
    }

    @Test
    fun optimisticLocking() {
        val timestamp = Instant.now()
        val person = Person("abrzozo1", "Aleksander Brzozowski", timestamp, Jsonb("{}"))
        personRepository.insert(person)
        val versionedPerson = personRepository.find("abrzozo1")!!

        personRepository.update(person, versionedPerson.version)

        assertThrows<PersonRepository.OptimisticLockException> { personRepository.update(person, versionedPerson.version) }
    }

    @Test
    fun duplicatedPerson() {
        val timestamp = Instant.now()
        val person = Person("abrzozo1", "Aleksander Brzozowski", timestamp, Jsonb("{}"))
        personRepository.insert(person)

        assertThrows<PersonRepository.DuplicatedPersonException> { personRepository.insert(person) }
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
