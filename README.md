# Telegram Order Bot

This project is implementation of Telegram Order Bot.

It is used to make online orders to restaurant through Telegram App.

To synchronize bot with restaurant, restaurant must use **JOWi System**: https://www.jowi.club/

### Configuration in application.yml:
* `bot.bot-token: ${BOT_TOKEN}` Telegram bot token. Can be retrieved after creating Telegram Bot using BotFather https://telegram.me/BotFather
* `bot.username` Telegram bot username. Should be configured on Telegram Bot creation using BotFather https://telegram.me/BotFather
* `jowi.api-key: ${JOWI_API_KEY}` JOWi API Key. Can be retrieved after registering in https://dev.jowi.club
* `jowi.api-secret: ${JOWI_API_SECRET}` JOWi API Secret. Can be retrieved after registering in https://dev.jowi.club
