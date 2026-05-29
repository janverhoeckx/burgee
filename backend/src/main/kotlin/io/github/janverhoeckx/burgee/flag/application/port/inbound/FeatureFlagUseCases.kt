package io.github.janverhoeckx.burgee.flag.application.port.inbound

import io.github.janverhoeckx.burgee.flag.domain.FeatureFlag
import java.util.UUID

fun interface ListFlagsUseCase {
    fun list(): List<FeatureFlag>
}

interface GetFlagByIdUseCase {
    sealed interface Result {
        data class Found(val flag: FeatureFlag) : Result
        data object NotFound : Result
    }

    fun getById(id: UUID): Result
}

interface GetFlagByKeyUseCase {
    sealed interface Result {
        data class Found(val flag: FeatureFlag) : Result
        data object NotFound : Result
    }

    fun getByKey(key: String): Result
}

interface CreateFlagUseCase {
    data class Command(
        val key: String,
        val name: String,
        val description: String?,
        val enabled: Boolean,
    )

    sealed interface Result {
        data class Created(val flag: FeatureFlag) : Result
        data class DuplicateKey(val key: String) : Result
    }

    fun create(command: Command): Result
}

interface UpdateFlagUseCase {
    data class Command(
        val id: UUID,
        val name: String,
        val description: String?,
        val enabled: Boolean,
    )

    sealed interface Result {
        data class Updated(val flag: FeatureFlag) : Result
        data object NotFound : Result
    }

    fun update(command: Command): Result
}

interface ToggleFlagUseCase {
    sealed interface Result {
        data class Toggled(val flag: FeatureFlag) : Result
        data object NotFound : Result
    }

    fun toggle(id: UUID): Result
}

interface DeleteFlagUseCase {
    sealed interface Result {
        data object Deleted : Result
        data object NotFound : Result
    }

    fun delete(id: UUID): Result
}
