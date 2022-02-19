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

package org.jraf.slackignore.slack

import org.jraf.slackignore.slack.apimodels.response.SlackApiChannel
import org.jraf.slackignore.slack.apimodels.response.SlackApiMember
import org.slf4j.LoggerFactory

private val LOGGER = LoggerFactory.getLogger(WebSocketHandler::class.java)

class WebSocketHandler(
    val tokenOwnerId: String,
    memberList: List<SlackApiMember>,
    channelList: List<SlackApiChannel>,

    private val ignoreRules: List<IgnoreRule>,
) {
    val membersById: Map<String, SlackApiMember> = memberList.associateBy { it.id }
    val channelsById: Map<String, SlackApiChannel> = channelList.associateBy { it.id }

    fun shouldIgnore(message: ChannelMessage): Boolean {
        LOGGER.debug("shouldIgnore message=$message")

        return ignoreRules.any { rule ->
            (rule.channelName == null || message.channelName != null && message.channelName.matches(rule.channelName)) &&
                    (rule.messageAuthorNickName == null || message.authorNickName != null && message.authorNickName.matches(
                        rule.messageAuthorNickName)) &&
                    (rule.messageAuthorRealName == null || message.authorRealName.matches(rule.messageAuthorRealName)) &&
                    (rule.messageAuthorIsBot == null || message.authorIsBot == rule.messageAuthorIsBot) &&
                    (rule.messageText == null || message.text.matches(rule.messageText))
        }
    }

    data class ChannelMessage(
        /**
         * The channel name, or `null` if the message is not in a channel (private message).
         */
        val channelName: String?,
        val authorNickName: String?,
        val authorRealName: String,
        val authorIsBot: Boolean,
        val text: String,
    )

    class IgnoreRule(
        val channelName: Regex?,
        val messageAuthorNickName: Regex?,
        val messageAuthorRealName: Regex?,
        val messageAuthorIsBot: Boolean?,
        val messageText: Regex?,
    ) {
        constructor(
            channelName: String? = null,
            messageAuthorNickName: String? = null,
            messageAuthorRealName: String? = null,
            messageAuthorIsBot: Boolean? = null,
            messageText: String? = null,
        ) : this(
            channelName = channelName?.let { Regex(it) },
            messageAuthorNickName = messageAuthorNickName?.let { Regex(it) },
            messageAuthorRealName = messageAuthorRealName?.let { Regex(it) },
            messageAuthorIsBot = messageAuthorIsBot,
            messageText = messageText?.let { Regex(it) }
        )
    }
}
