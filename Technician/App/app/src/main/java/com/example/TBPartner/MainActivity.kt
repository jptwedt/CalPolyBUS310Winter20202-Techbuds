package com.example.TBPartner

import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import com.github.nkzawa.emitter.Emitter
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
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException


public class MainActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    LocationListener {
    private lateinit var mMap: GoogleMap;
    private val SERVERURL = "http://52.89.101.6:3000"
    private lateinit var mSocket: Socket
    private lateinit var fusedLocationClient:FusedLocationProviderClient
    private var locReq: LocationRequest? = null;
    private var myLoc = LatLng(35.300422, -120.664504)
    private var clientLoc = LatLng(35.299986, -120.664435)
    private var clientMarker: Marker? = null
    private lateinit var locCallback:LocationCallback
    private val LOCTAG = "techLocation"
    private val DBGTAG = "techDebug"
    private var msg = ""
    private val onNewMessage = Emitter.Listener { args ->
        runOnUiThread(Runnable {
            val data = args[0] as JSONObject
            val latitude:Double;
            val longitude:Double;

            try {
                latitude = data.getString("clientlat").toDouble()
                longitude = data.getString("clientlong").toDouble()
                clientLoc = LatLng(latitude.toDouble(), longitude.toDouble())
                Log.d("clientLocation", "" + clientLoc.latitude +
                        ";" + clientLoc.longitude)
                animateMap()
            } catch (e: JSONException) {
                return@Runnable
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_maps)

        socket()
        mSocket.connect()

        Log.d(DBGTAG, "technician app created")
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locCallback = object : LocationCallback() {
            override fun onLocationResult(locResult:LocationResult?){
                locResult ?: return
                for(location in locResult.locations){
                    myLoc = LatLng(location.latitude, location.longitude)
                    msg = "" + location.latitude + ";" + location.longitude;
                    posChangeNotify(LOCTAG, msg)
                }
            }
        }

        createLocationRequest()

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                myLoc = LatLng(location.latitude, location.longitude)
                msg = "" + location.latitude + ";" + location.longitude;

                posChangeNotify(LOCTAG, msg)
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
        msg = "technician map ready"

        statusChangeNotify(DBGTAG, msg)

        animateMap()
    }

    override fun onConnected(p0: Bundle?) {
        msg = "technician connected"
        posChangeNotify(DBGTAG, msg)
    }

    override fun onConnectionSuspended(p0: Int) {
        msg = "technician connection suspended"
        posChangeNotify(DBGTAG, msg)
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        msg = "technician connection failed"
        posChangeNotify(DBGTAG, msg)
    }

    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            msg = "technician changed location is:\n\tLatitude " + location.latitude +
                    "\n\tLongitude: " + location.longitude + "\n"
            statusChangeNotify(DBGTAG, msg)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        statusChangeNotify(DBGTAG, "technician resuming")
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locReq, locCallback, Looper.getMainLooper())
        statusChangeNotify(DBGTAG, "technician starting location updates")
    }


    private fun socket(){
        try{
            mSocket=IO.socket(SERVERURL)
            mSocket.on("serverclientpos", onNewMessage)
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
            interval = 2000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun posChangeNotify(LOCTAG: String, msg:String){
        Log.d(LOCTAG, msg);
        attemptSend(LOCTAG, msg)
    }

    private fun statusChangeNotify(DBGTAG: String, msg:String){
        Log.d(DBGTAG, msg);
        attemptSend(DBGTAG, msg)
    }

    private fun animateMap(){
        if(mMap != null) {
            if (clientMarker == null) {
                clientMarker = mMap.addMarker(MarkerOptions().position(clientLoc).title("Client"))
            } else {
                clientMarker!!.position = clientLoc
            }

            mMap.animateCamera(
                CameraUpdateFactory
                    .newLatLngZoom(clientLoc, 20.0f)
            )
        }
    }

    fun onRadioButtonClicked(view:View){
        if(view is RadioButton){
            val checked = view.isChecked

            when(view.getId()){
                R.id.radioStart -> {
                    if (checked) {
                        attemptSend("jobStart", "Technician begin")
                        statusChangeNotify("startBtn", "pressed")
                    }
                }
                R.id.radioEnd -> {
                    if (checked) {
                        attemptSend("jobEnd", "Technician complete")
                        statusChangeNotify("endBtn", "pressed")
                    }
                }
            }
        }
    }
}
