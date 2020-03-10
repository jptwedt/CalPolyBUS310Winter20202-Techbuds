package com.example.techbuds

import android.location.Location
import com.google.android.gms.location.LocationListener
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.net.URISyntaxException

public class MainActivity : AppCompatActivity(), OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private lateinit var mMap: GoogleMap;
    //private final val SERVERURL = "http://10.0.2.2:3000"
    private val SERVERURL = "http://52.89.101.6:3000"
    private lateinit var mSocket: Socket
    private lateinit var fusedLocationClient:FusedLocationProviderClient
    private var locReq: LocationRequest? = null;
    private var techLoc = LatLng(35.300422, -120.664504)
    private var techMarker: Marker? = null
    private lateinit var locCallback:LocationCallback
    private val TAG = "techLocation"
    private var msg = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        socket()
        mSocket.connect()

        Log.d(TAG, "oncreate location")
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locCallback = object : LocationCallback() {
            override fun onLocationResult(locResult:LocationResult?){
                locResult ?: return
                for(location in locResult.locations){
                    techLoc = LatLng(location.latitude, location.longitude)
                    msg = "cb location is \n\tLatitude " + location.latitude +
                            "\n\tLongitude " + location.longitude;
                    Log.d(TAG, msg)
                    attemptSend(TAG, msg)
                    animateMap()
                }
            }
        }

        createLocationRequest()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                techLoc = LatLng(location.latitude, location.longitude)
                msg = "Start location is:\n\tLatitude " + location.latitude +
                        "\n\tLongitude: " + location.longitude + "\n"

                Log.d(TAG, msg)
                attemptSend(TAG, msg)
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_HYBRID;
        msg = "map ready"

        attemptSend(TAG, msg)
        Log.d(TAG, msg)

        animateMap()
    }

    override fun onConnected(p0: Bundle?) {
        msg = "connected"
        posChangeNotify(TAG, msg)
    }

    override fun onConnectionSuspended(p0: Int) {
        msg = "connection suspended"
        posChangeNotify(TAG, msg)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        msg = "connection failed"
        posChangeNotify(TAG, msg)
    }

    override fun onLocationChanged(location: Location?) {
        Log.d(TAG, "in location changed")
        if (location != null) {
            msg = "New location is:\n\tLatitude " + location.latitude +
                    "\n\tLongitude: " + location.longitude + "\n"
            posChangeNotify(TAG, msg)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locReq, locCallback, Looper.getMainLooper())
    }


    private fun socket(){
        Log.d("LOCATE", "socket init")
        try{
            mSocket=IO.socket(SERVERURL)
        }
        catch(e:URISyntaxException){
            e.printStackTrace()
        }
    }

    private fun attemptSend(TAG:String, msg:String){
        mSocket.emit(TAG, msg);
    }

    private fun createLocationRequest(){
        locReq  = LocationRequest.create()?.apply {
            interval = 1000
            fastestInterval = 1000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun posChangeNotify(TAG: String, msg:String){
        Log.d(TAG, msg);
        attemptSend(TAG, msg)
    }

    private fun animateMap(){
        if(mMap != null) {
            if (techMarker == null) {
                techMarker = mMap.addMarker(MarkerOptions().position(techLoc).title("Technician"))
            } else {
                techMarker!!.position = techLoc
            }

            mMap.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(techLoc, 20.0f)
            )
        }
    }
}
