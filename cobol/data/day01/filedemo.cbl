       IDENTIFICATION DIVISION.
       PROGRAM-ID. FILEDEMO.

       ENVIRONMENT DIVISION.
      *> 输入输出节，定义程序与外部文件之间的关系。
       INPUT-OUTPUT SECTION.
      *> 文件控制段，定义逻辑文件与物理文件的对应关系。
       FILE-CONTROL.
      *> FILEN 是文件变量名代表文件逻辑名
      *> 声明逻辑文件名（在程序中使用的文件名）。
           SELECT FILEN ASSIGN TO "input.txt"
      *> 组织有序
               ORGANIZATION IS LINE SEQUENTIAL.

       DATA DIVISION.
       FILE SECTION.
      *> 定义文件的结构。定义文件描述符（File Description），必须和 SELECT FILEN 对应。
       FD  FILEN.
       01  FILE-RECORD.
           05  LINE-CONTENT  PIC X(15).

      *> 程序运行期间 一直存在 的变量（全局变量）。
       WORKING-STORAGE SECTION.
       01  WS-STATUS         PIC 9(1) VALUE 0.
       01  WS-END            PIC X(10).
       01  WS-NUM2 PIC PPP999 VALUE 0.123.
       01  WS-NUM3 PIC 999PPP VALUE 123.
       01 WS-NUM1 PIC 99V9 VALUE IS 3.5.
       01 WS-NAME PIC A(6) VALUE 'ABCD'.
       01 WS-ID PIC 99 VALUE ZERO.
       01 WS-DATE        PIC 9(8).
       01 WS-YEAR        PIC 9(4).
       01 WS-MONTH       PIC 9(2).
       01 WS-DAY         PIC 9(2).

       COPY "ABC.COPY".

      *> 每次调用程序（或 PERFORM 段）时 重新分配、清空 的变量。
       LOCAL-STORAGE SECTION.
       01 LS-CLASS PIC 9(3).
       01 LS-ID PIC 9(5).

       PROCEDURE DIVISION.
       MAIN-SECTION.
           PERFORM INIT-SECTION
           PERFORM PROCESS-SECTION
           OPEN INPUT FILEN
           PERFORM UNTIL WS-STATUS = 1
               READ FILEN
                   AT END 
                       MOVE 1 TO WS-STATUS 
                       DISPLAY WS-STATUS
                   NOT AT END DISPLAY LINE-CONTENT "*"
               END-READ
           END-PERFORM
           CLOSE FILEN
           *> 调用子程序
           MOVE 12345 TO LS-ID
           CALL 'SUBPROG' USING LS-ID
           CALL 'NOPARAMSUB'

           MOVE HIGH-VALUES TO WS-END.
           DISPLAY "WS-END:" WS-END.
           MOVE LOW-VALUES TO WS-END.
           DISPLAY "WS-END:" WS-END.

           DISPLAY "WS-NUM1 : "WS-NUM1.
           DISPLAY "WS-NAME : "WS-NAME.
           DISPLAY "WS-ID   : "WS-ID.
           DISPLAY "WS-NUM2:" WS-NUM2.

           DISPLAY "WS-NUM3:" WS-NUM3.

           ACCEPT WS-DATE FROM DATE YYYYMMDD

           MOVE WS-DATE(1:4) TO WS-YEAR
           MOVE WS-DATE(5:2) TO WS-MONTH
           MOVE WS-DATE(7:2) TO WS-DAY

           DISPLAY "System Date: " WS-YEAR "-" WS-MONTH "-" WS-DAY

           
           DISPLAY "WS-DESCRIPTION:" WS-DESCRIPTION
           STOP RUN.

       INIT-SECTION.
           DISPLAY "Initializing...".
           EXIT.
           
       PROCESS-SECTION.
           DISPLAY "Processing...".
           DISPLAY "This isn't invalid".
           EXIT.


