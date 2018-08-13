# ReliefBot

This is a custom Rocket League bot designed to work offline only.
For more context, see http://www.rlbot.org/

## Environment Setup

1. Make sure you've installed Python 3.6.5 or newer. Here's [Python 3.7 64 bit](https://www.python.org/ftp/python/3.7.0/python-3.7.0-amd64.exe). Some older versions like 3.6.0 will not work. During installation:
   - Select "Add Python to PATH"
   - Make sure pip is included in the installation
1. Make sure you've installed the Java 8 JDK or newer. Here's the [Java 10 JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk10-downloads-4416644.html).
1. Make sure you've [set the JAVA_HOME environment variable](https://javatutorial.net/set-java-home-windows-10).


## Running ReliefBot

1. Look in rlbot.cfg and modify as desired.
1. Open Rocket League.
1. On the command line, run `run-bot.bat` to start ReliefBots' brain.
The console output may show a percentage less than 100%, but once a little
java window pops up, it's ready to go.
1. On the command line, run `run-framework.bat`. This should start up a game.

For more advanced ways to use rlbot.cfg, see https://github.com/RLBot/RLBot/wiki/Setup-Instructions-%28current%29

If you have trouble with these instructions, see if you can at least get
https://github.com/RLBot/RLBotJavaExample working. Same general concept,
but more thorough instructions (including video tutorial).

## ReliefBot Development Environment Setup

1. Install Intellij IDEA along with the Kotlin and Python plugins
1. Navigate to this directory on the command line
1. Run `gradlew.bat idea`
1. Open IntelliJ and open this folder as a project

You now have the option of running ReliefBot directly from IntelliJ. Different options:
- In the gradle tool view, right click on the 'run' task and choose Run or Debug.
- OR, right click on `src/main/java/tarehart/rlbot/ReliefBotMain.kt` and choose Run or Debug.
   If you do this, you will need to update that run configuration to include the JVM args
   defined in build.gradle, e.g. java.library.path.


There is also a C++ component, but I expect it to be so temporary that
it's not worth documenting. If I was wrong, ask me.


## Tournament Submissions

If you want to submit ReliefBot to a tournament, you can generate a nice zip file with `gradlew.bat distZip`.
The zip will automatically contain a README which explains to the tournament organizer how to run ReliefBot.
