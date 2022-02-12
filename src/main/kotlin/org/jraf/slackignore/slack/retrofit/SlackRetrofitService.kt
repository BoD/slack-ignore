/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2020-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.slackignore.slack.retrofit

import org.jraf.slackignore.slack.apimodels.query.SlackApiChatPostMessageQuery
import org.jraf.slackignore.slack.apimodels.query.SlackApiConversationsMarkQuery
import org.jraf.slackignore.slack.apimodels.response.SlackApiChatPostMessageResponse
import org.jraf.slackignore.slack.apimodels.response.SlackApiConversationsHistoryResponse
import org.jraf.slackignore.slack.apimodels.response.SlackApiConversationsInfoResponse
import org.jraf.slackignore.slack.apimodels.response.SlackApiConversationsListResponse
import org.jraf.slackignore.slack.apimodels.response.SlackApiConversationsMarkResponse
import org.jraf.slackignore.slack.apimodels.response.SlackApiRtmConnectResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SlackRetrofitService {
    // https://api.slack.com/methods/chat.postMessage
    @POST("chat.postMessage")
    suspend fun chatPostMessage(
        @Body
        query: SlackApiChatPostMessageQuery,
    ): SlackApiChatPostMessageResponse

    // https://api.slack.com/methods/rtm.connect
    @POST("rtm.connect")
    suspend fun rtmConnect(): SlackApiRtmConnectResponse

    // https://api.slack.com/methods/conversations.info
    @GET("conversations.info")
    suspend fun conversationsInfo(
        @Query("channel")
        channel: String,
    ): SlackApiConversationsInfoResponse

    // https://api.slack.com/methods/conversations.history
    @GET("conversations.history")
    suspend fun conversationsHistory(
        @Query("channel")
        channel: String,
    ): SlackApiConversationsHistoryResponse

    // https://api.slack.com/methods/conversations.mark
    @POST("conversations.mark")
    suspend fun conversationsMark(
        @Body
        query: SlackApiConversationsMarkQuery,
    ): SlackApiConversationsMarkResponse


    // https://api.slack.com/methods/conversations.list
    @GET("conversations.list")
    suspend fun conversationsList(
        @Query("cursor")
        cursor: String? = null,

        @Query("exclude_archived")
        excludeArchived: Boolean = true,

        @Query("types")
        types: String = "public_channel,private_channel",
    ): SlackApiConversationsListResponse

}
