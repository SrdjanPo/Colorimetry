package com.example.colorize

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.algorithmia.Algorithmia
import org.json.JSONObject
import com.example.colorize.models.OutputImage
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_main.*
import com.google.gson.reflect.TypeToken
import com.algorithmia.Algorithmia.file
import java.nio.file.Files.exists
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Filter
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.algorithmia.algo.AlgoResponse
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_filter.*
import java.io.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round


class MainActivity : AppCompatActivity() {

    private val FIRST_TOAST: Long = 3000
    private val SECOND_TOAST: Long = 5000
    private val THIRD_TOAST: Long = 5000

    private var REQUEST_CODE_STORAGE_PERMISSION = 1
    private var REQUEST_CODE_SELECT_IMAGE = 2
    private var REQUEST_CODE_WRITE_EXTERNAL = 3
    private var REQUEST_CODE_FILTER = 4
    private lateinit var photoFromGallery: String
    private lateinit var bitmapImageGray: Bitmap
    private lateinit var bitmapImageColored: Bitmap

    private var buttonsHeight = 0
    private var helpButtonsHeight = 0
    private var switchViewHeight = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonsHeight = buttons.height
        helpButtonsHeight = helpButtons.height
        switchViewHeight = switchView.height

        val bitmapGrayDefault =
            BitmapFactory.decodeResource(resources, R.drawable.default_gray_image)
        imageview.setImageBitmap(bitmapGrayDefault)
        bitmapImageGray = bitmapGrayDefault

        val bitmapColoredDefault =
            BitmapFactory.decodeResource(resources, R.drawable.default_colorized_image)
         bitmapImageColored = bitmapColoredDefault

        photoSwitch.setOnCheckedChangeListener { buttonView, isChecked ->

            val animation =
                AlphaAnimation(0.9f, 0.8f);//to change visibility from visible to invisible
            animation.setDuration(100) //0.5 second duration for each animation cycle
            animation.setInterpolator(LinearInterpolator())
            //animation.setRepeatCount(Animation.INFINITE) //repeating indefinitely
            //animation.setRepeatMode(Animation.REVERSE) //animation will start from end point once ended.
            imageview.startAnimation(animation) //to start animation

            if (isChecked) {

                imageview.setImageBitmap(bitmapImageColored)
                switchText.text = "Enabled"
                switchText.setTextColor(Color.parseColor("#b794f6"))

            } else {

                imageview.setImageBitmap(bitmapImageGray)
                switchText.text = "Disabled"
                switchText.setTextColor(Color.parseColor("#ffffff"))
            }
        }


        fromGalleryButton.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_STORAGE_PERMISSION
                )
            } else {

                selectImage()
            }
        }

        downloadBtn.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_WRITE_EXTERNAL
                )
            } else {

                downloadPhoto()
            }
        }

        filterBtn.setOnClickListener {

            val bmp = bitmapImageColored
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val filePath = tempFileImage(this, bitmapImageColored, "name")

            var intent = Intent(this, FilterActivity::class.java)
            intent.putExtra("sentImageByteArray", filePath)
            intent.putExtra("imageWidth", bitmapImageColored.width)
            intent.putExtra("imageHeight", bitmapImageColored.height)
            startActivityForResult(intent, REQUEST_CODE_FILTER)
        }

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

    fun colorize(url: String) {

        //photoSwitch.isEnabled = false

        Thread {

            val input = JSONObject()
            input.put(
                "image", url
            )

            val client = Algorithmia.client("simfKEVZTqs/8uoB4b3EsOyXJag1")
            val algo = client.algo("deeplearning/ColorfulImageColorization/1.1.14")
            //algo.setTimeout(300L, java.util.concurrent.TimeUnit.SECONDS) //optional

            try {

                val result = algo.pipeJson(input.toString())

                val body = result.asJsonString()
                val gson = GsonBuilder().create()
                val collectionType = object : TypeToken<OutputImage>() {}.type
                val finalUrl = gson.fromJson(body, collectionType) as OutputImage
                val uncutUrl = finalUrl.output

                val text_file = uncutUrl

                try {
                    if (client.file(text_file).exists()) {
                        val localfile = client.file(text_file).bytes
                        val bitmap = BitmapFactory.decodeByteArray(localfile, 0, localfile.size)

                        bitmapImageColored = bitmap

                        runOnUiThread {
                            imageview.setImageBitmap(bitmap)

                            photoSwitch.isChecked = true
                            switchText.text = "Enabled"
                            switchText.setTextColor(Color.parseColor("#b794f6"))

                            image_loader.visibility = View.GONE
                            infoMessages.visibility = View.GONE

                            imageview.visibility = View.VISIBLE
                            switchView.visibility = View.VISIBLE

                            buttons.visibility = View.VISIBLE
                            helpButtons.visibility = View.VISIBLE
                            switchView.visibility = View.VISIBLE


                        }


                    } else {
                        println("Please check that your file exists")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            } catch (exception: Exception) {

                runOnUiThread {

                    image_loader.visibility = View.GONE
                    infoMessages.visibility = View.GONE
                    imageview.visibility = View.VISIBLE
                    switchView.visibility = View.VISIBLE
                    buttons.visibility = View.VISIBLE
                    helpButtons.visibility = View.VISIBLE
                    switchView.visibility = View.VISIBLE


                    //photoSwitch.isEnabled = true


                }

            }

        }.start()
    }


    fun selectImage() {

        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
        }
    }

    fun downloadPhoto() {

        val root = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        ).toString()
        val myDir = File("$root/Colorimetry")
        myDir.mkdirs()
        val generator = Random()

        var n = 10000
        n = generator.nextInt(n)
        val fname = "Image-$n.jpg"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bitmapImageColored.compress(Bitmap.CompressFormat.JPEG, 90, out)
            // sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
            //     Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
            out.flush()
            out.close()

            Log.d("absolute path", file.absolutePath)


            val snackBar = Snackbar.make(main_constraintLayout, "Image saved to gallery", 5000)
                .setAction("OPEN", {
                    openImage(file)
                })

            snackBar.show()

            /*Toast.makeText(
                this,
                "Image saved to: Gallery/Colorimetry/".plus(fname),
                Toast.LENGTH_LONG
            ).show()*/

        } catch (e: Exception) {
            e.printStackTrace()

            Toast.makeText(this, "Failed. Please try again", Toast.LENGTH_LONG).show()

        }

// Tell the media scanner about the new file so that it is
// immediately available to the user.
        MediaScannerConnection.scanFile(
            this, arrayOf(file.toString()), null
        ) { path, uri ->
            Log.i("ExternalStorage", "Scanned $path:")
            Log.i("ExternalStorage", "-> uri=$uri")
        }


    }

    private fun openImage(file: File) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val photoURI = FileProvider.getUriForFile(
            this,
            this.getApplicationContext().getPackageName() + ".provider",
            file
        )

        intent.setDataAndType(photoURI, "image/*")

        startActivity(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.size > 0) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                selectImage()

            } else {

                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL && grantResults.size > 0) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                downloadPhoto()

            } else {

                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getPathFromUri(contentUri: Uri): String {

        var filePath = ""
        val cursor = contentResolver.query(contentUri, null, null, null, null)

        if (cursor == null) {

            filePath = contentUri.path!!
        } else {

            cursor.moveToFirst()
            val index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
            cursor.close()
        }

        return filePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {

                val selectedImageUri = data.data

                if (selectedImageUri != null) {

                    try {


                        val inputStream = contentResolver.openInputStream(selectedImageUri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)

                        Log.d("PRIJE RESCALA", bitmap.width.toString() + " , " + bitmap.height.toString())


                        //val bitmapNew = modifyOrientation(inputStream!!, bitmap)

                        val bitmapNew = rescaleImage(bitmap)

                        Log.d("RESCALED", bitmapNew.width.toString() + " , " + bitmapNew.height.toString())

                        photoFromGallery = encodeImage(bitmapNew)!!

                        bitmapImageGray = bitmapNew

                        val encodedPhoto = "data:image/png;base64,".plus(photoFromGallery)

                        imageview.setImageBitmap(bitmapNew)
                        image_loader.visibility = View.VISIBLE
                        infoMessages.visibility = View.VISIBLE

                        buttons.visibility = View.GONE
                        helpButtons.visibility = View.GONE
                        switchView.visibility = View.GONE

                        colorize(encodedPhoto)

                        val selectedImageFile = File(getPathFromUri(selectedImageUri))


                    } catch (exception: Exception) {

                        Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }

        if (requestCode == REQUEST_CODE_FILTER && resultCode == Activity.RESULT_OK) {

            if (data != null) {

                val dataImage = data.getStringExtra("result")


                //gets the file path
                val finalFilePath = intent.getStringExtra("result")

                //loads the file
                val file = File(dataImage)

                val filteredImageResult = BitmapFactory.decodeFile(file.absolutePath)
                //imageView.setImageBitmap(bitmap)

                bitmapImageColored = filteredImageResult
                //set filtered image on ImageView
                imageview.setImageBitmap(filteredImageResult)
                photoSwitch.isChecked = true

            } else {

                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun rescaleImage(bitmap: Bitmap): Bitmap {

        val density = resources.displayMetrics.density

        val currentBitmapWidth = bitmap.width
        val currentBitmapHeight = bitmap.height

        val ivWidth = photoLL.width
        val ivHeight = photoLL.height

        //var newWidth = ivWidth
        //var newHeight = ivHeight

        var newWidth = ivWidth
        var newHeight = ivHeight - switchViewHeight - 100 * density

        Log.d("Prije width i height", newWidth.toString().plus(" , " + newHeight.toString()))


        if (currentBitmapWidth > currentBitmapHeight) {
            //newWidth = main_constraintLayout.width
            newHeight = round(currentBitmapHeight.toDouble() * newWidth/currentBitmapWidth).toFloat()

        } else if (currentBitmapHeight > currentBitmapWidth) {
            //newHeight = main_constraintLayout.height - buttonsHeight - helpButtonsHeight - switchViewHeight
            newWidth = round(currentBitmapWidth.toDouble() * newHeight / currentBitmapHeight).toInt()

        } else {

            Log.d("ISTO", "ISTO")

            return bitmap
        }

        Log.d("Poslije width i height", newWidth.toString().plus(" , " + newHeight.toString()))

        Log.d("DENSITY", resources.displayMetrics.density.toString())



        val newBitmap = Bitmap.createScaledBitmap(bitmap, newWidth.toInt(), newHeight.toInt(), true)

        return newBitmap
    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    fun modifyOrientation(inputStream: InputStream, bitmap: Bitmap): Bitmap {

        val exifInterface = ExifInterface(inputStream)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            -1
        )

        var finalBitmap = bitmap

        Log.d("orientationnn", orientation.toString())


        Log.d("preee", finalBitmap.toString())

        if (orientation != -1) {

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 ->
                    finalBitmap = rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 ->
                    finalBitmap = rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 ->
                    finalBitmap = rotateImage(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL ->
                    finalBitmap = flip(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL ->
                    finalBitmap = flip(bitmap, false, true)
                ExifInterface.ORIENTATION_NORMAL ->
                    finalBitmap = rotateImage(bitmap, 0f)
            }

            Log.d("ORIJENTACIJA", orientation.toString())

        }


        Log.d("posleeee", finalBitmap.toString())


        return finalBitmap
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val mat = Matrix()
        mat.postRotate(angle)
        val bitmap =
            Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), mat, true)

        Log.d("ROTATEE", bitmap.toString())


        return bitmap
    }


    fun flip(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        Log.d("FLIPP", "FLIPP")

        val matrix = Matrix()
        matrix.preScale((if (horizontal) -1 else 1).toFloat(), (if (vertical) -1 else 1).toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

}

