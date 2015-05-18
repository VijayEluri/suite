-- in :: Param
-- iter :: Param -> (Either Param Value)
-- tco :: (Param -> (Either Param Value)) -> Param -> Value

fc-reduce-tail-call (DEF-VARS (.fun .do0,) .value0) (DEF-VARS (.fun .dox,) .valuex)
	:- fc-reduce-tail-call0 .fun ()/.vars .do0 .iter .flag
	, bound .flag
	, !
	, temp .in
	, fc-vars-expression .vars .expr0
	, fc-bind-expression .vars .expr1
	, fc-bind (VAR .in) .expr1 .iter ERROR .do1
	, fc-reduce-tail-call .do1 .do2
	, fc-vars-fun .vars (TCO (FUN .in .do2) .expr0) .dox
	, fc-reduce-tail-call .value0 .valuex
#
fc-reduce-tail-call .do0 .dox :- fc-rewrite .do0 .dox .ts/(), fc-reduce-tail-call-list .ts
#

fc-reduce-tail-call-list () #
fc-reduce-tail-call-list (.t0 .tx, .ts) :- fc-reduce-tail-call .t0 .tx, fc-reduce-tail-call-list .ts #

fc-reduce-tail-call0 .fun .vars/.vars .do (PAIR (BOOLEAN false) PAIR .expr (PRAGMA TYPE-SKIP-CHECK NUMBER 0)) Y
	:- length .vars .length
	, length .values .length
	, fc-expression .values .expr
	, fc-values-invoke .values (VAR .fun) .do
	, !
#
fc-reduce-tail-call0 .fun .vars0/.varsx (FUN .var .do) .pair .flag
	:- !
	, .vars1 = (.var, .vars0)
	, fc-reduce-tail-call0 .fun .vars1/.varsx .do .pair .flag
#
fc-reduce-tail-call0 .fun .vars (PRAGMA .pragma .do0) (PRAGMA .pragma .dox) .flag
	:- !
	, fc-reduce-tail-call0 .fun .vars .do0 .dox .flag
#
fc-reduce-tail-call0 .fun .vars (IF .if .then .else) (IF .if .then1 .else1) .flag
	:- !
	, fc-reduce-tail-call0 .fun .vars .then .then1 .flag
	, fc-reduce-tail-call0 .fun .vars .else .else1 .flag
#
fc-reduce-tail-call0 _ .vars/.vars .do (PAIR (BOOLEAN true) PAIR (PRAGMA TYPE-SKIP-CHECK NUMBER 0) .do) _
#

fc-expression () (ATOM ()) #
fc-expression (.var, .vars) (PAIR .var .vars1) :- fc-expression .vars .vars1 #

fc-vars-expression () (ATOM ()) #
fc-vars-expression (.var, .vars) (PAIR (VAR .var) .vars1) :- fc-vars-expression .vars .vars1 #

fc-bind-expression () (ATOM ()) #
fc-bind-expression (.var, .vars) (PAIR (NEW-VAR .var) .vars1) :- fc-bind-expression .vars .vars1 #

fc-vars-fun () .do .do #
fc-vars-fun (.var, .vars) .do .do1 :- fc-vars-fun .vars (FUN .var .do) .do1 #

fc-values-invoke () .do .do #
fc-values-invoke (.value, .values) .do (INVOKE .value .do1) :- fc-values-invoke .values .do .do1 #
