package com.fast_prog.dyanate.models

import java.io.Serializable

/**
 * Created by sarathk on 2/13/17.
 */

class Drivers : Serializable {

    var driverID: String? = null
    var driverMobile: String? = null
    var driverName: String? = null
    var dmMobNumber: String? = null
    var dmLatitude: String? = null
    var dmLongitude: String? = null
//    var tdmLatitude: String? = null
//    var tdmLongitude: String? = null
    var distanceKm  = ""
        var tripRate = ""
    var tripIsNegotiable = false
    var tripDate = ""
//    var isAccepted: Boolean = false
//    var isRejected: Boolean = false
}
