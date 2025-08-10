# P&L Bot

Telegram bot for tracking MOEX and cryptocurrency performance.

## Architecture

```
                               +---------------------------+
                               |        Telegram           |
                               +-------------+-------------+
                                             |
                                             v
                       +---------------------+---------------------+
                       |                    Bot                    |
                       |      (python-telegram-bot webhook)       |
                       +---------------------+---------------------+
                                             |
                                             v
                                     +-------+-------+
                                     |   Gateway     |
                                     |   FastAPI     |
                                     +---+-------+---+
                                         |       |
                                         |       |
                                         v       v
                                   +-----+--+  +--+-----+
                                   |Postgres|  | Redis  |
                                   +-----+--+  +--+-----+
                                         |       |
                                         v       v
                                   +-----+-------+-----+
                                   |     Workers       |
                                   | (Celery/RQ alerts)|
                                   +-----+-------+-----+
                                         |
                             +-----------+-----------+
                             |   External Providers  |
                             | MOEX, Tinkoff,        |
                             | CoinGecko, Binance,   |
                             | Fear&Greed, WhaleAlert|
                             +-----------+-----------+
                                         |
                             +-----------+-----------+
                             | Monitoring & Logging   |
                             | Prometheus, Grafana,   |
                             | Sentry, structlog      |
                             +-----------------------+
```

## Bot Commands

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

