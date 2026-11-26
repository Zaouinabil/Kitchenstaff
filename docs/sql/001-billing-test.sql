-- Kitchenstaff Pro: additive schema only, MySQL 8+/9.x, InnoDB.
-- Run once, against the explicitly selected Kitchenstaff database, after backup.
-- Existing table users(id BIGINT) must already exist.
-- No existing table or row is altered, removed or replaced by this script.
-- Do not use mysql --force: an existing table or any error requires review.

CREATE TABLE establishments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    owner_user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_establishment_owner UNIQUE (owner_user_id),
    CONSTRAINT fk_establishment_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    establishment_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    stripe_subscription_id VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
    stripe_customer_id VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
    checkout_session_id VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NULL,
    checkout_attempt_id VARCHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
    attempt_started_at DATETIME(6) NULL,
    cancel_at_period_end BIT(1) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_billing_establishment UNIQUE (establishment_id),
    CONSTRAINT uk_billing_stripe_subscription UNIQUE (stripe_subscription_id),
    CONSTRAINT uk_billing_checkout_session UNIQUE (checkout_session_id),
    CONSTRAINT fk_billing_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stripe_webhook_events (
    id VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    type VARCHAR(100) NOT NULL,
    establishment_id BIGINT NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_stripe_event_establishment (establishment_id),
    CONSTRAINT fk_stripe_event_establishment FOREIGN KEY (establishment_id) REFERENCES establishments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
