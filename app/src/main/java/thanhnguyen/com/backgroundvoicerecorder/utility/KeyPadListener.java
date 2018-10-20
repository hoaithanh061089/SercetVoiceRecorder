package thanhnguyen.com.backgroundvoicerecorder.utility;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import java.util.List;

import thanhnguyen.com.backgroundvoicerecorder.StartupActivity;
import thanhnguyen.com.backgroundvoicerecorder.cameraservice.AudioBackgroundService;

public class KeyPadListener extends BroadcastReceiver{

	Context context;
	Intent shortcutIntent;

@Override
public void onReceive(final Context context, Intent intent) {
	
	this.context = context;
	 if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
		 
		 SharedPreferences shared = PreferenceManager.getDefaultSharedPreferences(context);	 
		 String number = getResultData();   
	     if (number!=null) {

	    	 
	           if(shared.getBoolean("hide_icon", false)){
	        	   
	        	   
	        	   String savednumber = shared.getString("app_opendialnumber", null);
	        	   
	        	   if(savednumber!=null){
	        		   
	        		   if(number.equals(savednumber)){


	        		   	 setResultData(null);
						   PackageManager p =  context.getPackageManager();
						   p.setComponentEnabledSetting(new ComponentName(context, thanhnguyen.com.backgroundvoicerecorder.StartupActivity.class),
								   PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
								   PackageManager.DONT_KILL_APP);
	      	            
	      	             Intent newintent = new Intent(context ,StartupActivity.class);
	      	             newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	      	             context.startActivity(newintent);

	        			   
	        		   }
	        		   
	        		   
	        	     } else {
	        		   
	        		   if(number.equals("*#6789#*")){

						   setResultData(null);
						   PackageManager p =  context.getPackageManager();
						   p.setComponentEnabledSetting(new ComponentName(context, thanhnguyen.com.backgroundvoicerecorder.StartupActivity.class),
								   PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
								   PackageManager.DONT_KILL_APP);

						   Intent newintent = new Intent(context ,StartupActivity.class);
						   newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						   context.startActivity(newintent);

	        		   }
	        		   

	           }

	           } else if(shared.getBoolean("instant_recording", false)){

				 String savednumber = shared.getString("videodialnumber", null);

				   if(savednumber!=null){

					   if(number.equals(savednumber)){

						   setResultData(null);
						   if(isMyServiceRunning(AudioBackgroundService.class)){
							   shortcutIntent = new Intent(context,
									   AudioBackgroundService.class);

							   Handler handler = new Handler(Looper.getMainLooper());
							   handler.postDelayed(new Runnable(){

								   @Override
								   public void run() {

									   context.stopService(shortcutIntent);

								   }

							   }, 3000);


						   } else {

							   List<String> listPermissionsNeeded = CheckPermission.checkCameraRelatedPermission(context);

							   if(listPermissionsNeeded.isEmpty()){


								   Intent shortcutIntent1 = new Intent(context,
										   AudioBackgroundService.class).putExtra("servicetrigger", "keypad");
								   context.startService(shortcutIntent1);



							   } else {

								   CheckPermission.sendNotificationVideoRecording(context, "Permission needed to start recording",
										   listPermissionsNeeded.toString());

							   }




						   }




					   }



					   } else {


					   if(number.equals("*#0610#*")){

						   setResultData(null);
						   if(isMyServiceRunning(AudioBackgroundService.class)){
							   shortcutIntent = new Intent(context,
									   AudioBackgroundService.class);

							   Handler handler = new Handler(Looper.getMainLooper());
							   handler.postDelayed(new Runnable(){

								   @Override
								   public void run() {

									   context.stopService(shortcutIntent);

								   }

							   }, 3000);


						   } else {

							   List<String> listPermissionsNeeded = CheckPermission.checkCameraRelatedPermission(context);

							   if(listPermissionsNeeded.isEmpty()){


								   Intent shortcutIntent1 = new Intent(context,
										   AudioBackgroundService.class).putExtra("servicetrigger", "keypad");
								   context.startService(shortcutIntent1);




							   } else {

								   CheckPermission.sendNotificationVideoRecording(context, "Permission needed to start recording",
										   listPermissionsNeeded.toString());

							   }




						   }



					   }



				   }


			 }


	      }
	
	       }
		}

	private boolean isMyServiceRunning(Class<?> serviceClass) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}



}
