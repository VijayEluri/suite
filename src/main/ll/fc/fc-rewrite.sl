fc-rewrite (ATOM .a) (ATOM .a) .ts/.ts
#
fc-rewrite (BOOLEAN .b) (BOOLEAN .b) .ts/.ts
#
fc-rewrite (CHARS .cs) (CHARS .cs) .ts/.ts
#
fc-rewrite (DEF-VARS (.var .value0, .list0) .do0) (DEF-VARS (.var .value1, .list1) .do1) .ts0/.tsx
	:- .ts0 = (.value0 .value1, .ts1)
	, fc-rewrite (DEF-VARS .list0 .do0) (DEF-VARS .list1 .do1) .ts1/.tsx
#
fc-rewrite (DEF-VARS () .do0) (DEF-VARS () .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite ERROR ERROR .ts/.ts
#
fc-rewrite (FUN .var .do0) (FUN .var .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (IF .if0 .then0 .else0) (IF .if1 .then1 .else1) .ts0/.tsx
	:- .ts0 = (.if0 .if1, .then0 .then1, .else0 .else1, .tsx)
#
fc-rewrite (INVOKE .param0 .callee0) (INVOKE .param1 .callee1) .ts0/.tsx
	:- .ts0 = (.param0 .param1, .callee0 .callee1, .tsx)
#
fc-rewrite (NUMBER .i) (NUMBER .i) .ts/.ts
#
fc-rewrite (PAIR .left0 .right0) (PAIR .left1 .right1) .ts0/.tsx
	:- .ts0 = (.left0 .left1, .right0 .right1, .tsx)
#
fc-rewrite (PRAGMA .pragma0 .do0) (PRAGMA .pragma1 .do1) .ts0/.tsx
	:- fc-rewrite-pragma .pragma0 .pragma1 .ts0/.ts1
	, .ts1 = (.do0 .do1, .tsx)
#
fc-rewrite (TCO .iter0 .in0) (TCO .iter1 .in1) .ts0/.tsx
	:- .ts0 = (.iter0 .iter1, .in0 .in1, .tsx)
#
fc-rewrite (TREE .oper .left0 .right0) (TREE .oper .left1 .right1) .ts0/.tsx
	:- .ts0 = (.left0 .left1, .right0 .right1, .tsx)
#
fc-rewrite (USING .mode .linkOption .m .do0) (USING .mode .linkOption .m .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (UNWRAP .do0) (UNWRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#
fc-rewrite (VAR .var) (VAR .var) .ts/.ts
#
fc-rewrite (WRAP .do0) (WRAP .do1) .ts0/.tsx
	:- .ts0 = (.do0 .do1, .tsx)
#

fc-rewrite-pragma DEF-OUTSIDE DEF-OUTSIDE .ts/.ts
#
fc-rewrite-pragma (DEF-TYPE .type0 .class0) (DEF-TYPE .type1 .class1) .ts/.ts
	:- fc-rewrite-type .type0 .type1
	, fc-rewrite-type .class0 .class1
#
fc-rewrite-pragma NEW NEW .ts/.ts
#
fc-rewrite-pragma (TYPE-CAST .type0) (TYPE-CAST .type1) .ts/.ts
	:- fc-rewrite-type .type0 .type1
#
fc-rewrite-pragma TYPE-CAST-TO-CLASS TYPE-CAST-TO-CLASS .ts/.ts
#
fc-rewrite-pragma TYPE-RESOLVE TYPE-RESOLVE .ts/.ts
#
fc-rewrite-pragma TYPE-SKIP-CHECK TYPE-SKIP-CHECK .ts/.ts
#
fc-rewrite-pragma (TYPE-VERIFY .v0 .type0) (TYPE-VERIFY .v1 .type1) .ts0/.tsx
	:- fc-rewrite .v0 .v1 .ts0/.tsx
	, fc-rewrite-type .type0 .type1
	, !
#

fc-rewrite-type .unbound .unbound
	:- not bound .unbound, !
#
fc-rewrite-type (ATOM-OF .atom) (ATOM-OF .atom)
#
fc-rewrite-type BOOLEAN BOOLEAN
#
fc-rewrite-type (CLASS .t) (CLASS .t)
#
fc-rewrite-type (FUN-OF .paramType0 .returnType0) (FUN-OF .paramType1 .returnType1)
	:- fc-rewrite-type .paramType0 .paramType1
	, fc-rewrite-type .returnType0 .returnType1
#
fc-rewrite-type (FUNCTOR-OF .functor .type0) (FUNCTOR-OF .functor .type1)
	:- fc-rewrite-type .type0 .type1
#
fc-rewrite-type (LIST-OF .type0) (LIST-OF .type1)
	:- fc-rewrite-type .type0 .type1
#
fc-rewrite-type NUMBER NUMBER
#
fc-rewrite-type (PAIR-OF .leftType0 .rightType0) (PAIR-OF .leftType1 .rightType1)
	:- fc-rewrite-type .leftType0 .leftType1
	, fc-rewrite-type .rightType0 .rightType1
#
