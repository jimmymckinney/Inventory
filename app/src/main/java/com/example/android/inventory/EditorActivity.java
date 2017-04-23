package com.example.android.inventory;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract.InventoryEntry;
import com.example.android.inventory.data.InventoryDbHelper;
import com.squareup.picasso.Picasso;

/**
 * Allows user to add a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the inventory data loader */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    private static final int PICK_IMAGE_REQUEST = 1;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    private boolean mProductHasChanged = false;

    private InventoryDbHelper mDbHelper;

    private EditText mProductNameEditText;
    private EditText mProductQuantityEditText;
    private EditText mProductPriceEditText;
    private EditText mProductSupplierEditText;
    private EditText mProductSalesEditText;

    private ImageView mProductPictureView;
    private Uri mImageUri;
    private String mImageUriString = "No image stored.";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            setTitle(R.string.editor_activity_title_new_product);
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(R.string.editor_activity_title_edit_product);
        }

        // Find all relevant views that we will need to read user input from
        mProductNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mProductQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mProductPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mProductSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        mProductSalesEditText = (EditText) findViewById(R.id.edit_product_sales);
        mProductPictureView = (ImageView) findViewById(R.id.edit_product_picture);

        mProductNameEditText.setOnTouchListener(mTouchListener);
        mProductQuantityEditText.setOnTouchListener(mTouchListener);
        mProductPriceEditText.setOnTouchListener(mTouchListener);
        mProductSupplierEditText.setOnTouchListener(mTouchListener);
        mProductSalesEditText.setOnTouchListener(mTouchListener);
        mProductPictureView.setOnTouchListener(mTouchListener);

        mProductPictureView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageSelector();
            }
        });

        mDbHelper = new InventoryDbHelper(this);

        getSupportLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveProduct() {
        String productNameString = mProductNameEditText.getText().toString().trim();
        String productQuantityString = mProductQuantityEditText.getText().toString().trim();
        String productPriceString = mProductPriceEditText.getText().toString().trim();
        String productSupplierString = mProductSupplierEditText.getText().toString().trim();
        String productSalesString = mProductSalesEditText.getText().toString().trim();

        // If the weight is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int productQuantityInt = 0;
        int productSalesInt = 0;
        int productPriceInt = 0;
        if (!TextUtils.isEmpty(productQuantityString)) {
            productQuantityInt = Integer.parseInt(productQuantityString);
        }
        if (!TextUtils.isEmpty(productSalesString)) {
            productSalesInt = Integer.parseInt(productSalesString);
        }
        if (!TextUtils.isEmpty(productPriceString)) {
            productPriceInt = Integer.parseInt(productPriceString);
        }

        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(productNameString) && TextUtils.isEmpty(productQuantityString) &&
                TextUtils.isEmpty(productPriceString) && TextUtils.isEmpty(productSupplierString) &&
                TextUtils.isEmpty(productSalesString) && TextUtils.isEmpty(mImageUriString)) {return;}

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, productNameString);
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, productQuantityInt);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, productPriceInt);
        values.put(InventoryEntry.COLUMN_PRODUCT_SUPPLIER, productSupplierString);
        values.put(InventoryEntry.COLUMN_PRODUCT_PICTURE, mImageUriString);
        values.put(InventoryEntry.COLUMN_PRODUCT_SALES, productSalesInt);

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // Insert a new product into the provider, returning the content URI for the new product.
            mCurrentProductUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
            // Toast nessage displaying id.
            if (mCurrentProductUri == null) {
                Toast.makeText(this, R.string.editor_insert_product_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_insert_product_successful, Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);
            // Toast message displaying id.
            if (rowsAffected == 0) {
                Toast.makeText(this, R.string.editor_insert_product_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.editor_insert_product_successful, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentProductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                //Save product to database
                saveProduct();
                //Exit Activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        if (mCurrentProductUri == null) {
            return null;
        }
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT_NAME,
                InventoryEntry.COLUMN_PRODUCT_QUANTITY,
                InventoryEntry.COLUMN_PRODUCT_PRICE,
                InventoryEntry.COLUMN_PRODUCT_SUPPLIER,
                InventoryEntry.COLUMN_PRODUCT_PICTURE,
                InventoryEntry.COLUMN_PRODUCT_SALES};

        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Provider content URI to query
                projection,             // Columns to include in resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int productNameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME);
            int productQuantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY);
            int productPriceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE);
            int productSupplierColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_SUPPLIER);
            int productPictureColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PICTURE);
            int productSalesColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_SALES);

            // Extract out the value from the Cursor for the given column index
            String productName = cursor.getString(productNameColumnIndex);
            int productQuantity = cursor.getInt(productQuantityColumnIndex);
            int productPrice = cursor.getInt(productPriceColumnIndex);
            String productSupplier = cursor.getString(productSupplierColumnIndex);
            mImageUriString = cursor.getString(productPictureColumnIndex);
            int productSales = cursor.getInt(productSalesColumnIndex);

            // Update the views on the screen with the values from the database
            mProductNameEditText.setText(productName);
            mProductQuantityEditText.setText(Integer.toString(productQuantity));
            mProductPriceEditText.setText(Integer.toString(productPrice));
            mProductSupplierEditText.setText(productSupplier);
            Picasso.with(this).load(mImageUriString).placeholder(R.drawable.ic_photo_black_24dp).error(R.drawable.ic_photo_black_24dp).fit().centerInside().into(mProductPictureView);
            mProductSalesEditText.setText(Integer.toString(productSales));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mProductNameEditText.setText("");
        mProductQuantityEditText.setText("");
        mProductPriceEditText.setText("");
        mProductSupplierEditText.setText("");
        mProductSalesEditText.setText("");
        //Picasso.with(this).load(R.drawable.ic_photo_black_24dp).into(mProductPictureView);
    }

    public void openImageSelector() {
        Intent imageSelector;

        if (Build.VERSION.SDK_INT < 19) {
            imageSelector = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            imageSelector = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            imageSelector.addCategory(Intent.CATEGORY_OPENABLE);
        }

        //File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        //String pictureDirectoryPath = pictureDirectory.getPath();
        //Uri pictureUri = Uri.parse(pictureDirectoryPath);
        imageSelector.setType("image/*");
        startActivityForResult(Intent.createChooser(imageSelector, "Select an image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {

            if (resultData != null) {
                mImageUri = resultData.getData();
                mImageUriString = mImageUri.toString();
                Picasso.with(this).load(mImageUri).placeholder(R.drawable.ic_photo_black_24dp).error(R.drawable.ic_photo_black_24dp).fit().centerInside().into(mProductPictureView);
            }
        }
    }
}