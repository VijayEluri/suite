"use strict";

var log = function(m) {
	var text = document.getElementById("log").value + m + "\n";
	if (text.length > 256) text = text.substring(text.length - 256, text.length);
	document.getElementById("log").value = text;
	// alert(m);
};

var keyboard = function(document) {
	var tokeycode = function(e) { return (!(e.which)) ? e.keyCode : (e.which ? e.which : 0); };

	var ispressed = {};
	document.onkeydown = function(e) { ispressed[tokeycode(e)] = true; };
	document.onkeyup = function(e) { ispressed[tokeycode(e)] = false; };

	// log("onkeydown = " + tokeycode(e) + "/" + String.fromCharCode(tokeycode(e)));

	return {
		dirx: function() { return ispressed[37] ? -1 : (ispressed[39] ? 1 : 0); }
		, diry: function() { return ispressed[38] ? -1 : (ispressed[40] ? 1 : 0); }
		, paused: function() { return ispressed[80]; }
		, pressed: function(keycode) {
			return ispressed[keycode];
		}
	};
};

var mouse = function(document) {
	var x, y;
	var down;

	document.onmousemove = function(e) {
		var e1 = (!e) ? window.event : e;
		if (e1.pageX || e1.pageY) {
			x = e1.pageX;
			y = e1.pageY;
		} else if (e1.clientX || e1.clientY) {
			x = e1.clientX + document.body.scrollLeft + document.documentElement.scrollLeft;
			y = e1.clientY + document.body.scrollTop + document.documentElement.scrollTop;
		}
		log("onmousemove = " + x + ", " + y);
	};
	document.onmousedown = function(e) { down = true; };
	document.onmouseup = function(e) { down = false; };
};

var controller = function(canvas, keyboard, mouse, objects) {
	var width = canvas.width, height = canvas.height;

	var context = canvas.getContext("2d");

	var repaint = function(context, objects) {
		context.fillStyle = "#777777";
		context.fillRect(0, 0, width, height);

		context.fillStyle = "#000000";
		context.font = "10px Helvetica";
		context.fillText("Demo", 16, height - 16);

		map(function(object) { object.draw(context); }, objects);
	};

	return {
		tick: function() {
			if (!keyboard.paused()) {
				objects = concat(map(function(object) {
					object.input(keyboard, mouse);
					object.move();
					return object.spawn();
				}, objects));

				repaint(context, objects);
			}
		}
	};
};
