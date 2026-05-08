package io.burgee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.Clock

@SpringBootApplication
@EnableTransactionManagement
class BurgeeApplication {

    @Bean
    fun systemClock(): Clock = Clock.systemUTC()
}

fun main(args: Array<String>) {
    runApplication<BurgeeApplication>(*args)
}
