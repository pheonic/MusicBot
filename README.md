# MusicBot
A discord bot for playing music. Meant to be a self hosted solution rather than having one bot that is added to every server. Though this bot should be able to handle many servers.

This bot is built with [lavaplayer](https://github.com/sedmelluq/lavaplayer) and [JDA](https://github.com/DV8FromTheWorld/JDA). It is also based on [Just-Some-Bots/MusicBot](https://github.com/Just-Some-Bots/MusicBot), many of the base ideas came from that bot.

# Compiling
For now if you want to use this bot you'll have to compile it into a jar yourself.

You can clone this repo and run `./gradlew shadowJar` which will produce a fatJar in the build/lib folder then you can use `java -jar MusicBot-fat-1.0-SNAPSHOT.jar` to run it.

You will also need to create a config file in your home directory `~/.musicbot/config.properties`

## Config skeleton
```
token=<bot_token>
# Commands will be prefixed with ! so for example !play <url>
command_prefix=!
# 20 seems like a reasonable start volume
start_volume=20
# Command channels should be separated by spaces, the bot will only listen to text commands in the listed channels
command_channels=<channel ids>
```

# Basic Commands
- summon : Connects the bot to the channel that the user who summoned the bot is connected to
- play <url> : Queues a song to play by url. Supports youtube, bandcamp and more. See [lavaplayer's supported formats](https://github.com/sedmelluq/lavaplayer/blob/master/README.md#supported-formats)
- skip : Skips the current song
- clear : Clears the song queue
- disconnect : Disconnects the bot from its voice channel
- pause 
- resume
- queue : Views all the songs currently in the queue
- shuffle
- volume <number>
- musicbot-help

You can see the rest of the commands by using the musicbot-help command 

# Contributing
If you want to contribute you can make a pull request and I'll take a look at it. 

You can also feel free to make an issue with a feature request or bug.
