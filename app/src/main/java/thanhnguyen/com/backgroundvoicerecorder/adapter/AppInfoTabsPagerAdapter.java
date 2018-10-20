package thanhnguyen.com.backgroundvoicerecorder.adapter;


import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import thanhnguyen.com.backgroundvoicerecorder.fragment.GalleryFragment;
import thanhnguyen.com.backgroundvoicerecorder.fragment.SettingViewFragmnent;

public class AppInfoTabsPagerAdapter extends FragmentPagerAdapter {

	private String[] titles = {
			"Settings",
			"Audio"
	};


	Context context;
	

	public AppInfoTabsPagerAdapter(FragmentManager fm, Context context) {
		super(fm);
		this.context=context;


	}

	@Override
	public Fragment getItem(int position) {

		switch (position) {
        
		case 0:
            return new SettingViewFragmnent();
            
         
        case 1:
            return new GalleryFragment();
        	 
	}


		return null;
	}

	@Override
	public int getCount() {

		return 2;
	}
	@Override
	public CharSequence getPageTitle(int position) {
		return titles[position];
	}



}
