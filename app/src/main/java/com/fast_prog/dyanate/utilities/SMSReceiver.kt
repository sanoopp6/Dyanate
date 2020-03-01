package com.fast_prog.dyanate.utilities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.fast_prog.dyanate.views.VerifyOTPActivity

/**
 * Created by Ravi on 09/07/15.
 */
class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val bundle = intent.extras
        try {
            if (bundle != null) {
                val pdusObj = bundle.get("pdus") as Array<*>

                for (aPdusObj in pdusObj) {
                    val currentMessage = SmsMessage.createFromPdu(aPdusObj as ByteArray)
                    val senderAddress = currentMessage.originatingAddress
                    val message = currentMessage.displayMessageBody

                    // if the SMS is not from our gateway, ignore the message
                    if (!senderAddress!!.toLowerCase().contains(Constants.SMS_ORIGIN.toLowerCase())) {
                        return
                    }

                    // verification code from sms
                    val verificationCode = getVerificationCode(message)
                    VerifyOTPActivity.updateData(verificationCode!!)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Getting the OTP from sms message body
     * ':' is the separator of OTP from the message
     *
     * @param message
     * @return
     */
    private fun getVerificationCode(message: String): String? {
        var code: String? = null
        val index = message.indexOf(Constants.OTP_DELIMITER)

        if (index != -1) {
            val start = index + 1
            code = message.substring(start, message.length)
            return code.trim()
        }

        return null
    }
}
