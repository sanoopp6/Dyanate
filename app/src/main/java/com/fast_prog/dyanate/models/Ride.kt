package com.fast_prog.dyanate.models

import java.io.Serializable

/**
 * Created by sarathk on 1/4/17.
 */

class Ride : Serializable {

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

    companion object { lateinit var instance: Ride }

    init { instance = this }
}
