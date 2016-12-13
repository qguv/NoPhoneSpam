/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.nophonespam;

import android.Manifest;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import at.bitfire.nophonespam.model.DbHelper;
import at.bitfire.nophonespam.model.Number;

public class BlacklistActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Set<Number>>, AdapterView.OnItemClickListener {

    protected Settings settings;
    CoordinatorLayout coordinatorLayout;

    ListView list;
    ArrayAdapter<Number> adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist);

        settings = new Settings(this);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        list = (ListView)findViewById(R.id.numbers);
        list.setAdapter(adapter = new NumberAdapter(this));
        list.setOnItemClickListener(this);

        list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.blacklist_delete_numbers, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.delete:
                        deleteSelectedNumbers();
                        actionMode.finish();
                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE }, 0);

        getLoaderManager().initLoader(0, null, this);
    }

    protected void deleteSelectedNumbers() {
        final List<String> numbers = new LinkedList<>();

        SparseBooleanArray checked = list.getCheckedItemPositions();
        for (int i = checked.size() - 1; i >= 0; i--)
            if (checked.valueAt(i)) {
                int position = checked.keyAt(i);
                numbers.add(adapter.getItem(position).number);
            }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                DbHelper dbHelper = new DbHelper(BlacklistActivity.this);
                try {
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    for (String number : numbers)
                        db.delete(Number._TABLE, Number.NUMBER + "=?", new String[] { number });
                } finally {
                    dbHelper.close();
                }

                getLoaderManager().restartLoader(0, null, BlacklistActivity.this);
                return null;
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean ok = true;
        for (int result : grantResults)
            if (result != PackageManager.PERMISSION_GRANTED)
                ok = false;

        if (!ok)
            Snackbar.make(coordinatorLayout, R.string.blacklist_permissions_required, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.blacklist_request_permissions, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(BlacklistActivity.this, new String[] { Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE }, 0);
                        }
                    })
                    .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_blacklist, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.block_hidden_numbers).setChecked(settings.blockHiddenNumbers());
        menu.findItem(R.id.notifications).setChecked(settings.showNotifications());
        return true;
    }

    public void onBlockHiddenNumbers(MenuItem item) {
        settings.blockHiddenNumbers(!item.isChecked());
    }

    public void onShowNotifications(MenuItem item) {
        settings.showNotifications(!item.isChecked());
    }

    public void onAbout(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://gitlab.com/bitfireAT/NoPhoneSpam/")));
    }

    public void addNumber(View view) {
        startActivity(new Intent(this, EditNumberActivity.class));
    }


    @Override
    public Loader<Set<Number>> onCreateLoader(int i, Bundle bundle) {
        return new NumberLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Set<Number>> loader, Set<Number> numbers) {
        adapter.clear();
        adapter.addAll(numbers);
    }

    @Override
    public void onLoaderReset(Loader<Set<Number>> loader) {
        adapter.clear();
    }


    private static class NumberAdapter extends ArrayAdapter<Number> {

        public NumberAdapter(Context context) {
            super(context, R.layout.blacklist_item);
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = View.inflate(getContext(), R.layout.blacklist_item, null);

            Number number = getItem(position);

            TextView tv = (TextView)view.findViewById(R.id.number);
            tv.setText(Number.wildcardsDbToView(number.number));

            tv = (TextView)view.findViewById(R.id.name);
            tv.setText(number.name);

            tv = (TextView)view.findViewById(R.id.stats);
            if (number.lastCall != null) {
                tv.setVisibility(View.VISIBLE);
                tv.setText(getContext().getResources().getQuantityString(R.plurals.blacklist_call_details, number.timesCalled,
                        number.timesCalled, SimpleDateFormat.getDateTimeInstance().format(new Date(number.lastCall))));
            } else
                tv.setVisibility(View.GONE);

            return view;
        }

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Number number = adapter.getItem(position);

        Intent intent = new Intent(this, EditNumberActivity.class);
        intent.putExtra(EditNumberActivity.EXTRA_NUMBER, number.number);
        startActivity(intent);
    }


    protected static class NumberLoader extends AsyncTaskLoader<Set<Number>> implements BlacklistObserver.Observer {

        public NumberLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            BlacklistObserver.addObserver(this, true);
        }

        @Override
        public Set<Number> loadInBackground() {
            DbHelper dbHelper = new DbHelper(getContext());
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                Set<Number> numbers = new LinkedHashSet<>();
                Cursor c = db.query(Number._TABLE, null, null, null, null, null, Number.NUMBER);
                while (c.moveToNext()) {
                    ContentValues values = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, values);
                    numbers.add(Number.fromValues(values));
                }
                c.close();

                return numbers;
            } finally {
                dbHelper.close();
            }
        }

        @Override
        public void onBlacklistUpdate() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            BlacklistObserver.removeObserver(this);
        }

    }

}
