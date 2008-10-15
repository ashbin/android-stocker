/*

Copyright (C) 2008 David B. Moffett <davidbmoffett@gmail.com>

This file is part of Stocker.  A simple program to deliver stock 
quotes to the Android platform.

Stocker is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Stocker is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Stocker.  If not, see <http://www.gnu.org/licenses/>.

 */


package com.twofuse.stocker;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;


import com.twofuse.stocker.R;
import android.app.Activity;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.KeyEvent.Callback;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsoluteLayout;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.LayoutInflater;


public class Stocker extends ListActivity implements View.OnClickListener, KeyEvent.Callback {
	private static final String TAG = "Stocker";
	private static final String SYMBOL = "symbol";
	private static final String CURRENT = "current";
	private static final String CHANGE = "change";
	private static final String VOLUME = "volume";
	private static final String OPEN = "open";
	private static final String DAY_LOW = "day-low";
	private static final String DAY_HIGH = "day-high";
	private static final String WK52_DAY_LOW = "52wkLow";
	private static final String WK52_DAY_HIGH = "52wkHi";
	private static final String COMPANY_NAME = "companyName";

	private static Activity __sharedActivity = null;

	private QuoteAdaptor quoteAdaptor;
	private ViewGroup viewContainer;
	private View quoteView;
	private View quoteEntryView;
	private EditText symbolEntryTextField;
	private Button addButton;
	private Button cancelButton;
	private Button okButton;
	private Button deleteButton;
	private Dialog dialog = null;
	private Dialog creditDialog = null;
	private TextView currentTextView, changeTextView, openTextView, volumeTextView, dayLowTextView, dayHighTextView, wk52LowTextView, wk52HighTextView;
	private ImageView chartView;
	private int currentSelectedIndex;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		__sharedActivity = this;

		setContentView(R.layout.main);

		getListView().setEmptyView(findViewById(R.id.empty));
		quoteAdaptor = new QuoteAdaptor(this, this);
		this.setListAdapter(quoteAdaptor);

		addButton = (Button) findViewById(R.id.add_symbols_button);
		addButton.setOnClickListener(this);

		symbolEntryTextField = (EditText) findViewById(R.id.stock_symbols);

		viewContainer = (ViewGroup) findViewById(R.id.container);
		quoteView = (View) findViewById(R.id.list_container);
		quoteEntryView = (View) findViewById(R.id.entry_container);

		// ((ViewGroup) viewContainer.getParent()).setKeepAnimations(true);
	}

	protected void onResume(){
		super.onResume();
		if(quoteAdaptor != null)
			quoteAdaptor.startRefresh();
	}

	protected void onStop(){
		super.onStop();
		quoteAdaptor.stopRefresh();
	}

	static public Activity activity(){
		return __sharedActivity;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		/*
        menu.add(0, 0, "Satellite View").setIcon(R.drawable.satview);
        menu.add(0, 1, "Credits");
        menu.add(0, 2, "Tutorial").setIcon(R.drawable.help);
        menu.add(0, 3, "Version 0.33c").setIcon(R.drawable.vvlogo);
		 */
		// menu.add(0, 0, 0, "Preferences");
		menu.add(0, 1, 0, "Credits");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case 0:
			applyRotation(180, 90, -1);
			return true;
		case 1:
			if(creditDialog == null){
				creditDialog = new Dialog(__sharedActivity);
				creditDialog.setContentView(R.layout.credits);
				okButton = (Button) creditDialog.findViewById(R.id.ok_button);
				okButton.setOnClickListener(this);
			}
			creditDialog.show();
			return true;
		case 2:
		case 3:        	
		}
		return false;
	}


	protected void onListItemClick(ListView l, View v, int position, long id){
		super.onListItemClick(l,v, position, id);
		JSONObject quote;
		try {
			quote = quoteAdaptor.portfolio.getQuoteForIndex(position);
			if(dialog == null){
				dialog = new Dialog(__sharedActivity);
				dialog.setContentView(R.layout.quote_detail);
				deleteButton = (Button) dialog.findViewById(R.id.delete);
				deleteButton.setOnClickListener(this);
				cancelButton = (Button) dialog.findViewById(R.id.close);
				cancelButton.setOnClickListener(this);
				currentTextView = (TextView) dialog.findViewById(R.id.current);
				changeTextView = (TextView) dialog.findViewById(R.id.change);
				openTextView = (TextView) dialog.findViewById(R.id.open);
				volumeTextView = (TextView) dialog.findViewById(R.id.volume);
				dayLowTextView = (TextView) dialog.findViewById(R.id.day_low);
				dayHighTextView = (TextView) dialog.findViewById(R.id.day_high);
				wk52LowTextView = (TextView) dialog.findViewById(R.id.wk_52_low);
				wk52HighTextView = (TextView) dialog.findViewById(R.id.wk_52_high);
				chartView = (ImageView)dialog.findViewById(R.id.chart_view);
			}
			dialog.setTitle(quote.getString(COMPANY_NAME));

			String str = quote.getString(CHANGE);

			if(str.indexOf('-') > -1)
				changeTextView.setTextColor(0xffb22222);	
			else changeTextView.setTextColor(0xff2e8b57);

			currentTextView.setText(quote.getString(CURRENT));
			changeTextView.setText("  (" + str + ")");
			openTextView.setText(quote.getString(OPEN));
			volumeTextView.setText(quote.getString(VOLUME));
			dayLowTextView.setText(quote.getString(DAY_LOW));
			dayHighTextView.setText(quote.getString(DAY_HIGH));
			wk52LowTextView.setText(quote.getString(WK52_DAY_LOW));
			wk52HighTextView.setText(quote.getString(WK52_DAY_HIGH));
			chartView.setImageBitmap(quoteAdaptor.portfolio.getChartForSymbol(quote.getString(SYMBOL)));
			dialog.show();
			currentSelectedIndex = position;
			Log.e(TAG, "Quote selected: " + quote.getString(SYMBOL));	
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	private void addSymbol(){
		String symbolsStr = symbolEntryTextField.getText().toString();
		symbolsStr = symbolsStr.replace("\n", " ");
		String symbolArray[] = symbolsStr.split(" ");
		int index, count = symbolArray.length;
		ArrayList<String> symbolList = new ArrayList<String>();
		for(index = 0; index < count; index++){
			symbolList.add(symbolArray[index]);
		}
		quoteAdaptor.addSymbolsToPortfolio(symbolList);
		symbolEntryTextField.setText(null);
	}

	public void onClick(View view) {
		if(view == addButton){
			addSymbol();
		} else if(view == cancelButton){
			dialog.dismiss();
		} else if(view == deleteButton){
			quoteAdaptor.removeQuoteAtIndex(currentSelectedIndex);
			dialog.dismiss();
		} else if(view == okButton){
			creditDialog.dismiss();
		} else if(view.getParent() instanceof RelativeLayout){
			RelativeLayout rl = (RelativeLayout)view.getParent();
			this.onListItemClick(getListView(), rl, rl.getId()-33, rl.getId());
		} else if(view instanceof RelativeLayout){
			this.onListItemClick(getListView(), view, view.getId()-33, view.getId());
		}
		/* else {
    		if(view != quoteView)
    			applyRotation(0, 0, 90);
    		else	
    			applyRotation(-1, 180, 90);
    	} */
	}    

	private void applyRotation(final int position, final float start, final float end) {
		// Find the center of the container
		final float centerX = viewContainer.getWidth() / 2.0f;
		final float centerY = viewContainer.getHeight() / 2.0f;

		// Create a new 3D rotation with the supplied parameter
		// The animation listener is used to trigger the next animation
		final Rotate3dAnimation rotation = new Rotate3dAnimation(start, end, centerX, centerY, 310.0f, true);
		rotation.setDuration(500);
		rotation.setFillAfter(true);
		rotation.setInterpolator(new AccelerateInterpolator());
		rotation.setAnimationListener(new DisplayNextView(position));

		viewContainer.startAnimation(rotation);
	}

	private final class DisplayNextView implements Animation.AnimationListener {
		private final int mPosition;

		private DisplayNextView(int position) {
			mPosition = position;
		}

		public void onAnimationStart(Animation animation) {
		}

		public void onAnimationEnd(Animation animation) {
			viewContainer.post(new SwapViews(mPosition));
		}

		public void onAnimationRepeat(Animation animation) {
		}
	}

	/**
	 * This class is responsible for swapping the views and start the second
	 * half of the animation.
	 */
	private final class SwapViews implements Runnable {
		private final int mPosition;

		public SwapViews(int position) {
			mPosition = position;
		}

		public void run() {
			final float centerX = viewContainer.getWidth() / 2.0f;
			final float centerY = viewContainer.getHeight() / 2.0f;
			Rotate3dAnimation rotation;

			if (mPosition > -1) {
				quoteView.setVisibility(View.GONE);
				quoteEntryView.setVisibility(View.VISIBLE);
				quoteEntryView.requestFocus();

				rotation = new Rotate3dAnimation(90, 180, centerX, centerY, 310.0f, false);
			} else {
				quoteEntryView.setVisibility(View.GONE);
				quoteView.setVisibility(View.VISIBLE);
				quoteView.requestFocus();

				rotation = new Rotate3dAnimation(90, 0, centerX, centerY, 310.0f, false);
			}

			rotation.setDuration(500);
			rotation.setFillAfter(true);
			rotation.setInterpolator(new DecelerateInterpolator());

			viewContainer.startAnimation(rotation);
		}
	}



	public class QuoteAdaptor extends BaseAdapter implements ListAdapter, Runnable {
		private static final int SYMBOL_ID = 1;
		private static final int CURRENT_ID = 2;
		private static final int CHANGE_ID = 2;
		private static final int DISPLAY_COUNT = 10;
		public Portfolio portfolio;
		private boolean forceUpdate = false;

		Context context;
		Stocker stocker;
		LayoutInflater inflater;

		QuoteRefreshTask quoteRefreshTask = null;
		int progressInterval;
		Handler messageHandler = new Handler();


		public QuoteAdaptor(Stocker aController, Context c) {
			context = c;
			stocker = aController;
			portfolio = new Portfolio();
		}

		public int getCount() {
			return portfolio.stocksInPortfolio();
		}

		public Object getItem(int position) {
			try {
				return portfolio.getQuoteForIndex(position);
			} catch (JSONException e) {
				Log.e(TAG, e.getMessage());
			}	
			return null;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			JSONObject quote;
			inflater = LayoutInflater.from(context);
			RelativeLayout cellLayout = (RelativeLayout)inflater.inflate(R.layout.quote_cell, null);
			cellLayout.setMinimumWidth(parent.getWidth());
			int color;
			stocker.setProgress(progressInterval*(position + 1));
			if(position % 2 > 0)
				color = Color.rgb(48,92,131);
			else 
				color = Color.rgb(119,138,170);
			cellLayout.setBackgroundColor(color);
			try {
				quote = portfolio.getQuoteForIndex(position);
				TextView field = (TextView)cellLayout.findViewById(R.id.symbol);
				field.setBackgroundColor(color);
				field.setText(quote.getString(SYMBOL));
				field.setClickable(true);
				field.setOnClickListener(stocker);

				field = (TextView)cellLayout.findViewById(R.id.current);
				field.setText(quote.getString(CURRENT));
				field.setClickable(true);
				field.setOnClickListener(stocker);

				field = (TextView)cellLayout.findViewById(R.id.change);
				field.setClickable(true);
				field.setOnClickListener(stocker);
				String current = quote.getString(CHANGE);
				if(current.indexOf("+") >= 0)
					field.setBackgroundColor(0xff2e8b57);
				else field.setBackgroundColor(0xffb22222);
				field.setText(current);
			} catch(JSONException e){
				Log.e(TAG, e.getMessage());
			}

			cellLayout.setId(position + 33);
			cellLayout.setClickable(true);
			cellLayout.setOnClickListener(stocker);
			return cellLayout;
		}



		public boolean areAllItemsSelectable() {
			return true;
		}

		public boolean isSelectable(int arg0) {
			return true;
		}

		public void unregisterDataSetObserver(DataSetObserver arg0) {
			// TODO Auto-generated method stub

		}

		public void stopRefresh(){
			quoteRefreshTask.cancelTimer();
			quoteRefreshTask = null;
		}

		public void startRefresh(){
			if(quoteRefreshTask == null)
				quoteRefreshTask = new QuoteRefreshTask(this);
		}


		public void refreshQuotes(){
			messageHandler.post(this);		
		}

		public void run(){
			if(portfolio.stocksInPortfolio() > 0 && portfolio.quoteCount() == 0){
				forceUpdate = true;
			}
			if(portfolio.stocksInPortfolio() > 0){
				if(portfolio.isMarketOpen() || forceUpdate ){
					forceUpdate = false;
					if(portfolio.stocksInPortfolio() > DISPLAY_COUNT)
						progressInterval = 10000/portfolio.quoteCount()+1;
					else
						progressInterval = 10000/DISPLAY_COUNT;
					// stocker.setProgressBarVisibility(true);
					// stocker.setProgress(progressInterval);
					portfolio.refreshStocks();
				}
				this.notifyDataSetChanged();
			}
		}

		public void addSymbolsToPortfolio(ArrayList<String> symbols){
			forceUpdate = true;
			portfolio.addSymbolsToPortfolio(symbols);
			messageHandler.post(this);
		}

		public void removeQuoteAtIndex(int index){
			forceUpdate = true;
			portfolio.removeQuoteByIndex(index);
			messageHandler.post(this);
		}

		public class QuoteRefreshTask extends TimerTask {
			QuoteAdaptor quoteAdaptor;
			Timer        refreshTimer;
			final static int  TENSECONDS = 10000;

			public QuoteRefreshTask(QuoteAdaptor anAdaptor){
				refreshTimer = new Timer("Quote Refresh Timer");
				refreshTimer.schedule(this, TENSECONDS, TENSECONDS);
				quoteAdaptor = anAdaptor;
			}

			public void run(){
				messageHandler.post(quoteAdaptor);
			}

			public void startTimer(){
				if(refreshTimer == null){
					refreshTimer = new Timer("Quote Refresh Timer");
					refreshTimer.schedule(this, TENSECONDS, TENSECONDS);
				}
			}

			public void cancelTimer(){
				this.cancel();
				refreshTimer.cancel();
				refreshTimer = null;
			}

		}


	}


	public boolean onKeyDown(int keyCode, KeyEvent event){
		return false;    	
	}

	public boolean onKeyMultiple(int keyCode, KeyEvent event){
		return false;
	}

	public boolean onKeyUp(int keyCode, KeyEvent event){
		if(keyCode == KeyEvent.KEYCODE_ENTER){
			addSymbol();
			return true;
		}
		return false;
	}

}