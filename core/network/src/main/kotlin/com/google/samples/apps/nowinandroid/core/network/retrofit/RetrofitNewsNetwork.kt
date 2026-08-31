/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.core.network.retrofit

import androidx.tracing.trace
import com.google.samples.apps.nowinandroid.core.network.NewsNetworkDataSource
import com.google.samples.apps.nowinandroid.core.network.model.NetworkNewsArticle
import dagger.Lazy
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

private interface RetrofitNewsNetworkApi {
    @GET(value = "api/v1/cors/news-feed")
    suspend fun getNewsFeed(): Map<String, List<NetworkNewsArticle>>
}

private const val NEWS_FEED_BASE_URL = "https://ok.surf/"

@Singleton
internal class RetrofitNewsNetwork @Inject constructor(
    networkJson: Json,
    okhttpCallFactory: Lazy<Call.Factory>,
) : NewsNetworkDataSource {

    private val networkApi = trace("RetrofitNewsNetwork") {
        Retrofit.Builder()
            .baseUrl(NEWS_FEED_BASE_URL)
            .callFactory { okhttpCallFactory.get().newCall(it) }
            .addConverterFactory(
                networkJson.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(RetrofitNewsNetworkApi::class.java)
    }

    override suspend fun getNewsFeed(): Map<String, List<NetworkNewsArticle>> =
        networkApi.getNewsFeed()
}
