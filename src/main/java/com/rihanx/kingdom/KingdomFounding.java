package com.rihanx.kingdom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Pure validation for founding a kingdom — names, radius, overlap, ownership caps.
 */
public final class KingdomFounding {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{2,23}$");

    public enum Failure {
        NAME_INVALID,
        EXISTS,
        LIMIT,
        RADIUS,
        OVERLAP
    }

    public record Result(@Nullable Failure failure, @Nullable String detail, int radius) {
        public boolean ok() {
            return failure == null;
        }

        public static @NotNull Result success(int radius) {
            return new Result(null, null, radius);
        }

        public static @NotNull Result fail(@NotNull Failure failure, @Nullable String detail) {
            return new Result(failure, detail, -1);
        }
    }

    private KingdomFounding() {
    }

    public static boolean validName(@Nullable String raw) {
        return raw != null && NAME_PATTERN.matcher(raw).matches();
    }

    public static @NotNull String normalizeId(@NotNull String raw) {
        return raw.toLowerCase(Locale.ROOT);
    }

    public static int resolveRadius(@Nullable Integer requested, int defaultRadius, int min, int max) {
        int radius = requested != null ? requested : defaultRadius;
        if (radius < min || radius > max) {
            return -1;
        }
        return radius;
    }

    public static boolean overlapsAny(@NotNull KingdomBounds candidate, @NotNull Iterable<KingdomBounds> existing) {
        for (KingdomBounds other : existing) {
            if (candidate.overlaps(other)) {
                return true;
            }
        }
        return false;
    }

    public static @NotNull Result validateCreate(
            @Nullable String rawName,
            @Nullable Integer requestedRadius,
            int alreadyOwned,
            int maxOwned,
            boolean bypassLimit,
            int defaultRadius,
            int minRadius,
            int maxRadius,
            boolean nameTaken,
            @NotNull KingdomBounds candidate,
            @NotNull Iterable<KingdomBounds> existingBounds
    ) {
        if (!validName(rawName)) {
            return Result.fail(Failure.NAME_INVALID, rawName);
        }
        if (nameTaken) {
            return Result.fail(Failure.EXISTS, rawName);
        }
        if (!bypassLimit && alreadyOwned >= maxOwned) {
            return Result.fail(Failure.LIMIT, String.valueOf(maxOwned));
        }
        int radius = resolveRadius(requestedRadius, defaultRadius, minRadius, maxRadius);
        if (radius < 0) {
            return Result.fail(Failure.RADIUS, requestedRadius == null ? String.valueOf(defaultRadius) : String.valueOf(requestedRadius));
        }
        if (overlapsAny(candidate, existingBounds)) {
            return Result.fail(Failure.OVERLAP, null);
        }
        return Result.success(radius);
    }
}
