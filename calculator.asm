
;
; calculator
; weeeeeeeeeeee
;

%define HALTER_ADDRESS 0xFFFE
%define IOMC_ADDRESS 0x8000

;	int acc = 0;
; 	while(true) {
;		printstr(msg_accumulator);
;		iomc.printint(acc);
;		iomc.printchr('\n');
;		printstr(prompt_op);
;		
;		getstr(buffer, 5);
;		
;		if(matches(buffer, symbol_clr)) {
;			acc = 0;
;		} else if(matches(buffer, symbol_end)) {
;			halt();
;		} else {
;			int operation = -1;
;
;			if(matches(buffer, symbol_add)) {
;				operation = 0;
;			} else if(matches(buffer, symbol_sub)) {
;				operation = 1;
;			} else if(matches(buffer, symbol_mul)) {
;				operation = 2;
;			}
;
;			if(operation == -1) {
;				printstr(msg_unknown);
;				continue;
;			}
;
;			int v = iomc.getint();
;
;			switch(operation) {
;				case 0:
;					acc += v;
;					break;
;				
;				case 1:
;					acc -= v;
;					break;
;
;				case 2:
;					acc *= v;
;					break;
;
;				default:
;			}
;		}
;	}


; void main()
; main
func_main:
	MOV A, 3 * (23 - 2) + (83 / (28 * 2 * (15 + 95) - 23)) + 12 ; = 75
	MOV A, 0x00		; acc
	
	
.loop:
	; printstr(msg_accumulator)
	PUSH A
	PUSH ptr msg_accumulator
	CALL func_printstr
	ADD SP, 4
	POP A
	
	; iomc.printint(acc)
	MOV [IOMC_ADDRESS + 0], A
	MOV [IOMC_ADDRESS + 5], AL
	
	; iomc.printchr('\n')
	MOV B, 0x0A
	MOV [IOMC_ADDRESS + 0], BL
	MOV [IOMC_ADDRESS + 4], BL
	
	; printstr(prompt_op)
	PUSH A
	PUSH ptr prompt_op
	CALL func_printstr
	ADD SP, 4
	
	; getstr(buffer, 5)
	PUSH byte 5
	PUSH ptr buffer
	CALL func_getstr
	ADD SP, 5
	
	; matches(buffer, symbol_clr)
	PUSH ptr symbol_clr
	PUSH ptr buffer
	CALL func_matches
	ADD SP, 8
	
	CMP A, 0
	JE .next0
	
	POP B
	MOV A, 0x00
	JMP .loop
	
.next0:
	; matches(buffer, symbol_end)
	PUSH ptr symbol_end
	PUSH ptr buffer
	CALL func_matches
	ADD SP, 8
	
	CMP A, 0
	JE .next1
	JMP func_halt

.next1:
	; matches(buffer, symbol_add)
	PUSH ptr symbol_add
	PUSH ptr buffer
	CALL func_matches
	ADD SP, 8
	
	CMP A, 0			; if matches operation = 0
	JE .next2
	MOV A, byte 0x00
	JMP .readint
	
.next2:
	; matches(buffer, symbol_sub)
	PUSH ptr symbol_sub
	PUSH ptr buffer
	CALL func_matches
	ADD SP, 8
	
	CMP A, 0			; if matches operation = 1
	JE .next3
	MOV A, byte 0x01
	JMP .readint

.next3:
	; matches(buffer, symbol_mul)
	PUSH ptr symbol_mul
	PUSH ptr buffer
	CALL func_matches
	ADD SP, 8
	
	CMP A, 0			; if matches operation = 2
	JE .next4
	MOV A, byte 0x02
	JMP .readint

.next4:
	; unknown operation
	; printstr(msg_unknown)
	PUSH ptr msg_unknown
	CALL func_printstr
	ADD SP, 4
	POP A
	JMP .loop

	; sneaky jump table
.switch_table:
	db .case0 - .case0
	db .case1 - .case0
	db .case2 - .case0

.readint:
	; v = iomc.getint();
	MOV BL, [IOMC_ADDRESS + 5]
	MOV B, [IOMC_ADDRESS + 0]
	
	; switch 
	MOVW C:D, .switch_table
	MOV DL, [C:D + A]
	POP A
	JMP DL

.case0:
	ADD A, B
	JMP .loop

.case1:
	SUB A, B
	JMP .loop

.case2:
	MUL A, B
	JMP .loop
	

; boolean matches(char* a, char* b)
; returns true if the contents of a are equal to the contents of b
;
;	int index = 0
;	while(a[index] != 0 && b[index] != 0) {
;		index++;
;		if(a[index != b[index]) return 0;
;	}
;	return 1;
func_matches:
	PUSH I
	PUSH BP
	
	MOV I, 0x00
	MOVW C:D, [SP + 10]	; a
	MOV BP, [SP + 14]	; b
	
.loop:
	MOV AL, [C:D + I]	; a[i]
	MOV BL, [BP + I]	; b[i]
	CMP AL, BL
	JNE .false
	
	CMP AL, 0x00		; end
	JE .true
	
	INC I
	JMP .loop

.true:
	MOV A, 0x01
	JMP .end
	
.false:	
	MOV A, 0x00

.end:
	POP BP
	POP I
	RET
	
; void readln(char* buff, byte size)
; reads up to size bytes into buff, newline terminated
;
;	charsRead = 0;
;	while(charsRead < size) {
;		buff[charsRead] = iomc.readChar();
;		if(buff[charsRead] == '\n') break;
;	}
func_getstr:
	MOV A, 0x00			; AH = data, AL = chars read
	MOVW B:C, [SP + 4]	; buffer
	MOV DL, [SP + 8]	; size
	
	PUSH BP
	MOV BP, IOMC_ADDRESS
	
.loop:
	CMP AL, DL			; size check
	JNL .end
	
	; read
	MOV AH, [BP + 4]	; read char command
	MOV AH, [BP + 0]	; get char
	MOV [B:C], AH		; write buffer
	INC C
	ICC B
	
	; check end
	CMP AH, 0x0A
	JNE .loop
	
.end:
	POP BP
	RET

; void printstr(char* str)
; prints a string
func_printstr:
	; callee preserved
	PUSH I
	PUSH J
	
	; get argument
	MOVW J:I, [SP + 8]
	MOVZ B:C, IOMC_ADDRESS
	
.loop:
	MOV AL, [J:I]		; str char
	INC I				; increment pointer
	ICC J
	MOV [B:C + 0], AL	; buffer
	MOV [B:C + 4], AL	; write buffer
	CMP A, 0x00			; zero terminated
	JNE .loop

.end:
	POP J
	POP I
	RET

; void halt()
; writes anything to the halting address
func_halt:
	MOV [HALTER_ADDRESS], A

; data
msg_accumulator		db "Accumulator: ", 0x00
msg_unknown			db "Unknown operation.\n", 0x00
prompt_value		db "Value: ", 0x00
prompt_op			db "Operation: ", 0x00
symbol_add			db "+", 0x00
symbol_sub			db "-", 0x00
symbol_mul			db "*", 0x00
symbol_clr			db "clr", 0x00
symbol_end			db "end", 0x00
buffer				resb 5