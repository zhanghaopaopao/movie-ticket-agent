CREATE TABLE app_user (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    login_failure_count INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE TABLE email_code (
    id VARCHAR(40) PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_email_code_lookup ON email_code(email, purpose, created_at);

CREATE TABLE refresh_token (
    id VARCHAR(40) PRIMARY KEY,
    user_id VARCHAR(40) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id, revoked_at, expires_at);

CREATE TABLE user_preference (
    user_id VARCHAR(40) PRIMARY KEY,
    district VARCHAR(80) NULL,
    hall_type VARCHAR(40) NULL,
    budget INT NULL,
    seat_zone VARCHAR(40) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_preference_user FOREIGN KEY (user_id) REFERENCES app_user(id)
);

CREATE TABLE movie (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    english_name VARCHAR(160) NULL,
    genres VARCHAR(200) NOT NULL,
    duration_minutes INT NOT NULL,
    rating DECIMAL(3,1) NULL,
    poster_url VARCHAR(500) NULL,
    release_date DATE NOT NULL,
    synopsis VARCHAR(2000) NULL,
    cast_names VARCHAR(1000) NULL,
    want_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_movie_status_release ON movie(status, release_date);
CREATE INDEX idx_movie_name ON movie(name);

CREATE TABLE cinema (
    id VARCHAR(40) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    city VARCHAR(80) NOT NULL,
    district VARCHAR(80) NOT NULL,
    address VARCHAR(300) NOT NULL,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    service_tags VARCHAR(300) NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_cinema_location ON cinema(city, district, status);
CREATE INDEX idx_cinema_brand ON cinema(brand, status);

CREATE TABLE hall (
    id VARCHAR(40) PRIMARY KEY,
    cinema_id VARCHAR(40) NOT NULL,
    name VARCHAR(80) NOT NULL,
    hall_type VARCHAR(40) NOT NULL,
    seat_template_id VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_hall_name UNIQUE (cinema_id, name),
    CONSTRAINT fk_hall_cinema FOREIGN KEY (cinema_id) REFERENCES cinema(id)
);
CREATE INDEX idx_hall_type ON hall(cinema_id, hall_type, status);

CREATE TABLE seat (
    id VARCHAR(40) PRIMARY KEY,
    hall_id VARCHAR(40) NOT NULL,
    row_no INT NOT NULL,
    seat_no INT NOT NULL,
    zone VARCHAR(40) NOT NULL,
    seat_type VARCHAR(30) NOT NULL,
    couple_group VARCHAR(40) NULL,
    CONSTRAINT uk_seat_position UNIQUE (hall_id, row_no, seat_no),
    CONSTRAINT fk_seat_hall FOREIGN KEY (hall_id) REFERENCES hall(id)
);

CREATE TABLE showtime (
    id VARCHAR(64) PRIMARY KEY,
    movie_id VARCHAR(40) NOT NULL,
    hall_id VARCHAR(40) NOT NULL,
    start_at TIMESTAMP(6) NOT NULL,
    end_at TIMESTAMP(6) NOT NULL,
    base_price INT NOT NULL,
    language VARCHAR(30) NOT NULL,
    format VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_showtime_hall_start UNIQUE (hall_id, start_at),
    CONSTRAINT fk_showtime_movie FOREIGN KEY (movie_id) REFERENCES movie(id),
    CONSTRAINT fk_showtime_hall FOREIGN KEY (hall_id) REFERENCES hall(id)
);
CREATE INDEX idx_showtime_movie_time ON showtime(movie_id, start_at, status);
CREATE INDEX idx_showtime_hall_time ON showtime(hall_id, start_at, status);

CREATE TABLE showtime_seat (
    id VARCHAR(100) PRIMARY KEY,
    showtime_id VARCHAR(64) NOT NULL,
    seat_id VARCHAR(40) NOT NULL,
    price INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    lock_owner VARCHAR(40) NULL,
    lock_expires_at TIMESTAMP(6) NULL,
    version INT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_showtime_seat UNIQUE (showtime_id, seat_id),
    CONSTRAINT fk_showtime_seat_showtime FOREIGN KEY (showtime_id) REFERENCES showtime(id),
    CONSTRAINT fk_showtime_seat_seat FOREIGN KEY (seat_id) REFERENCES seat(id)
);
CREATE INDEX idx_showtime_seat_inventory ON showtime_seat(showtime_id, status);
CREATE INDEX idx_showtime_seat_expiry ON showtime_seat(status, lock_expires_at);

CREATE TABLE ticket_order (
    id VARCHAR(40) PRIMARY KEY,
    order_no VARCHAR(40) NOT NULL,
    user_id VARCHAR(40) NOT NULL,
    showtime_id VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_order_no UNIQUE (order_no),
    CONSTRAINT fk_ticket_order_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_ticket_order_showtime FOREIGN KEY (showtime_id) REFERENCES showtime(id)
);
CREATE INDEX idx_ticket_order_user_status ON ticket_order(user_id, status, created_at);
CREATE INDEX idx_ticket_order_expiry ON ticket_order(status, expires_at);

CREATE TABLE purchase_draft (
    id VARCHAR(40) PRIMARY KEY,
    user_id VARCHAR(40) NOT NULL,
    movie_id VARCHAR(40) NULL,
    cinema_id VARCHAR(40) NULL,
    date_time_json LONGTEXT NULL,
    showtime_id VARCHAR(64) NULL,
    ticket_count INT NOT NULL DEFAULT 1,
    budget_json LONGTEXT NULL,
    seats_json LONGTEXT NULL,
    source_mode VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    order_id VARCHAR(40) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_purchase_draft_order UNIQUE (order_id),
    CONSTRAINT fk_purchase_draft_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_purchase_draft_movie FOREIGN KEY (movie_id) REFERENCES movie(id),
    CONSTRAINT fk_purchase_draft_cinema FOREIGN KEY (cinema_id) REFERENCES cinema(id),
    CONSTRAINT fk_purchase_draft_showtime FOREIGN KEY (showtime_id) REFERENCES showtime(id),
    CONSTRAINT fk_purchase_draft_order FOREIGN KEY (order_id) REFERENCES ticket_order(id)
);
CREATE INDEX idx_purchase_draft_user_status ON purchase_draft(user_id, status, updated_at);

CREATE TABLE order_item (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NOT NULL,
    seat_id VARCHAR(40) NOT NULL,
    unit_price INT NOT NULL,
    CONSTRAINT uk_order_item_seat UNIQUE (order_id, seat_id),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
    CONSTRAINT fk_order_item_seat FOREIGN KEY (seat_id) REFERENCES seat(id)
);

CREATE TABLE payment (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount INT NOT NULL,
    processed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_payment_idempotency UNIQUE (order_id, idempotency_key),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES ticket_order(id)
);

CREATE TABLE ticket (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NOT NULL,
    order_item_id VARCHAR(40) NOT NULL,
    ticket_code VARCHAR(40) NOT NULL,
    qr_content VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_ticket_code UNIQUE (ticket_code),
    CONSTRAINT uk_ticket_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_ticket_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
    CONSTRAINT fk_ticket_order_item FOREIGN KEY (order_item_id) REFERENCES order_item(id)
);

CREATE TABLE seat_lock_log (
    id VARCHAR(40) PRIMARY KEY,
    order_id VARCHAR(40) NULL,
    showtime_id VARCHAR(64) NOT NULL,
    seat_id VARCHAR(40) NOT NULL,
    action VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_seat_lock_order FOREIGN KEY (order_id) REFERENCES ticket_order(id),
    CONSTRAINT fk_seat_lock_showtime FOREIGN KEY (showtime_id) REFERENCES showtime(id),
    CONSTRAINT fk_seat_lock_seat FOREIGN KEY (seat_id) REFERENCES seat(id)
);
CREATE INDEX idx_seat_lock_audit ON seat_lock_log(showtime_id, seat_id, created_at);

CREATE TABLE system_config (
    config_key VARCHAR(80) PRIMARY KEY,
    config_value VARCHAR(500) NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    description VARCHAR(300) NULL,
    updated_by VARCHAR(40) NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_system_config_user FOREIGN KEY (updated_by) REFERENCES app_user(id)
);

CREATE TABLE operation_log (
    id VARCHAR(40) PRIMARY KEY,
    operator_id VARCHAR(40) NULL,
    operation_type VARCHAR(80) NOT NULL,
    target_type VARCHAR(80) NOT NULL,
    target_id VARCHAR(64) NULL,
    detail_json LONGTEXT NULL,
    trace_id VARCHAR(64) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_operation_log_user FOREIGN KEY (operator_id) REFERENCES app_user(id)
);
CREATE INDEX idx_operation_log_target ON operation_log(target_type, target_id, created_at);

INSERT INTO system_config(config_key, config_value, value_type, description)
VALUES ('seat.lock.minutes', '15', 'INTEGER', 'Seat lock duration for new orders');
INSERT INTO system_config(config_key, config_value, value_type, description)
VALUES ('agent.timeout.seconds', '8', 'INTEGER', 'Python agent timeout contract');
INSERT INTO system_config(config_key, config_value, value_type, description)
VALUES ('cinema.nearby.radius.km', '5', 'INTEGER', 'Nearby cinema search radius');
INSERT INTO system_config(config_key, config_value, value_type, description)
VALUES ('showtime.alternative.window.minutes', '90', 'INTEGER', 'Alternative showtime time window');
INSERT INTO system_config(config_key, config_value, value_type, description)
VALUES ('payment.simulation.enabled', 'true', 'BOOLEAN', 'Enable simulated payment for new requests');
