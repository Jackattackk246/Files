package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.model.*
import com.example.ui.*
import com.example.ui.wallpaper.BuiltInWallpaperBackdrop
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import com.example.util.EnvironmentalPreferences
import com.example.util.FileManager
import com.example.util.GyroscopeParallaxEngine
import com.example.util.HapticFeedbackHelper
import com.example.util.RecentFilesTracker
import com.example.util.ThemePreferences
import android.view.View
import com.example.ui.viewer.ProtectedPathDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity(), ImageLoaderFactory {

  private val appImageLoader: ImageLoader by lazy {
    ImageLoader.Builder(applicationContext)
      .memoryCache {
        MemoryCache.Builder(applicationContext)
          .maxSizePercent(0.20)
          .strongReferencesEnabled(true)
          .build()
      }
      .diskCache {
        DiskCache.Builder()
          .directory(cacheDir.resolve("image_cache"))
          .maxSizePercent(0.02)
          .build()
      }
      .respectCacheHeaders(false)
      .allowHardware(true)
      .allowRgb565(false)
      .crossfade(true)
      .build()
  }

  override fun newImageLoader(): ImageLoader = appImageLoader

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    try {
      appImageLoader.memoryCache?.trimMemory(level)
    } catch (_: Exception) {}
  }

  override fun onLowMemory() {
    super.onLowMemory()
    try {
      appImageLoader.memoryCache?.clear()
    } catch (_: Exception) {}
  }

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    coil.Coil.setImageLoader(appImageLoader)
    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT

    // FORCE INJECTION: Pull the root decor window element directly from the OS layer
    val rootDecorWindowView: View = window.decorView.rootView
    
    // Hardcode the canvas background color strictly to a low-light dark workspace profile
    val midnightCharcoalCanvas = android.graphics.Color.parseColor("#0F1115")
    rootDecorWindowView.setBackgroundColor(midnightCharcoalCanvas)

    setContent {
      val context = LocalContext.current
      val coroutineScope = rememberCoroutineScope()

      // Protected path intercept dialog target
      var protectedPathTarget by remember { mutableStateOf<File?>(null) }

      // 1. Persistent Multi-Theme State (Read synchronously from SharedPreferences)
      var themeMode by remember { mutableStateOf(ThemePreferences.getSavedThemeMode(context)) }
      var customAccentColor by remember { mutableStateOf(ThemePreferences.getSavedCustomAccentColor(context)) }
      var environmentalConfig by remember { mutableStateOf(EnvironmentalPreferences.getConfig(context)) }

      // 2. Manage External Storage & Permissions Startup Check
      var hasStoragePermission by remember {
        mutableStateOf(
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
          } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
          }
        )
      }

      val legacyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
      ) { permissions ->
        hasStoragePermission = permissions.values.all { it }
      }

      val manageStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
      ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          hasStoragePermission = Environment.isExternalStorageManager()
          if (hasStoragePermission) {
            Toast.makeText(context, "Storage Access Granted", Toast.LENGTH_SHORT).show()
          }
        }
      }

      LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          if (!Environment.isExternalStorageManager()) {
            try {
              val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
              }
              manageStorageLauncher.launch(intent)
            } catch (_: Exception) {
              try {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(fallbackIntent)
              } catch (_: Exception) {}
            }
          }
        } else {
          legacyPermissionLauncher.launch(
            arrayOf(
              Manifest.permission.READ_EXTERNAL_STORAGE,
              Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
          )
        }
      }

      // Root Starting Directory
      val rootDir = remember { FileManager.getRootDirectory() }
      var currentDirectory by remember { mutableStateOf(rootDir) }
      val directoryHistory = remember { mutableStateListOf<File>() }
      var filesList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
      var highlightFilePath by remember { mutableStateOf<String?>(null) }

      // Live Storage Hardware Metrics
      var storageMetrics by remember { mutableStateOf(FileManager.getStorageMetrics()) }

      fun refreshDirectoryFiles() {
        filesList = FileManager.listFiles(currentDirectory)
        storageMetrics = FileManager.getStorageMetrics()
      }

      LaunchedEffect(currentDirectory) {
        refreshDirectoryFiles()
      }

      // Navigation & Drawer State
      var selectedNavNode by remember { mutableStateOf(NavigationNode.DASHBOARD) }
      val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

      // Search and Wallpaper Engine State
      var searchQuery by remember { mutableStateOf("") }
      var searchOptions by remember { mutableStateOf(SearchOptions()) }
      var wallpaperConfig by remember { mutableStateOf(WallpaperConfig()) }

      // Dialog States
      var showConfigurationsDialog by remember { mutableStateOf(false) }
      var showSearchConfigDialog by remember { mutableStateOf(false) }
      var showWallpaperEngineDialog by remember { mutableStateOf(false) }
      var showEnvironmentalDialog by remember { mutableStateOf(false) }

      var activeManageFileItem by remember { mutableStateOf<FileItem?>(null) }
      var isFromRecentsTabDialog by remember { mutableStateOf(false) }
      var activeAnalyticsFileItem by remember { mutableStateOf<FileItem?>(null) }
      var renameTargetItem by remember { mutableStateOf<FileItem?>(null) }
      var renameInputText by remember { mutableStateOf("") }
      var isCreateFolderDialogOpen by remember { mutableStateOf(false) }
      var newFolderNameInput by remember { mutableStateOf("") }

      // Clipboard state
      var clipboardFile by remember { mutableStateOf<FileItem?>(null) }
      var clipboardOperation by remember { mutableStateOf<String?>(null) }

      // Compression Progress Dialog state
      var isCompressingOrUnzipping by remember { mutableStateOf(false) }
      var compressionProgressRatio by remember { mutableFloatStateOf(0f) }
      var compressionStatusMsg by remember { mutableStateOf("") }

      // Search scraper effect
      LaunchedEffect(searchQuery, searchOptions.deepTextSearch, currentDirectory) {
        if (searchQuery.isNotBlank()) {
          filesList = FileManager.searchFiles(
            rootFolder = currentDirectory,
            query = searchQuery,
            currentDirOnly = false,
            deepTextIndexing = searchOptions.deepTextSearch
          )
        } else {
          refreshDirectoryFiles()
        }
      }

      // 4. Native Hardware Back-Button / Back-Swipe Interceptors
      val isAnyDialogActive = activeManageFileItem != null ||
          activeAnalyticsFileItem != null ||
          renameTargetItem != null ||
          isCreateFolderDialogOpen ||
          showSearchConfigDialog ||
          showWallpaperEngineDialog ||
          showEnvironmentalDialog ||
          showConfigurationsDialog

      val canNavigateBackInExplorer = selectedNavNode == NavigationNode.EXPLORER &&
          (directoryHistory.isNotEmpty() || (currentDirectory.parentFile != null && currentDirectory.parentFile!!.exists() && currentDirectory.absolutePath != rootDir.absolutePath))

      val shouldInterceptBack = isAnyDialogActive ||
          drawerState.isOpen ||
          selectedNavNode != NavigationNode.DASHBOARD ||
          canNavigateBackInExplorer

      BackHandler(enabled = shouldInterceptBack) {
        when {
          drawerState.isOpen -> {
            coroutineScope.launch { drawerState.close() }
          }
          activeManageFileItem != null -> activeManageFileItem = null
          activeAnalyticsFileItem != null -> activeAnalyticsFileItem = null
          renameTargetItem != null -> renameTargetItem = null
          isCreateFolderDialogOpen -> isCreateFolderDialogOpen = false
          showSearchConfigDialog -> showSearchConfigDialog = false
          showWallpaperEngineDialog -> showWallpaperEngineDialog = false
          showEnvironmentalDialog -> showEnvironmentalDialog = false
          showConfigurationsDialog -> showConfigurationsDialog = false
          selectedNavNode == NavigationNode.EXPLORER -> {
            if (directoryHistory.isNotEmpty()) {
              val previousDir = directoryHistory.removeAt(directoryHistory.lastIndex)
              currentDirectory = previousDir
              refreshDirectoryFiles()
            } else if (currentDirectory.parentFile != null && currentDirectory.parentFile!!.exists() && currentDirectory.absolutePath != rootDir.absolutePath) {
              currentDirectory = currentDirectory.parentFile!!
              refreshDirectoryFiles()
            } else {
              selectedNavNode = NavigationNode.DASHBOARD
            }
          }
          selectedNavNode != NavigationNode.DASHBOARD -> {
            selectedNavNode = NavigationNode.DASHBOARD
          }
        }
      }

      // Navigation helpers
      fun navigateToDirectory(target: File) {
        // FORCE CONVERSION: Convert the entire file pathway string to pure lowercase
        val cleanPath = target.absolutePath.lowercase(Locale.ROOT)

        // ABSOLUTE SECURITY CHECK: Match exact system variations and directory strings
        val isDataPartition = cleanPath.contains("android/data")
        val isObbPartition = cleanPath.contains("android/obb")

        if (isDataPartition || isObbPartition) {
          if (!ThemePreferences.isDirectRootLaunchEnabled(context)) {
            // STOP ALL RENDER TASKS INSTANTLY: Do not pass go, do not let the folder view initialize
            protectedPathTarget = target
            return
          }
        }

        if (target.exists() && target.isDirectory) {
          if (currentDirectory != target) {
            directoryHistory.add(currentDirectory)
          }
          currentDirectory = target
          searchQuery = ""
          refreshDirectoryFiles()
        }
      }

      MyApplicationTheme(
        themeMode = themeMode,
        customAccentColor = customAccentColor,
        season = environmentalConfig.selectedSeason
      ) {
        val adaptivePrimaryTextColor = com.example.ui.theme.ThemeManager.getAdaptivePrimaryTextColor(
          themeMode = themeMode,
          season = environmentalConfig.selectedSeason
        )

        // Gyroscope-driven Parallax Offset for background canvas and UI depth
        val parallaxOffset = GyroscopeParallaxEngine.rememberParallaxOffset(enabled = true)

        // Modal Navigation Drawer Wrapper
        ModalNavigationDrawer(
          drawerState = drawerState,
          gesturesEnabled = true,
          drawerContent = {
            ModalDrawerSheet(
              modifier = Modifier.width(310.dp),
              drawerContainerColor = if (com.example.ui.theme.ThemeManager.isLightBackgroundProfile(themeMode, environmentalConfig.selectedSeason)) {
                Color(0xEEFFFFFF)
              } else {
                com.example.ui.theme.ThemeManager.GlassMaskCharcoal
              }
            ) {
              SidebarPanel(
                selectedNode = selectedNavNode,
                onNodeSelected = { node ->
                  selectedNavNode = node
                  coroutineScope.launch { drawerState.close() }
                },
                storageMetrics = storageMetrics,
                themeMode = themeMode,
                season = environmentalConfig.selectedSeason,
                onOpenConfigurationsDialog = {
                  coroutineScope.launch {
                    drawerState.close()
                    showConfigurationsDialog = true
                  }
                },
                onOpenSearchConfigDialog = {
                  coroutineScope.launch {
                    drawerState.close()
                    showSearchConfigDialog = true
                  }
                },
                onOpenWallpaperEngineDialog = {
                  coroutineScope.launch {
                    drawerState.close()
                    showWallpaperEngineDialog = true
                  }
                },
                onOpenEnvironmentalEngineDialog = {
                  coroutineScope.launch {
                    drawerState.close()
                    showEnvironmentalDialog = true
                  }
                }
              )
            }
          }
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(ThemeManager.getThemeVerticalGradient(themeMode, environmentalConfig.selectedSeason))
          ) {
            // Gyroscope-shifted Background Layer Container
            Box(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  translationX = parallaxOffset.backgroundX.dp.toPx()
                  translationY = parallaxOffset.backgroundY.dp.toPx()
                  scaleX = 1.08f
                  scaleY = 1.08f
                }
            ) {
              // 1. Dynamic Weather Canvas / Animated Environmental Engine Backdrop (Bleeds edge-to-edge behind status bar)
              if (ThemeManager.shouldMountBackdropCanvas(themeMode)) {
                AnimatedEnvironmentalBackground(
                  config = environmentalConfig,
                  modifier = Modifier.fillMaxSize()
                )
              }

              // 2. Custom Wallpaper Engine Background Layer
              if (wallpaperConfig.hasWallpaper && themeMode != AppThemeMode.DYNAMIC_WEATHER_CANVAS) {
                if (wallpaperConfig.imageUri != null) {
                  AsyncImage(
                    model = wallpaperConfig.imageUri,
                    contentDescription = null,
                    modifier = Modifier
                      .fillMaxSize()
                      .blur(wallpaperConfig.blurRadiusDp.dp),
                    contentScale = ContentScale.Crop
                  )
                } else if (wallpaperConfig.builtInPattern != null) {
                  BuiltInWallpaperBackdrop(
                    pattern = wallpaperConfig.builtInPattern!!,
                    modifier = Modifier
                      .fillMaxSize()
                      .blur(wallpaperConfig.blurRadiusDp.dp)
                  )
                }
                Box(
                  modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = wallpaperConfig.darkOverlayOpacity))
                )
              }
            }

            Scaffold(
              modifier = Modifier.graphicsLayer {
                translationX = parallaxOffset.foregroundX.dp.toPx()
                translationY = parallaxOffset.foregroundY.dp.toPx()
              },
              containerColor = Color.Transparent,
              topBar = {
                TopAppBar(
                  title = {
                    Text(
                      text = when (selectedNavNode) {
                        NavigationNode.DASHBOARD -> "Files & Storage"
                        NavigationNode.EXPLORER -> currentDirectory.name.ifEmpty { "Internal Storage" }
                        NavigationNode.RECENTS -> "Recent Files"
                        NavigationNode.SEARCH -> "Deep File Search"
                        NavigationNode.SETTINGS -> "Configurations"
                      },
                      style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = adaptivePrimaryTextColor
                      )
                    )
                  },
                  navigationIcon = {
                    IconButton(
                      onClick = { coroutineScope.launch { drawerState.open() } },
                      modifier = Modifier.testTag("drawer_hamburger_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Sidebar Menu",
                        tint = adaptivePrimaryTextColor
                      )
                    }
                  },
                  actions = {
                    // SAF Quick Backdoor Button in Top Bar (Device Status / Storage Access Action)
                    IconButton(
                      onClick = {
                        FileManager.openPathSAFBackdoor(context, currentDirectory.absolutePath)
                      },
                      modifier = Modifier.testTag("top_bar_saf_backdoor_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open Path in System Files",
                        tint = adaptivePrimaryTextColor
                      )
                    }

                    if (selectedNavNode == NavigationNode.EXPLORER) {
                      IconButton(
                        onClick = { isCreateFolderDialogOpen = true },
                        modifier = Modifier.testTag("top_bar_create_folder_button")
                      ) {
                        Icon(
                          imageVector = Icons.Default.CreateNewFolder,
                          contentDescription = "Create Folder",
                          tint = adaptivePrimaryTextColor
                        )
                      }
                    }

                    IconButton(
                      onClick = {
                        selectedNavNode = NavigationNode.SETTINGS
                      },
                      modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = adaptivePrimaryTextColor
                      )
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeManager.getAdaptiveTopBarColor(themeMode, environmentalConfig.selectedSeason),
                    scrolledContainerColor = ThemeManager.getAdaptiveTopBarColor(themeMode, environmentalConfig.selectedSeason)
                  )
                )
              },
              floatingActionButton = {
                if (selectedNavNode == NavigationNode.EXPLORER) {
                  FloatingActionButton(
                    onClick = { isCreateFolderDialogOpen = true },
                    containerColor = ThemeManager.getThemeAccentColor(themeMode, customAccentColor),
                    contentColor = if (ThemeManager.isLightBackgroundProfile(themeMode, environmentalConfig.selectedSeason)) Color.White else Color.Black,
                    modifier = Modifier.testTag("fab_create_folder")
                  ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Folder")
                  }
                }
              }
            ) { innerPadding ->
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(innerPadding)
              ) {
                // Animated Full Screen Layout Content Pane
                AnimatedContent(
                  targetState = selectedNavNode,
                  transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                  },
                  label = "main_screen_pane_transition"
                ) { targetNode ->
                MainContentPane(
                  selectedNavNode = targetNode,
                  currentDirectory = currentDirectory,
                  filesList = filesList,
                  highlightFilePath = highlightFilePath,
                  recentFiles = RecentFilesTracker.getRecents(),
                  searchQuery = searchQuery,
                  onSearchQueryChanged = { searchQuery = it },
                  searchOptions = searchOptions,
                  onSearchOptionsChanged = { searchOptions = it },
                  themeMode = themeMode,
                  onThemeModeChanged = {
                    themeMode = it
                    ThemePreferences.setSavedThemeMode(context, it)
                  },
                  customAccentColor = customAccentColor,
                  onCustomAccentColorChanged = {
                    customAccentColor = it
                    ThemePreferences.setSavedCustomAccentColor(context, it)
                  },
                  storageMetrics = storageMetrics,
                  onRefreshStorage = {
                    storageMetrics = FileManager.getStorageMetrics()
                    refreshDirectoryFiles()
                  },
                  onNavigateToExplorer = { targetFolder, filterQuery ->
                    selectedNavNode = NavigationNode.EXPLORER
                    if (targetFolder != null) {
                      navigateToDirectory(targetFolder)
                    }
                    if (filterQuery != null) {
                      searchQuery = filterQuery
                    }
                  },
                  onNavigateToSettings = {
                    selectedNavNode = NavigationNode.SETTINGS
                  },
                  onNavigateToDirectory = { dir ->
                    navigateToDirectory(dir)
                  },
                  onFileItemClick = { item ->
                    if (item.isDirectory) {
                      navigateToDirectory(item.file)
                    } else {
                      RecentFilesTracker.recordAccess(item.file)
                      FileManager.openWithSystemDefault(context, item.file)
                    }
                  },
                  onFileItemLongClick = { item ->
                    isFromRecentsTabDialog = false
                    activeManageFileItem = item
                  },
                  onRecentItemClick = { item ->
                    if (item.isDirectory) {
                      navigateToDirectory(item.file)
                      selectedNavNode = NavigationNode.EXPLORER
                    } else {
                      isFromRecentsTabDialog = true
                      activeManageFileItem = item
                    }
                  },
                  onClearRecentHistory = { RecentFilesTracker.clear() },
                  onBatchZipRequest = { batch ->
                    coroutineScope.launch {
                      isCompressingOrUnzipping = true
                      compressionProgressRatio = 0f
                      compressionStatusMsg = "Preparing batch archive..."
                      val outputFile = File(currentDirectory, "Batch_Archive_${System.currentTimeMillis() % 10000}.zip")
                      val result = FileManager.compressToZip(
                        sources = batch.map { it.file },
                        zipOutputFile = outputFile,
                        onProgress = { ratio, msg ->
                          compressionProgressRatio = ratio
                          compressionStatusMsg = msg
                        }
                      )
                      isCompressingOrUnzipping = false
                      if (result.isSuccess) {
                        HapticFeedbackHelper.performTransferSuccessFeedback(context)
                        Toast.makeText(context, "Archive created: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                        refreshDirectoryFiles()
                      } else {
                        HapticFeedbackHelper.performErrorFeedback(context)
                        Toast.makeText(context, "Archive failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                      }
                    }
                  },
                  onCreateFolderRequest = { isCreateFolderDialogOpen = true },
                  season = environmentalConfig.selectedSeason,
                  onOpenSearchConfigDialog = { showSearchConfigDialog = true },
                  onOpenWallpaperEngineDialog = { showWallpaperEngineDialog = true },
                  onOpenEnvironmentalEngineDialog = { showEnvironmentalDialog = true }
                )
              }
            }
          }
        }

        // Universal 'Manage File' Dialog (With 3x2 action cards grid)
        activeManageFileItem?.let { item ->
          ManageFileDialog(
            fileItem = item,
            isFromRecentsTab = isFromRecentsTabDialog,
            onDismiss = { activeManageFileItem = null },
            onShowInFolder = { target ->
              val parent = target.file.parentFile
              if (parent != null && parent.exists()) {
                selectedNavNode = NavigationNode.EXPLORER
                navigateToDirectory(parent)
                highlightFilePath = target.path
                searchQuery = ""

                coroutineScope.launch {
                  refreshDirectoryFiles()
                  delay(2000)
                  highlightFilePath = null
                }
              } else {
                Toast.makeText(context, "Parent folder not accessible", Toast.LENGTH_SHORT).show()
              }
            },
            onRenameRequest = {
              renameTargetItem = it
              renameInputText = it.name
            },
            onCopyRequest = {
              clipboardFile = it
              clipboardOperation = "COPY"
              Toast.makeText(context, "Copied to clipboard. Navigate and tap 'Paste Here'", Toast.LENGTH_LONG).show()
            },
            onMoveRequest = {
              clipboardFile = it
              clipboardOperation = "MOVE"
              Toast.makeText(context, "Cut to clipboard. Navigate and tap 'Paste Here'", Toast.LENGTH_LONG).show()
            },
            onDeleteRequest = { target ->
              coroutineScope.launch {
                val res = FileManager.delete(target.file)
                if (res.isSuccess) {
                  HapticFeedbackHelper.performToggleFeedback(context)
                  RecentFilesTracker.removeAll { it.path == target.path }
                  Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                  refreshDirectoryFiles()
                } else {
                  HapticFeedbackHelper.performErrorFeedback(context)
                  Toast.makeText(context, "Delete failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
              }
            },
            onZipRequest = { target ->
              coroutineScope.launch {
                isCompressingOrUnzipping = true
                compressionProgressRatio = 0f
                compressionStatusMsg = "Compressing ${target.name}..."
                val outputFile = File(target.file.parentFile ?: currentDirectory, "${target.name}.zip")
                val result = FileManager.compressToZip(
                  sources = listOf(target.file),
                  zipOutputFile = outputFile,
                  onProgress = { ratio, msg ->
                    compressionProgressRatio = ratio
                    compressionStatusMsg = msg
                  }
                )
                isCompressingOrUnzipping = false
                if (result.isSuccess) {
                  HapticFeedbackHelper.performTransferSuccessFeedback(context)
                  Toast.makeText(context, "Compressed: ${outputFile.name}", Toast.LENGTH_SHORT).show()
                  refreshDirectoryFiles()
                } else {
                  HapticFeedbackHelper.performErrorFeedback(context)
                  Toast.makeText(context, "Zip failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
              }
            },
            onUnzipRequest = { target ->
              coroutineScope.launch {
                isCompressingOrUnzipping = true
                compressionProgressRatio = 0f
                compressionStatusMsg = "Unpacking ${target.name}..."
                val targetDir = File(target.file.parentFile ?: currentDirectory, target.name.removeSuffix(".zip"))
                val result = FileManager.extractZip(
                  zipFile = target.file,
                  targetDir = targetDir,
                  onProgress = { ratio, msg ->
                    compressionProgressRatio = ratio
                    compressionStatusMsg = msg
                  }
                )
                isCompressingOrUnzipping = false
                if (result.isSuccess) {
                  HapticFeedbackHelper.performTransferSuccessFeedback(context)
                  Toast.makeText(context, "Extracted to ${targetDir.name}", Toast.LENGTH_SHORT).show()
                  refreshDirectoryFiles()
                } else {
                  HapticFeedbackHelper.performErrorFeedback(context)
                  Toast.makeText(context, "Unzip failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
              }
            },
            onAnalyticsRequest = {
              activeAnalyticsFileItem = it
            }
          )
        }

        // Folder Analytics Modal
        activeAnalyticsFileItem?.let { item ->
          FolderDetailsDialog(
            fileItem = item,
            onDismiss = { activeAnalyticsFileItem = null }
          )
        }

        // Compression Progress Dialog
        if (isCompressingOrUnzipping) {
          CompressionProgressDialog(
            title = "Zip Archive Stream",
            progressRatio = compressionProgressRatio,
            statusMessage = compressionStatusMsg
          )
        }

        // Rename Dialog
        renameTargetItem?.let { target ->
          AlertDialog(
            onDismissRequest = { renameTargetItem = null },
            title = { Text("Rename ${target.name}") },
            text = {
              OutlinedTextField(
                value = renameInputText,
                onValueChange = { renameInputText = it },
                singleLine = true,
                label = { Text("New Name") },
                modifier = Modifier.fillMaxWidth().testTag("rename_input_field")
              )
            },
            confirmButton = {
              Button(
                onClick = {
                  val targetItem = renameTargetItem
                  renameTargetItem = null
                  if (targetItem != null && renameInputText.isNotBlank()) {
                    coroutineScope.launch {
                      val res = FileManager.rename(targetItem.file, renameInputText.trim())
                      if (res.isSuccess) {
                        HapticFeedbackHelper.performTransferSuccessFeedback(context)
                        Toast.makeText(context, "Renamed to ${renameInputText.trim()}", Toast.LENGTH_SHORT).show()
                        refreshDirectoryFiles()
                      } else {
                        HapticFeedbackHelper.performErrorFeedback(context)
                        Toast.makeText(context, "Rename failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                      }
                    }
                  }
                }
              ) {
                Text("Save")
              }
            },
            dismissButton = {
              TextButton(onClick = { renameTargetItem = null }) {
                Text("Cancel")
              }
            }
          )
        }

        // Create Folder Dialog
        if (isCreateFolderDialogOpen) {
          AlertDialog(
            onDismissRequest = { isCreateFolderDialogOpen = false },
            title = { Text("Create New Folder") },
            text = {
              OutlinedTextField(
                value = newFolderNameInput,
                onValueChange = { newFolderNameInput = it },
                singleLine = true,
                label = { Text("Folder Name") },
                modifier = Modifier.fillMaxWidth().testTag("create_folder_input_field")
              )
            },
            confirmButton = {
              Button(
                onClick = {
                  val folderName = newFolderNameInput.trim()
                  isCreateFolderDialogOpen = false
                  newFolderNameInput = ""
                  if (folderName.isNotBlank()) {
                    val newDir = File(currentDirectory, folderName)
                    if (!newDir.exists()) {
                      newDir.mkdirs()
                      HapticFeedbackHelper.performTransferSuccessFeedback(context)
                      Toast.makeText(context, "Created folder $folderName", Toast.LENGTH_SHORT).show()
                      refreshDirectoryFiles()
                    } else {
                      HapticFeedbackHelper.performErrorFeedback(context)
                      Toast.makeText(context, "Folder already exists", Toast.LENGTH_SHORT).show()
                    }
                  }
                }
              ) {
                Text("Create")
              }
            },
            dismissButton = {
              TextButton(onClick = { isCreateFolderDialogOpen = false }) {
                Text("Cancel")
              }
            }
          )
        }

        // Configurations Dialog (Direct Floating Tool Modal)
        if (showConfigurationsDialog) {
          ConfigurationsDialog(
            currentThemeMode = themeMode,
            onThemeModeChanged = {
              themeMode = it
              ThemePreferences.setSavedThemeMode(context, it)
            },
            customAccentColor = customAccentColor,
            onCustomAccentColorChanged = {
              customAccentColor = it
              ThemePreferences.setSavedCustomAccentColor(context, it)
            },
            storageMetrics = storageMetrics,
            onRefreshStorage = {
              storageMetrics = FileManager.getStorageMetrics()
              refreshDirectoryFiles()
            },
            onDismiss = { showConfigurationsDialog = false }
          )
        }

        // Search Config Dialog
        if (showSearchConfigDialog) {
          SearchConfigDialog(
            searchOptions = searchOptions,
            onSearchOptionsChanged = { searchOptions = it },
            onDismiss = { showSearchConfigDialog = false }
          )
        }

        // Wallpaper Engine Dialog
        if (showWallpaperEngineDialog) {
          WallpaperEngineDialog(
            wallpaperConfig = wallpaperConfig,
            onWallpaperConfigChanged = { wallpaperConfig = it },
            onDismiss = { showWallpaperEngineDialog = false }
          )
        }

        // Environmental Engine Dialog
        if (showEnvironmentalDialog) {
          EnvironmentalEngineDialog(
            config = environmentalConfig,
            onConfigChanged = {
              environmentalConfig = it
              EnvironmentalPreferences.saveConfig(context, it)
            },
            onDismiss = { showEnvironmentalDialog = false }
          )
        }

        // Protected Path Intercept Gate Modal
        protectedPathTarget?.let { target ->
          ProtectedPathDialog(
            path = target.absolutePath,
            themeMode = themeMode,
            customAccentColor = customAccentColor,
            onOpenOtherApp = {
              FileManager.openPathSAFBackdoor(context, target.absolutePath)
              protectedPathTarget = null
            },
            onOpenAnyway = {
              if (currentDirectory != target) {
                directoryHistory.add(currentDirectory)
              }
              currentDirectory = target
              searchQuery = ""
              refreshDirectoryFiles()
              protectedPathTarget = null
            },
            onDismiss = {
              protectedPathTarget = null
            }
          )
        }
      }
    }
  }
}
}

@Composable
private fun MainContentPane(
  selectedNavNode: NavigationNode,
  currentDirectory: File,
  filesList: List<FileItem>,
  highlightFilePath: String?,
  recentFiles: List<RecentFileItem>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  searchOptions: SearchOptions,
  onSearchOptionsChanged: (SearchOptions) -> Unit,
  themeMode: AppThemeMode,
  onThemeModeChanged: (AppThemeMode) -> Unit,
  customAccentColor: Color?,
  onCustomAccentColorChanged: (Color?) -> Unit,
  storageMetrics: FileManager.StorageMetrics,
  onRefreshStorage: () -> Unit,
  onNavigateToExplorer: (File?, String?) -> Unit,
  onNavigateToSettings: () -> Unit,
  onNavigateToDirectory: (File) -> Unit,
  onFileItemClick: (FileItem) -> Unit,
  onFileItemLongClick: (FileItem) -> Unit,
  onRecentItemClick: (FileItem) -> Unit,
  onClearRecentHistory: () -> Unit,
  onBatchZipRequest: (List<FileItem>) -> Unit,
  onCreateFolderRequest: () -> Unit,
  season: com.example.model.EnvironmentalSeason = com.example.model.EnvironmentalSeason.AUTO,
  onOpenSearchConfigDialog: () -> Unit,
  onOpenWallpaperEngineDialog: () -> Unit,
  onOpenEnvironmentalEngineDialog: () -> Unit
) {
  when (selectedNavNode) {
    NavigationNode.DASHBOARD -> {
      DashboardScreen(
        storageMetrics = storageMetrics,
        currentDirectory = currentDirectory,
        themeMode = themeMode,
        season = season,
        onNavigateToExplorer = onNavigateToExplorer,
        onNavigateToSettings = onNavigateToSettings
      )
    }

    NavigationNode.EXPLORER -> {
      ExplorerScreen(
        currentDirectory = currentDirectory,
        filesList = filesList,
        highlightFilePath = highlightFilePath,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        searchOptions = searchOptions,
        onSearchOptionsChanged = onSearchOptionsChanged,
        onNavigateToDirectory = onNavigateToDirectory,
        onFileItemClick = onFileItemClick,
        onFileItemLongClick = onFileItemLongClick,
        onBatchZipRequest = onBatchZipRequest,
        onCreateFolderRequest = onCreateFolderRequest,
        themeMode = themeMode,
        customAccentColor = customAccentColor
      )
    }

    NavigationNode.RECENTS -> {
      RecentFilesScreen(
        onFileSelected = onRecentItemClick,
        themeMode = themeMode,
        customAccentColor = customAccentColor
      )
    }

    NavigationNode.SEARCH -> {
      ExplorerScreen(
        currentDirectory = currentDirectory,
        filesList = filesList,
        highlightFilePath = highlightFilePath,
        searchQuery = searchQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        searchOptions = searchOptions,
        onSearchOptionsChanged = onSearchOptionsChanged,
        onNavigateToDirectory = onNavigateToDirectory,
        onFileItemClick = onFileItemClick,
        onFileItemLongClick = onFileItemLongClick,
        onBatchZipRequest = onBatchZipRequest,
        onCreateFolderRequest = onCreateFolderRequest,
        themeMode = themeMode,
        customAccentColor = customAccentColor
      )
    }

    NavigationNode.SETTINGS -> {
      SettingsScreen(
        currentThemeMode = themeMode,
        onThemeModeChanged = onThemeModeChanged,
        customAccentColor = customAccentColor,
        onCustomAccentColorChanged = onCustomAccentColorChanged,
        storageMetrics = storageMetrics,
        onRefreshStorage = onRefreshStorage,
        onOpenSearchConfigDialog = onOpenSearchConfigDialog,
        onOpenWallpaperEngineDialog = onOpenWallpaperEngineDialog,
        onOpenEnvironmentalEngineDialog = onOpenEnvironmentalEngineDialog
      )
    }
  }
}
