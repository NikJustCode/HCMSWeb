-- Скрипт создания базы данных с русскими названиями таблиц и полей
-- Для использования в новом проекте

CREATE TABLE пользователи (
    ид SERIAL PRIMARY KEY,
    электронная_почта VARCHAR(255) UNIQUE NOT NULL,
    пароль VARCHAR(255) NOT NULL,
    роль VARCHAR(50) NOT NULL, -- ADMIN, FRANCHISEE
    фио VARCHAR(255),
    телефон VARCHAR(50)
);

CREATE TABLE сотрудники (
    ид SERIAL PRIMARY KEY,
    фио VARCHAR(255) NOT NULL,
    телефон VARCHAR(50) NOT NULL,
    электронная_почта VARCHAR(255),
    пароль VARCHAR(255),
    тип_графика VARCHAR(50), -- WEEKLY_DAYS, SHIFT_PATTERN
    паттерн_смен VARCHAR(50), -- например "2/2"
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE
);

CREATE TABLE рабочие_дни_сотрудника (
    сотрудник_ид INT NOT NULL REFERENCES сотрудники(ид) ON DELETE CASCADE,
    день_недели VARCHAR(20) NOT NULL -- MONDAY, TUESDAY и т.д.
);

CREATE TABLE торговые_автоматы (
    ид SERIAL PRIMARY KEY,
    название VARCHAR(255) NOT NULL,
    адрес VARCHAR(255) NOT NULL,
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE,
    активен BOOLEAN DEFAULT TRUE
);

CREATE TABLE работники_автоматы (
    сотрудник_ид INT NOT NULL REFERENCES сотрудники(ид) ON DELETE CASCADE,
    автомат_ид INT NOT NULL REFERENCES торговые_автоматы(ид) ON DELETE CASCADE,
    PRIMARY KEY(сотрудник_ид, автомат_ид)
);

CREATE TABLE товары (
    ид SERIAL PRIMARY KEY,
    название VARCHAR(255) NOT NULL,
    категория VARCHAR(255),
    цена DECIMAL(10, 2),
    описание TEXT,
    ссылка_на_фото VARCHAR(255),
    единица_измерения VARCHAR(50),
    активен BOOLEAN DEFAULT TRUE
);

CREATE TABLE складские_записи (
    ид SERIAL PRIMARY KEY,
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE,
    товар_ид INT NOT NULL REFERENCES товары(ид) ON DELETE CASCADE,
    количество DECIMAL(10, 2) NOT NULL DEFAULT 0.0,
    UNIQUE(франчайзи_ид, товар_ид)
);

CREATE TABLE движения_склада (
    ид SERIAL PRIMARY KEY,
    тип_операции VARCHAR(50) NOT NULL, -- INCOME, OUTCOME, MACHINE_SERVICE
    сумма DECIMAL(10, 2) NOT NULL,
    дата_операции TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    описание VARCHAR(255) NOT NULL,
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE,
    товар_ид INT NOT NULL REFERENCES товары(ид) ON DELETE CASCADE,
    автомат_ид INT REFERENCES торговые_автоматы(ид) ON DELETE SET NULL
);

CREATE TABLE отчеты_обслуживания (
    ид SERIAL PRIMARY KEY,
    дата_обслуживания TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    комментарий TEXT,
    сотрудник_ид INT NOT NULL REFERENCES сотрудники(ид) ON DELETE CASCADE,
    автомат_ид INT NOT NULL REFERENCES торговые_автоматы(ид) ON DELETE CASCADE
);

CREATE TABLE расходники_отчета (
    ид SERIAL PRIMARY KEY,
    отчет_ид INT NOT NULL REFERENCES отчеты_обслуживания(ид) ON DELETE CASCADE,
    товар_ид INT NOT NULL REFERENCES товары(ид) ON DELETE CASCADE,
    количество INT NOT NULL
);

CREATE TABLE фотографии_отчета (
    ид SERIAL PRIMARY KEY,
    отчет_ид INT NOT NULL REFERENCES отчеты_обслуживания(ид) ON DELETE CASCADE,
    ссылка_на_фото VARCHAR(255) NOT NULL
);

CREATE TABLE заказы (
    ид SERIAL PRIMARY KEY,
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE,
    дата_заказа TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    статус VARCHAR(50) NOT NULL, -- NEW, PROCESSING, COMPLETED
    общая_сумма DECIMAL(10, 2) NOT NULL,
    комментарий TEXT
);

CREATE TABLE позиции_заказа (
    ид SERIAL PRIMARY KEY,
    заказ_ид INT NOT NULL REFERENCES заказы(ид) ON DELETE CASCADE,
    товар_ид INT NOT NULL REFERENCES товары(ид) ON DELETE CASCADE,
    количество INT NOT NULL,
    цена_на_момент DECIMAL(10, 2) NOT NULL
);

CREATE TABLE лог_статусов_заказа (
    ид SERIAL PRIMARY KEY,
    заказ_ид INT NOT NULL REFERENCES заказы(ид) ON DELETE CASCADE,
    статус VARCHAR(50) NOT NULL,
    дата_смены TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE обращения_техподдержка (
    ид SERIAL PRIMARY KEY,
    франчайзи_ид INT NOT NULL REFERENCES пользователи(ид) ON DELETE CASCADE,
    тема VARCHAR(255) NOT NULL,
    сообщение TEXT NOT NULL,
    статус VARCHAR(50) NOT NULL, -- OPEN, CLOSED
    дата_создания TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ответ TEXT
);
