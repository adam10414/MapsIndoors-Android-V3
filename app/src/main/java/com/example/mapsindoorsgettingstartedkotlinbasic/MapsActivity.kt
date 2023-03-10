package com.example.mapsindoorsgettingstartedkotlinbasic

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.annotation.Nullable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.textfield.TextInputEditText
import com.mapsindoors.livesdk.LiveDataDomainTypes
import com.mapsindoors.mapssdk.*
import com.mapsindoors.mapssdk.errors.MIError


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnRouteResultListener{

    private lateinit var mMap: GoogleMap
    private lateinit var mapView: View
    private lateinit var mMapControl: MapControl
    private lateinit var mSearchFragment: SearchFragment
    private lateinit var mNavigationFragment: NavigationFragment
    private lateinit var mBtmnSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var mSearchTxtField: TextInputEditText
    private var mCurrentFragment: Fragment? = null
    private val mUserLocation: Point = Point(38.897389429704695, -77.03740973527613, 0.0)

    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mpRoutingProvider: MPRoutingProvider? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Initiates MapsIndoors.
        val mapsIndoorsKey = resources.getString(R.string.maps_indoors_key)
        val googleMapsKey = resources.getString(R.string.google_maps_key)

        MapsIndoors.initialize(applicationContext, mapsIndoorsKey)
        MapsIndoors.setGoogleAPIKey(googleMapsKey)

        mapFragment.view?.let {
            mapView = it
        }

        mSearchTxtField = findViewById(R.id.search_edit_txt)
        //Listener for when the user searches through the keyboard
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager

        mSearchTxtField.setOnEditorActionListener { textView, i, _ ->
            if (i == EditorInfo.IME_ACTION_DONE || i == EditorInfo.IME_ACTION_SEARCH) {
                if (textView.text.isNotEmpty()) {
                    //TODO: Call the search method when you have created it following the tutorial
                    search(mSearchTxtField.text.toString())
                }
                //Making sure keyboard is closed.
                imm.hideSoftInputFromWindow(textView.windowToken, 0)

                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        //ClickListener to start a search, when the user clicks the search button
        var searchBtn = findViewById<ImageButton>(R.id.search_btn)
        searchBtn.setOnClickListener {
            if (mSearchTxtField.text?.length != 0) {
                //There is text inside the search field. So lets do the search.
                //TODO: Call the search method when you have created it following the tutorial
                search(mSearchTxtField.text.toString())
            }
            //Making sure keyboard is closed.
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }

        var bottomSheet = findViewById<FrameLayout>(R.id.standardBottomSheet)
        mBtmnSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        mBtmnSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    if (mCurrentFragment != null) {
                        if (mCurrentFragment is NavigationFragment) {
                            //Clears the direction view if the navigation fragment is closed.
                            mpDirectionsRenderer?.clear()
                        }
                        //Clears the map if any searches has been done.
                        mMapControl?.let { mapControl ->
                            mapControl.clearMap()
                        }
                        //Removes the current fragment from the BottomSheet.
                        removeFragmentFromBottomSheet(mCurrentFragment!!)
                    }
                    mMapControl.setMapPadding(0, 0, 0, 0)
                } else {
                    mMapControl.setMapPadding(0, 0, 0, mBtmnSheetBehavior.peekHeight)
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mapView?.let { view ->
            initMapControl(view)
        }
    }

    private fun initMapControl(view: View) {
        //Creates a new instance of MapControl
        mMapControl = MapControl(this)
        enableLiveData()
        //Sets the Google map object and the map view to the MapControl
        mMapControl.setGoogleMap(mMap, view)
        mMapControl.init { miError ->
            if (miError == null) {
                //No errors so getting the first venue.
                val venue = MapsIndoors.getVenues()?.currentVenue

                runOnUiThread{
                    //Animates the camera to fit the venu
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(venue?.latLngBoundingBox, 19))
                }
            }
        }
    }

    private fun enableLiveData() {
        mMapControl.enableLiveData(LiveDataDomainTypes.AVAILABILITY_DOMAIN)
        mMapControl.enableLiveData(LiveDataDomainTypes.OCCUPANCY_DOMAIN)
        mMapControl.enableLiveData(LiveDataDomainTypes.POSITION_DOMAIN)
    }

    private fun search(searchQuery: String){
        val mpQuery = MPQuery.Builder().setQuery(searchQuery).build()
        val mpFilter = MPFilter.Builder().setTake(30).build()

        //Query the locations
        MapsIndoors.getLocationsAsync(mpQuery, mpFilter) {list: List<MPLocation?>?, miError: MIError? ->

            //Ensuring there is no error and the list is not empty.
            if (miError == null && !list.isNullOrEmpty()) {
                runOnUiThread {
                    mMapControl.displaySearchResults(list, true)
                }
                //Creating a new instance of the search fragment.
                mSearchFragment = SearchFragment.newInstance(list, this)
                //Make a transaction to the bottom sheet.
                addFragmentToBottomSheet(mSearchFragment)
                //Clear the search text since we got a result.
                mSearchTxtField.text?.clear()
            }
        }
    }

    override fun onRouteResult(@Nullable route: Route?, @Nullable miError: MIError?) {
        if (mpDirectionsRenderer == null) {
            mpDirectionsRenderer = MPDirectionsRenderer(this, mMap, mMapControl, OnLegSelectedListener { i: Int ->
                mpDirectionsRenderer?.setRouteLegIndex(i)
                mMapControl.selectFloor(mpDirectionsRenderer!!.currentFloor)
            })
        }

        //Setting the route on the Directions renderer
        mpDirectionsRenderer?.setRoute(route!!)

        //New instance of nav fragment
        mNavigationFragment = NavigationFragment.newInstance(route, this)

        //Starting a transaction and assigning it to the bottom sheet
        addFragmentToBottomSheet(mNavigationFragment)

        //since camera movement is involved, we're running this on the UI thread.
        runOnUiThread{
            mpDirectionsRenderer?.initMap(true)
        }
    }

    fun createRoute(mpLocation: MPLocation) {
        if (mpRoutingProvider == null) {
            mpRoutingProvider = MPRoutingProvider()
            mpRoutingProvider?.setOnRouteResultListener(this)

        }

        mpRoutingProvider?.setTravelMode(TravelMode.WALKING)
        mpRoutingProvider?.query(mUserLocation, mpLocation.point)
    }

    fun getMpDirectionsRenderer(): MPDirectionsRenderer? {
        return mpDirectionsRenderer
    }

    fun getMapControl(): MapControl {
        return mMapControl
    }

    fun addFragmentToBottomSheet(newFragment: Fragment) {
        if (mCurrentFragment != null) {
            supportFragmentManager.beginTransaction().remove(mCurrentFragment!!).commit()
        }
        supportFragmentManager.beginTransaction().replace(R.id.standardBottomSheet, newFragment).commit()
        mCurrentFragment = newFragment
        //Set the map padding to the height of the bottom sheets peek height. To not obfuscate the google logo.
        runOnUiThread {
            mMapControl.setMapPadding(0, 0, 0, mBtmnSheetBehavior.peekHeight)
            if (mBtmnSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                mBtmnSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    fun removeFragmentFromBottomSheet(fragment: Fragment) {
        if (mCurrentFragment == fragment) {
            mCurrentFragment = null
        }
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        runOnUiThread { mMapControl.setMapPadding(0, 0, 0, 0) }
    }
}