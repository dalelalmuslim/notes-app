package com.example.notely.model;

/**
 * Semantic version (MAJOR.MINOR.PATCH) with numeric comparison.
 *
 * Comparisons are numeric, never lexicographic, so 1.10.0 sorts after 1.9.0.
 * A leading "v"/"V" (common in Git tags) is accepted. Malformed strings are
 * rejected by {@link #parse(String)} returning {@code null}.
 */
public final class AppVersion implements Comparable<AppVersion> {

    private final int major;
    private final int minor;
    private final int patch;

    private AppVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static AppVersion parse(String version) {
        if (version == null) {
            return null;
        }
        String value = version.trim();
        if (value.startsWith("v") || value.startsWith("V")) {
            value = value.substring(1);
        }
        String[] parts = value.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        int[] numbers = new int[3];
        for (int i = 0; i < 3; i++) {
            if (parts[i].isEmpty() || !parts[i].matches("\\d+")) {
                return null;
            }
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return new AppVersion(numbers[0], numbers[1], numbers[2]);
    }

    @Override
    public int compareTo(AppVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
