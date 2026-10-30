package com.wanderwildwood.tancho;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import com.wanderwildwood.tancho.databinding.ActivityDownloadBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Fetches the two BirdNET model files, once, and checks each against a SHA-256 fixed here.
 *
 * The files are read from one commit of the whoBIRD-TFlite repository, not from its moving
 * master branch, so what arrives is exactly what was checked when this version was built.
 * Anything that does not hash
 * to the constant is deleted, never loaded.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Downloader {
    static final String modelFILE = "model.tflite";
    static final String metaModelFILE = "metaModel.tflite";
    private static final String BASE = "https://raw.githubusercontent.com/woheller69/whoBIRD-TFlite/a79d101796654cf22f46383328547def828510e8/";
    static final String model16URL = BASE + "BirdNET_GLOBAL_6K_V2.4_Model_FP16.tflite";
    static final String model32URL = BASE + "BirdNET_GLOBAL_6K_V2.4_Model_FP32.tflite";
    static final String metaModelURL = BASE + "BirdNET_GLOBAL_6K_V2.4_MData_Model_V2_FP16.tflite";
    static final String model16SHA256 = "5c64ba3ffab264ccbfafe784896392051a86be63b0cc507e7683db7df114546b";
    static final String model32SHA256 = "55f3e4055b1a13bfa9a2452731d0d34f6a02d6b775a334362665892794165e4c";
    static final String metaModelSHA256 = "1226f23fc20362617deb09178f366111b70cf085c2c82893f2814e5acedce6c2";
    static final long model16Size = 25932528;
    static final long model32Size = 51726412;
    static final long metaModelSize = 14770468;

    /** Set by Cancel; the running download looks at it between chunks. */
    private static volatile boolean cancelled = false;
    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    /** SHA-256 of a file as lower-case hex, or "" if it cannot be read. */
    static String sha256(File file) {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
            return hex(md.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            return "";
        }
    }

    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static boolean modelHashOk(String sha) {
        return sha.equals(model16SHA256) || sha.equals(model32SHA256);
    }

    static boolean metaHashOk(String sha) {
        return sha.equals(metaModelSHA256);
    }

    private static File modelFile(Context c) {
        return new File(c.getDir("filesdir", Context.MODE_PRIVATE), modelFILE);
    }

    private static File metaFile(Context c) {
        return new File(c.getDir("filesdir", Context.MODE_PRIVATE), metaModelFILE);
    }

    /** True when both files are there and are the right ones; a wrong file is deleted. */
    public static boolean checkModels(final Context context) {
        File model = modelFile(context);
        File meta = metaFile(context);
        boolean modelOk = false;
        boolean metaOk = false;
        if (model.exists()) {
            modelOk = modelHashOk(sha256(model));
            if (!modelOk) model.delete();
        }
        if (meta.exists()) {
            metaOk = metaHashOk(sha256(meta));
            if (!metaOk) meta.delete();
        }
        return modelOk && metaOk;
    }

    /** Stops the download that is running; the screen is put back by that download's own thread. */
    public static void cancel() {
        cancelled = true;
    }

    private static void ready(final Activity activity) {
        if (activity.isFinishing()) return;
        activity.startActivity(new Intent(activity, MainActivity.class));
        activity.finish();
    }

    private static void idle(final Activity activity, final ActivityDownloadBinding binding, final int error) {
        activity.runOnUiThread(() -> {
            binding.downloadProgress.setVisibility(View.INVISIBLE);
            binding.downloadButton.setVisibility(View.VISIBLE);
            binding.downloadButton.setEnabled(true);
            binding.downloadCancel.setVisibility(View.GONE);
            if (error != 0) {
                binding.downloadError.setText(error);
                binding.downloadError.setVisibility(View.VISIBLE);
            }
        });
    }

    public static void downloadModels(final Activity activity, final ActivityDownloadBinding binding) {
        if (running) return;
        final boolean big = binding.option32bit.isChecked();
        final long modelSize = big ? model32Size : model16Size;
        cancelled = false;
        running = true;
        binding.downloadError.setVisibility(View.GONE);
        binding.downloadProgress.setProgress(0);
        binding.downloadProgress.setVisibility(View.VISIBLE);
        binding.downloadButton.setVisibility(View.GONE);
        binding.downloadCancel.setVisibility(View.VISIBLE);

        final File model = modelFile(activity);
        final File meta = metaFile(activity);
        final long total = modelSize + metaModelSize;

        new Thread(() -> {
            int error = 0;
            try {
                long done = 0;
                if (!(model.exists() && modelHashOk(sha256(model)))) {
                    model.delete();
                    fetch(activity, binding, big ? model32URL : model16URL, model,
                            big ? model32SHA256 : model16SHA256, 0, total);
                }
                done = modelSize;
                if (!(meta.exists() && metaHashOk(sha256(meta)))) {
                    meta.delete();
                    fetch(activity, binding, metaModelURL, meta, metaModelSHA256, done, total);
                }
                running = false;
                activity.runOnUiThread(() -> ready(activity));
                return;
            } catch (Cancelled c) {
                error = 0;
            } catch (Wrong w) {
                error = R.string.error_download_wrong;
            } catch (IOException e) {
                Log.w("Birding", "model download failed", e);
                error = R.string.download_failed;
            }
            running = false;
            idle(activity, binding, error);
        }).start();
    }

    private static class Cancelled extends IOException {
    }

    private static class Wrong extends IOException {
    }

    /** One file: to a part file first, so a stopped download never leaves something that looks whole. */
    private static void fetch(Activity activity, ActivityDownloadBinding binding, String address,
                              File target, String expected, long before, long total) throws IOException {
        File part = new File(target.getPath() + ".part");
        try {
            URLConnection connection = new URL(address).openConnection();
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(10000);
            try (InputStream in = new BufferedInputStream(connection.getInputStream(), 16 * 1024);
                 FileOutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[16 * 1024];
                long got = 0;
                int shown = -1;
                int n;
                while ((n = in.read(buf)) != -1) {
                    if (cancelled) throw new Cancelled();
                    out.write(buf, 0, n);
                    got += n;
                    // One redraw per whole percent: the panel cannot take one per chunk.
                    final int percent = (int) (((before + got) * 100L) / total);
                    if (percent != shown) {
                        shown = percent;
                        activity.runOnUiThread(() -> binding.downloadProgress.setProgress(percent));
                    }
                }
            }
            if (cancelled) throw new Cancelled();
            if (!sha256(part).equals(expected)) throw new Wrong();
            target.delete();
            if (!part.renameTo(target)) throw new IOException("rename");
        } finally {
            part.delete();
        }
    }
}
