package at.bitfire.nophonespam.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    public DbHelper(Context context) {
        super(context, "database", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + Number._TABLE + "(" +
                Number.NUMBER + " TEXT NOT NULL PRIMARY KEY," +
                Number.NAME + " TEXT NULL," +
                Number.LAST_CALL + " INTEGER NULL," +
                Number.TIMES_CALLED + " INTEGER NOT NULL DEFAULT 0" +
        ")");

        ContentValues values = new ContentValues();

        /*values.put(Number.NAME, "Werbekuh Schule BV");
        values.put(Number.NUMBER, "+431810279346");
        db.insert(Number._TABLE, null, values);

        values.put(Number.NAME, "Luxemburg");
        values.put(Number.NUMBER, "+352*");
        db.insert(Number._TABLE, null, values);*/

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int from, int to) {
    }

}
