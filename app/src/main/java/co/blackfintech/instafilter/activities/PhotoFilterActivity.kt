package co.blackfintech.instafilter.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.AdapterView
import co.blackfintech.instafilter.adapters.AbstractFilterAdapter
import co.blackfintech.instafilter.R
import co.blackfintech.instafilter.filters.AbstractFilter
import co.blackfintech.instafilter.filters.Amartoka
import co.blackfintech.instafilter.filters.Juno
import co.blackfintech.instafilter.filters.Original
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.android.synthetic.main.activity_photo_filter.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.media.MediaScannerConnection
import android.net.Uri


////////////////////////////////////////////////////////////////////////////////////////////////////
class PhotoFilterActivity : AppCompatActivity() {

  // region Variables
  companion object {

    val REQUEST_CODE_PHOTO_PICK = 1
    val MY_PERMISSION_WRITE_EXTERNAL_STORAGE = 1000
  }

  private val filters by lazy {

    Array<AbstractFilter>(3, { i ->

      when (i) {

        0 -> Original(this@PhotoFilterActivity)
        1 -> Juno(this@PhotoFilterActivity)
        2 -> Amartoka(this@PhotoFilterActivity)
        else -> throw IndexOutOfBoundsException()
      }
    })
  }

  private var lastFilterIndex = 0
  private var lastUsedFilter: AbstractFilter? = null

  private val adapter: AbstractFilterAdapter by lazy {

    AbstractFilterAdapter.lastPosition = lastFilterIndex
    AbstractFilterAdapter(this@PhotoFilterActivity,
        R.layout.list_filter, filters)
  }
  // endregion

  // region Lifecycle
  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_photo_filter)
    bindViews()
    initAdapter()
    pickPhoto()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

    when (requestCode) {

      REQUEST_CODE_PHOTO_PICK -> {

        when (resultCode) {

          Activity.RESULT_OK -> {

            gpuImage.setImage(data?.data)
            gpuImage.setScaleType(GPUImage.ScaleType.CENTER_CROP)
          }

          else -> finish()
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }
  // endregion

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    when(requestCode){
      MY_PERMISSION_WRITE_EXTERNAL_STORAGE ->{
        if(grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) saveImage()
      }
    }
  }

  private fun checkPermission(){
    if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
      if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){

      }else{
        ActivityCompat.requestPermissions(this,
                Array(1,{i->Manifest.permission.WRITE_EXTERNAL_STORAGE}),
                MY_PERMISSION_WRITE_EXTERNAL_STORAGE)
      }
    }else{
      saveImage()
    }
  }

  // region Salin-salin methods
  private fun bindViews() {

    saveButton.setOnClickListener {

      checkPermission()
    }
  }

  private fun initAdapter() {

    AbstractFilterAdapter.lastPosition = lastFilterIndex
    horizontalList.adapter = adapter
    horizontalList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->

      try {

        if (position == lastFilterIndex) return@OnItemClickListener
        lastFilterIndex = position
        AbstractFilterAdapter.lastPosition = position
        adapter.notifyDataSetChanged()

        filters[position].context = this@PhotoFilterActivity
        filters[position].initialize()
        val imageFilter = filters[position].filter

        gpuImage.filter = imageFilter
        gpuImage.requestRender()

        if (lastUsedFilter == null) {
          lastUsedFilter = filters[position]
        } else {
          lastUsedFilter?.release()
          lastUsedFilter = filters[position]
        }
      } catch (e: OutOfMemoryError) {

        Log.e("APP:", "Error $e")
      }
    }
  }

  private fun pickPhoto() {

    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    startActivityForResult(intent, REQUEST_CODE_PHOTO_PICK)
  }

  private fun saveImage() {

    val filename = "${System.currentTimeMillis()}.jpg"
    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val file = File(path, "${getString(R.string.app_name)}/$filename")
    file.parentFile.mkdirs()

    var out: FileOutputStream? = null
    val bitmap = gpuImage.gpuImage.bitmapWithFilterApplied
    try {
      out = FileOutputStream(file)
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    } catch (e: Exception) {
      e.printStackTrace()
    } finally {
      try {
        out?.close()
      } catch (ioe: IOException) {
        ioe.printStackTrace()
      }
      setResult(Activity.RESULT_OK)
      finish()
    }

    MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null,
            object : MediaScannerConnection.OnScanCompletedListener {
              override fun onScanCompleted(path: String, uri: Uri) {
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=" + uri)
              }
            })
  }
  // endregion
}
////////////////////////////////////////////////////////////////////////////////////////////////////
