INSERT INTO CURRENCY VALUES ('EUR');
INSERT INTO CURRENCY VALUES ('GBP');
INSERT INTO CURRENCY VALUES ('USD');
COMMIT;

--*******************************************************************
INSERT INTO INSTRUMENT_TYPE VALUES (0, 'BOND');
INSERT INTO INSTRUMENT_TYPE VALUES (1, 'BOND FUTURE');
--INSERT INTO INSTRUMENT_TYPE VALUES (2, 'IR FUTURE');
--INSERT INTO INSTRUMENT_TYPE VALUES (3, 'FUTURE OPTION');
COMMIT;

--*******************************************************************
INSERT INTO RATING VALUES (0, 'SP', 'AAA');
INSERT INTO RATING VALUES (1, 'SP', 'AA');
INSERT INTO RATING VALUES (2, 'SP', 'A');
INSERT INTO RATING VALUES (3, 'SP', 'BBB');
INSERT INTO RATING VALUES (4, 'SP', 'BB');
INSERT INTO RATING VALUES (5, 'SP', 'B');
INSERT INTO RATING VALUES (6, 'MOODY', 'A+');
INSERT INTO RATING VALUES (7, 'MOODY', 'A');
INSERT INTO RATING VALUES (8, 'MOODY', 'A-');
INSERT INTO RATING VALUES (9, 'MOODY', 'B+');
INSERT INTO RATING VALUES (10, 'MOODY', 'B');
INSERT INTO RATING VALUES (11, 'MOODY', 'B-');
COMMIT;

--*******************************************************************
INSERT INTO TRADE_TYPE VALUES (0, 'BUY');
INSERT INTO TRADE_TYPE VALUES (1, 'SELL');
COMMIT;

--*******************************************************************
INSERT INTO VERSION_STATUS VALUES (0, 'GOOD');
INSERT INTO VERSION_STATUS VALUES (1, 'AMENDED');
INSERT INTO VERSION_STATUS VALUES (2, 'CANCELLED');
COMMIT;

--*******************************************************************
INSERT INTO COUNTERPARTY VALUES (0, 'GENERAL TRADERS PLC');
INSERT INTO COUNTERPARTY VALUES (1, 'FINANCIAL PRODUCTS LIMITED');
INSERT INTO COUNTERPARTY VALUES (2, 'BIG BANK CO');
COMMIT;