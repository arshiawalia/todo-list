package benawad.com.todolist;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import benawad.com.todolist.adapters.FinishedItemsArrayAdapter;
import benawad.com.todolist.adapters.ItemsArrayAdapter;
import benawad.com.todolist.contentprovider.NoteContentProvider;
import benawad.com.todolist.database.NoteTable;


public class NoteActivity extends AppCompatActivity {

    private final static String TAG = NoteActivity.class.getSimpleName();
    public final static int SLASHED = 1;
    public final static int UNSLASHED = 0;
    ItemsArrayAdapter mItemsArrayAdapter;
    FinishedItemsArrayAdapter mFinishedItemsArrayAdapter;
    EditText mNewItemText;
    DynamicListView mItemsListView;
    ListView mFinishedItemsListView;
    ArrayList<String> mItems;
    ArrayList<String> mFinishedItems;
    //    FloatingActionButton fab;
    private Uri noteUri;
    public ArrayList<String> slashes;
    EditText mNoteTitle;
    ActionBar mActionBar;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_note);
        slashes = new ArrayList<>();

        mFinishedItems = new ArrayList<>();
        mFinishedItemsArrayAdapter = new FinishedItemsArrayAdapter(this, mFinishedItems, true);
        mFinishedItemsListView = (ListView) findViewById(R.id.finishedItems);
        mFinishedItemsListView.setAdapter(mFinishedItemsArrayAdapter);
        mFinishedItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView item = (TextView) view.findViewById(R.id.itemText);
                String text = item.getText().toString();
                mFinishedItems.remove(text);
                mItems.add(text);
                mFinishedItemsArrayAdapter.notifyDataSetChanged();
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        });

        mItems = new ArrayList<String>();
        mItemsArrayAdapter = new ItemsArrayAdapter(this, mItems, false);
        mItemsListView = (DynamicListView) findViewById(R.id.itemsListView);
        mItemsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mItemsListView.setAdapter(mItemsArrayAdapter);
        mItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView item = (TextView) view.findViewById(R.id.itemText);
                String text = item.getText().toString();
                mItems.remove(text);
                mFinishedItems.add(text);
                mFinishedItemsArrayAdapter.notifyDataSetChanged();
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        });

        Bundle extras = getIntent().getExtras();

        // check from the saved Instance
        noteUri = (bundle == null) ? null : (Uri) bundle
                .getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);

        mActionBar = getSupportActionBar();
        View view = getLayoutInflater().inflate(R.layout.note_actionbar, null);

        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setCustomView(view);
        //ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionBar.setDisplayShowCustomEnabled(true);
        mNoteTitle = (EditText) mActionBar.getCustomView().findViewById(R.id.noteName);

        // Or passed from the other activity
        if (extras != null) {
            if(extras.getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE) != null) {
                noteUri = extras
                        .getParcelable(NoteContentProvider.CONTENT_ITEM_TYPE);
            }
            fillData(noteUri);

        }

        mItemsListView.setCheeseList(mItems);
    }

    private void fillData(Uri uri) {

        String[] projection = {NoteTable.COLUMN_ITEMS, NoteTable.COLUMN_SLASHED, NoteTable.COLUMN_NOTE_TITLE};
        Cursor cursor = null;

        try {
            cursor = getContentResolver().query(uri, projection, null, null,
                    null);
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointerException caught: ", e);
        }
        if (cursor != null) {
            cursor.moveToFirst();

            String sItems = cursor.getString(cursor
                    .getColumnIndexOrThrow(NoteTable.COLUMN_ITEMS));

            String sSlashes = cursor.getString(cursor.getColumnIndexOrThrow(NoteTable.COLUMN_SLASHED));

            String title = cursor.getString(cursor.getColumnIndexOrThrow(NoteTable.COLUMN_NOTE_TITLE));

            try {
                JSONArray jsonArray = new JSONArray(sItems);

                mNoteTitle.setText(title);

                JSONArray slashesJsonArray = new JSONArray(sSlashes);
                for (int i = 0; i < slashesJsonArray.length(); i++) {
                    slashes.add("" + slashesJsonArray.get(i));
                    if(slashesJsonArray.get(i).equals(NoteActivity.UNSLASHED)){
                        mItems.add((String) jsonArray.get(i));
                    }
                    else{
                        mFinishedItems.add((String) jsonArray.get(i));
                    }
                }
                mFinishedItemsArrayAdapter.notifyDataSetChanged();
                mItemsArrayAdapter.notifyDataSetChanged();
            } catch (JSONException ignored) {
            }

            // always close the cursor
            cursor.close();
        }

    }

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveState();
        outState.putParcelable(NoteContentProvider.CONTENT_ITEM_TYPE, noteUri);
    }

    protected void onPause() {
        super.onPause();
        saveState();
    }

    private void saveState()
    {
        mItems.addAll(mFinishedItems);
        String note = new JSONArray(mItems).toString();
        ArrayList<Integer> slashes = new ArrayList<>();

        for (int i = 0; i < mItemsListView.getChildCount(); i++) {
            slashes.add(NoteActivity.UNSLASHED);
        }
        for (int i = 0; i < mFinishedItemsListView.getChildCount(); i++) {
            slashes.add(NoteActivity.SLASHED);
        }

        String sSlashes = new JSONArray(slashes).toString();

        if (mItems.isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(NoteTable.COLUMN_ITEMS, note);
        values.put(NoteTable.COLUMN_SLASHED, sSlashes);

        String noteTitle = mNoteTitle.getText().toString();
        if(noteTitle.isEmpty()){
            noteTitle = "Untitled";
        }

        values.put(NoteTable.COLUMN_NOTE_TITLE, noteTitle);
        if (noteUri == null) {
            noteUri = getContentResolver().insert(NoteContentProvider.CONTENT_URI, values);
            String firstPart = NoteContentProvider.CONTENT_URI.toString();
            Long id = ContentUris.parseId(noteUri);
            String correctURIs = firstPart + "/" + id;
            Uri correctedUri = Uri.parse(correctURIs);
            try{
                getContentResolver().update(noteUri, values, null, null);
            }
            catch (Exception e){
                noteUri = correctedUri;
            }

        } else {
            getContentResolver().update(noteUri, values, null, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds mItems to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.addItem) {
//            item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//                @Override
//                public boolean onMenuItemClick(MenuItem item) {
//                    addItem(null);
//                    return false;
//                }
//            });
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void addItem(View v) {

        if (mItems.size() < 100) {
            AlertDialog alertToShow = getDialog().create();
            alertToShow.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            alertToShow.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Max Items")
                    .setMessage("You have reached the maximum " +
                            "number of items (100) one note can hold.")
                    .setPositiveButton("OK", null);
            builder.show();
        }

    }

    public AlertDialog.Builder getDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        LinearLayout newNoteBaseLayout = (LinearLayout) li.inflate(R.layout.new_item_dialog, null);

        mNewItemText = (EditText) newNoteBaseLayout.getChildAt(0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeKeyboard();
                String text = mNewItemText.getText().toString();
                if (!mItems.contains(text) && !text.isEmpty()) {
                    mItems.add(text);
                    mItemsArrayAdapter.update();
                    mItemsArrayAdapter.notifyDataSetChanged();
                }
                else if(mItems.contains(text)){
                    Toast.makeText(NoteActivity.this, text + " is already added.", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeKeyboard();
            }
        })
                .setTitle("New Item");

        builder.setView(newNoteBaseLayout);
        return builder;
    }

    private void closeKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mNewItemText.getWindowToken(), 0);
    }

    public void deleteItem(int position) {
        mItems.remove(position);
        mItemsArrayAdapter.update();
        mItemsArrayAdapter.notifyDataSetChanged();
    }

    public void editItem(final int position) {
        AlertDialog.Builder builder = getDialog();
        mNewItemText.setText(mItems.get(position));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeKeyboard();
                mItems.set(position, mNewItemText.getText().toString());
                mItemsArrayAdapter.update();
                mItemsArrayAdapter.notifyDataSetChanged();
            }
        });
        AlertDialog alertToShow = builder.create();
        alertToShow.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertToShow.show();
        mNewItemText.setSelection(mNewItemText.getText().length());
    }

    public void uncheckAll(View view) {
        mItems.addAll(mFinishedItems);
        mFinishedItems.clear();
        mItemsArrayAdapter.notifyDataSetChanged();
        mFinishedItemsArrayAdapter.notifyDataSetChanged();
    }

    public void deleteFinishedItem(int position) {
        mFinishedItems.remove(position);
        mFinishedItemsArrayAdapter.notifyDataSetChanged();
    }

    public void editFinishedItem(final int position) {
        AlertDialog.Builder builder = getDialog();
        mNewItemText.setText(mFinishedItems.get(position));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mFinishedItems.set(position, mNewItemText.getText().toString());
                mFinishedItemsArrayAdapter.notifyDataSetChanged();
            }
        });
        builder.show();
    }

}
