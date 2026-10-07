package com.mantz_it.rfanalyzer.ui.composable

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mantz_it.rfanalyzer.decoder.DecodedEvent
import kotlinx.coroutines.delay

/**
 * <h1>RF Analyzer - Decoder Tab</h1>
 *
 * Module:      DecoderTab.kt
 * Description: Tab to enable and control the rtl_433 based decoding of the
 * received IQ stream and to inspect/log the decoded events.
 *
 * Copyright (C) 2026 RF Analyzer contributors
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher
 */

/** Target decoder input rates offered in the UI (in Samples/s). */
val DECODER_TARGET_SAMPLE_RATES = listOf(250000, 500000, 1000000)

/** Unit conversion applied to decoded values (matches rtl_433's -C modes). */
enum class DecoderConversionMode(val displayName: String, val bridgeValue: Int) {
    NATIVE("Native (as decoded)", 0),
    SI("SI (metric)", 1),
    CUSTOMARY("US customary", 2),
}

data class DecoderTabActions(
    val onEnabledChanged: (Boolean) -> Unit,
    val onTargetSampleRateChanged: (Int) -> Unit,
    val onChannelizedChanged: (Boolean) -> Unit,
    val onConversionModeChanged: (DecoderConversionMode) -> Unit,
    val onAutoLevelChanged: (Boolean) -> Unit,
    val onMinSnrChanged: (Float) -> Unit,
    val onReportMetaChanged: (Boolean) -> Unit,
    val onLogToFileChanged: (Boolean) -> Unit,
    val onShowLogClicked: () -> Unit,
    val onSaveLogToFileClicked: (Uri) -> Unit,
    val onShareLogClicked: () -> Unit,
)

@Composable
fun DecoderTabComposable(
    decoderEnabled: Boolean,
    decoderRunning: Boolean,
    analyzerRunning: Boolean,
    targetSampleRate: Int,
    channelized: Boolean,
    conversionMode: DecoderConversionMode,
    autoLevel: Boolean,
    minSnr: Float,
    reportMeta: Boolean,
    logToFile: Boolean,
    logFilePath: String,
    eventCount: Int,
    events: List<DecodedEvent>,
    decoderTabActions: DecoderTabActions,
) {
    val destinationFileChooser = rememberCreateFilePicker(
        suggestedFileName = "decoded_events.json",
        mimeType = "application/json",
        onAbort = { },
        onFileCreated = { destUri -> decoderTabActions.onSaveLogToFileClicked(destUri) })

    ScrollableColumnWithFadingEdge {
        OutlinedSwitch(
            label = "Decode with rtl_433",
            helpText = "Decode OOK/FSK sensor packets in the current band using rtl_433. " +
                    "Decoding only covers the bandwidth around the center frequency.",
            isChecked = decoderEnabled,
            onCheckedChange = decoderTabActions.onEnabledChanged,
            helpSubPath = "decoding.html"
        )

        if (decoderEnabled) {
            OutlinedListDropDown(
                label = "Decoder Input Rate",
                items = DECODER_TARGET_SAMPLE_RATES,
                selectedItem = targetSampleRate,
                getDisplayName = { "${it / 1000} kSps" },
                onSelectionChanged = decoderTabActions.onTargetSampleRateChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedSwitch(
                label = "Channelized Decoding",
                helpText = "Filter to the demodulation channel before decoding so strong " +
                        "signals elsewhere in the band do not desensitise the decoder. Tune the " +
                        "demodulation channel to the signal and pick a demodulation mode whose " +
                        "channel width matches it (e.g. AM at 20 kHz).",
                isChecked = channelized,
                onCheckedChange = decoderTabActions.onChannelizedChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedEnumDropDown(
                label = "Units",
                selectedEnum = conversionMode,
                enumClass = DecoderConversionMode::class,
                getDisplayName = { it.displayName },
                onSelectionChanged = decoderTabActions.onConversionModeChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedSwitch(
                label = "Auto Level",
                helpText = "Automatically adapt the detection threshold to the noise floor. " +
                        "Recommended while roaming; disable for a fixed threshold.",
                isChecked = autoLevel,
                onCheckedChange = decoderTabActions.onAutoLevelChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedSlider(
                label = "Minimum SNR",
                unit = "dB",
                unitInLabel = true,
                minValue = 3f,
                maxValue = 24f,
                decimalPlaces = 0,
                value = minSnr,
                onValueChanged = decoderTabActions.onMinSnrChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedSwitch(
                label = "Report Signal Metadata",
                helpText = "Include modulation, frequency, RSSI, SNR and noise in each decoded event",
                isChecked = reportMeta,
                onCheckedChange = decoderTabActions.onReportMetaChanged,
                helpSubPath = "decoding.html"
            )

            OutlinedBox(label = "Status") {
                Column(modifier = Modifier.padding(8.dp)) {
                    val status = when {
                        decoderRunning -> "Running"
                        analyzerRunning -> "Starting..."
                        else -> "Waiting for analyzer"
                    }
                    Text(status, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Decoded events: $eventCount", fontSize = 12.sp)
                    if (events.isNotEmpty())
                        Text(
                            "Last: ${events.last().model}",
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                }
            }

            if (events.isNotEmpty()) {
                val latest = events.last()
                OutlinedBox(label = "Latest: ${latest.model}") {
                    Column(modifier = Modifier.padding(8.dp)) {
                        latest.fields.forEach { (key, value) ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                Text(
                                    key,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(96.dp)
                                )
                                Text(value, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            OutlinedSwitch(
                label = "Log Decoded Events",
                helpText = "Append every decoded event as a JSON line to a file in the app's storage",
                isChecked = logToFile,
                onCheckedChange = decoderTabActions.onLogToFileChanged,
                helpSubPath = "decoding.html"
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    val logAvailable = logFilePath.isNotEmpty()
                    Button(
                        onClick = decoderTabActions.onShowLogClicked,
                        enabled = logAvailable,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(end = 3.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = "View decoded log")
                    }
                    Button(
                        onClick = destinationFileChooser,
                        enabled = logAvailable,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.padding(horizontal = 3.dp).weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save decoded log")
                    }
                    Button(
                        onClick = decoderTabActions.onShareLogClicked,
                        enabled = logAvailable,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f).padding(start = 3.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share decoded log")
                    }
                }
            }
        }
    }
}

/**
 * Small, unobtrusive status badge drawn over the spectrum view while decoding
 * is enabled. It briefly highlights when a packet was successfully decoded.
 */
@Composable
fun DecoderStatusBadge(
    enabled: Boolean,
    running: Boolean,
    lastModel: String,
    lastEventTimestamp: Long,
    modifier: Modifier = Modifier,
) {
    if (!enabled) return

    var highlight by remember { mutableStateOf(false) }
    LaunchedEffect(lastEventTimestamp) {
        if (lastEventTimestamp > 0L) {
            highlight = true
            delay(2000)
            highlight = false
        }
    }

    val container = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
    val content = if (highlight) MaterialTheme.colorScheme.onPrimary
                  else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (running) Color(0xFF4CAF50) else Color(0xFFFFC107), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (highlight && lastModel.isNotEmpty()) lastModel else "rtl_433",
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
