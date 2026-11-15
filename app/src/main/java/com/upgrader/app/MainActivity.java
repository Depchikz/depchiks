package com.upgrader.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_FILE = 1;
    private static final int REQ_PERM = 2;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false); // звуки игры (WebAudio) без лишних блокировок автозапуска
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                if (hasPhotoPermission()) {
                    openPicker();
                } else {
                    requestPermissions(new String[]{photoPermission()}, REQ_PERM);
                }
                return true;
            }
        });
        if (b != null) web.restoreState(b); else web.loadUrl("file:///android_asset/index.html");
    }

    private String photoPermission() {
        return Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private boolean hasPhotoPermission() {
        return checkSelfPermission(photoPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    private void openPicker() {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i, "Выберите фото"), REQ_FILE);
        } catch (Exception e) {
            cancelFile();
            Toast.makeText(this, "Не удалось открыть галерею", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelFile() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code != REQ_PERM) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            openPicker();
        } else {
            cancelFile();
            Toast.makeText(this, "Без разрешения на фото аватарку не сменить", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code != REQ_FILE || fileCallback == null) return;
        Uri[] uris = null;
        if (result == RESULT_OK && data != null) {
            uris = WebChromeClient.FileChooserParams.parseResult(result, data);
        }
        fileCallback.onReceiveValue(uris);
        fileCallback = null;
    }

    @Override protected void onSaveInstanceState(Bundle o) { super.onSaveInstanceState(o); web.saveState(o); }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
