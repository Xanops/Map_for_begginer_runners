package com.example.mapforbeginnerrunners

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.*
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.network.NetworkError
import com.yandex.runtime.network.RemoteError


class MainActivity : AppCompatActivity(), Session.SearchListener {

    private lateinit var mapview: MapView
    private lateinit var searchManager: SearchManager
    private lateinit var searchEdit: EditText
    private lateinit var searchSession: Session

    private fun submitQuery(query: String = "") {
        searchSession = searchManager.submit(
            query,
            VisibleRegionUtils.toPolygon(mapview.map.visibleRegion),
            SearchOptions(),
            this)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val MAPKIT_API_KEY: String =
                applicationContext.assets.open("MAPKIT_KEY.txt").bufferedReader().use {
                    it.readText() }
        var flag = true

        MapKitFactory.setApiKey(MAPKIT_API_KEY)

        MapKitFactory.initialize(this)
        SearchFactory.initialize(this)

        setContentView(R.layout.activity_main)

        searchEdit = findViewById(R.id.search_manager)
        searchEdit.setOnClickListener {
            if (flag) {
                searchEdit.isCursorVisible = true
            }
            flag = true
        }

        mapview = findViewById(R.id.mapview)
        mapview.map.move(
            CameraPosition(Point(55.751574, 37.573856), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0.toFloat()),
            null)

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
        searchEdit = findViewById<View>(R.id.search_manager) as EditText
        searchEdit.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitQuery(searchEdit.text.toString())
                searchEdit.isCursorVisible = false

                val klaviatura: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                klaviatura.hideSoftInputFromWindow(searchEdit.windowToken,
                                                   InputMethodManager.HIDE_NOT_ALWAYS)
                flag = false
                }
            false
        }
        submitQuery(searchEdit.text.toString())
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
}
