package io.miniapp.core.openplatform.miniapp.utils

import io.miniapp.core.openplatform.bot.BotInfo
import io.miniapp.core.openplatform.bot.CommandInfo
import io.miniapp.core.openplatform.bot.Option
import io.miniapp.core.openplatform.bot.Scope
import io.miniapp.core.openplatform.common.apis.data.BotDto
import io.miniapp.core.openplatform.common.apis.data.CommandDto
import io.miniapp.core.openplatform.common.apis.data.DAppDto
import io.miniapp.core.openplatform.common.apis.data.MiniAppDto
import io.miniapp.core.openplatform.common.apis.data.OptionDto
import io.miniapp.core.openplatform.common.apis.data.PeerParams
import io.miniapp.core.openplatform.common.apis.data.ScopeDto
import io.miniapp.core.openplatform.miniapp.DAppInfo
import io.miniapp.core.openplatform.miniapp.MiniAppInfo
import io.miniapp.core.openplatform.miniapp.Peer

internal fun MiniAppDto.toInfo() : MiniAppInfo {
    return MiniAppInfo(
        id = this.id,
        identifier = this.identifier,
        title = this.title,
        description = this.description,
        shortDescription = this.shortDescription,
        iconUrl = this.iconUrl,
        bannerUrl = this.bannerUrl,
        botId = this.botId,
        botName = this.botName,
        createAt = this.createAt,
        updateAt = this.updateAt
    )
}

internal fun DAppDto.toInfo() : DAppInfo {
    return DAppInfo(
        id = this.id,
        title = this.title,
        description = this.description,
        shortDescription = this.shortDescription,
        iconUrl = this.iconUrl,
        bannerUrl = this.bannerUrl,
        url = this.url
    )
}

internal fun BotDto.toInfo(): BotInfo {
    return BotInfo(
        id = this.id,
        name = this.name,
        token = this.token,
        userId = this.userId,
        provider = this.provider,
        identifier = this.identifier,
        bio = this.bio,
        avatarUrl = this.avatarUrl,
        metadata = this.metadata,
        command = this.commands?.map { it.toInfo() },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

internal fun CommandDto.toInfo(): CommandInfo {
    return CommandInfo(
        command = this.command,
        type = this.type,
        scope = this.scope?.toInfo(),
        options = this.options?.map { it.toInfo() },
        languageCode = this.languageCode,
        description = this.description
    )
}

internal fun OptionDto.toInfo(): Option {
    return Option(
        name = this.name,
        type = this.type,
        required = this.required,
        description = this.description
    )
}

internal fun ScopeDto.toInfo(): Scope {
    return Scope(
        type = this.type,
        chatId = this.chatId,
        userId = this.userId
    )
}

internal fun Peer.toParams(): PeerParams {
    return PeerParams(
        roomId = this.roomId,
        userId = this.userId,
        accessHash = this.accessHash
    )
}
