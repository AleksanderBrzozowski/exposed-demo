package com.brzozowski

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

data class Jsonb(val raw: String)

class JsonbColumn: ColumnType() {

    override fun sqlType(): String = "jsonb"

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val jsonb = PGobject().apply {
            this.type = "jsonb"
            this.value = (value as Jsonb?)?.raw
        }
        stmt[index] = jsonb
    }
}

fun Table.jsonb(name: String): Column<Jsonb> = registerColumn(name, JsonbColumn())
