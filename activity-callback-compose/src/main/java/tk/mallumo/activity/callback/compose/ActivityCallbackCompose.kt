package tk.mallumo.activity.callback.compose

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * ## Tool for handling android activity response (intent or permission)
 * ### Example:
 * ```@kotlin
 * CompositionLocalProvider(LocalActivityResult provides ActivityCallback.remember()) {
 *      //...
 * }
 * ```
 * @see ActivityCallback.intent
 * @see ActivityCallback.permission
 */
@Composable
fun ActivityCallback.Companion.remember(): ActivityCallback {
    val ctx = LocalContext.current
    return androidx.compose.runtime.remember(ctx) {
        if (ctx is ComponentActivity) get(ctx)
        else defaultPreview
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
            get() = object : ActivityCallback.PermissionContract {
                override fun requestSelf(vararg permission: String, response: ActivityCallback.PermissionScope.() -> Unit) {
                    Log.d("permission.requestSelf", permission.toString())
                }

                override fun checkSelf(vararg permissions: String): Boolean {
                    Log.d("permission.checkSelf", permission.toString())
                    return true
                }

            }

    }