package me.matsumo.grabee.core.common

import kotlinx.serialization.json.Json

val formatter = Json {
    isLenient = true
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}
