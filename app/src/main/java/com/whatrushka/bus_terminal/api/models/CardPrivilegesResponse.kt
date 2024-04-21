package com.whatrushka.bus_terminal.api.models

import kotlinx.serialization.Serializable

@Serializable
data class CardPrivilegesResponse(
    val result: List<String>
)
