consult "asm.${platform}.fp" ~

expand null := pointer:number 0 ~
expand buffer.size := 256 ~
expand (assert .check ~ .expr) := if .check then .expr else error ~
expand !adjust.pointer .pointer .add := pointer:numberp !asm.adjust.pointer (type address.of.any .pointer) .add ~
expand !peek .pointer := !asm.peek type address.of.any .pointer ~
expand (!poke (.pointer, .value) ~ .expr) := (perform !do !asm.poke (type address.of.any .pointer) .value ~ .expr) ~

define max (a, b) := if (a < b) then b else a ~
define min (a, b) := if (a < b) then a else b ~

define !mmap length := !do
	pointer:numberp !asm.mmap length
~

define !munmap (length, pointer) := !do
	!asm.munmap length numberp:pointer pointer
~

let.global alloc.pointer := null ~
let.global alloc.free.chain := null ~

define !alloc size0 := !do
	let size1 := max (os.ps, size0) ~
	let sizep := numberp:number size1 ~
	define {
		!alloc.chain pointer := !do
			let chain := ^(pointer:pointer pointer) ~
			if (chain != null) then (
				let pointer1 := !adjust.pointer chain os.ps ~
				if (^(pointer:pointer chain) != sizep) then (
					!alloc.chain pointer1
				) else (
					assign ^pointer := ^(pointer:pointer pointer1) ~
					chain
				)
			) else null
		~
	} ~
	let p0 := !alloc.chain address.of alloc.free.chain ~
	if (p0 = null) then (
		let ap := alloc.pointer ~
		let pointer.head := if (ap != null) then ap else !mmap 16384 ~
		let pointer.block := !adjust.pointer pointer.head os.ps ~
		assign ^pointer.head := sizep ~
		assign alloc.pointer := !adjust.pointer pointer.block size1 ~
		pointer.block
	) else p0
~

define !dealloc (size0, pointer.block) := !do
	let sizep := numberp:number max (os.ps, size0) ~
	let pointer.head := !adjust.pointer pointer.block (0 - os.ps) ~
	assert (sizep = ^pointer.head) ~
	assign ^(pointer:pointer pointer.block) := alloc.free.chain ~
	assign alloc.free.chain := pointer.head ~
	{}
~

define !new.pool length := !do
	let pool := !mmap length ~
	{
		destroy {} := !munmap (length, pool) ~
		pool ~
		start := 0 ~
	}
~

define !new.mut.number init := !do
	type init = number ~
	let size := size.of init ~
	let address := !alloc size ~
	let pointer := type (address.of number) address ~
	assign ^pointer := init ~
	{
		destroy {} := !dealloc (size, address) ~
		get {} := !do (^pointer) ~
		set v1 := !do (assign ^pointer := v1 ~ {}) ~
	}
~

define !read (pointer, length) := !do
	type pointer = address.of.any ~
	!asm.read pointer length
~

define !write (pointer, length) := !do
	type pointer = address.of.any ~
	!asm.write pointer length
~

define !write.all (pointer, length) :=
	type pointer = address.of.any ~
	!for (n = length; 0 < n;
		let p1 := !adjust.pointer pointer (length - n) ~
		let n1 := !write (p1, n) ~
		assert (n1 != 0) ~
		n - n1
	)
~

define !get.char {} := !do
	let.global buffer := array buffer.size * byte ~
	let.global start.end := (0, 0) ~
	let (s0, e0) := start.end ~
	let (s1, e1) := if (s0 < e0) then start.end else (0, !read (address.of buffer, buffer.size)) ~
	assert (s1 < e1) ~
	assign start.end := (s1 + 1, e1) ~
	buffer [s0]
~

define !put.char ch := !write.all (address.of predef [type byte ch,], 1) ~

define !put.number n :=
	define {
		!put.number_ n := !do
			if (0 < n) then (
				let div := n / 10 ~
				let mod := n % 10 ~
				!put.number_ div ~
				!put.char byte:number (mod + number '0')
			) else {}
		~
	} ~
	case
	|| 0 < n =>
		!put.number_ n
	|| n < 0 => !do
		!put.char byte '-' ~
		!put.number_ n
	|| !put.char byte '0'
~

define !put.string s :=
	!for (i = 0; (^s) [i] != byte 0;
		!put.char (^s) [i] ~
		i + 1
	)
~

define !cat {} :=
	!for (n = 1; n != 0;
		let pointer := address.of predef (array buffer.size * byte) ~
		let nBytesRead := !read (pointer, buffer.size) ~
		!write.all (pointer, nBytesRead) ~
		nBytesRead
	)
~

{
	!alloc,
	!cat,
	!dealloc,
	!get.char,
	!mmap,
	!munmap,
	!put.char,
	!put.number,
	!put.string,
	!read,
	!write,
}
