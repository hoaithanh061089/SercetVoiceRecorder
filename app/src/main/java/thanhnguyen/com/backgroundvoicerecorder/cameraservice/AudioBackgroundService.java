package thanhnguyen.com.backgroundvoicerecorder.cameraservice;


import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import thanhnguyen.com.backgroundvoicerecorder.ApplicationAppClass;
import thanhnguyen.com.backgroundvoicerecorder.R;
import thanhnguyen.com.backgroundvoicerecorder.StartupActivity;
import thanhnguyen.com.backgroundvoicerecorder.database.AudioDatabaseOpenHelper;

public class AudioBackgroundService extends Service implements
		LocationListener,
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener {

	FrameLayout preview;
	MediaRecorder mMediaRecorder;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_AUDIO = 2;

	boolean isRecording = false;
	Runnable r;
	SharedPreferences sharedpre;
	Parameters cameraParams;
	boolean isaudiorunning = false;
	static File mediaStorageDir;
	File savingfile;
	private static final String SCREEN_LABEL = "Real time camera";
	private static final String TABLE_NAME = "hiddenaudiodatabase";
	private static final String FIELD_ID = "_id";
	private static final String FIELD_DATE = "date";
	private static final String FIELD_DURATION = "duration";
	private static final String FIELD_AUDIOPATH = "audiopath";
	private static final String FIELD_LONGTITUDE = "longtitude";
	private static final String FIELD_LATITUDE = "latitude";
	String audiopath;
	static String timeStamp;
	Handler handler;
	SharedPreferences pre;
	String servicetrigger;

	////// LOCATION

	LocationRequest mLocationRequest;
	GoogleApiClient mGoogleApiClient;
	Location mCurrentLocation;
	public static int UPDATE_INTERVAL = 30000;
	public static int FATEST_INTERVAL = 30000;


	@Override
	public void onDestroy() {
		super.onDestroy();

		//remove location update

		if(mGoogleApiClient!=null){

			if(mGoogleApiClient.isConnected()){
				LocationServices.FusedLocationApi.removeLocationUpdates(
						mGoogleApiClient, (LocationListener) this);
				mGoogleApiClient.disconnect();

			}
		}

		handler.removeCallbacks(r);
		try {
			releaseMediaRecorder();

		} catch (Exception e) {

			sendNotificationVideoRecording("Camera stop failed", "");


		}

		if (isaudiorunning) {
			if (pre.getBoolean("camera_vibrate", true)) {

				Vibrator v = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);
				// Vibrate for 500 milliseconds
				v.vibrate(300);

			}
			AudioDatabaseOpenHelper contactdatabase = new AudioDatabaseOpenHelper(AudioBackgroundService.this);
			SQLiteDatabase db = contactdatabase.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put(FIELD_AUDIOPATH, savingfile.getAbsolutePath());
			values.put(FIELD_DATE, java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime()));
			MediaPlayer mplayer = MediaPlayer.create(AudioBackgroundService.this, Uri.fromFile(new File(savingfile.getAbsolutePath())));
			if (mplayer != null) {
				int msec = mplayer.getDuration();
				String duration = String.format("%d min : %d sec",
						TimeUnit.MILLISECONDS.toMinutes(msec),
						TimeUnit.MILLISECONDS.toSeconds(msec) -
								TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(msec)));
				values.put(FIELD_DURATION, duration);


				//save location data to file

				if(mCurrentLocation!=null){
					values.put(FIELD_LONGTITUDE, String.valueOf(mCurrentLocation.getLongitude()));
					values.put(FIELD_LATITUDE, String.valueOf(mCurrentLocation.getLatitude()));

				}

				long result = db.insert(TABLE_NAME, null, values);
				removeNotificationVideoRecording();
				Tracker tracker = ApplicationAppClass.getDefaultTracker(getBaseContext());
				tracker.send(new HitBuilders.EventBuilder()
						.setCategory("AudioService")
						.setAction("Voice_Recording_Stopped")
						.build());

			}


		}

		if (pre.getBoolean("google_upload_allow", false)) {

			if (pre.getBoolean("google_upload_wifi", false)) {

				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

				if (mWifi.isConnected()) {


					uploadfiletodrive(savingfile);


				}


			} else {

				   uploadfiletodrive(savingfile);

			}


		}


	}



	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if(intent.getExtras()==null){

			servicetrigger = "startvideobutton";

		} else {

			servicetrigger = intent.getExtras().getString("servicetrigger");

		}


		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		//set up location
		locationSetup();
		sharedpre = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		pre = PreferenceManager.getDefaultSharedPreferences(this);
		handler = new Handler();
		r = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				if (prepareAudioRecorder()) {


					try {
						mMediaRecorder.start();

						Tracker tracker = ApplicationAppClass.getDefaultTracker(getBaseContext());
						tracker.send(new HitBuilders.EventBuilder()
								.setCategory("AudioService")
								.setAction("Voice_Recording_Started")
								.build());

						if (pre.getBoolean("app_notification", true)) {

							if (servicetrigger.equals("shortcut")) {

								sendNotificationVideoRecording("New audio is being recorded.", "Tap shortcut icon again to stop recording.");

							} else if (servicetrigger.equals("keypad")) {

								sendNotificationVideoRecording("New audio is being recorded.", "Dial your number again to stop recording.");


							} else if (servicetrigger.equals("servicemaxduration")) {

								sendNotificationVideoRecording("New audio is being recorded.","Maximum file duration, audio recording restarts.");


							}  else if (servicetrigger.equals("startvideobutton")) {

								sendNotificationVideoRecording("New audio is being recorded.", "");


							}

						}


					} catch (Exception e) {

						sendNotificationVideoRecording("Unexpected error initializing camera, video recording failed!", "");

						releaseMediaRecorder();
						removeNotificationVideoRecording();

						stopSelf();


					}


					isaudiorunning = true;
					if (pre.getBoolean("camera_vibrate", true)) {

						Vibrator v = (Vibrator) getBaseContext().getSystemService(Context.VIBRATOR_SERVICE);
						v.vibrate(300);

					}



					if (mMediaRecorder != null) {

						mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

							@Override
							public void onInfo(MediaRecorder mr, int what, int extra) {

								if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

									Toast.makeText(getBaseContext(), "Max file!", Toast.LENGTH_SHORT).show();


									AudioBackgroundService.this.stopSelf();



								} else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {

									AudioBackgroundService.this.stopSelf();

									AudioBackgroundService.this.startService(new Intent(AudioBackgroundService.this, AudioBackgroundService.class).putExtra("servicetrigger", "servicemaxduration"));



								}


							}

						});
					}


				}

			}


		};

		handler.postDelayed(r, 2000);


	}



	private boolean prepareAudioRecorder() {

		mMediaRecorder = new MediaRecorder();
		mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
			mMediaRecorder.setAudioEncodingBitRate(48000);
		} else {
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
			mMediaRecorder.setAudioEncodingBitRate(64000);
		}
		mMediaRecorder.setAudioSamplingRate(16000);

		audiopath = sharedpre.getString("video_path", null);
		if (audiopath == null) {

			savingfile = getOutputMediaFile(MEDIA_TYPE_AUDIO);
			audiopath = mediaStorageDir.getPath();

		} else {

			File mediaStorageDir = new File(audiopath);
			if (!mediaStorageDir.exists()) {
				savingfile = getOutputMediaFile(MEDIA_TYPE_AUDIO);

			} else {

				String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
				savingfile = new File(audiopath + File.separator +
						timeStamp + ".m4a");


			}


		}


		// Step 4: Set output file
		mMediaRecorder.setOutputFile(savingfile.getAbsolutePath());

		String recordingduration_string = sharedpre.getString("camera_duration", "3");
		int recordingduration = Integer.parseInt(recordingduration_string);

		    if(recordingduration==0){

				mMediaRecorder.setMaxDuration(60*1000);

			} else if(recordingduration==1){
				mMediaRecorder.setMaxDuration(120*1000);

			} else if(recordingduration==2){
				mMediaRecorder.setMaxDuration(300*1000);

			} else if(recordingduration==3){
				mMediaRecorder.setMaxDuration(900*1000);

			} else if(recordingduration==4){
				mMediaRecorder.setMaxDuration(3600*1000);

			}else if(recordingduration==5){
				mMediaRecorder.setMaxDuration(13600*1000);

			}



		// Step 6: Prepare configured MediaRecorder
		try {
			mMediaRecorder.prepare();

		} catch (IllegalStateException e) {
			releaseMediaRecorder();

			return false;
		} catch (IOException e) {
			releaseMediaRecorder();

			return false;
		}
		return true;
	}

	private void releaseMediaRecorder() {
		if (mMediaRecorder != null) {
			if (isaudiorunning) {
				mMediaRecorder.stop();
			}

			mMediaRecorder.reset();   // clear recorder configuration
			mMediaRecorder.release(); // release the recorder object
			mMediaRecorder = null;

		}
	}

	private static File getOutputMediaFile(int type) {
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		mediaStorageDir = new File(Environment.getExternalStorageDirectory(
		), "Secret Audio Recorder");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {

				return null;
			}
		}

		// Create a media file name
		timeStamp = new SimpleDateFormat("ddMMyyyy_HHmmss").format(new Date());
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_" + timeStamp + ".jpg");
		} else if (type == MEDIA_TYPE_AUDIO) {
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					timeStamp + ".m4a");
		} else {
			return null;
		}

		return mediaFile;
	}


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void onConnected(@Nullable Bundle bundle) {

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {

			return;
		}
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, (LocationListener) this);

	}

	@Override
	public void onConnectionSuspended(int i) {

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

	}

	@Override
	public void onLocationChanged(Location location) {

		mCurrentLocation = location;


	}




	private void sendNotificationVideoRecording(String title, String messageBody) {
		Intent intent = new Intent(this, StartupActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
				PendingIntent.FLAG_ONE_SHOT);

		String channelId = getString(R.string.app_name);
		Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		NotificationCompat.Builder notificationBuilder =
				new NotificationCompat.Builder(this, channelId)
						.setSmallIcon(R.drawable.eye)
						.setContentTitle(title)
						.setContentText(messageBody)
						.setAutoCancel(true)
						.setSound(defaultSoundUri)
						.setContentIntent(pendingIntent);

		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(01
                /* ID of notification */, notificationBuilder.build());
	}
	private void removeNotificationVideoRecording() {

		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.cancel(1);
	}
	private boolean isGooglePlayServicesAvailable() {
		GoogleApiAvailability api = GoogleApiAvailability.getInstance();
		int isAvailable = api.isGooglePlayServicesAvailable(this);


		if (isAvailable == ConnectionResult.SUCCESS) {

			return true;
		}
		return false;
	}
	public void locationSetup(){

		if (isGooglePlayServicesAvailable()) {
			mLocationRequest = new LocationRequest();
			mLocationRequest.setInterval(UPDATE_INTERVAL);
			mLocationRequest.setFastestInterval(FATEST_INTERVAL);
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			mLocationRequest.setSmallestDisplacement(50);
			//mLocationRequest.setSmallestDisplacement(10.0f);  /* min dist for location change, here it is 10 meter */
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addApi(LocationServices.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();

			mGoogleApiClient.connect();
		}



	};


	public void uploadfiletodrive(final File file){

		GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getBaseContext());

		if(acct==null){

			if (pre.getBoolean("app_notification", true)) {

				sendNotificationVideoRecording("Upload file to Google Drive failed, sign in your google account first.", "");

			}


		} else {


			final DriveResourceClient mDriveResourceClient = Drive.getDriveResourceClient(getBaseContext(), acct);
			final Task<DriveFolder> rootFolderTask = mDriveResourceClient.getRootFolder();
			final Task<DriveContents> createContentsTask = mDriveResourceClient.createContents();


			Tasks.whenAll(rootFolderTask, createContentsTask)
					.continueWithTask(new Continuation<Void, Task<DriveFile>>() {
						@Override
						public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
							DriveFolder parent = rootFolderTask.getResult();
							DriveContents contents = createContentsTask.getResult();
							OutputStream outputStream = contents.getOutputStream();

							FileInputStream inputstream = new FileInputStream(file.getPath());
							BufferedInputStream in = new BufferedInputStream(inputstream);
							byte[] buffer = new byte[8 * 1024];

							BufferedOutputStream out = new BufferedOutputStream(outputStream);
							int n = 0;
							try {
								while( ( n = in.read(buffer) ) > 0 ) {
									out.write(buffer, 0, n);
									out.flush();
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}


							try {
								in.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}


							MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
									.setTitle(file.getName())
									.setMimeType("audio/m4a")
									.setStarred(true)
									.build();

							return mDriveResourceClient.createFile(parent, changeSet, contents);
						}
					}).addOnSuccessListener(new OnSuccessListener<DriveFile>() {
						@Override
						public void onSuccess(DriveFile driveFile) {

							if (pre.getBoolean("app_notification", true)) {

								sendNotificationVideoRecording("Upload file to Google Drive successfully", "");

							}

						}
					}).addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception e) {

							if (pre.getBoolean("app_notification", true)) {

								sendNotificationVideoRecording("Upload file to Google Drive failed", "");

							}

						}
					});







		}





	}





}


