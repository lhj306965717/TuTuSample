package com.example.lhj.tutusimple;

/**
 * Created by LiaoHongjie on 2017/8/24.
 */

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.lhj.tutusimple.video.AndroidMediaPlayer;
import com.example.lhj.tutusimple.video.IMediaPlayer;
import com.example.lhj.tutusimple.video.IVideoPlayer;

import java.io.IOException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by LiaoHongjie on 2017/8/24.
 */

public abstract class VideoPlayerController implements IVideoPlayer, VideoMedipalyerListener {

    /**
     * 播放错误
     **/
    public static final int STATE_ERROR = -1;
    /**
     * 播放未开始
     **/
    public static final int STATE_IDLE = 0;
    /**
     * 播放准备中
     **/
    public static final int STATE_PREPARING = 1;
    /**
     * 播放准备就绪
     **/
    public static final int STATE_PREPARED = 2;
    /**
     * 正在播放
     **/
    public static final int STATE_PLAYING = 3;
    /**
     * 暂停播放
     **/
    public static final int STATE_PAUSED = 4;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，缓冲区数据足够后恢复播放)
     **/
    public static final int STATE_BUFFERING_PLAYING = 5;
    /**
     * 正在缓冲(播放器正在播放时，缓冲区数据不足，进行缓冲，此时暂停播放器，继续缓冲，缓冲区数据足够后恢复暂停
     **/
    public static final int STATE_BUFFERING_PAUSED = 6;
    /**
     * 播放完成
     **/
    public static final int STATE_COMPLETED = 7;
    /**
     * 普通模式
     **/
    public static final int MODE_NORMAL_SCREEN = 10;
    /**
     * 全屏模式
     **/
    public static final int MODE_FULL_SCREEN = 11;

    /**
     * 初始化
     */
    private int mCurrentState = STATE_IDLE;
    private int mCurrentMode = MODE_NORMAL_SCREEN;
    private final Context mContext;
    private String mUrl;
    private Map<String, String> mHeaders;
    private boolean continueFromLastPosition;
    private AudioManager mAudioManager;
    private AndroidMediaPlayer mMediaPlayer;
    protected MyGLSurfaceView mGLSurfaceView;
    protected VideoPlayer mVideoPlayer;
    private int mBufferPercentage;
    private Timer mUpdateProgressTimer;
    private TimerTask mUpdateProgressTimerTask;
    private Handler mHandler;


    public VideoPlayerController(@NonNull Context context) {
        this.mContext = context;
        init();
    }

    private void init() {
        mHandler = new Handler(Looper.myLooper());
    }

    public void setUp(String url, Map<String, String> headers) {
        this.mUrl = url;
        this.mHeaders = headers;
    }

    /**
     * 是否从上一次的位置继续播放
     *
     * @param continueFromLastPosition true从上一次的位置继续播放
     */
    @Override
    public void continueFromLastPosition(boolean continueFromLastPosition) {
        this.continueFromLastPosition = continueFromLastPosition;
    }

    @Override
    public void start() {
        if (mCurrentState == STATE_IDLE) {
            //            NiceVideoPlayerManager.instance().setCurrentNiceVideoPlayer(this);
            initSurfaceView();
            initAudioManager();
            initMediaPlayer();
            addSurfaceView();
        } else {
            Log.d("LHJ", "NiceVideoPlayer只有在mCurrentState == STATE_IDLE时才能调用start方法.");
        }
    }

    /**
     * 从指定位置开始播放
     *
     * @param position 播放位置
     */
    @Override
    public void start(long position) {
        seekTo(position);
    }

    /**
     * 重新播放
     */
    @Override
    public void restart() {

    }

    @Override
    public void pause() {
        if (isPlaying() || isBufferingPlaying()) {
            mMediaPlayer.pause();
        }
    }

    @Override
    public void seekTo(long pos) {
        mMediaPlayer.seekTo(pos);
    }

    /**
     * @param volume 音量值
     */
    @Override
    public void setVolume(int volume) {
        mMediaPlayer.setVolume(volume, volume);
    }

    public void setVideoPlayer(VideoPlayer videoPlayer) {
        this.mVideoPlayer = videoPlayer;
    }

    private void addSurfaceView() {
        if (mVideoPlayer == null)
            throw new NullPointerException("VideoPlayer 不能为空!");
        mVideoPlayer.addView(mGLSurfaceView, 0);
        mMediaPlayer.setSurface(mGLSurfaceView.getSurface());
    }

    private void initSurfaceView() {
        if (mGLSurfaceView == null) {
            mGLSurfaceView = new MyGLSurfaceView(mContext);
            mGLSurfaceView.setVideoPlayerController(this);
        }
    }

    private void initMediaPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new AndroidMediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        } else {
            mMediaPlayer.reset(); //重置
        }
    }

    private void initAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void openMediaPlayer() {

        // 屏幕常量
        mVideoPlayer.setKeepScreenOn(true);

        // 是否使用SurfaceHolder来显示
        mMediaPlayer.setScreenOnWhilePlaying(false);
        // 视频资源加载回调监听
        mMediaPlayer.setOnPreparedListener(this);
        // 视频大小变化监听
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        // 播放完成监听
        mMediaPlayer.setOnCompletionListener(this);
        // 播放错误监听
        mMediaPlayer.setOnErrorListener(this);
        // 指示信息和警告信息监听
        mMediaPlayer.setOnInfoListener(this);
        // 缓冲更新监听
        mMediaPlayer.setOnBufferingUpdateListener(this);
        // seekto定位监听
        mMediaPlayer.setOnSeekCompleteListener(this);

        // 设置dataSource
        try {
            mMediaPlayer.setDataSource(mContext.getApplicationContext(), Uri.parse(mUrl), mHeaders);
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
            onPlayStateChanged(mCurrentState);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * UI改变
     *
     * @param currentState
     */
    public abstract void onPlayStateChanged(int currentState);

    /**
     * 横竖屏切换
     *
     * @param playMode
     */
    public abstract void onPlayModeChanged(int playMode);

    @Override
    public boolean isIdle() {
        return mCurrentState == STATE_IDLE;
    }

    @Override
    public boolean isPreparing() {
        return mCurrentState == STATE_PREPARING;
    }

    @Override
    public boolean isPrepared() {
        return mCurrentState == STATE_PREPARED;
    }

    @Override
    public boolean isBufferingPlaying() {
        return mCurrentState == STATE_BUFFERING_PLAYING;
    }

    @Override
    public boolean isBufferingPaused() {
        return mCurrentState == STATE_BUFFERING_PAUSED;
    }

    @Override
    public boolean isPlaying() {
        return mCurrentState == STATE_PLAYING;
    }

    @Override
    public boolean isPaused() {
        return mCurrentState == STATE_PAUSED;
    }

    @Override
    public boolean isError() {
        return mCurrentState == STATE_ERROR;
    }

    @Override
    public boolean isCompleted() {
        return mCurrentState == STATE_COMPLETED;
    }

    @Override
    public boolean isFullScreen() {
        return mCurrentMode == MODE_FULL_SCREEN;
    }

    @Override
    public boolean isTinyWindow() {
        return false;
    }

    @Override
    public boolean isNormal() {
        return mCurrentMode == MODE_NORMAL_SCREEN;
    }

    @Override
    public int getMaxVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public int getVolume() {
        if (mAudioManager != null) {
            return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    @Override
    public long getDuration() {
        return mMediaPlayer != null ? mMediaPlayer.getDuration() : 0;
    }

    @Override
    public long getCurrentPosition() {
        return mMediaPlayer != null ? mMediaPlayer.getCurrentPosition() : 0;
    }

    @Override
    public int getBufferPercentage() {
        return mBufferPercentage;
    }

    @Override
    public void enterFullScreen() {

    }

    @Override
    public boolean exitFullScreen() {
        return false;
    }

    @Override
    public void releasePlayer() {

    }

    @Override
    public void release() {

    }


    @Override
    public void onBufferingUpdate(IMediaPlayer mp, int percent) {
        this.mBufferPercentage = percent;
    }

    /**
     * 开启更新进度的计时器。
     */
    protected void startUpdateProgressTimer() {
        cancelUpdateProgressTimer();
        if (mUpdateProgressTimer == null) {
            mUpdateProgressTimer = new Timer();
        }
        if (mUpdateProgressTimerTask == null) {
            mUpdateProgressTimerTask = new TimerTask() {
                @Override
                public void run() {
                    if (isPlaying() || isPaused() || isBufferingPlaying() || isBufferingPaused()) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mVideoPlayer.setProgressAndText(getCurrentPosition(), getDuration(), getBufferPercentage());
                            }
                        });
                    }

                }
            };
        }
        mUpdateProgressTimer.schedule(mUpdateProgressTimerTask, 0, 300);
    }

    /**
     * 取消更新进度的计时器。
     */
    protected void cancelUpdateProgressTimer() {
        if (mUpdateProgressTimer != null) {
            mUpdateProgressTimer.cancel();
            mUpdateProgressTimer = null;
        }
        if (mUpdateProgressTimerTask != null) {
            mUpdateProgressTimerTask.cancel();
            mUpdateProgressTimerTask = null;
        }
    }
}