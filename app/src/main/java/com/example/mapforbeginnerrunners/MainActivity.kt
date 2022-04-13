package com.example.mapforbeginnerrunners

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.geometry.SubpolylineHelper
import com.yandex.mapkit.layers.GeoObjectTapEvent
import com.yandex.mapkit.layers.GeoObjectTapListener
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.mapkit.transport.TransportFactory
import com.yandex.mapkit.transport.masstransit.*
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError
import com.yandex.mapkit.search.Session as SearchSession
import com.yandex.mapkit.transport.masstransit.Session as MasstransitSession
import com.yandex.mapkit.transport.masstransit.SummarySession as MasstransitSummarySession


class MainActivity : AppCompatActivity(), SearchSession.SearchListener, GeoObjectTapListener,
    InputListener, CameraListener, MasstransitSession.RouteListener { //, MasstransitSummarySession.SummaryListener {

    private lateinit var mapview: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var startingPoint_searchEditText: EditText
    private lateinit var endPoint_searchEditText: EditText
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var choose_point_button: Button
    private lateinit var mtRouter: PedestrianRouter     // Пешеходный маршрутизатор
    private val TARGET_LOCATION = Point(59.936760, 30.314673)
    private var last_search_query = ""
    private var starting_point: Point? = null
    private var end_point: Point? = null
    private lateinit var object_point: Point
    private lateinit var typeOfQuery: String
    private lateinit var objectAddress_textView: TextView
    private var routing_is_on: Boolean = true           // установлено true, для того чтобы при
                                                        // запуске не выполнялся поиск пустой
                                                        // строки в методе OnPositionChanged
    //private lateinit var linearLayout: LinearLayout
    //private lateinit var linearLayout2: LinearLayout

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
        TransportFactory.initialize(this)   // Инициализация главного "завода" по построению маршрутов

        setContentView(R.layout.activity_main)

        //linearLayout = findViewById(R.id.linearLayout)
        //linearLayout2 = findViewById(R.id.linearLayout2)
        //set_LinearLayouts_width()

        val klaviatura: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                as InputMethodManager

        startingPoint_searchEditText = findViewById(R.id.starting_point_search_manager)
        endPoint_searchEditText = findViewById(R.id.end_point_search_manager)
        startingPoint_searchEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) startingPoint_searchEditText.isCursorVisible = true }
        endPoint_searchEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) endPoint_searchEditText.isCursorVisible = true }


        mapview = findViewById<View>(R.id.mapview) as MapView
        mapview.requestFocus()
        mapview.map.addTapListener(this)
        mapview.map.addInputListener(this)
        mapview.map.addCameraListener(this)

        // Move the camera to the center of Saint Petersburg.
        mapview.map.move(
            CameraPosition(TARGET_LOCATION, 17.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 1F),
            null
        )

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        mtRouter = TransportFactory.getInstance().createPedestrianRouter()      // Создание образца пешеходного маршрутизатора


        bottomSheetDialog = BottomSheetDialog(this)

        bottomSheetDialog.setContentView(R.layout.object_description)
        choose_point_button = bottomSheetDialog.findViewById(R.id.choose_point_button)!!
        objectAddress_textView = bottomSheetDialog.findViewById(R.id.object_address)!!
        choose_point_button.setOnClickListener { view ->
            choosePoints()
            if (starting_point != null && end_point == null)
                choose_point_button.setText(R.string.button_to_choose_end_point_in_bottom_sheet)
            else if (starting_point != null && end_point != null)
                choose_point_button.setText(R.string.button_to_choose_starting_point_in_bottom_sheet)
            bottomSheetDialog.dismiss()
        }


        startingPoint_searchEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            startingPoint_searchEditText.isCursorVisible = true
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitStringQuery(startingPoint_searchEditText.text.toString())
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
                submitStringQuery(endPoint_searchEditText.text.toString())
                endPoint_searchEditText.isCursorVisible = false
                mapview.requestFocus()

                klaviatura.hideSoftInputFromWindow(endPoint_searchEditText.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS)
            }
            false
        }

    }

    private fun submitStringQuery(query: String = "") {
        typeOfQuery = "string"
        routing_is_on = false
        searchManager.submit( query,
            VisibleRegionUtils.toPolygon(mapview.map.visibleRegion),
            SearchOptions(),
            this)
        last_search_query = query
    }

    override fun onSearchResponse(response: Response) {
        if (typeOfQuery == "string") {
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
        else if (typeOfQuery == "point") {
            val address = mutableListOf(
                response.collection.children.firstOrNull()?.obj                         // city
                    ?.metadataContainer?.getItem(ToponymObjectMetadata::class.java)
                    ?.address?.components
                    ?.firstOrNull { it.kinds.contains(Address.Component.Kind.LOCALITY) }
                    ?.name,
                response.collection.children.firstOrNull()?.obj                         // street
                    ?.metadataContainer
                    ?.getItem(ToponymObjectMetadata::class.java)
                    ?.address
                    ?.components
                    ?.firstOrNull { it.kinds.contains(Address.Component.Kind.STREET) }
                    ?.name,
                response.collection.children.firstOrNull()?.obj                         // number of
                    ?.metadataContainer                                                 // house
                    ?.getItem(ToponymObjectMetadata::class.java)
                    ?.address
                    ?.components
                    ?.firstOrNull { it.kinds.contains(Address.Component.Kind.HOUSE) }
                    ?.name
            )
            var result = ""
            for (elem in address) if (elem != null) result += "$elem, "
            if (result == "") objectAddress_textView.setText(R.string.object_address)
            else objectAddress_textView.text = result.dropLast(2)
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
            object_point = geoObjectTapEvent.geoObject.geometry[0].point!!
            typeOfQuery = "point"
            val searchOptions = SearchOptions()
            searchOptions.searchTypes = SearchType.GEO.value
            searchManager.submit(object_point, mapview.map.cameraPosition.zoom.toInt(), searchOptions, this)
            bottomSheetDialog.show()
        }

        return selectionMetadata != null
    }


    private fun choosePoints() {
        if (starting_point != null && end_point == null) {
            end_point = object_point
            val points: MutableList<RequestPoint> = ArrayList()

            points.add(RequestPoint(starting_point!!, RequestPointType.WAYPOINT, null))
            points.add(RequestPoint(end_point!!, RequestPointType.WAYPOINT, null))
            mtRouter.requestRoutes(points, TimeOptions(), this)
            //mtRouter.requestRoutesSummary(points, TimeOptions(), this)
        } else {
            starting_point = object_point
            end_point = null
        }
    }


    override fun onMapTap(map: Map, point: Point) {
        mapview.map.deselectGeoObject()
    }

    override fun onMapLongTap(map: Map, point: Point) {
        mapview.map.deselectGeoObject()
    }

    override fun onCameraPositionChanged(p0: Map, p1: CameraPosition, p2: CameraUpdateReason,
                                         finished: Boolean)
    {
        if (finished and !routing_is_on) {
            typeOfQuery = "string"
            submitStringQuery(last_search_query)
        }
    }

    override fun onMasstransitRoutes(routes: MutableList<Route>) {
        if (routes.isNotEmpty()) {
            for (section in routes[0].sections) {
                drawSection(
                    section.metadata.data,
                    SubpolylineHelper.subpolyline( routes[0].geometry, section.geometry )
                )
            }
        }
    }


    private fun drawSection(data: SectionMetadata.SectionData, geometry: Polyline) {

        val mapObjects = mapview.map.mapObjects
        mapObjects.clear()
        val polylineMapObject = mapObjects.addCollection().addPolyline(geometry)

        if (data.walk != null) {
            polylineMapObject.strokeColor = -0x1000000 // Black
            routing_is_on = true
            return
        }
        routing_is_on = false
    }

    override fun onMasstransitRoutesError(error: Error) {
        var errorMessage = getString(R.string.unknown_error_message)
        if (error is RemoteError) {
            errorMessage = getString(R.string.remote_error_message)
        } else if (error is NetworkError) {
            errorMessage = getString(R.string.network_error_message)
        }
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }
}