
;
; [[ Snake Renderer ]]
; Functions for drawing the snake and apples to the screen
; Constants/implementation set for:
;	320x240 pixels
;	Display at address 0xC000_0000-0xC000_257F
;	3x3 pixel snake
;	1x1 pixel apple
;

;
; [ LIBRARY INFO ]
;	Functions
;		void func_snake_draw(int8 x, int8 y)
;		void func_snake_clear(int8 x, int8 y)
;		void func_apple_draw(int8 x, int8 y)
;		void func_apple_clear(int8 x, int8 y)
;

; Primary definitions
%define SCREEN_WIDTH 320
%define SCREEN_HEIGHT 240
%define SCREEN 0xC000_0000
%define SNAKE_BLOCK_MASK 0xE0
%define SNAKE_BLOCK_SIZE 3
%define APPLE_BLOCK_MASK 0x80
%define APPLE_BLOCK_SIZE 1

; Secondary definitions
%define SCREEN_WIDTH_BYTES (SCREEN_WIDTH / 8)

; function int32 calc_screen_offset(int8 x, int8 y)
; calculates the offset and shift value of a given position
; returns the shift offset in AH and the offset in D
func_calc_screen_offset:
	PUSH BP
	MOV BP, SP
	
	; D  = adr = (x / 8) + (y * SCREEN_WIDTH_BYTES)
	; AH = shift_offset = x % 8
	MOVZ A, byte [BP + 8]	; x
	MOVZ D, byte [BP + 9]	; y
	
.main:
	DIVM A, 8
	MULH D, SCREEN_WIDTH_BYTES
	ADD DL, AL
	ICC DH
	
	POP BP
	RET
	
	
; function int32 gen_block_info(int8 x, int8 y, int8 mask)
; generates the screen offset and bit mask for the block at the given position
; returns bit mask in D and offset in A
func_gen_block_info:
	PUSH BP
	MOV BP, SP
	
	; calc_screen_offset(x, y)
	PUSH word [BP + 8]
	CALL func_calc_screen_offset
	ADD SP, 2
	
	; shift mask into place
	MOV C, 0
	MOV CH, [BP + 10]
.shift_loop:
	SHR C, 1
	DEC AH
	JNZ .shift_loop
	
	; done
	MOV A, B
	MOV D, C
	POP BP
	RET
	

; function void block_draw(int8 x, int8 y, int8 mask, int8 size)
; draws a block at the given x and y
func_block_draw:
	PUSH BP
	MOV BP, SP
	
	; use info generator
	; A = offset, D = mask
	PUSH byte [BP + 10]
	PUSH word [BP + 8]
	CALL func_gen_block_info
	ADD SP, 3
	
	; or mask with current screen
	MOVZ C, byte [BP + 11]
.mask_loop:
	MOV B, [SCREEN + A]			; load
	OR B, D						; mask
	MOV [SCREEN + A], B			; store
	ADD A, SCREEN_WIDTH_BYTES	; increment
	DEC C
	JNZ .mask_loop
	
	; done
	POP BP
	RET
	
	
; function void block_clear(int8 x, int8 y, int8 mask, int8 size)
; clears a block at the given x and y
func_block_clear:
	PUSH BP
	MOV BP, SP
	
	; use info generator
	; A = offset, D = mask
	PUSH byte [BP + 10]
	PUSH word [BP + 8]
	CALL func_gen_block_info
	ADD SP, 3
	
	; AND inverse of mask to clear its bits
	NOT D
	MOVZ C, byte [BP + 11]
.mask_loop:
	MOV B, [SCREEN + A]			; load
	AND B, D					; mask
	MOV [SCREEN + A], B			; store
	ADD A, SCREEN_WIDTH_BYTES	; increment
	DEC C
	JNZ .mask_loop
	
	; done
	POP BP
	RET
	

; function void snake_draw(int8 x, int8 y)
func_snake_draw:
	PUSH BP
	MOV BP, SP
	
	PUSH word (SNAKE_BLOCK_SIZE * 256) + SNAKE_BLOCK_MASK
	PUSH word [BP + 8]
	CALL func_block_draw
	ADD SP, 4
	
	POP BP
	RET


; function void snake_clear(int8 x, int8 y)
func_snake_clear:
	PUSH BP
	MOV BP, SP
	
	PUSH word (SNAKE_BLOCK_SIZE * 256) + SNAKE_BLOCK_MASK
	PUSH word [BP + 8]
	CALL func_block_clear
	ADD SP, 4
	
	POP BP
	RET
	

; function void apple_draw(int8 x, int8 y)
func_apple_draw:
	PUSH BP
	MOV BP, SP
	
	PUSH word (APPLE_BLOCK_SIZE * 256) + APPLE_BLOCK_MASK
	PUSH word [BP + 8]
	CALL func_block_draw
	ADD SP, 4
	
	POP BP
	RET
	

; function void apple_clear(int8 x, int8 y)
func_apple_clear:
	PUSH BP
	MOV BP, SP
	
	PUSH word (APPLE_BLOCK_SIZE * 256) + APPLE_BLOCK_MASK
	PUSH word [BP + 8]
	CALL func_block_clear
	ADD SP, 4
	
	POP BP
	RET