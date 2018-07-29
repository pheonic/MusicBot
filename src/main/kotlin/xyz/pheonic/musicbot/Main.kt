package xyz.pheonic.musicbot

import mu.KotlinLogging
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

val logger = KotlinLogging.logger { }
fun main(args: Array<String>) {
    logger.debug("Starting...")
    val config = Config()
    val client = createClient(config)
    val listener = createListener(config, client)
    client.dispatcher.registerListener(listener)
}

fun createListener(config: Config, client: IDiscordClient): Listener {
    logger.debug("Creating listener")
    return Listener(client, config)
}

fun createClient(config: Config): IDiscordClient {
    logger.debug("Creating client")
    val clientBuilder = ClientBuilder()
    clientBuilder.withToken(config.token)
    return checkNotNull(clientBuilder.login())
}
