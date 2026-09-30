package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table("flag_condition")
data class FlagConditionRow(
    val attribute: String,
    val operator: String,
    @Column("values")
    val values: List<String>,
)
