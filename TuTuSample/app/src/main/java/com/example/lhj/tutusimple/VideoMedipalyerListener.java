package com.example.lhj.tutusimple;

import com.example.lhj.tutusimple.video.IMediaPlayer;

/**
 * Created by LiaoHongjie on 2017/8/24.
 */

public interface VideoMedipalyerListener extends
        IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener,
        IMediaPlayer.OnSeekCompleteListener,
        IMediaPlayer.OnVideoSizeChangedListener,
        IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnInfoListener {
}
