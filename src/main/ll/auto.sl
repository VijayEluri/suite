append () .list .list #
append (.head, .tail) .list (.head, .tail1) :- append .tail .list .tail1 #

clear :- retract.all (_ :- _), ! #

member (.e, _) .e #
member (_, .tail) .e :- member .tail .e #

repeat #
repeat :- repeat #

sum .a .b .c :- bound .a, bound .b, let .c (.a - .b) #
sum .a .b .c :- bound .a, bound .c, let .b (.a - .c) #
sum .a .b .c :- bound .b, bound .c, let .a (.b + .c) #

trace .pred :- asserta (.pred .ps :- dump .pred .ps, nl, fail) #

yes #
