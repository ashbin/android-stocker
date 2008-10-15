/*

Copyright (C) 2008 David B. Moffett <davidbmoffett@gmail.com>

This file is part of Stocker.  A simple program to deliver stock 
quotes to the Andriod platform.
    
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

import java.lang.System;
import java.net.URLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import android.graphics.drawable.BitmapDrawable;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.util.Log;
import com.twofuse.stocker.Stocker;


public class Portfolio {
	private static final String OK = "OK";
	private static final String STATUS = "status";
	private static final String TAG = "Stocker.Portfolio";
	private static final String QUOTES = "quotes";
	private static final String QURL = "http://doublediamond.com:1313/quote?sym=";
	private static final String ADD_URL = "http://doublediamond.com:1315/add_symbol?sym=";
//	private static final String QURL = "http://192.168.0.11:1313/quote?sym=";
//	private static final String ADD_URL = "http://192.168.0.11:1315/add_symbol?sym=";
	private static final String GRAPH_URL = "http://ichart.finance.yahoo.com/t?s=";
	private static final String SYMBOL_FILE_NAME = "symbols.txt";
	private int BUF_SIZE = 16384;
	private JSONObject quoteResult;
	private JSONArray  quotes;
	private ArrayList  <String>stocks;

	public Portfolio(){
		super();
		this.readPortfolio();
		if(stocks != null)
			this.refreshStocks();
	}

	public boolean isMarketOpen(){
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("New York City"));
		int dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK);
		if(dayOfWeek > java.util.Calendar.SUNDAY && dayOfWeek < java.util.Calendar.SATURDAY){
			int hourOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY);
			int minuteOfDay = cal.get(java.util.Calendar.MINUTE);
			if(hourOfDay > 6 && minuteOfDay > 30 && hourOfDay < 16)
				return true;	
		}
		return true;
	}

	private JSONObject parseQuotesFromStream(InputStream aStream){
		if(aStream != null){
			BufferedInputStream iStream = new BufferedInputStream(aStream);
			InputStreamReader iReader = new InputStreamReader(iStream);
			StringBuffer strBuf = new StringBuffer();
			char buf[] = new char[BUF_SIZE];
			try {
				int charsRead;
				while((charsRead = iReader.read(buf, 0, buf.length)) != -1){
					strBuf.append(buf, 0, charsRead);
				}
			} catch(IOException iox){
				Log.e(TAG, iox.getMessage());
			}
			try {
				JSONObject jobj = new JSONObject(strBuf.toString());
				return jobj;
			} catch(JSONException x){
				Log.e(TAG, x.getMessage());
			}
		}
		return null;
	}


	protected JSONObject __getQuotesForArray(ArrayList<String> stockSymbols){
		if(stockSymbols != null && stockSymbols.size() > 0){
			HttpClient req = new DefaultHttpClient();
			// req.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			StringBuffer buf = new StringBuffer();
			int index, count = stockSymbols.size();
			buf.append(QURL);
			buf.append(stockSymbols.get(0));
			for(index = 1; index < count; index++){
				buf.append(",");	
				buf.append(stockSymbols.get(index));
			}
			try {
				HttpGet httpGet = new HttpGet(buf.toString());
				HttpResponse response = req.execute(httpGet);
				try {
					InputStream iStream = response.getEntity().getContent();
					JSONObject obj;
					obj = parseQuotesFromStream(iStream);
					iStream.close();
					return obj;
				} catch ( IOException iox ){
					iox.printStackTrace();
				}
			} catch (IOException usx){
				Log.e(TAG, usx.getMessage());
			}
		}
		return null;
	}


	protected void __addQuotes(ArrayList<String> stockSymbols){
		if(stockSymbols != null && stockSymbols.size() > 0){
			HttpClient req = new DefaultHttpClient();
//			req.getHttpConnectionManager().getParams().setConnectionTimeout(30000);
			StringBuffer buf = new StringBuffer();
			int index, count = stockSymbols.size();
			buf.append(ADD_URL);
			buf.append(stockSymbols.get(0));
			for(index = 1; index < count; index++){
				buf.append(",");	
				buf.append(stockSymbols.get(index));
			}
			try {
				HttpGet httpGet = new HttpGet(buf.toString());
				HttpResponse response = req.execute(httpGet);
				try {
					InputStream iStream = response.getEntity().getContent();
					iStream.close();
				} catch ( IOException iox ){
					iox.printStackTrace();
				}
			} catch (Exception usx){
				Log.e(TAG, usx.getMessage());
			}
		}
	}

	private void savePortfolio(){
		if(stocks.size() > 0){
			FileOutputStream outStream = null;
			OutputStreamWriter oWriter;
			try {
				outStream = Stocker.activity().getApplication().openFileOutput(SYMBOL_FILE_NAME, 0);
				oWriter = new OutputStreamWriter(outStream); 
				StringBuffer buf = new StringBuffer();
				int index, count = stocks.size();
				buf.append(stocks.get(0));
				for(index = 1; index < count; index++){
					buf.append(",");	
					buf.append(stocks.get(index));
				}
				String outStr = buf.toString();
				oWriter.write(outStr, 0, outStr.length());
				oWriter.close();
				outStream.close();
			} catch(IOException iox){
				Log.e(TAG, iox.getMessage());
			}
		}
	}

	private void readPortfolio(){
		File fullPath;
		if(stocks == null) stocks = new ArrayList<String>();
		try {
			FileInputStream inStream;
			BufferedReader bReader;
			fullPath = Stocker.activity().getApplication().getFileStreamPath(SYMBOL_FILE_NAME);
			if(fullPath.canRead()){
				inStream = new FileInputStream(fullPath);
				bReader = new BufferedReader(new InputStreamReader(inStream));
				String quoteStr = bReader.readLine();
				String strArray[] = quoteStr.split(",");
				int index, count = strArray.length;
				stocks.clear();
				for(index = 0; index < count; index++) stocks.add(strArray[index]);
				bReader.close();
				inStream.close();
			}
		} catch(IOException iox){
			Log.e(TAG, iox.getMessage());
		}
	}

	public synchronized void addSymbolsToPortfolio(ArrayList<String> stockList){
		if(stockList != null){
			if(stocks == null){ 
				stocks = new ArrayList<String>();
				stocks.addAll(stockList);
			} else {
				int i1, i2, c1 = stocks.size(), c2 = stockList.size();
				ArrayList <String>newStocks = new ArrayList<String>();
				boolean foundSymbol = false;
				for(i2 = 0; i2 < c2; i2++){
					String newSymbol = stockList.get(i2);
					for(i1 = 0; i1 < c1; i1++){
						if(newSymbol.equals(stocks.get(i1))){
							foundSymbol = true;
							break;
						}
					}
					if(!foundSymbol){
						newStocks.add(newSymbol);
					}
					foundSymbol = false;
				}
				if(stockList != null){
					__addQuotes(newStocks);
					stocks.addAll(newStocks);
				}
			}
			this.savePortfolio();
		}
	}
	
	public Bitmap getChartForSymbol(String symbol){
		try {	
			try {
				String symbol_str;
				StringBuilder sb = new StringBuilder(GRAPH_URL);
				if(symbol.charAt(0) == '.'){
					symbol_str = symbol.replace('.', '^');
					symbol_str = URLEncoder.encode(symbol_str);
				} else symbol_str = symbol;
				sb = sb.append(symbol_str);
				Log.d(TAG, sb.toString());
				HttpClient req = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(sb.toString());
				HttpResponse response = req.execute(httpGet);
				InputStream iStream;
				BitmapDrawable bitMap;
				iStream = response.getEntity().getContent();
				bitMap = new BitmapDrawable(iStream);
				iStream.close();
				iStream = null;
				return bitMap.getBitmap();
			} catch ( IOException iox ){
				Log.d(TAG, iox.getMessage());	
			}
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		return null;
	}

	public synchronized void refreshStocks(){
		long startTime = System.currentTimeMillis(), endTime;
		quoteResult = __getQuotesForArray(stocks);
		try {
			if(quoteResult != null && ((String)quoteResult.get(STATUS)).equals(OK)){
				quotes = quoteResult.getJSONArray(QUOTES);
			}
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		}
		endTime = System.currentTimeMillis();
		Log.d(TAG, "Refresh ran for " + (endTime - startTime) + " millisenconds");
	}

	public synchronized int stocksInPortfolio(){
		if(stocks != null)
			return stocks.size();
		return 0;
	}

	public synchronized int quoteCount(){
		if(quotes != null)
			return quotes.length();
		return 0;
	}
	
	public synchronized void removeQuoteByIndex(int index){
		stocks.remove(index);
		savePortfolio();
	}
	
	public synchronized JSONObject getQuoteForIndex(int index) throws JSONException{
		return quotes.getJSONObject(index);
	}

}
