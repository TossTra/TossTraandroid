package com.app.tosstra.fragments.driver;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.tosstra.R;
import com.app.tosstra.ViewPagerHome.VPAdapter;
import com.app.tosstra.activities.AppUtil;
import com.app.tosstra.activities.JobOfferForMyJob;
import com.app.tosstra.activities.PagerContainer;
import com.app.tosstra.models.AllJobsToDriver;
import com.app.tosstra.models.GenricModel;
import com.app.tosstra.models.MarkerDetails;
import com.app.tosstra.models.Profile;
import com.app.tosstra.services.Interface;
import com.app.tosstra.utils.CommonUtils;
import com.app.tosstra.utils.CustomInfoViewAdapter;
import com.app.tosstra.utils.MyGps;
import com.app.tosstra.utils.PreferenceHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AllJobsFragment extends Fragment implements OnMapReadyCallback, LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener, View.OnClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener, ClusterItem {

    private MarkerOptions place1, place2;
    LocationManager locationManager;
    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_LOCATION = 1;
    private GoogleMap mMap;
    public static double currentLatitude, currentLongitude;
    private View locationButton;

    VPAdapter vpAdapter;
    ViewPager vp_home;
    PagerContainer mContainer;
    ArrayList<String> lat = new ArrayList<String>();
    ArrayList<String> lon = new ArrayList<String>();
    ArrayList<String> address = new ArrayList<String>();
    ArrayList<String> job_id = new ArrayList<String>();

    private static final String TAG = "MainActivity";
    TextView tvGo, tvGoOffline, tv_offline, tv_offline_online;
    RelativeLayout rl_offline;
    private AllJobsToDriver data;
    private MyGps gpsTracker;

    private String online_status;
    private HashMap<MarkerOptions, Integer> mHashMap = new HashMap<MarkerOptions, Integer>();


    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.all_jobs_fragment, container, false);
        gpsTracker = new MyGps(getContext());
        gpsTracker.getLocation();
        initUI(view);
        hitProfileViewAPI();
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            locationPermission();
        }

        if(PreferenceHandler.readString(getActivity(),"status_onlie","").equalsIgnoreCase("1")){
            tv_offline_online.setVisibility(View.GONE);
            tvGo.setVisibility(View.GONE);
            tv_offline.setVisibility(View.VISIBLE);
            tvGoOffline.setVisibility(View.VISIBLE);
            hitAllJobsToDriverAPI("");
        }else {
            tv_offline_online.setVisibility(View.VISIBLE);
            tvGo.setVisibility(View.VISIBLE);
            tv_offline.setVisibility(View.GONE);
            tvGoOffline.setVisibility(View.GONE);
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mContainer.setVisibility(View.VISIBLE);
                final SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                if (mMap != null) {
                    mMap.clear();
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(final GoogleMap googleMap) {
                        hitAllJobsToDriverAPI("");
                        if (data != null) {
                            if (online_status.equalsIgnoreCase("1")) {
                                setViewPager(data);
                            }
                        }
                        mMap = googleMap;
                     /*   mMap.getUiSettings().setZoomControlsEnabled(true);
                        mMap.setMinZoomPreference(11);*/
                        LatLng curent = new LatLng(currentLatitude, currentLongitude);
                        place1 = new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current location").snippet("");
                        int i;
                        if(data!=null){
                            if(data.getData()!=null)
                            for (i = 0; i < data.getData().size(); i++) {
                                if(!lat.get(i).isEmpty() && !lon.get(i).isEmpty()){
                                    place2 = new MarkerOptions().position(new LatLng(Double.parseDouble(lat.get(i)), Double.parseDouble(lon.get(i)))).
                                            title(address.get(i)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker)).snippet(String.valueOf(i)).snippet(String.valueOf(i));
//                            googleMap.addMarker(place2);

                                    final MarkerDetails markerDetails = new MarkerDetails();
                                    markerDetails.setJob_id(data.getData().get(i).getJobId());
                                    markerDetails.setDis_id(data.getData().get(i).getDispatcherId());
                                    markerDetails.setDri_id(data.getData().get(i).getDriverId());
                                    markerDetails.setJob_start_status(data.getData().get(i).getJobStatus());
                                    CustomInfoViewAdapter customInfoWindow = new CustomInfoViewAdapter(LayoutInflater.from(getContext()), getContext(), data.getData().get(i));
                                    mMap.setInfoWindowAdapter(customInfoWindow);
                                    Marker m = mMap.addMarker(place2);
                                    m.setTag(markerDetails);
                                    m.showInfoWindow();
                                    googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                                        @Override
                                        public void onInfoWindowClick(Marker marker) {
                                            int pos= Integer.parseInt(marker.getSnippet());
                                            // CommonUtils.showSmallToast(getActivity(),marker.getSnippet());
                                            Intent intent = new Intent(getContext(), JobOfferForMyJob.class);
                                            intent.putExtra("job_id", data.getData().get(pos).getJobId());
                                            intent.putExtra("disp_id", data.getData().get(pos).getDispatcherId());
                                            intent.putExtra("dri_id", data.getData().get(pos).getDriverId());
                                            intent.putExtra("status_accept",data.getData().get(pos).getJobStatus());
                                            intent.putExtra("marker", "1");
                                            startActivity(intent);
                                        }
                                    });
                                }

                            }
                        }

                        if (online_status != null)
                            if (online_status.equalsIgnoreCase("1")) {
                                CameraPosition googlePlex = CameraPosition.builder()
                                        .target(curent)
                                        .zoom(12)
                                        .bearing(0)
                                        .tilt(45)
                                        .build();
                                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex));
                            //    googleMap.addMarker(place1);
                                googleMap.getUiSettings().isMyLocationButtonEnabled();
                                googleMap.getUiSettings().isZoomControlsEnabled();
                                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                            }
                    }
                });
            }
        }, 3000);

        // tvGo.performClick();

        // hitAllJobsToDriverAPI("");

        tvGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PreferenceHandler.writeString(getContext(), "status_onlie", "1");
                hitOnlineStatus("1");
                mContainer.setVisibility(View.VISIBLE);
                tv_offline_online.setVisibility(View.GONE);
                tvGo.setVisibility(View.GONE);
                tv_offline.setVisibility(View.VISIBLE);
                tvGoOffline.setVisibility(View.VISIBLE);
                SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                if (mMap != null) {
                    mMap.clear();
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {


                        if (data != null)
                            setViewPager(data);
                        // setViewPager(data);
                        mMap = googleMap;
                        boolean success = googleMap.setMapStyle(
                                MapStyleOptions.loadRawResourceStyle(
                                        getContext(), R.raw.style_json));
                        if (!success) {
                            Log.e("ooo", "Style parsing failed.");
                        }
                        LatLng curent = new LatLng(currentLatitude, currentLongitude);

                        place1 = new MarkerOptions().position(new LatLng(currentLatitude, currentLongitude)).title("Current location");

                        int i;
                        for (i = 0; i < lat.size(); i++) {
                            place2 = new MarkerOptions().position(new LatLng(Double.parseDouble(lat.get(i)), Double.parseDouble(lon.get(i)))).
                                    title(address.get(i)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.marker)).snippet(String.valueOf(i));
                            googleMap.addMarker(place2);
                        }

                        CameraPosition googlePlex = CameraPosition.builder()
                                .target(curent)
                                .zoom(12)
                                .bearing(0)
                                .tilt(45)
                                .build();
                        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex));
                       // googleMap.addMarker(place1);
                        googleMap.getUiSettings().isMyLocationButtonEnabled();
                        googleMap.getUiSettings().isZoomControlsEnabled();
                        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                    }
                });
            }
        });
        return view;
    }


    private void hitProfileViewAPI() {
        final Dialog dialog = AppUtil.showProgress(getActivity());
        Interface service = CommonUtils.retroInit();
        Call<Profile> call = service.view_profile(PreferenceHandler.readString(getActivity(),
                PreferenceHandler.USER_ID, ""));
        call.enqueue(new Callback<Profile>() {
            @Override
            public void onResponse(Call<Profile> call, Response<Profile> response) {
                Profile data = response.body();
                assert data != null;
                if (data.getCode().equalsIgnoreCase("201")) {
                    online_status = data.getData().get(0).getOnlineStatus();
                    if (online_status != null)
                        if (online_status.equalsIgnoreCase("1")) {
                            hitAllJobsToDriverAPI("");
                            PreferenceHandler.writeString(getActivity(),"status_onlie","1");

                        } else {
                            PreferenceHandler.writeString(getActivity(),"status_onlie","0");

                        }
                    dialog.dismiss();

                } else {
                    dialog.dismiss();
                    // CommonUtils.showSmallToast(getActivity(), data.getMessage());
                }
            }

            @Override
            public void onFailure(Call<Profile> call, Throwable t) {
                dialog.dismiss();
                CommonUtils.showSmallToast(getActivity(), t.getMessage());
            }
        });
    }


  /*  @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceHandler.writeString(getActivity(),"map","false");
    }*/


    private void initUI(View view) {
        mContainer = view.findViewById(R.id.pager_container3);
        vp_home = view.findViewById(R.id.pager3);
        tvGo = view.findViewById(R.id.tvGo);
        tvGoOffline = view.findViewById(R.id.tvGoOffline);
        tvGoOffline.setOnClickListener(this);
        tv_offline = view.findViewById(R.id.tv_offline);
        rl_offline = view.findViewById(R.id.rl_offline);
        tv_offline_online = view.findViewById(R.id.tv_offline_online);
        locationButton = ((View) view.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
    }

    private void hitAllJobsToDriverAPI(String onLocation) {
        // final Dialog dialog = AppUtil.showProgress(getActivity());
        Interface service = CommonUtils.retroInit();
        Call<AllJobsToDriver> call = service.allJobsToDriver(PreferenceHandler.readString(getContext(), PreferenceHandler.USER_ID, ""),
                currentLatitude, currentLongitude, "2000");
        call.enqueue(new Callback<AllJobsToDriver>() {
            @Override
            public void onResponse(Call<AllJobsToDriver> call, Response<AllJobsToDriver> response) {
                data = response.body();
                assert data != null;
                if (online_status != null)
                    if (online_status.equalsIgnoreCase("1")) {
                        setViewPager(data);
                    }
                if (online_status != null)
                    if (online_status.equalsIgnoreCase("0")) {
                        if (mMap != null)
                            mMap.clear();
                    }
                if (data.getCode().equalsIgnoreCase("201")) {
                    for (int i = 0; i < data.getData().size(); i++) {
                        lat.addAll(Collections.singleton(data.getData().get(i).getPuplatitude()));
                        lon.addAll(Collections.singleton(data.getData().get(i).getPuplongitude()));
                        address.addAll(Collections.singleton(data.getData().get(i).getCompanyName()));
                        job_id.addAll(Collections.singleton(data.getData().get(i).getJobId()));

                        LatLng latLng = new LatLng(Double.parseDouble(data.getData().get(i).getPuplatitude()),
                                Double.parseDouble(data.getData().get(i).getPuplongitude()));


                    }

                } else {

                    // CommonUtils.showSmallToast(getContext(), data.getMessage());
                }
            }

            @Override
            public void onFailure(Call<AllJobsToDriver> call, Throwable t) {
                //  dialog.dismiss();
                CommonUtils.showSmallToast(getContext(), t.getMessage());
            }
        });
    }


    private void setViewPager(AllJobsToDriver data) {
        vp_home = mContainer.getViewPager();
        vpAdapter = new VPAdapter(getFragmentManager(), data);
        vp_home.setAdapter(vpAdapter);
        //   vp_store.setCurrentItem(selectedValue);
        vp_home.setOffscreenPageLimit(5);
        vp_home.setPageMargin(0);
        vp_home.setClipChildren(false);
        vpAdapter.notifyDataSetChanged();

        vp_home.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


   /* private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(
                getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
    }*/


    @Override
    public void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
        //  mapFragment.getMapAsync(this);
        //   hitAllJobsToDriverAPI();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    private void locationPermission() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        checkLocation(); //c
        getLocation();
    }

    private void getLocation() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        onLocationChanged(location);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        startLocationUpdates();

        Location mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLocation == null) {
            startLocationUpdates();
        }
        if (mLocation != null) {

        } else {

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
            // PreferenceHandler.writeString(getActivity(),"map","false");
        }
    }

    protected void startLocationUpdates() {
        /* 10 secs */
        long UPDATE_INTERVAL = 5 * 1000;
        long FASTEST_INTERVAL = 10000;
        LocationRequest mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            Log.e("ttt", String.valueOf(tvGo.getVisibility()));

        } else {
            checkLocation();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();

        return isLocationEnabled();
    }


    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        checkLocation();
                        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                dialog.dismiss();
                            }
                        });
                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tvGoOffline:
                PreferenceHandler.writeString(getContext(), "status_onlie", "0");
                tv_offline_online.setVisibility(View.VISIBLE);
                tvGo.setVisibility(View.VISIBLE);
                tv_offline.setVisibility(View.GONE);
                tvGoOffline.setVisibility(View.GONE);
                hitOnlineStatus("0");
                if (mMap != null) {
                    mMap.clear();
                }
                mContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void hitOnlineStatus(final String s) {
        //final Dialog dialog = AppUtil.showProgress(getActivity());
        Interface service = CommonUtils.retroInit();
        Call<GenricModel> call = service.onlone_status(PreferenceHandler.readString(getActivity(), PreferenceHandler.USER_ID, ""), s,
                currentLatitude, currentLongitude);
        call.enqueue(new Callback<GenricModel>() {
            @Override
            public void onResponse(Call<GenricModel> call, Response<GenricModel> response) {
                GenricModel data = response.body();
                assert data != null;
                if (data.getCode().equalsIgnoreCase("201")) {
                    //            dialog.dismiss();
                    //     CommonUtils.showSmallToast(getActivity(), data.getMessage());
                    if (s.equalsIgnoreCase("1")) {
                        PreferenceHandler.writeString(getActivity(), "online_status", "1");
                    } else {
                        PreferenceHandler.writeString(getActivity(), "online_status", "0");
                    }

                } else {
                    //         dialog.dismiss();
                    // CommonUtils.showSmallToast(getContext(), data.getMessage());
                }
            }

            @Override
            public void onFailure(Call<GenricModel> call, Throwable t) {
                //       dialog.dismiss();
                CommonUtils.showSmallToast(getContext(), t.getMessage());
            }
        });
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @NonNull
    @Override
    public LatLng getPosition() {
        return null;
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }

    public class CustomMarkerInfoWindowView implements GoogleMap.InfoWindowAdapter {
        private final View markerItemView;

        CustomMarkerInfoWindowView() {
            markerItemView = LayoutInflater.from(getContext()).inflate(R.layout.activity_job_offer, null);  // 1
        }

        @Override
        public View getInfoWindow(Marker marker) {
            //  Intent i=new Intent(getContext(), ActiveJobDetail.class);
            //  startActivity(i);
            TextView company = markerItemView.findViewById(R.id.tv_company_name);
            TextView itemAddressTextView = markerItemView.findViewById(R.id.tv_header2);
            company.setText("Company Name - " + marker.getTitle());
            return markerItemView;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }

}