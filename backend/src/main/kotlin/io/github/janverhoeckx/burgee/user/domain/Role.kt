package io.github.janverhoeckx.burgee.user.domain

enum class Role {
    /** Full management permissions. */
    ADMIN,

    /** Standard member access granted by an admin; no management permissions. */
    USER,

    /** Default role for newly provisioned users; no permissions until an admin upgrades them. */
    NEW,
    ;

    val authority: String get() = "ROLE_$name"
    val isAdmin: Boolean get() = this == ADMIN
}
