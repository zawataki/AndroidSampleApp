package io.github.zawataki.sampleapplication;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.apache.commons.codec.binary.Hex;

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
         * Cannot use {@link Hex#encodeHexString(byte[])} because
         * {@link NoSuchMethodError} occurs
         *
         * @see <a href="https://qiita.com/komitake/items/375863848e2534d29a87">AndroidでApache Commons Codecを使う時の注意点 - Qiita</a>
         */
        final char[] tagIdHexChars = Hex.encodeHex(tag.getId());
        final String tagIdStr = new String(tagIdHexChars);

        TextView textView = findViewById(R.id.textViewNfcId);

        textView.setText(tagIdStr);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }
}
