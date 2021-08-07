package xyz.pheonic.musicbot

import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent

val logger = KotlinLogging.logger { }
fun main() {
    logger.debug("Starting...")
    val client = createClient(Config)
    val listener = createListener(Config, client)
    client.addEventListener(listener)
}

fun createListener(config: Config, client: JDA): MusicBot {
    logger.debug("Creating listener")
    return MusicBot(client, config)
}

fun createClient(config: Config): JDA {
    logger.debug("Creating client")
    return JDABuilder.create(config.token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_VOICE_STATES).build()
}
