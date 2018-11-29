## Running Integration Tests

- Configure rlbot.cfg so that there is an rlbot
with player index 0 on the blue team
  - Currently you can copy the contents of rlbot-configuration.cfg into rlbot.cfg to accomplish this.
- Launch the python framework, e.g. with run-framework.bat
- Use IntelliJ to run the integration tests

You can expect each test to use game state manipulation
to construct a scenario and measure the bot's performance.