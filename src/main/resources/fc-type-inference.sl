-------------------------------------------------------------------------------
-- Type inference predicates
--
-- Environment consists of:
-- .ue - dictionary of inside variables / their corresponding types
-- .ve - dictionary of outside variables / their corresponding types
-- .te - list of types / their corresponding belonging classes
-- .tr - type deduction rule to be assembled
--
-- Inside variables include parent function definitions and parameter variables
-- that do not need type specialization.
-- Outside variables are local variables that require type specialization.
--
-- Kinds of generic types:
-- - Generic type class, usually used in abstract data structures.
--   Written like B-TREE/:t.
--   Represented internally as (CLASS (PARAMETERIZED (VAR t) B-TREE)).
--   Resolved by binding the type structures.
-- - Generic type, usually used in method signatures.
--   Written like :t :- .t => .t.
--   Represented internally as (GENERIC-OF (VAR t) FUN-OF (VAR t) (VAR t)).
--   Resolved by SUB-SUPER-TYPES.
-- - Generic type caused by not enough variable information during type inference.
--   Any variable usage, in case having unbinded variables, will also be cloned.
--   Resolved by CLONE-TO-FROM-TYPES.
--

infer-type-rules () _ .tr/.tr () :- ! #
infer-type-rules (.e, .es) .env .tr0/.trx (.t, .ts)
	:- infer-type-rule .e .env .tr0/.tr1 .t
	, infer-type-rules .es .env .tr1/.trx .ts
#

infer-type-rule .p .env .tr/.tr .type
	:- find-simple-type .p .env .type, !
#
infer-type-rule (USING .lib .do) .env .tr/.tr .type
	:- !, load-precompiled-library .lib
	, infer-type-rule-using-libs (.lib,) .do .env .tr1/() .type
	, resolve-types .tr1
#
infer-type-rule (
	OPTION (DEF-TYPE .definedType .classes .typeVars) .do
) .ue/.ve/.te .tr .type
	:- !
	, .te1 = (.definedType/.classes/.typeVars, .te)
	, infer-type-rule .do .ue/.ve/.te1 .tr .type
#
infer-type-rule (DEF-VAR .name .value .do) .ue/.ve/.te .tr0/.trx .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, .env1 = .ue1/.ve/.te
	, once (infer-type-rule .value .env1 .tr0/.tr1 .varType
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .env1 .tr1/.trx .type
#
infer-type-rule (
	OPTION ALLOW-RECURSIVE-DEFINITION DEF-VAR .name .value .do
) .ue/.ve/.te .tr .type
	:- !
	, fc-dict-add .name/.varType .ue/.ue1
	, fc-dict-add .name/.varType .ve/.ve1
	, .insideEnv = .ue1/.ve/.te
	, .outsideEnv = .ue/.ve1/.te
	, once (infer-type-rule .value .insideEnv .vtr/() .varType
		, resolve-types .vtr
		; fc-error "at variable" .name
	)
	, infer-type-rule .do .outsideEnv .tr .type
#
infer-type-rule (OPTION CHECK-TUPLE-TYPE .tuple) .ue/.ve/.te .tr0/.trx .classType
	:- !
	, infer-type-rule .tuple .ue/.ve/.te .tr0/.tr1 .tupleType
	, .classType = CLASS _
	, .tr1 = (SUB-SUPER-TYPES .te .tupleType .classType, .trx)
#
infer-type-rule (FUN .var .do) .ue/.ve/.te .tr (FUN-OF .varType .type)
	:- !
	, fc-dict-add .var/.varType .ue/.ue1
	, infer-type-rule .do .ue1/.ve/.te .tr .type
#
infer-type-rule (INVOKE .param .callee) .ue/.ve/.te .tr0/.trx .type
	:- !
	, infer-type-rule .callee .ue/.ve/.te .tr0/.tr1 .funType
	, infer-type-rule .param .ue/.ve/.te .tr1/.tr2 .actualParamType
	, .tr2 = (SUB-SUPER-TYPES .te (FUN-OF .signParamType .type) .funType
		, SUB-SUPER-TYPES .te .actualParamType .signParamType
		, .trx
	)
#
infer-type-rule (IF .if .then .else) .env .tr0/.trx .type
	:- !
	, infer-type-rule .if .env .tr0/.tr1 BOOLEAN
	, infer-compatible-types .then .else .env .tr1/.trx .type
#
infer-type-rule (TREE .oper .left .right) .env .tr0/.trx .type
	:- member (' + ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.tr1 .type
	, .tr1 = (TYPE-IN-TYPES .type (NUMBER, STRING,), .trx)
	; member (' - ', ' * ', ' / ', ' %% ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx .type
	, .type = NUMBER
	; member (' = ', ' != ', ' > ', ' < ', ' >= ', ' <= ',) .oper, !
	, infer-compatible-types .left .right .env .tr0/.trx _
	, .type = BOOLEAN
#
infer-type-rule (TUPLE .name .elems) .env .tr (TUPLE-OF .name .types)
	:- !
	, infer-type-rules .elems .env .tr .types
#
infer-type-rule (OPTION (CAST .dir .type) .do) .ue/.ve/.te .tr0/.trx .type
	:- !, infer-type-rule .do .ue/.ve/.te .tr0/.tr1 .type0
	, once (
		.dir = DOWN, .subType = .type0, .superType = .type
		; .dir = UP, .subType = .type, .superType = .type0
	)
	, .tr1 = (SUB-SUPER-TYPES .te .subType .superType, .trx)
#
infer-type-rule (OPTION (AS .var .varType) .do) .ue/.ve/.te .tr .type
	:- !
	, fc-dict-get .ue .var/.varType
	, infer-type-rule .do .ue/.ve/.te .tr .type
#
infer-type-rule (OPTION _ .do) .env .tr .type
	:- !
	, infer-type-rule .do .env .tr .type
#
infer-type-rule (VAR .var) .ue/.ve/.te .tr0/.trx .type
	:- (fc-dict-get .ve .var/.varType
		, !, .tr0 = (CLONE-TO-FROM-TYPES .type .varType, .trx)
	)
	; !, fc-error "Undefined variable" .var
#

find-simple-type (OPTION RESOLVE-TYPES .do) .env .type
	:- infer-type-rule .do .env .tr/() .type
	, resolve-types .tr
#
find-simple-type (CONSTANT _) _ _ #
find-simple-type (BOOLEAN _) _ BOOLEAN #
find-simple-type (NUMBER _) _ NUMBER #
find-simple-type (STRING _) _ STRING #
find-simple-type (TUPLE () ()) _ (LIST-OF _) #
find-simple-type (OPTION NO-TYPE-CHECK _) _ _ #
find-simple-type (VAR .var) .ue/.ve/.te .type
	:- fc-dict-get .ue .var/.type
	; default-fun-type .var .type
#

infer-compatible-types .a .b .ue/.ve/.te .tr0/.trx .type
	:- infer-type-rule .a .ue/.ve/.te .tr0/.tr1 .type0
	, infer-type-rule .b .ue/.ve/.te .tr1/.tr2 .type1
	, .tr2 = (SUB-SUPER-TYPES .te .type0 .type
		, SUB-SUPER-TYPES .te .type1 .type
		, .trx
	)
#

resolve-types .tr :- resolve-types0 .tr, ! #
resolve-types _ :- fc-error "Unable to resolve types" #

-- When resolving types:
-- - Try bind equivalent sub-type to super-type relation;
--   - Do not resolve relation when both types are not clear;
--   - Generalize generic types to resolve;
--   - Try reduce to type classes to resolve;
--   - Try morph children types to resolve;
-- - Try bind generic-type and specialized-type relation;
-- - Try bind type choice relation.
resolve-types0 () :- ! #
resolve-types0 (DUMP .d, .tr1)
	:- !, dump .d, nl, resolve-types0 .tr1
#
resolve-types0 (SUB-SUPER-TYPES .te .t0 .t1, .tr1)
	:- !, resolve-sub-super-types .te .t0 .t1, resolve-types0 .tr1
#
resolve-types0 (CLONE-TO-FROM-TYPES .t0 .t1, .tr1)
	:- !, clone .t1 .t0, resolve-types0 .tr1
#
resolve-types0 (TYPE-IN-TYPES .t .ts, .tr1)
	:- !, member .ts .t, resolve-types0 .tr1
#
resolve-types0 _ :- !, fc-error "Not enough type information" #

resolve-sub-super-types _ .t .t #
resolve-sub-super-types .te .t0 .tx
	:- bound .t0
	, sub-super-type-pair .te .t0 .t1
	, resolve-sub-super-types .te .t1 .tx
	; bound .tx
	, sub-super-type-pair .te .t1 .tx
	, resolve-sub-super-types .te .t0 .t1
#

sub-super-type-pair .te .type1 .class1 -- reduce to type classes
	:- once (bound .type1; bound .class1)
	, member .te .type/.classes/.typeVars
	, member .classes .class
	, instantiate-type .typeVars .type/.class .type1/.class1
#
sub-super-type-pair .te .t0 .t1 -- morph children types to their supers
	:- bound .t0
	, children-of-type .t0 .t1 .ts/()
	, choose-one-pair .ts .childType0/.childType1
	, sub-super-type-pair .te .childType0 .childType1
#
sub-super-type-pair _ .t0 .t1 :- generic-specific-pair .t0 .t1 #
sub-super-type-pair _ .t0 .t1 :- generic-specific-pair .t1 .t0 #

generic-specific-pair (GENERIC-OF .typeVar .type) .t1
	:- bound .typeVar
	, replace .type/.t1 .typeVar/_
#

choose-one-pair (.t0/.t1, .ts) .t0/.t1 :- equate-pairs .ts #
choose-one-pair (.t/.t, .ts) .tr :- choose-one-pair .ts .tr #

equate-pairs () #
equate-pairs (.t/.t, .ts) :- equate-pairs .ts #

instantiate-type () .tc .tc #
instantiate-type (.typeVar, .typeVars) .tc0 .tcx
	:- replace  .tc0/.tc1 .typeVar/_
	, instantiate-type .typeVars .tc1 .tcx
#

children-of-types () () .pq/.pq :- ! #
children-of-types (.t0, .ts0) (.t1, .ts1) .pq0/.pqx
	:- .pq0 = (.t0/.t1, .pq1)
	, children-of-types .ts0 .ts1 .pq1/.pqx
#

children-of-type (FUN-OF .pt0 .rt0) (FUN-OF .pt1 .rt1) .pq0/.pqx
	:- !, .pq0 = (.pt0/.pt1, .rt0/.rt1, .pqx)
#
children-of-type (LIST-OF .t0) (LIST-OF .t1) .pq0/.pqx
	:- !, .pq0 = (.t0/.t1, .pqx)
#
children-of-type (TUPLE-OF .name .ts0) (TUPLE-OF .name .ts1) .pq
	:- !, children-of-types .ts0 .ts1 .pq
#
children-of-type .t .t .pq/.pq #

default-fun-type () (LIST-OF _) #
default-fun-type _compare (FUN-OF .type (FUN-OF .type NUMBER)) #
default-fun-type _cons (FUN-OF .type (FUN-OF (LIST-OF .type) (LIST-OF .type))) #
default-fun-type _lhead (FUN-OF (LIST-OF .type) .type) #
default-fun-type _log (FUN-OF .type .type) #
default-fun-type _log2 (FUN-OF (LIST-OF NUMBER) (FUN-OF .type .type)) #
default-fun-type _ltail (FUN-OF (LIST-OF .type) (LIST-OF .type)) #
default-fun-type _popen (FUN-OF (LIST-OF NUMBER) (FUN-OF (LIST-OF NUMBER) (LIST-OF NUMBER))) #
default-fun-type _prove (FUN-OF _ BOOLEAN) #
default-fun-type _subst (FUN-OF _ (FUN-OF _ _)) #
default-fun-type _thead (FUN-OF (TUPLE-OF _ (.type, _)) .type) #
default-fun-type _ttail (FUN-OF (TUPLE-OF .n (_, .types)) (TUPLE-OF .n .types)) #
default-fun-type error _ #
default-fun-type fgetc (FUN-OF _ (FUN-OF NUMBER NUMBER)) #
default-fun-type is-tree (FUN-OF (LIST-OF .type) BOOLEAN) #
default-fun-type is-tuple (FUN-OF (TUPLE-OF _ (_, _)) BOOLEAN) #
