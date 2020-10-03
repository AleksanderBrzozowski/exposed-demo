package com.brzozowski

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.support.TransactionTemplate
import java.lang.RuntimeException
import java.time.Instant
import javax.sql.DataSource

@Repository
class PostgresPersonRepository(private val inTransaction: TransactionExecutor) : PersonRepository {

    override fun insert(person: Person): Unit = inTransaction {
        try {
            PersonTable.insert {
                it[id] = person.id
                it[name] = person.name
                it[timestamp] = person.timestamp
                it[version] = 0
                it[metadata] = person.metadata
            }
        } catch (t: ExposedSQLException) {
            when (t.sqlState) {
                PostgresErrorCodes.UNIQUE_VIOLATION -> throw PersonRepository.DuplicatedPersonException()
                else -> throw t
            }
        }
    }

    override fun update(person: Person, version: Int): Unit = inTransaction {
        val updatedRows = PersonTable
                .update({ (PersonTable.id eq person.id) and (PersonTable.version eq version) }) {
                    it[name] = person.name
                    it[timestamp] = person.timestamp
                    it[PersonTable.version] = version + 1
                }

        if (updatedRows == 0) {
            throw PersonRepository.OptimisticLockException()
        }
    }


    override fun find(id: String): VersionedPerson? = inTransaction {
        PersonTable.select { PersonTable.id eq id }
                .singleOrNull()
                ?.let { VersionedPerson(it.toPerson(), it[PersonTable.version]) }
    }

    override fun findAll(): List<Person> = inTransaction {
        PersonTable.selectAll()
                .map { it.toPerson() }
    }

    private fun ResultRow.toPerson() = Person(
            id = this[PersonTable.id],
            name = this[PersonTable.name],
            timestamp = this[PersonTable.timestamp],
            metadata = this[PersonTable.metadata]
    )
}

interface PersonRepository {
    @Throws(DuplicatedPersonException::class)
    fun insert(person: Person)

    @Throws(OptimisticLockException::class)
    fun update(person: Person, version: Int)

    fun find(id: String): VersionedPerson?

    fun findAll(): List<Person>

    class DuplicatedPersonException : RuntimeException("Duplicate")

    class OptimisticLockException : RuntimeException("Optimistic Lock")
}

data class Person(val id: String, val name: String, val timestamp: Instant, val metadata: Jsonb?)
data class VersionedPerson(val person: Person, val version: Int)

object PersonTable : Table("person") {

    val id = text("id")
    val name = text("name")
    val timestamp = timestamp("timestamp")
    val version = integer("version")
    val metadata = jsonb("metadata").nullable()

    override val primaryKey = PrimaryKey(id)
}

class TransactionExecutor(private val transactionManager: PlatformTransactionManager) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T> invoke(isolationLevel: Isolation = Isolation.DEFAULT,
                            propagation: Propagation = Propagation.REQUIRED,
                            callback: (TransactionStatus) -> T): T {
        return TransactionTemplate(transactionManager).let {
            it.isolationLevel = isolationLevel.value()
            it.propagationBehavior = propagation.value()

            it.execute(callback) as T
        }
    }

}

@Configuration
class AppConfig {

    @Bean
    fun transactionManager(dataSource: DataSource) = SpringTransactionManager(dataSource)

    @Bean
    fun transactionExecutor(transactionManager: SpringTransactionManager) = TransactionExecutor(transactionManager)

}

@SpringBootApplication
class SpringExposedDemoApplication

fun main(args: Array<String>) {
    runApplication<SpringExposedDemoApplication>(*args)
}


