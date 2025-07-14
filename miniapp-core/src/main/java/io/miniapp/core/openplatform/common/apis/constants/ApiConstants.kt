package io.miniapp.core.openplatform.common.apis.constants
internal object ApiConstants {

    // verifier
    const val URI_REQUEST_VERIFIER = "/api/v1/users/auth"

    // bot extend info
    const val URI_REQUEST_BOT_INFO = "/api/v1/bots/{idOrName}"

    // get mini-app info by name
    const val URI_REQUEST_APP_INFO_BY_NAME = "/api/v1/bots/{idOrName}/miniapps/by-identifier/{appName}"

    // get mini-app detail
    const val URI_MINI_APP_INFO_BY_ID = "/api/v1/miniapps/{id}"

    // batch mini-app detail
    const val URI_BATCH_MINI_APP = "/api/v1/miniapps/batch-get"

    // get mini-app launch info
    const val URI_REQUEST_LAUNCH_URL = "/api/v1/miniapps/launch"

    // invoke custom methods
    const val URI_INVOKE_CUSTOM_METHODS = "/api/v1/custom_methods/invoke"

    // get d-app detail
    const val URI_D_APP_INFO_BY_ID = "/api/v1/dapps/{id}"

    // get bot menus data
    const val URI_BOT_MENUS = "/api/v1/menus/batch-get"

    // get d-app launch url
    const val URI_D_APP_LAUNCH = "/api/v1/webpage/open"

    // get ai complete data
    const val URI_AI_COMPLETION = "/api/v1/ai/completion"

    //inline button callback
    const val URI_INLINE_CALLBACK = "/api/v1/callback/query"
}