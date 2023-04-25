package com.vittach.teleghost.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.text.*
import android.text.Annotation
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.doOnDetach
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import com.vittach.teleghost.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import me.aartikov.sesame.loading.simple.Loading
import me.aartikov.sesame.localizedstring.LocalizedString
import me.aartikov.sesame.localizedstring.localizedText

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

@ColorInt
fun Resources.color(@ColorRes colorId: Int) = ResourcesCompat.getColor(this, colorId, null)

fun View.visible(visible: Boolean, useGone: Boolean = true) {
    this.visibility = if (visible) View.VISIBLE else if (useGone) View.GONE else View.INVISIBLE
}

fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.applyTextWatcher(
    inputFilterPattern: Regex? = null,
    listenDelKey: Boolean = true,
    formatter: (String) -> String,
) {
    val (oldFilterPattern, oldListener) = tag as? Pair<Regex?, TextWatcher> ?: null to null
    if (inputFilterPattern != null && oldFilterPattern == inputFilterPattern) return

    removeTextChangedListener(oldListener)

    val listener = object : TextWatcher {
        private var previousText = ""

        override fun afterTextChanged(s: Editable) {
            if (listenDelKey && s.length < previousText.length) {
                previousText = s.toString()
                return
            }

            removeTextChangedListener(this)

            val previousSelection = selectionStart

            if (inputFilterPattern == null || inputFilterPattern.matches(s)) {
                val formattedString = formatter(s.toString())
                previousText = formattedString
            }

            val selection = if (previousSelection == s.toString().length) {
                previousText.length
            } else {
                previousSelection
            }

            setText(previousText)
            try {
                setSelection(selection)
            } catch (e: Exception) {
                // nothing
            }

            addTextChangedListener(this)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit
    }

    tag = inputFilterPattern to listener

    addTextChangedListener(listener)
}

@ExperimentalCoroutinesApi
@CheckResult
fun EditText.textChanges(): Flow<CharSequence?> {
    return callbackFlow<CharSequence?> {
        val listener = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                offer(s)
            }
        }
        addTextChangedListener(listener)
        awaitClose { removeTextChangedListener(listener) }
    }.onStart { emit(text) }
}

inline fun EditText.doOnDone(crossinline action: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            action.invoke()
            return@setOnEditorActionListener true
        }
        false
    }
}

fun dpToPx(dp: Int) = (dp * Resources.getSystem().displayMetrics.density).toInt()

fun Int.dp() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun TextView.setDrawableStart(@DrawableRes drawableId: Int) {
    this.setCompoundDrawablesWithIntrinsicBounds(drawableId, 0, 0, 0)
}

fun TextView.setDrawableStart(drawable: Drawable) {
    this.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
}

fun TextView.setLocalizedText(localizedString: LocalizedString?) {
    localizedText = localizedString
}

fun Context.copyToClipboard(text: String) {
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    val clip = ClipData.newPlainText("label", text)
    clipboard!!.setPrimaryClip(clip)
}

fun Context.getClipboardContent(): String? {
    val clipboard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
    return clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
}

inline fun View.doOnClick(crossinline action: () -> Unit) {
    this.setOnClickListener { action() }
}

fun View.setVisible(visible: Boolean) {
    isVisible = visible
}

inline fun MaterialToolbar.doOnNavigationClick(crossinline action: () -> Unit) {
    this.setNavigationOnClickListener { action() }
}

var MaterialToolbar.localizedTitle: LocalizedString?
    get() = LocalizedString.raw(title)
    set(value) {
        title = value?.resolve(context)
    }

inline fun TextView.doOnTextChanged(crossinline action: (text: String) -> Unit) {
    doOnTextChanged { text, _, _, _ ->
        action(text?.toString() ?: "")
    }
}

private const val SPAN_KEY_COLOR = "color"
private const val SPAN_VALUE_ACCENT = "accent"

fun CharSequence.toColoredSpannable(context: Context): CharSequence {
    val spannable = SpannableString(this)
    val color = MaterialColors.getColor(context, R.attr.colorAccent, 0)

    spannable.getSpans(0, spannable.length, Annotation::class.java)
        .filter { it.key == SPAN_KEY_COLOR && it.value == SPAN_VALUE_ACCENT }
        .forEach {
            spannable.setSpan(
                ForegroundColorSpan(color),
                spannable.getSpanStart(it),
                spannable.getSpanEnd(it),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    return spannable
}

private const val SPAN_KEY_LINK = "link"

fun TextView.setTextWithLinksAndArg(
    @StringRes res: Int,
    arg: String,
    listener: (value: String) -> Unit,
) {
    movementMethod = LinkMovementMethod.getInstance()
    val textWithArgs = context.getString(res, arg)
    val styledSpannable = SpannableString(context.getText(res))

    val spannableWithArgs = SpannableString(textWithArgs)
    text = context.setSpans(styledSpannable, spannableWithArgs, listener)
}

fun Context.setSpans(
    initSpannable: SpannableString,
    resSpannable: SpannableString,
    listener: (value: String) -> Unit,
): SpannableString {
    val color = MaterialColors.getColor(this, R.attr.colorAccent, 0)

    initSpannable.getSpans(0, initSpannable.length, Annotation::class.java)
        .filter { it.key == SPAN_KEY_LINK }
        .forEach {
            resSpannable.setSpan(
                ForegroundColorSpan(color),
                initSpannable.getSpanStart(it),
                initSpannable.getSpanEnd(it),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            resSpannable.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        listener(it.value)
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = false
                    }
                },
                initSpannable.getSpanStart(it),
                initSpannable.getSpanEnd(it),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    return resSpannable
}

fun TextView.setTextWithLinks(
    @StringRes res: Int,
    listener: (value: String) -> Unit
) {
    movementMethod = LinkMovementMethod.getInstance()
    text = context.getText(res).toClickableSpannable(context, listener)
}

fun CharSequence.toClickableSpannable(
    context: Context,
    listener: (value: String) -> Unit,
): Spannable {
    val spannable = SpannableString(this)
    return context.setSpans(spannable, spannable, listener)
}

fun TabLayout.setHorizontalPadding(@DimenRes paddingRes: Int) {
    val padding = resources.getDimensionPixelSize(paddingRes)
    this.getChildAt(0).setPadding(padding, 0, padding, 0)
}

fun View.hideKeyboard() {
    clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

var Fragment.systemUiVisibility
    get() = this.requireActivity().window.decorView.systemUiVisibility
    set(value) {
        this.requireActivity().window.decorView.systemUiVisibility = value
    }

fun TextInputLayout.clickOnEndIcon() {
    val endIconContentDescription = this.endIconContentDescription
    val foundViews: ArrayList<View> = ArrayList()
    this.findViewsWithText(
        foundViews,
        endIconContentDescription,
        View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION
    )
    foundViews.firstOrNull()?.performClick()
}

fun TextInputLayout.clearStartPaddingIndicatorView() {
    isErrorEnabled = true
    val indicatorTextView: TextView? = this.findViewById(
        com.google.android.material.R.id.textinput_error
    )
    val container = indicatorTextView
        ?.parent
        ?.parent as? View
    container?.setPadding(0, container.paddingTop, container.paddingRight, container.paddingBottom)
    isErrorEnabled = false
}

fun Fragment.onKeyboardStateChange(callback: (isShown: Boolean) -> Unit) {
    val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val stableBottomInset = view?.rootWindowInsets?.stableInsetBottom
        val bottomInset = view?.rootWindowInsets?.systemWindowInsetBottom
        if (bottomInset != null && stableBottomInset != null) {
            callback(bottomInset > stableBottomInset)
        }
    }
    view?.apply {
        viewTreeObserver?.addOnGlobalLayoutListener(globalLayoutListener)
        doOnDetach {
            it.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
        }
    }
}

fun RecyclerView.doOnScrollToEnd(difference: Int = 10, listener: () -> Unit) {
    addOnScrollListener(object : RecyclerView.OnScrollListener() {

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val totalItemCount = recyclerView.layoutManager!!.itemCount
            val lastVisibleItem =
                (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            if (dy > 0 && totalItemCount - lastVisibleItem < difference) {
                recyclerView.post { listener() }
            }
        }
    })
}

// TODO: think about error transforming
fun <T1, T2, R> Loading.State<T1>.combine(
    state2: Loading.State<T2>,
    transform: (t1: T1, t2: T2) -> R
): Loading.State<R> {
    return when {
        this is Loading.State.Empty || state2 is Loading.State.Empty -> Loading.State.Empty
        this is Loading.State.Loading || state2 is Loading.State.Loading -> Loading.State.Loading
        this is Loading.State.Error -> Loading.State.Error(this.throwable)
        state2 is Loading.State.Error -> Loading.State.Error(state2.throwable)
        this is Loading.State.Data && state2 is Loading.State.Data -> Loading.State.Data(
            transform(this.data, state2.data),
            this.refreshing || state2.refreshing
        )
        else -> Loading.State.Loading
    }
}