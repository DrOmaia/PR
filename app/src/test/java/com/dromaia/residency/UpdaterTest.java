package com.dromaia.residency;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

/** The update check and download against a local web server standing in for the public update folder. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UpdaterTest {
    private ServerSocket server;
    private final byte[] apk = "pretend apk bytes".getBytes(StandardCharsets.UTF_8);
    private String json;

    @Before
    public void start() throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(apk)) hex.append(String.format("%02x", b));
        json = "{\"versionCode\": 7, \"versionName\": \"1.6\", \"sha256\": \"" + hex + "\"}";
        server = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"));
        Thread t = new Thread(() -> {
            while (!server.isClosed()) {
                try (Socket s = server.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                    String path = in.readLine().split(" ")[1];
                    for (String line; (line = in.readLine()) != null && !line.isEmpty(); ) { /* headers */ }
                    byte[] body = path.startsWith("/version.json") ? json.getBytes(StandardCharsets.UTF_8)
                            : path.startsWith("/" + Updater.APK) ? apk : null;
                    OutputStream out = s.getOutputStream();
                    out.write(((body == null ? "HTTP/1.1 404 Not Found" : "HTTP/1.1 200 OK") + "\r\nContent-Length: "
                            + (body == null ? 0 : body.length) + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    if (body != null) out.write(body);
                    out.flush();
                } catch (IOException e) {
                    // closed
                }
            }
        });
        t.setDaemon(true);
        t.start();
        Updater.base = "http://127.0.0.1:" + server.getLocalPort() + "/";
    }

    @After
    public void stop() throws IOException {
        server.close();
        Updater.base = "";
    }

    @Test
    public void check_readsTheVersion() throws Exception {
        Updater.Info i = Updater.check();
        assertEquals(7, i.code);
        assertEquals("1.6", i.name);
    }

    @Test
    public void download_savesTheFileWhenTheChecksumMatches() throws Exception {
        File f = Updater.download(RuntimeEnvironment.getApplication(), Updater.check());
        assertArrayEquals(apk, Files.readAllBytes(f.toPath()));
    }

    @Test
    public void download_refusesAChangedFile() throws Exception {
        Updater.Info i = Updater.check();
        i.sha256 = "00" + i.sha256.substring(2);
        try {
            Updater.download(RuntimeEnvironment.getApplication(), i);
            fail("a file with the wrong checksum was accepted");
        } catch (IOException expected) {
            assertFalse(new File(RuntimeEnvironment.getApplication().getCacheDir(), "update.apk").exists());
        }
    }
}
