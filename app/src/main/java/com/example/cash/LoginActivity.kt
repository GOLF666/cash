package com.example.cash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // 你的 layout

        // 關閉按鈕
        findViewById<ImageView>(R.id.iv_close).setOnClickListener {
            finish()
        }

        // 設定 Google SignInOption
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail() // 要拿到email
            .requestProfile()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 登入按鈕
        val loginBtn = findViewById<TextView>(R.id.tv_google_login).parent as LinearLayout
        loginBtn.setOnClickListener {
            // 每次都 signOut 先，把 session 清掉，再 signIn
            googleSignInClient.signOut().addOnCompleteListener {
                signIn()
            }
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // 登入成功，回傳資料
            val avatarUrl = account.photoUrl?.toString() ?: ""
            val userName = account.displayName ?: ""
            val email = account.email ?: ""
            val resultIntent = Intent().apply {
                putExtra("user_avatar", avatarUrl)
                putExtra("user_name", userName)
                putExtra("user_email", email)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: ApiException) {
            Toast.makeText(this, "登入失敗: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }
}
