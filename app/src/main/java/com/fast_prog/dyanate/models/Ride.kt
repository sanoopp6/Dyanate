package com.fast_prog.dyanate.models

import android.media.Image
import java.io.File
import java.io.Serializable


class Ride : Serializable {
    var tripId = ""
    var vehicleSizeId: String? = null
    var vehicleSizeName: String = ""
    var pickUpLatitude: String? = null
    var pickUpLongitude: String? = null
    var pickUpLocation: String? = null
    var dropOffLatitude: String? = null
    var dropOffLongitude: String? = null
    var dropOffLocation: String? = null
    //var isFromSelf: Boolean? = null
    var fromName: String = ""
    var fromMobile: String = ""
    //var isToSelf: Boolean? = null
    var toName: String = ""
    var toMobile: String = ""
//    var subject: String = ""
    var shipment: String = ""
    var shipmentTypeID: String = ""
    var shipmentTypeName: String = ""
    var date: String = ""
    var hijriDate: String = ""
    var time: String = ""
    //var isMessage: Boolean? = null
    var distanceStr: String? = null
    var requiredPersons = "0"
    var requiredUnpackAndInstall = "0"
    var loadingCount = "0"
    var unloadingCount = "0"
    var buildingLevel = ""
    var pickUpLocationNameArabic = ""
    var dropOffLocationNameArabic = ""
    var storeName = ""
    var storeInvoiceName = ""
    var invoiceImage: File? = null

    companion object { lateinit var instance: Ride }

    init { instance = this }
}
