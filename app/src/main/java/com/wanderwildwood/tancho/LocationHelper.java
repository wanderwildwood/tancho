package com.wanderwildwood.tancho;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

public class LocationHelper {
    private static Location oldLocation;
    private static long oldLocationTime = 0;
    private static Location preciseLocation;
    private static LocationListener locationListenerGPS;
    static {
        preciseLocation = new Location("GPS");
        preciseLocation.setLatitude(0.0f);
        preciseLocation.setLongitude(0.0f);
    }

    static void stopLocation(Context context){
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationListenerGPS!=null) locationManager.removeUpdates(locationListenerGPS);
        locationListenerGPS=null;
    }

    /**
     * Whether the app knows where it is.
     *
     * False until a fix arrives, and false for a manual location left at its 0/0 default:
     * that pair passes the format check and is a real point on the map, in the Gulf of
     * Guinea, and the meta model will happily tell you what sings there. Nobody who turned
     * the switch on without typing anything meant to ask about the equator.
     */
    static boolean hasLocation(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("manual_location", false)) {
            return !isNowhere(manualLocation(prefs));
        }
        return oldLocation != null;
    }

    private static Location manualLocation(SharedPreferences prefs) {
        String value = prefs.getString("manual_location_value", "0.000/0.000");
        String[] parts = value.split("/");
        Location location = new Location("GPS");
        try {
            location.setLatitude(Double.parseDouble(parts[0]));
            location.setLongitude(Double.parseDouble(parts[1]));
        } catch (Exception e) {
            location.setLatitude(0.0);
            location.setLongitude(0.0);
        }
        return location;
    }

    /** 0/0 is not a place anyone birds. It is what the app holds when it has been told nothing. */
    private static boolean isNowhere(Location location) {
        return location.getLatitude() == 0.0 && location.getLongitude() == 0.0;
    }

    static void requestLocation(Context context, SoundClassifier soundClassifier) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getBoolean("manual_location", false)){
            preciseLocation = manualLocation(sharedPreferences);
            // A manual location still sitting at its default is no location. Running the
            // meta model on it would weigh every answer towards the Gulf of Guinea while
            // the screen said "Expected first", which is a confident answer to a question
            // the app cannot answer.
            if (isNowhere(preciseLocation)) {
                oldLocation = null;
                oldLocationTime = 0;
                soundClassifier.noteLocationKnown(false);
                return;
            }
            oldLocation = preciseLocation;
            soundClassifier.runMetaInterpreter(oldLocation);
            soundClassifier.noteLocationKnown(true);
            oldLocationTime = 0;
            return;
        }

        if (System.currentTimeMillis() - oldLocationTime > 3 * 60 * 1000) {oldLocation = null; oldLocationTime = 0;}  //location older than 3 min -> reset
        else soundClassifier.runMetaInterpreter(oldLocation);
        soundClassifier.noteLocationKnown(oldLocation != null);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && checkLocationProvider(context)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationListenerGPS==null) locationListenerGPS = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    preciseLocation = location;
                    Location roundLoc = new Location(location);
                    roundLoc.setLatitude(Math.round(location.getLatitude() * 100.0) / 100.0);
                    roundLoc.setLongitude(Math.round(location.getLongitude() * 100.0) / 100.0);
                    if (oldLocation == null ||
                            (roundLoc.getLatitude() != oldLocation.getLatitude()) ||
                            (roundLoc.getLongitude() != oldLocation.getLongitude())){

                        oldLocation = roundLoc;
                        oldLocationTime = System.currentTimeMillis();
                        soundClassifier.runMetaInterpreter(roundLoc);
                        soundClassifier.noteLocationKnown(true);
                    }
                }

                @Deprecated
                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            };
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListenerGPS);
        }
    }

    public static boolean checkLocationProvider(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(context, "Error no GPS", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    public static Location getPreciseLocation(){
        return preciseLocation;
    }
}