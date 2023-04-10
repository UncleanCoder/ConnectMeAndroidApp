package com.example.connectme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText messageEditText;
    private static final int READ_CONTACTS_REQUEST_CODE = 1;
    private static final int SEND_MESSAGES_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        messageEditText = (EditText) findViewById(R.id.editText);
        Button sendButton = (Button) findViewById(R.id.button);
        ListView contactListView = (ListView) findViewById(R.id.listView);

        ArrayAdapter<Contact> contactAdapter = new ContactAdapter(this, android.R.layout.simple_list_item_1, getContacts());
        contactListView.setAdapter(contactAdapter);

        View.OnClickListener sendButtonClickListener = createSendButtonClickListener();
        sendButton.setOnClickListener(sendButtonClickListener);
    }

    @SuppressLint("Range")
    private List<Contact> getContacts() {
        List<Contact> contacts = new ArrayList<>();
        if (hasPermission(Manifest.permission.READ_CONTACTS)) {
            try(Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contacts.add(new Contact(name, number));
                }
            }
        } else {
            requestPermission(Manifest.permission.READ_CONTACTS, READ_CONTACTS_REQUEST_CODE);
        }
        return contacts;
    }

    private View.OnClickListener createSendButtonClickListener() {
        return clickListener -> {
            if (hasPermission(Manifest.permission.SEND_SMS)) {
                sendMessage();
            } else {
                requestPermission(Manifest.permission.SEND_SMS, SEND_MESSAGES_REQUEST_CODE);
            }
        };
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString();
        List<Contact> contacts = getContacts();
        for (int i=0 ; i < contacts.size() ; i++) {
            SmsManager smsManager = SmsManager.getDefault();
            PendingIntent sentIntent = PendingIntent.getBroadcast(this, i, new Intent("SMS_SENT"), 0);
            smsManager.sendTextMessage(contacts.get(i).number, null, message, sentIntent, null);

            BroadcastReceiver smsSentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = null;
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            message = "Message sent successfully";
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            message = "Failed to send message: generic failure";
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            message = "Failed to send message: no service";
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            message = "Failed to send message: null PDU";
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            message = "Failed to send message: radio off";
                            break;
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            };
            registerReceiver(smsSentReceiver, new IntentFilter("SMS_SENT"));
        }
    }

    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(String permission, int requestCode) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_CONTACTS_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getContacts();
        }
        if (requestCode == SEND_MESSAGES_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            sendMessage();
        }
    }

    private class Contact {
        private final String name;
        private final String number;

        public Contact(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    public class ContactAdapter extends ArrayAdapter<Contact> {
        public ContactAdapter(Context context, int resource, List<Contact> contacts) {
            super(context, resource, contacts);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            }
            Contact contact = getItem(position);
            TextView listItemText = (TextView) convertView.findViewById(android.R.id.text1);
            listItemText.setText(contact.name);
            return convertView;
        }
    }
}