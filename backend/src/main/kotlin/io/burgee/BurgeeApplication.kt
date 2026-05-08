package io.burgee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BurgeeApplication

fun main(args: Array<String>) {
    runApplication<BurgeeApplication>(*args)
}
