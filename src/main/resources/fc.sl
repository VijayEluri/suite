-------------------------------------------------------------------------------
-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl
--
-- .mode can be EAGER or LAZY

() :- import.file 'fc-infer-type.sl'
	, import.file 'fc-parse.sl'
	, import.file 'generate-code.sl'
	, import.file 'rbt.sl'
#

compile-function-without-precompile .mode (.lib, .libs) .do .c
	:- !, load-library .lib
	, fc-add-functions .lib .do .do1
	, compile-function-without-precompile .mode .libs .do1 .c
#
compile-function-without-precompile .mode () .do .c
	:- compile-function .mode .do .c
#

compile-function .mode .do .c0
	:- .c0 = (_ ENTER, .c1)
	, !, fc-parse .do .parsed
	, !, infer-type-rule .parsed ()/()/() .tr/() _
	, !, resolve-type-rules .tr
	, !, fc-compile .mode .parsed 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ RETURN-VALUE .reg, _ LEAVE, .d0)
	, !, cg-generate-code .c0
#

fc-compile .mode (USING .lib .do) .fve .cdr
	:- !, load-precompiled-library .lib
	, fc-compile-using-lib .mode .lib .do .fve .cdr
#

load-precompiled-library .lib
	:- once (fc-imported-precompile-library .lib
		; home.dir .homeDir
		, concat .homeDir "/" .lib ".rpn" .rpnFilename
		, file.read .rpnFilename .rpn
		, rpn .precompiled .rpn
		, import .precompiled
	)
#

load-library .lib
	:- once (fc-imported .lib
		; home.dir .homeDir
		, concat .homeDir "/src/main/resources/" .lib ".slf" .slfFilename
		, whatever (file.exists .slfFilename
			, file.read .slfFilename .slf
			, to.atom ".p" .var
			, concat .slf .var .slf1
			, parse .slf1 .node
			, assert (fc-add-functions .lib .var .node)
			, assert (fc-imported-library .lib)
		)
	)
#

fc-frame-difference .frame0 .frame1 0 :- same .frame0 .frame1, ! #
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not is.tree .frame0, !
	, fc-frame-difference .frame0 .frame1 .frameDiff0
	, let .frameDiff (.frameDiff0 - 1)
#
fc-frame-difference (.frame0 + 1) (.frame1 + 1) .frameDiff
	:- !, fc-frame-difference .frame0 .frame1 .frameDiff
#

fc-define-default-fun 2 _compare COMPARE #
fc-define-default-fun 2 _lcons CONS-LIST #
fc-define-default-fun 1 _lhead HEAD #
fc-define-default-fun 1 _log LOG1 #
fc-define-default-fun 2 _log2 LOG2 #
fc-define-default-fun 1 _ltail TAIL #
fc-define-default-fun 2 _pcons CONS-PAIR #
fc-define-default-fun 1 _pleft HEAD #
fc-define-default-fun 2 _popen POPEN #
fc-define-default-fun 1 _pright TAIL #
fc-define-default-fun 1 _prove PROVE #
fc-define-default-fun 2 _subst SUBST #
fc-define-default-fun 0 error ERROR #
fc-define-default-fun 2 fgetc FGETC #
fc-define-default-fun 1 is-list IS-TREE #
fc-define-default-fun 1 is-pair IS-TREE #

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
		'.',
	) .oper
#

fc-error .m :- !, write .m, nl, fail #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-merge-replace .t0 .t1 .t2 :- rbt-merge-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #

fc-add-functions STANDARD .p (
	define compare = (a => b => _compare {a} {b}) >>
	define cons = (head => tail => _lcons {head} {tail}) >>
	define head = (list => _lhead {list}) >>
	define log = (m => _log {m}) >>
	define log2 = (m => n => _log2 {m} {n}) >>
	define prove = (goal => _prove {goal}) >>
	define subst = (var => node => _subst {var} {node}) >>
	define tail = (list => _ltail {list}) >>
	define tuple-head = (tuple => _pleft {tuple}) >>
	define tuple-tail = (tuple => _pright {tuple}) >>
	define and = (x => y =>
		if x then y else false
	) >>
	define drop = (n => list =>
		if:: n > 0 && is-list {list}
		then:: list | tail | drop {n - 1}
		else:: list
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold-left = (fun => init =>
		match
		|| $h; $t => fold-left {fun} {fun {init} {h}} {t}
		|| otherwise init
	) >>
	define fold-right = (fun => init =>
		match
		|| $h; $t => fun {h} {fold-right {fun} {init} {t}}
		|| otherwise init
	) >>
	define greater = (a => b =>
		if (a > b) then a else b
	) >>
	define id = (v =>
		v
	) >>
	define invoke = (f => x =>
		f {x}
	) >>
	define lesser = (a => b =>
		if (a > b) then b else a
	) >>
	define not = (x =>
		if x then false else true
	) >>
	define or = (x => y =>
		if x then true else y
	) >>
	define repeat = (n => elem =>
		if (n > 0) then (elem; repeat {n - 1} {elem}) else ()
	) >>
	define scan-left = (fun => init =>
		match
		|| $h; $t => init; scan-left {fun} {fun {init} {h}} {t}
		|| otherwise (init;)
	) >>
	define scan-right = (fun => init =>
		match
		|| $h; $t =>
			let r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}; r
		|| otherwise (init;)
	) >>
	define source = (is =>
		let fgets = (pos =>
			let c = fgetc {is} {pos} >>
			if (c >= 0) then (c; fgets {pos + 1}) else ()
		) >>
		fgets {0}
	) >>
	define str-to-int = (s =>
		let unsigned-str-to-int = fold-left {v => d => v * 10 + d - 48} {0} >>
			if:: is-list {s} && head {s} = 45
			then:: `0 - ` . unsigned-str-to-int . tail
			else:: unsigned-str-to-int
		{s}
	) >>
	define tails = (
		match
		|| $h; $t => (h; t); tails {t}
		|| otherwise (;)
	) >>
	define take = (n => list =>
		if:: n > 0 && is-list {list}
		then:: list | tail | take {n - 1} | cons {list | head}
		else:: ()
	) >>
	define take-while = (fun =>
		match
		|| $elem; $elems =>
			if (fun {elem}) then (elem; take-while {fun} {elems}) else ()
		|| otherwise ()
	) >>
	define tget0 =
		tuple-head
	>>
	define tget1 =
		tuple-head . tuple-tail
	>>
	define tget2 =
		tuple-head . tuple-tail . tuple-tail
	>>
	define unfold-right = (fun => init =>
		let r = fun {init} >>
		if:: is-list {r}
		then:: r | tail | head | unfold-right {fun} | cons {r | head}
		else:: ()
	) >>
	define zip = (fun =>
		match
		|| $h0; $t0 => (
			match
			|| $h1; $t1 => fun {h0} {h1}; zip {fun} {t0} {t1}
			|| otherwise ()
		)
		|| otherwise (anything => ())
	) >>
	define append = (
		match
		|| $h; $t => cons {h} . append {t}
		|| otherwise id
	) >>
	define apply =
		fold-right {`.`} {id}
	>>
	define fold = (fun => list =>
		fold-left {fun} {list | head} {list | tail}
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item; list) else list
		} {}
	) >>
	define get = (n =>
		head . (tail | repeat {n} | apply)
	) >>
	define length =
		fold-left {v => e => v + 1} {0}
	>>
	define map = (fun =>
		fold-right {i => list => fun {i}; list} {}
	) >>
	define popen = (command => in =>
		in | _popen {command} | source
	) >>
	define reverse =
		fold-left {cons/} {}
	>>
	define substring = (start => end => list =>
		let len = length {list} >>
		let s = (if (start >= 0) then start else (len + start)) >>
		let e = (if (end > 0) then end else (len + end)) >>
		list | take {e} | drop {s}
	) >>
	define uniq =
		fold-right {item => list =>
			if-bind (list = (item; $t)) then list else (item; list)
		} {}
	>>
	define concat =
		fold-left {append} {}
	>>
	define cross = (fun => l1 => l2 =>
		l1 | map {e1 => l2 | map {e1 | fun}}
	) >>
	define int-to-str = (i =>
		let unsigned-int-to-str =
			reverse
			. map {`+ 48`}
			. unfold-right {i => if (i != 0) then (i % 10; i / 10;) else ()}
		>> i |
			if (i > 0) then
				unsigned-int-to-str
			else-if (i < 0) then
				append {"-"} . unsigned-int-to-str . `0 -`
			else
				anything => "0"
	) >>
	define maximum =
		fold {greater}
	>>
	define minimum =
		fold {lesser}
	>>
	define merge = (merger => list =>
		let len = length {list} >>
		if (len > 1) then
			let len2 = len / 2 >>
			let list0 = (list | take {len2} | merge {merger}) >>
			let list1 = (list | drop {len2} | merge {merger}) >>
			merger {list0} {list1}
		else
			list
	) >>
	define range = (start => end => inc =>
		unfold-right {i => if (i < end) then (i; i + inc;) else ()} {start}
	) >>
	define starts-with = (
		match
		|| $sh; $st => (
			match
			|| sh; $t => starts-with {st} {t}
			|| otherwise false
		)
		|| otherwise (anything => true)
	) >>
	define split = (separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. filter {not . `=` {}}
		. tails . cons {separator}
	) >>
	define transpose = (m =>
		let height = length {m} >>
		let width = if (height > 0) then (m | head | length) else 0 >>
		if (width > 0) then
			let w1 = width - 1 >>
			let gets = (tail | repeat {w1} | tails | reverse) >>
			gets | map {f => map {head . apply {f}} {m}}
		else
			()
	) >>
	define contains = (m =>
		fold-left {or} {false} . map {m | starts-with} . tails
	) >>
	define dump = type (:t :- :t => list-of number) no-type-check (
		let dump-string = (s =>
			let length = prove-with-result /_s:s (string.length _s _l) _l >>
			0 until length | map {i =>
				prove-with-result /_s:s/_i:i (
					substring _s _i 0 _c, to.int _c _asc
				) _asc
			}
		) >>
		let dump0 = (prec => n =>
			if (is-list {n}) then
				concat {dump0 {true} {n | head}; "; "; dump0 {false} {n | tail};}
				| if prec then (s => concat {"("; s; ")";}) else id
			else-if (n = ()) then
				"()"
			else-if (prove /_n:n (is.atom _n)) then
				prove-with-result /_n:n (to.string _n _s) _s | dump-string
			else
				int-to-str {n}
		) >>
		dump0 {false}
	) >>
	define ends-with = (end =>
		starts-with {end | reverse} . reverse
	) >>
	define join = (separator =>
		concat . map {separator; | append/}
	) >>
	define merge-sort = (
		define merger = (list0 => list1 =>
			if-bind (list0 = ($h0; $t0)) then
				if-bind (list1 = ($h1; $t1)) then
					if:: h0 < h1
					then:: h0; merger {t0} {list1}
					else-if:: h0 > h1
					then:: h1; merger {list0} {t1}
					else:: h0; h1; merger {t0} {t1}
				else
					list0
			else
				list1
		) >>
		merge {merger}
	) >>
	define quick-sort = (cmp =>
		match
		|| $pivot; $t =>
			let filter0 = (not . cmp {pivot}) >>
			let filter1 = cmp {pivot} >>
			let l0 = (t | filter {filter0} | quick-sort {cmp}) >>
			let l1 = (t | filter {filter1} | quick-sort {cmp}) >>
			concat {l0; (pivot;); l1;}
		|| otherwise ()
	) >>
	.p
) #
