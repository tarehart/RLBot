# ReliefBot

## Initial RLBot Setup

ReliefBot uses a framework called RLBot.
- Website: http://www.rlbot.org
- Framework: https://github.com/RLBot/RLBot

## ReliefBot Development Environment Setup

1. Make sure you've installed Python 3.6.5 or newer. Here's [Python 3.7 64 bit](https://www.python.org/ftp/python/3.7.0/python-3.7.0-amd64.exe). Some older versions like 3.6.0 will not work. During installation:
   - Select "Add Python to PATH"
   - Make sure pip is included in the installation
1. Make sure you've installed the Java 8 JDK or newer. Here's the [Java 10 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html).
1. Make sure you've [set the JAVA_HOME environment variable](https://javatutorial.net/set-java-home-windows-10).
1. Install Intellij IDEA along with the Kotlin and Python plugins
1. Navigate to this directory on the command line
1. Run `gradlew.bat idea`
1. Open IntelliJ and open this folder as a project

There is also a C++ component, but I expect it to be so temporary that
it's not worth documenting. If I was wrong, ask me.


## Running ReliefBot

1. Look in rlbot.cfg and modify as desired.
2. Open Rocket League.
3. Launch the java component of ReliefBot. You've got options:
   - In IntelliJ, right click on `src/main/java/tarehart/rlbot/ReliefBotMain.kt` and choose Run or Debug
   - Or, on the command line, run `gradlew.bat run`
4. On the command line, run `python runner.py`

## Tournament Submissions

If you want to submit ReliefBot to a tournament, you can generate a nice zip file with `gradlew.bat distZip`.
The zip will automatically contain a README which explains to the tournament organizer how to run ReliefBot.
