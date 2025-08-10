CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE rate_limits (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    quota_name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    quota_limit INT NOT NULL,
    used INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_rate_limits_user_quota_date ON rate_limits (user_id, quota_name, date);

CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    message VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE INDEX idx_alerts_user_active ON alerts (user_id, active);

CREATE TABLE crypto_alerts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    symbol VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL
);
CREATE INDEX idx_crypto_alerts_user_active ON crypto_alerts (user_id, active);

CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL
);

CREATE TABLE positions (
    id BIGSERIAL PRIMARY KEY,
    portfolio_id BIGINT NOT NULL REFERENCES portfolios(id),
    asset VARCHAR(255) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL
);

CREATE TABLE trades (
    id BIGSERIAL PRIMARY KEY,
    position_id BIGINT NOT NULL REFERENCES positions(id),
    price DOUBLE PRECISION NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    executed_at TIMESTAMP NOT NULL
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE referrals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    referred_user_id BIGINT NOT NULL REFERENCES users(id),
    code VARCHAR(255) NOT NULL
);
