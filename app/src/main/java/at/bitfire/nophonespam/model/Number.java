/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.nophonespam.model;

import android.content.ContentValues;

public class Number {

    public static final String
            _TABLE = "numbers",
            NUMBER = "number",
            NAME = "name",
            LAST_CALL = "lastCall",
            TIMES_CALLED = "timesCalled";

    public String number;
    public String name;

    public Long lastCall;
    public int timesCalled;


    public static Number fromValues(ContentValues values) {
        Number number = new Number();
        number.number = values.getAsString(NUMBER);
        number.name = values.getAsString(NAME);
        number.lastCall = values.getAsLong(LAST_CALL);
        number.timesCalled = values.getAsInteger(TIMES_CALLED);
        return number;
    }

    public static String wildcardsDbToView(String number) {
        return number
                .replace('%','*')
                .replace('_','#');
    }

    public static String wildcardsViewToDb(String number) {
        return number
                .replaceAll("[^+#*1-9]", "")
                .replace('*','%')
                .replace('#','_');
    }

}
