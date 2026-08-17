package com.example.ui

import android.content.Intent
import android.content.res.Configuration
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DashboardWidgetConfig
import com.example.model.DashboardWidgetId
import com.example.model.EnvironmentalSeason
import com.example.model.WidgetSizeMode
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeManager
import com.example.util.*
import java.io.File

/**
 * Dynamic Modular Dashboard Screen v2.4.6 Production.
 *
 * Implements:
 * 1. Global "Device Storage" naming & accurate physical block capacity metrics via SystemStorageStatsEngine.
 * 2. Adaptive Three-Way Layout Routing (Phone, Tablet with max-width cap, External Samsung DeX).
 * 3. 5 Category Hubs (Images, Audio, Videos, APKs, Docs) in flat capsule archetypes.
 * 4. Permanent Dashboard Recycle Bin (.jack_recycle_bin) with 0-item click freeze & "Empty" state.
 * 5. Primary Storage Grid [Documents, Download, Main Storage, System].
 */
@Composable
fun DashboardScreen(
  storageMetrics: FileManager.StorageMetrics,
  currentDirectory: File,
  themeMode: AppThemeMode = AppThemeMode.MIDNIGHT_MATTE_BLACK,
  season: EnvironmentalSeason = EnvironmentalSeason.AUTO,
  onNavigateToExplorer: (File?, String?) -> Unit,
  onNavigateToSettings: () -> Unit
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val desktopPalette by com.aistudio.fileslauncher.ui.ThemeSynchronizationBridge.paletteState.collectAsState()

  val displayProfile = remember(configuration, desktopPalette.isForcedWindows11Desktop) {
    if (desktopPalette.isForcedWindows11Desktop) {
      DeviceDisplayProfile.EXTERNAL_DEX_DESKTOP
    } else {
      DeviceEnvironmentDetector.resolveDisplayProfile(context, configuration)
    }
  }

  val physicalMetrics = remember {
    SystemStorageStatsEngine.getPhysicalStorageMetrics(context)
  }

  var widgetConfigs by remember { mutableStateOf<List<DashboardWidgetConfig>>(DashboardPreferences.getWidgetLayoutOrder(context)) }
  var isEditModeUnlocked by remember { mutableStateOf(DashboardPreferences.isEditModeUnlocked(context)) }
  var showInfoDialog by remember { mutableStateOf(false) }
  var showRecycleBinDialog by remember { mutableStateOf(false) }
  var usbDriveDetails by remember { mutableStateOf<FileManager.UsbDriveDetails?>(null) }
  var trashedCount by remember { mutableStateOf(RecycleBinEngine.getItemCount()) }

  val isLight = ThemeManager.isLightBackgroundProfile(themeMode, season)
  val primaryTextColor = ThemeManager.getAdaptivePrimaryTextColor(themeMode, season)
  val secondaryTextColor = ThemeManager.getAdaptiveSecondaryTextColor(themeMode, season)
  val accentColor = desktopPalette.customAccentColor
  val cardContainer = if (desktopPalette.isDesktopCanvasActive) desktopPalette.widescreenContainerBg else ThemeManager.getAdaptiveCardContainerColor(themeMode, season)
  val cardBorder = ThemeManager.getAdaptiveCardBorderColor(themeMode, season)

  LaunchedEffect(Unit) {
    usbDriveDetails = FileManager.detectUsbDrive()
    trashedCount = RecycleBinEngine.getItemCount()
  }

  fun updateAndPersistWidgets(newConfigs: List<DashboardWidgetConfig>) {
    widgetConfigs = newConfigs
    DashboardPreferences.saveWidgetLayoutOrder(context, newConfigs)
  }

  // Tablet / Widescreen constraint box
  val contentModifier = if (displayProfile == DeviceDisplayProfile.TABLET) {
    Modifier
      .fillMaxSize()
      .widthIn(max = 500.dp)
      .padding(horizontal = 16.dp)
  } else {
    Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(if (desktopPalette.isDesktopCanvasActive) desktopPalette.widescreenContainerBg else Color.Transparent),
    contentAlignment = Alignment.TopCenter
  ) {
    LazyColumn(
      modifier = contentModifier.testTag("dashboard_scroll_view"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
      // 1. App Header & Hardware Context Icon Swapping
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Adaptive Device Vector Icon
            val hardwareIcon = when (displayProfile) {
              DeviceDisplayProfile.EXTERNAL_DEX_DESKTOP -> Icons.Default.DesktopWindows
              DeviceDisplayProfile.TABLET -> Icons.Default.TabletAndroid
              DeviceDisplayProfile.PHONE -> Icons.Default.Smartphone
            }

            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.20f))
                .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = hardwareIcon,
                contentDescription = "Hardware Profile",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
              )
            }

            val dynamicGreeting = remember {
              com.example.util.UserProfilePreferences.getDynamicTimeGreeting(context)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                text = dynamicGreeting,
                modifier = Modifier.testTag("greeting_text_view"),
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 20.sp,
                  color = primaryTextColor
                )
              )
              Text(
                text = when (displayProfile) {
                  DeviceDisplayProfile.EXTERNAL_DEX_DESKTOP -> "Windows 11 Desktop Workspace"
                  DeviceDisplayProfile.TABLET -> "Tablet Workspace Edition"
                  DeviceDisplayProfile.PHONE -> "Phone Standard Profile"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                  color = secondaryTextColor,
                  fontWeight = FontWeight.Medium
                )
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Toggle Windows 11 Desktop Canvas / Tablet Toggle Profile (Silent, persistent)
            IconButton(
              onClick = {
                val nextState = !desktopPalette.isForcedWindows11Desktop
                com.aistudio.fileslauncher.ui.ThemeSynchronizationBridge.setForcedWindows11DesktopEnabled(context, nextState)
              },
              modifier = Modifier.testTag("dashboard_desktop_canvas_toggle_button")
            ) {
              Icon(
                imageVector = if (desktopPalette.isForcedWindows11Desktop) Icons.Default.DesktopWindows else Icons.Default.LaptopMac,
                contentDescription = "Toggle Windows 11 Desktop Workspace",
                tint = if (desktopPalette.isForcedWindows11Desktop) accentColor else primaryTextColor
              )
            }

            IconButton(
              onClick = {
                isEditModeUnlocked = !isEditModeUnlocked
                DashboardPreferences.setEditModeUnlocked(context, isEditModeUnlocked)
              },
              modifier = Modifier.testTag("dashboard_customize_button")
            ) {
              Icon(
                imageVector = if (isEditModeUnlocked) Icons.Default.Check else Icons.Default.DashboardCustomize,
                contentDescription = "Customize Dashboard Layout",
                tint = primaryTextColor
              )
            }

            IconButton(
              onClick = { showInfoDialog = true },
              modifier = Modifier.testTag("header_info_button")
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = primaryTextColor
              )
            }
          }
        }
      }

      // 2. Edit Mode Banner
      if (isEditModeUnlocked) {
        item {
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("dashboard_edit_mode_banner"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardContainer),
            border = BorderStroke(1.dp, accentColor)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  Icons.Default.EditNote,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(20.dp)
                )
                Text(
                  text = "Edit Mode: Reorder & resize widgets freely.",
                  style = MaterialTheme.typography.labelSmall.copy(
                    color = primaryTextColor,
                    fontWeight = FontWeight.SemiBold
                  )
                )
              }

              Button(
                onClick = {
                  isEditModeUnlocked = false
                  DashboardPreferences.setEditModeUnlocked(context, false)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.testTag("done_edit_mode_button")
              ) {
                Text("Done", fontSize = 11.sp, color = if (isLight) Color.White else Color.Black)
              }
            }
          }
        }
      }

      // 3. Dynamic Reorderable Dashboard Widgets
      itemsIndexed(
        items = widgetConfigs,
        key = { _, config -> config.widgetId.id }
      ) { index, widgetConfig ->
        WidgetCardContainer(
          index = index,
          config = widgetConfig,
          isEditMode = isEditModeUnlocked,
          isFirst = index == 0,
          isLast = index == widgetConfigs.lastIndex,
          isLight = isLight,
          primaryTextColor = primaryTextColor,
          accentColor = accentColor,
          cardContainer = cardContainer,
          cardBorder = cardBorder,
          onMoveUp = {
            if (index > 0) {
              val mutable = widgetConfigs.toMutableList()
              val moved = mutable.removeAt(index)
              mutable.add(index - 1, moved)
              updateAndPersistWidgets(mutable)
            }
          },
          onMoveDown = {
            if (index < widgetConfigs.lastIndex) {
              val mutable = widgetConfigs.toMutableList()
              val moved = mutable.removeAt(index)
              mutable.add(index + 1, moved)
              updateAndPersistWidgets(mutable)
            }
          },
          onToggleSizeMode = { newMode ->
            val mutable = widgetConfigs.toMutableList()
            mutable[index] = widgetConfig.copy(sizeMode = newMode)
            updateAndPersistWidgets(mutable)
          }
        ) {
          when (widgetConfig.widgetId) {
            DashboardWidgetId.DEVICE_STORAGE_METER -> {
              DeviceStorageMeterWidget(
                physicalMetrics = physicalMetrics,
                sizeMode = widgetConfig.sizeMode,
                isLight = isLight,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                cardContainer = cardContainer,
                cardBorder = cardBorder,
                onNavigateToSettings = onNavigateToSettings
              )
            }

            DashboardWidgetId.LOCAL_STORAGE_HUBS -> {
              LocalStorageHubsWidget(
                physicalMetrics = physicalMetrics,
                sizeMode = widgetConfig.sizeMode,
                usbDriveDetails = usbDriveDetails,
                trashedCount = trashedCount,
                isLight = isLight,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                cardContainer = cardContainer,
                cardBorder = cardBorder,
                onNavigateToExplorer = onNavigateToExplorer,
                onOpenRecycleBin = { showRecycleBinDialog = true }
              )
            }

            DashboardWidgetId.APKS_INSTALLER_CENTER -> {
              FiveCategoryHubsWidget(
                sizeMode = widgetConfig.sizeMode,
                themeMode = themeMode,
                isLight = isLight,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                cardContainer = cardContainer,
                cardBorder = cardBorder,
                onNavigateToExplorer = onNavigateToExplorer
              )
            }

            DashboardWidgetId.QUICK_FILE_ACTIONS -> {
              QuickFileActionsWidget(
                sizeMode = widgetConfig.sizeMode,
                currentDirectory = currentDirectory,
                isLight = isLight,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                accentColor = accentColor,
                cardContainer = cardContainer,
                cardBorder = cardBorder,
                onNavigateToExplorer = onNavigateToExplorer
              )
            }
          }
        }
      }
    }
  }

  // App Info Dialog
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      shape = RoundedCornerShape(20.dp),
      containerColor = cardContainer,
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Icon(Icons.Default.Info, contentDescription = null, tint = accentColor)
          Text("Files v2.4.6 Production", fontWeight = FontWeight.Bold, color = primaryTextColor)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Files v2.4.6 is a high-speed local Android Files & Storage Manager.", style = MaterialTheme.typography.bodyMedium.copy(color = primaryTextColor))
          Text("• Total Capacity: ${physicalMetrics.formattedTotal}", style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor))
          Text("• Used Blocks: ${physicalMetrics.formattedUsed} (${(physicalMetrics.usedRatio * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor))
          Text("• Free Blocks: ${physicalMetrics.formattedFree}", style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor))
          Text("• Display Profile: ${displayProfile.name}", style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor))
          Text("• Isolated Trash (.jack_recycle_bin): $trashedCount files", style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor))
          Text("• Made by Jack Lawton aka Jackattackk2.4.6", style = MaterialTheme.typography.bodySmall.copy(color = accentColor, fontWeight = FontWeight.Bold))
        }
      },
      confirmButton = {
        Button(
          onClick = { showInfoDialog = false },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
          Text("Close", color = if (isLight) Color.White else Color.Black)
        }
      }
    )
  }

  // Recycle Bin Dialog / Explorer View
  if (showRecycleBinDialog) {
    RecycleBinDialog(
      themeMode = themeMode,
      customAccentColor = accentColor,
      onDismiss = {
        showRecycleBinDialog = false
        trashedCount = RecycleBinEngine.getItemCount()
      }
    )
  }
}

// =========================================================================
// WIDGET CARD WRAPPER
// =========================================================================
@Composable
private fun WidgetCardContainer(
  index: Int,
  config: DashboardWidgetConfig,
  isEditMode: Boolean,
  isFirst: Boolean,
  isLast: Boolean,
  isLight: Boolean,
  primaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onToggleSizeMode: (WidgetSizeMode) -> Unit,
  content: @Composable () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dashboard_widget_container_${config.widgetId.id}"),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    if (isEditMode) {
      Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardContainer,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(color = accentColor, shape = CircleShape) {
              Text(
                text = "#${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = if (isLight) Color.White else Color.Black
                ),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
              )
            }
            Text(
              text = config.widgetId.title,
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor)
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = config.sizeMode == WidgetSizeMode.FULL,
              onClick = { onToggleSizeMode(WidgetSizeMode.FULL) },
              label = { Text("Full", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp)
            )
            FilterChip(
              selected = config.sizeMode == WidgetSizeMode.COMPACT,
              onClick = { onToggleSizeMode(WidgetSizeMode.COMPACT) },
              label = { Text("Compact", fontSize = 10.sp) },
              modifier = Modifier.height(28.dp)
            )

            IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp), tint = if (!isFirst) primaryTextColor else Color.Gray)
            }

            IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
              Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp), tint = if (!isLast) primaryTextColor else Color.Gray)
            }
          }
        }
      }
    }

    content()
  }
}

// =========================================================================
// 1. DEVICE STORAGE METERS WIDGET (Total Hardware Allocation)
// =========================================================================
@Composable
private fun DeviceStorageMeterWidget(
  physicalMetrics: PhysicalStorageMetrics,
  sizeMode: WidgetSizeMode,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onNavigateToSettings: () -> Unit
) {
  val usedPercent = (physicalMetrics.usedRatio * 100).toInt()

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("device_storage_meter_card"),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, cardBorder),
    colors = CardDefaults.cardColors(containerColor = cardContainer)
  ) {
    if (sizeMode == WidgetSizeMode.FULL) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Storage,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Device Storage",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = primaryTextColor
              )
            )
          }

          Text(
            text = "Configurations",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              color = accentColor
            ),
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .clickable { onNavigateToSettings() }
              .padding(horizontal = 6.dp, vertical = 2.dp)
              .testTag("manage_settings_link")
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Used: ${physicalMetrics.formattedUsed} of ${physicalMetrics.formattedTotal}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor)
          )
          Text(
            text = "$usedPercent%",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor)
          )
        }

        LinearProgressIndicator(
          progress = { physicalMetrics.usedRatio },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape),
          color = accentColor,
          trackColor = if (isLight) Color(0x33000000) else Color(0x44FFFFFF)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Free: ${physicalMetrics.formattedFree}",
            style = MaterialTheme.typography.labelMedium.copy(
              color = secondaryTextColor,
              fontWeight = FontWeight.Medium
            )
          )
          Text(
            text = "Hardware Block Allocation",
            style = MaterialTheme.typography.labelMedium.copy(
              color = secondaryTextColor,
              fontWeight = FontWeight.Medium
            )
          )
        }
      }
    } else {
      // Compact View
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.20f))
            .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Storage, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Device Storage", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = primaryTextColor)
            Text("$usedPercent% Used", fontWeight = FontWeight.Bold, color = accentColor, style = MaterialTheme.typography.labelMedium)
          }

          LinearProgressIndicator(
            progress = { physicalMetrics.usedRatio },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(CircleShape),
            color = accentColor,
            trackColor = if (isLight) Color(0x33000000) else Color(0x44FFFFFF)
          )

          Text(
            text = "${physicalMetrics.formattedUsed} / ${physicalMetrics.formattedTotal} (Free: ${physicalMetrics.formattedFree})",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor, fontWeight = FontWeight.Medium)
          )
        }
      }
    }
  }
}

// =========================================================================
// 2. LOCAL STORAGE HUBS WIDGET (Windows 11 Desktop Drives + Primary Grid + Recycle Bin)
// =========================================================================
@Composable
private fun LocalStorageHubsWidget(
  physicalMetrics: PhysicalStorageMetrics,
  sizeMode: WidgetSizeMode,
  usbDriveDetails: FileManager.UsbDriveDetails?,
  trashedCount: Int,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onNavigateToExplorer: (File?, String?) -> Unit,
  onOpenRecycleBin: () -> Unit
) {
  val context = LocalContext.current
  val desktopPalette by com.aistudio.fileslauncher.ui.ThemeSynchronizationBridge.paletteState.collectAsState()

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("local_storage_hubs_container"),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Folder,
        contentDescription = null,
        tint = desktopPalette.folderIconTint,
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = if (desktopPalette.isDesktopCanvasActive) "Windows 11 Devices & Drives" else "Local Storage Hubs",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = primaryTextColor
        )
      )
    }

    // Authentic Hardware Drive Capacity Cell: [System (C:)] + Connected USB OTG (if attached)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      DesktopDriveCapacityCard(
        driveLabel = "System (C:)",
        volumeName = "Internal Storage Volume",
        path = "/storage/emulated/0",
        usedRatio = physicalMetrics.usedRatio,
        capacityText = "${physicalMetrics.formattedFree} free of ${physicalMetrics.formattedTotal}",
        accentSwatch = desktopPalette.systemDriveCColor,
        folderIconTint = desktopPalette.folderIconTint,
        cardContainer = cardContainer,
        cardBorder = cardBorder,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        testTag = "drive_system_c"
      ) {
        onNavigateToExplorer(File("/storage/emulated/0"), null)
      }

      if (usbDriveDetails != null && usbDriveDetails.isConnected) {
        DesktopDriveCapacityCard(
          driveLabel = usbDriveDetails.name,
          volumeName = "External OTG Drive",
          path = usbDriveDetails.path.absolutePath,
          usedRatio = 0.20f,
          capacityText = "${usbDriveDetails.freeGbFormatted} free of ${usbDriveDetails.totalGbFormatted}",
          accentSwatch = desktopPalette.backupDriveDColor,
          folderIconTint = desktopPalette.folderIconTint,
          cardContainer = cardContainer,
          cardBorder = cardBorder,
          primaryTextColor = primaryTextColor,
          secondaryTextColor = secondaryTextColor,
          testTag = "drive_usb_otg"
        ) {
          onNavigateToExplorer(usbDriveDetails.path, null)
        }
      }
    }

    // Adaptive Authentic Storage Grid: Auto-resized and realigned side-by-side [Download & Main Storage]
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      PrimaryGridHubCard(
        title = "Download",
        path = "/storage/emulated/0/Download",
        icon = Icons.Default.Download,
        modifier = Modifier.weight(1f),
        accentColor = desktopPalette.folderIconTint,
        cardContainer = cardContainer,
        cardBorder = cardBorder,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor
      ) {
        onNavigateToExplorer(File("/storage/emulated/0/Download"), null)
      }

      PrimaryGridHubCard(
        title = "Main Storage",
        path = "/storage/emulated/0",
        icon = Icons.Default.Smartphone,
        modifier = Modifier.weight(1f),
        accentColor = desktopPalette.folderIconTint,
        cardContainer = cardContainer,
        cardBorder = cardBorder,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor
      ) {
        onNavigateToExplorer(File("/storage/emulated/0"), null)
      }
    }

    // Permanent Standalone Recycle Bin Row (.jack_recycle_bin)
    val isRecycleEmpty = trashedCount == 0
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .then(
          if (!isRecycleEmpty) {
            Modifier.clickable { onOpenRecycleBin() }
          } else {
            Modifier
          }
        )
        .testTag("dashboard_recycle_bin_row"),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = cardContainer),
      border = BorderStroke(1.dp, if (isRecycleEmpty) cardBorder.copy(alpha = 0.5f) else cardBorder)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isRecycleEmpty) Color(0x1A888888) else accentColor.copy(alpha = 0.20f))
              .border(1.dp, if (isRecycleEmpty) Color(0x33888888) else accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isRecycleEmpty) Icons.Default.DeleteOutline else Icons.Default.Delete,
              contentDescription = "Recycle Bin",
              tint = if (isRecycleEmpty) secondaryTextColor else accentColor,
              modifier = Modifier.size(22.dp)
            )
          }

          Column {
            Text(
              text = "Recycle Bin",
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
              )
            )
            Text(
              text = if (isRecycleEmpty) "Empty • Auto-purge after 30 days" else "$trashedCount items in trash",
              style = MaterialTheme.typography.labelSmall.copy(
                color = if (isRecycleEmpty) secondaryTextColor else accentColor
              )
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isRecycleEmpty) Color(0x1A888888) else accentColor.copy(alpha = 0.25f)
        ) {
          Text(
            text = if (isRecycleEmpty) "Empty" else "$trashedCount Items",
            style = MaterialTheme.typography.labelSmall.copy(
              color = if (isRecycleEmpty) secondaryTextColor else accentColor,
              fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }
    }

    // USB / OTG Drive Row if available
    val usb = usbDriveDetails
    if (usb != null && usb.isConnected) {
      LocalStorageHubCard(
        title = usb.name,
        subtitle = "Capacity: ${usb.freeGbFormatted} Free of ${usb.totalGbFormatted}",
        icon = Icons.Default.Usb,
        pillText = "USB Mounted",
        tag = "hub_external_usb_card",
        isLight = isLight,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        accentColor = accentColor,
        cardContainer = cardContainer,
        cardBorder = cardBorder,
        onClick = { onNavigateToExplorer(usb.path, null) }
      )
    }
  }
}

@Composable
private fun DesktopDriveCapacityCard(
  driveLabel: String,
  volumeName: String,
  path: String,
  usedRatio: Float,
  capacityText: String,
  accentSwatch: Color,
  folderIconTint: Color,
  cardContainer: Color,
  cardBorder: Color,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  modifier: Modifier = Modifier,
  testTag: String = "desktop_drive_card",
  onClick: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = {
        HapticManager.navigationClick(context)
        onClick()
      })
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(accentSwatch.copy(alpha = 0.20f))
              .border(1.dp, accentSwatch.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (driveLabel.startsWith("System")) Icons.Default.Computer else if (driveLabel.startsWith("Backup")) Icons.Default.FolderSpecial else Icons.Default.CloudQueue,
              contentDescription = null,
              tint = folderIconTint,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Text(
              text = driveLabel,
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor)
            )
            Text(
              text = volumeName,
              style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor, fontSize = 11.sp)
            )
          }
        }

        // Color accent swatch badge
        Box(
          modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(accentSwatch)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
        )
      }

      // Horizontal Capacity Tracking Bar Graphic
      LinearProgressIndicator(
        progress = { usedRatio.coerceIn(0f, 1f) },
        modifier = Modifier
          .fillMaxWidth()
          .height(5.dp)
          .clip(CircleShape),
        color = accentSwatch,
        trackColor = accentSwatch.copy(alpha = 0.20f)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = capacityText,
          style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        )
        Text(
          text = "${(usedRatio * 100).toInt()}% Used",
          style = MaterialTheme.typography.labelSmall.copy(color = accentSwatch, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
private fun PrimaryGridHubCard(
  title: String,
  path: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = {
        HapticManager.navigationClick(context)
        onClick()
      })
      .testTag("primary_hub_${title.lowercase()}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(accentColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
      }

      Column {
        Text(
          title,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          path,
          style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor, fontSize = 10.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

// =========================================================================
// 3. 5-HUB CATEGORY GRID (Images, Audio, Videos, APKs, Docs)
// =========================================================================
@Composable
private fun FiveCategoryHubsWidget(
  sizeMode: WidgetSizeMode,
  themeMode: AppThemeMode,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onNavigateToExplorer: (File?, String?) -> Unit
) {
  val categories = listOf(
    CategoryHubItem("Images", "Images", Icons.Default.Image, "images"),
    CategoryHubItem("Audio", "Audio", Icons.Default.MusicNote, "audio"),
    CategoryHubItem("Videos", "Videos", Icons.Default.Movie, "videos"),
    CategoryHubItem("APKs", "APK", Icons.Default.Android, "apk", noSubtitle = true),
    CategoryHubItem("Docs", "Docs", Icons.Default.Article, "docs")
  )

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Category,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = "Media & Package Hubs",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          color = primaryTextColor
        )
      )
    }

    // 5 Flat Capsule Archetype Hubs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      categories.forEach { hub ->
        FlatCapsuleCategoryCard(
          item = hub,
          themeMode = themeMode,
          modifier = Modifier.weight(1f),
          accentColor = accentColor,
          cardContainer = cardContainer,
          cardBorder = cardBorder,
          primaryTextColor = primaryTextColor,
          secondaryTextColor = secondaryTextColor
        ) {
          onNavigateToExplorer(File("/storage/emulated/0"), hub.filterTag)
        }
      }
    }
  }
}

private data class CategoryHubItem(
  val title: String,
  val subtext: String,
  val icon: ImageVector,
  val filterTag: String,
  val noSubtitle: Boolean = false
)

@Composable
private fun FlatCapsuleCategoryCard(
  item: CategoryHubItem,
  themeMode: AppThemeMode,
  modifier: Modifier = Modifier,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  onClick: () -> Unit
) {
  val context = LocalContext.current
  Card(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = {
        HapticManager.navigationClick(context)
        onClick()
      })
      .testTag("category_hub_${item.title.lowercase()}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.dp, cardBorder)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      val isSamsungExperience = themeMode == AppThemeMode.SAMSUNG_EXPERIENCE
      val iconBgColor = when {
        isSamsungExperience && item.filterTag == "images" -> Color(0xFFFF5B72)
        isSamsungExperience && item.filterTag == "audio" -> Color(0xFF29B6F6)
        isSamsungExperience && item.filterTag == "docs" -> Color(0xFF3B66F5)
        isSamsungExperience -> accentColor
        else -> accentColor.copy(alpha = 0.18f)
      }
      val iconTint = if (isSamsungExperience) Color.White else accentColor
      val iconShape = if (isSamsungExperience) RoundedCornerShape(18.dp) else RoundedCornerShape(8.dp)

      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(iconShape)
          .background(iconBgColor)
          .testTag("media_icon_${item.filterTag}"),
        contentAlignment = Alignment.Center
      ) {
        Icon(item.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
      }

      Text(
        item.subtext,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = primaryTextColor,
          fontSize = 11.sp
        ),
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
  }
}

// =========================================================================
// 4. QUICK FILE ACTIONS WIDGET
// =========================================================================
@Composable
private fun QuickFileActionsWidget(
  sizeMode: WidgetSizeMode,
  currentDirectory: File,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onNavigateToExplorer: (File?, String?) -> Unit
) {
  val context = LocalContext.current

  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "Quick File Actions",
      style = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = primaryTextColor
      ),
      modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )

    QuickActionRowCard(
      title = "Open Document Picker",
      pillTag = "Picker",
      subtitle = "Use Android SAF document picker",
      icon = Icons.Default.Description,
      testTag = "action_card_pick_document",
      isLight = isLight,
      primaryTextColor = primaryTextColor,
      secondaryTextColor = secondaryTextColor,
      accentColor = accentColor,
      cardContainer = cardContainer,
      cardBorder = cardBorder,
      onClick = { FileManager.openPathSAFBackdoor(context, currentDirectory.absolutePath) }
    )

    QuickActionRowCard(
      title = "Open Root Directory",
      pillTag = "Storage",
      subtitle = "Browse /storage/emulated/0 filesystem",
      icon = Icons.Default.FolderSpecial,
      testTag = "action_card_open_root",
      isLight = isLight,
      primaryTextColor = primaryTextColor,
      secondaryTextColor = secondaryTextColor,
      accentColor = accentColor,
      cardContainer = cardContainer,
      cardBorder = cardBorder,
      onClick = { onNavigateToExplorer(File("/storage/emulated/0"), null) }
    )
  }
}

@Composable
private fun LocalStorageHubCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  pillText: String,
  tag: String,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(tag),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.dp, cardBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(accentColor.copy(alpha = 0.20f))
            .border(1.dp, accentColor.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        }

        Column {
          Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor))
          Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor))
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.20f)
      ) {
        Text(
          text = pillText,
          style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun QuickActionRowCard(
  title: String,
  pillTag: String,
  subtitle: String,
  icon: ImageVector,
  testTag: String,
  isLight: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  accentColor: Color,
  cardContainer: Color,
  cardBorder: Color,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = cardContainer),
    border = BorderStroke(1.dp, cardBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
        Column {
          Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = primaryTextColor))
          Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor))
        }
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = accentColor.copy(alpha = 0.15f)
      ) {
        Text(
          text = pillTag,
          style = MaterialTheme.typography.labelSmall.copy(color = accentColor, fontWeight = FontWeight.Bold, fontSize = 10.sp),
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
      }
    }
  }
}

// =========================================================================
// RECYCLE BIN MODAL DIALOG
// =========================================================================
@Composable
private fun RecycleBinDialog(
  themeMode: AppThemeMode,
  customAccentColor: Color,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val isLight = ThemeManager.isLightBackgroundProfile(themeMode)
  val primaryTextColor = ThemeManager.getAdaptivePrimaryTextColor(themeMode)
  val secondaryTextColor = ThemeManager.getAdaptiveSecondaryTextColor(themeMode)
  val cardContainer = if (isLight) Color(0xFFFFFFFF) else Color(0xFF14171F)
  val cardBorder = if (isLight) Color(0x33000000) else Color(0x3338BDF8)

  var trashedList by remember { mutableStateOf(RecycleBinEngine.getRecycledItems()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = cardContainer,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.Delete, contentDescription = null, tint = customAccentColor)
          Text("Recycle Bin", fontWeight = FontWeight.Bold, color = primaryTextColor)
        }
        if (trashedList.isNotEmpty()) {
          TextButton(
            onClick = {
              RecycleBinEngine.emptyRecycleBin()
              trashedList = emptyList()
              Toast.makeText(context, "Emptied Recycle Bin", Toast.LENGTH_SHORT).show()
            }
          ) {
            Text("Empty All", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    text = {
      if (trashedList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(48.dp))
            Text("Recycle Bin is empty", style = MaterialTheme.typography.bodyMedium.copy(color = primaryTextColor, fontWeight = FontWeight.Bold))
            Text("Files removed will be kept here for 30 days.", style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor), textAlign = TextAlign.Center)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 340.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(trashedList.size) { idx ->
            val item = trashedList[idx]
            Card(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B)),
              border = BorderStroke(1.dp, cardBorder)
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(item.fileName, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  Text("Original: ${item.originalPath}", style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                  IconButton(
                    onClick = {
                      val ok = RecycleBinEngine.restoreItem(item.id)
                      if (ok) {
                        trashedList = RecycleBinEngine.getRecycledItems()
                        Toast.makeText(context, "Restored ${item.fileName}", Toast.LENGTH_SHORT).show()
                      }
                    }
                  ) {
                    Icon(Icons.Default.Restore, contentDescription = "Restore", tint = customAccentColor)
                  }

                  IconButton(
                    onClick = {
                      val ok = RecycleBinEngine.deletePermanently(item.id)
                      if (ok) {
                        trashedList = RecycleBinEngine.getRecycledItems()
                      }
                    }
                  ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Delete", tint = Color(0xFFEF4444))
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = customAccentColor)
      ) {
        Text("Done", color = if (isLight) Color.White else Color.Black)
      }
    }
  )
}
