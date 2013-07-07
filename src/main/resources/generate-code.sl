-------------------------------------------------------------------------------
-- code generator and peep hole optimizer

cg-optimize-segment .c/() .co0/.cox
	:- cg-optimize .c .co
	, append .co .cox .co0
#

cg-optimize .c0 .cx
	:- cg-optimize-dup-labels .c0 .c1
	, once (not is.compiled; pretty.print cg-optimize-tail-calls .c1 .cx, nl)
	, cg-optimize-tail-calls .c1 .cx
#

cg-optimize-dup-labels (.label LABEL, .label LABEL, .insts0) .insts1
	:- !, cg-optimize-dup-labels (.label LABEL, .insts0) .insts1
#
cg-optimize-dup-labels (.inst, .insts0) (.inst, .insts1)
	:- !, cg-optimize-dup-labels .insts0 .insts1
#
cg-optimize-dup-labels () () #

cg-optimize-jumps0 (.inst .inst, .insts) :- !, cg-optimize-jumps0 .insts #
cg-optimize-jumps0 () #

cg-optimize-jumps1 (.inst0, .insts0) (.inst1, .insts1)
	:- !
	, once (cg-jump-target-instruction .inst0 .inst1; .inst0 = .inst1)
	, cg-optimize-jumps1 .insts0 .insts1
#
cg-optimize-jumps1 () () #

cg-jump-target-instruction (_ JUMP _ JUMP .target) .inst1
	:- !, cg-jump-target-instruction (_ JUMP .target) .inst1
#
cg-jump-target-instruction (_ JUMP .redirInst) .redirInst
	:- cg-redirect-instruction .redirInst, !
#

cg-redirect-instruction (_ CALL _) #
cg-redirect-instruction (_ CALL-CLOSURE _) #
cg-redirect-instruction (_ CALL-REG _) #
cg-redirect-instruction (_ RETURN) #
cg-redirect-instruction (_ RETURN-VALUE _) #

cg-optimize-tail-calls .li0 .ri0
	:- cg-push-pop-pairs .li0/.li1 .li2/.li3 .ri1/.ri2 .ri0/.ri1
	, member (CALL/JUMP, CALL-REG/JUMP-REG,) .call/.jump
	, .li1 = (_ .call .target, .li2)
	, cg-is-restore-csp .li3/.li4 .ri2/.ri3
	, cg-is-returning .li4
	, .ri3 = (_ .jump .target, .ri4)
	, !
	, cg-optimize-tail-calls .li4 .ri4
#
cg-optimize-tail-calls (.inst, .insts0) (.inst, .insts1)
	:- !, cg-optimize-tail-calls .insts0 .insts1
#
cg-optimize-tail-calls () () #

cg-push-pop-pairs
(_ PUSH .reg, .i0)/.ix (_ POP-ANY, .j0)/.jx
(_ PUSH .reg, .k0)/.kx (_ POP-ANY, .l0)/.lx
	:- !, cg-push-pop-pairs .i0/.ix .j0/.jx .k0/.kx .l0/.lx
#
cg-push-pop-pairs .i/.i .j/.j .k/.k .l/.l #

cg-is-restore-csp (_ RESTORE-CSP .cspReg, .i)/.i (_ RESTORE-CSP .cspReg, .j)/.j :- ! #
cg-is-restore-csp .i/.i .j/.j #

cg-is-returning (.inst, .insts) :- cg-is-skip .inst, !, cg-is-returning .insts #
cg-is-returning (_ RETURN, _) #

cg-is-skip (_ LABEL) #
cg-is-skip (_ REMARK _) #

cg-generate-code .code :- cg-assign-line-numbers 0 .code, ! #

cg-assign-line-numbers _ () #
cg-assign-line-numbers .n (.n _, .insts)
	:- let .n1 (.n + 1), cg-assign-line-numbers .n1 .insts
#
