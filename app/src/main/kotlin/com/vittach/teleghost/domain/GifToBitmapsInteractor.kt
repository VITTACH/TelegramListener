package com.vittach.teleghost.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by VITTACH on 13.06.2022.
 */
class GifToBitmapsInteractor {

    suspend fun execute(context: Context, gifUri: Uri) = suspendCoroutine<List<Bitmap>> {
        Glide.with(context)
            .asGif()
            .load(gifUri)
            .into(callBack { bitmaps -> it.resume(bitmaps) })
    }

    private fun callBack(
        response: (List<Bitmap>) -> Unit
    ) = object : SimpleTarget<GifDrawable>() {
        override fun onResourceReady(
            drawable: GifDrawable,
            transition: Transition<in GifDrawable>?
        ) {
            val bitmaps = mutableListOf<Bitmap>()
            val gifState = drawable.constantState!!
            val frameLoader = gifState.javaClass.getDeclaredField("frameLoader")
            frameLoader.isAccessible = true
            val gifFrameLoader = frameLoader.get(gifState)

            val decoder = gifFrameLoader.javaClass.getDeclaredField("gifDecoder")
            decoder.isAccessible = true
            val gifDecoder = decoder.get(gifFrameLoader) as StandardGifDecoder

            for (i in 0..gifDecoder.frameCount) {
                gifDecoder.advance()
                bitmaps.add(gifDecoder.nextFrame!!)
            }
            response(bitmaps)
        }
    }
}