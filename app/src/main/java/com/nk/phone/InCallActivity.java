package com.nk.phone;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.telecom.Call;

public class InCallActivity extends Activity {

    static InCallActivity instance;
    TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        instance = this;

        getWindow().setStatusBarColor(Color.rgb(8,13,25));
        getWindow().setNavigationBarColor(Color.rgb(8,13,25));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(25,25,25,25);
        layout.setBackgroundColor(Color.rgb(8,13,25));

        TextView title = new TextView(this);
        title.setText("NK PHONE");
        title.setTextSize(30);
        title.setTextColor(Color.CYAN);
        layout.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(22);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0,50,0,50);
        layout.addView(status);

        Button answer = new Button(this);
        answer.setText("ANSWER");
        answer.setOnClickListener(v -> {
            Call c = MyInCallService.currentCall;
            if(c != null) {
                c.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY);
                refresh();
            }
        });
        layout.addView(answer);

        Button end = new Button(this);
        end.setText("END CALL");
        end.setTextColor(Color.WHITE);
        end.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(Color.RED)
        );
        end.setOnClickListener(v -> {
            Call c = MyInCallService.currentCall;
            if(c != null) c.disconnect();
            finish();
        });
        layout.addView(end);

        setContentView(layout);
        refresh();
    }

    public static void refresh() {
        if(instance == null) return;

        instance.runOnUiThread(() -> {
            Call c = MyInCallService.currentCall;

            if(c == null) {
                instance.status.setText("Call ended");
                return;
            }

            int state = c.getState();

            String text;

            if(state == Call.STATE_RINGING)
                text = "INCOMING CALL";
            else if(state == Call.STATE_DIALING)
                text = "CALLING...";
            else if(state == Call.STATE_ACTIVE)
                text = "CONNECTED";
            else if(state == Call.STATE_HOLDING)
                text = "CALL ON HOLD";
            else
                text = "CALL STATUS: " + state;

            instance.status.setText(text);
        });
    }

    @Override
    protected void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
