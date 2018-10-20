package thanhnguyen.com.backgroundvoicerecorder.database;



import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AudioDatabaseOpenHelper extends SQLiteOpenHelper{
    private static final String DBNAME = "audiolist";
	
	private static final int VERSION = 1;
	private static final String TABLE_NAME = "hiddenaudiodatabase";
	private static final String FIELD_ID = "_id";
	private static final String FIELD_DATE = "date";
	private static final String FIELD_DURATION = "duration";
	private static final String FIELD_AUDIOPATH = "audiopath";
	private static final String FIELD_LONGTITUDE = "longtitude";
	private static final String FIELD_LATITUDE = "latitude";
		
	public AudioDatabaseOpenHelper(Context context)
									 {
		super(context, DBNAME, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "";
		
		// Defining table structure
		sql = "create table IF NOT EXISTS " + "" + TABLE_NAME + "" +
										" ( " +
											FIELD_ID + " integer primary key autoincrement, " + 
											FIELD_DATE + " text , " +
											FIELD_DURATION + " text , " +
									    	FIELD_LONGTITUDE + " text , " + 
											FIELD_LATITUDE + " text , " +
											FIELD_AUDIOPATH + " text " +
											
											
											") " ;
		
		// Creating table
		db.execSQL(sql);			
		
						
		}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {



}
}