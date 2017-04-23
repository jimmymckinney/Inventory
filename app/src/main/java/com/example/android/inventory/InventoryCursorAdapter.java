package com.example.android.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventory.data.InventoryContract;
import com.squareup.picasso.Picasso;

/**
 * {@link InventoryCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class InventoryCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link InventoryCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find fields to populate in inflated template
        TextView productNameTextView = (TextView) view.findViewById(R.id.product_name);
        final TextView productQuantityTextView = (TextView) view.findViewById(R.id.product_quantity);
        TextView productPriceTextView = (TextView) view.findViewById(R.id.product_price);
        TextView productSupplierTextView = (TextView) view.findViewById(R.id.product_supplier);
        ImageView productPictureImageView = (ImageView) view.findViewById(R.id.product_picture);
        TextView productSalesTextView = (TextView) view.findViewById(R.id.product_sales);

        // Extract properties from cursor
        int rowId = cursor.getInt(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry._ID));
        String productName = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_NAME));
        final String productQuantity = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_QUANTITY));
        String productPrice = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_PRICE));
        String productSupplier = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_SUPPLIER));
        String productPicture = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_PICTURE));
        final String productSales = cursor.getString(cursor.getColumnIndexOrThrow(InventoryContract.InventoryEntry.COLUMN_PRODUCT_SALES));

        Uri productPictureUri = Uri.parse(productPicture);

        // Populate fields with extracted properties
        productNameTextView.setText(productName);
        productQuantityTextView.setText("In-Stock: " + productQuantity);
        productPriceTextView.setText("$" + productPrice);
        productSupplierTextView.setText("Supplier: " + productSupplier);
        Picasso.with(context).load(productPictureUri).placeholder(R.drawable.ic_photo_black_24dp).error(R.drawable.ic_photo_black_24dp).fit().centerInside().into(productPictureImageView);
        productSalesTextView.setText("Items sold: " + productSales);

        final Uri currentProductUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, rowId);

        TextView sellButton = (TextView) view.findViewById(R.id.sell_button);
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int productQuantityInteger = Integer.parseInt(productQuantity);
                int productSalesInteger = Integer.parseInt(productSales);
                if (productQuantityInteger <= 0) {
                    Toast.makeText(context, R.string.no_more_stock,
                            Toast.LENGTH_SHORT).show();
                } else if (productQuantityInteger > 0) {
                    ContentValues values = new ContentValues();
                    values.put(InventoryContract.InventoryEntry.COLUMN_PRODUCT_QUANTITY, --productQuantityInteger);
                    values.put(InventoryContract.InventoryEntry.COLUMN_PRODUCT_SALES, ++productSalesInteger);
                    context.getContentResolver().update(currentProductUri, values,
                            null, null);
                    context.getContentResolver().notifyChange(currentProductUri, null);
                }
            }
        });
    }
}