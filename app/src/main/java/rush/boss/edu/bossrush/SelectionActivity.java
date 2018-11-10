package rush.boss.edu.bossrush;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class SelectionActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    ToggleButton tglReadWrite;
    EditText textTagContent;

    TextView heroSpot;
    TextView monSpot;
    TextView envSpot;

    Button startButton;

    Intent toBattle;
    Bundle battleData = new Bundle();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_screen);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        tglReadWrite = findViewById(R.id.tglReadWrite);
        textTagContent = findViewById(R.id.txtTagContent);
        heroSpot = findViewById(R.id.heroSelect);
        monSpot = findViewById(R.id.monSelect);
        envSpot = findViewById(R.id.envSelect);


        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(heroSpot.getText().toString().contains("HER") )battleData.putString("HERO", heroSpot.getText().toString());
                else battleData.putString("HERO", "HER00001");
                if(monSpot.getText().toString().contains("MON")) battleData.putString("MON",monSpot.getText().toString());
                else battleData.putString("MON", "MON00001");
                if(envSpot.getText().toString().contains("ENV")) battleData.putString("ENV",envSpot.getText().toString());
                else battleData.putString("ENV", "ENV00001");

                toBattle.setClass(getApplicationContext(), BattleActivity.class);
                toBattle.putExtras(battleData);
                startActivity(toBattle);
            }
        });

        if(nfcAdapter != null && nfcAdapter.isEnabled()){
            //Toast.nameText(this, "Ready to Play", Toast.LENGTH_LONG).show();
        }
        else{
            //Toast.nameText(this, "Not Ready to Play", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, "NfcIntent!", Toast.LENGTH_SHORT).show();

            if(tglReadWrite.isChecked())
            {
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                if(parcelables != null && parcelables.length > 0)
                {
                    readTextFromMessage((NdefMessage) parcelables[0]);
                }else{
                    Toast.makeText(this, "No NDEF messages found!", Toast.LENGTH_SHORT).show();
                }

            }else{
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefMessage ndefMessage = createNdefMessage(textTagContent.getText()+"");

                writeNdefMessage(tag, ndefMessage);
            }

        }
    }

    private void readTextFromMessage(NdefMessage ndefMessage) {

        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0){

            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);


            if(parseTag(tagContent)){
                textTagContent.setText(tagContent);
                if(tagContent.contains("HER")) heroSpot.setText(tagContent);
                else if(tagContent.contains("MON")) monSpot.setText(tagContent);
                else if(tagContent.contains("ENV")) envSpot.setText(tagContent);
            }
            else Toast.makeText(this, "No HERO or MONSTER or ENVIRONMENT card found!", Toast.LENGTH_SHORT).show();


        }else
        {
            Toast.makeText(this, "No NDEF records found!", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean parseTag(String tagContent) {
        if(tagContent.contains("HER") || tagContent.contains("MON") || tagContent.contains("ENV")) return true;
        return false;
    }

    @Override
    protected void onResume() {

        Intent intent = new Intent(this, SelectionActivity.class);
        intent.addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilter = new IntentFilter[]{};

        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, null);

        super.onResume();
    }

    @Override
    protected void onPause() {

        nfcAdapter.disableForegroundDispatch(this);

        super.onPause();
    }

    private void disableForegroundDispatchSystem() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {

            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(this, "Tag is not ndef formatable!", Toast.LENGTH_SHORT).show();
                return;
            }


            ndefFormatable.connect();
            ndefFormatable.format(ndefMessage);
            ndefFormatable.close();

            Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }

    }

    private void writeNdefMessage(Tag tag, NdefMessage ndefMessage) {

        try {

            if (tag == null) {
                Toast.makeText(this, "Tag object cannot be null", Toast.LENGTH_SHORT).show();
                return;
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                // format tag with the ndef format and writes the message.
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Toast.makeText(this, "Tag is not writable!", Toast.LENGTH_SHORT).show();

                    ndef.close();
                    return;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();

                Toast.makeText(this, "Tag writen!", Toast.LENGTH_SHORT).show();

            }

        } catch (Exception e) {
            Log.e("writeNdefMessage", e.getMessage());
        }

    }


    private NdefRecord createTextRecord(String content) {
        try {
            byte[] language;
            language = Locale.getDefault().getLanguage().getBytes("UTF-8");

            final byte[] text = content.getBytes("UTF-8");
            final int languageSize = language.length;
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(1 + languageSize + textLength);

            payload.write((byte) (languageSize & 0x1F));
            payload.write(language, 0, languageSize);
            payload.write(text, 0, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());

        } catch (UnsupportedEncodingException e) {
            Log.e("createTextRecord", e.getMessage());
        }
        return null;
    }


    private NdefMessage createNdefMessage(String content) {

        NdefRecord ndefRecord = createTextRecord(content);

        NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

        return ndefMessage;
    }

    public void tglReadWriteOnClick(View view) {
        textTagContent.setText("");
    }

    public String getTextFromNdefRecord(NdefRecord ndefRecord)
    {
        String tagContent = null;
        try {
            byte[] payload = ndefRecord.getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            int languageSize = payload[0] & 0063;
            tagContent = new String(payload, languageSize + 1,
                    payload.length - languageSize - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("getTextFromNdefRecord", e.getMessage(), e);
        }
        return tagContent;
    }
}
