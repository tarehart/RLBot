package tarehart.rlbot.input;

import tarehart.rlbot.Bot;
import tarehart.rlbot.math.vector.Vector3;
import tarehart.rlbot.time.GameTime;

public class CarData {
    public final Vector3 position;
    public final Vector3 velocity;
    public final CarOrientation orientation;
    public final CarSpin spin;
    public final double boost;
    public final boolean isSupersonic;
    public final Bot.Team team;
    public final int playerIndex;
    public final GameTime time;
    public final long frameCount;
    public final boolean hasWheelContact;
    public boolean isDemolished;

    private CarData(Builder builder) {
        position = builder.position;
        velocity = builder.velocity;
        orientation = builder.orientation;
        spin = builder.spin;
        boost = builder.boost;
        isSupersonic = builder.isSupersonic;
        team = builder.team;
        playerIndex = builder.playerIndex;
        time = builder.time;
        frameCount = builder.frameCount;
        hasWheelContact = builder.hasWheelContact;
        isDemolished = builder.isDemolished;
    }


    public static final class Builder {
        private Vector3 position;
        private Vector3 velocity;
        private CarOrientation orientation;
        private CarSpin spin;
        private double boost;
        private boolean isSupersonic;
        private Bot.Team team;
        private int playerIndex;
        private GameTime time;
        private long frameCount;
        private boolean hasWheelContact;
        private boolean isDemolished;

        public Builder() {
        }

        public Builder withPosition(Vector3 position) {
            this.position = position;
            return this;
        }

        public Builder withVelocity(Vector3 velocity) {
            this.velocity = velocity;
            return this;
        }

        public Builder withOrientation(CarOrientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder withSpin(CarSpin spin) {
            this.spin = spin;
            return this;
        }

        public Builder withBoost(double boost) {
            this.boost = boost;
            return this;
        }

        public Builder withIsSupersonic(boolean isSupersonic) {
            this.isSupersonic = isSupersonic;
            return this;
        }

        public Builder withTeam(Bot.Team team) {
            this.team = team;
            return this;
        }

        public Builder withPlayerIndex(int playerIndex) {
            this.playerIndex = playerIndex;
            return this;
        }

        public Builder withTime(GameTime time) {
            this.time = time;
            return this;
        }

        public Builder withFrameCount(long frameCount) {
            this.frameCount = frameCount;
            return this;
        }

        public Builder withHasWheelContact(boolean hasWheelContact) {
            this.hasWheelContact = hasWheelContact;
            return this;
        }

        public Builder withIsDemolished(boolean isDemolished) {
            this.isDemolished = isDemolished;
            return this;
        }

        public CarData build() {
            return new CarData(this);
        }
    }
}
