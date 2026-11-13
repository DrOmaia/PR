package com.dromaia.residency;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Update from inside the app: reads version.json from the public update folder, downloads the APK, checks it and
 * hands it to Android's installer (which asks the user to confirm and only accepts an APK signed with the same key).
 */
final class Updater {
    /** The public update folder: version.json + residency-counter.apk (no dates inside). "" = no checks (tests). */
    static String base = "https://dromaia.github.io/residency/";
    static final String APK = "residency-counter.apk";
    static final String ACTION_STATUS = "com.dromaia.residency.INSTALL_STATUS";

    private Updater() {}

    static final class Info {
        int code;
        String name;
        String sha256;
    }

    static Info parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        Info i = new Info();
        i.code = o.getInt("versionCode");
        i.name = o.optString("versionName", String.valueOf(i.code));
        i.sha256 = o.optString("sha256", "");
        return i;
    }

    static Info check() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (InputStream in = open(base + "version.json?t=" + System.currentTimeMillis())) {
            copy(in, b, null);
        }
        return parse(new String(b.toByteArray(), StandardCharsets.UTF_8));
    }

    static File download(Context c, Info info) throws Exception {
        File f = new File(c.getCacheDir(), "update.apk");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = open(base + APK + "?v=" + info.code); OutputStream out = new FileOutputStream(f)) {
            copy(in, out, md);
        }
        StringBuilder hex = new StringBuilder();
        for (byte x : md.digest()) hex.append(String.format("%02x", x));
        if (!info.sha256.isEmpty() && !info.sha256.equalsIgnoreCase(hex.toString())) {
            f.delete();
            throw new IOException("checksum mismatch");
        }
        return f;
    }

    /** Gives the file to Android's installer; the result comes back to MainActivity.onNewIntent. */
    static void install(Activity a, File apk) throws IOException {
        PackageInstaller pi = a.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        p.setAppPackageName(a.getPackageName());
        int id = pi.createSession(p);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (OutputStream out = s.openWrite("update.apk", 0, apk.length()); InputStream in = new FileInputStream(apk)) {
                copy(in, out, null);
                s.fsync(out);
            }
            Intent i = new Intent(a, MainActivity.class).setAction(ACTION_STATUS);
            PendingIntent result = PendingIntent.getActivity(a, id, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            s.commit(result.getIntentSender());
        }
    }

    private static InputStream open(String url) throws IOException {
        URLConnection u = new URL(url).openConnection();
        u.setConnectTimeout(15000);
        u.setReadTimeout(30000);
        u.setUseCaches(false);
        return u.getInputStream();
    }

    private static void copy(InputStream in, OutputStream out, MessageDigest md) throws IOException {
        byte[] buf = new byte[16384];
        for (int n; (n = in.read(buf)) > 0; ) {
            out.write(buf, 0, n);
            if (md != null) md.update(buf, 0, n);
        }
    }
}
