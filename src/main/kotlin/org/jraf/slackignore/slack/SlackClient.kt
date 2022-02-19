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

import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.internal.notify
import okhttp3.internal.wait
import okio.ByteString
import org.jraf.slackignore.slack.apimodels.query.SlackApiChatPostMessageQuery
import org.jraf.slackignore.slack.apimodels.query.SlackApiConversationsMarkQuery
import org.jraf.slackignore.slack.apimodels.response.SlackApiChannel
import org.jraf.slackignore.slack.apimodels.response.SlackApiMember
import org.jraf.slackignore.slack.apimodels.response.SlackApiMessage
import org.jraf.slackignore.slack.apimodels.response.SlackApiWebSocketMessage
import org.jraf.slackignore.slack.apimodels.response.SlackApiWebSocketMessageMessage
import org.jraf.slackignore.slack.retrofit.SlackRetrofitService
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

private val LOGGER = LoggerFactory.getLogger(SlackClient::class.java)

private const val SLACK_BASE_URI = "https://slack.com/api/"
private const val USE_PROXY = false

class SlackClient(
    private val authToken: String,
    private val cookie: String,
) {
    private fun createRetrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient.Builder()
                .apply {
                    if (USE_PROXY) {
                        sslSocketFactory(trustAllCertsSslSocketFactory(), trustAllCerts()[0])
                        proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 8888)))
                    }
                }
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $authToken")
                        .addHeader("Cookie", "d=${cookie}")
                        .build()
                    chain.proceed(request)
                }
                .build()
        )
        .baseUrl(SLACK_BASE_URI)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    private val service: SlackRetrofitService = createRetrofit().create(SlackRetrofitService::class.java)

    suspend fun tokenOwnerId(): String {
        return try {
            val response = service.authTest()
            response.userId
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun chatPostMessage(channel: String, text: String): Boolean {
        return try {
            val response = service.chatPostMessage(SlackApiChatPostMessageQuery(channel = channel, text = text))
            response.ok
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            false
        }
    }

    suspend fun rtmConnect(): String {
        return try {
            val response = service.rtmConnect()
            response.url
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun lastReadMessageTs(channel: String): String {
        return try {
            val response = service.conversationsInfo(channel)
            response.channel.lastRead!!
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun conversationsHistory(channel: String): List<SlackApiMessage> {
        return try {
            val response = service.conversationsHistory(channel)
            response.messages
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun conversationsMark(channel: String, ts: String): Boolean {
        return try {
            val response = service.conversationsMark(SlackApiConversationsMarkQuery(channel = channel, ts = ts))
            response.ok
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun getAllMembers(): List<SlackApiMember> {
        return try {
            val memberList = mutableListOf<SlackApiMember>()
            var cursor: String? = null
            do {
                val response = service.usersList(cursor = cursor)
                memberList += response.members
                cursor = response.responseMetadata?.nextCursor?.ifBlank { null }
            } while (cursor != null)
            memberList
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    suspend fun getAllChannels(): List<SlackApiChannel> {
        return try {
            val channelList = mutableListOf<SlackApiChannel>()
            var cursor: String? = null
            do {
                val response = service.conversationsList(cursor = cursor)
                channelList += response.channels
                cursor = response.responseMetadata?.nextCursor?.ifBlank { null }
            } while (cursor != null)
            channelList
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            throw e
        }
    }

    fun startWebSocket(url: String, handler: WebSocketHandler) {
        val syncObject = url

        val moshi: Moshi = Moshi.Builder().build()
        val messageJsonAdapter = moshi.adapter(SlackApiWebSocketMessage::class.java)
        val messageMessageJsonAdapter = moshi.adapter(SlackApiWebSocketMessageMessage::class.java)

        val httpClient = OkHttpClient.Builder()
            .pingInterval(60, TimeUnit.SECONDS) // set ping frame sending interval
            .build()
        val request = Request.Builder()
            .url(url)
            .build()
        httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                LOGGER.debug("WebSocket opened $response")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                LOGGER.debug("WebSocket message $text")
                try {
                    val message = messageJsonAdapter.fromJson(text)!!
                    if (message.type == "message" && (message.subtype == null || message.subtype == "bot_message")) {
                        val messageMessage = messageMessageJsonAdapter.fromJson(text)!!
                        handleChannelMessage(messageMessage, handler)
                    }
                } catch (e: Exception) {
                    LOGGER.error("Exception while handling message", e)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                LOGGER.debug("WebSocket message $bytes")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                LOGGER.debug("WebSocket closing $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                LOGGER.debug("WebSocket closed $code $reason")
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LOGGER.debug("WebSocket failure $t $response")
                synchronized(syncObject) {
                    syncObject.notify()
                }
            }
        })

        synchronized(syncObject) {
            try {
                syncObject.wait()
            } catch (_: InterruptedException) {
            }
        }
    }

    private fun handleChannelMessage(
        message: SlackApiWebSocketMessageMessage,
        handler: WebSocketHandler,
    ) {
        LOGGER.debug("message=$message")

        if (message.user == handler.tokenOwnerId) {
            LOGGER.debug("Own message, skipped")
            return
        }
        val shouldIgnore = handler.shouldIgnore(message.toChannelMessage(handler.membersById, handler.channelsById))
        LOGGER.debug("shouldIgnore=$shouldIgnore")
        if (!shouldIgnore) {
            LOGGER.info("Message $message should not be ignored: skipping")
            return
        } else {
            LOGGER.info("Message $message should be ignored: check if we're up to date on the channel")
        }

        val conversationHistory = runBlocking { conversationsHistory(message.channel).map { it.ts } }
        val lastReadMessageTs = runBlocking { lastReadMessageTs(message.channel) }

        LOGGER.debug("lastReadMessageTs=$lastReadMessageTs conversationHistory=$conversationHistory")

        if (lastReadMessageTs == conversationHistory[0] || lastReadMessageTs == conversationHistory[1]) {
            LOGGER.debug("Up to date")
            LOGGER.info("Message $message should be ignored: marking as read")
            runBlocking { conversationsMark(message.channel, message.ts) }
        } else {
            LOGGER.debug("Not up to date: don't mark as read")
        }
    }
}

private fun SlackApiWebSocketMessageMessage.toChannelMessage(
    membersById: Map<String, SlackApiMember>,
    channelsById: Map<String, SlackApiChannel>,
): WebSocketHandler.ChannelMessage {
    val member = membersById[user]
    val channel = channelsById[channel] // Can be null if this is a private message

    val memberNickName = member?.name ?: userName
    val memberRealName = member?.realName ?: userName ?: botProfile?.name
    val memberIsBot = member?.isBot ?: (botProfile != null)
    val messageText = text.ifBlank { null }
    val attachmentPretext = attachments?.firstOrNull()?.pretext
    val attachmentText = attachments?.firstOrNull()?.text
    val text = listOfNotNull(messageText, attachmentPretext, attachmentText).joinToString(" ")

    return WebSocketHandler.ChannelMessage(
        channelName = channel?.name,
        authorNickName = memberNickName,
        authorRealName = memberRealName!!,
        authorIsBot = memberIsBot,
        text = text,
    )
}

private fun trustAllCertsSslSocketFactory() = SSLContext.getInstance("SSL").apply {
    init(null, trustAllCerts(), SecureRandom())
}.socketFactory

private fun trustAllCerts() = arrayOf<X509TrustManager>(
    object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate?> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
        override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
    }
)
