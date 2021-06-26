package tk.mallumo.activity.callback.compose

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import tk.mallumo.activity.callback.ActivityCallback
import kotlin.reflect.KClass

/**
 * ## Tool for handling android activity response (intent or permission)
 * ### Example:
 * ```@kotlin
 * CompositionLocalProvider(LocalActivityResult provides ActivityResult.remember()) {
 *      //...
 * }
 * ```
 * @see ActivityCallback
 * */
@Suppress("unused")
val LocalActivityCallback = staticCompositionLocalOf<ActivityCallback> { error("ActivityCallback is not implemented by CompositionLocalProvider") }

@Composable
fun ActivityCallback.Companion.remember(): ActivityCallback {
    val ctx = LocalContext.current
    return remember(ctx) {
        if (ctx is ComponentActivity) get(ctx)
        else defaultPreview
    }
}

@JvmInline
value class ReferenceCallback(private val callback: () -> Unit) {
    @Suppress("unused")
    fun removeOnBackPressCallback() = callback()
}

@SuppressLint("ComposableNaming")
@Composable
fun ActivityCallback.OnBackPressContract.rememberOnBackPress(body: () -> Boolean): ReferenceCallback {

    val reference = remember {
        mutableStateOf<ActivityCallback.OnBackPressContract.Reference?>(null)
    }

    DisposableEffect(key1 = Unit) {
        reference.value = register(body)

        onDispose {
            reference.value?.release()
        }
    }
    return remember {
        ReferenceCallback {
            reference.value?.release()
        }
    }
}

private val defaultPreview
    get() = object : ActivityCallback {

        override val intent: ActivityCallback.IntentContract
            get() = object : ActivityCallback.IntentContract() {

                override fun <T : Activity> activity(activityClass: KClass<T>, launchOpt: ActivityOptionsCompat?, response: (resultCode: Int, data: Intent?) -> Unit) {
                    Log.d("intent.activity", "activity intent: ${activityClass::class.simpleName}")
                }

                override fun activity(intent: Intent, launchOpt: ActivityOptionsCompat?, response: (resultCode: Int, data: Intent?) -> Unit) {
                    Log.d("intent.activity", "intent call")
                }

            }

        override val permission: ActivityCallback.PermissionContract
            get() = object : ActivityCallback.PermissionContract {}

        override val onBackPress: ActivityCallback.OnBackPressContract
            get() = object : ActivityCallback.OnBackPressContract {}
    }