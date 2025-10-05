       IDENTIFICATION DIVISION.
       PROGRAM-ID. SUBPROG.

       DATA DIVISION.


       WORKING-STORAGE SECTION.
       01  LS-TEMP         PIC 9(9) VALUE 0.

       01 WS-DESCRIPTION.
       05 WS-DATE1 VALUE '20140831'.
       10 WS-YEAR PIC X(4).
       10 WS-MONTH PIC X(2).
       10 WS-DATE PIC X(2).
       05 WS-DATE2 REDEFINES WS-DATE1 PIC 9(6).

       LINKAGE SECTION.
       01 LS-ID PIC 9(5).

       PROCEDURE DIVISION USING LS-ID.

           DISPLAY "=== Subprogram Start ===".
           DISPLAY "Received ID: " LS-ID.
           MOVE LS-ID TO LS-TEMP.
           ADD 10 TO LS-TEMP.
           DISPLAY "LS-TEMP after adding 10: " LS-TEMP.
           DISPLAY "=== Subprogram End ===".

           DISPLAY "WS-DATE1 : "WS-DATE1.
           DISPLAY "WS-DATE2 : "WS-DATE2.
           EXIT PROGRAM.
