package com.finance

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(excludeName = ["org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"])
@EnableScheduling
class FinanceApplication

fun main(args: Array<String>) {
    runApplication<FinanceApplication>(*args)
}
