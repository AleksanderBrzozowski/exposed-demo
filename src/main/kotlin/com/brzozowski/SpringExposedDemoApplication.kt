package com.brzozowski

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.`java-time`.timestamp
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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
import java.time.Instant
import javax.sql.DataSource

@Repository
class PostgresPersonRepository(private val inTransaction: TransactionExecutor) : PersonRepository {

    override fun insert(person: Person): Unit = inTransaction {
        PersonTable.insert {
            it[id] = person.id
            it[name] = person.name
            it[timestamp] = person.timestamp
        }
    }

    override fun findAll(): List<Person> = inTransaction {
        PersonTable.selectAll()
                .map { it.toPerson() }
    }

    private fun ResultRow.toPerson() = Person(
            id = this[PersonTable.id],
            name = this[PersonTable.name],
            timestamp = this[PersonTable.timestamp]
    )
}

interface PersonRepository {
    fun insert(person: Person)
    fun findAll(): List<Person>
}

data class Person(val id: String, val name: String, val timestamp: Instant)

object PersonTable : Table("person") {

    val id = text("id")
    val name = text("name")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}

class TransactionExecutor(private val transactionManager: PlatformTransactionManager) {


    operator fun <T : Any> invoke(isolationLevel: Isolation = Isolation.DEFAULT,
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


