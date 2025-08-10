# P&L Bot

Multi-module Kotlin project for a Telegram bot tracking MOEX and crypto performance.

## Modules

* `core` – domain models and use cases
* `data` – Exposed repositories and Flyway migrations
* `clients` – HTTP/WS clients for MOEX, Tinkoff, CoinGecko, Binance, Fear&Greed, Whale Alert
* `bot` – Ktor webhook server and Telegram bot logic (pengrad)
* `worker` – Quartz jobs for alerts and digests
* `metrics` – Micrometer Prometheus endpoint
* `infra` – configuration, Koin DI, Redis, Sentry, common utilities

## Architecture

```
                                 +-------------+
                                 |  Telegram   |
                                 +------+------+
                                        |
                                        v
                          +-------------+-------------+
                          |            Bot            |
                          |  Ktor webhook + pengrad   |
                          +------+-------+------+-----+
                                 |              |
                                 v              v
                           +-----+-----+   +----+----+
                           |  Clients  |   | Metrics |
                           +--+-----+--+   +----+----+
                              |     |           |
                              v     v           |
                        +-----+-----+     +----+----+
                        |   Data    |<----+ Worker |
                        |  Exposed  |     | Quartz |
                        +-----+-----+     +----+----+
                              |                 |
                              v                 |
                            +--+----------------+--+
                            |       Infra         |
                            | Koin, Redis, Sentry |
                            +--+------------------+
                              |
                              v
                            +----+
                            |Core|
                            +----+
```

External providers: MOEX ISS & Options API, Tinkoff Invest WS, CoinGecko, Binance Spot, Alternative.me Fear & Greed, Whale Alert

### MOEX option calculator responses

* `series` – `SECID` list
* `chain` – `STRIKE`, `CALL`, `PUT`
* `greeks` – `delta`, `gamma`, `theta`, `vega`, `iv`
* `smile` – `strike`, `iv`
* `strategy_pnl` – `price`, `pnl` with `breakeven` (`low`, `high`)

## Commands

* `/start` – start the bot
* `/help` – show help
* `/upgrade` – upgrade account
* `/quote` – get MOEX quote
* `/options` – list available options
* `/dividends` – show dividends
* `/portfolio add|show` – manage portfolio
* `/alert` – set MOEX price alert
* `/cquote` – get crypto quote
* `/calert` – set crypto alert
* `/cgainers` – top crypto gainers
* `/convert` – convert currencies
* `/cfear` – crypto fear & greed index
