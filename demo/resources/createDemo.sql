CREATE TABLE CURRENCY
(
    CODE                        CHAR(3)           NOT NULL,
    CONSTRAINT PK_CURRENCY PRIMARY KEY (CODE)
);

--*******************************************************************
CREATE TABLE INSTRUMENT_TYPE
(
    INSTRUMENT_TYPE_ID          INT                   NOT NULL,
    NAME                        VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_INSTRUMENT_TYPE PRIMARY KEY (INSTRUMENT_TYPE_ID)
);

--*******************************************************************
CREATE TABLE INSTRUMENT (
     INSTRUMENT_ID        INT                            NOT NULL,
     CURRENCY             CHAR(3)                        NULL,
     INSTRUMENT_TYPE_ID   INT                            NULL,
     DESCRIPTION          VARCHAR(50)                    NULL,
     VALID                CHAR(1)                        NOT NULL,
     ISSUER_COUNTRY       VARCHAR(255)                   NULL,
     CONSTRAINT PK_INSTRUMENT PRIMARY KEY (INSTRUMENT_ID)
);
CREATE INDEX IDX_INSTRUMENT_INSTRUMENT_TYPE ON INSTRUMENT(INSTRUMENT_TYPE_ID);
ALTER TABLE INSTRUMENT ADD CONSTRAINT FK_INSTRUMENT_INSTRUMENT_TYPE FOREIGN KEY (INSTRUMENT_TYPE_ID) REFERENCES INSTRUMENT_TYPE (INSTRUMENT_TYPE_ID);
CREATE INDEX IDX_INSTRUMENT_CURRENCY ON INSTRUMENT(CURRENCY);
ALTER TABLE INSTRUMENT ADD CONSTRAINT FK_INSTRUMENT_CURRENCY FOREIGN KEY (CURRENCY) REFERENCES CURRENCY (CODE);

--*******************************************************************
CREATE TABLE BOND (
     INSTRUMENT_ID        INT                            NOT NULL,
     MATURITYDATE         DATETIME                       NULL,
     PARAMOUNT            NUMERIC(18,4)                  NULL,
     COUPON               FLOAT                          NULL,
     ZSPREAD              FLOAT                          NULL,
     CONSTRAINT PK_BOND PRIMARY KEY (INSTRUMENT_ID)
);
ALTER TABLE BOND ADD CONSTRAINT FK_BOND_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);

--*******************************************************************
CREATE TABLE BOND_FUTURE (
     INSTRUMENT_ID        INT                            NOT NULL,
     CONTRACTSIZE         NUMERIC(19,2)                  NULL,
     FIRSTDELIVERYDATE    DATETIME                       NULL,
     LASTDELIVERYDATE     DATETIME                       NULL,
     CONSTRAINT PK_BOND_FUTURE PRIMARY KEY (INSTRUMENT_ID)
);
ALTER TABLE BOND_FUTURE ADD CONSTRAINT FK_BOND_FUTURE_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);

--*******************************************************************
CREATE TABLE RATING (
     RATING_ID            INT                            NOT NULL,
     RATER                VARCHAR(50)                    NOT NULL,
     RATING               VARCHAR(50)                    NOT NULL,
     CONSTRAINT PK_RATING PRIMARY KEY (RATING_ID)
);

--*******************************************************************
CREATE TABLE INSTRUMENT_RATING (
     INSTRUMENT_ID         INT                            NOT NULL,
     RATING_ID             INT                            NOT NULL,
     CONSTRAINT PK_INSTRUMENT_RATING PRIMARY KEY (INSTRUMENT_ID, RATING_ID)
);
CREATE INDEX IDX_INSTRUMENT_RATING_INSTRUMENT ON INSTRUMENT_RATING(INSTRUMENT_ID);
ALTER TABLE INSTRUMENT_RATING ADD CONSTRAINT FK_INSTRUMENT_RATING_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);
CREATE INDEX IDX_INSTRUMENT_RATING_RATING ON INSTRUMENT_RATING(RATING_ID);
ALTER TABLE INSTRUMENT_RATING ADD CONSTRAINT FK_INSTRUMENT_RATING_RATING FOREIGN KEY (RATING_ID) REFERENCES RATING (RATING_ID);

--*******************************************************************
CREATE TABLE TRADE_TYPE
(
    TRADE_TYPE_ID                   INT                   NOT NULL,
    NAME                            VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_TRADE_TYPE PRIMARY KEY (TRADE_TYPE_ID)
);

--*******************************************************************
CREATE TABLE VERSION_STATUS
(
    VERSION_STATUS_ID               INT                   NOT NULL,
    NAME                            VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_VERSION_STATUS PRIMARY KEY (VERSION_STATUS_ID)
);

--*******************************************************************
CREATE TABLE COUNTERPARTY
(
    COUNTERPARTY_ID               NUMERIC(18,0)         NOT NULL,
    NAME                          VARCHAR(50)           NOT NULL,
    CONSTRAINT PK_COUNTERPARTY PRIMARY KEY (COUNTERPARTY_ID)
);

--*******************************************************************
CREATE TABLE TRADE
(
    TRADE_ID                        INT                             NOT NULL,
    VERSION                         INT                             NOT NULL,
    VERSION_STATUS_ID               INT                             NOT NULL,
    TRADE_DATE                      DATETIME                        NULL,
    TRADE_TYPE_ID                   INT                             NOT NULL,
    INSTRUMENT_ID                   INT                             NOT NULL,
    COMMENTS                        VARCHAR(255)                    NULL,
    COUNTERPARTY_ID                 NUMERIC(18,0)                   NULL,
    QUANTITY                        FLOAT                           NULL,
    PRICE                           FLOAT                           NULL,
    CONSTRAINT PK_TRADE PRIMARY KEY (TRADE_ID, VERSION)
);
CREATE INDEX IDX_TRADE_INSTRUMENT ON TRADE(INSTRUMENT_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);
CREATE INDEX IDX_TRADE_VERSION_STATUS ON TRADE(VERSION_STATUS_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_VERSION_STATUS FOREIGN KEY (VERSION_STATUS_ID) REFERENCES VERSION_STATUS (VERSION_STATUS_ID);
CREATE INDEX IDX_TRADE_TRADE_TYPE ON TRADE(TRADE_TYPE_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_TRADE_TYPE FOREIGN KEY (TRADE_TYPE_ID) REFERENCES TRADE_TYPE (TRADE_TYPE_ID);
CREATE INDEX IDX_TRADE_COUNTERPARTY ON TRADE(COUNTERPARTY_ID);
ALTER TABLE TRADE ADD CONSTRAINT FK_TRADE_COUNTERPARTY FOREIGN KEY (COUNTERPARTY_ID) REFERENCES COUNTERPARTY (COUNTERPARTY_ID);

--*******************************************************************
CREATE TABLE POSITION2
(
    INSTRUMENT_ID                   INT                             NOT NULL,
    QUANTITY                        FLOAT                           NULL,
    UPDATE_TIMESTAMP                DATETIME                        NULL,
    CONSTRAINT PK_POSITION PRIMARY KEY (INSTRUMENT_ID)
);
ALTER TABLE POSITION2 ADD CONSTRAINT FK_POSITION_INSTRUMENT FOREIGN KEY (INSTRUMENT_ID) REFERENCES INSTRUMENT (INSTRUMENT_ID);
