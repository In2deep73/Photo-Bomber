package com.example.in2de.photobomber

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.view.Menu
import android.view.MenuItem
import java.io.File
import android.util.Log
import android.app.NotificationManager
import android.os.IBinder
import android.text.InputType


private const val PERMISSION_REQUEST = 10
private const val PICK_IMAGE = 100

class MainActivity : AppCompatActivity() {

    var myService: BackgroundService? = null
    var isBound = false

    private fun restoreGuiFromSharedPrefs(){


    }
    //var notificationmanager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private lateinit var copyIntent:Intent
    private lateinit var receivedUri: Uri
    private lateinit var uriArray: ArrayList<Uri>
    private var permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private lateinit var ctx: Context
    private val mLocalBroadcast = object : BroadcastReceiver() {
        var counter = 0
        override fun onReceive(context: Context, intent: Intent) {
            //stopService(Intent(context,BackgroundService::class.java))
            counter++
            val message = intent.getStringExtra("message")
            val progress = Integer.parseInt(intent.getStringExtra("progress"))
            val startingNum = Integer.parseInt(intent.getStringExtra("startnum"))
            val endingNum = Integer.parseInt(intent.getStringExtra("endnum"))
            val totalBytesOfOperation = intent.getStringExtra("totalbytes")
            textView.visibility = View.VISIBLE
            textView.text = message
            if (counter == 1) {
                storageBar.secondaryProgress = StorageUtils().getPercentageUtilizationAfterOperation(totalBytesOfOperation.toLong())
            }
            textViewStorage.setText(StorageUtils().formatSize(totalBytesOfOperation.toLong()))
            textViewStorage.append(" out of ")
            textViewStorage.append(StorageUtils().getAvailableExternalMemorySize())
            textViewStorage.append(" will be used")

            setProgressBar(progress)
            val myPreferences = MyPreferences(applicationContext)
            if (progress==100) {
                configureStorageBar()
                //setProgressBar(0)

                counter = 0
                if (cbshare.isChecked) {
                    startShareIntent(startingNum, endingNum)
                }
                toggleAndLockGui(true)
            }
        }
    }

/*    private val myConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            val binder = service as BackgroundService.MyBinder
            myService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val myPreferences = MyPreferences(this)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        copyIntent = Intent(baseContext, BackgroundService::class.java)
        //bindService(copyIntent, myConnection, Context.BIND_AUTO_CREATE)
        //todo other options here
        //val whatsapp mode
        ctx = this
        permissionsCheck(ctx, permissions)
        setContentView(R.layout.activity_main)
        toggleAndLockGui(!myPreferences.getIsCopying())
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.mipmap.ic_launcher)
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalBroadcast,
                IntentFilter("myBroadcast"))
        configureListeners()
        configureStorageBar()
        if(myPreferences.getProgress()>0){
            progressBar.visibility = View.VISIBLE
            setProgressBar(myPreferences.getProgress())
            textView.text = myPreferences.getProgressBarMessage()
        }

        if (tryToPopulateImageView()) {
            Log.d("t545", "try to pop")
            //todo this thinks its being opened via intent when it shouldnt
            // app opened via share intent - reset prefs
        }else{
            if(myPreferences.getImageUri()!="null"){
                receivedUri = Uri.parse(myPreferences.getImageUri())
                tryToPopulateImageView()
            }

        }
    }

    override fun onResume() {
        super.onResume()
        tryToPopulateImageView()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("t5533","ondest")
        //if (myConnection != null) {
        //unbindService(myConnection)
        //}
        val myPreferences = MyPreferences(this)
        if(!myPreferences.getIsCopying()){
            //todo restore gui to defaults
            myPreferences.setImageUri("null")
            myPreferences.setIsShareCbChecked(true)
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalBroadcast)
    }

    private fun permissionsCheck(ctx: Context, permissionsArray: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (i in permissionsArray.indices) {
                if (checkCallingOrSelfPermission(permissionsArray[i]) == PackageManager.PERMISSION_DENIED)
                    requestPermissions(permissions, PERMISSION_REQUEST)
            }
        }
    }

    private fun tryToPopulateImageView(): Boolean {
        val myPreferences = MyPreferences(this)
        if (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) != null) {
            btnbrowse.visibility = View.INVISIBLE
            receivedUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            picture.setImageURI(receivedUri)
            picture.visibility = View.VISIBLE
            sharetext.visibility = View.VISIBLE
            myPreferences.setIsShareCbChecked(cbshare.isChecked)
            myPreferences.setImageUri(receivedUri.toString())
            intent.removeExtra(Intent.EXTRA_STREAM)
            // todo - app thinks its a share intent even when its not
            return true
        }
        try {
            picture.setImageURI(receivedUri)
            btnbrowse.visibility = View.INVISIBLE
            picture.visibility = View.VISIBLE
            sharetext.visibility = View.VISIBLE
            myPreferences.setImageUri(receivedUri.toString())
            return true
        } catch (e: UninitializedPropertyAccessException) { }
        btnbrowse.visibility = View.VISIBLE
        picture.visibility = View.INVISIBLE
        setProgressBar(0)
        textView.text = ""
        progressBar.visibility = View.INVISIBLE
        myPreferences.setProgressBarMessage("")
        myPreferences.setProgress(0)
        return false
    }

    private fun configureStorageBar() {
        val totalMemAvail = StorageUtils().getAvailableExternalMemorySizeInMiB()
        val totalMem = StorageUtils().getTotalExternalMemorySizeInMiB()
        val progress = 100 - (totalMemAvail.toDouble() / totalMem.toDouble()) * 100
        Log.d("t88", "avail $totalMemAvail tot $totalMem progress $progress")
        storageBar.setProgress(progress.toInt())
        textViewStorageBar.setText(StorageUtils().getAvailableExternalMemorySize())
        textViewStorageBar.append("/")
        textViewStorageBar.append(StorageUtils().getTotalExternalMemorySize())
        textViewStorageBar.append(" free")
        //textViewStorage.setText(StorageUtils().formatSize(1048576 * totalMemAvail.toLong()) + " free.")
        if (::receivedUri.isInitialized) {
            //saveATempFileAndPopulateTextView()
        }
    }

    private fun configureListeners() {
        button1.setOnClickListener {
            val myPreferences = MyPreferences(this)
            if (myPreferences.getIsCopying()) {
                myPreferences.setIsCopying(false)
                toggleAndLockGui(true)
                Log.d("t64", "stop")
            } else if (btnbrowse.visibility == View.INVISIBLE && et1.length() > 0) {
                val myPreferences = MyPreferences(this)
                myPreferences.setIsCopying(true)
                myPreferences.setIsShareCbChecked(cbshare.isChecked)
                myPreferences.setImageUri(receivedUri.toString())
                startIntentToBackgroundService("copy", receivedUri, Integer.parseInt(et1.text.toString()))
                Log.d("t999", receivedUri.toString())
            }
        }
        btnbrowse.setOnClickListener {
            openGallery()
        }
        picture.setOnClickListener { openGallery() }
    }

    private fun openGallery() {
        val gallery = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE) {
            if (data != null) {
                receivedUri = data.data
                val myPreferences = MyPreferences(this)
                setProgressBar(0)
                textView.text = ""
                progressBar.visibility = View.INVISIBLE
                myPreferences.setProgressBarMessage("")
                myPreferences.setProgress(0)
            }
        }
    }

    private fun startShareIntent(staringNum: Int, endingNum: Int) {
        val uris = ArrayList<Uri>()
        for (i in staringNum..endingNum) {
            val FilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + getString(R.string.meme_directory)
            val sd = File(FilePath)
            val filename = getString(R.string.meme_prefix) + i + ".jpg"
            val destest = FileProvider.getUriForFile(applicationContext, BuildConfig.APPLICATION_ID + ".provider",File(sd, filename))
            uris.add(destest)
        }
        this.uriArray = uris
        val share = Intent(Intent.ACTION_SEND_MULTIPLE)
        share.type = "image/*"
        val shareText = sharetext.text.toString()
        if(shareText!=""){
            share.putExtra(Intent.EXTRA_TEXT, shareText)
        }
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        //share.setPackage("com.whatsapp")
        this.startActivity(share)
    }

    private fun startIntentToBackgroundService(method: String, imageUri: Uri, numOfCopies: Int) {
        if (settingsWillAllowThisOperation(numOfCopies)) {
            toggleAndLockGui(false)
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val quality = prefs.getString("quality", "100")
            copyIntent.putExtra("method", method)
            copyIntent.putExtra("imageuri", imageUri)
            copyIntent.putExtra("numoftimes", numOfCopies)
            //copyIntent.putExtra("shoulddelete", cbdelete.isChecked)
            copyIntent.putExtra("quality", quality)
            startService(copyIntent)
        }
    }

    private fun settingsWillAllowThisOperation(numOfCopies: Int): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val maxPicsToDownload = prefs.getString("maxcopies", "100")
        val quality = prefs.getString("quality", "100")
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        if (Integer.parseInt(quality) > 100 || Integer.parseInt(quality) <= 0) {
            AlertDialog.Builder(this).setTitle(getString(R.string.app_name))
                    .setMessage("Please go to settings and change image quality to a value between" +
                            " 1 and 100").setNegativeButton(getString(R.string.ok))
                    { dialogInterface, _ -> dialogInterface.dismiss() }.setPositiveButton("Go to settings")
                    { dialogInterface, _ -> startActivity(settingsIntent) }.show()
            return false
        }
        if (numOfCopies <= Integer.parseInt(maxPicsToDownload)) {
            return true
        }
        val alertDialog = AlertDialog.Builder(this).setTitle(getString(R.string.app_name))
                .setMessage("You tried to make $numOfCopies copies and you have the max number of " +
                        "copies set to $maxPicsToDownload.").setNegativeButton(getString(R.string.ok))
                { dialogInterface, _ -> dialogInterface.dismiss() }.setPositiveButton("Go to settings")
                { dialogInterface, _ -> startActivity(settingsIntent) }.show()
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.settings_menu_option -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.about_menu_option -> {
                val alertDialog = AlertDialog.Builder(this).setTitle(getString(R.string.app_name))
                        .setMessage(getString(R.string.version)).setNegativeButton(getString(R.string.ok))
                        { dialogInterface, _ -> dialogInterface.dismiss() }.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleAndLockGui(bool: Boolean) {
        //cbdelete.isClickable = bool
        cbshare.isClickable = bool
        picture.isClickable = bool
        if (!bool) {
            //button1.setText(getString(R.string.stop_button))
            button1.setBackgroundResource(R.drawable.stopbuttonpng)
            progressBar.visibility = View.VISIBLE
            Log.d("t554",sharetext.inputType.toString())
            sharetext.inputType = 0
        } else {
            //button1.setText(getString(R.string.copy_button))
            button1.setBackgroundResource(R.drawable.buttoncopypng)
            //progressBar.visibility = View.INVISIBLE
            sharetext.inputType=131073
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putBoolean("checkbox_status", cbshare.isClickable)
        outState?.putString("text_view", textView.text.toString())
        if (::receivedUri.isInitialized) {
            outState?.putString("receiveduri", receivedUri.toString())
        }
    }

    private fun setProgressBar(progress: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            progressBar.setProgress(progress, true)
        } else {
            progressBar.progress = progress
        }
    }
}