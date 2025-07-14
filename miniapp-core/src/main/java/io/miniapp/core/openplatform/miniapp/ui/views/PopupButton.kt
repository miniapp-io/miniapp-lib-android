package io.miniapp.core.openplatform.miniapp.ui.views

import org.json.JSONObject

internal class PopupButton(obj: JSONObject) {

    var id: String
    var text: String? = null
    var textColorKey:String = ""

    init {
        id = obj.getString("id")
        val type = obj.getString("type")
        var textRequired = false
        when (type) {
            "default" -> {
                textRequired = true
            }

            "ok" -> {
                text = "Ok"
            }

            "close" -> {
                text = "Close"
            }

            "cancel" -> {
                text = "Cancel"
            }

            "destructive" -> {
                textRequired = true
                textColorKey = "text_err_color"
            }

            else -> {
                textRequired = true
            }
        }
        if (textRequired) {
            text = obj.getString("text")
        }
    }
}
