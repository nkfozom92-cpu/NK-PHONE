package com.nk.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.telecom.TelecomManager;
import android.telecom.PhoneAccountHandle;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    int bg = Color.rgb(239,241,255);
    int dark = Color.rgb(25,35,65);
    int purple = Color.rgb(105,105,230);
    int green = Color.rgb(0,175,125);

    LinearLayout root, content;
    EditText search;
    String page = "Home";
    String filter = "All";
    String query = "";

    ArrayList<Person> contacts = new ArrayList<>();
    ArrayList<Person> favorites = new ArrayList<>();
    ArrayList<Person> recents = new ArrayList<>();

    static class Person {
        String name, number, date;
        int type;
        Person(String n, String p, String d, int t) {
            name=n; number=p; date=d; type=t;
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        buildShell();
        loadData();

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
            }, 100);
        }
    }

    int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density * 0.92f);
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size * 0.96f);
        t.setTextColor(color);
        return t;
    }

    LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    void buildShell() {
        root = vertical();
        root.setBackgroundColor(bg);
        root.setPadding(0, dp(18), 0, 0);

        LinearLayout header = horizontal();
        header.setPadding(dp(20),dp(18),dp(20),dp(12));

        TextView logo = text("NK",25,purple);
        logo.setTypeface(null,Typeface.BOLD);
        header.addView(logo);

        TextView title = text(" PHONE",23,dark);
        title.setTypeface(null,Typeface.BOLD);
        header.addView(title);

        Space space = new Space(this);
        header.addView(space,new LinearLayout.LayoutParams(0,1,1));

        TextView defaultBtn = text("⚙",25,dark);
        defaultBtn.setOnClickListener(v -> requestDefaultDialer());
        header.addView(defaultBtn);

        root.addView(header);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(15);
        search.setTextColor(dark);
        search.setHintTextColor(Color.GRAY);
        search.setHint("☰   Search contacts or numbers");
        search.setPadding(dp(18),0,dp(18),0);
        search.setBackground(shape(Color.WHITE,35));

        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1,dp(54));
        sp.setMargins(dp(16),dp(5),dp(16),dp(15));
        root.addView(search,sp);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            public void onTextChanged(CharSequence s,int st,int before,int count) {
                query=s.toString().toLowerCase(Locale.ROOT);
                renderContent();
            }
            public void afterTextChanged(Editable e) {}
        });

        ScrollView scroll = new ScrollView(this);
        content = vertical();
        content.setPadding(dp(16),dp(5),dp(16),dp(15));
        scroll.addView(content,new ScrollView.LayoutParams(-1,-2));

        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(10),dp(8),dp(10),dp(8));
        nav.setBackgroundColor(Color.WHITE);

        TextView home = text("⌂\nHome",15,page.equals("Home")?purple:dark);
        home.setGravity(Gravity.CENTER);
        home.setOnClickListener(v -> {
            page="Home";
            renderContent();
            updateNav(home, keypad);
        });

        keypad = text("▦\nKeypad",15,dark);
        keypad.setGravity(Gravity.CENTER);
        keypad.setOnClickListener(v -> {
            page="Keypad";
            renderContent();
            updateNav(home,keypad);
        });

        home.setTag("home");
        keypad.setTag("keypad");

        nav.addView(home,new LinearLayout.LayoutParams(0,dp(55),1));
        nav.addView(keypad,new LinearLayout.LayoutParams(0,dp(55),1));
        root.addView(nav);

        setContentView(root);
        renderContent();
    }

    TextView keypad;

    void updateNav(TextView home, TextView key) {
        home.setTextColor(page.equals("Home")?purple:dark);
        key.setTextColor(page.equals("Keypad")?purple:dark);
    }

    void loadData() {
        contacts.clear();
        favorites.clear();
        recents.clear();

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                Cursor c = getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED
                    },
                    null,null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                );

                if(c != null) {
                    while(c.moveToNext()) {
                        String name=c.getString(0);
                        String number=c.getString(1);
                        int starred=c.getInt(2);
                        if(number == null) continue;

                        Person p = new Person(
                            name==null || name.isEmpty() ? "Unknown" : name,
                            number,"",0
                        );
                        contacts.add(p);
                        if(starred==1 && !containsNumber(favorites,number))
                            favorites.add(p);
                    }
                    c.close();
                }
            } catch(Exception e) {}
        }

        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                == PackageManager.PERMISSION_GRANTED) {
            try {
                Cursor c = getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{
                        CallLog.Calls.CACHED_NAME,
                        CallLog.Calls.NUMBER,
                        CallLog.Calls.DATE,
                        CallLog.Calls.TYPE
                    },
                    null,null,
                    CallLog.Calls.DATE + " DESC"
                );

                if(c != null) {
                    while(c.moveToNext()) {
                        String name=c.getString(0);
                        String number=c.getString(1);
                        long time=c.getLong(2);
                        int type=c.getInt(3);
                        if(number==null) continue;

                        String date = new SimpleDateFormat(
                            "dd MMM, h:mm a",Locale.getDefault()
                        ).format(new Date(time));

                        recents.add(new Person(
                            name==null || name.isEmpty()?number:name,
                            number,date,type
                        ));
                    }
                    c.close();
                }
            } catch(Exception e) {}
        }
    }

    boolean containsNumber(ArrayList<Person> list,String n) {
        for(Person p:list) if(p.number.equals(n)) return true;
        return false;
    }

    void renderContent() {
        if(content==null) return;
        content.removeAllViews();

        if(page.equals("Keypad")) {
            showKeypad();
            return;
        }

        LinearLayout tabs = horizontal();
        String[] names={"All","Missed","Contacts"};

        for(String name:names) {
            TextView chip=text(name,14,filter.equals(name)?Color.WHITE:dark);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(18),dp(10),dp(18),dp(10));
            chip.setBackground(shape(
                filter.equals(name)?purple:Color.WHITE,14
            ));
            chip.setOnClickListener(v -> {
                filter=name;
                renderContent();
            });
            LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(0,dp(43),1);
            cp.setMargins(dp(3),0,dp(3),dp(10));
            tabs.addView(chip,cp);
        }
        content.addView(tabs);

        if(!query.isEmpty()) {
            addHeading("Search results");
            if(filter.equals("Contacts")) showPeople(contacts);
            else showRecent(recents);
            return;
        }

        if(filter.equals("Contacts")) {
            addHeading("My Contacts  •  "+contacts.size());
            showPeople(contacts);
            return;
        }

        if(filter.equals("All")) {
            addHeading("★  Favorites");
            showFavorites();
        }

        addHeading(filter.equals("Missed")?"Missed Calls":"Recent Calls");

        if(filter.equals("Missed")) {
            ArrayList<Person> missed=new ArrayList<>();
            for(Person p:recents)
                if(p.type==CallLog.Calls.MISSED_TYPE) missed.add(p);
            showRecent(missed);
        } else {
            showRecent(recents);
        }
    }

    void addHeading(String s) {
        TextView h=text(s,18,dark);
        h.setTypeface(null,Typeface.BOLD);
        h.setPadding(dp(4),dp(12),0,dp(12));
        content.addView(h);
    }

    void showFavorites() {
        if(favorites.isEmpty()) {
            TextView t=text("No favorite contacts yet",13,Color.GRAY);
            content.addView(t);
            return;
        }

        HorizontalScrollView hsv=new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row=horizontal();

        int count=0;
        for(Person p:favorites) {
            if(!matches(p)) continue;
            LinearLayout item=vertical();
            item.setGravity(Gravity.CENTER);
            TextView avatar=text(initial(p.name),22,dark);
            avatar.setGravity(Gravity.CENTER);
            avatar.setBackground(shape(
                new int[]{0xffa9e8f5,0xffffc1df,0xffffd66b,0xffc9c5ff}[count++%4],50
            ));
            item.addView(avatar,new LinearLayout.LayoutParams(dp(62),dp(62)));

            TextView name=text(p.name,12,dark);
            name.setGravity(Gravity.CENTER);
            name.setMaxWidth(dp(78));
            name.setMaxLines(1);
            item.addView(name);

            item.setOnClickListener(v -> makeCall(p.number));
            LinearLayout.LayoutParams ip =
                new LinearLayout.LayoutParams(dp(82),dp(105));
            row.addView(item,ip);
        }
        hsv.addView(row);
        content.addView(hsv);
    }

    String initial(String s) {
        if(s==null || s.isEmpty()) return "?";
        return s.substring(0,1).toUpperCase(Locale.ROOT);
    }

    boolean matches(Person p) {
        return query.isEmpty() ||
            p.name.toLowerCase(Locale.ROOT).contains(query) ||
            p.number.toLowerCase(Locale.ROOT).contains(query);
    }

    void showPeople(ArrayList<Person> list) {
        int count=0;
        for(Person p:list) {
            if(!matches(p)) continue;
            addPersonRow(p,count++);
        }
        if(count==0) addEmpty("No contacts found");
    }

    void showRecent(ArrayList<Person> list) {
        int count=0;
        for(Person p:list) {
            if(!matches(p)) continue;
            addPersonRow(p,count++);
            if(count>=100) break;
        }
        if(count==0) addEmpty(
            checkSelfPermission(Manifest.permission.READ_CALL_LOG)
            ==PackageManager.PERMISSION_GRANTED
            ?"No call records found"
            :"Allow Call Log permission after setting NK PHONE as default"
        );
    }

    void addPersonRow(Person p,int index) {
        LinearLayout card=horizontal();
        card.setPadding(dp(12),dp(12),dp(12),dp(12));
        card.setBackground(shape(Color.WHITE,17));

        TextView avatar=text(initial(p.name),20,dark);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(0xffe1e5f5,40));
        card.addView(avatar,new LinearLayout.LayoutParams(dp(48),dp(48)));

        LinearLayout info=vertical();
        info.setPadding(dp(12),0,0,0);

        TextView name=text(p.name,15,dark);
        name.setTypeface(null,Typeface.BOLD);
        info.addView(name);

        TextView number=text(p.number,14,0xff555b70);
        info.addView(number);

        if(!p.date.isEmpty()) {
            String arrow = p.type==CallLog.Calls.MISSED_TYPE ? "↙ " : "↗ ";
            int color = p.type==CallLog.Calls.MISSED_TYPE
                ? Color.rgb(190,55,65) : 0xff777d90;
            TextView detail=text(arrow+p.date,12,color);
            info.addView(detail);
        }

        card.addView(info,new LinearLayout.LayoutParams(0,-2,1));

        TextView call=text("☎",23,green);
        call.setGravity(Gravity.CENTER);
        card.addView(call,new LinearLayout.LayoutParams(dp(42),dp(48)));

        card.setOnClickListener(v -> makeCall(p.number));
        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle(p.name)
                .setMessage(p.number)
                .setPositiveButton("Call",(d,w)->makeCall(p.number))
                .setNegativeButton("Close",null)
                .show();
            return true;
        });

        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,dp(3),0,dp(7));
        content.addView(card,lp);
    }

    void addEmpty(String message) {
        TextView t=text(message,14,Color.GRAY);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10),dp(30),dp(10),dp(30));
        content.addView(t);
    }


    EditText dialInput;

    void showKeypad() {
        TextView heading=text("Dial a number",23,dark);
        heading.setTypeface(null,Typeface.BOLD);
        heading.setPadding(0,dp(15),0,dp(20));
        content.addView(heading);

        dialInput=new EditText(this);
        dialInput.setSingleLine(true);
        dialInput.setTextSize(25);
        dialInput.setGravity(Gravity.CENTER);
        dialInput.setHint("Enter phone number");
        dialInput.setInputType(3);
        dialInput.setTextColor(dark);
        dialInput.setBackground(shape(Color.WHITE,15));
        content.addView(dialInput,new LinearLayout.LayoutParams(-1,dp(58)));

        String[][] keys={
            {"1","2","3"},{"4","5","6"},
            {"7","8","9"},{"*","0","#"}
        };

        for(String[] r:keys) {
            LinearLayout row=horizontal();
            for(String k:r) {
                Button btn=new Button(this);
                btn.setText(k);
                btn.setTextSize(22);
                btn.setTextColor(dark);
                btn.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.WHITE)
                );
                btn.setOnClickListener(v->dialInput.append(k));
                LinearLayout.LayoutParams bp =
                    new LinearLayout.LayoutParams(0,dp(62),1);
                bp.setMargins(dp(4),dp(4),dp(4),dp(4));
                row.addView(btn,bp);
            }
            content.addView(row);
        }

        Button call=new Button(this);
        call.setText("☎   CALL • CHOOSE SIM");
        call.setTextColor(Color.WHITE);
        call.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(green)
        );
        call.setOnClickListener(v->makeCall(dialInput.getText().toString()));
        content.addView(call,new LinearLayout.LayoutParams(-1,dp(55)));

        Button save=new Button(this);
        save.setText("＋   SAVE CONTACT");
        save.setTextColor(Color.WHITE);
        save.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(purple)
        );
        save.setOnClickListener(v->saveContact(dialInput.getText().toString()));
        content.addView(save,new LinearLayout.LayoutParams(-1,dp(52)));

        Button del=new Button(this);
        del.setText("⌫  DELETE");
        del.setOnClickListener(v->{
            int n=dialInput.length();
            if(n>0) dialInput.getText().delete(n-1,n);
        });
        content.addView(del);
    }

    void saveContact(String phone) {
        if(phone==null || phone.trim().isEmpty()) {
            Toast.makeText(this,"আগে একটি নম্বর লিখুন",Toast.LENGTH_SHORT).show();
            return;
        }

        EditText nameInput=new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setHint("Contact name");
        nameInput.setTextColor(dark);

        new AlertDialog.Builder(this)
            .setTitle("Save new contact")
            .setMessage("নাম লিখুন: "+phone)
            .setView(nameInput)
            .setPositiveButton("Continue",(dialog,which)->{
                String name=nameInput.getText().toString().trim();
                if(name.isEmpty()) {
                    Toast.makeText(this,"নাম লিখুন",Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Intent intent=new Intent(Intent.ACTION_INSERT);
                    intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                    intent.putExtra(ContactsContract.Intents.Insert.NAME,name);
                    intent.putExtra(ContactsContract.Intents.Insert.PHONE,phone.trim());
                    startActivity(intent);
                } catch(Exception e) {
                    Toast.makeText(this,"Contacts app পাওয়া যায়নি",Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel",null)
            .show();
    }

    void makeCall(String phone) {
        if(phone==null || phone.trim().isEmpty()) {
            Toast.makeText(this,"Phone number is empty",Toast.LENGTH_SHORT).show();
            return;
        }

        if(checkSelfPermission(Manifest.permission.CALL_PHONE)
                !=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE},101);
            return;
        }

        try {
            TelecomManager telecom=(TelecomManager)getSystemService(TELECOM_SERVICE);
            java.util.List<PhoneAccountHandle> accounts =
                telecom==null ? null : telecom.getCallCapablePhoneAccounts();

            if(accounts==null || accounts.isEmpty()) {
                placeCall(phone.trim(),null);
                return;
            }

            if(accounts.size()==1) {
                placeCall(phone.trim(),accounts.get(0));
                return;
            }

            String[] labels=new String[accounts.size()];
            for(int i=0;i<accounts.size();i++) {
                android.telecom.PhoneAccount account =
                    telecom.getPhoneAccount(accounts.get(i));
                labels[i]=account!=null
                    ? account.getLabel().toString()
                    : "SIM "+(i+1);
            }

            new AlertDialog.Builder(this)
                .setTitle("কোন SIM দিয়ে কল করবেন?")
                .setItems(labels,(dialog,which)->
                    placeCall(phone.trim(),accounts.get(which)))
                .setNegativeButton("Cancel",null)
                .show();

        } catch(SecurityException e) {
            Toast.makeText(this,
                "SIM selection permission denied",
                Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    void placeCall(String phone,PhoneAccountHandle account) {
        try {
            Intent intent=new Intent(
                Intent.ACTION_CALL,
                Uri.fromParts("tel",phone,null)
            );
            if(account!=null) {
                intent.putExtra(
                    TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE,account
                );
            }
            startActivity(intent);
        } catch(Exception e) {
            Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    void requestDefaultDialer() {
        RoleManager rm=getSystemService(RoleManager.class);
        if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
            if(!rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                startActivityForResult(
                    rm.createRequestRoleIntent(RoleManager.ROLE_DIALER),100
                );
            } else {
                Toast.makeText(this,"NK PHONE is default",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        loadData();
        renderContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(root!=null) {
            loadData();
            renderContent();
        }
    }
}
