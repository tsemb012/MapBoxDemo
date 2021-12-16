package com.example.mapboxdemo.ui.main

import android.graphics.Bitmap
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.example.mapboxdemo.R
import com.example.mapboxdemo.utils.LocationPermissionHelper
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import java.lang.IllegalStateException
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private val mapView: MapView by lazy { MapView(requireContext()) }
    private lateinit var locationPermissionHelper: LocationPermissionHelper
    private lateinit var bitmap: Bitmap
    // Create an instance of the Annotation API and get the PointAnnotationManager.



// Add the draggable pointAnnotation to the map.

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {//向いている方角が変わった時のリスナー
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())//詳細は後で確認
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {//場所が変わった時のリスナ０
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)//詳細は後で確認
    }

    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {//動きはじめ
            onCameraTrackingDismissed()
        }
        override fun onMove(detector: MoveGestureDetector): Boolean {//動いてる途中
            return false
        }
        override fun onMoveEnd(detector: MoveGestureDetector) {//動き終わり

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        /*TODO　レイアウトからMapViewを生成する方法
            val view = inflater.inflate(R.layout.main_fragment, container, false)
            mapView = view.findViewById(R.id.mapView) as MapView
            return inflater.inflate(R.layout.main_fragment, container, false)*/

        /*TODO
            ContextからMapViewを生成する方法
            mapView = MapView(requireContext())*/

        return mapView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bitmap = AppCompatResources.getDrawable(requireContext(), R.drawable.common_google_signin_btn_icon_dark)?.toBitmap() ?: throw IllegalStateException()

        val annotationApi = mapView.annotations
        val pointAnnotationManager = annotationApi.createPointAnnotationManager(mapView)
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(139.7222018, 35.6517392 ))
            .withIconImage(bitmap)// Make the annotation draggable.
            .withDraggable(true)
        pointAnnotationManager.create(pointAnnotationOptions)

        locationPermissionHelper = LocationPermissionHelper(WeakReference(requireActivity()))//Permission周りを手伝ってくれるオブジェクト
        locationPermissionHelper.checkPermissions { onMapReady() } //Permissionを確認して、コールバックを起動してくれるみたい。


        /*mapView.getMapboxMap()
            .loadStyleUri(Style.MAPBOX_STREETS
            ) { mapView.location.updateSettings{
                enabled = true
                pulsingEnabled = true
            } }*/

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    }



/*
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
*/

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    private fun onMapReady() {
        mapView.getMapboxMap().let {
            it.setCamera(
                CameraOptions.Builder()//カメラでどのようにとるか//地図を画面でどのように切り取るかを設定できる。
                    .zoom(14.0)
                    .build()
            )
            it.loadStyleUri(
                Style.MAPBOX_STREETS//マップのスタイルをUriからダウンロード（例）航空写真とか
            ) {//ロード後に呼び出されるコールバック
                initLocationComponent()
                setupGesturesListener()//ジェスチャー（指の動きを受けた時の操作を定義）
            }
        }

    }

    private fun setupGesturesListener() {
        mapView.gestures.addOnMoveListener(onMoveListener)
    }

    private fun initLocationComponent() {

        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            this.enabled = true //ユーザーの現在地を表示
            this.locationPuck = LocationPuck2D(//現在地アイコン
                bearingImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_user_puck_icon,
                ),
                shadowImage = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.mapbox_user_icon_shadow,
                ),
                scaleExpression = interpolate {//ここでなにしているの不明。いずれ確認する。
                    linear()
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(20.0)
                        literal(1.0)
                    }
                }.toJson()
            )
        }
        locationComponentPlugin.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)//場所が変わった時のリスナー
        locationComponentPlugin.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)//自分が向いている方角が変更した時のリスナー
    }


    private fun onCameraTrackingDismissed() {
        Toast.makeText(requireContext(), "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}