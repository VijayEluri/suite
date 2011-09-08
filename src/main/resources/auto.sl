append () .list .list #
append (.head, ..tail) .list (.head, ..tail1) :- append ..tail .list ..tail1 #

enable-trace
	:- to.atom ".call" .call
	, asserta (.call :- write 'TRACE: ', dump .call, nl, fail)
#

file-read .fn .contents
	:- concat "s = org.util.IoUtil.readStream(new java.io.FileInputStream('" .fn "'))" .js
	, eval.js .js .contents
#

file-write .fn .contents
	:- concat "org.util.IoUtil.writeStream(new java.io.FileOutputStream('" .fn "'), '" .contents "')" .js
	, eval.js .js _
#

if .if then .then _ :- .if, !, .then #
if _ then _ else-if .elseIf :- !, if .elseIf #
if _ then _ else .else :- .else #

member (.e, _) .e #
member (_, ..tail) .e :- member ..tail .e #

pp-list .n :- bound .n, .n = (.a, .b), !, pp-list .a, write '%0A, ', pp-list .b #
pp-list .n :- dump .n #

repeat #
repeat :- repeat #

replace .t0/.t1 .from/.to
	:- bound .t0, .t0 = .from, !, .t1 = .to
#
replace .t0/.t1 .from/.to
	:- tree .t0 .left0 .operator .right0
	, tree .t1 .left1 .operator .right1
	, !, replace .left0/.left1 .from/.to, replace .right0/.right1 .from/.to
#
replace .node/.node _/_ #

sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #
sum .a .b .c :- bound .a, bound .c, let .a (.a - .c) #
sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #

use .fn :- IMPORTED .fn; assert IMPORTED .fn, import .fn #

whatever .g :- .g; yes #

yes #

() :- write 'READY', nl #
