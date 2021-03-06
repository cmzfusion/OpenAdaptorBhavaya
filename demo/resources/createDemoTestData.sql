--*******************************************************************
INSERT INTO INSTRUMENT VALUES (0, 'EUR', 0, 'EIB 3.75 30/09/2007', 'Y', 'DE');
INSERT INTO INSTRUMENT VALUES (1, 'EUR', 0, 'MRA 4.75 30/12/2004', 'Y', 'DE');
INSERT INTO INSTRUMENT VALUES (2, 'EUR', 1, '3M EIB 2.75 01/01/2005', 'Y', 'FR');
INSERT INTO INSTRUMENT VALUES (3, 'GBP', 0, 'M&S 7.75 30/12/2003', 'Y', 'GB');
INSERT INTO INSTRUMENT VALUES (4, 'GBP', 0, 'M&S 6.75 30/12/2003', 'Y', 'GB');
COMMIT;

--*******************************************************************
INSERT INTO BOND VALUES (0, '2007-09-30', 1000000, 0.0375, 2.5);
INSERT INTO BOND VALUES (1, '2004-12-30', 500000, 0.0475, 3.2);
INSERT INTO BOND VALUES (3, '2003-12-30', 1000, 0.0775, 0.93);
INSERT INTO BOND VALUES (4, '2003-12-30', 1000, 0.0675, 0.01);
COMMIT;

--*******************************************************************
INSERT INTO BOND_FUTURE VALUES (2, 1000000, '2003-09-01', '2003-12-01');
COMMIT;

--*******************************************************************
INSERT INTO INSTRUMENT_RATING VALUES (0, 0);
INSERT INTO INSTRUMENT_RATING VALUES (0, 6);
INSERT INTO INSTRUMENT_RATING VALUES (1, 2);
INSERT INTO INSTRUMENT_RATING VALUES (1, 9);
INSERT INTO INSTRUMENT_RATING VALUES (3, 0);
INSERT INTO INSTRUMENT_RATING VALUES (3, 6);
INSERT INTO INSTRUMENT_RATING VALUES (4, 5);
INSERT INTO INSTRUMENT_RATING VALUES (4, 11);
COMMIT;

--*******************************************************************
INSERT INTO TRADE VALUES (1, 0, 0, '2002-08-11', 0, 0, 'FOR DEMO', 2, 3000000, 102.34);
COMMIT;

INSERT INTO POSITION2 VALUES (0, 3000000, '2002-08-11');
COMMIT;
