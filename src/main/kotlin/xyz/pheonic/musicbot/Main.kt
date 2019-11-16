package xyz.pheonic.musicbot

import mu.KotlinLogging
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder

val logger = KotlinLogging.logger { }
fun main() {
    logger.debug("Starting...")
    val config = Config()
    val client = createClient(config)
    val listener = createListener(config, client)
    client.addEventListener(listener)
}

fun createListener(config: Config, client: JDA): Listener {
    logger.debug("Creating listener")
    return Listener(client, config)
}

fun createClient(config: Config): JDA {
    logger.debug("Creating client")
    return JDABuilder(AccountType.BOT).setToken(config.token).build()
}
