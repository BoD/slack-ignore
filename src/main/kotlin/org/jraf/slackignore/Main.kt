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

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.jraf.slackignore.arguments.Arguments
import org.jraf.slackignore.slack.SlackClient
import org.jraf.slackignore.slack.WebSocketHandler
import org.slf4j.LoggerFactory
import org.slf4j.impl.SimpleLogger

private val LOGGER = run {
    // This must be done before any logger is initialized
    System.setProperty(SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "trace")
    System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")
    System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss")

    LoggerFactory.getLogger("Main")
}

@OptIn(DelicateCoroutinesApi::class)
suspend fun main(av: Array<String>) {
    LOGGER.info("Hello, World!")
    val arguments = Arguments(av)

    val slackClient = SlackClient(authToken = arguments.slackAuthToken, cookie = arguments.slackCookie)

    val tokenOwnerId = slackClient.tokenOwnerId()
    LOGGER.debug("Token owner's id: $tokenOwnerId")

    LOGGER.info("Getting the list of all members (that could take a while)...")
    val allMembers = GlobalScope.async { slackClient.getAllMembers() }

    LOGGER.info("Getting the list of all channels (that could take a while)...")
    val allChannels = GlobalScope.async { slackClient.getAllChannels() }

    while (true) {
        val webSocketUrl = slackClient.rtmConnect()
        LOGGER.debug("Connecting to WebSocket webSocketUrl=$webSocketUrl")

        slackClient.startWebSocket(
            webSocketUrl,
            WebSocketHandler(
                tokenOwnerId = tokenOwnerId,
                memberList = allMembers.await(),
                channelList = allChannels.await(),
                ignoreRules = arguments.ignoreRules
            )
        )
        LOGGER.debug("Was disconnected, reconnecting...")
    }
}
