package com.example.mapforbeginnerrunners

// import com.yandex.mapkit.map.SizeChangedListener
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError


class MainActivity : AppCompatActivity(), Session.SearchListener, GeoObjectTapListener,
    InputListener {

    private lateinit var mapview: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var startingPoint_searchEditText: EditText
    private lateinit var endPoint_searchEditText: EditText
    private val TARGET_LOCATION = Point(59.936760, 30.314673)
    private var last_search_query = ""
    //private lateinit var linearLayout: LinearLayout
    //private lateinit var linearLayout2: LinearLayout

    private fun submitQuery(query: String = "") {
        searchManager.submit( query,
                              VisibleRegionUtils.toPolygon(mapview.map.visibleRegion),
                              SearchOptions(),
                              this)
        last_search_query = query
    }


    // Недоделанная функция для того, чтобы 2 LinearLayouts делили экран по ширине пополам

    /*private fun set_LinearLayouts_width() {
        val display: Display = windowManager.defaultDisplay
        val size: android.graphics.Point = android_point()
        display.getSize(size)
        val windowWidth = size.x
        // val windowHeight = size.y


        val linearLayout_size: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(windowWidth / 2, 100)
        //linearLayout_size.addRule()
        linearLayout.layoutParams = linearLayout_size
        linearLayout2.layoutParams = linearLayout_size


        val constraintLayout: ConstraintLayout = findViewById(R.id.activity_main)
        val constraintSet = ConstraintSet()
        var mConstraintLayout: ConstraintLayout
        constraintSet.connect(R.id.linearLayout2, ConstraintSet.END,
                              R.id.activity_main, ConstraintSet.END)
        constraintSet.connect(R.id.linearLayout2, ConstraintSet.TOP,
                              R.id.activity_main, ConstraintSet.TOP)
        constraintSet.applyTo(constraintLayout)

    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val MAPKIT_API_KEY: String =
                applicationContext.assets.open("MAPKIT_KEY.txt").bufferedReader().use {
                    it.readText() }

        MapKitFactory.setApiKey(MAPKIT_API_KEY)

        MapKitFactory.initialize(this)
        SearchFactory.initialize(this)

        setContentView(R.layout.activity_main)

        //linearLayout = findViewById(R.id.linearLayout)
        //linearLayout2 = findViewById(R.id.linearLayout2)
        //set_LinearLayouts_width()

        val klaviatura: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        startingPoint_searchEditText = findViewById(R.id.starting_point_search_manager)
        endPoint_searchEditText = findViewById(R.id.end_point_search_manager)
        startingPoint_searchEditText.setOnClickListener { startingPoint_searchEditText.isCursorVisible = true }
        endPoint_searchEditText.setOnClickListener { endPoint_searchEditText.isCursorVisible = true }


        val editTextClickListener: View.OnClickListener = View.OnClickListener { v ->
                startingPoint_searchEditText.isCursorVisible = true
        }

        startingPoint_searchEditText.setOnClickListener(editTextClickListener)


        mapview = findViewById<View>(R.id.mapview) as MapView
        mapview.requestFocus()
//        mapview.addSizeChangedListener(sizeChangedListener())

        // And to show what can be done with it, we move the camera to the center of Saint Petersburg.
        mapview.map.move(
            CameraPosition(TARGET_LOCATION, 17.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1F),
            null
        )
        mapview.map.addTapListener(this)
        mapview.map.addInputListener(this)
        
        
        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)

        startingPoint_searchEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            startingPoint_searchEditText.isCursorVisible = true
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitQuery(startingPoint_searchEditText.text.toString())
                startingPoint_searchEditText.isCursorVisible = false
                mapview.requestFocus()

                klaviatura.hideSoftInputFromWindow(startingPoint_searchEditText.windowToken,
                                                   InputMethodManager.HIDE_NOT_ALWAYS)
                }
            false
        }

        endPoint_searchEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            endPoint_searchEditText.isCursorVisible = true
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitQuery(endPoint_searchEditText.text.toString())
                endPoint_searchEditText.isCursorVisible = false
                mapview.requestFocus()

                klaviatura.hideSoftInputFromWindow(endPoint_searchEditText.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS)
            }
            false
        }
    }

    override fun onSearchResponse(response: Response) {
        val mapObjects = mapview.map.mapObjects
        mapObjects.clear()

        for (searchResult in response.collection.children) {
            val resultLocation = searchResult.obj?.geometry?.get(0)?.point
            if (resultLocation != null) {
                mapObjects.addPlacemark(
                    resultLocation,
                    ImageProvider.fromResource(this, R.drawable.search_result)
                )
            }
        }
    }

    override fun onSearchError(error: Error) {
        var errorMessage = getString(R.string.unknown_error_message)
        if (error is RemoteError) {
            errorMessage = getString(R.string.remote_error_message)
        } else if (error is NetworkError) {
            errorMessage = getString(R.string.network_error_message)
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        mapview.onStop()
        MapKitFactory.getInstance().onStop()
    }

    override fun onStart() {
        super.onStart()
        mapview.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onObjectTap(geoObjectTapEvent: GeoObjectTapEvent): Boolean {
        val selectionMetadata = geoObjectTapEvent
            .geoObject
            .metadataContainer
            .getItem(GeoObjectSelectionMetadata::class.java)
        if (selectionMetadata != null) {
            mapview.map.selectGeoObject(selectionMetadata.id, selectionMetadata.layerId)
        }
        return selectionMetadata != null
    }

    override fun onMapTap(map: Map, point: Point) {
        mapview.map.deselectGeoObject()
    }

    override fun onMapLongTap(map: Map, point: Point) {
        // TODO document why this method is empty
    }

}
