package ar.com.tristeslostrestigres.diasporanativewebapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class PrefManager {

//	private boolean welcome = true;
	private boolean loadImages = true;
    private final Context context;
	
	public PrefManager(Context ctx) {
		SharedPreferences sp = null;
		this.context = ctx;
		try {
			sp = PreferenceManager.getDefaultSharedPreferences(context);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (sp != null) {
//			welcome = sp.getBoolean("welcome", true);
			loadImages = sp.getBoolean("loadImages", true);
		}
	}
	

//	public void setWelcome(Boolean valor) {
////		welcome = valor;
//		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
//        Editor edit = sp.edit();
//        edit.putBoolean("welcome", valor);
//        edit.apply();
//	}
//
//	public boolean getWelcome() {
//		return welcome;
//	}




	public boolean getLoadImages() {
		return loadImages;
	}


	public void setLoadImages(boolean loadImages) {
		this.loadImages = loadImages;
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = sp.edit();
        edit.putBoolean("loadImages", loadImages);
        edit.commit();
	}


}
