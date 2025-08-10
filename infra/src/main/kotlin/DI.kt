package pl.bot.infra

import org.koin.dsl.module
import pl.bot.clients.cache.Cache
import pl.bot.clients.cache.InMemoryCache
import pl.bot.clients.coingecko.CoinGeckoClient
import pl.bot.clients.moex.MoexIssClient
import pl.bot.core.crypto.CryptoQuoteService
import pl.bot.core.crypto.RealCryptoQuoteService
import pl.bot.core.quote.QuoteService
import pl.bot.core.quote.RealQuoteService

val clientsModule = module {
    single<Cache> { InMemoryCache() }
    single { MoexIssClient(get()) }
    single { CoinGeckoClient(get()) }
}

val repositoriesModule = module { }

val servicesModule = module {
    single<QuoteService> { RealQuoteService(get(), get()) }
    single<CryptoQuoteService> { RealCryptoQuoteService(get()) }
}
