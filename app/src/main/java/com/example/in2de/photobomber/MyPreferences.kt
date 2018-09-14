package com.example.in2de.photobomber

import android.content.Context
import android.net.Uri
import com.example.in2de.photobomber.R.id.cbshare

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class MyPreferences(context:Context){

    val PREFERENCE_IMAGE_URI = "imageuri"
    val PREFERENCE_IS_COPYING = "isrunning"
    val PREFERENCE_IS_SHARE_CHECKED = "issharedchecked"
    val PREFERENCE_PROGRESS = "progress"
    val PREFERENCE_PROGRESS_MESSAGE = "progressmessage"


    val preferences = context.getSharedPreferences("com.example.in2de.photobomber.preferences",
            Context.MODE_PRIVATE)

    fun getImageUri():String{
        return preferences.getString(PREFERENCE_IMAGE_URI, "null")
    }

    fun setImageUri(imageUri:String){
        val editor = preferences.edit()
        editor.putString(PREFERENCE_IMAGE_URI, imageUri)
        editor.apply()
    }

    fun getIsCopying():Boolean{
        return preferences.getBoolean(PREFERENCE_IS_COPYING, false)
    }

    fun setIsCopying(isRunning:Boolean){
        val editor = preferences.edit()
        editor.putBoolean(PREFERENCE_IS_COPYING, isRunning)
        editor.apply()
    }

    fun getProgress():Int{
        return preferences.getInt(PREFERENCE_PROGRESS, 0)
    }

    fun setProgress(progress:Int){
        val editor = preferences.edit()
        editor.putInt(PREFERENCE_PROGRESS, progress)
        editor.apply()
    }

    fun getProgressBarMessage():String{
        return preferences.getString(PREFERENCE_PROGRESS_MESSAGE, "")
    }

    fun setProgressBarMessage(message:String){
        val editor = preferences.edit()
        editor.putString(PREFERENCE_PROGRESS_MESSAGE, message)
        editor.apply()
    }

    fun doneCopyingAndOnDestroy(){
        setImageUri("null")
    }

    fun getIsShareCbChecked():Boolean{
        return preferences.getBoolean(PREFERENCE_IS_SHARE_CHECKED, true)
    }

    fun setIsShareCbChecked(isShareChecked:Boolean){
        val editor = preferences.edit()
        editor.putBoolean(PREFERENCE_IS_SHARE_CHECKED, isShareChecked)
        editor.apply()
    }

}