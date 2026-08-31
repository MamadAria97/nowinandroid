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

package com.google.samples.apps.nowinandroid.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.samples.apps.nowinandroid.core.model.data.NewsArticle
import kotlinx.datetime.Instant

@Entity(
    tableName = "news_articles",
)
data class NewsArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val link: String,
    @ColumnInfo(name = "og_image")
    val ogImage: String?,
    val source: String,
    @ColumnInfo(name = "source_icon")
    val sourceIcon: String?,
    val category: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Instant,
)

fun NewsArticleEntity.asExternalModel() = NewsArticle(
    id = id,
    title = title,
    link = link,
    ogImage = ogImage,
    source = source,
    sourceIcon = sourceIcon,
    category = category,
    fetchedAt = fetchedAt,
)
