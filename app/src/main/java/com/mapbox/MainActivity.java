package com.mapbox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;

import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionListener,
MapboxMap.OnMapClickListener{

    private MapView mapView;
    private MapboxMap map;
    private Button startButton;
    private PermissionManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationLayerPlugin locationLayerPlugin;
    private Location originLocation;
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private NavigationMapRoute navigationMapRoute;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        startButton = findViewById(R.id.startButton);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this); //callback

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .origin(originPosition)
                        .destination(destinationPosition)
                        .shouldSimulateRoute(true)
                        .build();
                NavigationLauncher.startNavigation(MainActivity.this, options);
            }
        });
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap){
        map = mapboxMap;
        map.addOnMapClickListener(this);
        enableLocation();
    }

    private void enableLocation(){
        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();
        } else{
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationEngine(){
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null){
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else{
            locationEngine.addLocationEngineListener(this);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void initializeLocationLayer(){
        locationLayerPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);
    }

    private void setCameraPosition (Location location){
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getLatitude(),
                location.getLongitude()), zoom:13.0));
    }

    @Override
    public void onMapClick(@NonNull LatLng point){

        if(destinationMarker != null) {
            map.removeMarker(destinationMarker);
        }
        destinationMarker = map.addMarker(new MarkerOptions().position(point));

        destinationPosition = Point.fromLngLat(point.Longitude(), point,getLatitude());
        originPosition = Point.fromLngLat(originLocation.getLongitude(), originLocation.getLatitude());
        getRoute(originPosition, destinationPosition);
        startButton.setEnabled(true);
        startButton.setBackgroundResource(R.color.mapboxBlue);
    }

    private void getRoute(Point origin, Point destination){
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>(){
                @Override
                public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                    if(response.body() == null){
                        Log.e(TAG ,"No routes found, check right user and access token");
                        return;
                    }
                    else if(response.body().routes().size() == 0){
                        Log.e(TAG, "No routes found");
                        return;
                    }

                    DirectionsRoute currentRoute = response.body().routes().get(0);

                    if(navigationMapRoute != null) {
                        navigationMapRoute.removeRoute();
                    }
                    else {
                        navigationMapRoute = new NavigationMapRoute(null, MapView, map);
                    }
                    navigationMapRoute.addRoute(currentRoute);
                }

                @Override
                public void onFailure(Call<DirectionsResponse> call, Throwable t) {
                    Log.e(TAG,"Error:" + t.getMessage());
                }
            });
}


    @SuppressWarnings("MissingPermission")
    @Override
    public void onConnected(){
        locationEngine.requestLocationUpdate();
    }

    @Override
    public void onLocationChanged(Location location){
        if(location != null){
            originLocation = location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionToExplain){
        //present toast for when the user denies access
    }

    @Override
    public void onPersmissionResult(boolean granted){
        if(granted){
            enableLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionResult(requestCode, permissions, grantResults);
    }

    @SuppressWarnings("MissingPermission")
    @Override
    protected void onStart(){
        super.onStart();
        if (locationEngine != null){
            locationEngine.requestLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(locationEngine != null){
            locationEngine.removeLocationUpdates();
        }
        if(locationLayerPlugin != null){
            locationLayerPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(locationLayerPlugin != null){
            locationEngine.deactivate();
        }
        mapView.onDestroy();
    }




}