-- ============================================================
--  BudgetWise – Oracle Schema
--  Run once in SQL Developer / SQL*Plus
-- ============================================================

CREATE TABLE users (
    id         NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR2(50)  UNIQUE NOT NULL,
    email      VARCHAR2(100) UNIQUE NOT NULL,
    password   VARCHAR2(255) NOT NULL,
    created_at DATE DEFAULT SYSDATE
);

CREATE TABLE monthly_budgets (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      NUMBER NOT NULL,
    month        NUMBER(2) NOT NULL,
    year         NUMBER(4) NOT NULL,
    total_budget NUMBER(12,2) NOT NULL,
    created_at   DATE DEFAULT SYSDATE,
    CONSTRAINT fk_mb_user   FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_user_month UNIQUE (user_id, month, year)
);

CREATE TABLE expenses (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id      NUMBER NOT NULL,
    title        VARCHAR2(100) NOT NULL,
    amount       NUMBER(10,2)  NOT NULL,
    category     VARCHAR2(50)  DEFAULT 'General',
    note         VARCHAR2(255),
    expense_date DATE NOT NULL,
    created_at   DATE DEFAULT SYSDATE,
    CONSTRAINT fk_exp_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_exp_user_date ON expenses(user_id, expense_date);
