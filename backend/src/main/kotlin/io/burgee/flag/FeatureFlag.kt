package io.burgee.flag

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "feature_flags")
class FeatureFlag(
    @Column(nullable = false, unique = true, length = 128)
    var key: String,

    @Column(nullable = false, length = 256)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
)
