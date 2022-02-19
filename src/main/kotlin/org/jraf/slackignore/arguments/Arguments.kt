/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalCli::class)

package org.jraf.slackignore.arguments

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.multiple
import kotlinx.cli.required
import org.jraf.slackignore.slack.WebSocketHandler

class Arguments(av: Array<String>) {
    private val parser = ArgParser("slack-ignore")

    val slackAuthToken: String by parser.option(
        type = ArgType.String,
        fullName = "slack-auth-token",
        shortName = "t",
        description = "Slack xoxc token"
    ).required()

    val slackCookie: String by parser.option(
        type = ArgType.String,
        fullName = "slack-cookie",
        shortName = "c",
        description = "Slack 'd' cookie"
    ).required()

    private val ignoreRuleStrings: List<String> by parser.option(
        type = ArgType.String,
        fullName = "ignore",
        shortName = "i",
        description = "Add an ignore rule"
    ).multiple()

    val ignoreRules = mutableListOf<WebSocketHandler.IgnoreRule>()

    init {
        parser.parse(av)
        for (ignoreRuleString in ignoreRuleStrings) {
            ignoreRules.add(IgnoreRuleArguments(ignoreRuleString).toIgnoreRule())
        }
    }

    class IgnoreRuleArguments(ignoreRuleString: String) {
        private val parser = ArgParser("ignore")

        private val channelName: String? by parser.option(
            type = ArgType.String,
            fullName = "channel-name",
            shortName = "c",
            description = "Channel name regexp"
        )

        private val messageAuthorNickname: String? by parser.option(
            type = ArgType.String,
            fullName = "message-author-nickname",
            shortName = "n",
            description = "Message author nickname regexp"
        )

        private val messageAuthorRealName: String? by parser.option(
            type = ArgType.String,
            fullName = "message-author-real-name",
            shortName = "r",
            description = "Message author real name regexp"
        )

        private val messageAuthorIsBot: Boolean? by parser.option(
            type = ArgType.Boolean,
            fullName = "message-author-is-bot",
            shortName = "b",
            description = "Message author is bot"
        )

        private val messageText: String? by parser.option(
            type = ArgType.String,
            fullName = "message-text",
            shortName = "t",
            description = "Message text regexp"
        )

        init {
            parser.parse(ignoreRuleString.split(' ').toTypedArray())
        }

        fun toIgnoreRule(): WebSocketHandler.IgnoreRule {
            return WebSocketHandler.IgnoreRule(
                channelName = channelName,
                messageAuthorNickname = messageAuthorNickname,
                messageAuthorRealName = messageAuthorRealName,
                messageAuthorIsBot = messageAuthorIsBot,
                messageText = messageText,
            )
        }
    }

}
