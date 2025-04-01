package dev.kosmx.discorducky.eliza

import org.graalvm.polyglot.Context
import java.lang.ref.Cleaner

class ElizaWrapper(noRandom: Boolean = false): AutoCloseable {
    private val context = Context.create()


    companion object {
        private val cleaner = Cleaner.create()

        val elizajs = ElizaWrapper::class.java.getResourceAsStream("/eliza/elizabot.js").bufferedReader().use { it.readText() }
        val elizadata = ElizaWrapper::class.java.getResourceAsStream("/eliza/elizadata.js").bufferedReader().use { it.readText() }
    }

    init {
        context.eval("js", elizadata)
        context.eval("js", elizajs)

    }
    val closeable = cleaner.register(this, Resource(context))!!

    private val elizaBot = context.eval("js", "new ElizaBot($noRandom)")


    override fun close() {
        closeable.clean()
    }


    fun transform(inputString: String): String {
        return elizaBot.invokeMember("transform", inputString).asString()
    }

    var memSize: Int
        get() = elizaBot.getMember("memSize").asInt()
        set(value) {
            elizaBot.putMember("memSize", value)
        }

    private data class Resource(val context: Context): Runnable {
        override fun run() {
            context.close()
        }
    }
}