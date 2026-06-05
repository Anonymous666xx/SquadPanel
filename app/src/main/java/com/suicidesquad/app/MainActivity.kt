package com.suicidesquad.app

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    private lateinit var loginScreen: LinearLayout
    private lateinit var dashboardScreen: LinearLayout
    private lateinit var webScreen: LinearLayout

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var loginError: TextView
    private lateinit var loggedUser: TextView
    private lateinit var logoutBtn: TextView

    private lateinit var webView: WebView
    private lateinit var webProgress: ProgressBar
    private lateinit var webTitle: TextView
    private lateinit var webBackBtn: ImageButton
    private lateinit var webRefreshBtn: ImageButton

    private lateinit var btnHome: LinearLayout
    private lateinit var btnTargets: LinearLayout
    private lateinit var btnTools: LinearLayout
    private lateinit var btnTerminal: LinearLayout
    private lateinit var btnAdmin: LinearLayout

    private var loggedIn = false
    private var currentUrl = "https://suicidesquad.site/"

    private val validUsername = "omr"
    private val validPassword = "omrx15"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        initViews()
        setupLogin()
        setupDashboard()
        setupWebView()
    }

    private fun initViews() {
        loginScreen = findViewById(R.id.loginScreen)
        dashboardScreen = findViewById(R.id.dashboardScreen)
        webScreen = findViewById(R.id.webScreen)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginBtn = findViewById(R.id.loginBtn)
        loginError = findViewById(R.id.loginError)
        loggedUser = findViewById(R.id.loggedUser)
        logoutBtn = findViewById(R.id.logoutBtn)

        webView = findViewById(R.id.webView)
        webProgress = findViewById(R.id.webProgress)
        webTitle = findViewById(R.id.webTitle)
        webBackBtn = findViewById(R.id.webBackBtn)
        webRefreshBtn = findViewById(R.id.webRefreshBtn)

        btnHome = findViewById(R.id.btnHome)
        btnTargets = findViewById(R.id.btnTargets)
        btnTools = findViewById(R.id.btnTools)
        btnTerminal = findViewById(R.id.btnTerminal)
        btnAdmin = findViewById(R.id.btnAdmin)
    }

    private fun setupLogin() {
        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); true } else false
        }
        loginBtn.setOnClickListener { attemptLogin() }
    }

    private fun attemptLogin() {
        val user = usernameInput.text.toString().trim().lowercase()
        val pass = passwordInput.text.toString().trim().lowercase()

        if (user.isEmpty() || pass.isEmpty()) {
            loginError.text = "Fill in both fields"
            loginError.visibility = View.VISIBLE
            return
        }

        if (user != validUsername || pass != validPassword) {
            loginError.text = "Access denied — invalid credentials"
            loginError.visibility = View.VISIBLE
            return
        }

        loginError.visibility = View.GONE
        loggedIn = true
        loggedUser.text = "@$user"
        showDashboard()
    }

    private fun showDashboard() {
        loginScreen.visibility = View.GONE
        webScreen.visibility = View.GONE
        dashboardScreen.visibility = View.VISIBLE
    }

    private fun setupDashboard() {
        btnHome.setOnClickListener { loadInWebView("https://suicidesquad.site/") }
        btnTargets.setOnClickListener { loadInWebView("https://suicidesquad.site/targets") }
        btnTools.setOnClickListener { loadInWebView("https://suicidesquad.site/tools") }
        btnTerminal.setOnClickListener { loadInWebView("https://suicidesquad.site/#terminal") }
        btnAdmin.setOnClickListener { loadAdminPanel() }
        logoutBtn.setOnClickListener {
            loggedIn = false
            webScreen.visibility = View.GONE
            dashboardScreen.visibility = View.GONE
            loginScreen.visibility = View.VISIBLE
            usernameInput.text.clear()
            passwordInput.text.clear()
            loginError.visibility = View.GONE
            webView.removeAllViews()
        }
    }

    private fun loadInWebView(url: String) {
        currentUrl = url
        dashboardScreen.visibility = View.GONE
        webScreen.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun loadAdminPanel() {
        val adminUrl = "https://suicidesquad.site/admin_panel"
        currentUrl = adminUrl
        dashboardScreen.visibility = View.GONE
        webScreen.visibility = View.VISIBLE

        Thread {
            try {
                val json = JSONObject().apply {
                    put("username", validUsername)
                    put("password", validPassword)
                }

                val conn = URL("https://lklkol.itsjust.workers.dev/api/login").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }

                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().readText()
                    val token = JSONObject(responseText).optString("token", "")

                    Handler(Looper.getMainLooper()).post {
                        webView.loadUrl(adminUrl)
                        webView.postDelayed({
                            val js = """
                                javascript:(function() {
                                    localStorage.setItem('admin_token', '$token');
                                    location.reload();
                                })();
                            """.trimIndent()
                            webView.loadUrl(js)
                        }, 800)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        webView.loadUrl(adminUrl)
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    webView.loadUrl(adminUrl)
                }
            }
        }.start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                webProgress.visibility = View.VISIBLE
                webProgress.progress = 0
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                webProgress.visibility = View.GONE
                webTitle.text = url?.replace("https://", "") ?: ""

                // If loading admin panel, try to fill and submit login form
                if (url?.contains("admin_panel") == true) {
                    val js = """
                        javascript:(function() {
                            var inputs = document.querySelectorAll('input');
                            var u, p, f = document.querySelector('form');
                            inputs.forEach(function(i) {
                                var t = i.type.toLowerCase();
                                if (t === 'text' || t === 'email' || t === 'username') u = i;
                                if (t === 'password') p = i;
                            });
                            if (!u || !p) {
                                inputs.forEach(function(i) {
                                    var n = (i.name||'').toLowerCase();
                                    var pid = (i.id||'').toLowerCase();
                                    if (n.indexOf('user') > -1 || n.indexOf('name') > -1 || pid.indexOf('user') > -1 || pid.indexOf('name') > -1) u = i;
                                    if (n.indexOf('pass') > -1 || pid.indexOf('pass') > -1) p = i;
                                });
                            }
                            if (u && p) {
                                u.value = '$validUsername';
                                p.value = '$validPassword';
                            }
                            if (f) { setTimeout(function(){ f.submit(); }, 200); }
                        })();
                    """.trimIndent()
                    webView.postDelayed({ webView.loadUrl(js) }, 800)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                webProgress.progress = newProgress
                if (newProgress == 100) webProgress.visibility = View.GONE
            }
        }

        webBackBtn.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
            else closeWebView()
        }

        webRefreshBtn.setOnClickListener { webView.reload() }
    }

    override fun onBackPressed() {
        when {
            webScreen.isVisible && webView.canGoBack() -> webView.goBack()
            webScreen.isVisible -> closeWebView()
            dashboardScreen.isVisible && loggedIn -> logoutBtn.performClick()
            else -> super.onBackPressed()
        }
    }

    private fun closeWebView() {
        webScreen.visibility = View.GONE
        if (loggedIn) dashboardScreen.visibility = View.VISIBLE
        else loginScreen.visibility = View.VISIBLE
    }
}
