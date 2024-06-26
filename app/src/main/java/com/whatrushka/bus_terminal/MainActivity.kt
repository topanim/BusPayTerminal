package com.whatrushka.bus_terminal

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatrushka.bus_terminal.api.Api
import com.whatrushka.bus_terminal.api.Client
import com.whatrushka.bus_terminal.ui.theme.NfcScannerTheme
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.time.delay
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.time.Duration
import kotlin.experimental.and

sealed class TerminalType {
    data object EventTerminal : TerminalType()
    data object PaymentTerminal : TerminalType()
}

data class TerminalState(
    val sum: Int = 39,
    val pensSum: Int = 14,
    val serviceName: String = "Проезд",
    val isWait: Boolean = true,
    val isFinished: Boolean = false,
    val isSuccessfully: Boolean = false,
    val cardHash: String? = null,
    val type: TerminalType = TerminalType.PaymentTerminal,
    val terminalCode: Int = 43512323,
    val categoryId: Int = 1
)

object Strings {
    const val tapToPay = "Приложите карту к считывателю"
    const val wait = "Подождите"
    const val successfully = "Проезд успешно оплачен"
    const val failed = "Неизвестная ошибка"
}

class MainActivity : ComponentActivity() {

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private val state = mutableStateOf(TerminalState())


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = Api(Client)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            NfcScannerTheme {

                val icon = remember { mutableStateOf<Int?>(R.drawable.tap_to_pay) }
                val text = remember { mutableStateOf(Strings.tapToPay) }
                val operationDescription = remember { mutableStateOf("") }

                when (state.value.type) {
                    TerminalType.PaymentTerminal -> operationDescription.value =
                        "Оплата услуги ${state.value.serviceName.lowercase()}:\n" +
                                "Стандарт - ${state.value.sum}₽\n" +
                                "Пенсионерам - ${state.value.pensSum}₽"
                    TerminalType.EventTerminal -> operationDescription.value =
                        "Услуга: \n${state.value.serviceName}"
                }

                LaunchedEffect(state.value) {
                    Log.d("m", state.value.toString())
                    if (!state.value.isWait && !state.value.isFinished && !state.value.cardHash.isNullOrEmpty()) {
                        icon.value = null
                        text.value = Strings.wait

                        val privileges = api.getPrivileges(state.value.cardHash!!)
                        val payAmount =
                            if ("pens" in privileges) state.value.pensSum else state.value.sum
                        val response = api.pay(
                            state.value.cardHash!!,
                            "${state.value.serviceName} - ${payAmount}₽",
                            state.value.terminalCode,
                            payAmount,
                            state.value.categoryId
                        )

                        Log.d("m", response.status.value.toString())
                        Log.d("m", response.bodyAsText())

                        delay(Duration.ofSeconds(1))

                        (response.status.value in 200..299).let {
                            state.value = state.value.copy(
                                isFinished = true,
                                isSuccessfully = it
                            )

                            if (it) {
                                text.value = Strings.successfully
                                icon.value = R.drawable.success
                            } else {
                                text.value = Strings.failed
                                icon.value = R.drawable.failed
                            }
                        }
                    } else {
                        delay(Duration.ofSeconds(2))

                        state.value = TerminalState()

                        Log.d("m", state.value.toString())

                        text.value = Strings.tapToPay
                        icon.value = R.drawable.tap_to_pay
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                        ) {
                            Text(
                                text = "Терминал автобуса",
                                style = TextStyle(
                                    fontWeight = FontWeight.W700,
                                    fontSize = 26.sp,
                                    color = Color.Black
                                )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = operationDescription.value,
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = Color.DarkGray
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        ) {
                            icon.value?.let {
                                Image(
                                    painter = painterResource(it),
                                    contentDescription = null
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = text.value,
                                style = TextStyle(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            )
                        }

                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val strings = readFromIntent(intent)

        state.value = state.value.copy(
            cardHash = strings[0],
            isWait = false
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }

    fun readFromIntent(intent: Intent): List<String> {
        val action = intent.action
        val data = mutableListOf<String>()
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action || NfcAdapter.ACTION_TECH_DISCOVERED == action || NfcAdapter.ACTION_NDEF_DISCOVERED == action) {
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            val messages = mutableListOf<NdefMessage>()
            if (rawMessages != null) {
                for (i in rawMessages.indices) {
                    messages.add(i, rawMessages[i] as NdefMessage)
                }
                val res = buildTagViews(messages.toTypedArray())
                data.addAll(res)
            }
        }
        return data
    }

    private fun buildTagViews(msg: Array<NdefMessage>?): List<String> {
        if (msg.isNullOrEmpty()) return emptyList()
        val data = mutableListOf<String>()

        msg.forEach { message ->
            message.records.forEach { record ->
                val textEncoding: Charset =
                    if ((record.payload[0] and 128.toByte()).toInt() == 0) Charsets.UTF_8
                    else Charsets.UTF_16
                val languageCodeLength: Int =
                    (record.payload[0] and 51).toInt() // Get the Language Code, e.g. "en"
                try {
                    data.add(
                        String(
                            record.payload,
                            languageCodeLength + 1,
                            record.payload.size - languageCodeLength - 1,
                            textEncoding
                        )
                    )
                } catch (e: UnsupportedEncodingException) {
                    Log.e("UnsupportedEncoding", e.toString())
                }
            }
        }
        return data
    }
}