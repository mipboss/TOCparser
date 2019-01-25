package marinater.dada_file_index;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenericSearchRecycler extends Fragment{
	private RecyclerView mRecyclerView;
	private GenericSearchAdapter mAdapter;
	private RecyclerView.LayoutManager mLayoutManager;
	private final boolean[] switchStatuses = new boolean[8];

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getActivity().setTitle(getArguments().getString("TITLE"));
		String folderName = getArguments().getString("FILE_NAME");
		String fileName = getArguments().getString("FOLDER_PATH");
		File buttonFile = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS + "/toc/" + fileName);
		File fileFolder = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS + "/toc/" + folderName);

		if (!buttonFile.exists() || !fileFolder.exists()){
			View view = inflater.inflate(R.layout.layout_file_not_found, container, false);
			Log.e("Hello", "nope");
			return view;
		}


		View view = inflater.inflate(R.layout.layout_lyricsbyname,
				container, false);

		mRecyclerView = (RecyclerView) view.findViewById(R.id.lyrics_recycler);
		mLayoutManager = new LinearLayoutManager(this.getActivity());
		mRecyclerView.setLayoutManager(mLayoutManager);
		mAdapter = new GenericSearchAdapter(this.getActivity());
		mRecyclerView.setAdapter(mAdapter);
		DividerItemDecoration itemDecorator = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
		itemDecorator.setDrawable(ContextCompat.getDrawable(getContext(), R.drawable.divider));
		mRecyclerView.addItemDecoration(itemDecorator);

		final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.settingsFAB);
		final LinearLayout settingsMenu = (LinearLayout) view.findViewById(R.id.settingsMenu);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int visibility = settingsMenu.getVisibility();
				if(visibility == View.GONE) {
					settingsMenu.setVisibility(View.VISIBLE);
				}
				else{
					settingsMenu.setVisibility(View.GONE);
				}
			}
		});

		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.searchbar_menu, menu);

		final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				mAdapter.getFilter().filter(newText);
				return false;
			}
		});

		final Switch[] filterSwitches = {
				getView().findViewById(R.id.switchDiwali),
				getView().findViewById(R.id.switchGarba),
				getView().findViewById(R.id.switchPublic),
				getView().findViewById(R.id.switchGurupunam),
				getView().findViewById(R.id.switchJayanthi),
				getView().findViewById(R.id.switchPunyatithi),
				getView().findViewById(R.id.switchSwami),
				getView().findViewById(R.id.switchNotSung)
		};

		ToggleButton filterToggle = (ToggleButton) getView().findViewById(R.id.toggleButton2);
		filterToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ToggleButton b = (ToggleButton) view.findViewById(R.id.toggleButton2);
				boolean buttonState = b.isChecked();
				if(!buttonState) {
					for (Switch elem : filterSwitches) {
						elem.setChecked(false);
					}
				}
			}
		});

		ToggleButton styleToggle = (ToggleButton) getView().findViewById(R.id.toggleButton3);
		styleToggle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ToggleButton c = (ToggleButton) view;
				if(c.isChecked()){
					mAdapter.setEnglish();
				}
				else{
					mAdapter.setDefault();
				}
			}
		});

		for(int i = 0; i < filterSwitches.length; i++){
			filterSwitches[i].setTag(i);
			filterSwitches[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					ToggleButton b = (ToggleButton) getView().findViewById(R.id.toggleButton2);
					boolean buttonState = b.isChecked();
					if(isChecked && !buttonState){
						b.setChecked(true);
					}
					int tag = Integer.parseInt(buttonView.getTag().toString());
					switchStatuses[tag] = isChecked;
					mAdapter.updateVals(switchStatuses);
					mAdapter.getFilter().filter(searchView.getQuery());
				}
			});
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

		setHasOptionsMenu(true);
	}

}


class GenericSearchAdapter extends RecyclerView.Adapter<GenericSearchAdapter.GenericViewHolder> implements Filterable{
	List<GenericBook> currentBooklist;
	List<GenericBook> fullBookList;

	Context mCont;
	Boolean isGujaratiStyle = true;

	boolean[] searchSettings = new boolean[8];

	public static class GenericViewHolder extends RecyclerView.ViewHolder {
		CardView mCardView;
		TextView name, alternateTitle, pageBox;

		public GenericViewHolder(CardView c) {
			super(c);
			mCardView = c;
			name = (TextView) c.findViewById(R.id.title);
			alternateTitle = (TextView) c.findViewById(R.id.alt);
			pageBox = (TextView) c.findViewById(R.id.page);

			c.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					FileLauncher.launchPDF(mCardView.getContext(), "swarvadini/" + v.getTag() + ".pdf");
				}
			});
		}
	}

	public GenericSearchAdapter (Context context) {
		fullBookList  = GenericCSV.toBookList(GenericCSV.parse(context));
		currentBooklist = new ArrayList<>();

		for (GenericBook b : fullBookList){
			currentBooklist.add(b.copy());
		}

		mCont = context;
	}

	void setEnglish(){
		isGujaratiStyle = false;
		notifyDataSetChanged();
	}

	void setDefault(){
		isGujaratiStyle = true;
		notifyDataSetChanged();
	}

	@Override
	public GenericSearchAdapter.GenericViewHolder onCreateViewHolder(ViewGroup parent,
													 int viewType) {

		CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.books_layout, parent, false);
		GenericViewHolder vh = new GenericViewHolder(v);
		return vh;
	}

	@Override
	public void onBindViewHolder(GenericViewHolder holder, int position) {
		if(isGujaratiStyle) {
			holder.name.setText(currentBooklist.get(position).getTitle());
			holder.alternateTitle.setText(currentBooklist.get(position).getAlternate());
		}
		else{
			holder.alternateTitle.setText(currentBooklist.get(position).getTitle());
			holder.name.setText(currentBooklist.get(position).getAlternate());
		}

		holder.mCardView.setTag(currentBooklist.get(position).getPage());
		holder.pageBox.setText(currentBooklist.get(position).getPage().replaceFirst("page_0*", ""));

	}

	public void updateVals(boolean[] p){
		searchSettings = p;
	}

	@Override
	public int getItemCount() {
		return currentBooklist.size();
	}

	@Override
	public Filter getFilter(){
		return searchFilter;
	}

	private Filter searchFilter = new Filter(){
		@Override
		protected FilterResults performFiltering(CharSequence constraint){
			List<GenericBook> filteredList = new ArrayList<>();
			List<GenericBook> pageNumList = new ArrayList<>();

			if((constraint == null || constraint.length() == 0)){
				for(GenericBook item : fullBookList){
					if (matchesSettings(item.getFilterVals())) {
						filteredList.add(item);
					}
				}
			}
			else{
				String filterPattern = constraint.toString().toLowerCase().trim();
				for (GenericBook item : fullBookList) {
					if (item.getAlternate().toLowerCase().contains(filterPattern) && matchesSettings(item.getFilterVals())) {
						filteredList.add(item);
					}

					else if (item.getPage().toLowerCase().contains(filterPattern)) {
						pageNumList.add(item);
					}
				}
			}

			filteredList.addAll(pageNumList);
			FilterResults results = new FilterResults();
			results.values = filteredList;
			return results;
		}
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results){
			currentBooklist.clear();
			currentBooklist.addAll((List) results.values);
			notifyDataSetChanged();
		}
	};

	private boolean matchesSettings(String itemVals){
		boolean isAnySettingTrue = false;
		for (int i = 0; i < ((searchSettings.length > itemVals.length())?itemVals.length():searchSettings.length); i++){
			if(searchSettings[i]){
				if (itemVals.charAt(i) == 'Y'){
					return true;
				}
				isAnySettingTrue = true;
			}
		}
		return !isAnySettingTrue;
	}
}

class GenericBook {
	private String title, alternate, page, vals, info;
	public GenericBook() {}

	public GenericBook(String title, String alternate, String page, String vals) {
		this.title = title;
		this.alternate = alternate;
		this.page = page;
		this.vals = vals;
	}

	public String getTitle() {
		return title;
	}
	public String getPage() {
		return page;
	}
	public String getFilterVals() { return vals;};
	public String getAlternate() {
		return alternate;
	}
	public GenericBook copy(){
		return new GenericBook(this.title, this.alternate, this.page, this.vals);
	}
}

class GenericCSV {
	public static List<GenericBook> toBookList(List<ArrayList<String>> stringList){
		List<GenericBook>  listBook = new ArrayList<>();

		for(int i = 0; i < stringList.get(0).size(); i++){
			GenericBook tempBook = new GenericBook(stringList.get(0).get(i), stringList.get(1).get(i), stringList.get(2).get(i), stringList.get(3).get(i));
			listBook.add(tempBook);
		}
		return listBook;
	}

	public static List<ArrayList<String>> parse(Context mContext){
		StringBuilder sb = new StringBuilder();

		String fileString = "csv4.txt";
		File file = new File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOCUMENTS + "/toc/Button-Maps/" + fileString);

		String[] lineElems;
		List<ArrayList<String>> masterList = new ArrayList<>();
		ArrayList<String> titles = new ArrayList<>();
		ArrayList<String> alts = new ArrayList<>();
		ArrayList<String> pages = new ArrayList<>();

		ArrayList<String> filterValues = new ArrayList<>();

		try {
			BufferedReader nBufferedReader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = nBufferedReader.readLine()) != null) {
				lineElems = line.split("\\t");
				if (lineElems.length < 4){
					Log.e("Hello", line);
					break;
				}
				titles.add(lineElems[1]);
				pages.add(lineElems[0]);
				alts.add(lineElems[2]);
				filterValues.add(lineElems[3]);
				
			}
		}catch (IOException e){Log.e("Hello", "IO exception");}

		masterList.add(titles);
		masterList.add(alts);
		masterList.add(pages);
		masterList.add(filterValues);

		return masterList;
	}
}