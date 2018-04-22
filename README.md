# ReliefBot

## Initial RLBot Setup

ReliefBot uses a framework called RLBot.
- General description: https://github.com/RLBot/RLBot
- Initial setup instructions: https://github.com/RLBot/RLBot/wiki/Setup-Instructions-%28current%29

## ReliefBot Development Environment Setup

For deluxe instructions, check out https://github.com/aherbig/ReliefBotSetupInstructions,
courtesy of Andreas, a.k.a. groen.

In brief:

1. Install Java 8 JDK
2. Install Intellij IDEA
3. Navigate to this directory on the command line
4. Run `gradlew.bat idea`
5. Open IntelliJ and open this folder as a project


## Running ReliefBot

1. Look in rlbot.cfg and modify as desired.
2. Launch the java component of ReliefBot. You've got options:
   - In IntelliJ, right click on `java/src/tarehart/rlbot/GrpcServer.java` and choose Run or Debug
   - Or, on the command line, run `gradlew.bat run`
3. On the command line, run `python runner.py`

For more advanced ways to use rlbot.cfg, see https://github.com/RLBot/RLBot/wiki/Setup-Instructions-%28current%29

## Tournament Submissions

If you want to submit ReliefBot to a tournament, you can generate a nice zip file with `gradlew.bat distZip`.
The zip will automatically contain a README which explains to the tournament organizer how to run ReliefBot.
