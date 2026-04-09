package net.dark.editorapi.model;

public enum InterpolationMode {
    STEP,
    LINEAR,
    SMOOTH,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    CUBIC;

    public String label() {
        return switch (this) {
            case STEP -> "Step";
            case LINEAR -> "Linear";
            case SMOOTH -> "Smooth";
            case EASE_IN -> "Ease In";
            case EASE_OUT -> "Ease Out";
            case EASE_IN_OUT -> "Ease In-Out";
            case CUBIC -> "Cubic";
        };
    }

    public double apply(double value) {
        return switch (this) {
            case STEP -> 0.0D;
            case LINEAR -> value;
            case SMOOTH -> value * value * (3.0D - 2.0D * value);
            case EASE_IN -> value * value;
            case EASE_OUT -> 1.0D - Math.pow(1.0D - value, 2.0D);
            case EASE_IN_OUT -> value < 0.5D
                    ? 2.0D * value * value
                    : 1.0D - Math.pow(-2.0D * value + 2.0D, 2.0D) / 2.0D;
            case CUBIC -> value * value * value;
        };
    }

    public InterpolationMode next() {
        InterpolationMode[] modes = values();
        return modes[(this.ordinal() + 1) % modes.length];
    }
}
