package thanhnguyen.com.backgroundvoicerecorder.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;

import thanhnguyen.com.backgroundvoicerecorder.R;

public class GalleryItem extends CursorAdapter {
	
	 Context context;
     String fileuploadname;
	 String audiopath;
	 String id;
	 String duration;
	 String date;
	 String latitude;
	 String longtitude;

	public GalleryItem(Context context, Cursor c) {
		super(context, c);
		this.context = context;


	}
	@Override
	public int getCount() {

	return this.getCursor().getCount();     }

	@Override
	public void bindView(View view, final Context context, Cursor cursor) {
		final MyViewHolder viewHolder = (MyViewHolder) view.getTag();
	    final File file = new File(audiopath);
	    if(file.exists()){


			if (viewHolder.durationtextv != null) {
				viewHolder.durationtextv.setText(duration);
			}


			if (viewHolder.datetextv != null) {
				viewHolder.datetextv.setText(date);
			}
	    	
		}  else {


				view.setVisibility(View.GONE);

				return;



	    }


	    
	}


@Override
public View newView(Context context, Cursor cursor, ViewGroup parent) {
	 audiopath = cursor.getString(cursor.getColumnIndex("audiopath"));
	 id = cursor.getString(cursor.getColumnIndex("_id"));
	 duration = cursor.getString(cursor.getColumnIndex("duration"));
	 date = cursor.getString(cursor.getColumnIndex("date"));
	 latitude = cursor.getString(cursor.getColumnIndex("latitude"));
	 longtitude = cursor.getString(cursor.getColumnIndex("longtitude"));
	LayoutInflater inflater= (LayoutInflater)context.getSystemService (Context.LAYOUT_INFLATER_SERVICE);
	View convertView = inflater.inflate(R.layout.galleryitemlayout, parent, false);
	MyViewHolder viewHolder = new MyViewHolder(convertView);
	convertView.setTag(viewHolder);
    return convertView;
}

	public class MyViewHolder  {
		public TextView durationtextv;
		public TextView datetextv;
		public SimpleDraweeView imagev;

		public MyViewHolder(View view) {
			imagev =
					(SimpleDraweeView) view.findViewById(R.id.thumbnail);
			durationtextv =
					(TextView) view.findViewById(R.id.duration);
			datetextv =
					(TextView) view.findViewById(R.id.date);

		}
	}





}
