package io.github.zawataki.sampleapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import trikita.log.Log;

/**
 * @see <a href="https://developer.android.com/reference/android/nfc/package-summary">
 * android.nfc  |  Android Developers</a>
 * @see <a href="https://github.com/nasneg/ReadNFC/blob/master/src/me/gensan/android/readnfc/SampleNFCActivity.java">
 * ReadNFC/SampleNFCActivity.java</a>
 * @see <a href="https://qiita.com/nshiba/items/38f94d61c020a17314b6">
 * AndroidでFelica(NFC)のブロックデータの取得 - Qiita</a>
 * @see <a href="https://developer.android.com/guide/topics/connectivity/nfc/nfc">
 * NFC basics  |  Android Developers</a>
 */
public class NfcReaderActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private IntentFilter[] intentFilterArray;
    private String[][] techListArray;
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_reader);

        pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(
                        Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter intentFilter =
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            intentFilter.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        intentFilterArray = new IntentFilter[]{intentFilter};

        techListArray = new String[][]{
                new String[]{IsoDep.class.getName()},
                new String[]{NfcA.class.getName()},
                new String[]{NfcB.class.getName()},
                new String[]{NfcF.class.getName()},
                new String[]{NfcV.class.getName()},
                new String[]{Ndef.class.getName()},
                new String[]{NdefFormatable.class.getName()},
                new String[]{MifareClassic.class.getName()},
                new String[]{MifareUltralight.class.getName()}
        };

        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());

        preferences = getPreferences(MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Enable reading NFC
        nfcAdapter.enableForegroundDispatch(this, pendingIntent,
                intentFilterArray, techListArray);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            return;
        }

        /**
         * Cannot use org.apache.commons.codec.binary.Hex#encodeHexString(byte[])
         * because {@link NoSuchMethodError} occurs
         *
         * @see <a href="https://qiita.com/komitake/items/375863848e2534d29a87">AndroidでApache Commons Codecを使う時の注意点 - Qiita</a>
         */
        final byte[] tagIdBytes = tag.getId();
        String tagIdStr = "";
        for (byte b : tagIdBytes) {
            tagIdStr += String.format("%02X", b);
        }

        setValueToTextView(R.id.textViewNfcId, tagIdStr);

        final String username = getUsernameByTagId(tagIdStr);
        if (username == null) {
            Log.i("User is NOT registered for tag: " + tagIdStr);
            setValueToTextView(R.id.textViewUserSearchResut,
                    "You are not registered. Please register");
            setValueToTextView(R.id.textViewUsername, "");
            findViewById(R.id.editTextUsername).setVisibility(View.VISIBLE);
            findViewById(R.id.buttonUserRegistration).setVisibility(View.VISIBLE);
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(findViewById(R.id.editTextUsername),
                    InputMethodManager.SHOW_IMPLICIT);
        } else {
            Log.i("User is already registered. username: " + username);
            setValueToTextView(R.id.textViewUserSearchResut, "Existing user");
            setValueToTextView(R.id.textViewUsername, username);
        }
    }

    private void setValueToTextView(@IdRes int resourceId, String value) {
        TextView textView = findViewById(resourceId);
        textView.setText(value);
    }

    private void setValueAndColorToTextView(@IdRes int resourceId, String value, int color) {
        TextView textView = findViewById(resourceId);
        textView.setText(value);
        textView.setTextColor(color);
    }

    private String getUsernameByTagId(String tagId) {
        return preferences.getString(tagId, null);
    }

    public void registerUser(View view) {
        final EditText editTextUsername = findViewById(R.id.editTextUsername);
        final String newUsername = editTextUsername.getText().toString();

        if (StringUtils.isBlank(newUsername)) {
            setValueAndColorToTextView(R.id.textViewUserSearchResut,
                    "Username is blank. Please input valid username", Color.RED);
            setValueToTextView(R.id.editTextUsername, "");

            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(findViewById(R.id.editTextUsername),
                    InputMethodManager.SHOW_IMPLICIT);
            return;
        }

        TextView textView = findViewById(R.id.textViewNfcId);
        final String tagId = textView.getText().toString();
        preferences.edit().putString(tagId, newUsername).commit();
        setValueToTextView(R.id.editTextUsername, "");
        findViewById(R.id.editTextUsername).setVisibility(View.INVISIBLE);
        findViewById(R.id.buttonUserRegistration).setVisibility(View.INVISIBLE);
        setValueToTextView(R.id.textViewUserSearchResut,
                "Hi " + newUsername + ",\nThank you for registration");
        Log.i("User registration is successful. tagId: " + tagId + ". username: " + newUsername);
    }

    public void deleteAllUsers(View view) {
        preferences.edit().clear().commit();
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }
}
