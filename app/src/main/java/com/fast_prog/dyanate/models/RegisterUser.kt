package com.fast_prog.dyanate.models

import java.io.Serializable

/**
 * Created by sarathk on 11/9/16.
 */

class RegisterUser : Serializable {

    var name: String? = null
    var mobile: String? = null
    var mail: String? = null
    var address: String? = null
    var username: String? = null
    var password: String? = null
    var latitude: String? = null
    var longitude: String? = null
    var loginMethod: String? = null
}
