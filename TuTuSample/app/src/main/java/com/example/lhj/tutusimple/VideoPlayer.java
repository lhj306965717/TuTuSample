package com.example.lhj.tutusimple;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.tqit.stereorast.render.TQITVideoRenderer;

/**
 * Created by LiaoHongjie on 2017/8/24.
 * <p>
 * UI
 */

public abstract class VideoPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener {

    private final Context mContext;
    private View mLayout_bottom;
    private ImageView mIv_start;
    private TextView mTv_current;
    private SeekBar mBottom_seek_progress;
    private TextView mTv_total;
    private Switch mLrswap;
    private ImageView mIv_fullscreen;
    private ProgressBar mBottom_progress;
    private ProgressBar mLoading;
    private View mFl_retry;
    private TextView mTv_retry;
    private VideoPlayerController mVideoController;
    private Handler mHandler;
    private CountDownTimer mDismissTopBottomCountDownTimer;

    public VideoPlayer(@NonNull Context context) {
        super(context);
        this.mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View rootView = inflate(mContext, getLayoutId(), this);

        // 封面
        ImageView thumb = rootView.findViewById(R.id.thumb);

        // 底部控制
        mLayout_bottom = rootView.findViewById(R.id.layout_bottom);
        mIv_start = rootView.findViewById(R.id.iv_start);
        mTv_current = rootView.findViewById(R.id.current);
        mBottom_seek_progress = rootView.findViewById(R.id.bottom_seek_progress);
        mTv_total = rootView.findViewById(R.id.total);
        mLrswap = rootView.findViewById(R.id.lrswap);
        mIv_fullscreen = rootView.findViewById(R.id.fullscreen);
        mBottom_progress = rootView.findViewById(R.id.bottom_progress);

        // 最底部进度条
        mLoading = rootView.findViewById(R.id.loading);

        // 重播
        mFl_retry = rootView.findViewById(R.id.fl_retry);
        mTv_retry = rootView.findViewById(R.id.retry_text);

        addView(rootView);

        mIv_start.setOnClickListener(this);
        mIv_fullscreen.setOnClickListener(this);
        mBottom_seek_progress.setOnSeekBarChangeListener(this);
        mLrswap.setOnCheckedChangeListener(this);
    }

    public void setVideoPlayerController(VideoPlayerController controller) {
        this.mVideoController = controller;
    }

    @Override
    public void onClick(View view) {

        int viewId = view.getId();

        if (viewId == R.id.iv_start) {
            // 只有这两种状态才行
            if (mVideoController.isPlaying() || mVideoController.isPaused()) {
                if (mVideoController.isPlaying()) {
                    mVideoController.pause();
                    mIv_start.setImageResource(R.drawable.icon_bf_stop);
                } else {
                    mVideoController.start(0); // 播放位置
                    mIv_start.setImageResource(R.drawable.icon_start);
                }
            }
        } else if (viewId == R.id.fullscreen) {
            // 全屏切换
            if (mVideoController.isFullScreen()) {
                mVideoController.onPlayModeChanged(VideoPlayerController.MODE_NORMAL_SCREEN);
            } else {
                mVideoController.onPlayModeChanged(VideoPlayerController.MODE_FULL_SCREEN);
            }
        }
    }

    protected abstract int getLayoutId();

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

        View childVide = getChildAt(0);

        if (childVide instanceof GLSurfaceView) {

            MyGLSurfaceView surfaceView = (MyGLSurfaceView) childVide;

            if (isChecked) {
                surfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_LR);
            } else {
                surfaceView.setMode(TQITVideoRenderer.TQIT_VIDEO_MODE_2D);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * 开启top、bottom自动消失的timer
     */
    private void startDismissTopBottomTimer() {
        cancelDismissTopBottomTimer();
        if (mDismissTopBottomCountDownTimer == null) {
            // 倒计时 5秒 内，每秒执行一次onTick
            mDismissTopBottomCountDownTimer = new CountDownTimer(5000, 5000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    setBottomVisible(false);
                    setLeftVisible(false);
                    setTopVisible(false);
                }
            };
        }
        mDismissTopBottomCountDownTimer.start();
    }

    public void setProgressAndText(long position, long duration, int bufferPercentage){

        int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));

        mBottom_seek_progress.setProgress(progress);

        mBottom_seek_progress.setSecondaryProgress(bufferPercentage);

        mTv_current.setText(VideoPlayerUtil.formatTime(position));
        mTv_total.setText(VideoPlayerUtil.formatTime(duration));
    }

    /**
     * 取消top、bottom自动消失的timer
     */
    private void cancelDismissTopBottomTimer() {
        if (mDismissTopBottomCountDownTimer != null) {
            mDismissTopBottomCountDownTimer.cancel();
        }
    }

    /**
     * 设置bottom的显示和隐藏
     *
     * @param visible true显示，false隐藏.
     */
    private void setBottomVisible(boolean visible) {
        mLayout_bottom.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (visible) {
            if (!mVideoController.isPaused() && !mVideoController.isBufferingPaused()) {
                startDismissTopBottomTimer();
            }
        } else {
            cancelDismissTopBottomTimer();
        }
    }

    /**
     * 设置top的显示和隐藏
     *
     * @param visible
     */
    public abstract void setTopVisible(boolean visible);

    /**
     * 设置Left的显示和隐藏
     *
     * @param visible
     */
    public abstract void setLeftVisible(boolean visible);

}
