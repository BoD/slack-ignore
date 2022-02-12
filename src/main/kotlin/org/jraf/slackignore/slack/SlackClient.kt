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

import okhttp3.OkHttpClient
import org.jraf.slackignore.slack.retrofit.SlackRetrofitService
import org.jraf.slackignore.slack.retrofit.apimodels.query.SlackApiPostMessageQuery
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
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
                //.sslSocketFactory(trustAllCertsSslSocketFactory(), trustAllCerts()[0])
                //.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress("localhost", 8888)))
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

    suspend fun postMessage(channel: String, text: String): Boolean {
        return try {
            val response = service.postMessage(
                query = SlackApiPostMessageQuery(channel = channel, text = text)
            )
            response.ok
        } catch (e: Exception) {
            LOGGER.warn("Could not make network call", e)
            false
        }
    }
}
