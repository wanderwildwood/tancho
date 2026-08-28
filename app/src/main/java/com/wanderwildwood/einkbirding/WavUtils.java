package com.wanderwildwood.einkbirding;

import static android.os.Environment.DIRECTORY_MUSIC;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class WavUtils {
    public static final String TAG = "WavUtils";

    /** Where the shared-storage version of this used to put them. Emptied on first run. */
    private static final String LEGACY_DIR = "/eInk Birding";

    /**
     * Where a detection's audio is kept: the app's own directory on external storage.
     *
     * It used to be the reader's Music folder. Each of these is 288KB and one bird singing
     * for thirteen seconds makes four of them - the recogniser runs every 800ms and every
     * detection above the threshold writes one - so a morning outdoors would drop hundreds
     * of megabytes into a music library, owned by media_rw, where every player, scanner and
     * backup would find them and offer them as songs.
     *
     * Here they belong to the app: no permission is needed on any Android version, nothing
     * scans them, and uninstalling takes them with it.
     */
    public static File recordingsDir(Context context) {
        return context.getExternalFilesDir(DIRECTORY_MUSIC);
    }

    /** Every recording currently kept, newest first is not promised - order is the caller's. */
    public static File[] recordings(Context context) {
        File dir = recordingsDir(context);
        File[] files = dir == null ? null : dir.listFiles();
        return files == null ? new File[0] : files;
    }

    /** Throw them all away. Used by the setting that keeps recordings for one session only. */
    public static int clearRecordings(Context context) {
        int deleted = 0;
        for (File file : recordings(context)) {
            if (file.delete()) deleted++;
        }
        return deleted;
    }

    /**
     * Carry anything left in the old shared-storage folder across, once, so that a row
     * recorded before this change still plays. The old folder is then removed, because
     * leaving an empty "eInk Birding" in someone's Music is its own small rudeness.
     */
    public static void migrateLegacyRecordings(Context context) {
        File legacy = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).getPath() + LEGACY_DIR);
        if (!legacy.isDirectory()) return;
        File target = recordingsDir(context);
        if (target == null) return;
        if (!target.exists() && !target.mkdirs()) return;
        File[] files = legacy.listFiles();
        if (files != null) {
            for (File file : files) {
                File moved = new File(target, file.getName());
                if (!moved.exists() && !file.renameTo(moved)) {
                    Log.w(TAG, "Could not move " + file.getName() + " out of Music");
                    continue;
                }
                file.delete();
            }
        }
        if (!legacy.delete()) Log.w(TAG, "Old recordings folder not removed: " + legacy);
    }

    public static void playWaveFile(Context context, long timestamp) {
        File path = new File(recordingsDir(context), timestamp + ".wav");
        Log.d(TAG,"Play "+path.getAbsolutePath());
        if (path.exists()){
            Log.d(TAG,"Play "+path.getAbsolutePath());
            MediaPlayer mediaPlayer = MediaPlayer.create(context, Uri.parse(path.getAbsolutePath()));
            mediaPlayer.setVolume(1, 1);
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.reset();
                mp.release();
            });
        }
    }

    /**
     * Read a recording back as the writer left it: samples in short range held as floats,
     * which is the scale [MelSpectrogram] divides down from. Returns null if there is no
     * such recording, which is the normal answer for a row heard while the setting was off.
     */
    public static FloatBuffer readWaveFile(Context context, long timestamp) {
        File path = new File(recordingsDir(context), timestamp + ".wav");
        if (!path.exists()) return null;
        try {
            byte[] bytes = new byte[(int) path.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(path)) {
                int read = 0;
                while (read < bytes.length) {
                    int n = in.read(bytes, read, bytes.length - read);
                    if (n < 0) break;
                    read += n;
                }
                if (read <= HEADER_BYTES) return null;
                ByteBuffer buffer = ByteBuffer.wrap(bytes, HEADER_BYTES, read - HEADER_BYTES);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                java.nio.ShortBuffer shorts = buffer.asShortBuffer();
                FloatBuffer samples = FloatBuffer.allocate(shorts.remaining());
                while (shorts.hasRemaining()) samples.put(shorts.get());
                samples.rewind();
                return samples;
            }
        } catch (IOException e) {
            Log.w(TAG, "Could not read " + path.getName() + ": " + e.getMessage());
            return null;
        }
    }

    /** The fixed-size header this class writes: RIFF/WAVE with a 16-byte PCM fmt chunk. */
    private static final int HEADER_BYTES = 44;

    public static void createWaveFile(Context context, long timestamp, FloatBuffer samplesBuffer, int sampleRate, int numChannels, int bytesPerSample) {
        try {
            byte[] samples = convertFloatBufferToWavBytes(samplesBuffer);
            int dataSize = samples.length; // actual data size in bytes
            int audioFormat = (bytesPerSample == 2) ? 1 : (bytesPerSample == 4) ? 3 : 0; // PCM_16 = 1, PCM_FLOAT = 3

            if (!isExternalStorageWritable()) {
                Log.e(TAG, "External Storage not writeable ");
                return;
            }
            File path = recordingsDir(context);
            if (path == null || (!path.exists() && !path.mkdirs())) {
                Log.e(TAG, "Failed to make directory: " + path);
                return;
            }
            String filePath = path.getAbsolutePath()+"/" + timestamp+".wav";
            Log.d(TAG, filePath);
            File outFile = new File(filePath);
            if (outFile.exists()) {
                Log.d(TAG, ".wav exists already");
                return; //in case this is the second detection in this file
            }
            FileOutputStream fileOutputStream = new FileOutputStream(filePath);
            fileOutputStream.write("RIFF".getBytes(StandardCharsets.UTF_8)); // Write the "RIFF" chunk descriptor
            fileOutputStream.write(intToByteArray(36 + dataSize), 0, 4); // Total file size - 8 bytes
            fileOutputStream.write("WAVE".getBytes(StandardCharsets.UTF_8)); // Write the "WAVE" format
            fileOutputStream.write("fmt ".getBytes(StandardCharsets.UTF_8)); // Write the "fmt " sub-chunk
            fileOutputStream.write(intToByteArray(16), 0, 4); // Sub-chunk size (16 for PCM)
            fileOutputStream.write(shortToByteArray((short) audioFormat), 0, 2); // Audio format (1 for PCM)
            fileOutputStream.write(shortToByteArray((short) numChannels), 0, 2); // Number of channels
            fileOutputStream.write(intToByteArray(sampleRate), 0, 4); // Sample rate
            fileOutputStream.write(intToByteArray(sampleRate * numChannels * bytesPerSample), 0, 4); // Byte rate
            fileOutputStream.write(shortToByteArray((short) (numChannels * bytesPerSample)), 0, 2); // Block align
            fileOutputStream.write(shortToByteArray((short) (bytesPerSample * 8)), 0, 2); // Bits per sample
            fileOutputStream.write("data".getBytes(StandardCharsets.UTF_8)); // Write the "data" sub-chunk
            fileOutputStream.write(intToByteArray(dataSize), 0, 4); // Data size

            // Write audio samples
            fileOutputStream.write(samples);

            // Close the file output stream
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error...", e);
        }
    }

    private static byte[] intToByteArray(int value) {
        byte[] byteArray = new byte[4]; // Create a 4-byte array

        // Convert and store the bytes in little-endian order
        byteArray[0] = (byte) (value & 0xFF);         // Least significant byte (LSB)
        byteArray[1] = (byte) ((value >> 8) & 0xFF);  // Second least significant byte
        byteArray[2] = (byte) ((value >> 16) & 0xFF); // Second most significant byte
        byteArray[3] = (byte) ((value >> 24) & 0xFF); // Most significant byte (MSB)

        return byteArray;
    }

    private static byte[] shortToByteArray(int value) {
        byte[] byteArray = new byte[2]; // Create a 2-byte array

        // Convert and store the bytes in little-endian order
        byteArray[0] = (byte) (value & 0xFF);        // Least significant byte (LSB)
        byteArray[1] = (byte) ((value >> 8) & 0xFF); // Most significant byte (MSB)

        return byteArray;
    }

    /* Checks if external storage is available for read and write */
    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static byte[] convertFloatBufferToWavBytes(FloatBuffer floatBuffer) {
        // Ensure the FloatBuffer is in the correct position
        floatBuffer.rewind();
        int bufferLength = floatBuffer.remaining();
        short[] shortArray = new short[bufferLength];

        // Step 1: Extract float values
        for (int i = 0; i < bufferLength; i++) {
            float value = floatBuffer.get();
            shortArray[i] = (short) value;
        }

        // Step 2: Convert short array to byte array
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferLength * 2);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.asShortBuffer().put(shortArray);

        return byteBuffer.array();
    }

}
