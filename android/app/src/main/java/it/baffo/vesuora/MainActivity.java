package it.baffo.vesuora;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebViewAssetLoader;

/**
 * Una sola schermata con una WebView:
 *  - l'elenco delle fermate è il sito incluso nell'APK (assets/www/index.html), servito da
 *    WebViewAssetLoader su https://appassets.androidplatform.net così localStorage funziona;
 *  - i tabelloni EAV (orariotreni.eavsrl.it) si aprono dentro l'app, e il tasto Indietro
 *    riporta all'elenco;
 *  - tutti gli altri link si aprono nel browser del telefono;
 *  - trascinando verso il basso si ricarica la pagina (utile per aggiornare il tabellone).
 */
public class MainActivity extends AppCompatActivity {

    private static final String ASSET_HOST = WebViewAssetLoader.DEFAULT_DOMAIN; // appassets.androidplatform.net
    private static final String START_URL = "https://" + ASSET_HOST + "/assets/www/index.html";
    private static final String EAV_HOST = "orariotreni.eavsrl.it";

    private WebView webView;
    private SwipeRefreshLayout swipe;
    private WebViewAssetLoader assetLoader;
    /** Indirizzo del tabellone che non si è caricato: "trascina per aggiornare" lo riprova. */
    private String failedUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Con targetSdk 35 Android 15 disegna sempre sotto le barre di sistema: facciamo lo
        // stesso anche sulle versioni precedenti e gestiamo noi gli spazi con gli insets.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        swipe = new SwipeRefreshLayout(this);
        swipe.setBackgroundColor(ContextCompat.getColor(this, R.color.board));
        swipe.setColorSchemeColors(ContextCompat.getColor(this, R.color.line));
        webView = new WebView(this);
        swipe.addView(webView);
        setContentView(swipe);

        // Il contenuto sta tra barra di stato, barra di navigazione e notch;
        // dietro le barre si vede lo sfondo scuro del tabellone.
        ViewCompat.setOnApplyWindowInsetsListener(swipe, (v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        // Icone chiare (bianche) sulle barre scure.
        WindowCompat.getInsetsController(getWindow(), swipe).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), swipe).setAppearanceLightNavigationBars(false);

        // https://appassets.androidplatform.net/assets/... -> file in assets/ dell'APK.
        assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        configureWebView();

        // Il gesto parte solo quando la pagina è già in cima, altrimenti scorre la pagina.
        swipe.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        swipe.setOnRefreshListener(() -> {
            if (failedUrl != null) {
                String url = failedUrl;
                failedUrl = null;
                webView.loadUrl(url);
            } else {
                webView.reload();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    failedUrl = null;
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });

        if (savedInstanceState == null || webView.restoreState(savedInstanceState) == null) {
            webView.loadUrl(START_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        // Con più finestre disattivate i link target="_blank" si aprono in questa WebView.
        s.setSupportMultipleWindows(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        // I tabelloni EAV sono pensati per i monitor di stazione e non hanno il meta viewport:
        // come fa Chrome, li impaginiamo larghi e li rimpiccioliamo per farli stare nello
        // schermo, lasciando lo zoom con due dita. Il sito dell'elenco ha il suo viewport
        // (width=device-width), quindi per lui non cambia niente.
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (ASSET_HOST.equals(host) || EAV_HOST.equals(host)) return false;
                openExternal(uri);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!swipe.isRefreshing() && url != null && url.contains(EAV_HOST)) swipe.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipe.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!request.isForMainFrame()) return;
                failedUrl = request.getUrl().toString();
                swipe.setRefreshing(false);
                view.loadDataWithBaseURL(null, offlinePage(), "text/html", "utf-8", failedUrl);
            }
        });
    }

    private String offlinePage() {
        return "<!doctype html><html lang=\"it\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<style>body{margin:0;padding:48px 24px;font-family:sans-serif;background:#1c2630;color:#fff}"
                + "h1{color:#ffc82e;font-size:22px;text-transform:uppercase;margin:0 0 12px}"
                + "p{color:#c3ccd5;font-size:16px;line-height:1.5;margin:0}</style></head><body>"
                + "<h1>" + getString(R.string.offline_title) + "</h1>"
                + "<p>" + getString(R.string.offline_text) + "</p></body></html>";
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }
}
