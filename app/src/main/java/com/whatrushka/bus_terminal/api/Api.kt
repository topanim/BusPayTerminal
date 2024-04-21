package com.whatrushka.bus_terminal.api

import android.util.Log
import com.whatrushka.bus_terminal.api.models.CardPrivilegesResponse
import com.whatrushka.bus_terminal.api.models.PayRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class Api(
    private val client: HttpClient
) {
    companion object {
        const val DOMAIN = "http://45.155.207.232:1290"
        const val PAY = "$DOMAIN/api/user/cash/minus"
        const val PRIVILEGES = "$DOMAIN/api/user/privileges/card"
    }

    suspend fun pay(cardHash: String, reason: String, inn: Int, amount: Int, categoryId: Int) = client
        .post(PAY) {
            contentType(ContentType.Application.Json)
            setBody(
                PayRequest(
                    cardHash = cardHash,
                    reason = reason,
                    inn = inn,
                    price = amount,
                    categoryId = categoryId
                )
            )
        }

    suspend fun getPrivileges(cardHash: String) = client
        .get(PRIVILEGES) {
            contentType(ContentType.Application.Json)
            parameter("card_hash", cardHash)
        }.also {
            Log.d("m", it.bodyAsText())
        }.body<CardPrivilegesResponse>().result
}