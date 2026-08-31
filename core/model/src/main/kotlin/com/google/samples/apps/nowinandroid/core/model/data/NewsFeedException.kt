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

package com.google.samples.apps.nowinandroid.core.model.data

sealed class NewsFeedException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class NetworkException(
        cause: Throwable? = null,
    ) : NewsFeedException("Network connection failed. Please check your internet.", cause)

    class RemoteServerException(
        val code: Int? = null,
        cause: Throwable? = null,
    ) : NewsFeedException("Remote server error occurred" + if (code != null) " (Code: $code)" else "", cause)

    class LocalStorageException(
        cause: Throwable? = null,
    ) : NewsFeedException("Local database error occurred while saving news articles.", cause)

    class EmptyResponseException(
        cause: Throwable? = null,
    ) : NewsFeedException("Server returned an empty news feed with no articles.", cause)
}
