package io.miniapp.core.openplatform.ai

import androidx.annotation.Keep

@Keep
data class CompletionRequest(
    val promptId: Int,
    val args: Map<String, String>
) {

    class Builder {
        private var promptId: Int = 0
        private var args: MutableMap<String, String> = mutableMapOf()

        fun promptId(promptId: Int): Builder {
            this.promptId = promptId
            return this
        }

        fun args(key: String, value: String): Builder {
            args[key] = value
            return this
        }

        fun build(): CompletionRequest {
            return CompletionRequest(promptId, args)
        }
    }
}

@Keep
interface AIService {
    fun complete(req: CompletionRequest) : String
}

internal class AIServiceImpl : AIService {
    override fun complete(req: CompletionRequest): String {
        TODO("Not yet implemented")
    }
}