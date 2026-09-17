package com.hibrmemory.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.widget.FrameLayout;

public class MainActivity extends Activity {
    private static final String APP_URL = "https://hibr-notes-omar.netlify.app/";
    private static final int FILE_CHOOSER = 2202;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createAlarmChannel();
        setupWebView();
        handleIntent(getIntent(), true);
    }

    private void setupWebView() {
        web = new WebView(this);
        setContentView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " HibrAndroid/2.2.0");
        web.addJavascriptInterface(new HibrAlarmBridge(this), "HibrAlarm");
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u=req.getUrl(); String host=u.getHost()==null?"":u.getHost();
                if (host.equals("hibr-notes-omar.netlify.app") || host.endsWith("supabase.co")) return false;
                startActivity(new Intent(Intent.ACTION_VIEW,u)); return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                view.evaluateJavascript("window.__HIBR_NATIVE_ANDROID__=true;document.documentElement.classList.add('hibr-native-android');window.dispatchEvent(new Event('hibr-native-ready'));", null);
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if(fileCallback!=null) fileCallback.onReceiveValue(null); fileCallback=cb;
                Intent i=p.createIntent(); try{startActivityForResult(i,FILE_CHOOSER);return true;}catch(Exception e){fileCallback=null;return false;}
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    for(String r:request.getResources()) if(PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)){
                        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) request.grant(new String[]{PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                        else { request.deny(); requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},2203); }
                        return;
                    }
                    request.deny();
                });
            }
        });
    }

    private void createAlarmChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);NotificationChannel ch=new NotificationChannel("hibr_alarm_channel_v1","Hibr Alarms",NotificationManager.IMPORTANCE_HIGH);ch.setDescription("Alarm reminders from Hibr");ch.setSound(null,null);ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);nm.createNotificationChannel(ch);}
    }

    private String urlWithNote(String noteId){ if(noteId==null||noteId.isEmpty())return APP_URL; return APP_URL+"?alarmNote="+Uri.encode(noteId); }
    private void handleIntent(Intent intent, boolean initial){ String note=intent==null?null:intent.getStringExtra("alarm_note_id"); if(initial)web.loadUrl(urlWithNote(note)); else if(note!=null&&!note.isEmpty())web.loadUrl(urlWithNote(note)); }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent,false);}
    @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();AlarmScheduler.rescheduleAll(this);}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==FILE_CHOOSER&&fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));fileCallback=null;}}
}
