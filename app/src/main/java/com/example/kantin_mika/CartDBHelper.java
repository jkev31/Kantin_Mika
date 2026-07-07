package com.example.kantin_mika;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CartDBHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "kantin_mika.db";
    public static final int DATABASE_VERSION = 3; // Incremented version

    public static final String TABLE_CART = "cart";
    public static final String COLUMN_ID_MENU = "id_menu";
    public static final String COLUMN_ID_TENANT = "id_tenant";
    public static final String COLUMN_NAMA_MENU = "nama_menu";
    public static final String COLUMN_NAMA_TENANT = "nama_tenant";
    public static final String COLUMN_NAMA_PEMILIK = "nama_pemilik";
    public static final String COLUMN_HARGA = "harga";
    public static final String COLUMN_QTY = "qty";

    public static final String TABLE_ORDERS = "orders";
    public static final String COLUMN_ORDER_ID = "order_id";
    // Reuse other columns for order items

    public CartDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CART_TABLE = "CREATE TABLE " + TABLE_CART + "("
                + COLUMN_ID_MENU + " INTEGER PRIMARY KEY,"
                + COLUMN_ID_TENANT + " INTEGER,"
                + COLUMN_NAMA_MENU + " TEXT,"
                + COLUMN_NAMA_TENANT + " TEXT,"
                + COLUMN_NAMA_PEMILIK + " TEXT,"
                + COLUMN_HARGA + " INTEGER,"
                + COLUMN_QTY + " INTEGER" + ")";
        db.execSQL(CREATE_CART_TABLE);

        String CREATE_ORDERS_TABLE = "CREATE TABLE " + TABLE_ORDERS + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_ORDER_ID + " TEXT,"
                + COLUMN_ID_TENANT + " INTEGER,"
                + COLUMN_NAMA_MENU + " TEXT,"
                + COLUMN_NAMA_TENANT + " TEXT,"
                + COLUMN_HARGA + " INTEGER,"
                + COLUMN_QTY + " INTEGER" + ")";
        db.execSQL(CREATE_ORDERS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        onCreate(db);
    }

    public void addToCart(int idMenu, int idTenant, String namaMenu, String namaTenant, String namaPemilik, int harga, int qty) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_CART, new String[]{COLUMN_QTY}, COLUMN_ID_MENU + "=?",
                new String[]{String.valueOf(idMenu)}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int currentQty = cursor.getInt(0);
            ContentValues values = new ContentValues();
            values.put(COLUMN_QTY, currentQty + qty);
            db.update(TABLE_CART, values, COLUMN_ID_MENU + "=?", new String[]{String.valueOf(idMenu)});
            cursor.close();
        } else {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID_MENU, idMenu);
            values.put(COLUMN_ID_TENANT, idTenant);
            values.put(COLUMN_NAMA_MENU, namaMenu);
            values.put(COLUMN_NAMA_TENANT, namaTenant);
            values.put(COLUMN_NAMA_PEMILIK, namaPemilik);
            values.put(COLUMN_HARGA, harga);
            values.put(COLUMN_QTY, qty);
            db.insert(TABLE_CART, null, values);
        }
    }

    public void saveOrder(String orderId, int idTenant, String namaMenu, String namaTenant, int harga, int qty) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ORDER_ID, orderId);
        values.put(COLUMN_ID_TENANT, idTenant);
        values.put(COLUMN_NAMA_MENU, namaMenu);
        values.put(COLUMN_NAMA_TENANT, namaTenant);
        values.put(COLUMN_HARGA, harga);
        values.put(COLUMN_QTY, qty);
        db.insert(TABLE_ORDERS, null, values);
    }

    public void updateQty(int idMenu, int qty) {
        SQLiteDatabase db = this.getWritableDatabase();
        if (qty <= 0) {
            db.delete(TABLE_CART, COLUMN_ID_MENU + "=?", new String[]{String.valueOf(idMenu)});
        } else {
            ContentValues values = new ContentValues();
            values.put(COLUMN_QTY, qty);
            db.update(TABLE_CART, values, COLUMN_ID_MENU + "=?", new String[]{String.valueOf(idMenu)});
        }
    }

    public void clearCart() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, null, null);
    }
    
    public void clearOrders() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ORDERS, null, null);
    }
}
