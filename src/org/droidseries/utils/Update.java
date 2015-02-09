package org.droidseries.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.droidseries.droidseries;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Display;

public class Update
{
	// TODO: change this method from void to boolean to check if all updates go well
	public boolean updateDroidSeries(Context context, Display display) {
		droidseries.db.execQuery("CREATE TABLE IF NOT EXISTS droidseries (version VARCHAR)");
		String version = getVersion();
		// runs the updates if the db version isn't the current version
		boolean done = false;
		if (!version.equals(droidseries.VERSION)) {
			if (version.equals("0.1.5-7")) {
				done = u0157To0157G(context, display);
			} else if (version.equals("") || !version.equals(droidseries.VERSION)) {
				u0141To0142(context, display);
				u0142To0143(context, display);
				u0154To0155(context, display);
				u0156To0157(context, display);
				done = u0157To0157G(context, display);
			}
		}
		return done;
	}

	private String getVersion() {
		String version = "";
		try {
			Cursor c = droidseries.db.Query("SELECT version FROM droidseries");
			try {
				c.moveToFirst();
				if (c != null && c.isFirst()) {
					if (c.getCount() != 0) {
						version = c.getString(0); // get the version
					}
				}
			} catch (Exception e) {
				// does nothing
			}
			c.close();
		} catch (SQLiteException e) {
			Log.e("DroidSeries", e.getMessage());
		}
		return version;
	}

	private boolean u0141To0142(Context context, Display display) {
		String version = getVersion();
		if (version.equals("")) {
			// insert sql statment with the current version
			droidseries.db.execQuery("INSERT INTO droidseries (version) VALUES ('" + droidseries.VERSION
				+ "')");
			// do the changes you have to do
			try {
				List<String> serieIds = droidseries.db.getSeriesByName();
				for (int i = 0; i < serieIds.size(); i++) {
					try {
						Cursor c = droidseries.db.Query("SELECT posterInCache, posterThumb FROM series WHERE id='"
							+ serieIds.get(i) + "'");
						c.moveToFirst();
						if (c != null && c.isFirst()) {
							File thumbImage = new File(c.getString(1));
							thumbImage.delete();
							// create the new thumb
							Bitmap posterThumb = BitmapFactory.decodeFile(c.getString(0));
							int width = display.getWidth();
							int height = display.getHeight();
							int newHeight = (int) ((height > width ? height : width) * 0.25);
							int newWidth = (int) (newHeight * .75);
							Bitmap resizedBitmap = Bitmap.createScaledBitmap(posterThumb, newWidth, newHeight, true);
							File dirTmp = new File(context.getFilesDir().getAbsolutePath()
								+ "/thumbs/banners/posters");
							if (!dirTmp.isDirectory()) {
								dirTmp.mkdirs();
							}
							OutputStream fOut = null;
							File thumFile = new File(c.getString(1));
							fOut = new FileOutputStream(thumFile);
							resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
							fOut.flush();
							fOut.close();
							posterThumb.recycle();
							resizedBitmap.recycle();
							System.gc();
							posterThumb = null;
							resizedBitmap = null;
						}
						c.close();
					} catch (SQLiteException e) {
						Log.e("DroidSeries", e.getMessage());
					}
				}
			} catch (Exception e) {
			}
		}
		return true;
	}

	private boolean u0142To0143(Context context, Display display) {
		String version = getVersion();
		if (version.equals("0.1.4-2")) {
			// insert sql statment with the current version
			droidseries.db.execQuery("UPDATE droidseries SET version='" + droidseries.VERSION + "'");
			// do the changes you have to do
			try {
				List<String> serieIds = droidseries.db.getSeriesByName();
				for (int i = 0; i < serieIds.size(); i++) {
					try {
						Cursor c = droidseries.db.Query("SELECT posterInCache, posterThumb FROM series WHERE id='"
							+ serieIds.get(i) + "'");
						c.moveToFirst();
						if (c != null && c.isFirst()) {
							File thumbImage = new File(c.getString(1));
							thumbImage.delete();
							// create the new thumb
							Bitmap posterThumb = BitmapFactory.decodeFile(c.getString(0));
							int width = display.getWidth();
							int height = display.getHeight();
							int newHeight = (int) ((height > width ? height : width) * 0.25);
							int newWidth = (int) (newHeight * .75);
							Bitmap resizedBitmap = Bitmap.createScaledBitmap(posterThumb, newWidth, newHeight, true);
							File dirTmp = new File(context.getFilesDir().getAbsolutePath()
								+ "/thumbs/banners/posters");
							if (!dirTmp.isDirectory()) {
								dirTmp.mkdirs();
							}
							OutputStream fOut = null;
							File thumFile = new File(c.getString(1));
							fOut = new FileOutputStream(thumFile);
							resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
							fOut.flush();
							fOut.close();
							posterThumb.recycle();
							resizedBitmap.recycle();
							System.gc();
							posterThumb = null;
							resizedBitmap = null;
						}
						c.close();
					} catch (SQLiteException e) {
						Log.e("DroidSeries", e.getMessage());
					}
				}
			} catch (Exception e) {
				// does nothings
			}
		}
		return true;
	}

	private boolean u0154To0155(Context context, Display display) {
		String version = getVersion();
		if (!(version.equals("0.1.5-5") || version.equals("0.1.5-6"))) {
			Log.d("DroidSeries", "UPDATING TO VERSION 0.1.5-5!!!");
			// create new series table
			droidseries.db.execQuery("CREATE TABLE IF NOT EXISTS series_new ("
				+ "id VARCHAR PRIMARY KEY, " + // 0
				"serieId VARCHAR, " + // 1
				"language VARCHAR, " + // 2
				"serieName VARCHAR, " + // 3
				"banner VARCHAR, " + // 4
				"overview TEXT, " + // 5
				"firstAired VARCHAR, " + // 6
				"imdbId VARCHAR, " + // 7
				"zap2ItId VARCHAR, " + // 8
				"airsDayOfWeek VARCHAR, " + // 9
				"airsTime VARCHAR, " + // 10
				"contentRating VARCHAR, " + // 11
				"network VARCHAR, " + // 12
				"rating VARCHAR, " + // 13
				"runtime VARCHAR, " + // 14
				"status VARCHAR, " + // 15
				"fanart VARCHAR, " + // 16
				"lastUpdated VARCHAR, " + // 17
				"poster VARCHAR," + // 18
				"posterInCache VARCHAR, " + // 19
				"posterThumb VARCHAR" + // 20
				")");
			// copy data from the old table to the new table
			try {
				Cursor c = droidseries.db.Query("SELECT id, serieId, language, serieName, banner, overview, firstAired, "
					+ "imdbId, zap2ItId, airsDayOfWeek, airsTime, contentRating, network, rating, runtime, status, "
					+ "fanart, lastUpdated, poster, posterInCache, posterThumb FROM series");
				c.moveToFirst();
				if (c != null && c.isFirst()) {
					if (c.getCount() != 0) {
						do {
							droidseries.db.execQuery("INSERT INTO series_new (id, serieId, language, serieName, banner, overview, "
								+ "firstAired, imdbId, zap2ItId, airsDayOfWeek, airsTime, contentRating, "
								+ "network, rating, runtime, status, fanart, lastUpdated, passiveStatus, poster, "
								+ "posterInCache, posterThumb) VALUES (" + "'"
								+ c.getString(0) + "', " + "'"
								+ c.getString(1) + "', " + "'"
								+ c.getString(2) + "', " + "\""
								+ c.getString(3) + "\", " + "'"
								+ c.getString(4) + "', " + "\""
								+ c.getString(5) + "\", " + "'"
								+ c.getString(6) + "', " + "'"
								+ c.getString(7) + "', " + "'"
								+ c.getString(8) + "', " + "'"
								+ c.getString(9) + "', " + "'"
								+ c.getString(10) + "', " + "'"
								+ c.getString(11) + "', " + "'"
								+ c.getString(12) + "', " + "'"
								+ c.getString(13) + "', " + "'"
								+ c.getString(14) + "', " + "'"
								+ c.getString(15) + "', " + "'"
								+ c.getString(16) + "', " + "'"
								+ c.getString(17) + "', " + "'"
								+ c.getString(18) + "', " + "'"
								+ c.getString(19) + "', " + "'"
								+ c.getString(20) + "'" + ")");
						} while (c.moveToNext());
					}
				}
				c.close();
			} catch (SQLiteException e) {
				Log.e("DroidSeries", e.getMessage());
			}
			// drop the old table
			droidseries.db.execQuery("DROP TABLE series");
			// rename the new table
			droidseries.db.execQuery("ALTER TABLE series_new RENAME TO series");
			droidseries.db.execQuery("UPDATE droidseries SET version='" + droidseries.VERSION + "'");
		}
		return true;
	}

	private boolean u0156To0157(Context context, Display display) {
		Log.d("DroidSeries", "UPDATING TO VERSION 0.1.5-7!!!");
		String version = getVersion();
		if (!version.equals("0.1.5-7")) { 
			// update series table
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN passiveStatus INTEGER DEFAULT 0");
			droidseries.db.execQuery("UPDATE droidseries SET version='" + droidseries.VERSION + "'");
		}
		return true;
	}
	
	private boolean u0157To0157G(Context context, Display display) {
		Log.d("DroidSeries", "UPDATING TO VERSION 0.1.5-7G!!!");
		String oldVersion = getVersion();
		if (!oldVersion.equals("0.1.5-7G")) {
			// update series table
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN seasonCount INTEGER");
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN unwatchedAired INTEGER");
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN unwatched INTEGER");
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN nextEpisode VARCHAR");
			droidseries.db.execQuery("ALTER TABLE series ADD COLUMN nextAir VARCHAR");
			droidseries.db.execQuery("UPDATE droidseries SET version='" + droidseries.VERSION + "'");
			String newVersion = getVersion();
			Log.d("DroidSeries", "Database version updated from "+ oldVersion +" to "+ newVersion);
			return true;
		} else {
		return false;
		}
	}
}