package com.fast_prog.dyanate.models

import android.graphics.Bitmap
import java.io.Serializable

/**
 * Created by sarathk on 9/30/17.
 */

class Docs : Serializable {

    var id: Int = 0
    var docName: String
    var docPath: String
    var docbase64: String
    var docBm: Bitmap? = null

    init {
        this.id = -1
        this.docName = ""
        this.docPath = ""
        this.docbase64 = ""
        this.docBm = null
    }

}
