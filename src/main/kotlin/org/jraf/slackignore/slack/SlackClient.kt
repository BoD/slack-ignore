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
import okio.ByteString
import org.jraf.slackignore.slack.apimodels.query.SlackApiChatPostMessageQuery
import org.jraf.slackignore.slack.apimodels.query.SlackApiConversationsMarkQuery
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

class SlackClient(
    private val authToken: String,
    private val cookie: String,
) {
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

    private fun createRetrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient.Builder()
                .sslSocketFactory(trustAllCertsSslSocketFactory(), trustAllCerts()[0])
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 8888)))
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
            response.channel.lastRead
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

    suspend fun startWebSocket(url: String) {
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
                val message = messageJsonAdapter.fromJson(text)!!
                if (message.type == "message") {
                    val messageMessage = messageMessageJsonAdapter.fromJson(text)!!
                    LOGGER.debug("messageMessage=$messageMessage")

                    val conversationHistory = runBlocking { conversationsHistory(messageMessage.channel).map { it.ts } }
                    val lastReadMessageTs = runBlocking { lastReadMessageTs(messageMessage.channel) }

                    LOGGER.debug("lastReadMessageTs=$lastReadMessageTs conversationHistory=$conversationHistory")

                    if (lastReadMessageTs == conversationHistory[0] || lastReadMessageTs == conversationHistory[1]) {
                        LOGGER.debug("Up to date!")
                        if (messageMessage.text.contains("sport", ignoreCase = true)) {
                            LOGGER.info("Ignoring message '${messageMessage.text}'")
                            runBlocking { conversationsMark(messageMessage.channel, messageMessage.ts) }
                        }
                    } else {
                        LOGGER.debug("NOT up to date!")
                    }
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
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                LOGGER.debug("WebSocket failure $t $response")
            }
        })
    }
}
