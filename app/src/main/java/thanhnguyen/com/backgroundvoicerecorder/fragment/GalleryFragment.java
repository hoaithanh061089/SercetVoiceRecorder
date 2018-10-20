package thanhnguyen.com.backgroundvoicerecorder.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.valdesekamdem.library.mdtoast.MDToast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import thanhnguyen.com.backgroundvoicerecorder.ApplicationAppClass;
import thanhnguyen.com.backgroundvoicerecorder.R;
import thanhnguyen.com.backgroundvoicerecorder.adapter.GalleryItem;
import thanhnguyen.com.backgroundvoicerecorder.database.AudioDatabaseOpenHelper;
import thanhnguyen.com.backgroundvoicerecorder.location.VideoGPSLocation;

public class GalleryFragment extends Fragment{
	
	public static GridView gridview;
	AudioDatabaseOpenHelper database ;
	SQLiteQueryBuilder queryBuilder;
	SwipeRefreshLayout mySwipeRefreshLayout;
	GalleryItem galleryitem;
	Cursor datacursor;
	private static final String TABLE_NAME = "hiddenaudiodatabase";
	private static final String FIELD_ID = "_id";
	MaterialDialog login_dialog;


	@Override
	public void onResume() {
		super.onResume();
		datacursor = queryBuilder.query(database.getReadableDatabase(),
				null ,
				null, null, null, null, null , null);

		galleryitem = new GalleryItem(GalleryFragment.this.getActivity(), datacursor);;
		gridview.setAdapter(galleryitem);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
			Tracker tracker = ApplicationAppClass.getDefaultTracker(getContext());
		    tracker.setScreenName("Gallery Fragment");
		    tracker.send(new HitBuilders.ScreenViewBuilder().build());

		    View rootView = inflater.inflate(R.layout.videogallery, container, false);
		    gridview = (GridView) rootView.findViewById(R.id.gridView1);		    
			database = new AudioDatabaseOpenHelper(GalleryFragment.this.getActivity());
	        queryBuilder = new SQLiteQueryBuilder();    	
	    	queryBuilder.setTables("hiddenaudiodatabase");
		    datacursor = queryBuilder.query(database.getReadableDatabase(),
					null ,
					null, null, null, null, null , null);

		    while (datacursor.moveToNext()) {

			   String imagepath_cursor = datacursor.getString(datacursor.getColumnIndex("audiopath"));
			   String id_cursor = datacursor.getString(datacursor.getColumnIndex("_id"));

			   if(!new File(imagepath_cursor).exists()){

				   database.getWritableDatabase().delete(TABLE_NAME, FIELD_ID + "="+id_cursor,null);
			   }

		        }

		    gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				datacursor = queryBuilder.query(database.getReadableDatabase(),
						null ,
						null, null, null, null, null , null);

				showPopupMenu(datacursor, position);



			}
		});


		    mySwipeRefreshLayout = rootView.findViewById(R.id.swipe_container);
		    mySwipeRefreshLayout.setColorSchemeResources(R.color.swiperefreshcolor);
		    mySwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {

				Cursor cursor = queryBuilder.query(database.getReadableDatabase(),
						null ,
						null, null, null, null, null , null);

				galleryitem = new GalleryItem(GalleryFragment.this.getActivity(),cursor );
				gridview.setAdapter(galleryitem);

				mySwipeRefreshLayout.setRefreshing(false);

			}
		});




			return rootView;
	  }


	  public void showPopupMenu(final Cursor cursor, final int position ){
		  new MaterialDialog.Builder(getActivity())
				  .title("Audio")
				  .items(R.array.vieo_play)
				  .itemsCallback(new MaterialDialog.ListCallback() {
					  @Override
					  public void onSelection(MaterialDialog dialog, View view, int selection, CharSequence text) {
					  	if(selection==0) {

							cursor.moveToPosition(position);
							String audiopath = cursor.getString(cursor.getColumnIndex("audiopath"));
							File file = new File(audiopath);
							if (file.exists()) {

								Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(audiopath));
								intent.setDataAndType(Uri.parse(audiopath), "audio/*");
								if (intent.resolveActivityInfo(getActivity().getPackageManager(), 0) != null)
								{

									startActivity(intent);

								}
								else
								{

									MDToast.makeText(getContext(), "No any audio player app installed on your device. Please open file at " + audiopath

											, MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show();

								}


							}

						}

							 else if(selection==1){

					  		     cursor.moveToPosition(position);

								 new MaterialDialog.Builder(getActivity())
										 .title("Delete audio file")
										 .content("Do you really want to delete this audio?")
										 .positiveText("Agree")
										 .onPositive(new MaterialDialog.SingleButtonCallback() {
											 @Override
											 public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
												 String videopath = cursor.getString(cursor.getColumnIndex("audiopath"));
												 File file = new File(videopath);
												 file.getAbsoluteFile().delete();


												 String id_cursor = cursor.getString(cursor.getColumnIndex("_id"));
												 database.getWritableDatabase().delete(TABLE_NAME, FIELD_ID + "="+id_cursor,null);

												 datacursor = queryBuilder.query(database.getReadableDatabase(),
														 null ,
														 null, null, null, null, null , null);
												 galleryitem = new GalleryItem(GalleryFragment.this.getActivity(), datacursor);;
												 gridview.setAdapter(galleryitem);

											 }
										 })
										 .show();




							 }



						 else if(selection==2){
							cursor.moveToPosition(position);
							String audiopath = cursor.getString(cursor.getColumnIndex("audiopath"));
							Intent intent = new Intent(Intent.ACTION_VIEW);
							intent.setDataAndType(Uri.parse(new File(audiopath).getAbsoluteFile().getParent()), "resource/folder");

							if (intent.resolveActivityInfo(getActivity().getPackageManager(), 0) != null)
							{

								startActivity(intent);

							}
							else
							{
								// if you reach this place, it means there is no any file
								// explorer app installed on your device

								MDToast.makeText(getContext(), "No any file explorer app installed on your device. Please open file at " + audiopath, MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show();

							}


						}

						else if(selection==3){

							cursor.moveToPosition(position);

							String longtitude = cursor.getString(cursor.getColumnIndex("longtitude"));
							String latitude = cursor.getString(cursor.getColumnIndex("latitude"));

							if(longtitude!=null & latitude!=null){

								Bundle bundle = new Bundle();
								bundle.putString("longtitude", longtitude);
								bundle.putString("latitude", latitude);
								startActivity(new Intent(getContext(), VideoGPSLocation.class).putExtras(bundle));


							} else {

								MDToast.makeText(getContext(), "No location for this audio found.", MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show();


							}



						} else if(selection==4){

							cursor.moveToPosition(position);
							String audiopath = cursor.getString(cursor.getColumnIndex("audiopath"));
							final File file = new File(audiopath);

							GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(getActivity());

							if(acct==null){

								MDToast.makeText(getContext(), "Sign in your google account first, please go to Settings - Google Drive upload.!", MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show();



							} else {

								login_dialog = new MaterialDialog.Builder(getActivity())
										.title("Video upload in process")
										.content("Please wait ...")
										.progress(true, 0)
										.show();

								final DriveResourceClient mDriveResourceClient = Drive.getDriveResourceClient(getContext(), acct);
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
                                            })
                                            .addOnSuccessListener(getActivity(),
                                                    new OnSuccessListener<DriveFile>() {
                                                        @Override
                                                        public void onSuccess(DriveFile driveFile) {

															login_dialog.dismiss();

                                                        	MDToast.makeText(getContext(), "Upload file successfully.", MDToast.LENGTH_SHORT, MDToast.TYPE_WARNING).show();


														}
                                                    })
                                            .addOnFailureListener(getActivity(), new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {

													login_dialog.dismiss();

													MDToast.makeText(getContext(), e.toString(), MDToast.LENGTH_LONG, MDToast.TYPE_WARNING).show();


													//MDToast.makeText(getContext(), "Upload file failed.", MDToast.LENGTH_LONG, MDToast.TYPE_WARNING).show();


												}
                                            });





							}


						}


					  }
				  })
				  .show();

	  }


	public void updatevideo() {

		datacursor = queryBuilder.query(database.getReadableDatabase(),
				null ,
				null, null, null, null, null , null);

		galleryitem.changeCursor(datacursor);
		gridview.setAdapter(galleryitem);


	}
}
