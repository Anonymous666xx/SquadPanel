package com.suicidesquad.app

import android.annotation.SuppressLint
import android.app.AlertDialog
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

    private lateinit var webView: WebView
    private lateinit var webProgress: ProgressBar
    private lateinit var webTitle: TextView
    private lateinit var webBackBtn: ImageButton
    private lateinit var webRefreshBtn: ImageButton

    private var loggedIn = false
    private var currentUrl = "https://suicidesquad.site/"

    private val validUsername = "omr"
    private val validPassword = "omrx15"
    private var adminToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            loginError.text = "Access denied"
            loginError.visibility = View.VISIBLE
            return
        }

        loginError.visibility = View.GONE
        loggedIn = true

        // Fetch admin token in background
        Thread {
            try {
                val json = JSONObject().apply { put("username", validUsername); put("password", validPassword) }
                val conn = URL("https://lklkol.itsjust.workers.dev/api/login").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
                if (conn.responseCode == 200) {
                    adminToken = JSONObject(conn.inputStream.bufferedReader().readText()).optString("token", "")
                }
                conn.disconnect()
            } catch (_: Exception) {}
        }.start()

        showDashboard()
    }

    private fun showDashboard() {
        loginScreen.visibility = View.GONE
        webScreen.visibility = View.GONE
        dashboardScreen.visibility = View.VISIBLE
    }

    private fun setupDashboard() {
        findViewById<LinearLayout>(R.id.btnHome).setOnClickListener { loadUrl("https://suicidesquad.site/") }
        findViewById<LinearLayout>(R.id.btnTargets).setOnClickListener { loadUrl("https://suicidesquad.site/targets") }
        findViewById<LinearLayout>(R.id.btnTools).setOnClickListener { loadUrl("https://suicidesquad.site/tools") }
        findViewById<LinearLayout>(R.id.btnTerminal).setOnClickListener { loadUrl("https://suicidesquad.site/#terminal") }
        findViewById<LinearLayout>(R.id.btnAdmin).setOnClickListener { loadAdminPanel() }
        findViewById<LinearLayout>(R.id.btnAddUser).setOnClickListener { showAddUserDialog() }
        findViewById<LinearLayout>(R.id.btnAddTool).setOnClickListener { showAddToolDialog() }
        findViewById<LinearLayout>(R.id.btnAddTarget).setOnClickListener { showAddTargetDialog() }

        findViewById<TextView>(R.id.logoutBtn).setOnClickListener {
            loggedIn = false
            adminToken = null
            dashboardScreen.visibility = View.GONE
            loginScreen.visibility = View.VISIBLE
            usernameInput.text.clear()
            passwordInput.text.clear()
            loginError.visibility = View.GONE
            webView.removeAllViews()
        }
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        dashboardScreen.visibility = View.GONE
        webScreen.visibility = View.VISIBLE
        webView.loadUrl(url)
    }

    private fun loadAdminPanel() {
        val t = adminToken
        loadUrl(if (!t.isNullOrEmpty()) "https://suicidesquad.site/admin_panel?key=$t" else "https://suicidesquad.site/admin_panel")
    }

    // ───── Native Admin Dialogs ─────

    private fun showAddUserDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 24)
        }
        val nameInput = EditText(this).apply {
            hint = "username"
            setTextColor(0xfff0f0f0.toInt())
            setHintTextColor(0xff660000.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }
        val passInput = EditText(this).apply {
            hint = "password"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(0xfff0f0f0.toInt())
            setHintTextColor(0xff660000.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(32, 16, 32, 16)
        }
        layout.addView(nameInput)
        layout.addView(passInput)

        AlertDialog.Builder(this).apply {
            setTitle("Add User")
            setView(layout)
            setPositiveButton("ADD") { _, _ ->
                val u = nameInput.text.toString().trim()
                val p = passInput.text.toString().trim()
                if (u.isNotEmpty() && p.isNotEmpty()) adminApiPost("/admin/users", JSONObject().apply { put("username", u); put("password", p) })
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun showAddToolDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 24)
        }
        fun makeInput(hint: String, isLast: Boolean = false): EditText {
            val et = EditText(this).apply {
                this.hint = hint
                setTextColor(0xfff0f0f0.toInt())
                setHintTextColor(0xff660000.toInt())
                setBackgroundResource(R.drawable.bg_input)
                setPadding(32, 16, 32, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (!isLast) setMargins(0, 0, 0, 12) }
            }
            return et
        }
        val nameInput = makeInput("tool name")
        val catInput = makeInput("category (Hack Tool, App, RAT, Exploit, Utility)")
        val installInput = makeInput("install command")
        val downloadInput = makeInput("download URL")
        val descInput = makeInput("description")
        layout.addView(nameInput)
        layout.addView(catInput)
        layout.addView(installInput)
        layout.addView(downloadInput)
        layout.addView(descInput)

        AlertDialog.Builder(this).apply {
            setTitle("Add Tool")
            setView(layout)
            setPositiveButton("ADD") { _, _ ->
                val body = JSONObject().apply {
                    put("icon", "??")
                    put("name", nameInput.text.toString().trim())
                    put("category", catInput.text.toString().trim().ifEmpty { "Utility" })
                    put("install", installInput.text.toString().trim())
                    put("download", downloadInput.text.toString().trim())
                    put("description", descInput.text.toString().trim())
                }
                adminApiPost("/admin/tools", body)
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun showAddTargetDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 24)
        }
        val nameInput = EditText(this).apply {
            hint = "target name"
            setTextColor(0xfff0f0f0.toInt())
            setHintTextColor(0xff660000.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(32, 16, 32, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }
        val discordInput = EditText(this).apply {
            hint = "discord ID (numeric)"
            setTextColor(0xfff0f0f0.toInt())
            setHintTextColor(0xff660000.toInt())
            setBackgroundResource(R.drawable.bg_input)
            setPadding(32, 16, 32, 16)
        }
        layout.addView(nameInput)
        layout.addView(discordInput)

        AlertDialog.Builder(this).apply {
            setTitle("Add Target")
            setView(layout)
            setPositiveButton("ADD") { _, _ ->
                val body = JSONObject().apply {
                    put("name", nameInput.text.toString().trim())
                    put("discord", nameInput.text.toString().trim())
                    put("discordId", discordInput.text.toString().trim())
                    put("status", "passive")
                }
                adminApiPost("/admin/targets", body)
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    private fun adminApiPost(path: String, body: JSONObject) {
        val token = adminToken; if (token.isNullOrEmpty()) return
        Thread {
            try {
                val conn = URL("https://lklkol.itsjust.workers.dev$path").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("X-Admin-Token", token)
                conn.doOutput = true
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
                val success = conn.responseCode in 200..299
                conn.disconnect()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, if (success) "Done" else "Failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    // ───── WebView ─────

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

        webBackBtn = findViewById(R.id.webBackBtn)
        webRefreshBtn = findViewById(R.id.webRefreshBtn)

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
            dashboardScreen.isVisible && loggedIn -> {
                loggedIn = false; adminToken = null; dashboardScreen.visibility = View.GONE; loginScreen.visibility = View.VISIBLE
            }
            else -> super.onBackPressed()
        }
    }

    private fun closeWebView() {
        webScreen.visibility = View.GONE
        if (loggedIn) dashboardScreen.visibility = View.VISIBLE
        else loginScreen.visibility = View.VISIBLE
    }
}
