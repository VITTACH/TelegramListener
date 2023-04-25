package com.vittach.teleghost.data.telegram

import android.content.Context
import android.os.Build
import com.vittach.teleghost.domain.Authentication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.aartikov.sesame.property.PropertyHost
import me.aartikov.sesame.property.state
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.TdApi.*
import timber.log.Timber.w
import java.util.*
import kotlin.math.abs

class TelegramClient(context: Context) : Client.ResultHandler, PropertyHost {

    companion object {
        private const val HOUR_PERIOD = 3600 * 1000
        private const val MINUTE_PERIOD = 60 * 1000
    }

    override val propertyHostScope get() = CoroutineScope(Dispatchers.IO)

    private var client: Client? = null

    var authState by state(Authentication.UNKNOWN)

    var message by state<Message?>(null)

    private var me: User? = null

    private val tdLibParameters = TdlibParameters().apply {
        useMessageDatabase = false
        useSecretChats = true
        systemLanguageCode = Locale.getDefault().language
        databaseDirectory = context.filesDir.absolutePath
        deviceModel = Build.MODEL
        systemVersion = Build.VERSION.RELEASE
        applicationVersion = "0.1"
        enableStorageOptimizer = true
    }

    override fun onResult(data: Object) {
        w("TelegramClient: handle message result: ${data::class.java.simpleName}")

        when (data.constructor) {
            UpdateAuthorizationState.CONSTRUCTOR -> {
                onAuthorizationStateUpdated((data as UpdateAuthorizationState).authorizationState)
            }

            UpdateOption.CONSTRUCTOR -> {
            }

            UpdateNewMessage.CONSTRUCTOR -> {
                message = (data as UpdateNewMessage).message
            }

            UpdateChatLastMessage.CONSTRUCTOR -> {
                val newMessage = (data as UpdateChatLastMessage).lastMessage ?: return
                if (abs(newMessage.date * 1000L - System.currentTimeMillis()) < MINUTE_PERIOD) {
                    message = newMessage
                }
            }

            UpdateUser.CONSTRUCTOR -> {
                me = (data as UpdateUser).user
            }

            else -> w("Unhandled onResult call with result: $data.")
        }
    }

    fun setApiIdAndHash(id: Int, hash: String) = safeLaunch {
        tdLibParameters.apiId = id
        tdLibParameters.apiHash = hash

        client = Client.create(this, null, null).apply {
            send(SetLogVerbosityLevel(1), this@TelegramClient)
            send(GetAuthorizationState(), this@TelegramClient)
        }
    }

    fun onClose() {
        client?.close()
    }

    fun startAuthentication() {
        if (authState != Authentication.UNAUTHENTICATED) {
            throw IllegalStateException("Start authentication called but client already authenticated. State: ${authState}.")
        }

        safeLaunch {
            client?.send(SetTdlibParameters(tdLibParameters)) {
                w("TelegramClient: set parameters result: $it")
            }
        }
    }

    fun setPhoneNumber(phoneNumber: String) {
        val settings = PhoneNumberAuthenticationSettings(
            false, // Allow Flash call
            false, // Allow Missed call
            true,  // Is current phone number
            false, // Allow SMS retriever
            emptyArray() // Auth tokens
        )
        client?.send(SetAuthenticationPhoneNumber(phoneNumber, settings)) {
            w("TelegramClient: phoneNumber result: $it")
        }
    }

    fun setAuthCode(code: String) = safeLaunch {
        client?.send(CheckAuthenticationCode(code)) {
            w("TelegramClient: insert code result: $it")
        }
    }

    fun logout() = safeLaunch {
        client?.send(LogOut()) {
            w("TelegramClient: logout result $it")
        }
    }

    fun updateProfilePhoto(photoPath: String) = safeLaunch {
        client?.send(SetProfilePhoto(InputChatPhotoStatic(InputFileLocal(photoPath)))) {
            w("TelegramClient: update profile photo result $it")
            getUserProfilePhotos { photos ->
                photos.drop(1).forEach {
                    if (System.currentTimeMillis() - it.addedDate * 1000 < HOUR_PERIOD) {
                        deleteProfilePhoto(it.id)
                    }
                }
            }
        }
    }

    fun deleteProfilePhoto(profilePhotoId: Long) = safeLaunch {
        client?.send(DeleteProfilePhoto(profilePhotoId)) {
            w("TelegramClient: delete profile photo result $it")
        }
    }

    fun getUserProfilePhotos(
        userId: Long? = null,
        callback: (Array<ChatPhoto>) -> Unit
    ) = safeLaunch {
        client?.send(GetUserProfilePhotos(userId ?: me!!.id, 0, 100)) { data ->
            w("TelegramClient: get profile photos result $data")
            (data as? ChatPhotos)?.photos?.let {
                callback(it)
            }
        }
    }

    fun downloadableFile(file: File): Flow<String?> =
        file.takeIf {
            it.local?.isDownloadingCompleted == false
        }?.id?.let { fileId ->
            downloadFile(fileId).map {
                (it ?: file).local?.path
            }
        } ?: flowOf(file.local?.path)

    fun downloadFile(fileId: Int): Flow<File?> = callbackFlow {
        client?.send(DownloadFile(fileId, 1, 0, 0, true)) {
            when (it.constructor) {
                Ok.CONSTRUCTOR -> offer(null)
                File.CONSTRUCTOR -> offer(it as File)
                else -> cancel("", Exception(""))
            }
        }
        awaitClose()
    }

    private fun safeLaunch(job: () -> Unit) = propertyHostScope.launch { job() }

    private fun onAuthorizationStateUpdated(state: AuthorizationState) = when (state.constructor) {
        AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateWaitTdlibParameters -> state = UNAUTHENTICATED")
            authState = Authentication.UNAUTHENTICATED
        }
        AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateWaitEncryptionKey")
            client?.send(CheckDatabaseEncryptionKey()) {
                when (it.constructor) {
                    Ok.CONSTRUCTOR -> w("CheckDatabaseEncryptionKey: OK")
                    Error.CONSTRUCTOR -> w("CheckDatabaseEncryptionKey: Error")
                }
            }
        }
        AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateWaitPhoneNumber -> state = WAIT_FOR_NUMBER")
            authState = Authentication.WAIT_FOR_NUMBER
        }
        AuthorizationStateWaitCode.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateWaitCode -> state = WAIT_FOR_CODE")
            authState = Authentication.WAIT_FOR_CODE
        }
        AuthorizationStateWaitPassword.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateWaitPassword")
            authState = Authentication.WAIT_FOR_PASSWORD
        }
        AuthorizationStateReady.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateReady -> state = AUTHENTICATED")
            authState = Authentication.AUTHENTICATED
        }
        AuthorizationStateLoggingOut.CONSTRUCTOR -> {
            w("onResult: AuthorizationStateLoggingOut")
            authState = Authentication.UNAUTHENTICATED
        }
        AuthorizationStateClosing.CONSTRUCTOR -> w("onResult: AuthorizationStateClosing")

        AuthorizationStateClosed.CONSTRUCTOR -> w("onResult: AuthorizationStateClosed")

        else -> w("Unhandled authorizationState with data: $state")
    }
}