-- Assembler. Only supports 32-bit addressing.
--
-- as	= assembler
-- asi	= assemble instruction

asi:.s .ai .e1 :- asi0:.s .ai .e0/(), as-lets .e0 .e1 #

as-lets () () #
as-lets (.e0, .es0) (.e1, .es1) :- let .e1 .e0, as-lets .es0 .es1 #

asi0:.s .ai .e0/.ex :- asis:.s .ai .e0/.ex, ! #
asi0:_s .ai .e0/.ex :- asis:8 .ai .e0/.ex, ! #
asi0:16 .ai (+x66, .e1)/.ex :- asis:32 .ai .e1/.ex, ! #
asi0:32 .ai (+x66, .e1)/.ex :- asis:16 .ai .e1/.ex, ! #

asis:_s (_a ()) .e/.e #
asis:_s (_a AAA ()) (+x37, .e)/.e #
asis:.s (_a ADC (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x10 +x80 2 .e0/.ex #
asis:.s (_a ADD (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x00 +x80 0 .e0/.ex #
asis:_s (.a ADVANCE .a) .e/.e #
asis:.s (.a0 ADVANCE .a1) (0, .e1)/.ex :- .a0 < .a1, let .a (.a0 + 1), asis:.s (.a ADVANCE .a1) .e1/.ex #
asis:.s (_a AND (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x20 +x80 4 .e0/.ex #
asis:_s (_a AOP ()) (+x67, .e)/.e #
asis:.s (.a CALL .target) (+xE8, .e1)/.ex :- asi-jump-rel:.s .target .a 1 .rel, as-emit:.s .rel .e1/.ex #
asis:_s (_a CALL .rm) (+xFF, .e1)/.ex :- as-mod-num-rm:32 .rm 2 .e1/.ex #
asis:_s (_a CLI ()) (+xFA, .e)/.e #
asis:.s (_a CMP (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x38 +x80 07 .e0/.ex #
asis:_s (_a D8 .imm) .e0/.ex :- as-emit:8 .imm .e0/.ex #
asis:_s (_a D16 .imm) .e0/.ex :- as-emit:16 .imm .e0/.ex #
asis:_s (_a D32 .imm) .e0/.ex :- as-emit:32 .imm .e0/.ex #
asis:.s (_a DEC .op) .e0/.ex :- asi-1op:.s .op +x48 +xFE 1 .e0/.ex #
asis:.s (_a DIV .rm) .e0/.ex :- asi-rm:.s +xF6 .rm 6 .e0/.ex #
asis:_s (_a HLT ()) (+xF4, .e)/.e #
asis:.s (_a IDIV .rm) .e0/.ex :- asi-rm:.s +xF6 .rm 7 .e0/.ex #
asis:.s (_a IMUL .rm) .e0/.ex :- asi-rm:.s +xF6 .rm 5 .e0/.ex #
asis:.s (_a IMUL (.reg, .rm)) (+x0F, +xAF, .e1)/.ex :- as-rm-regwd:.s .rm .reg .e1/.ex #
asis:.s (_a IMUL (.reg, .rm, .imm)) (+x6B, .e1)/.ex :- as-imm:8 .imm, as-rm-regwd:.s .rm .reg .e1/.e2, as-emit:8 .imm .e2/.ex #
asis:.s (_a IMUL (.reg, .rm, .imm)) (+x69, .e1)/.ex :- as-imm:.s .imm, as-rm-regwd:.s .rm .reg .e1/.e2, as-emit:.s .imm .e2/.ex #
asis:.s (_a IN (.val, .port)) .e0/.ex :- asi-in-out:.s .val .port +xE4 .e0/.ex #
asis:.s (_a INC .op) .e0/.ex :- asi-1op:.s .op +x40 +xFE 0 .e0/.ex #
asis:_s (_a INT 3) (+xCC, .e)/.e #
asis:_s (_a INT .imm) (+xCD, .e0)/.ex :- as-emit:8 .imm .e0/.ex #
asis:_s (_a INTO ()) (+xCE, .e)/.e #
asis:_s (_a INVLPG .m) (+x0F, +x01, .e1)/.ex :- as-mod-num-rm:_ .m 7 .e1/.ex #
asis:_s (_a IRET ()) (+xCF, .e)/.e #
asis:_s (.a JE .target) .e0/.ex :- asi-jump .a .target +x74 +x0F +x84 .e0/.ex #
asis:_s (.a JG .target) .e0/.ex :- asi-jump .a .target +x7F +x0F +x8F .e0/.ex #
asis:_s (.a JGE .target) .e0/.ex :- asi-jump .a .target +x7D +x0F +x8D .e0/.ex #
asis:_s (.a JL .target) .e0/.ex :- asi-jump .a .target +x7C +x0F +x8C .e0/.ex #
asis:_s (.a JLE .target) .e0/.ex :- asi-jump .a .target +x7E +x0F +x8E .e0/.ex #
asis:_s (.a JMP .target) .e0/.ex :- asi-jump .a .target +xEB () +xE9 .e0/.ex #
asis:_s (_a JMP .rm) .e0/.ex :- as-mod-num-rm:32 +xFF .rm 4 .e0/.ex #
asis:_s (.a JNE .target) .e0/.ex :- asi-jump .a .target +x75 +x0F +x85 .e0/.ex #
asis:_s (.a JNZ .target) .e0/.ex :- asi-jump .a .target +x75 +x0F +x85 .e0/.ex #
asis:_s (.a JZ .target) .e0/.ex :- asi-jump .a .target +x74 +x0F +x84 .e0/.ex #
asis:_s (_a LEA (.reg, .rm)) (+x8D, .e1)/.ex :- as-rm-regwd:_ .rm .reg .e1/.ex #
asis:_s (.a LOOP .target) (+xE2, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s (.a LOOPE .target) (+xE1, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s (.a LOOPNE .target) (+xE0, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s (.a LOOPNZ .target) (+xE0, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:_s (.a LOOPZ .target) (+xE1, .e1)/.ex :- asi-jump8 .a .target .e1/.ex #
asis:.s (_a LGDT .rm) (+x0F, +x01, .e1)/.ex :- as-mod-num-rm:.s .rm 2 .e1/.ex #
asis:.s (_a LIDT .rm) (+x0F, +x01, .e1)/.ex :- as-mod-num-rm:.s .rm 3 .e1/.ex #
asis:_s (_a LTR .rm) (+x0F, +x00, .e1)/.ex :- as-mod-num-rm:16 .rm 3 .e1/.ex #
asis:.s (_a MOV (.reg, .imm)) .e0/.ex :- asi-reg-imm:.s +xB0 .reg .imm .e0/.ex #
asis:.s (_a MOV (.rm, .imm)) .e0/.ex :- asi-rm-imm:.s +xC6 .rm 0 .imm .e0/.ex #
asis:.s (_a MOV (.rm0, .rm1)) .e0/.ex :- asi-rm-reg2:.s +x88 .rm0 .rm1 .e0/.ex #
asis:_s (_a MOV (.rm, .sreg)) (+x8C, .e1)/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16 .rm .sr .e1/.ex #
asis:_s (_a MOV (.sreg, .rm)) (+x8E, .e1)/.ex :- as-segment-reg .sreg .sr, as-mod-num-rm:16 .rm .sr .e1/.ex #
asis:.s (_a MOVSX (.reg, .rm)) .e0/.ex :- asi-reg-rm-extended:.s .reg .rm +xBE .e0/.ex #
asis:.s (_a MOVZX (.reg, .rm)) .e0/.ex :- asi-reg-rm-extended:.s .reg .rm +xB6 .e0/.ex #
asis:.s (_a MUL .rm) .e0/.ex :- asi-rm:.s +xF6 .rm 4 .e0/.ex #
asis:_s (_a NOP ()) (+x90, .e)/.e #
asis:_s (_a OOP ()) (+x66, .e)/.e #
asis:.s (_a OR (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x08 +x80 1 .e0/.ex #
asis:.s (_a OUT (.port, .val)) .e0/.ex :- asi-in-out:.s .val .port +xE6 .e0/.ex #
asis:.s (_a POP .op) .e0/.ex :- asi-1op:.s .op +x58 +x8E 0 .e0/.ex, .s != 8 #
asis:_s (_a POP DS) (+x1F, .e)/.e #
asis:_s (_a POP ES) (+x07, .e)/.e #
asis:_s (_a POP FS) (+x0F, +xA1, .e)/.e #
asis:_s (_a POP GS) (+x0F, +xA9, .e)/.e #
asis:_s (_a POP SS) (+x17, .e)/.e #
asis:_s (_a POPA ()) (+x61, .e)/.e #
asis:_s (_a POPF ()) (+x9D, .e)/.e #
asis:.s (_a PUSH .imm) (.b, .e1)/.ex :- as-imm:.s .imm, as-emit:.s .imm .e1/.ex, if (.s = 8) (.b = +x6A) (.b = +x68) #
asis:.s (_a PUSH .op) .e0/.ex :- asi-1op:.s .op +x50 +xFE 6 .e0/.ex, .s != 8 #
asis:_s (_a PUSH CS) (+x0E, .e)/.e #
asis:_s (_a PUSH DS) (+x1E, .e)/.e #
asis:_s (_a PUSH ES) (+x06, .e)/.e #
asis:_s (_a PUSH FS) (+x0F, +xA0, .e)/.e #
asis:_s (_a PUSH GS) (+x0F, +xA8, .e)/.e #
asis:_s (_a PUSH SS) (+x16, .e)/.e #
asis:_s (_a PUSHA ()) (+x60, .e)/.e #
asis:_s (_a PUSHF ()) (+x9C, .e)/.e #
asis:_s (_a RET ()) (+xC3, .e)/.e #
asis:_s (_a RET .imm) (+xC2, .e1)/.ex :- as-emit:16 .imm .e1/.ex #
asis:.s (_a SAL (.rm, .op)) .e0/.ex :- asi-shift:.s .rm .op +xD0 +xC0 4 .e0/.ex #
asis:.s (_a SAR (.rm, .op)) .e0/.ex :- asi-shift:.s .rm .op +xD0 +xC0 7 .e0/.ex #
asis:.s (_a SBB (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x18 +x80 3 .e0/.ex #
asis:.s (_a SETG .rm) (+x0F, +x9F, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SETGE .rm) (+x0F, +x9D, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SETL .rm) (+x0F, +x9C, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SETLE .rm) (+x0F, +x9E, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SETE .rm) (+x0F, +x94, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SETNE .rm) (+x0F, +x95, .e1)/.ex :- as-mod-num-rm:.s .rm 0 .e1/.ex #
asis:.s (_a SHL (.rm, .op)) .e0/.ex :- asi-shift:.s .rm .op +xD0 +xC0 4 .e0/.ex #
asis:.s (_a SHR (.rm, .op)) .e0/.ex :- asi-shift:.s .rm .op +xD0 +xC0 5 .e0/.ex #
asis:_s (_a STI ()) (+xFB, .e)/.e #
asis:.s (_a SUB (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x28 +x80 5 .e0/.ex #
asis:.s (_a TEST (.acc, .imm)) (.b, .e1)/.ex :- as-reg:.s .acc 0, as-emit:.s .imm .e1/.ex, if (.s = 8) (.b = +xA8) (.b = +xA9) #
asis:.s (_a TEST (.rm, .imm)) .e0/.ex :- asi-rm-imm:.s +xF6 .rm 0 .imm .e0/.ex #
asis:.s (_a TEST (.rm, .reg)) .e0/.ex :- asi-rm-reg:.s +x84 .rm .reg .e0/.ex #
asis:.s (_a XCHG (.acc, .reg)) (.b, .e)/.e :- as-reg:.s .acc 0, as-reg:.s .reg .r, let .b (+x90 + .r) #
asis:.s (_a XCHG (.rm, .reg)) .e0/.ex :- asi-rm-reg:.s +x86 .rm .reg .e0/.ex #
asis:.s (_a XOR (.op0, .op1)) .e0/.ex :- asi-2op:.s .op0 .op1 +x30 +x80 6 .e0/.ex #

asi-jump .a (BYTE .target) .b _ _ (.b, .e1)/.ex
	:- asi-jump8 .a .target .e1/.ex, !
#
asi-jump .a (DWORD .target) _ () .b (.b, .e1)/.ex
	:- asi-jump-rel:32 .target .a 1 .rel, as-emit:32 .rel .e1/.ex, !
#
asi-jump .a (DWORD .target) _ .b0 .b1 (.b0, .b1, .e1)/.ex
	:- asi-jump-rel:32 .target .a 2 .rel, as-emit:32 .rel .e1/.ex, !
#
asi-jump .a .target .b _ _ (.b, .e1)/.ex
	:- asi-jump .a (BYTE .target) .b _ _ (.b, .e1)/.ex
#

asi-jump8 .a .target .e0/.ex
	:- asi-jump-rel:8 .target .a 1 .rel
	, if (as-imm:8 .rel) (as-emit:8 .rel .e0/.ex) (throw "Jumping too far")
#

asi-jump-rel:.size .target .a .f .rel
	:- if (bound .target) (is.int .target, let .rel (.target - .a - .f - .size / 8)) (.rel = 0)
#

asi-in-out:.size .acc .port .b0 (.b2, .e1)/.ex
	:- as-reg:.size .acc 0
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
	, if (.port = DX) (.e1 = .ex, .b2 = .b1 + 8) (as-emit:8 .port .e1/.ex, .b1 = .b2)
#

asi-shift:.size .rm .op .b0 _ .n .e0/.ex
	:- asi-rm:.size .b1 .rm .n .e0/.ex
	, (.op = 1, .b1 = .b0; .op = CL, .b1 = .b0 + 2)
#
asi-shift:.size .rm .imm8 _ .b0 .n (.b1, .e1)/.ex
	:- asi-rm-imm8:.size .rm .imm8 .n .e1/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

-- Common single-operand instructions, like DEC, NEG
asi-1op:.size .reg .b _ _ .e0/.ex :- asi-reg:.size .b .reg .e0/.ex, .size != 8 #
asi-1op:.size .rm _ .b .n .e0/.ex :- asi-rm:.size .b .rm .n .e0/.ex #

-- Common two-operand instructions, like ADD, OR, XOR
asi-2op:.size .acc .imm .b0 _ _ .e0/.ex
	:- asi-acc-imm:.size .b1 .acc .imm .e0/.ex
	, .b1 = .b0 + 4
#
asi-2op:.size .rm0 .rm1 .b _ _ .e0/.ex
	:- asi-rm-reg2:.size .b .rm0 .rm1 .e0/.ex
#
asi-2op:.size .rm .imm8 _ .b0 .n (.b1, .e1)/.ex
	:- asi-rm-imm8:.size .rm .imm8 .n .e1/.ex
	, .b1 = .b0 + 3
#
asi-2op:.size .rm .imm _ .b .n .e0/.ex
	:- asi-rm-imm:.size .b .rm .n .imm .e0/.ex
#

asi-reg-rm-extended:.size .reg .rm .b0 (+x0F, .b1, .e1)/.ex
	:- as-reg:.size .reg .r
	, as-mod-num-rm:.size1 .rm .r .e1/.ex
	, if (.size1 = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-rm-imm8:.size .rm .imm8 .n .e0/.ex
	:- as-imm:8 .imm8
	, as-mod-num-rm:.size .rm .n .e0/.e1
	, as-emit:8 .imm8 .e1/.ex
#

asi-rm-imm:.size .b0 .rm .num .imm (.b1, .e1)/.ex
	:- as-mod-num-rm:.size .rm .num .e1/.e2
	, as-emit:.size .imm .e2/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-acc-imm:.size .b0 .acc .imm (.b1, .e1)/.ex
	:- as-reg:.size .acc 0
	, as-emit:.size .imm .e1/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-reg-imm:.size .b0 .reg .imm .e0/.ex
	:- asi-reg:.size .b1 .reg .e0/.e1
	, as-emit:.size .imm .e1/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 8)
#

asi-rm-reg2:.size .b .rm .reg .e0/.ex
	:- asi-rm-reg:.size .b .rm .reg .e0/.ex
#
asi-rm-reg2:.size .b0 .reg .rm .e0/.ex
	:- asi-rm-reg:.size .b1 .rm .reg .e0/.ex
	, .b1 = .b0 + 2
#

asi-rm-reg:.size .b0 .rm .reg (.b1, .e1)/.ex
	:- as-rm-regwd:.size .rm .reg .e1/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-rm:.size .b0 .rm .num (.b1, .e1)/.ex
	:- as-mod-num-rm:.size .rm .num .e1/.ex
	, if (.size = 8) (.b1 = .b0) (.b1 = .b0 + 1)
#

asi-reg:.size .b0 .reg (.b1, .e)/.e
	:- as-reg:.size .reg .r
	, .b1 = .b0 + .r
#

as-rm-regwd:.size .rm .reg .e0/.ex
	:- as-reg:.size .reg .r
	, as-mod-num-rm:.size .rm .r .e0/.ex
#

as-mod-num-rm:.size .rm .num (.modregrm, .e1)/.ex
	:- is.int .num
	, once (as-mod-rm:.size .rm (.modb .rmb) .e1/.ex)
	, let .modregrm (.modb * 64 + .num * 8 + .rmb)
#

as-mod-rm:.size .reg (3 .r) .e/.e
	:- as-reg:.size .reg .r
#
as-mod-rm:_ (`.disp`) (0 5) .e0/.ex
	:- as-emit:32 .disp .e0/.ex
#
as-mod-rm:_ (`.reg + .disp`) (.mod .r) .e0/.ex
	:- as-reg:32 .reg .r
	, as-disp-mod .disp .mod .e0/.ex
	, not (.r = 4; .mod = 0, .r = 5; .r = 12; .mod = 0, .r = 13)
#
as-mod-rm:_ (`.indexReg * .scale + .baseReg + .disp`) (.mod 4) (.sib, .e1)/.ex
	:- as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-disp-mod .disp .mod .e1/.ex
#
as-mod-rm:8 (BYTE `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:16 (WORD `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:32 (DWORD `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:64 (QWORD `.ptr`) (.mod .rm) .e :- as-mod-rm:_ `.ptr` (.mod .rm) .e #
as-mod-rm:_ (`.reg`) (.mod .rm) .e
	:- as-reg:32 .reg _
	, as-mod-rm:_ (`.reg + 0`) (.mod .rm) .e
#
as-mod-rm:_ (`.indexReg * .scale + .baseReg`) (.mod .rm) .e
	:- as-mod-rm:_ (`.indexReg * .scale + .baseReg + 0`) (.mod .rm) .e
#

as-disp-mod .disp .mod .e0/.ex
	:- .disp = 0, .mod = 0, .e0 = .ex
	; as-imm:8 .disp, .mod = 1, as-emit:8 .disp .e0/.ex
	; .mod = 2, as-emit:32 .disp .e0/.ex
#

as-sib (`.indexReg * .scale + .baseReg`) .sib
	, as-reg:32 .indexReg .ir
	, as-sib-scale .scale .s
	, as-reg:32 .baseReg .br
	, let .sib (.s * 64 + .ir * 8 + .br)
#

as-sib-scale 1 0 #
as-sib-scale 2 1 #
as-sib-scale 4 2 #
as-sib-scale 8 3 #

as-emit:32 .d32 (0, 0, 0, 0, .e)/.e :- not bound .d32, ! #
as-emit:32 .d32 .e0/.ex
	:- is.int .d32
	, let .w0 (.d32 and +xFFFF)
	, let .w1 (.d32 shr 16)
	, as-emit:16 .w0 .e0/.e1
	, as-emit:16 .w1 .e1/.ex
#

as-emit:16 .w16 (0, 0, .e)/.e :- not bound .w16, ! #
as-emit:16 .w16 .e0/.ex
	:- is.int .w16
	, let .b0 (.w16 and +xFF)
	, let .b1 (.w16 shr 8)
	, as-emit:8 .b0 .e0/.e1
	, as-emit:8 .b1 .e1/.ex
#

as-emit:8 .b8 (0, .e)/.e :- not bound .b8, ! #
as-emit:8 .b8 (.b8, .e)/.e :- is.int .b8 #

as-imm:8 .imm :- is.int .imm, -128 <= .imm, .imm < 128 #
as-imm:16 .imm :- is.int .imm, -32768 <= .imm, .imm < 32768 #
as-imm:32 .imm :- is.int .imm #

as-reg:8 AL 0 #
as-reg:8 CL 1 #
as-reg:8 DL 2 #
as-reg:8 BL 3 #
as-reg:8 AH 4 :- not as-mode-amd64 #
as-reg:8 CH 5 :- not as-mode-amd64 #
as-reg:8 DH 6 :- not as-mode-amd64 #
as-reg:8 BH 7 :- not as-mode-amd64 #
as-reg:8 R4B 4 :- as-mode-amd64 #
as-reg:8 R5B 5 :- as-mode-amd64 #
as-reg:8 R6B 6 :- as-mode-amd64 #
as-reg:8 R7B 7 :- as-mode-amd64 #
as-reg:8 R8B 8 :- as-mode-amd64 #
as-reg:8 R9B 9 :- as-mode-amd64 #
as-reg:8 R10B 10 :- as-mode-amd64 #
as-reg:8 R11B 11 :- as-mode-amd64 #
as-reg:8 R12B 12 :- as-mode-amd64 #
as-reg:8 R13B 13 :- as-mode-amd64 #
as-reg:8 R14B 14 :- as-mode-amd64 #
as-reg:8 R15B 15 :- as-mode-amd64 #
as-reg:16 AX 0 #
as-reg:16 CX 1 #
as-reg:16 DX 2 #
as-reg:16 BX 3 #
as-reg:16 SP 4 #
as-reg:16 BP 5 #
as-reg:16 SI 6 #
as-reg:16 DI 7 #
as-reg:16 R8W 8 :- as-mode-amd64 #
as-reg:16 R9W 9 :- as-mode-amd64 #
as-reg:16 R10W 10 :- as-mode-amd64 #
as-reg:16 R11W 11 :- as-mode-amd64 #
as-reg:16 R12W 12 :- as-mode-amd64 #
as-reg:16 R13W 13 :- as-mode-amd64 #
as-reg:16 R14W 14 :- as-mode-amd64 #
as-reg:16 R15W 15 :- as-mode-amd64 #
as-reg:32 EAX 0 #
as-reg:32 ECX 1 #
as-reg:32 EDX 2 #
as-reg:32 EBX 3 #
as-reg:32 ESP 4 #
as-reg:32 EBP 5 #
as-reg:32 ESI 6 #
as-reg:32 EDI 7 #
as-reg:32 R8D 8 :- as-mode-amd64 #
as-reg:32 R9D 9 :- as-mode-amd64 #
as-reg:32 R10D 10 :- as-mode-amd64 #
as-reg:32 R11D 11 :- as-mode-amd64 #
as-reg:32 R12D 12 :- as-mode-amd64 #
as-reg:32 R13D 13 :- as-mode-amd64 #
as-reg:32 R14D 14 :- as-mode-amd64 #
as-reg:32 R15D 15 :- as-mode-amd64 #
as-reg:64 RAX 0 #
as-reg:64 RCX 1 #
as-reg:64 RDX 2 #
as-reg:64 RBX 3 #
as-reg:64 RSP 4 #
as-reg:64 RBP 5 #
as-reg:64 RSI 6 #
as-reg:64 RDI 7 #
as-reg:64 R8 8 :- as-mode-amd64 #
as-reg:64 R9 9 :- as-mode-amd64 #
as-reg:64 R10 10 :- as-mode-amd64 #
as-reg:64 R11 11 :- as-mode-amd64 #
as-reg:64 R12 12 :- as-mode-amd64 #
as-reg:64 R13 13 :- as-mode-amd64 #
as-reg:64 R14 14 :- as-mode-amd64 #
as-reg:64 R15 15 :- as-mode-amd64 #

as-segment-reg ES 0 #
as-segment-reg CS 1 #
as-segment-reg SS 2 #
as-segment-reg DS 3 #
as-segment-reg FS 4 #
as-segment-reg GS 5 #

as-control-reg CR0 0 #
as-control-reg CR2 2 #
as-control-reg CR3 3 #
as-control-reg CR4 4 #

as-error .m :- !, write.error .m, nl, fail #
