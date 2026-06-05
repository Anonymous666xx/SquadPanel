package com.suicidesquad.app

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.app.AlertDialog
import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    private val WORKER_URL = "https://lklkol.itsjust.workers.dev"
    private val SITE_URL = "https://suicidesquad.site"

    private lateinit var loginScreen: View
    private lateinit var dashboardScreen: View
    private lateinit var webviewScreen: View

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginBtn: Button
    private lateinit var errorText: TextView
    private lateinit var userLabel: TextView

    private lateinit var logoutBtn: Button
    private lateinit var browseSiteBtn: Button
    private lateinit var adminPanelBtn: Button
    private lateinit var membersBtn: Button
    private lateinit var requestsBtn: Button
    private lateinit var targetsBtn: Button
    private lateinit var toolsBtn: Button

    private lateinit var terminalOutput: TextView
    private lateinit var terminalInput: EditText
    private lateinit var terminalSendBtn: Button

    private lateinit var webView: WebView
    private lateinit var backBtn: Button
    private lateinit var refreshBtn: Button
    private lateinit var homeBtn: Button

    private var adminToken: String? = null
    private var currentUser: String = ""
    private var currentUrl: String = SITE_URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupWebView()
        setupListeners()
    }

    private fun bindViews() {
        loginScreen = findViewById(R.id.loginScreen)
        dashboardScreen = findViewById(R.id.dashboardScreen)
        webviewScreen = findViewById(R.id.webviewScreen)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginBtn = findViewById(R.id.loginBtn)
        errorText = findViewById(R.id.errorText)
        userLabel = findViewById(R.id.userLabel)

        logoutBtn = findViewById(R.id.logoutBtn)
        browseSiteBtn = findViewById(R.id.browseSiteBtn)
        adminPanelBtn = findViewById(R.id.adminPanelBtn)
        membersBtn = findViewById(R.id.membersBtn)
        requestsBtn = findViewById(R.id.requestsBtn)
        targetsBtn = findViewById(R.id.targetsBtn)
        toolsBtn = findViewById(R.id.toolsBtn)

        terminalOutput = findViewById(R.id.terminalOutput)
        terminalInput = findViewById(R.id.terminalInput)
        terminalSendBtn = findViewById(R.id.terminalSendBtn)

        webView = findViewById(R.id.webView)
        backBtn = findViewById(R.id.backBtn)
        refreshBtn = findViewById(R.id.refreshBtn)
        homeBtn = findViewById(R.id.homeBtn)
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = false
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.settings, WebSettingsCompat.FORCE_DARK_ON)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                url?.let { currentUrl = it }
            }
        }
        webView.webChromeClient = WebChromeClient()
    }

    private fun setupListeners() {
        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                doLogin()
                true
            } else false
        }

        loginBtn.setOnClickListener { doLogin() }
        logoutBtn.setOnClickListener { doLogout() }
        browseSiteBtn.setOnClickListener { loadUrl(SITE_URL) }
        adminPanelBtn.setOnClickListener { loadAdminPanel() }
        membersBtn.setOnClickListener { showMembersDialog() }
        requestsBtn.setOnClickListener { showRequestsDialog() }
        targetsBtn.setOnClickListener { loadUrl("$SITE_URL/targets") }
        toolsBtn.setOnClickListener { loadUrl("$SITE_URL/tools") }

        terminalSendBtn.setOnClickListener { sendTerminalCommand() }
        terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTerminalCommand()
                true
            } else false
        }

        backBtn.setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        refreshBtn.setOnClickListener { webView.reload() }
        homeBtn.setOnClickListener { showScreen("dashboard") }
    }

    private fun doLogin() {
        val user = usernameInput.text.toString().trim()
        val pass = passwordInput.text.toString().trim()
        if (user.isEmpty() || pass.isEmpty()) {
            showError("Fill in all fields")
            return
        }

        loginBtn.isEnabled = false
        loginBtn.text = "AUTHENTICATING..."
        errorText.visibility = View.GONE

        Thread {
            try {
                val url = URL("$WORKER_URL/api/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val body = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                }
                OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                val code = conn.responseCode
                val responseText = if (code in 200..299) {
                    BufferedReader(InputStreamReader(conn.inputStream)).readText()
                } else {
                    BufferedReader(InputStreamReader(conn.errorStream)).readText()
                }

                runOnUiThread {
                    loginBtn.isEnabled = true
                    loginBtn.text = "SIGN IN"

                    if (code in 200..299) {
                        val json = JSONObject(responseText)
                        adminToken = json.optString("token")
                        currentUser = json.optString("username", user)

                        if (adminToken.isNullOrEmpty()) {
                            showError("No token received")
                            return@runOnUiThread
                        }

                        userLabel.text = "☠ $currentUser"
                        showScreen("dashboard")
                        terminalPrint("Authenticated as $currentUser")
                    } else {
                        try {
                            val errJson = JSONObject(responseText)
                            showError(errJson.optString("error", "Login failed ($code)"))
                        } catch (_: Exception) {
                            showError("Login failed ($code)")
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    loginBtn.isEnabled = true
                    loginBtn.text = "SIGN IN"
                    showError("Connection error: ${e.message}")
                }
            }
        }.start()
    }

    private fun doLogout() {
        adminToken = null
        currentUser = ""
        usernameInput.setText("")
        passwordInput.setText("")
        terminalOutput.text = ""
        errorText.visibility = View.GONE
        showScreen("login")
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = View.VISIBLE
    }

    private fun showScreen(screen: String) {
        loginScreen.visibility = if (screen == "login") View.VISIBLE else View.GONE
        dashboardScreen.visibility = if (screen == "dashboard") View.VISIBLE else View.GONE
        webviewScreen.visibility = if (screen == "webview") View.VISIBLE else View.GONE
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        webView.loadUrl(url)
        showScreen("webview")
    }

    private fun loadAdminPanel() {
        val token = adminToken
        if (token != null) {
            loadUrl("$SITE_URL/admin_panel?key=$token")
        } else {
            loadUrl("$SITE_URL/admin_panel")
        }
    }

    override fun onBackPressed() {
        if (webviewScreen.visibility == View.VISIBLE) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                showScreen("dashboard")
            }
        } else {
            super.onBackPressed()
        }
    }

    // ─── TERMINAL ──────────────────────────────────────────

    private var termState = "idle"
    private var termUser = ""
    private var termTempUser = ""

    private fun terminalPrint(text: String) {
        runOnUiThread {
            terminalOutput.append("$text\n")
            val scroll = terminalOutput.layout
            if (scroll != null) {
                val line = scroll.lineCount - 1
                val y = scroll.getLineBottom(line) - scroll.height
                if (y > 0) terminalOutput.scrollTo(0, y)
            }
        }
    }

    private fun sendTerminalCommand() {
        val cmd = terminalInput.text.toString().trim()
        terminalInput.setText("")
        if (cmd.isEmpty()) return

        terminalPrint("guest@squad:~$ $cmd")

        when (termState) {
            "idle" -> {
                when (cmd.lowercase()) {
                    "login", "admin" -> {
                        termState = "await_user"
                        terminalPrint("username:")
                    }
                    "clear" -> terminalOutput.text = ""
                    "help" -> {
                        terminalPrint("Commands: login, admin, clear, help")
                    }
                    else -> {
                        terminalPrint("Unknown command. Type 'help'")
                    }
                }
            }
            "await_user" -> {
                termTempUser = cmd
                termState = "await_pass"
                terminalPrint("password:")
            }
            "await_pass" -> {
                val user = termTempUser
                termState = "idle"
                terminalPrint("Authenticating...")

                Thread {
                    try {
                        val url = URL("$WORKER_URL/api/login")
                        val conn = url.openConnection() as HttpURLConnection
                        conn.requestMethod = "POST"
                        conn.doOutput = true
                        conn.setRequestProperty("Content-Type", "application/json")
                        conn.connectTimeout = 15000
                        conn.readTimeout = 15000

                        val body = JSONObject().apply {
                            put("username", user)
                            put("password", cmd)
                        }
                        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }

                        val code = conn.responseCode
                        val responseText = if (code in 200..299) {
                            BufferedReader(InputStreamReader(conn.inputStream)).readText()
                        } else {
                            BufferedReader(InputStreamReader(conn.errorStream)).readText()
                        }

                        runOnUiThread {
                            if (code in 200..299) {
                                val json = JSONObject(responseText)
                                adminToken = json.optString("token")
                                currentUser = json.optString("username", user)
                                userLabel.text = "☠ $currentUser"
                                terminalPrint("\u2714 Authenticated as $currentUser")
                            } else {
                                try {
                                    val errJson = JSONObject(responseText)
                                    terminalPrint("\u2718 ${errJson.optString("error", "Failed")}")
                                } catch (_: Exception) {
                                    terminalPrint("\u2718 Auth failed ($code)")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            terminalPrint("\u2718 Error: ${e.message}")
                        }
                    }
                }.start()
            }
        }
    }

    // ─── ADMIN API ──────────────────────────────────────────

    private fun apiFetch(method: String, path: String, body: String? = null): String? {
        val token = adminToken ?: return null
        try {
            val url = URL("$WORKER_URL$path")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Admin-Token", token)
            conn.connectTimeout = 15000
            conn.readTimeout = 15000

            if (body != null) {
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(body) }
            }

            val code = conn.responseCode
            return if (code in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).readText()
            } else {
                val err = BufferedReader(InputStreamReader(conn.errorStream)).readText()
                throw Exception("HTTP $code: $err")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun showMembersDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("MEMBERS")
            .setMessage("Loading...")
            .setCancelable(true)
            .setPositiveButton("ADD") { _, _ -> showAddMemberDialog() }
            .setNegativeButton("CLOSE", null)
            .create()

        dialog.show()

        Thread {
            try {
                val result = apiFetch("GET", "/admin/members")
                runOnUiThread {
                    if (result != null) {
                        try {
                            val arr = JSONArray(result)
                            val sb = StringBuilder()
                            for (i in 0 until arr.length()) {
                                val m = arr.getJSONObject(i)
                                sb.append("${m.optString("id","?")} | ${m.optString("name","?")} | ${m.optString("role","?")}\n")
                            }
                            if (sb.isEmpty()) sb.append("No members found")
                            dialog.setMessage(sb.toString().trim())
                        } catch (_: Exception) {
                            dialog.setMessage(result.take(500))
                        }
                    } else {
                        dialog.setMessage("Not authenticated")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { dialog.setMessage("Error: ${e.message}") }
            }
        }.start()
    }

    private fun showAddMemberDialog() {
        val inflater = layoutInflater
        val view = inflater.inflate(android.R.layout.simple_list_item_1, null) as TextView
        view.visibility = View.GONE

        val inputName = EditText(this).apply {
            setHint("Name")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
        }
        val inputNick = EditText(this).apply {
            setHint("Nickname/Handle")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
        }
        val inputAge = EditText(this).apply {
            setHint("Age")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val inputDiscord = EditText(this).apply {
            setHint("Discord")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
        }
        val inputRole = EditText(this).apply {
            setHint("Role")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 16, 32, 16)
            addView(inputName)
            addView(inputNick)
            addView(inputAge)
            addView(inputDiscord)
            addView(inputRole)
            for (i in 0 until childCount) {
                getChildAt(i).let {
                    val lp = it.layoutParams
                    if (lp is LinearLayout.LayoutParams) {
                        lp.setMargins(0, 0, 0, 8)
                        it.layoutParams = lp
                    }
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("ADD MEMBER")
            .setView(layout)
            .setPositiveButton("ADD") { _, _ ->
                val name = inputName.text.toString().trim()
                val nick = inputNick.text.toString().trim()
                val age = inputAge.text.toString().trim()
                val discord = inputDiscord.text.toString().trim()
                val role = inputRole.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Thread {
                    try {
                        val body = JSONObject().apply {
                            put("name", name)
                            put("nick", nick)
                            put("age", age)
                            put("discord", discord)
                            put("role", role)
                        }
                        apiFetch("POST", "/admin/members", body.toString())
                        runOnUiThread { Toast.makeText(this, "Member added", Toast.LENGTH_SHORT).show() }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                }.start()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showRequestsDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("JOIN REQUESTS")
            .setMessage("Loading...")
            .setCancelable(true)
            .setNegativeButton("CLOSE", null)
            .create()

        dialog.show()

        Thread {
            try {
                val result = apiFetch("GET", "/admin/requests")
                runOnUiThread {
                    if (result != null) {
                        try {
                            val arr = JSONArray(result)
                            if (arr.length() == 0) {
                                dialog.setMessage("No pending requests")
                                return@runOnUiThread
                            }

                            val sb = StringBuilder()
                            for (i in 0 until arr.length()) {
                                val r = arr.getJSONObject(i)
                                val id = r.optString("id", "?")
                                val name = r.optString("name", r.optString("username", "?"))
                                sb.append("[$id] $name\n")
                            }
                            dialog.setMessage(sb.toString().trim())

                            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "APPROVE") { _, _ ->
                                showRequestActionDialog("approve")
                            }
                            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "REJECT") { _, _ ->
                                showRequestActionDialog("reject")
                            }
                        } catch (_: Exception) {
                            dialog.setMessage(result.take(500))
                        }
                    } else {
                        dialog.setMessage("Not authenticated")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { dialog.setMessage("Error: ${e.message}") }
            }
        }.start()
    }

    private fun showRequestActionDialog(action: String) {
        val input = EditText(this).apply {
            setHint("Request ID")
            setTextColor(-0x10101)
            setHintTextColor(-0x99999a)
            setBackgroundDrawable(resources.getDrawable(R.drawable.bg_input, theme))
            setPadding(16, 12, 16, 12)
        }

        AlertDialog.Builder(this)
            .setTitle(if (action == "approve") "APPROVE REQUEST" else "REJECT REQUEST")
            .setView(input)
            .setPositiveButton("CONFIRM") { _, _ ->
                val id = input.text.toString().trim()
                if (id.isEmpty()) {
                    Toast.makeText(this, "Enter an ID", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                Thread {
                    try {
                        if (action == "approve") {
                            apiFetch("POST", "/admin/requests/$id/approve")
                        } else {
                            apiFetch("DELETE", "/admin/requests/$id")
                        }
                        runOnUiThread { Toast.makeText(this, "Request $action'd", Toast.LENGTH_SHORT).show() }
                    } catch (e: Exception) {
                        runOnUiThread { Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                }.start()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}
