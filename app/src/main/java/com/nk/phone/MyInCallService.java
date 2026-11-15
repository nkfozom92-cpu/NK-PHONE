package com.nk.phone;

import android.telecom.InCallService;
import android.telecom.Call;
import android.content.Intent;

public class MyInCallService extends InCallService {

    public static Call currentCall;

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);

        currentCall = call;

        call.registerCallback(new Call.Callback() {
            @Override
            public void onStateChanged(Call c, int state) {
                InCallActivity.refresh();
            }
        });

        Intent intent = new Intent(this, InCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);

        if (currentCall == call) {
            currentCall = null;
        }

        InCallActivity.refresh();
    }
}
