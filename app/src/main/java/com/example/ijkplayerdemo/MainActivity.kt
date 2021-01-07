package com.example.ijkplayerdemo
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ijkplayerdemo.IjkVideoView.VideoPlayerListener
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.player.pragma.DebugLog.i
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val Tag:String = this.javaClass.name
    private val path:String = "http://cctvalih5ca.v.myalicdn.com/live/cctv1_2/index.m3u8"
    private var player: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var startAutoplay = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initIjkPlayer(path)
        mIjkVideoView.setOnClickListener {
            if (mIjkVideoView.isPlaying)
            {
                mIjkVideoView.pause()
                return@setOnClickListener
            }
            mIjkVideoView.start()
        }

    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onStart() {
        super.onStart()
        //初始化player
        initPlayer()
        //让player_view准备好
        video_view?.onResume()
    }

    private fun initializePlayer() {
    }

    private fun initIjkPlayer(videoPth: String) {
        IjkMediaPlayer.loadLibrariesOnce(null)
        IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        //监听
        mIjkVideoView.setListener(object : VideoPlayerListener() {
            override fun onPrepared(mp: IMediaPlayer) {
                //播放成功处理
                mp.start()
            }

            override fun onCompletion(iMediaPlayer: IMediaPlayer) {
                i("RetrofitVideoPlayerListener", "onCompletion")
            }

            override fun onError(p0: IMediaPlayer?, p1: Int, p2: Int): Boolean {
                i("RetrofitVideoPlayerListener", "onError")
                Toast.makeText(this@MainActivity, "播放失败", Toast.LENGTH_LONG).show()
                return true
            }
        })
        //路径
        mIjkVideoView.setPath(videoPth)
        mIjkVideoView.start()
    }

    override fun onStop() {
        super.onStop()
        Log.d(Tag,"onStop()")
        mIjkVideoView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Tag,"onDestroy()")
        mIjkVideoView.release()

        releasePlayer()
    }

    override fun onPause() {
        super.onPause()
        Log.d(Tag,"onPause()")
        mIjkVideoView.pause()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(Tag,"onRestart()")
        mIjkVideoView.reset()
    }

    private fun createMediaSource(path: String): MediaSource? {
        //首先你需要获取到Uri，我这里是通过另一个activity的intent传递过来的
        var videoUri: Uri = Uri.parse(path)
        //接下来你需要分析出你要播放的视频是什么格式的
        val type: Int = Util.inferContentType(videoUri)
        println("解析出的视频格式为 ： $type")
        //这里的BassApplication是我自定义的application的名称
        var dataSourceFactory: DataSource.Factory =
            DefaultHttpDataSourceFactory(Util.getUserAgent(this, "MyApplication"))
        //接下来就是根据不同的视频格式，创建不同的MediaSource了
        return when (type) {
            C.TYPE_DASH ->
                DashMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri)
            C.TYPE_SS ->
                SsMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri)
            C.TYPE_HLS ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri)
            C.TYPE_OTHER ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUri)
            else -> null
        }
    }

    private fun initPlayer() {
        if (player == null) {
            trackSelector = DefaultTrackSelector(this)
            //初始化Exoplayer
            player = SimpleExoPlayer.Builder(this).build()
            //监听播放状态以及失败原因
            player!!.addListener(PlayerEventListener())
            //将错误信息打印出来
            player!!.addAnalyticsListener(EventLogger(trackSelector))
            //
            player!!.setAudioAttributes(
                AudioAttributes.DEFAULT, /* handleAudioFocus= */  /* handleAudioFocus= */
                true
            )
            //当他准备好资源后就播放
            player!!.playWhenReady = startAutoplay
            //这里是将player设置近player_view中
            video_view.player = player
            //
            video_view.setPlaybackPreparer {  }
        }
        //初始化完成后，我们就将资源设置近player中
        createMediaSource(path)?.let { player!!.prepare(it) }
    }

    //这里监听到播放时的状态，以及获取到异常情况
    private class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            println("xxxxx  onPlayerStateChanged")
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (error.type == ExoPlaybackException.TYPE_SOURCE) {
                val cause: IOException = error.sourceException
                if (cause is HttpDataSource.HttpDataSourceException) {
                    val httpError: HttpDataSource.HttpDataSourceException = cause
                    val dataSpec = httpError.dataSpec

                    if (httpError is HttpDataSource.InvalidResponseCodeException) {
                        // Cast to InvalidResponseCodeException and retrieve the response code,
                        // message and headers.
                    } else {
                        // Try calling httpError.getCause() to retrieve the underlying cause,
                        // although note that it may be null.
                    }
                }
            }
            println("xxxxxxxxxx   发生播放异常")
        }

        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
            println("xxxxx  onTracksChanged")
        }
    }
    private fun releasePlayer() {
        if (player != null) {
            player!!.release()
            player = null
            trackSelector = null
        }
    }
}