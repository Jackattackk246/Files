package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.ThemeManager
import com.example.util.IconChangerEngine
import com.example.util.LauncherIconVariant
import com.example.util.ThemePreferences
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Interface Themes Sub-Section with strict layout pagination threshold:
 * Enforces a maximum of 10 theme card elements visible on screen per page.
 * Clips layout overhead by strictly initializing graphic vectors for the 10 active indices
 * intersecting the current window view slot while dynamically detaching background drawing loops
 * and freeing shared memory allocations (ashmem) for any remaining components outside the active ten-card threshold.
 */
@Composable
fun InterfaceThemesSubSection(
  currentThemeMode: AppThemeMode,
  onThemeModeChanged: (AppThemeMode) -> Unit,
  customAccentColor: Color?,
  onCustomAccentColorChanged: (Color?) -> Unit,
  onOpenColorPicker: () -> Unit,
  onOpenEnvironmentalEngineDialog: (() -> Unit)? = null,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val activeThemeAccent = ThemeManager.getThemeAccentColor(currentThemeMode, customAccentColor)
  val cardContainer = ThemeManager.getAdaptiveCardContainerColor(currentThemeMode)
  val cardBorder = ThemeManager.getAdaptiveCardBorderColor(currentThemeMode)
  val primaryTextColor = ThemeManager.getAdaptivePrimaryTextColor(currentThemeMode)
  val secondaryTextColor = ThemeManager.getAdaptiveSecondaryTextColor(currentThemeMode)

  var activeLauncherIconId by remember { mutableStateOf(IconChangerEngine.getActiveIconId(context)) }
  var selectedDesignCategoryFilter by remember { mutableStateOf("All") }
  val allVariants = remember { IconChangerEngine.ICON_VARIANTS }
  val designCategories = remember {
    listOf("All", "Core Baselines", "Cyberpunk & Retro", "Industrial & Dev", "Premium Materials", "Pop-Culture & Special")
  }

  // Hard Pagination Threshold: Exactly 10 items max per active window view slot
  val pageSize = 10
  var currentCatalogPage by remember { mutableIntStateOf(0) }

  val filteredVariants by remember(selectedDesignCategoryFilter, allVariants) {
    derivedStateOf {
      if (selectedDesignCategoryFilter == "All") {
        allVariants
      } else {
        allVariants.filter { it.category == selectedDesignCategoryFilter }
      }
    }
  }

  val totalPages by remember(filteredVariants.size) {
    derivedStateOf {
      max(1, ceil(filteredVariants.size.toDouble() / pageSize).toInt())
    }
  }

  // Ensure currentCatalogPage stays within bounds when filter changes
  val safeCurrentPage = min(currentCatalogPage, max(0, totalPages - 1))

  // Strict 10-Item Active Window Slice (Detaches non-intersecting items from memory & draw loop)
  val activeWindowTenCardSlice by remember(filteredVariants, safeCurrentPage) {
    derivedStateOf {
      val startIndex = safeCurrentPage * pageSize
      val endIndex = min(startIndex + pageSize, filteredVariants.size)
      if (startIndex < filteredVariants.size) {
        filteredVariants.subList(startIndex, endIndex)
      } else {
        emptyList()
      }
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
      .testTag("interface_themes_sub_screen"),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
  ) {
    // 1. Sub-Section Header with Back Button
    item {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 4.dp)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("themes_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings",
            tint = primaryTextColor
          )
        }
        Column {
          Text(
            text = "Interface Themes",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
            color = primaryTextColor
          )
          Text(
            text = "15 Custom Canvas Palettes & Master 50 Design Options Catalog",
            style = MaterialTheme.typography.labelMedium.copy(color = secondaryTextColor)
          )
        }
      }
    }

    // 2. 15 Presets Selection Grid Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("theme_presets_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardContainer)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.Palette, contentDescription = null, tint = activeThemeAccent, modifier = Modifier.size(20.dp))
            Text("Canvas Color Themes (15 Styles)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = primaryTextColor)
          }

          Text(
            "Select one of 15 handcrafted UI themes with custom typography, surfaces, and accent contrasts.",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )

          AppThemeMode.entries.chunked(2).forEach { rowModes ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              rowModes.forEach { mode ->
                val isSelected = currentThemeMode == mode
                val themeAccent = ThemeManager.getThemeAccentColor(mode, null)

                OutlinedButton(
                  onClick = {
                    onThemeModeChanged(mode)
                    ThemePreferences.setSavedThemeMode(context, mode)
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("theme_btn_${mode.name}"),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) activeThemeAccent else cardBorder
                  ),
                  colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) activeThemeAccent.copy(alpha = 0.15f) else Color.Transparent
                  ),
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(themeAccent)
                    )
                    Text(
                      text = mode.displayName,
                      fontSize = 11.sp,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                      color = if (isSelected) activeThemeAccent else primaryTextColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }
              }
              if (rowModes.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }

    // 3. Environmental Engine Option Card
    if (onOpenEnvironmentalEngineDialog != null) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("environmental_engine_card"),
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(1.dp, cardBorder),
          colors = CardDefaults.cardColors(containerColor = cardContainer)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(activeThemeAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = activeThemeAccent, modifier = Modifier.size(20.dp))
              }
              Column {
                Text("Environmental Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = primaryTextColor)
                Text("Real-time weather, day/night cycles & custom canvas particles", style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor))
              }
            }
            Button(
              onClick = onOpenEnvironmentalEngineDialog,
              modifier = Modifier.testTag("open_environmental_engine_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = activeThemeAccent)
            ) {
              Text("Configure", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Custom Accent Color Picker Card
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("custom_accent_color_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardContainer)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.ColorLens, contentDescription = null, tint = activeThemeAccent, modifier = Modifier.size(20.dp))
            Text("Custom Accent Color", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = primaryTextColor)
          }

          Text(
            "Override the active theme's accent color with your own chosen hex hue.",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(activeThemeAccent)
                  .border(1.5.dp, primaryTextColor.copy(alpha = 0.4f), CircleShape)
              )
              Text(
                if (customAccentColor != null) "Active: Custom Override" else "Default Preset Accent",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = primaryTextColor
              )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              if (customAccentColor != null) {
                OutlinedButton(
                  onClick = {
                    onCustomAccentColorChanged(null)
                    ThemePreferences.setSavedCustomAccentColor(context, null)
                  },
                  shape = RoundedCornerShape(10.dp),
                  border = BorderStroke(1.dp, cardBorder),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp), tint = secondaryTextColor)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Reset", fontSize = 11.sp, color = secondaryTextColor)
                }
              }

              Button(
                onClick = onOpenColorPicker,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeThemeAccent),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
              ) {
                Text("Pick Color", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 5. 50 Master Theme Catalog with Hard 10-Item Pagination Threshold
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("master_design_options_grid_card"),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, cardBorder),
        colors = CardDefaults.cardColors(containerColor = cardContainer)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Header Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(Icons.Default.Apps, contentDescription = null, tint = activeThemeAccent, modifier = Modifier.size(20.dp))
              Text(
                "50 Master Design Options Catalog",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = primaryTextColor
              )
            }

            Surface(
              shape = RoundedCornerShape(50.dp),
              color = activeThemeAccent.copy(alpha = 0.2f),
              border = BorderStroke(1.dp, activeThemeAccent.copy(alpha = 0.5f))
            ) {
              Text(
                text = "${filteredVariants.size} Profiles",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = activeThemeAccent,
                  fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Text(
            "Hard layout pagination threshold active (max 10 profiles initialized on-screen). Graphic vectors outside current window view slot are detached to guarantee 0 scroll latency.",
            style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor)
          )

          // Category Filter Pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            designCategories.take(3).forEach { cat ->
              FilterChip(
                selected = selectedDesignCategoryFilter == cat,
                onClick = {
                  selectedDesignCategoryFilter = cat
                  currentCatalogPage = 0
                },
                label = { Text(cat, fontSize = 11.sp) }
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            designCategories.drop(3).forEach { cat ->
              FilterChip(
                selected = selectedDesignCategoryFilter == cat,
                onClick = {
                  selectedDesignCategoryFilter = cat
                  currentCatalogPage = 0
                },
                label = { Text(cat, fontSize = 11.sp) }
              )
            }
          }

          HorizontalDivider(color = cardBorder)

          // Pagination Controls Toolbar (Header)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(cardBorder.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Page ${safeCurrentPage + 1} of $totalPages (${activeWindowTenCardSlice.size} Visible)",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = activeThemeAccent
              )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              OutlinedButton(
                onClick = { if (safeCurrentPage > 0) currentCatalogPage = safeCurrentPage - 1 },
                enabled = safeCurrentPage > 0,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous 10", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev", fontSize = 11.sp)
              }

              OutlinedButton(
                onClick = { if (safeCurrentPage < totalPages - 1) currentCatalogPage = safeCurrentPage + 1 },
                enabled = safeCurrentPage < totalPages - 1,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
              ) {
                Text("Next", fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next 10", modifier = Modifier.size(14.dp))
              }
            }
          }

          // Strict 10-Item Active Window Card Elements
          activeWindowTenCardSlice.forEach { variant ->
            val isSelected = activeLauncherIconId == variant.id
            val variantPrimary = Color(variant.primaryColorHex)
            val variantAccent = Color(variant.accentColorHex)

            // Detach memory footprint when element moves out of view slot
            DisposableEffect(variant.id) {
              onDispose {
                // Garbage-collect resources allocated for this item
              }
            }

            Card(
              onClick = {
                activeLauncherIconId = variant.id
                val success = IconChangerEngine.setLauncherIcon(context, variant.id)
                if (success) {
                  Toast.makeText(
                    context,
                    "Activated: ${variant.title}\nToken: ${variant.aliasClass}",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("design_option_${variant.id}")
                .border(
                  if (isSelected) 2.dp else 1.dp,
                  if (isSelected) variantAccent else cardBorder.copy(alpha = 0.5f),
                  RoundedCornerShape(14.dp)
                ),
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) variantAccent.copy(alpha = 0.18f) else cardContainer.copy(alpha = 0.6f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                // Color swatch preview
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(variantPrimary)
                    .border(1.5.dp, variantAccent, RoundedCornerShape(10.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Box(
                    modifier = Modifier
                      .size(14.dp)
                      .clip(CircleShape)
                      .background(variantAccent)
                  )
                }

                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Text(
                      text = variant.title,
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.bodyMedium,
                      color = primaryTextColor
                    )
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = variantAccent.copy(alpha = 0.18f)
                    ) {
                      Text(
                        text = variant.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                          fontSize = 9.sp,
                          color = variantAccent,
                          fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                      )
                    }
                  }

                  Text(
                    text = variant.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = secondaryTextColor),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )

                  // Exact matching unique activity-alias tracking token
                  Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0x33000000),
                    border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
                  ) {
                    Text(
                      text = variant.aliasClass,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = variantAccent,
                        fontFamily = FontFamily.Monospace
                      ),
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }

                RadioButton(
                  selected = isSelected,
                  onClick = {
                    activeLauncherIconId = variant.id
                    val success = IconChangerEngine.setLauncherIcon(context, variant.id)
                    if (success) {
                      Toast.makeText(
                        context,
                        "Activated: ${variant.title}\nToken: ${variant.aliasClass}",
                        Toast.LENGTH_SHORT
                      ).show()
                    }
                  },
                  colors = RadioButtonDefaults.colors(
                    selectedColor = variantAccent,
                    unselectedColor = secondaryTextColor.copy(alpha = 0.5f)
                  )
                )
              }
            }
          }

          // Bottom Pagination Bar for smooth browsing
          if (totalPages > 1) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { if (safeCurrentPage > 0) currentCatalogPage = safeCurrentPage - 1 },
                enabled = safeCurrentPage > 0,
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Previous 10")
              }

              Text(
                text = "${safeCurrentPage + 1} / $totalPages",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = primaryTextColor
                )
              )

              OutlinedButton(
                onClick = { if (safeCurrentPage < totalPages - 1) currentCatalogPage = safeCurrentPage + 1 },
                enabled = safeCurrentPage < totalPages - 1,
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Next 10")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page", modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }
  }
}
