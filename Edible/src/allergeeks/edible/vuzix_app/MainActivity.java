package allergeeks.edible.vuzix_app;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vuzix.speech.VoiceControl;

public class MainActivity extends Activity {
	
	ImageView view;
	private Activity main = this;
	private TextView infotext;
	private VoiceControl vc;
	Toast toast;
	Token token; 
	private String page = "http://edible.ddns.net";
	boolean created;
	String testexecute;
	Button scan;
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		view = (ImageView)findViewById(R.id.imageView1);
		scan = (Button)findViewById(R.id.scan);
		token = new Token();
		infotext = (TextView)findViewById(R.id.infotext);
		view.setImageResource(R.drawable.background);
		created = token.fileExistance("token.txt", main);
		Typeface type = Typeface.createFromAsset(getAssets(),"fonts/Quicksand-Regular.ttf");
		infotext.setTypeface(type);
		scan.setTypeface(type);
		infotext.setTextColor(Color.BLACK);
		infotext.setText(R.string.welcome);
		scan.setOnClickListener(new OnClickListener() {
		
			
			
			@Override
			public void onClick(View v) {
				
				// TODO Auto-generated method stub
				if(!created){
					toast = Toast.makeText(main, "Bitte koppeln Sie jetzt Ihr Ger�t", Toast.LENGTH_LONG);
					toast.show();
				}
				IntentIntegrator scanIntegrator = new IntentIntegrator(main);
				scanIntegrator.initiateScan();
				
			}
		});
//###########################Voice#######################################################################  
		vc = new myVoiceControl(this){
		
			
			protected void onRecognition(String arg0) {


				
				if(arg0.equals("select")){
					if(!created){
						toast = Toast.makeText(main, "Bitte koppeln Sie jetzt Ihr Ger�t", Toast.LENGTH_LONG);
						toast.show();
					}
					IntentIntegrator scanIntegrator = new IntentIntegrator(main);
					scanIntegrator.initiateScan();
				
				}


			}
		};
//##########################/Voice#######################################################################

	}
	
//Receiving the ScanResult and check the Token
//If the Token is available on the Vuzix, we will make a get-request with the params authToken ( Token that is saved on the Vuzix), and the scanned Code as ean
//else we will try to get a Token with a Post-request as param we use the scanned code	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//retrieve scan result
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		String ean = scanningResult.getContents();
		view.setImageResource(R.drawable.background);
		infotext.setText(R.string.wait);
		String authToken ="";
		if(created){ //go in if a token file exists
			try {				
				authToken = token.readToken(main);
				String url = page +"/api/v1/product/"+ean+"/"+authToken+"/";
				new GetProduct().execute(url);
			
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{//authToken not exists
			
			new PostSession().execute(ean);
		}
	}
	
		
	protected void onResume(){
		super.onResume();
		vc.on();
	}

	
	protected void onPause(){
		super.onDestroy();
		vc.off();
	}

	protected void onDestroy(){
		super.onDestroy();
		if(vc != null){
			vc.destroy();
		}
	}	
	
//getProduct class to receive the edible status from an user
//DoInBackground will make the Request onPostExecute we handle the response	
	public class GetProduct extends AsyncTask<String, Void, String>{
			
			@Override
			protected String doInBackground(String... params) {
				// TODO Auto-generated method stub
				
				try {
					//Get Edible Status
					//here we are making the request to the website and check the status code, witch we receive
					//if status code = 200 everything is ok so we can parse the received JSONObject 
					//else we are checking the codes if it is = 401 then we know that the vuzix was unpaired from the website and we can delete the Token
					//if the code is 404 than the scanned product isn't available in our Datastorage
					HttpClient client = new DefaultHttpClient();
					HttpGet get = new HttpGet(params[0]);
					HttpResponse response = client.execute(get);
					int status = response.getStatusLine().getStatusCode();
					
					//Check if server is connected 
					if(status == 200){
											
						HttpEntity entity = response.getEntity();
						String data = EntityUtils.toString(entity);
						
						JSONObject jObj = new JSONObject(data);
						String name = jObj.getString("edible");
						
						if(name.equals("true")){ 
							name = "edible";
						}else{
							name = "not edible";
						}
						
					return name;	
					}else if(status == 401){//token invalide
						token.delete(main);
						created = token.fileExistance("token.txt", main);
						String result = "tokenerror";
						return result;
					}else if(status == 404){//product not available
						String result = "not available";
						return result;						
					}
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return "false";
			}
			@Override
			protected void onPostExecute(String result) {
				// TODO Auto-generated method stub
				//Just checking the return value from the doInBackground and set the Backgroundimage from the application and/or show a Toast 
				super.onPostExecute(result);
				if (result.equals("edible")){	
					infotext.setText("");
					view.setImageResource(R.drawable.heart);
				}else if (result.equals("not edible")){
					infotext.setText("");
					view.setImageResource(R.drawable.brokenheart);
				}else if (result.equals("tokenerror")){
					toast = Toast.makeText(main, "Ihr Vuzix Ger�t ist nicht mit einem Account verbunden", Toast.LENGTH_LONG);
					toast.show();
					infotext.setText("");
					view.setImageResource(R.drawable.error);
				}else if (result.equals("not available")){
					toast = Toast.makeText(main, "Das gescannte Produkt ist uns leider nicht bekannt", Toast.LENGTH_LONG);
					toast.show();
					infotext.setText("");
					view.setImageResource(R.drawable.error);
				}else{
					infotext.setText("");
					toast = Toast.makeText(main, "Es ist ein Fehler aufgetreten, bitte versuchen Sie es sp�ter erneut", Toast.LENGTH_LONG);
					toast.show();
					view.setImageResource(R.drawable.error);
				}
				
			}
		}

	public class PostSession extends AsyncTask<String, Void, Boolean>{

		@Override
		protected Boolean doInBackground(String... params) {
			String url = page+"/api/v1/session/";
			
			//almost the same code like GetProcduct in this class we just added a List with NameValuePiar to set the params for the post-request
			//everything else is just straight forward checking statuscode and parsing the response save the result in the Tokenfile and finish
			try{
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost request = new HttpPost(url);
				List<NameValuePair> postParams = new ArrayList<NameValuePair>();
				postParams.add(new BasicNameValuePair("barcode", params[0]));
				
				UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(postParams);
				
				request.setEntity(formEntity);
				HttpResponse response = httpclient.execute(request);
				int status = response.getStatusLine().getStatusCode();
				
				switch(status){
				case 200: 	String data = EntityUtils.toString(response.getEntity());
							JSONObject jObj = new JSONObject(data);
							String authToken = jObj.getString("authToken");
							token.createToken(authToken, main);
							created = token.fileExistance("token.txt", main);
							break;
				default: 	created = false;
							break;
				}
				return created;
			}catch(Exception e){
				
			}
			return created;
		}
		@Override
		protected void onPostExecute(Boolean result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result){
				infotext.setText("");
				toast = Toast.makeText(main, "Kopplung war erfolgreich", Toast.LENGTH_LONG);
				toast.show();
				view.setImageResource(R.drawable.heart);//Token saved
			}else{
				infotext.setText("");
				toast = Toast.makeText(main, "Kopplung war NICHT erfolgreich", Toast.LENGTH_LONG);
				toast.show();
				view.setImageResource(R.drawable.brokenheart);//Token not saved
			}
			
			
			
		}
	}


}




