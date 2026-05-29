package io.github.janverhoeckx.burgee.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RoleTest {

    @Test
    fun `authority prefixes the role name with ROLE_`() {
        assertThat(Role.ADMIN.authority).isEqualTo("ROLE_ADMIN")
        assertThat(Role.USER.authority).isEqualTo("ROLE_USER")
        assertThat(Role.NEW.authority).isEqualTo("ROLE_NEW")
    }

    @Test
    fun `isAdmin is true only for ADMIN`() {
        assertThat(Role.ADMIN.isAdmin).isTrue()
        assertThat(Role.USER.isAdmin).isFalse()
        assertThat(Role.NEW.isAdmin).isFalse()
    }
}
