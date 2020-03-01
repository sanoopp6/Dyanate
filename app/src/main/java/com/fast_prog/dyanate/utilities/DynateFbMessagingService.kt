package com.fast_prog.dyanate.utilities

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.fast_prog.dyanate.models.Order
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Created by sarathk on 12/13/16.
 */

class DynateFbMessagingService : FirebaseMessagingService() {

    internal lateinit var sharedPreferences: SharedPreferences

    internal val TAG = "FirebaseMessaging"

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {

        sharedPreferences = this.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

        Log.e(TAG, "From: " + remoteMessage!!.from!!)

        if (remoteMessage.data.isNotEmpty()) {
            val mode = remoteMessage.data["MODE"]
            val noOfDrivers = remoteMessage.data["TripMNoOfDrivers"]?.toInt()

            val order = Order()
            order.tripId = remoteMessage.data["TripMID"]
            order.tripFromLat = remoteMessage.data["TripMFromLat"]
            order.tripFromLng = remoteMessage.data["TripMFromLng"]
            order.tripToLat = remoteMessage.data["TripMToLat"]
            order.tripToLng = remoteMessage.data["TripMToLng"]

            if (mode != null) {
                sendNotification(mode, noOfDrivers!!, order)
            }
        }

        if (remoteMessage.notification != null) {
            Log.e(TAG, "From: " + remoteMessage.notification)

            Log.e(TAG, "title = ${remoteMessage.notification!!.title?.trim()}")
            Log.e(TAG, "body = ${remoteMessage.notification!!.body?.trim()}")
        }
    }

    private fun sendNotification(mode: String, noOfDrivers: Int, order: Order) {

//        val preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
//        var notificationID = preferences.getInt(Constants.PREFS_NOTI_ID, 0)
//        notificationID = notificationID + 1
//
//        var lang = "en"
//
//        if (preferences.getString(Constants.PREFS_LANG, "")!!.equals("ar", ignoreCase = true)) {
//            lang = "ar"
//        }
//
//        val locale = Locale(lang)
//        Locale.setDefault(locale)
//        val confg = Configuration()
//        confg.locale = locale
//        baseContext.resources.updateConfiguration(confg, baseContext.resources.displayMetrics)
//
//        val editor = preferences.edit()
//        editor.putInt(Constants.PREFS_NOTI_ID, notificationID)
//        editor.putString(Constants.PREFS_LANG, lang)
//        editor.commit()
//
//        val notificationManager = NotificationManagerCompat.from(applicationContext)
//
//        //val intent = Intent(applicationContext, ShowDriversInMapActivity::class.java)
//        //intent.putExtra("order", order)
//        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
//        //val pendingIntent = PendingIntent.getActivity(applicationContext, Integer.parseInt(order.tripId), intent, PendingIntent.FLAG_ONE_SHOT)
//
//        val defaultSoundUri: Uri
//        val notificationBuilder: NotificationCompat.Builder
//
//        if (mode.equals("S", ignoreCase = true)) {
//            if (java.lang.Double.parseDouble(order.tripToLat) == 0.0 && java.lang.Double.parseDouble(order.tripToLng) == 0.0) {
//                defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sender_updated_waiting_receiver)
//
//                notificationBuilder = NotificationCompat.Builder(applicationContext)
//                        .setSmallIcon(R.drawable.logo_1)
//                        .setContentTitle(resources.getString(R.string.app_name))
//                        .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.SenderUpdatedWaitingReceiver))
//                        .setAutoCancel(true)
//                        .setTicker(resources.getString(R.string.app_name))
//                        .setSound(defaultSoundUri)
//                        .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//            } else {
//                if (noOfDrivers > 0) {
//                    defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sender_receiver_updated)
//
//                    notificationBuilder = NotificationCompat.Builder(applicationContext)
//                            .setSmallIcon(R.drawable.logo_1)
//                            .setContentTitle(resources.getString(R.string.app_name))
//                            .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.SenderReceiverUpdated))
//                            .setAutoCancel(false)
//                            .setTicker(resources.getString(R.string.app_name))
//                            .setSound(defaultSoundUri)
//                            //.setContentIntent(pendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//                } else {
//                    defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sender_receiver_updated)
//
//                    notificationBuilder = NotificationCompat.Builder(applicationContext)
//                            .setSmallIcon(R.drawable.logo_1)
//                            .setContentTitle(resources.getString(R.string.app_name))
//                            .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.SenderReceiverUpdated))
//                            .setAutoCancel(false)
//                            .setTicker(resources.getString(R.string.app_name))
//                            .setSound(defaultSoundUri)
//                            //.setContentIntent(pendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_HIGH)
//                }
//            }
//
//        } else {
//            if (order.tripFromLat!!.toDouble() == 0.0 && order.tripFromLng!!.toDouble() == 0.0) {
//                defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.receiver_updated_waiting_sender)
//
//                notificationBuilder = NotificationCompat.Builder(applicationContext)
//                        .setSmallIcon(R.drawable.logo_1)
//                        .setContentTitle(resources.getString(R.string.app_name))
//                        .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.ReceiverUpdatedWaitingSender))
//                        .setAutoCancel(true)
//                        .setTicker(resources.getString(R.string.app_name))
//                        .setSound(defaultSoundUri)
//                        .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//            } else {
//                if (noOfDrivers > 0) {
//                    defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sender_receiver_updated)
//
//                    notificationBuilder = NotificationCompat.Builder(applicationContext)
//                            .setSmallIcon(R.drawable.logo_1)
//                            .setContentTitle(resources.getString(R.string.app_name))
//                            .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.SenderReceiverUpdated))
//                            .setAutoCancel(false)
//                            .setTicker(resources.getString(R.string.app_name))
//                            .setSound(defaultSoundUri)
//                            //.setContentIntent(pendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_HIGH)
//
//                } else {
//                    defaultSoundUri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sender_receiver_updated)
//
//                    notificationBuilder = NotificationCompat.Builder(applicationContext)
//                            .setSmallIcon(R.drawable.logo_1)
//                            .setContentTitle(resources.getString(R.string.app_name))
//                            .setContentText(resources.getString(R.string.OrderNo) + order.tripId + "@" + resources.getString(R.string.SenderReceiverUpdated))
//                            .setAutoCancel(false)
//                            .setTicker(resources.getString(R.string.app_name))
//                            .setSound(defaultSoundUri)
//                            //.setContentIntent(pendingIntent)
//                            .setPriority(NotificationCompat.PRIORITY_HIGH)
//                }
//            }
//        }
//
//        notificationManager.notify(notificationID, notificationBuilder.build())
    }

}
