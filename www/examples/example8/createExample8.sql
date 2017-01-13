--*******************************************************************
CREATE TABLE CURRENCY
(
    CODE                        CHAR(3)           NOT NULL,
    CONSTRAINT PK_CURRENCY PRIMARY KEY (CODE)
);
--*******************************************************************
INSERT INTO CURRENCY VALUES ('EUR');
INSERT INTO CURRENCY VALUES ('GBP');
INSERT INTO CURRENCY VALUES ('USD');
COMMIT;
--*******************************************************************
--*******************************************************************

--*******************************************************************
CREATE TABLE INSTRUMENT (
     INSTRUMENT_ID        INT                            NOT NULL,
     CURRENCY             CHAR(3)                        NULL,
     INSTRUMENT_TYPE_ID   INT                            NULL,
     DESCRIPTION          VARCHAR(50)                    NULL,
     VALID                CHAR(1)                        NOT NULL,
     PRICE                FLOAT                          NULL,
     CONSTRAINT PK_INSTRUMENT PRIMARY KEY (INSTRUMENT_ID)
);

CREATE INDEX IDX_INSTRUMENT_CURRENCY ON INSTRUMENT(CURRENCY);
ALTER TABLE INSTRUMENT ADD CONSTRAINT FK_INSTRUMENT_CURRENCY FOREIGN KEY (CURRENCY) REFERENCES CURRENCY (CODE);
--*******************************************************************
INSERT INTO INSTRUMENT VALUES (0, 'USD', 0, 'EIB 3.75 30/09/2007', 'Y', 2.99);
INSERT INTO INSTRUMENT VALUES (1, 'EUR', 0, 'MRA 4.75 30/12/2004', 'Y', 5.73);
INSERT INTO INSTRUMENT VALUES (2, 'EUR', 1, '3M EIB 2.75 01/01/2005', 'Y', 1.24);
INSERT INTO INSTRUMENT VALUES (3, 'GBP', 0, 'M&S 7.75 30/12/2003', 'Y', 8.96);
INSERT INTO INSTRUMENT VALUES (4, 'GBP', 0, 'M&S 6.75 30/12/2003', 'Y', 3.52);
COMMIT;
--*******************************************************************
--*******************************************************************

--*******************************************************************
CREATE TABLE BOND (
     INSTRUMENT_ID        INT                            NOT NULL,
     MATURITYDATE         DATETIME                       NULL,
     PARAMOUNT            NUMERIC(18,4)                  NULL,
     COUPON               FLOAT                          NULL,
     CONSTRAINT PK_BOND PRIMARY KEY (INSTRUMENT_ID)
);
ALTER TABLE BOND ADD CONSTRAINT FK_BOND_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);
--*******************************************************************
INSERT INTO BOND VALUES (0, '2007-09-30', 1000000, 0.0375);
INSERT INTO BOND VALUES (1, '2004-12-30', 500000, 0.0475);
INSERT INTO BOND VALUES (3, '2003-12-30', 1000, 0.0775);
INSERT INTO BOND VALUES (4, '2003-12-30', 1000, 0.0675);
COMMIT;
--*******************************************************************
--*******************************************************************

--*******************************************************************
CREATE TABLE TRADE_TYPE
(
    TRADE_TYPE_ID                   INT                   NOT NULL,
    NAME                            VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_TRADE_TYPE PRIMARY KEY (TRADE_TYPE_ID)
);

--*******************************************************************
INSERT INTO TRADE_TYPE VALUES (0, 'BUY');
INSERT INTO TRADE_TYPE VALUES (1, 'SELL');
COMMIT;
--*******************************************************************
--*******************************************************************

--*******************************************************************
CREATE TABLE TRADE
(
    TRADE_ID                        INT                             NOT NULL,
    VERSION                         INT                             NOT NULL,
    TRADE_DATE                      DATETIME                        NULL,
    TRADE_TYPE_ID                   INT                             NOT NULL,
    INSTRUMENT_ID                   INT                             NOT NULL,
    COMMENTS                        VARCHAR(255)                    NULL,
    QUANTITY                        FLOAT                           NULL,
    PRICE                           FLOAT                           NULL,
    CONSTRAINT PK_TRADE PRIMARY KEY (TRADE_ID, VERSION)
);
CREATE INDEX IDX_TRADE_INSTRUMENT ON TRADE(INSTRUMENT_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);
CREATE INDEX IDX_TRADE_TRADE_TYPE ON TRADE(TRADE_TYPE_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_TRADE_TYPE FOREIGN KEY (TRADE_TYPE_ID) REFERENCES TRADE_TYPE (TRADE_TYPE_ID);
--*******************************************************************
INSERT INTO TRADE VALUES (1, 0, '2002-08-11', 0, 2, 'EVEN', 3000000, 102.34);
INSERT INTO TRADE VALUES (2, 0, '2003-04-23', 1, 1, 'PROFIT', 5000000, 107.34);
INSERT INTO TRADE VALUES (3, 0, '2001-07-19', 0, 3, 'LOSS', 2500000, 101.0);
INSERT INTO TRADE VALUES (4, 0, '2004-01-24', 1, 4, 'LOSS', 8200000, 82.34);
INSERT INTO TRADE VALUES (5, 0, '2002-05-17', 0, 0, 'PROFIT', 7900000, 102.34);
COMMIT;
--*******************************************************************
--*******************************************************************