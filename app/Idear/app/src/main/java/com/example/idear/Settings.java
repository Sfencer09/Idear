package com.example.idear;

public class Settings {
        private static Settings instance = new Settings();

        private double speed;
        private int voiceOption;

        private Settings() {
            speed = 1.0;
            voiceOption = 0;
        }

        public static Settings getInstance() { return instance; }

        public double getSpeed() {
            return speed;
        }

        public int voiceOption() {
            return voiceOption;
        }

        public void setSpeed(double sp) {
            speed = sp;
        }

        public void setVoiceOption(int opt) {
            if (opt >= 0 && opt < 3) {
                voiceOption = opt;
            }
        }
    }

    double speed = Settings.getInstance().getSpeed();
Settings.getInstance().setSpeed(speed + 0.1 );

}
