package nl.asymmetrics.droidshows.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.asymmetrics.droidshows.DroidShows;
import nl.asymmetrics.droidshows.R;
import nl.asymmetrics.droidshows.thetvdb.TheTVDB;
import nl.asymmetrics.droidshows.thetvdb.model.Serie;
import nl.asymmetrics.droidshows.thetvdb.model.TVShowItem;
import nl.asymmetrics.droidshows.utils.Utils;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import org.apache.commons.io.FileUtils;

public class AddSerie extends ListActivity
{
	private static List<Serie> search_series = null;
	private TheTVDB theTVDB;
	private SeriesSearchAdapter seriessearch_adapter;
	/* DIALOGS */
	private ProgressDialog m_ProgressDialog = null;
	/* Option Menus */
	private static final int ADD_SERIE_MENU_ITEM = Menu.FIRST;
	/* Context Menus */
	private static final int ADD_CONTEXT = Menu.FIRST;
	private ListView lv;
	private Utils utils = new Utils();
	static String searchQuery = "";
	private volatile Thread threadAddShow;
	private static boolean bAddShowTh = false;
	private List<String> series = DroidShows.db.getSeriesByName();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_serie);
		List<Serie> search_series = new ArrayList<Serie>();
		this.seriessearch_adapter = new SeriesSearchAdapter(this, R.layout.row_search_series, search_series);
		setListAdapter(seriessearch_adapter);
		Intent intent = getIntent();
		getSearchResults(intent); // Guillaume
	}

	/* Options Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, ADD_SERIE_MENU_ITEM, 0, getString(R.string.search_serie)).setIcon(android.R.drawable.ic_menu_search);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case ADD_SERIE_MENU_ITEM :
				onSearchRequested();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/* context menu */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ADD_CONTEXT, 0, getString(R.string.menu_context_add_serie));
	}

	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final ListView serieList = getListView();
		switch (item.getItemId()) {
			case ADD_CONTEXT :
				final Serie tmpSerie = (Serie) serieList.getAdapter().getItem(info.position);
				addSerie(tmpSerie);
				return true;
			default :
				return super.onContextItemSelected(item);
		}
	}
	private Runnable reloadSearchSeries = new Runnable() {
		public void run() {
			seriessearch_adapter.clear();
			if (search_series != null && search_series.size() > 0) {
				seriessearch_adapter.notifyDataSetChanged();
				for (int i = 0; i < search_series.size(); i++)
					seriessearch_adapter.add(search_series.get(i));
			}
			m_ProgressDialog.dismiss();
			seriessearch_adapter.notifyDataSetChanged();
		}
	};

	private void searchSeries(String searchQuery) {
		try {
			search_series = new ArrayList<Serie>();
			search_series = theTVDB.searchSeries(searchQuery, getString(R.string.lang_code));
			if (search_series == null) {
				m_ProgressDialog.dismiss();
				Looper.prepare();
				Toast.makeText(getApplicationContext(), R.string.messages_thetvdb_con_error, Toast.LENGTH_LONG).show();
				Looper.loop();
			} else {
				runOnUiThread(reloadSearchSeries);
			}
		} catch (Exception e) {
			Log.e(DroidShows.TAG, e.getMessage());
		}
	}

	private void Search() {
		m_ProgressDialog = ProgressDialog.show(AddSerie.this, getString(R.string.messages_title_search_series), getString(R.string.messages_search_series), true, true);
		new Thread(new Runnable() {
			public void run() {
				theTVDB = new TheTVDB("8AC675886350B3C3");
				if (theTVDB.getMirror() != null) {
					searchSeries(searchQuery);
				} else {
					Log.e(DroidShows.TAG, "Error searching for TV shows");
				}
			}
		}).start();
	}

	public synchronized void startAddShowTh() {
		if (threadAddShow == null) {
			threadAddShow = new Thread();
			threadAddShow.start();
		}
	}

	public synchronized void stopAddShowTh() {
		if (threadAddShow != null) {
			Thread moribund = threadAddShow;
			threadAddShow = null;
			moribund.interrupt();
			Log.d(DroidShows.TAG, "addShow Thread stopped");
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		m_ProgressDialog.dismiss();
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		if (threadAddShow != null) {
			if (threadAddShow.isAlive()) {
				Log.i(DroidShows.TAG, "!!! Thread is running!");
				bAddShowTh = true;
			}
		}
		super.onPause();
	}

	public void addSerie(final Serie s) {
		Runnable addnewserie = new Runnable() {
			public void run() {
				// gathers the TV show and all of its data
				Serie sToAdd = theTVDB.getSerie(s.getId(), getString(R.string.lang_code));
				if (sToAdd == null) {
					m_ProgressDialog.dismiss();
					Looper.prepare();
					Toast.makeText(getApplicationContext(), R.string.messages_thetvdb_con_error, Toast.LENGTH_LONG).show();
					Looper.loop();
				} else {
					Log.d(DroidShows.TAG, "Adding TV show: getting the poster");
					// get the poster and save it in cache
					URL imageUrl = null;
					URLConnection uc = null;
					String contentType = "";
					try {
						if (bAddShowTh) {
							stopAddShowTh();
							bAddShowTh = false;
							return;
						}
						try {
							imageUrl = new URL(sToAdd.getPoster());
							uc = imageUrl.openConnection();
							// timetout, 10s for slow connections
							uc.setConnectTimeout(10000);
							contentType = uc.getContentType();
						} catch (MalformedURLException e) {
							Log.e(DroidShows.TAG, e.getMessage());
						} catch (IOException e) {
							Log.e(DroidShows.TAG, e.getMessage());
						} catch (Exception e) {
							Log.d(DroidShows.TAG, e.getMessage());
						}
						int contentLength = uc.getContentLength();
						if (!TextUtils.isEmpty(contentType)) {
							if (!contentType.startsWith("image/") || contentLength == -1) {
								// throw new IOException("This is not a binary file.");
								Log.e(DroidShows.TAG, "This is not a image.");
							}
						}
						try {
							if (bAddShowTh) {
								stopAddShowTh();
								bAddShowTh = false;
								return;
							}
							File cacheImage = new File(getApplicationContext().getFilesDir().getAbsolutePath()
								+ imageUrl.getFile().toString());
							FileUtils.copyURLToFile(imageUrl, cacheImage);
							Bitmap posterThumb = BitmapFactory.decodeFile(getApplicationContext().getFilesDir().getAbsolutePath() + imageUrl.getFile().toString());
							Display display = getWindowManager().getDefaultDisplay();
							int width = display.getWidth();
							int height = display.getHeight();
							int newHeight = (int) ((height > width ? height : width) * 0.265);
							int newWidth = (int) (1.0 * posterThumb.getWidth() / posterThumb.getHeight() * newHeight);
							if (bAddShowTh) {
								stopAddShowTh();
								bAddShowTh = false;
								return;
							}
							Log.d(DroidShows.TAG, "Adding TV show: resizing the poster and creating the thumbnail");
							Bitmap resizedBitmap = Bitmap.createScaledBitmap(posterThumb, newWidth, newHeight, true);
							File dirTmp = new File(getApplicationContext().getFilesDir().getAbsolutePath() +"/thumbs/banners/posters");
							if (!dirTmp.isDirectory()) {
								dirTmp.mkdirs();
							}
							OutputStream fOut = null;
							File thumFile = new File(getApplicationContext().getFilesDir().getAbsolutePath(), "thumbs"
								+ imageUrl.getFile().toString());
							fOut = new FileOutputStream(thumFile);
							resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
							fOut.flush();
							fOut.close();
							// removes the bitmaps from memory
							posterThumb.recycle();
							resizedBitmap.recycle();
							System.gc();
							posterThumb = null;
							resizedBitmap = null;
							sToAdd.setPosterThumb(getApplicationContext().getFilesDir().getAbsolutePath()
								+ "/thumbs" + imageUrl.getFile().toString());
							cacheImage.delete();
							sToAdd.setPosterInCache("true");
						} catch (Exception e) {
							sToAdd.setPosterInCache("");
							Log.e(DroidShows.TAG, "Error copying the poster to cache.");
						}
					} catch (Exception e) {
						Log.e(DroidShows.TAG, e.getMessage());
					}
					boolean sucesso = false;
					try {
						if (bAddShowTh) {
							stopAddShowTh();
							bAddShowTh = false;
							return;
						}
						Log.d(DroidShows.TAG, "Adding TV show: saving TV show to database");
						sToAdd.saveToDB(DroidShows.db);
						Log.d(DroidShows.TAG, "Adding TV show: creating the TV show item");
						int nseasons = DroidShows.db.getSeasonCount(sToAdd.getId());
						String nextEpisode = DroidShows.db.getNextEpisode(sToAdd.getId(), -1).replace("[on]", DroidShows.on);
						Date nextAir = DroidShows.db.getNextAir(sToAdd.getId(), -1);
						int unwatchedAired = DroidShows.db.getEPUnwatchedAired(sToAdd.getId());
						int unwatched = DroidShows.db.getEPUnwatched(sToAdd.getId());
						Drawable d = Drawable.createFromPath(sToAdd.getPosterThumb());
						TVShowItem tvsi = new TVShowItem(sToAdd.getId(), sToAdd.getPosterThumb(), d, sToAdd.getSerieName(), nseasons, nextEpisode, nextAir, unwatchedAired, unwatched, false, sToAdd.getStatus());
						DroidShows.series.add(tvsi);
						series.add(sToAdd.getId());
						runOnUiThread(DroidShows.updateListView);
						runOnUiThread(reloadSearchSeries);
						sucesso = true;
					} catch (Exception e) {
						Log.e(DroidShows.TAG, "Error adding TV show");
					}
					// m_ProgressDialog.dismiss();
					if (!bAddShowTh) {
						m_ProgressDialog.dismiss();
						bAddShowTh = false;
					}
					if (sucesso) {
						CharSequence text = String.format(getString(R.string.messages_series_success), sToAdd.getSerieName());
						Looper.prepare();
						Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
						Looper.loop();
					}
				}
			}
		};
		boolean alreadyExists = false;
		for (String serieId : series) {
			if (serieId.equals(s.getId())) {
				alreadyExists = true;
				break;
			}
		}
		if (!alreadyExists) {
			threadAddShow = new Thread(null, addnewserie, "MagentoBackground");
			threadAddShow.start();
			m_ProgressDialog = ProgressDialog.show(AddSerie.this, getString(R.string.messages_title_adding_serie), getString(R.string.messages_adding_serie), true, false);
		}
	}

	// Guillaume: searches from within this activity were discarded
	@Override
	protected void onNewIntent(Intent intent) {
		getSearchResults(intent);
	}

	private void getSearchResults(Intent intent) {
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			searchQuery = intent.getStringExtra(SearchManager.QUERY);
			TextView title = (TextView) findViewById(R.id.add_serie_title);
			title.setText(getString(R.string.dialog_search) + " " + searchQuery);
			if (utils.isNetworkAvailable(AddSerie.this)) {
				Search();
			} else {
				Toast.makeText(getApplicationContext(), R.string.messages_no_internet, Toast.LENGTH_LONG).show();
			}
		}
		lv = getListView();
		lv.setOnTouchListener(new SwipeDetect());
		registerForContextMenu(getListView());
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final Serie sToAdd = AddSerie.search_series.get(position);
		AlertDialog sOverview = new AlertDialog.Builder(this)
		.setIcon(R.drawable.icon)
		.setTitle(sToAdd.getSerieName())
		.setMessage(sToAdd.getOverview())
		.setPositiveButton(getString(R.string.menu_context_add_serie), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				addSerie(sToAdd);
			}
		})
		.setNegativeButton(getString(R.string.dialog_Cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		})
		.show();
		sOverview.setCanceledOnTouchOutside(true);
		
		for (String serieId : series)
			if (serieId.equals(sToAdd.getId())) {
				sOverview.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
				break;
			}
	}
	
	private class SeriesSearchAdapter extends ArrayAdapter<Serie>
	{
		private List<Serie> items;

		public SeriesSearchAdapter(Context context, int textViewResourceId, List<Serie> series) {
			super(context, textViewResourceId, series);
			this.items = series;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.row_search_series, parent, false);
			}
			final Serie o = items.get(position);
			if (o != null) {
				TextView sn = (TextView) v.findViewById(R.id.seriename);
				CheckedTextView ctv = (CheckedTextView) v.findViewById(R.id.addserieBtn);
				if (sn != null) {
					sn.setText(o.getSerieName());
				}
				if (ctv != null) {
					boolean alreadyExists = false;
					for (String serieId : series) {
						if (serieId.equals(o.getId())) {
							alreadyExists = true;
							break;
						}
					}
					if (alreadyExists) {
						ctv.setCheckMarkDrawable(getResources().getDrawable(R.drawable.star));
						// ctv.setVisibility(View.GONE);
						ctv.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								// does nothing
								return;
							}
						});
					} else {
						// ctv.setVisibility(View.VISIBLE);
						ctv.setCheckMarkDrawable(getResources().getDrawable(R.drawable.add));
						ctv.setOnClickListener(new OnClickListener() {
							public void onClick(View v) {
								bAddShowTh = false;
								addSerie(o);
							}
						});
					}
				}
			}
			return v;
		}
	}
}