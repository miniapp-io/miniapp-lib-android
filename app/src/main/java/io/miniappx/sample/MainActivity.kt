package io.miniappx.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import io.miniapp.core.PLUGIN_OPEN_PLATFORM
import io.miniapp.core.PluginsManager
import io.miniapp.core.openplatform.OpenPlatformPlugin
import io.miniapp.core.openplatform.miniapp.DAppLaunchWParameters
import io.miniapp.core.openplatform.miniapp.IMiniApp
import io.miniapp.core.openplatform.miniapp.WebAppLaunchWithDialogParameters
import io.miniapp.core.openplatform.miniapp.WebAppPreloadParameters
import io.miniappx.sample.phantom.PhantomProvider
import io.miniappx.sample.ui.theme.MiniappandroidTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

val openPlatformPlugin = PluginsManager.getPlugin<OpenPlatformPlugin>(PLUGIN_OPEN_PLATFORM)!!
val miniAppService = openPlatformPlugin.getMiniAppService()

class MainActivity : AppCompatActivity() {

    private var miniApp: IMiniApp? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.also {
            PhantomProvider.onWalletIntent(it)
        }
    }

    private fun preloadApps(owner: LifecycleOwner, context: Context) {
        listOf("10").forEach {
            val config = WebAppPreloadParameters.Builder()
                .owner(owner)
                .context(context)
                .miniAppId(it)
                .build()

            MainScope().launch {
                miniAppService.preload( config = config)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preloadApps(this, this)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(miniApp==null || true==miniApp?.requestDismiss()) {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {

            val context = LocalContext.current
            val lifecycleOwner = this

            MiniappandroidTheme {
                // A surface container using the 'background' color from the theme
                ProvideWindowInsets {
                    val systemUiController = rememberSystemUiController()
                    SideEffect {
                        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
                    }
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            TopAppBar(
                                title = {
                                    Text(text = "MiniApp Demo")
                                },
                                contentPadding = rememberInsetsPaddingValues(
                                    insets = LocalWindowInsets.current.statusBars)
                            )
                            LaunchTgButton(lifecycleOwner) {
                                miniApp = it
                            }

                            Spacer(modifier = Modifier.height(50.dp))
                            DialogMiniAppButton(lifecycleOwner) {
                                miniApp = it
                            }
                            ChatButton(context)
                            AppMasterButton(context,lifecycleOwner)
                            MarketPlaceButton(context = context, lifecycleOwner = lifecycleOwner )
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun DialogMiniAppButton(lifecycleOwner: LifecycleOwner, complete: (IMiniApp?) -> Unit){

    // Dialog state
    val showDialog = remember { mutableStateOf(false) }

    Button(modifier = Modifier.width(250.dp), onClick = {
        showDialog.value = true
    }) {
        Text(text = "Launch With Dialog")
    }

    // Dialog content
    if (showDialog.value) {

        val context = LocalContext.current

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            //.startParam(uri.query)
            //.url(gameUrl)
            //.autoExpand(false)
            //.startParam("roomId=!EpPeSoGdAiLDqJSWfm:server.baidu.com")
            .botName("mini")
            .miniAppName("mini")
            .params(mapOf("viewStyle" to "modal"))
            .isLocal(true)
            .onDismissListener {
                showDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config).also(complete)
        }
    }
}

@Composable
fun ChatButton(context:Context) {
    Button(modifier = Modifier.width(250.dp), onClick = {
        val intent = Intent(context, ChatActivity::class.java)
        context.startActivity(intent)
    }) {
        Text(text = "Embed Launch In Chat")
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun LaunchTgButton(lifecycleOwner: LifecycleOwner, complete: (IMiniApp?) -> Unit){

    // Dialog state
    val showDialog = remember { mutableStateOf(false) }
    val showDAppDialog = remember { mutableStateOf(false) }

    //var textInput by remember { mutableStateOf("https://tv.cctv.com/live/cctv5plus/m/") }
    //var textInput by remember { mutableStateOf("https://game1.catizen.ai/game/tonkeeper/10215317") }
    var textInput by remember { mutableStateOf("https://www.magiceden.io") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter App Launch URL", style = TextStyle(color = Color.Gray)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .height(200.dp)
        )
        Button(
            modifier = Modifier.width(250.dp),
            onClick = {
                showDialog.value = true
            }
        ) {
            Text(text = "Launch MiniApp With Url")
        }
        Button(
            modifier = Modifier.width(250.dp),
            onClick = {
                showDAppDialog.value = true
            }
        ) {
            Text(text = "Launch DApp with url")
        }
    }

    // dApp
    if (showDAppDialog.value) {

        val context = LocalContext.current

        val tgUrl = textInput

        if (!tgUrl.startsWith("https://")) {
            Toast.makeText(context,"Please Enter DApp Launch URL", Toast.LENGTH_SHORT).show()
            showDAppDialog.value = false
            return
        }

        val config = DAppLaunchWParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .url(tgUrl)
            .onDismissListener {
                showDAppDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)?.also(complete) ?: run {
                showDAppDialog.value = false
            }
        }
    }


    // mini app dialog content
    if (showDialog.value) {

        val context = LocalContext.current

        val tgUrl = textInput

        if (!tgUrl.startsWith("https://")) {
            Toast.makeText(context,"Please Enter Telegram WebApp Launch URL", Toast.LENGTH_SHORT).show()
            showDialog.value = false
            return
        }

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            //.startParam("roomId=!EpPeSoGdAiLDqJSWfm:server.mtsocialdao.com")
            //.miniAppId("9")
            //.startParam("roomId=!EpPeSoGdAiLDqJSWfm:server.mtsocaildao.com")
            //.miniAppId("2lkLBhdIamQCa8Tibp73j0jk8gT")
            .context(context)
            .url(tgUrl)
            .isLaunchUrl(true)
            .onDismissListener {
                showDialog.value = false
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)?.also(complete) ?: run {
                showDialog.value = false
            }
        }
    }
}
@Composable
fun AppMasterButton(context:Context,lifecycleOwner: LifecycleOwner) {
    Button(modifier = Modifier.width(250.dp), onClick = {

        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .miniAppId("2lv8dp7JjF2AU0iEk2rMYUaySjU")
            //.miniAppId("338uovSvAvMAluyhazoolcZ9RDJ")
            //.miniAppId("33rz2o14iQorVCMYfMzM3uMu0pR") ld test
            .onDismissListener {
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)
        }
    }) {
        Text(text = "AppMaster")
    }
}

@Composable
fun MarketPlaceButton(context:Context,lifecycleOwner: LifecycleOwner) {
    Button(modifier = Modifier.width(250.dp), onClick = {
        val config = WebAppLaunchWithDialogParameters.Builder()
            .owner(lifecycleOwner)
            .context(context)
            .miniAppId("10")
            //.url("https://miniappx.io/apps/10?startapp=xxxx&spaceId=12345&appId=10")
            .params(mapOf("spaceId" to "11", "roomId" to "333"))
            //.id("48033")
            .onDismissListener {
            }
            .build()

        lifecycleOwner.lifecycleScope.launch {
            miniAppService.launch(config)
        }
    }) {
        Text(text = "MarketPlace")
    }
}
