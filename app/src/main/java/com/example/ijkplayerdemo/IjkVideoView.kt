package com.example.ijkplayerdemo

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException

/**
 * @author DELL
 */
class IjkVideoView : FrameLayout {
    private var mContext //上下文
            : Context? = null
    private var mMediaPlayer: IMediaPlayer? = null //视频控制类
    private var mVideoPlayerListener //自定义监听器
            : VideoPlayerListener? = null
    private var mSurfaceView //播放视图
            : SurfaceView? = null
    private var mPath = "" //视频文件地址

    constructor(context: Context) : super(context) {
        initVideoView(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        initVideoView(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        initVideoView(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
    }

    abstract class VideoPlayerListener : IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener

    private fun initVideoView(context: Context) {
        mContext = context
        isFocusable = true
    }

    fun setPath(path: String) {
        if (TextUtils.equals("", mPath)) {
            mPath = path
            initSurfaceView()
        } else {
            mPath = path
            loadVideo()
        }
    }

    private fun initSurfaceView() {
        mSurfaceView = SurfaceView(mContext)
        mSurfaceView!!.holder.addCallback(LmnSurfaceCallback())
        val layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
        mSurfaceView!!.layoutParams = layoutParams
        this.addView(mSurfaceView)
    }

    //surfaceView的监听器
    private inner class LmnSurfaceCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {}
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            loadVideo()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    //加载视频
    private fun loadVideo() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
            mMediaPlayer!!.release()
        }
        mMediaPlayer = IjkMediaPlayer()
        if (mVideoPlayerListener != null) {
            (mMediaPlayer as IjkMediaPlayer).setOnPreparedListener(mVideoPlayerListener)
            (mMediaPlayer as IjkMediaPlayer).setOnErrorListener(mVideoPlayerListener)
        }
        try {
            (mMediaPlayer as IjkMediaPlayer).setDataSource(mPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        (mMediaPlayer as IjkMediaPlayer).setDisplay(mSurfaceView!!.holder)
        (mMediaPlayer as IjkMediaPlayer).prepareAsync()
    }

    fun setListener(listener: VideoPlayerListener?) {
        mVideoPlayerListener = listener
        if (mMediaPlayer != null) {
            mMediaPlayer!!.setOnPreparedListener(listener)
        }
    }

    val isPlaying: Boolean
        get() = if (mMediaPlayer != null) {
            mMediaPlayer!!.isPlaying
        } else false

    fun start() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.start()
        }
    }

    fun pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
        }
    }

    fun stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.stop()
        }
    }

    fun reset() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
        }
    }

    fun release() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.reset()
            mMediaPlayer!!.release()
            mMediaPlayer = null
        }
    }
}