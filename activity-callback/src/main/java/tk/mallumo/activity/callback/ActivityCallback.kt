package tk.mallumo.activity.callback

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass


@Suppress("unused")
val ComponentActivity.callback: ActivityCallback
    get() = ActivityCallback.get(this)

interface ActivityCallback {

    companion object {
        @JvmStatic
        fun get(activity: ComponentActivity): ActivityCallback = ImplActivityCallback { activity }
    }

    val intent: IntentContract

    val permission: PermissionContract

    val onBackPress: OnBackPressContract

    abstract class IntentContract {
        inline fun <reified T : Activity> activity(launchOpt: ActivityOptionsCompat? = null, noinline response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> }) {
            activity(T::class, launchOpt, response)
        }

        abstract fun <T : Activity> activity(activityClass: KClass<T>, launchOpt: ActivityOptionsCompat? = null, response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> })
        abstract fun activity(intent: Intent, launchOpt: ActivityOptionsCompat? = null, response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> })
    }

    interface PermissionContract {
        fun requestSelf(vararg permission: String, response: Scope.() -> Unit) {
            Log.d("permission.requestSelf", permission.toString())
        }

        fun checkSelf(vararg permissions: String): Boolean {
            Log.d("permission.checkSelf", permissions.toString())
            return true
        }

        class Scope(val permission: List<String>) {

            internal var grantedImpl: (Map<String, Boolean>) -> Unit = {}
            internal var deniedImpl: (Map<String, Boolean>) -> Unit = {}

            /**
             * Function is "called" when all permissions are granted
             */
            @Suppress("unused")
            fun granted(body: (Map<String, Boolean>) -> Unit) {
                grantedImpl = body
            }

            /**
             * Function is "called" when one or more permissions are denied
             */
            @Suppress("unused")
            fun denied(body: (Map<String, Boolean>) -> Unit) {
                deniedImpl = body
            }
        }
    }

    interface OnBackPressContract {

        fun register(body: () -> Boolean): Reference {
            Log.d("onBackPress", "register")
            return Reference(
                onNotConsumed = {
                    Log.d("onBackPress", "register.onNotConsumed")
                },
                body = {
                    Log.d("onBackPress", "register.invoke")
                    false
                }
            )
        }

        fun virtualInvoke() {
            Log.d("onBackPress", "virtualInvoke")
        }

        class Reference internal constructor(
            private val onNotConsumed: () -> Unit,
            private val body: () -> Boolean
        ) {

            internal val callback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!body()) {
                        isEnabled = false
                        onNotConsumed()
                        isEnabled = true
                    }
                }
            }

            fun release() {
                callback.isEnabled = false
                callback.remove()
            }
        }
    }
}

private class ImplActivityCallback(private val activityRef: () -> ComponentActivity) : ActivityCallback {

    private val implActivityResult get() = ActivityResultContracts.StartActivityForResult()
    private val implPermissionResult get() = ActivityResultContracts.RequestMultiplePermissions()

    companion object {
        private val requestCodeGenerator by lazy {
            AtomicInteger(1)
        }
    }

    override val intent: ActivityCallback.IntentContract
        get() = object : ActivityCallback.IntentContract() {

            override fun <T : Activity> activity(activityClass: KClass<T>, launchOpt: ActivityOptionsCompat?, response: (resultCode: Int, data: Intent?) -> Unit) {
                activity(Intent(activityRef(), activityClass.java), launchOpt, response)
            }

            override fun activity(intent: Intent, launchOpt: ActivityOptionsCompat?, response: (resultCode: Int, data: Intent?) -> Unit) {

                var request: ActivityResultLauncher<Intent>? = null
                val key = "ImplActivityCallback-${requestCodeGenerator.getAndIncrement()}"

                request = activityRef().activityResultRegistry
                    .register(key, implActivityResult) {
                        request?.unregister()
                        try {
                            response(it.resultCode, it.data)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }

                request.launch(
                    intent,
                    launchOpt ?: ActivityOptionsCompat.makeCustomAnimation(
                        activityRef(),
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                    )
                )
            }
        }

    override val permission: ActivityCallback.PermissionContract
        get() = object : ActivityCallback.PermissionContract {
            override fun requestSelf(vararg permission: String, response: ActivityCallback.PermissionContract.Scope.() -> Unit) {

                val request = ActivityCallback.PermissionContract.Scope(permission.toList()).also {
                    response(it)
                }

                if (checkSelf(*permission)) {
                    request.grantedImpl(
                        permission.map { Pair(it, true) }
                            .toHashMap()
                    )
                } else {

                    var requester: ActivityResultLauncher<Array<String>>? = null
                    val key = "ActivityResultHolder-${requestCodeGenerator.getAndIncrement()}"

                    requester = activityRef().activityResultRegistry
                        .register(key, implPermissionResult) { permissionResponse ->
                            requester?.unregister()
                            try {
                                if (permissionResponse.values.all { it }) {
                                    request.grantedImpl(permissionResponse)
                                } else {
                                    request.deniedImpl(permissionResponse)
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    requester.launch(request.permission.toTypedArray())
                }
            }

            override fun checkSelf(vararg permissions: String): Boolean {
                return permissions.all { ContextCompat.checkSelfPermission(activityRef(), it) == PackageManager.PERMISSION_GRANTED }
            }
        }

    override val onBackPress: ActivityCallback.OnBackPressContract
        get() = object : ActivityCallback.OnBackPressContract {
            override fun register(body: () -> Boolean): ActivityCallback.OnBackPressContract.Reference =
                activityRef().onBackPressedDispatcher.let { dispatcher ->
                    ActivityCallback.OnBackPressContract.Reference(
                        onNotConsumed = { dispatcher.onBackPressed() },
                        body = body
                    ).apply {
                        dispatcher.addCallback(callback)
                    }
                }

            override fun virtualInvoke() {
                activityRef().onBackPressedDispatcher.onBackPressed()
            }
        }
}

private fun <A, T> List<Pair<A, T>>.toHashMap() = hashMapOf<A, T>().apply {
    putAll(this@toHashMap)
}