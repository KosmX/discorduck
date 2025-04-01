package io.github.populus_omnibus.vikbot.api.test

import dev.kosmx.discorducky.eliza.ElizaWrapper
import org.junit.jupiter.api.Test
import java.util.Scanner

object TestEliza {

    @JvmStatic
    fun main(args: Array<String>) {
        val eliza = ElizaWrapper(false)
        eliza.memSize = 100

        val scanner = Scanner(System.`in`)
        while (true) {
            val next = scanner.nextLine()
            println(eliza.transform(next))
        }
    }
}