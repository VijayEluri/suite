expand (!asm.adjust.pointer .p .add) := asm (RAX = .p; EBX = .add;) { ADD (RAX, RBX); }/RAX ~
expand (!asm.mmap .length) := asm (EAX = 9; EDI = 0; ESI = .length; EDX = 3; R10D = 34; R8D = -1; R9D = 0;) { SYSCALL (); }/RAX ~
expand (!asm.munmap .length .p) := asm (EAX = 11; RDI = .p; ESI = .length;) { SYSCALL (); }/EAX ~
expand (!asm.peek .p) := asm (RBX = .p;) { MOV (RAX, QWORD `RBX`); }/RAX ~
expand (!asm.poke .p .value) := asm (RAX = .value; RBX = .p;) { MOV (QWORD `RBX`, RAX); }/RAX ~
expand (!asm.read .p .length) := asm (EAX = 0; EDI = 0; RSI = .p; EDX = .length;) { SYSCALL (); }/EAX ~
expand (!asm.write .p .length) := asm (EAX = 1; EDI = 1; RSI = .p; EDX = .length;) { SYSCALL (); }/EAX ~

expand os.ps := 8 ~
