/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2022-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jraf.slackignore

import org.jraf.slackignore.arguments.Arguments
import org.jraf.slackignore.slack.SlackClient
import org.jraf.slackignore.slack.WebSocketHandler
import org.slf4j.LoggerFactory
import org.slf4j.impl.SimpleLogger

private val LOGGER = run {
    // This must be done before any logger is initialized
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "trace")

    LoggerFactory.getLogger("Main")
}

suspend fun main(av: Array<String>) {
    LOGGER.info("Hello, World!")
    val arguments = Arguments(av)

    val slackClient = SlackClient(authToken = arguments.slackAuthToken, cookie = arguments.slackCookie)

//    slackClient.chatPostMessage(
//        channel = "C01QX5H7C2J",
//        text = "The current time is ${Date()}"
//    )

    val tokenOwnerId = slackClient.tokenOwnerId()
    LOGGER.debug("Token owner's id: $tokenOwnerId")

    val webSocketUrl = slackClient.rtmConnect()
    LOGGER.debug("Connecting to WebSocket webSocketUrl=$webSocketUrl")

    slackClient.startWebSocket(webSocketUrl, WebSocketHandler(tokenOwnerId = tokenOwnerId))
}
