package org.droidseries.ui;

import java.util.ArrayList;
import java.util.List;
import org.droidseries.droidseries;
import org.droidseries.thetvdb.model.Season;
import org.droidseries.R;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SerieSeasons extends ListActivity
{
	private final String TAG = "DroidSeries";
	private static List<Integer> iseasons;
	private static List<Season> seasons;
	private String serieid;
	// Context Menus
	private static final int ALLEPSEEN_CONTEXT = Menu.FIRST;
	private static final int ALLUPTOTHIS_CONTEXT = ALLEPSEEN_CONTEXT + 1;
	private static final int ALLEPUNSEEN_CONTEXT = ALLUPTOTHIS_CONTEXT + 1;
	private static ListView listView;
	public static SeriesSeasonsAdapter seriesseasons_adapter;
	private static Thread infoTh;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.serie_seasons);
		serieid = getIntent().getStringExtra("serieid");
		setTitle(droidseries.db.getSerieName(serieid) + " - " + getString(R.string.messages_seasons));
		seasons = new ArrayList<Season>();
		seriesseasons_adapter = new SeriesSeasonsAdapter(this, R.layout.row_serie_seasons, seasons);
		listView = getListView();
		setListAdapter(seriesseasons_adapter);
		getSeasons();
		infoTh = new Thread(null, getSeasonInfo, "info");
		infoTh.start();
		listView.setOnTouchListener(new SwipeDetect());
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				try {
					Intent serieEpisode = new Intent(SerieSeasons.this, SerieEpisodes.class);
					serieEpisode.putExtra("serieid", serieid);
					serieEpisode.putExtra("nseason", iseasons.get(position));
					startActivity(serieEpisode);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage());
				}
			}
		});
		registerForContextMenu(getListView());
	}
	
	/* context menu */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, ALLEPSEEN_CONTEXT, 0, getString(R.string.messages_context_mark_seasonseen));
		menu.add(0, ALLUPTOTHIS_CONTEXT, 0, getString(R.string.messages_context_mark_asseenuptothis));
		menu.add(0, ALLEPUNSEEN_CONTEXT, 0, getString(R.string.messages_context_mark_seasonunseen));
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int nseason = 0;
		switch (item.getItemId()) {
			case ALLEPSEEN_CONTEXT :
				nseason = iseasons.get(info.position);
				droidseries.db.updateUnwatchedSeason(serieid, nseason);
				infoTh.run();
				return true;
			case ALLEPUNSEEN_CONTEXT :
				nseason = iseasons.get(info.position);
				droidseries.db.updateWatchedSeason(serieid, nseason);
				infoTh.run();
				return true;
			case ALLUPTOTHIS_CONTEXT :
				nseason = iseasons.get(info.position);
				for (int i = 1; i <= nseason; i++) {
					droidseries.db.updateUnwatchedSeason(serieid, i);
				}
				infoTh.run();
				return true;
			default :
				return super.onContextItemSelected(item);
		}
	}

	private void getSeasons() {
		seasons = new ArrayList<Season>();
		try {
			iseasons = new ArrayList<Integer>();
			Cursor cseasons = droidseries.db.Query("SELECT season FROM serie_seasons WHERE serieId = '"+ serieid + "'");
			cseasons.moveToFirst();
			if (cseasons.getCount() != 0) {
				do {
					iseasons.add(cseasons.getInt(0));
				} while (cseasons.moveToNext());
			}
			cseasons.close();
		} catch (Exception e) {
			Log.e(TAG, "Error getting seasons");
		}
		for (int tmpS = 0; tmpS < iseasons.size(); tmpS++) {
			String tmpSeason = "";
			if (iseasons.get(tmpS) == 0) {
				tmpSeason = getString(R.string.messages_specials);
			} else {
				tmpSeason = getString(R.string.messages_season) + " " + iseasons.get(tmpS);
			}
			Season season = new Season(serieid, iseasons.get(tmpS), tmpSeason, 0, 0, true, "");
			seasons.add(season);
		}
		for (int s = 0; s < iseasons.size(); s++) {
			seriesseasons_adapter.add(seasons.get(s));
		}
	}	
	
	public static Runnable getSeasonInfo = new Runnable() {
		public void run() {
			for (int i = 0; i < seasons.size(); i++) {
				String serieId = seasons.get(i).getSerieId();
				int sNumber = seasons.get(i).getSNumber();
				int unwatched = droidseries.db.getSeasonEPUnwatched(serieId, seasons.get(i).getSNumber());
				if (seasons.get(i).getUnwatched() != unwatched) {	// Only update in adapter when something's changed
					seasons.get(i).setUnwatched(unwatched);
					int unwatchedAired = droidseries.db.getSeasonEPUnwatchedAired(serieId, sNumber);
					seasons.get(i).setUnwatchedAired(unwatchedAired);
					if (unwatchedAired != 0) {
						seasons.get(i).setCompletelyWatched(false);
						seasons.get(i).setNextEpisode(droidseries.db.getNextEpisode(serieId, sNumber));
					} else {
						seasons.get(i).setCompletelyWatched(true);
					}
				}
			}
			listView.post(new Runnable() {
				public void run() {
					seriesseasons_adapter.notifyDataSetChanged();
				}
			});
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	private class SeriesSeasonsAdapter extends ArrayAdapter<Season>
	{
		private List<Season> items;

		public SeriesSeasonsAdapter(Context context, int textViewResourceId, List<Season> seasons) {
			super(context, textViewResourceId, seasons);
			this.items = seasons;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.row_serie_seasons, null);
				holder = new ViewHolder();
				holder.season = (TextView) convertView.findViewById(R.id.serieseason);
				holder.info = (TextView) convertView.findViewById(R.id.seasoninfo);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			Season s = items.get(position);
			if (holder.season != null) {
				holder.season.setText(s.getSeason());
			}
			String infoPart1 = "";
			if (holder.info != null) {
				int unwatchedAired = s.getUnwatchedAired();
				int unwatched = s.getUnwatched();
				if (unwatched != -1) {
					if (unwatched == 0) {
						infoPart1 = getString(R.string.messages_season_completely_watched);
					} else {
						infoPart1 = unwatchedAired + " " + getString(R.string.messages_of) + " " + unwatched + " ";
						if (unwatched == 1) {
							infoPart1 += getString(R.string.messages_episode_not_watched) +"\n";
						} else {
							infoPart1 += getString(R.string.messages_episodes_not_watched) +"\n";
						}
					}
				}
				if (!s.getCompletelyWatched()) {
					holder.info.setText(infoPart1 + getString(R.string.messages_next_episode) + " "
						+ s.getNextEpisode().replace("[on]", getString(R.string.messages_on)));
				} else {
					holder.info.setText(infoPart1);
				}
			}
			return convertView;
		}
	}
	static class ViewHolder
	{
		TextView season;
		TextView info;
	}
}