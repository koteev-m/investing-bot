package pl.bot.metrics

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
