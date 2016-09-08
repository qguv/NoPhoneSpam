/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.nophonespam;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.text.TextUtilsCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.ITelephony;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

import at.bitfire.nophonespam.model.DbHelper;
import at.bitfire.nophonespam.model.Number;

public class CallReceiver extends BroadcastReceiver {
    private static final String TAG = "NoPhoneSpam";

    private static final int NOTIFY_REJECTED = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE).equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.i(TAG, "Received call: " + incomingNumber);

            Settings settings = new Settings(context);
            if (TextUtils.isEmpty(incomingNumber)) {
                // private number (no caller ID)
                if (settings.blockHiddenNumbers())
                    rejectCall(context, null);

            } else {
                DbHelper dbHelper = new DbHelper(context);
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    Cursor c = db.query(Number._TABLE, null, "? LIKE " + Number.NUMBER, new String[] { incomingNumber }, null, null, null);
                    if (c.moveToNext()) {
                        ContentValues values = new ContentValues();
                        DatabaseUtils.cursorRowToContentValues(c, values);
                        Number number = Number.fromValues(values);

                        rejectCall(context, number);

                        values.clear();
                        values.put(Number.LAST_CALL, System.currentTimeMillis());
                        values.put(Number.TIMES_CALLED, number.timesCalled + 1);
                        db.update(Number._TABLE, values, Number.NUMBER + "=?", new String[]{number.number});

                        BlacklistObserver.notifyUpdated();
                    }
                    c.close();
                } finally {
                    dbHelper.close();
                }
            }
        }
    }

    protected void rejectCall(@NonNull Context context, Number number) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        Class c = null;
        try {
            c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);

            ITelephony telephony = (ITelephony)m.invoke(tm);

            telephony.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Settings settings = new Settings(context);
        if (settings.showNotifications()) {
            Notification notify = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(context.getString(R.string.receiver_notify_call_rejected))
                    .setContentText(number != null ? (number.name != null ? number.name : number.number) : context.getString(R.string.receiver_notify_private_number))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setShowWhen(true)
                    .setAutoCancel(true)
                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, BlacklistActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                    .addPerson("tel:" + number)
                    .setGroup("rejected")
                    .build();

            String tag = number != null ? number.number : "private";
            NotificationManagerCompat.from(context).notify(tag, NOTIFY_REJECTED, notify);
        }

    }

}
