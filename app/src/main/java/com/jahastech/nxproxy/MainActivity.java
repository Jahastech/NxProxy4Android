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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.jahastech.nxproxy.lib.Lib;
import com.jahastech.nxproxy.lib.LibCtx;
import com.jahastech.nxproxy.lib.NxLog;

import com.jahastech.nxproxy.vpn.AdVpnService;
import com.jahastech.nxproxy.vpn.Command;

//-----------------------------------------------
public class MainActivity extends AppCompatActivity {

    private Config cfg = null;

    private EditText edtServer;
    private EditText edtToken;
    private Button btnSave, btnTest, btnStart;
    private CheckBox chkSendUname, chkPasswdProtection;

    public final int REQUEST_START_VPN = 1;

    private final int PERMISSION_ALL = 13027;
    private String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS};

    //-----------------------------------------------
    public boolean hasPermission() {
        for (String permission : PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    //-----------------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    //LibCtx.toastText(this, "Please allow the requested permission to use the app.");
                    new Handler().postDelayed(() -> finish(), 3000);
                }
                return;
            }
        }
    }

    //-----------------------------------------------
    private boolean save() {
        String server = edtServer.getText().toString();
        if (!Lib.isValidIp(server)) {
            LibCtx.toastText(this, "Invalid server IP!");
            return false;
        }

        String token = edtToken.getText().toString();
        if (Lib.isEmpty(token) || !Lib.isValidToken(token)) {
            LibCtx.toastText(this, "Invalid login token!");
            return false;
        }

        //boolean sendUname = chkSendUname.isChecked();
        boolean passwdProtection = chkPasswdProtection.isChecked();

        cfg.setServer(server);
        cfg.setToken(token);
        //cfg.setSendUname(sendUname);
        cfg.setPasswdProtection(passwdProtection);
        cfg.savePreferences();
        cfg.readPreferences();

        return true;
    }

    //-----------------------------------------------
    private void test() {
        new Thread(() -> {
            int resCode = NxTalkie.getInstance().sendSignalTest();

            runOnUiThread(() -> {
                if (resCode == NxTalkie.INVALID_CONFIG) {
                    LibCtx.toastText(MainActivity.this, "Invalid connection values!");
                } else if (resCode == NxTalkie.CONN_ERR) {
                    LibCtx.toastText(MainActivity.this, "Connection error!");
                } else if (resCode == NxTalkie.LOGIN_ERR) {
                    LibCtx.toastText(MainActivity.this, "Login error!");
                } else if (resCode == NxTalkie.SUCCESS) {
                    LibCtx.toastText(MainActivity.this, "Test succeeded.");
                } else {
                    LibCtx.toastText(MainActivity.this, "Unknown error!");
                }
            });
        }).start();
    }

    //-----------------------------------------------
    private void goToLoginActivity() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //-----------------------------------------------
    private void initApp() {
        // Set cfg.
        cfg = Config.getInstance();

        // Password protection.
        if (cfg.isValid() && cfg.isPasswdProtection() && !cfg.getLoginAlreadyFlag()) {
            goToLoginActivity();
        }
        cfg.setLoginAlreadyFlag(false);

        edtServer = findViewById(R.id.edtServer);
        edtServer.setText(cfg.getServer());

        edtToken = findViewById(R.id.edtToken);
        edtToken.setText(cfg.getToken());

        Toolbar titlebar = findViewById(R.id.titlebar);
        setSupportActionBar(titlebar);
        getSupportActionBar().setTitle(R.string.app_name);

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(view -> save());

        btnTest = findViewById(R.id.btnTest);
        btnTest.setOnClickListener(view -> {
            if (save()) {
                test();
            }
        });

        btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(view -> {
            if (!cfg.isValid()) {
                LibCtx.toastText(this, "Invalid settings!");
                return;
            }
            startStopService();
        });

        /*
        chkSendUname = findViewById(R.id.chkSendUname);
        chkSendUname.setChecked(cfg.isSendUname());
        chkSendUname.setOnClickListener(view -> save());
         */

        chkPasswdProtection = findViewById(R.id.chkPasswdProtection);
        chkPasswdProtection.setChecked(cfg.isPasswdProtection());
        chkPasswdProtection.setOnClickListener(view -> save());

        // Start VPN at startup.
        if (cfg.isValid()) {
            startMyService();
        }

        // Start NxPing.
        NxPing nxPing = NxPing.getInstance();
        nxPing.setContext(this);
        nxPing.start();
    }

    //-----------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Acquire permission.
        if (Build.VERSION.SDK_INT >= 23) {
            if (!hasPermission()) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            }
        }

        // Init app.
        initApp();
    }

    //-----------------------------------------------
    private boolean startStopService() {
        if (AdVpnService.vpnStatus != AdVpnService.VPN_STATUS_STOPPED) {
            NxLog.info("Attempting to disconnect");

            Intent intent = new Intent(this, AdVpnService.class);
            intent.putExtra("COMMAND", Command.STOP.ordinal());
            startService(intent);
        } else {
            new Thread(() -> {
                NxPolicy.getInstance().updatePolicy(true);
            }).start();

            startMyService();
        }
        return true;
    }

    //-----------------------------------------------
    private void startMyService() {
        NxLog.info("Attempting to connect");
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_START_VPN);
        } else {
            onActivityResult(REQUEST_START_VPN, RESULT_OK, null);
        }
    }

    //-----------------------------------------------
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_START_VPN && resultCode == RESULT_CANCELED) {
            LibCtx.toastText(this, String.valueOf(R.string.could_not_configure_vpn_service));
        }
        if (requestCode == REQUEST_START_VPN && resultCode == RESULT_OK) {
            Intent intent = AdVpnService.getStartIntent(this);
            startForegroundService(intent);
        }
    }
}
