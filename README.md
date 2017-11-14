# ReliefBot

## Initial RLBot Setup

ReliefBot uses a framework called RLBot. 
- General description: https://github.com/drssoccer55/RLBot
- Initial setup instructions: https://github.com/drssoccer55/RLBot/wiki/Setup-Instructions

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
2. Prepare a game in Rocket League, per the instructions at https://github.com/drssoccer55/RLBot/wiki/Setup-Instructions
3. Launch the java component of ReliefBot. You've got options:
   - In IntelliJ, right click on `java/src/tarehart/rlbot/GrpcServer.java` and choose Run or Debug
   - Or, on the command line, run `gradlew.bat run`
4. On the command line, run `python runner.py`

## Tournament Submissions

If you want to submit ReliefBot to a tournament, you can generate a nice zip file with `gradlew.bat distZip`.
The zip will automatically contain a README which explains to the tournament organizer how to run ReliefBot.
