package com.example.romdownloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.romdownloader.ui.screens.HandheldScreen
import com.example.romdownloader.ui.screens.BrowseScreen
import com.example.romdownloader.ui.screens.ConsoleSelectScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.example.romdownloader.network.HostConfig
import com.example.romdownloader.ui.theme.AppTheme
import com.example.romdownloader.network.HostScanner
import com.example.romdownloader.network.SshUploader
import com.example.romdownloader.network.SshTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLDecoder
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system UI bars completely
        window.apply {
            decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        
        setContent {
            AppTheme {
                RomApp(viewModel = viewModel, downloader = AndroidDownloader(this))
            }
        }
    }
}

enum class Platform(val id: String, val label: String, val archiveUrl: String, val extensions: List<String>) {
    NES("nes", "NES", "insert_your_link_here", listOf("7z", "zip")),
    SNES("snes", "SNES", "insert_your_link_here", listOf("7z", "zip")),
    GENESIS("genesis", "Genesis", "insert_your_link_here", listOf("7z", "zip")),
    GB("gb", "Game Boy", "insert_your_link_here", listOf("7z", "zip")),
    GBA("gba", "GBA", "insert_your_link_here", listOf("7z", "zip")),
    GBC("gbc", "GBC", "insert_your_link_here", listOf("7z", "zip")),
    GAMEGEAR("gamegear", "Game Gear", "insert_your_link_here", listOf("7z", "zip")),
    NGP("ngp", "Neo Geo Pocket", "insert_your_link_here", listOf("7z", "zip")),
    SMS("sms", "Sega Master System", "insert_your_link_here", listOf("7z", "zip")),
    SEGACD("segacd", "Sega CD", "insert_your_link_here", listOf("7z", "zip", "chd", "cue", "bin")),
    SEGA32X("sega32x", "Sega 32X", "insert_your_link_here", listOf("7z", "zip")),
    SATURN("saturn", "Sega Saturn", "insert_your_link_here", listOf("7z", "zip")),
    TG16("tg16", "TurboGrafx-16", "insert_your_link_here", listOf("7z", "zip")),
    PS1("ps1", "PlayStation", "insert_your_link_here", listOf("7z", "zip", "cue")),
    N64("n64", "Nintendo 64", "insert_your_link_here", listOf("7z", "zip", "z64", "n64", "v64")),
    DREAMCAST("dreamcast", "Dreamcast", "insert_your_link_here", listOf("7z", "zip", "chd"));

    companion object { val all = entries.toList() }
}

data class RomItem(val displayName: String, val downloadUrl: String, val platform: Platform)

data class DiscoveredHost(val ip: String)

interface Downloader { fun download(context: Context, item: RomItem) }

data class HostTemplate(
    val name: String,
    val username: String,
    val password: String,
    val port: String,
    val remoteBasePath: String,
    val hostIp: String,
    val useGuest: Boolean,
    val isDhcpNetwork: Boolean
)

class AndroidDownloader(private val context: Context) : Downloader {
    override fun download(context: Context, item: RomItem) {
        val request = DownloadManager.Request(Uri.parse(item.downloadUrl))
            .setTitle(item.displayName)
            .setDescription("Downloading to ${item.platform.label}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "roms/${item.platform.id}/${item.displayName}"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    }
}

class RomRepository(private val client: OkHttpClient = OkHttpClient()) {
    // Match <a ... href="..." ...>
    private val linkRegex = Regex("<a\\s+[^>]*href=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE)

    private fun fetchHtml(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0 Safari/537.36"
            )
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "insert_your_link_here")
            .get()
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ""
            response.body?.string().orEmpty()
        }
    }

    private fun buildAbsoluteUrl(base: String, href: String): String {
        val normalizedBase = base.trimEnd('/') + "/"
        return if (href.startsWith("http://") || href.startsWith("https://")) href else normalizedBase + href.trimStart('/')
    }

    private fun isDirectoryLink(href: String): Boolean {
        if (href.isBlank()) return false
        if (href.startsWith("?")) return false
        if (href == "/") return false
        if (href.startsWith("../")) return false
        return href.endsWith("/")
    }

    private fun parseDirectoryRecursively(baseUrl: String, exts: Set<String>, depth: Int): List<RomItem> {
        val results = mutableListOf<RomItem>()
        val html = fetchHtml(baseUrl)
        if (html.isEmpty()) return results
        val links = linkRegex.findAll(html).mapNotNull { it.groupValues.getOrNull(1) }.toList()
        for (href in links) {
            val decoded = try { URLDecoder.decode(href, "UTF-8") } catch (e: Exception) { href }
            val lower = decoded.lowercase(Locale.ROOT)
            if (isDirectoryLink(href)) {
                if (depth > 0) {
                    val nextUrl = buildAbsoluteUrl(baseUrl, href)
                    results += parseDirectoryRecursively(nextUrl, exts, depth - 1)
                }
                continue
            }
            if (exts.any { lower.endsWith(".$it") }) {
                val nameOnly = decoded.substringAfterLast('/')
                val fullUrl = buildAbsoluteUrl(baseUrl, href)
                results += RomItem(displayName = nameOnly, downloadUrl = fullUrl, platform = Platform.GENESIS) // placeholder, will be corrected by caller
            }
        }
        return results
    }

    suspend fun search(platform: Platform, term: String): List<RomItem> = withContext(Dispatchers.IO) {
        val startUrl = platform.archiveUrl.trimEnd('/') + "/"
        val exts = platform.extensions.map { it.lowercase(Locale.ROOT) }.toSet()
        val rawItems = parseDirectoryRecursively(startUrl, exts, depth = 3)
        val items = rawItems.map { it.copy(platform = platform) }
        if (term.isBlank()) items else items.filter { it.displayName.contains(term, ignoreCase = true) }
    }
}

class MainViewModel : ViewModel() {
    var selectedPlatform by mutableStateOf<Platform?>(Platform.GENESIS)
        private set
    var searchTerm by mutableStateOf("")
        private set
    var results by mutableStateOf<List<RomItem>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set

    var hosts by mutableStateOf<List<DiscoveredHost>>(emptyList())
        private set
    var selectedHost by mutableStateOf<DiscoveredHost?>(null)
        private set
    var isScanning by mutableStateOf(false)
        private set

    private val repo = RomRepository()
    private val scanner = HostScanner()
    private val uploader = SshUploader()
    private val tester = SshTester()

    fun setPlatform(p: Platform?) { selectedPlatform = p }
    fun updateSearchTerm(t: String) { searchTerm = t }

    fun searchAll() {
        val term = searchTerm
        isSearching = true
        results = emptyList()
        viewModelScope.launch {
            val aggregated = mutableListOf<RomItem>()
            for (p in Platform.all) {
                val list = runCatching { repo.search(p, term) }.getOrDefault(emptyList())
                aggregated += list
            }
            results = aggregated
            isSearching = false
        }
    }

    fun searchSelected() {
        val term = searchTerm
        val p = selectedPlatform ?: return
        isSearching = true
        results = emptyList()
        viewModelScope.launch {
            val list = runCatching { repo.search(p, term) }.getOrDefault(emptyList())
            results = list
            isSearching = false
        }
    }

    fun scanHosts() {
        isScanning = true
        hosts = emptyList()
        selectedHost = null
        viewModelScope.launch(Dispatchers.IO) {
            val prefix = scanner.getLocalSubnetPrefix()
            val found = if (prefix != null) scanner.scanQuick(prefix) else emptyList()
            withContext(Dispatchers.Main) {
                hosts = found.map { DiscoveredHost(it) }
                selectedHost = hosts.firstOrNull()
                isScanning = false
            }
        }
    }

    fun selectHost(h: DiscoveredHost?) { selectedHost = h }

    fun addHostFromIp(ip: String): Boolean {
        val trimmed = ip.trim()
        val ipv4 = Regex("^((25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})\\.){3}(25[0-5]|2[0-4]\\d|[0-1]?\\d{1,2})$")
        if (!ipv4.matches(trimmed)) return false
        val host = DiscoveredHost(trimmed)
        if (hosts.none { it.ip == host.ip }) {
            hosts = hosts + host
        }
        selectedHost = host
        return true
    }

    // Connection settings
    var sshUsername by mutableStateOf("root")
    var sshPassword by mutableStateOf("root")
    var sshPort by mutableStateOf("22")
    var useGuest by mutableStateOf(false)
    var isDhcpNetwork by mutableStateOf(true)
    var remoteBasePath by mutableStateOf("")

    // Templates state
    var rocknixTemplate by mutableStateOf(
        HostTemplate(
            name = "Rocknix",
            username = "root",
            password = "rocknix",
            port = "22",
            remoteBasePath = "/storage/roms",
            hostIp = "192.168.0.132",
            useGuest = false,
            isDhcpNetwork = true
        )
    )
    var muosTemplate by mutableStateOf(
        HostTemplate(
            name = "muOS",
            username = "root",
            password = "muos",
            port = "22",
            remoteBasePath = "/mnt/mmc/ROMS",
            hostIp = "",
            useGuest = false,
            isDhcpNetwork = true
        )
    )

    private fun templateKey(name: String, field: String): String = "template.${name}.${field}"

    private fun getStringPref(sp: android.content.SharedPreferences, key: String, default: String): String {
        val anyValue = sp.all[key]
        return when (anyValue) {
            is String -> anyValue
            else -> default
        }
    }

    fun loadTemplates(context: Context) {
        val sp = context.getSharedPreferences("templates", Context.MODE_PRIVATE)
        fun readDefaulting(base: HostTemplate): HostTemplate {
            val n = base.name
            return HostTemplate(
                name = n,
                username = getStringPref(sp, templateKey(n, "username"), base.username),
                password = getStringPref(sp, templateKey(n, "password"), base.password),
                port = getStringPref(sp, templateKey(n, "port"), base.port),
                remoteBasePath = getStringPref(sp, templateKey(n, "remoteBasePath"), base.remoteBasePath),
                hostIp = getStringPref(sp, templateKey(n, "hostIp"), base.hostIp),
                useGuest = sp.getBoolean(templateKey(n, "useGuest"), base.useGuest),
                isDhcpNetwork = sp.getBoolean(templateKey(n, "isDhcpNetwork"), base.isDhcpNetwork)
            )
        }
        rocknixTemplate = readDefaulting(rocknixTemplate)
        muosTemplate = readDefaulting(muosTemplate)
    }

    fun updateTemplate(context: Context, updated: HostTemplate) {
        when (updated.name) {
            "Rocknix" -> rocknixTemplate = updated
            "muOS" -> muosTemplate = updated
        }
        val sp = context.getSharedPreferences("templates", Context.MODE_PRIVATE)
        val n = updated.name
        sp.edit()
            .putString(templateKey(n, "username"), updated.username)
            .putString(templateKey(n, "password"), updated.password)
            .putString(templateKey(n, "port"), updated.port)
            .putString(templateKey(n, "remoteBasePath"), updated.remoteBasePath)
            .putString(templateKey(n, "hostIp"), updated.hostIp)
            .putBoolean(templateKey(n, "useGuest"), updated.useGuest)
            .putBoolean(templateKey(n, "isDhcpNetwork"), updated.isDhcpNetwork)
            .apply()
    }

    fun getTemplate(name: String): HostTemplate = when (name) {
        "Rocknix" -> rocknixTemplate
        "muOS" -> muosTemplate
        else -> rocknixTemplate
    }

    fun applyTemplateRocknix() {
        val t = rocknixTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }

    fun applyTemplateMuOS() {
        val t = muosTemplate
        sshUsername = t.username
        sshPassword = t.password
        sshPort = t.port
        useGuest = t.useGuest
        isDhcpNetwork = t.isDhcpNetwork
        remoteBasePath = t.remoteBasePath
        if (t.hostIp.isNotBlank()) addHostFromIp(t.hostIp)
    }

    fun uploadFile(localFile: File, platform: Platform, onResult: (Boolean, String?) -> Unit) {
        val hostIp = selectedHost?.ip ?: return onResult(false, "No host selected")
        viewModelScope.launch(Dispatchers.IO) {
            val port = sshPort.toIntOrNull() ?: 22
            val username = if (useGuest) sshUsername.ifEmpty { "root" } else sshUsername.ifEmpty { "root" }
            val password = if (useGuest) "" else sshPassword
            val res = uploader.upload(localFile, platform.id, HostConfig(host = hostIp, port = port, username = username, password = password), baseOverride = remoteBasePath)
            val msg = res.exceptionOrNull()?.message ?: res.getOrNull() ?: ""
            Log.d("RomDL", "Upload result isSuccess=${res.isSuccess} pathOrError=${msg}")
            withContext(Dispatchers.Main) { onResult(res.isSuccess, msg) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomApp(viewModel: MainViewModel, downloader: Downloader) {
    var selectedTab by remember { mutableStateOf(0) } // 0=Handheld, 1=Consoles (merged), 2=Custom Hosts
    var showConsoleSelect by remember { mutableStateOf(false) }
    
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        val painter = androidx.compose.ui.res.painterResource(id = com.example.romdownloader.R.drawable.handhelds)
                        Icon(
                            painter = painter,
                            contentDescription = "Handheld",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = {
                        Text(
                            "Handheld",
                            color = if (selectedTab == 0) com.example.romdownloader.ui.theme.Purple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        showConsoleSelect = true
                    },
                    icon = { 
                        Icon(
                            Icons.Filled.Games, 
                            contentDescription = "Consoles",
                            tint = if (selectedTab == 1) com.example.romdownloader.ui.theme.Purple else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    },
                    label = { 
                        Text(
                            "Consoles",
                            color = if (selectedTab == 1) com.example.romdownloader.ui.theme.Purple else MaterialTheme.colorScheme.onSurfaceVariant
                        ) 
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        val painter = androidx.compose.ui.res.painterResource(id = com.example.romdownloader.R.drawable.network)
                        Icon(
                            painter = painter,
                            contentDescription = "Network",
                            tint = Color.Unspecified
                        )
                    },
                    label = {
                        Text(
                            "Network",
                            color = if (selectedTab == 2) com.example.romdownloader.ui.theme.Purple else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (selectedTab) {
                0 -> HandheldScreen(viewModel)
                1 -> {
                    if (showConsoleSelect) {
                        ConsoleSelectScreen(
                            viewModel = viewModel,
                            onConsoleSelected = { platform ->
                                viewModel.setPlatform(platform)
                                showConsoleSelect = false
                            }
                        )
                    } else {
                        BrowseScreen(viewModel, downloader)
                    }
                }
                else -> CustomHostsScreen(viewModel)
            }
        }
    }
}

@Composable
private fun OldBrowseScreen(viewModel: MainViewModel, downloader: Downloader) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        Spacer(Modifier.height(8.dp))
        // Search row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            var showPicker by remember { mutableStateOf(false) }
            var platformLabel by remember { mutableStateOf(viewModel.selectedPlatform?.label ?: "All Platforms") }

            Button(onClick = { showPicker = true }) { Text(platformLabel, style = MaterialTheme.typography.labelLarge) }
            if (showPicker) {
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text("Select Console") },
                    text = {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Button(onClick = {
                                    viewModel.setPlatform(null)
                                    platformLabel = "All Platforms"
                                    showPicker = false
                                }, modifier = Modifier.fillMaxWidth()) { Text("All Platforms") }
                            }
                            items(Platform.all) { p ->
                                Button(onClick = {
                                    viewModel.setPlatform(p)
                                    platformLabel = p.label
                                    showPicker = false
                                }, modifier = Modifier.fillMaxWidth()) { Text(p.label) }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPicker = false }) { Text("Close") }
                    }
                )
            }

            OutlinedTextField(
                value = viewModel.searchTerm,
                onValueChange = { viewModel.updateSearchTerm(it) },
                label = { Text("Search") },
                modifier = Modifier.weight(1f)
            )

            Button(onClick = {
                if (viewModel.selectedPlatform == null) viewModel.searchAll() else viewModel.searchSelected()
            }) { Text("Search") }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        // Quick filter chips for common platforms
        LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val quick = listOf(Platform.GENESIS, Platform.SNES, Platform.PS1, Platform.N64, Platform.GBA)
            items(quick) { p ->
                val selected = viewModel.selectedPlatform == p
                FilterChip(
                    selected = selected,
                    onClick = {
                        viewModel.setPlatform(if (selected) null else p)
                    },
                    label = { Text(p.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        ResultsList(results = viewModel.results, onDownload = { item ->
            downloader.download(context = ctx, item = item)
        }, onUpload = { item ->
            val localFile = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "roms/${item.platform.id}/${item.displayName}")
            viewModel.uploadFile(localFile, item.platform) { ok, remote ->
                val msg = if (ok) "Uploaded to: ${remote ?: "unknown"}" else "Upload failed"
                android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }
}

@Composable
fun ResultsList(
    results: List<RomItem>,
    onDownload: (RomItem) -> Unit,
    onUpload: (RomItem) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(results) { item ->
            androidx.compose.material3.ListItem(
                headlineContent = { Text(item.displayName) },
                supportingContent = { Text(item.platform.label) },
                leadingContent = { androidx.compose.material3.Icon(Icons.Default.List, contentDescription = null) },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.IconButton(onClick = { onDownload(item) }) {
                            androidx.compose.material3.Icon(Icons.Default.Download, contentDescription = "Download")
                        }
                        androidx.compose.material3.IconButton(onClick = { onUpload(item) }) {
                            androidx.compose.material3.Icon(Icons.Default.Send, contentDescription = "Upload")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun OldHandheldScreen(viewModel: MainViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
        val appCtx = androidx.compose.ui.platform.LocalContext.current
        LaunchedEffect(Unit) { viewModel.loadTemplates(appCtx) }
        var showTemplatesEditor by remember { mutableStateOf(false) }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Handheld", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { showTemplatesEditor = true }) { Text("Edit templates") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text("Rocknix", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.applyTemplateRocknix() }, modifier = Modifier.fillMaxWidth()) { Text("Connect") }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text("muOS", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.applyTemplateMuOS() }, modifier = Modifier.fillMaxWidth()) { Text("Connect") }
                }
            }
        }
        if (showTemplatesEditor) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTemplatesEditor = false },
                title = { Text("Edit Templates") },
                text = {
                    // Local editable copies
                    var rock by remember(showTemplatesEditor) { mutableStateOf(viewModel.rocknixTemplate) }
                    var mu by remember(showTemplatesEditor) { mutableStateOf(viewModel.muosTemplate) }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item { Text("Rocknix", style = MaterialTheme.typography.titleMedium) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rock.username,
                                    onValueChange = { rock = rock.copy(username = it) },
                                    label = { Text("Username") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rock.password,
                                    onValueChange = { rock = rock.copy(password = it) },
                                    label = { Text("Password") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rock.port,
                                    onValueChange = { rock = rock.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                                    label = { Text("Port") },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = rock.remoteBasePath,
                                onValueChange = { rock = rock.copy(remoteBasePath = it) },
                                label = { Text("Remote base path") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = rock.hostIp,
                                onValueChange = { rock = rock.copy(hostIp = it) },
                                label = { Text("Host IP") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var rockGuest by remember(rock.useGuest) { mutableStateOf(rock.useGuest) }
                                Button(onClick = {
                                    rockGuest = !rockGuest
                                    rock = rock.copy(useGuest = rockGuest)
                                    if (rockGuest) rock = rock.copy(password = "")
                                }) { Text(if (rockGuest) "Guest: ON" else "Guest: OFF") }
                                var rockDhcp by remember(rock.isDhcpNetwork) { mutableStateOf(rock.isDhcpNetwork) }
                                Button(onClick = {
                                    rockDhcp = !rockDhcp
                                    rock = rock.copy(isDhcpNetwork = rockDhcp)
                                }) { Text(if (rockDhcp) "DHCP" else "Manual") }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { Button(onClick = { viewModel.updateTemplate(appCtx, rock) }) { Text("Save Rocknix") } }

                        item { Spacer(Modifier.height(16.dp)) }
                        item { Text("muOS", style = MaterialTheme.typography.titleMedium) }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = mu.username,
                                    onValueChange = { mu = mu.copy(username = it) },
                                    label = { Text("Username") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mu.password,
                                    onValueChange = { mu = mu.copy(password = it) },
                                    label = { Text("Password") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = mu.port,
                                    onValueChange = { mu = mu.copy(port = it.filter { ch -> ch.isDigit() }.take(5)) },
                                    label = { Text("Port") },
                                    modifier = Modifier.weight(0.6f)
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = mu.remoteBasePath,
                                onValueChange = { mu = mu.copy(remoteBasePath = it) },
                                label = { Text("Remote base path") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            OutlinedTextField(
                                value = mu.hostIp,
                                onValueChange = { mu = mu.copy(hostIp = it) },
                                label = { Text("Host IP") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                var muGuest by remember(mu.useGuest) { mutableStateOf(mu.useGuest) }
                                Button(onClick = {
                                    muGuest = !muGuest
                                    mu = mu.copy(useGuest = muGuest)
                                    if (muGuest) mu = mu.copy(password = "")
                                }) { Text(if (muGuest) "Guest: ON" else "Guest: OFF") }
                                var muDhcp by remember(mu.isDhcpNetwork) { mutableStateOf(mu.isDhcpNetwork) }
                                Button(onClick = {
                                    muDhcp = !muDhcp
                                    mu = mu.copy(isDhcpNetwork = muDhcp)
                                }) { Text(if (muDhcp) "DHCP" else "Manual") }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                        item { Button(onClick = { viewModel.updateTemplate(appCtx, mu) }) { Text("Save muOS") } }
                    }
                },
                confirmButton = { TextButton(onClick = { showTemplatesEditor = false }) { Text("Close") } }
            )
        }
    }
}

@Composable
private fun CustomHostsScreen(viewModel: MainViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Network page title
            Text(
                text = "Network",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        
        item {
            // Host Discovery Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Host Discovery", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.scanHosts() },
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text(if (viewModel.isScanning) "Scanning..." else "Scan") 
                            }
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            Button(
                                onClick = {
                                    val ip = viewModel.selectedHost?.ip
                                    if (ip == null) {
                                        android.widget.Toast.makeText(ctx, "No host selected", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                                            val resultText = try {
                                                val port = viewModel.sshPort.toIntOrNull() ?: 22
                                                val username = if (viewModel.useGuest) viewModel.sshUsername.ifEmpty { "root" } else viewModel.sshUsername.ifEmpty { "root" }
                                                val password = if (viewModel.useGuest) "" else viewModel.sshPassword
                                                com.example.romdownloader.network.SshTester().test(HostConfig(host = ip, port = port, username = username, password = password)).getOrThrow()
                                            } catch (t: Throwable) {
                                                val err = t.message ?: "Connection failed"
                                                Log.e("RomDL", "Test connection failed", t)
                                                err
                                            }
                                            withContext(Dispatchers.Main) {
                                                val shortMsg = resultText.take(80)
                                                android.widget.Toast.makeText(ctx, shortMsg, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) { 
                                Text("Test") 
                            }
                        }
                    }
                    
                    // Manual IP entry
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        var ip by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = ip,
                            onValueChange = { ip = it },
                            label = { Text("Host IP Address") },
                            placeholder = { Text("192.168.0.159") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Button(
                            onClick = { viewModel.addHostFromIp(ip) },
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text("Add") 
                        }
                    }
                }
            }
        }
        
        item {
            // SSH Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("SSH Configuration", style = MaterialTheme.typography.titleMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.sshUsername,
                            onValueChange = { viewModel.sshUsername = it },
                            label = { Text("Username") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.sshPassword,
                            onValueChange = { viewModel.sshPassword = it },
                            label = { Text("Password") },
                            placeholder = { Text("Leave blank for guest") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.sshPort,
                            onValueChange = { viewModel.sshPort = it.filter { ch -> ch.isDigit() }.take(5) },
                            label = { Text("Port") },
                            placeholder = { Text("22") },
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    
                    // Settings toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            var guest by remember { mutableStateOf(viewModel.useGuest) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("Guest Mode", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = guest, 
                                    onCheckedChange = {
                                        guest = it
                                        viewModel.useGuest = it
                                        if (it) viewModel.sshPassword = ""
                                    }
                                )
                            }
                            var dhcp by remember { mutableStateOf(viewModel.isDhcpNetwork) }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text("DHCP Network", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = dhcp, 
                                    onCheckedChange = {
                                        dhcp = it
                                        viewModel.isDhcpNetwork = it
                                    }
                                )
                            }
                        }
                        val ctx2 = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                val ip = viewModel.selectedHost?.ip
                                val port = viewModel.sshPort
                                val user = viewModel.sshUsername
                                android.widget.Toast.makeText(ctx2, "Using $user@$ip:$port", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) { 
                            Text("Apply") 
                        }
                    }
                    
                    // Remote path
                    OutlinedTextField(
                        value = viewModel.remoteBasePath,
                        onValueChange = { viewModel.remoteBasePath = it },
                        label = { Text("Remote Base Path") },
                        placeholder = { Text("/roms or /mnt/mmc/ROMS") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        
        item {
            // Discovered Hosts Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Discovered Hosts", style = MaterialTheme.typography.titleMedium)
                    
                    if (viewModel.hosts.isEmpty()) {
                        Text(
                            "No hosts found. Use the Scan button to discover devices on your network.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        viewModel.hosts.forEach { host ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (viewModel.selectedHost == host) 
                                        com.example.romdownloader.ui.theme.Purple.copy(alpha = 0.1f)
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(host.ip, style = MaterialTheme.typography.bodyLarge)
                                    Button(
                                        onClick = { viewModel.selectHost(host) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = if (viewModel.selectedHost == host) 
                                            ButtonDefaults.buttonColors(containerColor = com.example.romdownloader.ui.theme.Purple)
                                        else 
                                            ButtonDefaults.buttonColors()
                                    ) { 
                                        Text(if (viewModel.selectedHost == host) "Selected" else "Select") 
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

object LocalAppContext {
    val current: Context
        @Composable get() = androidx.compose.ui.platform.LocalContext.current
}
