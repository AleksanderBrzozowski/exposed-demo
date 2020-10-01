package com.brzozowski

import org.jetbrains.exposed.spring.SpringTransactionManager
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionTemplate
import javax.sql.DataSource

@SpringBootApplication
class SpringExposedDemoApplication(private val transactionTemplate: TransactionTemplate): CommandLineRunner {


	override fun run(vararg args: String?) {

	}


}

@Configuration
class AppConfig {

	@Bean
	fun transactionManager(dataSource: DataSource) = SpringTransactionManager(dataSource)
}

fun main(args: Array<String>) {
	runApplication<SpringExposedDemoApplication>(*args)
}


