package com.justnothing.simplecaller

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.justnothing.simplecaller.ui.theme.MyApplicationTheme
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PhoneDialerScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun PhoneDialerScreen(modifier: Modifier = Modifier) {
    var phoneNumber by remember { mutableStateOf("") }
    var pendingCall by remember { mutableStateOf<String?>(null) }
    var isCalling by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCall != null) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = "tel:${sanitizePhone(pendingCall!!)}".toUri()
            }
            context.startActivity(intent)
            isCalling = true
        }
    }

    val hangupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            endCall(context)
            isCalling = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "拨打电话",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("输入电话号码") },
            placeholder = { Text("例如：11451419198") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val sanitized = sanitizePhone(phoneNumber)
                if (sanitized.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = "tel:$sanitized".toUri()
                    }
                    context.startActivity(intent)
                }
            },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("拨打（打开拨号器）")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val sanitized = sanitizePhone(phoneNumber)
                if (sanitized.isEmpty()) return@Button
                when {
                    ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        val intent = Intent(Intent.ACTION_CALL).apply {
                            data = "tel:$sanitized".toUri()
                        }
                        context.startActivity(intent)
                        isCalling = true
                    }
                    else -> {
                        pendingCall = sanitized
                        callPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
                    }
                }
            },
            enabled = phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("直接拨打")
        }

        if (isCalling) {
            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ANSWER_PHONE_CALLS
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            endCall(context)
                            isCalling = false
                        }
                        else -> {
                            hangupPermissionLauncher.launch(android.Manifest.permission.ANSWER_PHONE_CALLS)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("挂断")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "通话中...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        Text(
            text = "提示：直接拨打和挂断需要授权相应权限",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun endCall(context: Context) {
    try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager::class.java.getMethod("endCall").invoke(telephonyManager)
    } catch (_: Exception) {
    }
}

private fun sanitizePhone(input: String): String {
    return input.filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
}

@Preview(showBackground = true)
@Composable
fun PhoneDialerPreview() {
    MyApplicationTheme {
        PhoneDialerScreen()
    }
}
