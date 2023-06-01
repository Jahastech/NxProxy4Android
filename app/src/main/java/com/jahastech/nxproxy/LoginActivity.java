/* Written by Jahastech (devel@jahastech.com).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Contributions shall also be provided under any later versions of the
 * GPL.
 */
package com.jahastech.nxproxy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.LibCtx;
import com.jahastech.nxproxy.lib.NxLog;
import com.jahastech.nxproxy.vpn.AdVpnService;
import com.jahastech.nxproxy.vpn.Command;

import java.util.Locale;

//-----------------------------------------------
public class LoginActivity extends AppCompatActivity {

    private Config cfg = null;

    private EditText edtPasswd;
    private Button btnLogin, btnRestart;

    //-----------------------------------------------
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    //-----------------------------------------------
    private void restart() {
        new Thread(() -> {
            NxLog.info("Restarting VPN.");

            // Stop the VPN service if it is running
            if (AdVpnService.vpnStatus != AdVpnService.VPN_STATUS_STOPPED) {
                Intent intent = new Intent(this, AdVpnService.class);
                intent.putExtra("COMMAND", Command.STOP.ordinal());
                startService(intent);
            }

            Lib.sleep(1000 * 2);

            // Start the VPN service if it is not running
            if (AdVpnService.vpnStatus == AdVpnService.VPN_STATUS_STOPPED) {
                Intent intent = AdVpnService.getStartIntent(this);
                startForegroundService(intent);
            }
        }).start();
    }

    //-----------------------------------------------
    private void initApp() {
        // Set cfg.
        cfg = Config.getInstance();

        edtPasswd = findViewById(R.id.edtPasswd);

        btnLogin = findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(view -> {
            String passwd = edtPasswd.getText().toString().toUpperCase(Locale.ROOT);

            // Check if the password is correct
            if (passwd.equals(cfg.getToken())) {
                cfg.setLoginAlreadyFlag(true);
                goToMainActivity();
            } else {
                // Password is incorrect, show an error message
                LibCtx.toastText(LoginActivity.this, "Incorrect password");
            }
        });

        btnRestart = findViewById(R.id.btnRestart);
        btnRestart.setOnClickListener(view -> {
            restart();
        });
    }

    //-----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initApp();
    }
}

