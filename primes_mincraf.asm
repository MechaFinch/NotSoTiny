
; primes
; implements a segmented sieving algorithm written for a 6502-esque cpu with 256 bytes
; of ram

;
;	uint16 chunk, end;
;	for(chunk = 0; end = 1024; chunk > 0; chunk += 1024, end += 1024) {
; 		print("Seiving ");
;		print_num(chunk);
;		print(" to ");
;		print_num(end);
;		print("\n");
;
;		// init list
;		for(uint16 i = 0; i < 256; i++) {
;			main_sieve[i] = 0xAA;
;		}
;
;		// init low list
;		for(uint16 i = 0; i < 32; i++) {
;			lower_sieve[i] = 0xAA;
;		}
;
;		// loop over odds
;		uint8 val, mask, sieve_index;
;		uint16 x, i, ix2, j;
;		for(i = 3; i < 256; i += 2) {
;			ix2 = i << 1;
;			sieve_index = i >> 3;
;			
;			// check if this value has already been sieved
;			val = lower_sieve[sieve_index];
;			mask = 1 << (i & 0x07);
;
;			if(val & mask != 0) {
;				// sieve lower 256
;				lower_sieve[sieve_index] = val & ~mask;
;				
;				j = i + ix2;
;				while(j < 256) {
;					sieve_index = j >> 3;
;					mask = 1 << (j & 0x07);
;					lower_sieve[sieve_index] = lower_sieve[sieve_index] & ~mask;
;					j += ix2;
;				}
;
;				// go to chunk
;				x = ((chunk / i) * i) + i; 	// first multiple of i >= chunk
;				if(x & 1 == 0) x += i;		// make odd
;				
;				// sieve
;				while(x < end) {
;					sieve_index = (x >> 3) & 0x7F;
;					mask = 1 << (x & 0x07);
;					main_sieve[sieve_index] = main_sieve[sieve_index] & ~mask;
;					x += ix2;
;				}
;			}
;		}
;
;		// print chunk
;		x = chunk;
;		for(i = 0; i < 128; i++) {
;			// for each bit
;			val = main_sieve[i];
;
;			for(j = 0; j < 8; j++) {
;				if(val & 1 != 0) print_num(x);
;				val >>= 1;
;				x += 1;
;			}
;		}
; 	}
;	
; 	print("Done."); 

%define HALTER_ADDRESS 0xFFFE
%define IOMC_ADDRESS 0x8000

%define LOW_SIEVE 0x0800
%define MAIN_SIEVE (LOW_SIEVE + 0x0100)

%define CHUNK_OFFSET 6
%define END_OFFSET 8

; i = I
; ix2 = J

main:
	CALL func_primes
	MOV [HALTER_ADDRESS], AL
	JMP main

func_primes:
	; adhere to convention
	PUSH BP
	MOV BP, SP
	
	PUSH I
	PUSH J
	
	SUB SP, 4

	; init chunk & end
	MOV A, 0x00
	MOV [BP - CHUNK_OFFSET], A
	MOV A, 1024
	MOV [BP - END_OFFSET], A
	
.main_loop:
	; print section message
	PUSH ptr msg_sieving
	CALL func_printstr
	ADD SP, 4
	
	MOV A, [BP - CHUNK_OFFSET]
	MOV [IOMC_ADDRESS + 0], A
	MOV [IOMC_ADDRESS + 5], AL
	
	PUSH ptr msg_to
	CALL func_printstr
	ADD SP, 4
	
	MOV A, [BP - END_OFFSET]
	MOV [IOMC_ADDRESS + 0], A
	MOV [IOMC_ADDRESS + 5], AL
	
	MOV A, 0x0A
	MOV [IOMC_ADDRESS + 0], AL
	MOV [IOMC_ADDRESS + 4], AL
	
	; init main sieve
	MOVW A:B, MAIN_SIEVE
	MOVW C:D, 0xAAAA_AAAA
	MOV I, (256 / 4) - 1
.loop_init_main:
	MOVW [A:B + I*4], C:D
	DEC I
	JNC .loop_init_main
	
	; init low sieve
	MOVW A:B, LOW_SIEVE
	MOV I, (32 / 4) - 1
.loop_init_low:
	MOVW [A:B + I*4], C:D
	DEC I
	JNC .loop_init_low
	
	; breakpoint
	;MOV [HALTER_ADDRESS], AL
	
	; odds loop
	; i = 3
	MOV I, 3
.odds_loop:
	; ix2 = i << 1
	MOV J, I
	SHL J, 1
	
	; sieve_index = i >> 3;
	MOV A, I
	SHR A, 3
	
	; val = lower_sieve[sieve_index]
	MOV BL, [LOW_SIEVE + A]
	
	; mask
	MOV D, 1
	MOV C, I
	AND C, 7
	SHL D, C
	
	; if(val & mask != 0)
	MOV C, B
	AND C, D
	JZ .odds_loop_continue
	
	; sieve lower 256
	; low_sieve[sieve_index] = val & ~mask
	NOT D
	AND B, D
	MOV [LOW_SIEVE + A], BL
	
	; j = i + ix2
	MOV A, I
	JMP .low_sieve_loop_continue
.low_sieve_loop:
	; sieve_index = j >> 3
	MOV B, I
	SHR B, 3
	
	; mask = 1 << (j & 7)
	MOV D, 1
	MOV C, I
	AND C, 7
	SHL D, C
	NOT D
	
	; lower_sieve[sieve_index] = lower_sieve[sieve_index] & ~mask
	MOV CL, [LOW_SIEVE + B]
	AND C, D
	MOV [LOW_SIEVE + B], CL

.low_sieve_loop_continue:
	; j += ix2
	ADD A, J
	CMP A, 256
	JL .low_sieve_loop
	
	; go to chunk
	; x = ((chunk / i) * i) + i
	
	; breakpoint
	;MOV [HALTER_ADDRESS], AL
	
	MOV A, [BP - CHUNK_OFFSET]
	
	; if chunk zero, we want 3 i
	CMP A, 0
	JNE .nonzero
	ADD A, I
	
.nonzero:
	DIV A, I
	MUL A, I
	ADD A, I
	
	; if(x & 1 == 0) x += i
	MOV B, A
	AND B, 1
	JNZ .main_sieve_loop
	ADD A, I
	
	; sieve main
.main_sieve_loop:
	; sieve_index = (x >> 3) & 0x7F
	MOV B, A
	SHR B, 3
	AND B, 0x7F
	
	; mask = 1 << (x & 0x07)
	MOV D, 1
	MOV C, A
	AND C, 7
	SHL D, C
	NOT D
	
	; main_sieve[sieve_index] = main_sieve[sieve_index] & ~mask
	MOV CL, [MAIN_SIEVE + B]
	AND C, D
	MOV [MAIN_SIEVE + B], CL
	
	; x += ix2
	ADD A, J
	CMP A, [BP - END_OFFSET]
	JB .main_sieve_loop

.odds_loop_continue:
	; i += 2
	INC I	; inc saves 1 byte here
	INC I
	
	CMP I, 256
	JL .odds_loop
	
	; breakpoint
	;MOV [HALTER_ADDRESS], AL
	
	; print chunk
	MOV A, [BP - CHUNK_OFFSET]
	PUSH I
	MOV I, 0
	
.print_loop_outer:
	; val = main_sieve[i]
	MOV BL, [MAIN_SIEVE + I]
	
	MOV J, 0
.print_loop_inner:
	; if(val & 1 != 0) print_num(x)
	MOV CL, BL
	AND C, 1
	JZ .dont_print
	
	; value
	MOV [IOMC_ADDRESS + 0], A
	MOV [IOMC_ADDRESS + 5], AL
	
	; newline
	MOV D, 0x0A
	MOV [IOMC_ADDRESS + 0], DL
	MOV [IOMC_ADDRESS + 4], DL

.dont_print:
	; val >>= 1
	SHR B, 1
	
	; x += 1
	INC A
	
	; j++
	INC J
	
	; j < 8
	CMP J, 8
	JL .print_loop_inner
	
	; i++
	INC I
	
	; i < 128
	CMP I, 128
	JL .print_loop_outer
	
	POP I
	
	; chunk += 1024, end += 1024
	MOV A, [BP - CHUNK_OFFSET]
	ADD A, 1024
	MOV [BP - CHUNK_OFFSET], A
	
	MOV A, [BP - END_OFFSET]
	ADD A, 1024
	MOV [BP - END_OFFSET], A
	
	JNZ .main_loop ; end > 0
	
	; done
	PUSH ptr msg_done
	CALL func_printstr
	ADD SP, 4
	
	; cleanup
	ADD SP, 4
	POP J
	POP I
	POP BP
	
	RET

func_printstr:
	PUSH BP
	MOV BP, SP
	
	PUSH I
	PUSH J
	
	; get arg
	MOVW J:I, [BP + 8]
	MOVZ B:C, IOMC_ADDRESS
	
.loop:
	MOV AL, [J:I]
	CMP AL, 0x00
	JE .end
	
	INC I
	ICC J
	MOV [B:C + 0], AL
	MOV [B:C + 4], AL
	JMP .loop

.end:
	POP J
	POP I
	POP BP
	RET

; data
msg_sieving:	db "Sieving ", 0x00
msg_to:			db " to ", 0x00
msg_done:		db "Done.", 0x0A, 0x00