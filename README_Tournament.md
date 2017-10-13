These instructions are intended for a tournament organizer. In theory, you're reading this because
you've just unzipped something called ReliefBot.zip. If that's not your situation, go look at README.md
instead.

1. Install Java 8.
2. Run `pip install py4j` on the command line (one time setup).
3. Copy ReliefBot.py your RLBot directory, as you would do with a plain python bot.
4. Modify rlbot.cfg and set up the match as you normally would.
3. Before (or after) running `python runner.py`, double click on `bin/ReliefBot.bat`.


Advanced:

- It's fine to close and restart ReliefBot.bat while runner.py is active.
- You can also run ReliefBot.bat on the command line to see stack traces for debugging purposes.
- If there is a port conflict, you can create a file called `port.txt` with a single number in it to override the port used. 
The file must be in the same directory as both ReliefBot.jar and ReliefBot.py.
