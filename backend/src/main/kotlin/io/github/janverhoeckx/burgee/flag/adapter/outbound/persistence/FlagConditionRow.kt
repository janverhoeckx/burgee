package io.github.janverhoeckx.burgee.flag.adapter.outbound.persistence

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

/** One row of `flag_condition`; `flag_id` and `position` are managed by Spring Data JDBC. */
@Table("flag_condition")
data class FlagConditionRow(
    val attribute: String,
    val operator: String,
    @Column("values")
    val values: List<String>,
)
