package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;

/**
 * Displays list of products that were entered and stored in the app.
 */
public class InventoryActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PRODUCT_LOADER = 0;
    InventoryCursorAdapter mInventoryCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the pet data
        ListView inventoryListView = (ListView) findViewById(R.id.list_view_inventory);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        inventoryListView.setEmptyView(emptyView);

        // Setup the Adapter to create a list item for each row of pet data in the Cursor
        // There is no pet data yet (until the loader finishes) so pass in null for the Cursor
        mInventoryCursorAdapter = new InventoryCursorAdapter(this, null);
        // Attach the adapter to the ListView
        inventoryListView.setAdapter(mInventoryCursorAdapter);

        // Setup item click listener
        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Intent intent = new Intent(InventoryActivity.this, EditorActivity.class);
                Uri currentProductUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                intent.setData(currentProductUri);
                startActivity(intent);
            }
        });

        // Initialize loader
        getSupportLoaderManager().initLoader(PRODUCT_LOADER, null, this);
    }

    private void insertProduct() {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, "Echo Dot");
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, 40);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, "49.99");
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER, "Amazon");
        values.put(InventoryEntry.COLUMN_PRODUCT_SALES, 7);

        // Insert the new row, returning the primary key value of the new row
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
    }

    private void deleteAllProducts() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        Log.v("CatalogActivity", rowsDeleted + " rows deleted from pet database");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertProduct();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllProducts();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_PRODUCT_SUPPLIER,
                InventoryEntry.COLUMN_PRODUCT_PICTURE,
                InventoryEntry.COLUMN_PRODUCT_SALES};

        return new CursorLoader(this,   // Parent activity context
                InventoryEntry.CONTENT_URI,   // PRovider content URI to query
                projection,             // Columns to include in resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mInventoryCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)
        mInventoryCursorAdapter.swapCursor(null);
    }
}
