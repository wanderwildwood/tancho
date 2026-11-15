package com.wanderwildwood.tancho;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class PlayNotification {
    /**
     * The sound for a bird heard, when that setting is on. One that cannot be played is said on
     * the screen in front ([FootNotice]); said again it leaves the same line standing.
     */
    public static void playSound(Context context){
        MediaPlayer mediaPlayer= MediaPlayer.create(context,R.raw.notification);
        if (mediaPlayer == null) {
            cannotPlay(context);
            return;
        }
        mediaPlayer.setVolume(1, 1);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.reset();
            mp.release();
        });
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.w("PlayNotification", "Sound would not play: " + what + "/" + extra);
            mp.release();
            cannotPlay(context);
            return true;
        });
        mediaPlayer.start();
    }

    private static void cannotPlay(Context context) {
        FootNotice.INSTANCE.say(context, context.getString(R.string.chime_cannot_play));
    }
}
