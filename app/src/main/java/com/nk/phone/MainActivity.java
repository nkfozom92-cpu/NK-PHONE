package com.nk.phone;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.ViewGroup;

public class MainActivity extends Activity {

    EditText number;
    LinearLayout root;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(Color.rgb(8,13,25));
        getWindow().setNavigationBarColor(Color.rgb(8,13,25));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(25,25,25,25);
        root.setBackgroundColor(Color.rgb(8,13,25));

        TextView title = new TextView(this);
        title.setText("NK PHONE");
        title.setTextSize(32);
        title.setTextColor(Color.rgb(0,212,255));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("PREMIUM CALLING EXPERIENCE");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0,5,0,30);
        root.addView(subtitle);

        number = new EditText(this);
        number.setSingleLine(true);
        number.setTextSize(27);
        number.setGravity(Gravity.CENTER);
        number.setHint("Enter phone number");
        number.setHintTextColor(Color.GRAY);
        number.setTextColor(Color.WHITE);
        number.setInputType(3);
        root.addView(number, new LinearLayout.LayoutParams(
            -1, 75
        ));

        String[][] keys = {
            {"1","2","3"},
            {"4","5","6"},
            {"7","8","9"},
            {"*","0","#"}
        };

        for (String[] row : keys) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER);

            for (String key : row) {
                Button btn = new Button(this);
                btn.setText(key);
                btn.setTextSize(23);
                btn.setTextColor(Color.WHITE);
                btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        Color.rgb(27,39,60)
                    )
                );

                btn.setOnClickListener(v ->
                    number.append(key)
                );

                LinearLayout.LayoutParams p =
                    new LinearLayout.LayoutParams(0,65,1);
                p.setMargins(5,5,5,5);
                line.addView(btn,p);
            }

            root.addView(line);
        }

        Button call = new Button(this);
        call.setText("📞  CALL");
        call.setTextSize(20);
        call.setTextColor(Color.WHITE);
        call.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(0,170,110)
            )
        );

        call.setOnClickListener(v -> makeCall());

        root.addView(call,new LinearLayout.LayoutParams(-1,65));

        Button delete = new Button(this);
        delete.setText("⌫ DELETE");
        delete.setTextColor(Color.WHITE);
        delete.setOnClickListener(v -> {
            int len = number.length();
            if(len > 0) number.setText(
                number.getText().delete(len-1,len)
            );
        });

        root.addView(delete);

        Button defaultBtn = new Button(this);
        defaultBtn.setText("SET AS DEFAULT PHONE APP");
        defaultBtn.setTextColor(Color.WHITE);
        defaultBtn.setOnClickListener(v -> requestDefaultDialer());

        root.addView(defaultBtn);

        setContentView(root);

        if (checkSelfPermission(
            android.Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{android.Manifest.permission.CALL_PHONE},
                101
            );
        }
    }

    private void requestDefaultDialer() {
        RoleManager roleManager =
            getSystemService(RoleManager.class);

        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) {

            if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                startActivityForResult(
                    roleManager.createRequestRoleIntent(
                        RoleManager.ROLE_DIALER
                    ), 100
                );
            } else {
                Toast.makeText(this,
                    "NK PHONE is already default!",
                    Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeCall() {
        String phone = number.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this,
                "Enter a phone number",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkSelfPermission(
            android.Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{android.Manifest.permission.CALL_PHONE},
                101
            );
            return;
        }

        Intent intent = new Intent(
            Intent.ACTION_CALL,
            Uri.fromParts("tel", phone, null)
        );

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                "Unable to make call: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
        }
    }
}
