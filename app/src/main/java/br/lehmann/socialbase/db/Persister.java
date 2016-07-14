package br.lehmann.socialbase.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.lehmann.socialbase.twitter.TrendingTopic;
import br.lehmann.socialbase.twitter.Tweet;

/**
 * Classe que fornece um objeto SINGLETON para acesso a base SQLite.
 * Created by André on 07/07/2016.
 */

public class Persister extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Twitter.db";

    private static final String TRENDING_TABLE_NAME = "trending";
    private static final String TRENDING_VOLUME = "volume";
    private static final String TRENDING_NAME = "name";
    private static final String TRENDING_QUERY = "query";
    private static final String TRENDING_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TRENDING_TABLE_NAME + " (" +
                    TRENDING_VOLUME + " NUMBER, " +
                    TRENDING_NAME + " TEXT, " +
                    TRENDING_QUERY + " TEXT);";

    private static final String TWEET_TABLE_NAME = "tweet";
    private static final String TWEET_TEXT = "text";
    private static final String TWEET_QUERY = "query";
    private static final String TWEET_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TWEET_TABLE_NAME + " (" +
                    TWEET_QUERY + " TEXT, " +
                    TWEET_TEXT + " TEXT);";

    private static final String SQL_DELETE_TRENDINGS =
            "DELETE FROM " + TRENDING_TABLE_NAME;

    private static final String SQL_DELETE_TWEETS =
            "DELETE FROM " + TWEET_TABLE_NAME;

    private static Persister mInstance;

    private final Context mCtx;

    public static Persister getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new Persister(ctx.getApplicationContext());
        }
        return mInstance;
    }

    private Persister(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        this.mCtx = ctx;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TRENDING_TABLE_CREATE);
        db.execSQL(TWEET_TABLE_CREATE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // como o objetivo é usar essa tabela só como um cache de dados, removo todos os registros caso haja alguma atualização
        db.execSQL(SQL_DELETE_TRENDINGS);
        db.execSQL(SQL_DELETE_TWEETS);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void insertTT(List<TrendingTopic> tts) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (TrendingTopic tt : tts) {
                ContentValues values = new ContentValues();
                values.put(TRENDING_VOLUME, tt.getVolume());
                values.put(TRENDING_NAME, tt.getName());
                values.put(TRENDING_QUERY, tt.getQuery());
                db.insert(
                        TRENDING_TABLE_NAME,
                        null,
                        values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public ArrayList<TrendingTopic> read() {
        String[] projection = {
                TRENDING_VOLUME,
                TRENDING_NAME,
                TRENDING_QUERY
        };

        String sortOrder =
                TRENDING_VOLUME + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TRENDING_TABLE_NAME, projection, null, null, null, null, sortOrder);
        try {
            ArrayList<TrendingTopic> ret = new ArrayList<>(20);
            if (!c.moveToFirst()) {
                return ret;
            }
            do {
                ret.add(new TrendingTopic(c.getInt(c.getColumnIndex(TRENDING_VOLUME)), c.getString(c.getColumnIndex(TRENDING_NAME)), c.getString(c.getColumnIndex(TRENDING_QUERY))));
            } while (c.moveToNext());
            return ret;
        } finally {
            c.close();
            db.close();
        }
    }

    public void insert(List<Tweet> tweets) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Tweet tweet : tweets) {
                ContentValues values = new ContentValues();
                values.put(TWEET_TEXT, tweet.getText());
                values.put(TWEET_QUERY, tweet.getQuery());
                db.insert(
                        TWEET_TABLE_NAME,
                        null,
                        values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public ArrayList<Tweet> read(String query) {
        String[] projection = {
                TWEET_TEXT,
                TWEET_QUERY
        };

        String filter =
                TWEET_QUERY + "=?";


        String[] fieldsValue = {
                query
        };

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query(TWEET_TABLE_NAME, projection, filter, fieldsValue, null, null, null);
        try {
            ArrayList<Tweet> ret = new ArrayList<>(20);
            if (!c.moveToFirst()) {
                return ret;
            }
            do {
                ret.add(new Tweet(c.getString(c.getColumnIndex(TWEET_TEXT)), c.getString(c.getColumnIndex(TWEET_QUERY))));
            } while (c.moveToNext());
            return ret;
        } finally {
            c.close();
            db.close();
        }
    }

    public void removeTweets() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TWEET_TABLE_NAME, null, null);
        db.close();
    }

    public void removeTrendings() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TRENDING_TABLE_NAME, null, null);
        db.close();
    }
}
