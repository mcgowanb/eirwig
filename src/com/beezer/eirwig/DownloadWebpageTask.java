package com.beezer.eirwig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import android.util.Log;

public class DownloadWebpageTask extends AsyncTask<String, Void, String> {
	
	 @Override
     protected void onPreExecute() {
         super.onPreExecute();
         // Showing progress dialog
        
     }
	
    @Override
    protected String doInBackground(String... urls) {
    	 try {
             return downloadUrl(urls[0]);
         } catch (IOException e) {
             return "Unable to retrieve web page. URL may be invalid.";
         }
    	
    } 
    	
    // onPostExecute displays the results of the AsyncTask.
    @Override
    protected void onPostExecute(String result) {
        //ParseJson parser = new ParseJson();
    	//parser.dataCollector = result;
   }
    	
    
    private String downloadUrl(String myurl) throws IOException {
        InputStream inStream = null;
            
        try {
            URL url = new URL(myurl);
            StringBuilder builder = new StringBuilder();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.i("DEBUG_TAG", "The response is: " + response);
            
            inStream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inStream));
            String line;
	        while ((line = reader.readLine()) != null) {
	          builder.append(line);
	          Log.i("Web Output",line);
	        }

            return builder.toString();
           
        } finally {
            if (inStream != null) {
                inStream.close();
            } 
        }
    }
}