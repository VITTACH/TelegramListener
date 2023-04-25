package com.vittach.teleghost.services

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.animation.Animator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.*
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.*
import android.widget.*
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.hzy.libp7zip.P7ZipApi
import com.vittach.teleghost.R
import com.vittach.teleghost.data.guesture.TouchEvent
import com.vittach.teleghost.data.telegram.TelegramClient
import com.vittach.teleghost.data.utils.TouchAndDragListener
import com.vittach.teleghost.domain.Authentication
import com.vittach.teleghost.domain.GifToBitmapsInteractor
import com.vittach.teleghost.services.utils.fetchRedEnvelope
import com.vittach.teleghost.services.utils.toChatMessage
import com.vittach.teleghost.ui.utils.copyToClipboard
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import me.aartikov.sesame.property.PropertyObserver
import org.koin.android.ext.android.inject
import timber.log.Timber.w
import java.io.File
import java.io.FileOutputStream


class TeleGhostService : LifecycleService(), PropertyObserver {

    class BootBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            context.startService(Intent(context, TeleGhostService::class.java))
        }
    }

    private val telegramClient by inject<TelegramClient>()
    private val gifToBitmapsInteractor by inject<GifToBitmapsInteractor>()

    override val propertyObserverLifecycleOwner: LifecycleOwner get() = this

    private lateinit var windowManager: WindowManager
    private lateinit var rootParams: WindowManager.LayoutParams

    private lateinit var rootView: FrameLayout
    private lateinit var profileView: ImageView
    private lateinit var msgLayout: LinearLayout
    private lateinit var msgTextView: TextView
    private lateinit var paintView: PaintView
    private lateinit var lottieView: LottieAnimationView
    private lateinit var recBtn: Button
    private lateinit var playBtn: Button
    private lateinit var editorView: LinearLayout

    private var screenHeight = 0
    private var screenWidth = 0

    private val initFlags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCH_MODAL

    private val touchEvents = mutableMapOf<Int, List<TouchEvent>>()
    private var touchCounter = 0
    private var recStartTime = 0L

    private var isEditorModeEnabled = false
    private var isRecording = false
    private var isPlaying = false

    private val msgHandler = Handler(Looper.getMainLooper())

    companion object {
        const val ACTION_RECORD = "com.vittach.teleghost.TeleGhostService.ACTION_RECORD"
        const val ACTION_AVATAR = "com.vittach.teleghost.TeleGhostService.ACTION_AVATAR"
        const val AVATAR_PATH = "AVATAR_PATH"
        private const val KUCOIN_APP_PACKAGE = "com.kubi.kucoin"

        private val LIKE_REACTIONS = listOf("❤️", "\uD83D\uDC4D")
        private const val UPLOAD_PHOTO_PERIOD = 30 * 1000L
        private const val HIDE_MESSAGE_PERIOD = 4 * 1000L
    }

    override fun onBind(p0: Intent): IBinder? {
        super.onBind(p0)
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_AVATAR -> updateProfileImage(Uri.parse(intent.getStringExtra(AVATAR_PATH)))
            ACTION_RECORD -> {
                isEditorModeEnabled = !isEditorModeEnabled
                updateEditorMode()
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        initScreenUtils()
        initViews()
        initEventListener()
    }

    override fun onDestroy() {
        toast("Service ${this::class.java} was destroyed")
        windowManager.removeView(rootView)
        super.onDestroy()
    }

    private fun initViews() {
        rootView =
            LayoutInflater.from(this).inflate(R.layout.service_teleghost, null) as FrameLayout

        msgLayout = rootView.findViewById(R.id.msgLayout)
        msgTextView = rootView.findViewById(R.id.msgTextView)
        profileView = rootView.findViewById(R.id.profileView)

        lottieView = rootView.findViewById(R.id.lottieView)
        paintView = rootView.findViewById(R.id.paintView)

        playBtn = rootView.findViewById(R.id.playBtn)
        recBtn = rootView.findViewById(R.id.recBtn)

        editorView = rootView.findViewById(R.id.editorView)

        recBtn.setOnClickListener {
            if (!isRecording) {
                clearRecords()
                recStartTime = System.currentTimeMillis()
            }
            updateRecordState(!isRecording)
        }

        playBtn.setOnClickListener { playScenario() }

        paintView.setOnTouchListener(TouchAndDragListener(::performTouchEvent))

        DisplayMetrics().also {
            windowManager.defaultDisplay.getMetrics(it)
            paintView.init(it)
        }

        rootParams = WindowManager.LayoutParams(
            screenWidth,
            screenHeight,
            TYPE_APPLICATION_OVERLAY,
            initFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = 0
            y = 0
            gravity = Gravity.TOP or Gravity.END
        }

        windowManager.addView(rootView, rootParams)
        updateEditorMode()
    }

    private fun initEventListener() {
        telegramClient::authState bind { state ->
            when (state) {
                Authentication.UNAUTHENTICATED -> telegramClient.startAuthentication()

                Authentication.WAIT_FOR_NUMBER,
                Authentication.WAIT_FOR_CODE,
                Authentication.WAIT_FOR_PASSWORD -> {
                }

                Authentication.AUTHENTICATED -> {}

                Authentication.UNKNOWN -> {}
            }
            w(state.toString())
        }

        telegramClient::message bind { message ->
            message?.toChatMessage()?.let {
                val text = it.message?.let { msg ->
                    val (hasEnvelope, envelopeCode) = msg.fetchRedEnvelope()
                    when {
                        LIKE_REACTIONS.any { it in msg } -> startAnimation()
                        hasEnvelope -> startKucoin(envelopeCode!!)
                    }
                    msg
                }

                msgHandler.removeCallbacksAndMessages(null)
                msgHandler.postDelayed({ msgLayout.isVisible = false }, HIDE_MESSAGE_PERIOD)
                msgLayout.post {
                    msgLayout.isVisible = true
                    msgTextView.text = text
                }

                it.senderId?.let {
                    telegramClient.getUserProfilePhotos(it) { photos ->
                        val preview = photos.firstOrNull()?.minithumbnail
                        profileView.post {
                            profileView.isVisible = preview != null
                            Glide.with(this)
                                .load(preview?.data)
                                .circleCrop()
                                .into(profileView)
                        }
                    }
                }

                it.sticker?.let { sticker ->
                    CoroutineScope(Dispatchers.IO).launch {
                        telegramClient.downloadableFile(sticker).collect {
                            unArchiveTgSticker(it)
                        }
                    }
                }
            }
        }
    }

    private suspend fun unArchiveTgSticker(path: String?) = try {
        val archive = File(path)
        val destination = archive.path.replace(archive.name, "")
        P7ZipApi.executeCommand("7z x $path -o$destination -aoa")
        archive.delete()
        withContext(Dispatchers.Main) {
            startAnimation("$destination/${archive.nameWithoutExtension}")
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            toast(e.toString())
        }
    }

    private fun updateEditorMode() {
        editorView.isVisible = isEditorModeEnabled
        updateLayoutParams(isHandleTouch = isEditorModeEnabled)
    }

    private fun updateProfileImage(uri: Uri) = CoroutineScope(Dispatchers.IO).launch {
        val context = this@TeleGhostService
        val bitmaps = gifToBitmapsInteractor.execute(context, uri)

        bitmaps.forEachIndexed { index, bitmap ->
            val file = File(ContextWrapper(context).getDir("Images", MODE_PRIVATE), "$index.jpg")
            FileOutputStream(file).apply {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
                flush()
                close()
            }
            telegramClient.updateProfilePhoto(file.absolutePath)

            delay(UPLOAD_PHOTO_PERIOD)
            file.delete()
        }
    }

    private fun startKucoin(envelopeCode: String) {
        wakeUpScreen()
        copyToClipboard(envelopeCode)
        packageManager.getLaunchIntentForPackage(KUCOIN_APP_PACKAGE)?.let {
            startActivity(it)
            playScenario(true)
        }
    }

    private fun startAnimation(animationPath: String? = null) = with(lottieView) {
        wakeUpScreen()

        var animationFile: File? = null
        animationPath?.let {
            animationFile = File(animationPath)
            setAnimationFromJson(animationFile!!.readText())
        } ?: setAnimation(R.raw.flying_likes_classic)

        isVisible = true
        playAnimation()

        addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {
            }

            override fun onAnimationEnd(animation: Animator?) {
                isVisible = false
                animationFile?.delete()
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
            }
        })
    }

    private fun initScreenUtils() {
        val display = windowManager.defaultDisplay
        var statusBarHeight = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        screenWidth = display.width
        screenHeight = display.height - statusBarHeight
    }

    private fun wakeUpScreen() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            FULL_WAKE_LOCK or ACQUIRE_CAUSES_WAKEUP or ON_AFTER_RELEASE, "app::WakeLock"
        )
        wakeLock.acquire()
    }

    private fun updateRecordState(isRecording: Boolean) {
        this.isRecording = isRecording
        recBtn.text = if (isRecording) "Stop" else "Rec"
    }

    private fun clearRecords() {
        touchEvents.clear()
        touchCounter = 0
    }

    private fun updateLayoutParams(isHandleTouch: Boolean = true) {
        rootParams.flags = if (isHandleTouch && isEditorModeEnabled) {
            initFlags
        } else {
            initFlags or FLAG_NOT_TOUCHABLE
        }

        windowManager.updateViewLayout(rootView, rootParams)
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun playScenario(isPressHomeAtEnd: Boolean = false) {
        if (touchEvents.entries.isEmpty()) return
        isPlaying = true
        updateRecordState(isRecording = false)
        updateLayoutParams(isHandleTouch = false)
        CoroutineScope(Dispatchers.Main).launch {
            touchEvents.entries.forEach { (index, events) ->
                val previousTime = touchEvents[index - 1]?.last()?.touchDownTime ?: recStartTime
                delay((events[0].touchDownTime - previousTime))

                autoTouchService?.startEvents(events) {
                    if (index == touchCounter - 1) {
                        updateLayoutParams(isHandleTouch = true)
                        isPlaying = false
                        if (isPressHomeAtEnd) {
                            autoTouchService?.performGlobalAction(GLOBAL_ACTION_HOME)
                        }
                    }
                }
            }
        }
    }

    private fun performTouchEvent(touchEvent: List<TouchEvent>) {
        if (isPlaying) return
        if (isRecording) {
            touchEvents[touchCounter++] = ArrayList(touchEvent)
        }
        updateLayoutParams(isHandleTouch = false)
        rootView.post {
            autoTouchService?.startEvents(touchEvent) {
                updateLayoutParams(isHandleTouch = true)
            }
        }
    }
}