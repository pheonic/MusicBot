package xyz.pheonic.musicbot

import java.io.File

class Config {
    val token: String
    val prefix: String
    val channels: List<Long>
    val startVolume: Int

    init {
        val fileName = "${System.getProperty("user.home")}${File.separator}.musicbot${File.separator}config.properties"
        val configFile = File(fileName)
        val configMap = HashMap<String, String>()
        configFile.forEachLine {
            if (!it.startsWith('#')) {
                val split = it.split("=")
                configMap[split[0].trim().toLowerCase()] = split[1].trim()
            }
        }
        token = configMap["token"] ?: throw Exception("No token found in file at $fileName")
        prefix = configMap["command_prefix"] ?: throw Exception("No command_prefix found in $fileName")
        startVolume = configMap["start_volume"]?.toIntOrNull() ?: 20
        channels = configMap["command_channels"]?.split(' ')?.map {
            it.toLongOrNull() ?: throw Exception("Could not read channel id in $fileName")
        } ?: throw Exception("No command_channels in $fileName")
    }

}
