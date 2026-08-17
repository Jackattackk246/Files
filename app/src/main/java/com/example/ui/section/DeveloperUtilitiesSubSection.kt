package com.example.ui.section

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.model.FileSortOrder
import com.example.security.DeveloperSecurityEngine
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeManager
import com.example.util.FestiveCalendarEngine
import com.example.util.HapticFeedbackHelper
import java.io.File
import java.io.FileOutputStream

@Composable
fun DeveloperUtilitiesSubSection(
  onBack: () -> Unit,
  currentThemeMode: AppThemeMode = AppThemeMode.MIDNIGHT_MATTE_BLACK
) {
  val context = LocalContext.current
  val activeThemeAccent = ThemeManager.getThemeAccentColor(currentThemeMode)
  val cardContainer = ThemeManager.getAdaptiveCardContainerColor(currentThemeMode)
  val cardBorder = ThemeManager.getAdaptiveCardBorderColor(currentThemeMode)
  val primaryTextColor = ThemeManager.getAdaptivePrimaryTextColor(currentThemeMode)
  val secondaryTextColor = ThemeManager.getAdaptiveSecondaryTextColor(currentThemeMode)

  var selectedSimulatedEvent by remember { mutableStateOf(FestiveCalendarEngine.FestiveEvent.NONE) }
  var isSimEventDropdownExpanded by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("developer_utilities_screen"),
    verticalArrangement = Arrangement.spacedBy(14.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("developer_menu_back_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = primaryTextColor
          )
        }
        Column {
          Text(
            text = "Developer Diagnostics",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            ),
            color = primaryTextColor
          )
          Text(
            text = "Advanced Testing & Diagnostics v2.4.6",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )
        }
      }
    }

    // 1. Time-Warp Simulation Dropdown
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(1.dp, cardBorder)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = activeThemeAccent)
            Text(
              text = "Time-Warp Simulation Engine",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = primaryTextColor
            )
          }

          Text(
            text = "Inject mock system calendar events completely offline to preview dynamic seasonal headers & overlays.",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )

          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
              onClick = { isSimEventDropdownExpanded = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("spinner_countdown_day_selector"),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, activeThemeAccent)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (selectedSimulatedEvent == FestiveCalendarEngine.FestiveEvent.NONE) "Active Device Time (Live)" else selectedSimulatedEvent.title,
                  color = primaryTextColor,
                  fontWeight = FontWeight.Medium
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = primaryTextColor)
              }
            }

            DropdownMenu(
              expanded = isSimEventDropdownExpanded,
              onDismissRequest = { isSimEventDropdownExpanded = false },
              modifier = Modifier
                .background(cardContainer)
                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
            ) {
              FestiveCalendarEngine.FestiveEvent.values().forEach { evt ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (evt == FestiveCalendarEngine.FestiveEvent.NONE) "Live Device Clock" else evt.title,
                      color = if (evt == selectedSimulatedEvent) activeThemeAccent else primaryTextColor
                    )
                  },
                  onClick = {
                    selectedSimulatedEvent = evt
                    isSimEventDropdownExpanded = false
                    Toast.makeText(context, "Simulating: ${evt.title}", Toast.LENGTH_SHORT).show()
                  }
                )
              }
            }
          }
        }
      }
    }

    // 2. Dump Offline Installation APK
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        border = BorderStroke(1.dp, cardBorder)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = activeThemeAccent)
            Text(
              text = "Dump Installation APK Binary",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = primaryTextColor
            )
          }

          Text(
            text = "Queries local source package sector and writes an offline compiled .apk binary package copy directly into device storage without root.",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )

          Button(
            onClick = {
              try {
                val sourceDir = context.applicationInfo.sourceDir
                val sourceFile = File(sourceDir)
                val targetDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetApk = File(targetDir, "Files_v2.4.6_Release_Dump.apk")

                sourceFile.inputStream().use { input ->
                  targetApk.outputStream().use { output ->
                    input.copyTo(output)
                  }
                }
                HapticFeedbackHelper.performTransferSuccessFeedback(context)
                Toast.makeText(context, "Dumped APK to: Downloads/Files_v2.4.6_Release_Dump.apk", Toast.LENGTH_LONG).show()
              } catch (e: Exception) {
                HapticFeedbackHelper.performErrorFeedback(context)
                Toast.makeText(context, "Failed to dump APK: ${e.message}", Toast.LENGTH_SHORT).show()
              }
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = activeThemeAccent),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dump_installation_apk_button")
          ) {
            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DUMP INSTALLATION APK", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
