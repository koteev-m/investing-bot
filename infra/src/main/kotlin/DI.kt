package pl.bot.infra

import org.koin.dsl.module
import pl.bot.clients.ClientsPlaceholder
import pl.bot.data.DataPlaceholder
import pl.bot.core.CorePlaceholder

val clientsModule = module {
    single { ClientsPlaceholder }
}

val repositoriesModule = module {
    single { DataPlaceholder }
}

val servicesModule = module {
    single { CorePlaceholder }
}
