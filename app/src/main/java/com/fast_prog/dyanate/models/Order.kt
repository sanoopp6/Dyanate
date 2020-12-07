package com.fast_prog.dyanate.models

import java.io.Serializable

/**
 * Created by sarathk on 1/9/17.
 */

class Order : Serializable {

    var tripId: String? = null
    var tripNo: String? = null
    var tripFromAddress: String? = null
    var tripFromLat: String? = null
    var tripFromLng: String? = null

    var tripFromSelf: Boolean = false

    var tripFromName: String? = null
    var tripFromMob: String? = null
    var tripToAddress: String? = null
    var tripToLat: String? = null
    var tripToLng: String? = null

    var tripToSelf: Boolean = false

    var tripToName: String? = null
    var tripToMob: String? = null
    var vehicleModel: String? = null
    var vehicleType: String? = null
    var scheduleDate: String? = null
    var scheduleTime: String? = null
    var userName: String? = null
    var userMobile: String? = null
    var tripStatus: String? = null
    var tripFilter: String? = null
    var tripSubject: String? = null
    var tripNotes: String? = null
    var vehicleImage: String? = null
    var tripdId: String? = null
    var tripDNo: String? = null
    var tripDStatus: String? = null
    var tripDDmId: String? = null
    var tripDRate: String? = null
    var tripDIsNegotiable: String? = null
    var tripDDateTime: String? = null
    var tripDFilterName: String? = null
    var driverName: String = ""
    var driverMobileNumber: String = ""
    var driverID: String = ""
    var estimatedPrice: String = "0"
    var distanceKm: String? = null
    var vehicleSizeID = ""
    var workersRequired = "0"
    var isUnpackInstallRequired = ""
    var currentStatusText = ""
    var chatEnabled = false
    var shipmentId = ""
    var loadingCount = "0"
    var unloadingCount = "0"
    var is_loading_unloading_calculation = "0"
}
