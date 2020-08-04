package com.example.colorize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.example.colorize.Utils.BitmapUtils
import com.example.colorize.Utils.NonSwipeableViewPager
import com.example.colorize.adapters.ViewPagerAdapter
import com.example.colorize.imageInterface.EditImageFragmentListener
import com.example.colorize.imageInterface.FilterListFragmentListener
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import kotlinx.android.synthetic.main.activity_filter.*
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Handler
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import com.algorithmia.Algorithmia.file
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import androidx.core.os.HandlerCompat.postDelayed
import com.google.android.material.snackbar.Snackbar
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import kotlin.math.round


class FilterActivity : AppCompatActivity(), EditImageFragmentListener {

    private lateinit var mReceivedImage: Bitmap
    lateinit var photoEditor: PhotoEditor

    init {
        System.loadLibrary("NativeImageProcessor")
    }

    private var originalImage: Bitmap? = null
    private lateinit var filteredImage: Bitmap
    private lateinit var finalImage: Bitmap

    //private lateinit var filterListFragment: FilterListFragment
    private lateinit var editImageFragment: EditImageFragment

    private var brightnessFinal = 0
    private var saturationFinal = 1.0f
    private var contrastFinal = 1.0f

    object Main {

        val IMAGE_NAME = "flash.jpg"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Colorimetry"

        //gets the file path
        val filePath = intent.getStringExtra("sentImageByteArray")


        //loads the file
        val file = File(filePath!!)

        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        //imageView.setImageBitmap(bitmap)

        mReceivedImage = bitmap

        photoEditor = PhotoEditor.Builder(this, image_preview)
            .setPinchTextScalable(true)
            .build()


        loadImage()
        loadFragment()
        //setupViewPager(viewPager)
        //tabs.setupWithViewPager(viewPager)


    }

    private fun loadFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        editImageFragment = EditImageFragment()
        editImageFragment.setListener(this)
        transaction.replace(R.id.fragmentHolder, editImageFragment)
        //transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun setupViewPager(viewPager: NonSwipeableViewPager?) {

        val adapter = ViewPagerAdapter(supportFragmentManager)

        //add FilterListFragment
        //filterListFragment = FilterListFragment()
        //filterListFragment.setListener(this)

        //add EditImageFragment
        editImageFragment = EditImageFragment()
        editImageFragment.setListener(this)

        //adapter.addFragment(filterListFragment, "FILTERS")
        adapter.addFragment(editImageFragment, "EDIT")

        viewPager!!.adapter = adapter

        /*var editImageFragment = EditImageFragment.getInstance()
        editImageFragment.setListener()*/

        //filterListFragment.displayImage(mReceivedImage)

        /* val handler = Handler()
         handler.postDelayed( {
             filterListFragment.displayImage(mReceivedImage)
         }, 500)   //0.5 seconds*/

    }

    private fun loadImage() {
        //originalImage = BitmapUtils.getBitmapFromAssets(this, mReceivedImage, 300, 300)
        originalImage = mReceivedImage
        filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888, true)
        finalImage = originalImage!!.copy(Bitmap.Config.ARGB_8888, true)
        image_preview.source.setImageBitmap(originalImage)

    }

    private fun rescaleImageEdit(bitmap: Bitmap): Bitmap {

        val density = resources.displayMetrics.density

        val currentBitmapWidth = bitmap.width
        val currentBitmapHeight = bitmap.height

        val ivWidth = photoArea.width
        val ivHeight = photoArea.height

        //var newWidth = ivWidth
        //var newHeight = ivHeight

        var newWidth = ivWidth
        var newHeight = ivHeight - 100 * density

        Log.d("Prije width i height", newWidth.toString().plus(" , " + newHeight.toString()))


        if (currentBitmapWidth > currentBitmapHeight) {
            //newWidth = main_constraintLayout.width
            newHeight =
                round(currentBitmapHeight.toDouble() * newWidth / currentBitmapWidth).toFloat()

        } else if (currentBitmapHeight > currentBitmapWidth) {
            //newHeight = main_constraintLayout.height - buttonsHeight - helpButtonsHeight - switchViewHeight
            newWidth =
                round(currentBitmapWidth.toDouble() * newHeight / currentBitmapHeight).toInt()

        } else {

            Log.d("ISTO", "ISTO")

            return bitmap
        }

        Log.d("Poslije width i height", newWidth.toString().plus(" , " + newHeight.toString()))

        Log.d("DENSITY", resources.displayMetrics.density.toString())


        val newBitmap = Bitmap.createScaledBitmap(bitmap, newWidth.toInt(), newHeight.toInt(), true)

        return newBitmap
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (id == R.id.action_save) {
            saveImage()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun saveImage() {

        photoEditor.saveAsBitmap(object : OnSaveBitmap {
            override fun onFailure(e: Exception?) {
                val snackbar =
                    Snackbar.make(coordinator, e!!.message.toString(), Snackbar.LENGTH_LONG)
                snackbar.show()
            }

            override fun onBitmapReady(saveBitmap: Bitmap?) {
                val imageWidth = intent.getIntExtra("imageWidth", 0)
                val imageHeight = intent.getIntExtra("imageHeight", 0)
                val resizedImage = Bitmap.createScaledBitmap(saveBitmap!!, imageWidth, imageHeight, true)
                //mReceivedImage = resizedImage
                val filePathFinal = tempFileImage(this@FilterActivity, resizedImage!!, "name")
                val resultIntent = Intent()
                resultIntent.putExtra("result", filePathFinal)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        })

    }


    override fun onBrightnessChanged(brightness: Int) {
        brightnessFinal = brightness
        val myFilter = Filter()
        myFilter.addSubFilter(BrightnessSubFilter(brightness))
        image_preview.source.setImageBitmap(
            myFilter.processFilter(
                finalImage.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                )
            )
        )
    }

    override fun onSaturationChanged(saturation: Float) {
        saturationFinal = saturation
        val myFilter = Filter()
        myFilter.addSubFilter(SaturationSubfilter(saturation))
        image_preview.source.setImageBitmap(
            myFilter.processFilter(
                finalImage.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                )
            )
        )
    }

    override fun onContrastChanged(contrast: Float) {
        contrastFinal = contrast
        val myFilter = Filter()
        myFilter.addSubFilter(ContrastSubFilter(contrast))
        image_preview.source.setImageBitmap(
            myFilter.processFilter(
                finalImage.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                )
            )
        )
    }

    override fun onEditStarted() {

    }

    override fun onEditCompleted() {
        val bitmap = filteredImage.copy(Bitmap.Config.ARGB_8888, true)
        val myFilter = Filter()
        myFilter.addSubFilter(ContrastSubFilter(contrastFinal))
        myFilter.addSubFilter(SaturationSubfilter(saturationFinal))
        myFilter.addSubFilter(BrightnessSubFilter(brightnessFinal))

    }

    /*override fun onFilterSelected(filter: com.zomato.photofilters.imageprocessors.Filter) {
        resetControls()
        filteredImage = originalImage!!.copy(Bitmap.Config.ARGB_8888, true)
        image_preview.source.setImageBitmap(filter.processFilter(filteredImage))
        finalImage = filteredImage.copy(Bitmap.Config.ARGB_8888, true)
    }*/

    private fun resetControls() {
        if (editImageFragment != null) {
            editImageFragment.resetControls()
        }

        brightnessFinal = 0
        saturationFinal = 1.0f
        contrastFinal = 1.0f
    }

    //creates a temporary file and return the absolute file path
    fun tempFileImage(context: Context, bitmap: Bitmap, name: String): String {

        val outputDir = context.cacheDir
        val imageFile = File(outputDir, "$name.jpg")

        val os: OutputStream
        try {
            os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.flush()
            os.close()
        } catch (e: Exception) {
            Log.e(context.javaClass.simpleName, "Error writing file", e)
        }

        return imageFile.absolutePath
    }


    override fun onSupportNavigateUp(): Boolean {

        onBackPressed()
        return true
    }
}
