package com.example.in2de.photobomber

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.app.NotificationCompat
import android.os.Build
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat.PRIORITY_DEFAULT
import android.support.v4.app.NotificationManagerCompat
import android.app.PendingIntent
import android.os.Binder
import com.example.in2de.photobomber.MainActivity

private const val NOTIFICATION_ID = 101

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class BackgroundService : Service() {

    private val binder = MyBinder()
    private var isRunning = false

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private fun startForegroundNotification() {

    }

    fun stop() {
        Log.d("t777", "stopbind")
        isRunning = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val myPreferences = MyPreferences(this)
        Log.d("t5533", myPreferences.getIsCopying().toString())
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                } else {
                    ""
                }
        val intent2 = Intent(this, MainActivity::class.java)
        intent2.putExtra("com.example.in2de.photobomber.notifyId", NOTIFICATION_ID)
        intent2.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = PendingIntent.getActivity(this, 0, intent2,
                PendingIntent.FLAG_UPDATE_CURRENT)


        val nmc = NotificationManagerCompat.from(this)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder
                .setSmallIcon(R.drawable.ic_bomb)
                .setPriority(PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Description")
                .setProgress(100, 0, false)
                //.addAction(R.drawable.ic_bomb, "Action Button", pIntent)
                .setContentIntent(pIntent)
                .build()

        startForeground(NOTIFICATION_ID, notification)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val safeCopyEnabled = prefs.getBoolean("safecopy_switch", true)

        val bundle = intent?.extras
        if (bundle != null) {
            val method = bundle.get("method") //todo change if below to when
            if (method.equals("copy")) {
                val imageUri = bundle.get("imageuri")
                val numOfCopies = bundle.get("numoftimes")
                val shouldDelete = bundle.get("shoulddelete")
                val quality = bundle.get("quality")
                val uris = ArrayList<Uri>()
                val files = ArrayList<File>()


                val offset = findStartingIndex(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + getString(R.string.meme_directory))
                isRunning = true
                val r = Runnable {
                    Log.d("t64", "top " + imageUri.toString())
                    val ish = contentResolver.openInputStream(Uri.parse(imageUri.toString()))
                    val bitmap = BitmapFactory.decodeStream(ish)
                    val bitmapHardCopy = bitmap
                    ish!!.close()

                    val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + getString(R.string.meme_directory)
                    val sd = File(filePath)
                    if (!sd.exists()) {
                        sd.mkdirs()
                    }
                    for (i in (1 + offset)..(Integer.parseInt(numOfCopies.toString())) + offset) {
                        if (!myPreferences.getIsCopying()) {
                            // if operation stopped prematurely
                            notificationBuilder.setContentText("Canceled after copying " + (i - offset - 1) + " out of $numOfCopies pictures")
                            notificationBuilder.setProgress(0,0,false)
                            nmc.notify(101, notificationBuilder.build())
                            break
                        }
                        val filename = getString(R.string.meme_prefix) + i + ".jpg"
                        val dest = File(sd, filename)
                        if(i==(offset+1)) //myPreferences.setImageUri(dest.toString())
                        //todo above needs to be content scheme?
                            Log.d("t799", dest.toString())
                        try {
                            val out = FileOutputStream(dest)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, Integer.parseInt(quality.toString()), out)

                            out.flush()
                            out.close()

                            uris.add(Uri.parse(dest.toString()))
                            files.add(dest)

                            Log.d("t799", dest.toString())
                            //Log.d("t567", Uri.fromFile(dest).toString())
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        //MediaScannerConnection.scanFile(this@MyIntentService, arrayOf(dest.toString()), null) { s, uri -> }

                        val mediaScannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        val fileContentUri = Uri.fromFile(dest)
                        mediaScannerIntent.data = fileContentUri
                        this.sendBroadcast(mediaScannerIntent)

                        startBroadcastIntent(i - offset, Integer.parseInt(numOfCopies.toString()),
                                offset, dest.length(), imageUri.toString())
                        val progress = ((i - offset) * 100) / Integer.parseInt(numOfCopies.toString())
                        notificationBuilder.setProgress(100, progress, false)
                        notificationBuilder.setContentText("Copied " + (i - offset) + " out of $numOfCopies pictures")
                        nmc.notify(101, notificationBuilder.build())

                        myPreferences.setProgress(progress)
                        myPreferences.setProgressBarMessage("Copied " + (i - offset) + "/$numOfCopies pictures")


                        if (shouldDelete == true) { // not implemented yet.
                            for (i in uris) {
                                val fileToDelete = File(i.path)
                                if (fileToDelete.exists()) {
                                    if (fileToDelete.delete()) {
                                        Log.d("t88", "deleted")
                                    } else {
                                        Log.d("t88", "not deleted")
                                    }
                                }
                            }
                        }
                    }
                    if (myPreferences.getIsCopying()) {
                        // finished successfully
                        notificationBuilder
                                .setContentText("$numOfCopies pictures successfully saved!")
                                .setProgress(0,0,false).build()
                        nmc.notify(101, notificationBuilder.build())
                    }


                    stopForeground(false)

                    isRunning = false
                    myPreferences.setIsCopying(false)
                }
                val t = Thread(r)
                t.start()
            }
        }
        return START_STICKY
    }

    private fun saveGui(){

    }

    private fun startBroadcastIntent(done: Int, total: Int, offset: Int, bytesForOnePicture: Long, imageUri: String) {
        val intent = Intent("myBroadcast")
        intent.putExtra("message", "Copied $done/$total pictures")
        intent.putExtra("progress", ((done * 100) / total).toString())
        intent.putExtra("startnum", (offset + 1).toString())
        intent.putExtra("endnum", (offset + total).toString())
        intent.putExtra("totalbytes", (bytesForOnePicture * total).toString())
        intent.putExtra("imageuri", imageUri)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun findStartingIndex(dirPath: String): Int {
        var count = 0
        val f = File(dirPath)
        val files = f.listFiles()
        if (files != null)
            for (i in files!!.indices) {
                count++
                val file = files!![i]
                if (file.isDirectory) {
                    findStartingIndex(file.absolutePath)
                }
            }
        return count
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "my_service"
        val channelName = "My Background Service"
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    // Might put something like this in background service at some point to dynamically calculate
    // space needed for copy operation
    /*private fun saveATempFileAndPopulateTextView(){

        val ish = contentResolver.openInputStream(Uri.parse(receivedUri.toString()))
        val bitmap = BitmapFactory.decodeStream(ish)
        ish!!.close()

        val filePath = Environment.getExternalStorageDirectory().absolutePath + getString(R.string.meme_directory)
        val sd = File(filePath)
        sd.mkdirs()

        val filePathTemp = Environment.getExternalStorageDirectory().absolutePath + getString(R.string.meme_directory) + "/.temp"
        val sdTemp = File(filePathTemp)
        sdTemp.mkdirs()

        val filename = "tempphotobomber.jpg"
        val dest = File(sdTemp, filename)
        try {
            val out = FileOutputStream(dest)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (et1.text.equals("")){
            textViewStorage.append("No space required")
        }else{
            textViewStorage.append(StorageUtils().formatSize((dest.length()
                    *Integer.parseInt(et1.text.toString()))) + " needed to do this copy")
        }
    }*/

    inner class MyBinder : Binder() {
        fun getService(): BackgroundService {
            return this@BackgroundService
        }
    }

}