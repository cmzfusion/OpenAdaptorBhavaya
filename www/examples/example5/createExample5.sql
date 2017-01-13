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
INSERT INTO INSTRUMENT VALUES (0, 'EUR', 0, 'EIB 3.75 30/09/2007', 'Y', 2.99);
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
