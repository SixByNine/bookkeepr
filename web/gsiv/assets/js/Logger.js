/**
 * JSLogger (via Meta Tags)
 * @author Dan Allen (http://www.mojavelinux.com)
 * TODO: could tie this in with AJAX to log to a server
 */

function LogLevel() {}
LogLevel.TRACE = 0;
LogLevel.DEBUG = 1;
LogLevel.WARN = 2;
LogLevel.ERROR = 3;

LogLevel.valueOf = function(level) {
	switch (level) {
		case LogLevel.TRACE:
			return "TRACE";

		case LogLevel.DEBUG:
			return "DEBUG";

		case LogLevel.WARN:
			return "WARN";

		case LogLevel.ERROR:
			return "ERROR";
	}
}

function Logger() {}
Logger.level = LogLevel.DEBUG;
Logger.defaultLevel = LogLevel.DEBUG;
Logger.currentLine = 1;

Logger.isLoggable = function(level) {
	return (level >= Logger.level);
}

Logger.log = function (message, level) {
	if (typeof(level) == 'undefined') {
		level = Logger.defaultLevel;
	}

	if (!Logger.isLoggable(level))	{
		return;
	}

	if (typeof message == 'number') {
		message = new String(message);
	}
	else if (message && typeof message != 'string') {
		message = message.toString();
	}

	var lines = message.split("\n");
	var headTag = document.getElementsByTagName("head")[0];

	for (var i = 1; i <= lines.length; i++) {
		var value = lines[i - 1];

		if (i == 1) {
			value = LogLevel.valueOf(level) + ": " + value;
		}
		else
		{
			value = "> " + value;
		}

		var metaTag = document.createElement("meta");
		metaTag.setAttribute("name", "X-jslog:" + Logger.currentLine++);
		metaTag.setAttribute("content", value);
		headTag.appendChild(metaTag);
	}
}

// make a convenience method log()
log = Logger.log;
