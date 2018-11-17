package rush.boss.edu.bossrush;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Random;

import javax.crypto.EncryptedPrivateKeyInfo;


public class BattleActivity extends AppCompatActivity {
    Button exitButton;
    Button turnEnd;
    NfcAdapter nfcAdapter;


    String heroStr;
    String monStr;

    heroCards hero;
    monsterCards mon;

    Card current;

    Card lastCard;
    Card currentCard;

    boolean skip;
    Random rand;

    TextView[] heroAttr;
    TextView[] monAttr;
    TextView cardVis;
    deckCards deck = new deckCards();



    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, "NfcIntent!", Toast.LENGTH_SHORT).show();
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(parcelables != null && parcelables.length > 0)
            {
                readTextFromMessage((NdefMessage) parcelables[0]);
            }else{
                Toast.makeText(this, "No NDEF messages found!", Toast.LENGTH_SHORT).show();
            }

            }/*else{
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefMessage ndefMessage = createNdefMessage(textTagContent.getText()+"");

                writeNdefMessage(tag, ndefMessage);
            }*/

        }

    private String readTextFromMessage(NdefMessage ndefMessage) {

        NdefRecord[] ndefRecords = ndefMessage.getRecords();

        if(ndefRecords != null && ndefRecords.length>0){

            NdefRecord ndefRecord = ndefRecords[0];

            String tagContent = getTextFromNdefRecord(ndefRecord);


            if(tagContent.contains("DECK") && deck.getCard(tagContent) != null){
//                textTagContent.setText(tagContent);
                currentCard = deck.getCard(tagContent);
                cardVis.setText(tagContent);
                Toast.makeText(this, deck.getCard(tagContent).getName(), Toast.LENGTH_SHORT).show();

            }
            else Toast.makeText(this, "No deck card found!", Toast.LENGTH_SHORT).show();
            return tagContent;
        }else
        {
            Toast.makeText(this, "No NDEF records found!", Toast.LENGTH_SHORT).show();
            return "no tag found!";
        }
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


    private void game(){
        Random rand = new Random();

        //Determine characters
        hero = new heroCards(heroStr);
        mon = new monsterCards(monStr);

        //Determine first
        if(rand.nextBoolean()) current = hero;
        else current = mon;

        //Display who goes first
        TextView currentActor = findViewById(R.id.currentTurn);

        TextView[] heroAttr = {
                findViewById(R.id.heroName),
                findViewById(R.id.heroHealth),
                findViewById(R.id.heroAttack),
                findViewById(R.id.heroDefense)
        };
        TextView[] monAttr = {
                findViewById(R.id.monName),
                findViewById(R.id.monHealth),
                findViewById(R.id.monAttack),
                findViewById(R.id.monDefense)
        };
        currentActor.setText(current.getName());

        //Tie heroes and monsters to their visual counterparts
        heroAttr[0].setText(hero.getName());
        heroAttr[1].setText(Integer.toString(hero.getHealth()));
        heroAttr[2].setText(Integer.toString(hero.getAttack()));
        heroAttr[3].setText(Integer.toString(hero.getDefense()));

        monAttr[0].setText(mon.getName());
        monAttr[1].setText(Integer.toString(mon.getHealth()));
        monAttr[2].setText(Integer.toString(mon.getAttack()));
        monAttr[3].setText(Integer.toString(mon.getDefense()));
        while(hero.getHealth() > 0 && mon.getHealth() > 0){
            //DO TURNS
//Take user input (SUPER WIP)
            //if(false)Decklist.selectCard(0);
            //NFC CARD PICK HERE

//            Log.v("I am a", actor.getType());
            for(Map.Entry<String, Integer> effect : hero.timers.entrySet()){
                //proc effects here
                //Silver Bullets
                if(effect.getKey() == "DECK0001" && effect.getValue() == 3){
                    hero.setAttack(hero.getAttack()*2);
                }
                if(effect.getKey() == "DECK0001" && effect.getValue() == 0) {
                    //get original stats from the deck
                    hero.setAttack(hero.getCard(hero.getId()).getAttack());
                }
                //Tactical Roll
                if(effect.getKey() == "DECK0002" && effect.getValue() >= 2){
                    if(rand.nextInt(100) <= 25)
                        hero.setDefense(Integer.MAX_VALUE);
                }
                if(effect.getKey() == "DECK0002" && effect.getValue() == 0) {
                    hero.setDefense(hero.getCard(hero.getId()).getDefense());
                }
                //All Tied Up
                if(effect.getKey() == "DECK0003" && effect.getValue() >= 2){
                    skip = true;
                }
                if(effect.getKey() == "DECK0003" && effect.getValue() == 0) {
                    skip = false;
                }
            }
            //proc timer here
            current.procTimer();

            if(current.getType().equals("HERO")){
                //Do a hero's turn, get input
                Log.v("HEROLog", current.getType());

                mon.setHealth(mon.getHealth()-(hero.getAttack() - mon.getDefense()));
                //After getting input


            }else{
                //Do a monster's turn
                TextView modHealth;

                if (mon.getCard(mon.getId()).getHealth() - mon.getHealth() >= 10) {
                    hero.setHealth(hero.getHealth() - (mon.getAttack() - hero.getDefense()));
                    modHealth = findViewById(R.id.heroHealth);
                    modHealth.setText(Integer.toString(hero.getHealth()));
//                    System.out.println( mon.getName() + " attacks " + hero.getName() + "!");
                    Log.v("MonOut", mon.getName() + " attacks " + hero.getName() + "!");
                }
                else if (mon.getCard(mon.getId()).getHealth() - mon.getHealth() >= 50) {
                    //manipulate defense for the next turn
//                    System.out.println( mon.getName() + " defends themselves!");
                    Log.v("MonOut", mon.getName() + " defends themselves!");
                }
                else if (mon.getCard(mon.getId()).getHealth() - mon.getHealth() >= 70) {
                    mon.setHealth(mon.getHealth() + 1);
//                    System.out.println( mon.getName() + " is healing!");
                    Log.v("MonOut", mon.getName() + " is healing!");
                } else {
//                    System.out.println( mon.getName() + " falters!");
                    Log.v("MonOut", mon.getName() + " falters!" + Integer.toString(mon.getHealth()));
                }
                current = hero;
            }

            currentActor.setText(current.getId());
        }

    }
    @Override
    protected void onResume() {

        Intent intent = new Intent(this, BattleActivity.class);
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.battle_layout);

        Intent intent = getIntent();

        String envID = intent.getStringExtra("ENV");
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(nfcAdapter != null && nfcAdapter.isEnabled()){
            //Toast.nameText(this, "Ready to Play", Toast.LENGTH_LONG).show();
        }
        else{
            //Toast.nameText(this, "Not Ready to Play", Toast.LENGTH_LONG).show();
            finish();
        }

        //When we implement the deck
        //Game game = new Game(getCard(hero), getCard(mon));
        heroStr = intent.getStringExtra("HERO");
        monStr = intent.getStringExtra("MON");

        exitButton = findViewById(R.id.exitBattle);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent main = new Intent();
                main.setClass(getApplicationContext(), MainActivity.class);
                startActivity(main);
            }
        });
        turnEnd = findViewById(R.id.turnEnd);
        turnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hero == current && !skip) current = mon;
                else if (mon == current) current = hero;
            }
        });
        cardVis = findViewById(R.id.cardVis);
        cardVis.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(currentCard != lastCard) {
                    switch (currentCard.getId()){
                        case "DECK0001":
                            hero.timers.put("DECK0001", 3);
                            break;
                        case "DECK0002":
                            hero.timers.put("DECK0002", 2);
                            break;
                        case "DECK0003":
                            hero.timers.put("DECK0003", 2);
                            break;
                        case "DECK0004":

                            break;
                        case "DECK0005":

                            break;

                    }
                    lastCard = currentCard;

                }
            }
        });

        game();
        if(hero.getHealth() >= 0)  {
            System.out.println("You Won");
        }
        else{
            System.out.println("You Lost");
        }
    }


}
