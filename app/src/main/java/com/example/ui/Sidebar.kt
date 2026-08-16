package com.example.ui

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EnvironmentalSeason
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeManager
import com.example.util.FileManager
import com.example.util.HapticFeedbackHelper

enum class NavigationNode {
  DASHBOARD,
  EXPLORER,
  RECENTS,
  SEARCH,
  SETTINGS
}

@Composable
fun SidebarPanel(
  selectedNode: NavigationNode,
  onNodeSelected: (NavigationNode) -> Unit,
  storageMetrics: FileManager.StorageMetrics,
  themeMode: AppThemeMode = AppThemeMode.DYNAMIC_WEATHER_CANVAS,
  season: EnvironmentalSeason = EnvironmentalSeason.AUTO,
  customAccentColor: Color? = null,
  onOpenConfigurationsDialog: (() -> Unit)? = null,
  onOpenSearchConfigDialog: (() -> Unit)? = null,
  onOpenWallpaperEngineDialog: (() -> Unit)? = null,
  onOpenEnvironmentalEngineDialog: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isLight = ThemeManager.isLightBackgroundProfile(themeMode, season)
  val primaryTextColor = ThemeManager.getAdaptivePrimaryTextColor(themeMode, season)
  val secondaryTextColor = ThemeManager.getAdaptiveSecondaryTextColor(themeMode, season)
  val accentColor = ThemeManager.getThemeAccentColor(themeMode, customAccentColor)
  val containerBg = ThemeManager.getAdaptiveCardContainerColor(themeMode, season)
  val cardBorder = ThemeManager.getAdaptiveCardBorderColor(themeMode, season)
  val dividerColor = if (isLight) Color(0x221C1C1E) else Color.White.copy(alpha = 0.20f)

  Surface(
    modifier = modifier
      .fillMaxHeight()
      .testTag("sidebar_navigation_panel"),
    color = containerBg,
    tonalElevation = 0.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // App Title Header - Adaptive contrast typography
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.padding(vertical = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.20f))
              .border(1.dp, accentColor.copy(alpha = 0.40f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(24.dp)
            )
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Files v2.4.6",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = primaryTextColor
              )
            )
            Text(
              text = "Local Storage Manager",
              style = MaterialTheme.typography.labelMedium.copy(
                color = secondaryTextColor,
                fontWeight = FontWeight.Medium
              )
            )
          }
        }

        HorizontalDivider(color = dividerColor)

        // Navigation Nodes
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          SidebarItem(
            label = "Dashboard",
            icon = Icons.Default.Dashboard,
            selected = selectedNode == NavigationNode.DASHBOARD,
            tag = "nav_item_dashboard",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onNodeSelected(NavigationNode.DASHBOARD) }
          )

          SidebarItem(
            label = "File Explorer",
            icon = Icons.Default.FolderSpecial,
            selected = selectedNode == NavigationNode.EXPLORER,
            tag = "nav_item_explorer",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onNodeSelected(NavigationNode.EXPLORER) }
          )

          SidebarItem(
            label = "Recent Files",
            icon = Icons.Default.Schedule,
            selected = selectedNode == NavigationNode.RECENTS,
            tag = "nav_item_recents",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onNodeSelected(NavigationNode.RECENTS) }
          )

          SidebarItem(
            label = "Deep Search",
            icon = Icons.Default.Search,
            selected = selectedNode == NavigationNode.SEARCH,
            tag = "nav_item_search",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onNodeSelected(NavigationNode.SEARCH) }
          )

          SidebarItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            selected = selectedNode == NavigationNode.SETTINGS,
            tag = "nav_item_settings",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onNodeSelected(NavigationNode.SETTINGS) }
          )
        }

        HorizontalDivider(color = dividerColor)

        // Drawer Tools Section
        Text(
          text = "Drawer Tools & Engines",
          style = MaterialTheme.typography.labelSmall.copy(
            color = secondaryTextColor,
            fontWeight = FontWeight.Bold
          ),
          modifier = Modifier.padding(horizontal = 4.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          SidebarItem(
            label = "Environmental Backdrop",
            icon = Icons.Default.WbSunny,
            selected = false,
            tag = "drawer_item_environmental_engine",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onOpenEnvironmentalEngineDialog?.invoke() }
          )

          SidebarItem(
            label = "Search & Scrapers",
            icon = Icons.Default.FindInPage,
            selected = false,
            tag = "drawer_item_search_tools",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onOpenSearchConfigDialog?.invoke() }
          )

          SidebarItem(
            label = "Wallpaper Engine",
            icon = Icons.Default.Wallpaper,
            selected = false,
            tag = "drawer_item_wallpaper_engine",
            textColor = primaryTextColor,
            accentColor = accentColor,
            onClick = { onOpenWallpaperEngineDialog?.invoke() }
          )
        }
      }

      // Native Device Type Query for Hardware Factor
      val configuration = LocalConfiguration.current
      val isTablet = (configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE ||
          Build.MODEL.contains("Tab", ignoreCase = true) ||
          Build.MODEL.contains("SM-X", ignoreCase = true)
      val deviceIcon = if (isTablet) Icons.Default.Tablet else Icons.Default.Smartphone

      // Live StatFs Partition Tracker Metric Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("hardware_storage_tracker_card"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = containerBg)
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = deviceIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
              )
              Text(
                text = "Device Storage",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  color = primaryTextColor
                )
              )
            }
            Text(
              text = storageMetrics.totalGbFormatted,
              style = MaterialTheme.typography.labelSmall.copy(
                color = primaryTextColor,
                fontWeight = FontWeight.Bold
              )
            )
          }

          // Device storage metrics bar - high contrast fill & track
          LinearProgressIndicator(
            progress = { storageMetrics.usedRatio },
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
              text = "${storageMetrics.usedGbFormatted} Used",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = primaryTextColor
              )
            )
            Text(
              text = "${storageMetrics.freeGbFormatted} Free",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor
              )
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SidebarItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  tag: String,
  textColor: Color,
  accentColor: Color,
  onClick: () -> Unit
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val bgColor = if (selected) accentColor.copy(alpha = 0.20f) else Color.Transparent
  val iconTint = if (selected) accentColor else textColor

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .clickable {
        HapticFeedbackHelper.performToggleFeedback(context)
        onClick()
      }
      .padding(horizontal = 12.dp)
      .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(20.dp)
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )
    )
  }
}
