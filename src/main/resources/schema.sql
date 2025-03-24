create table orders(id int primary key,
                    currencyPair varchar(255) not null,
                    dealtCurrency varchar(255) not null,
                    direction varchar(255) not null,
                    amount  decimal(38,2) not null,
                    valueDate varchar(255) not null,
                    userId varchar(255) not null,
                    matchedPct decimal(3,2) default 0,
                    matchedAmount decimal(38,2) default 0);

CREATE SEQUENCE orders_seq
    START WITH 1
    INCREMENT BY 1;