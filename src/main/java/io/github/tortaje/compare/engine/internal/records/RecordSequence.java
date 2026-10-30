package io.github.tortaje.compare.engine.internal.records;

import io.github.tortaje.compare.engine.CompareOptions;
import io.github.tortaje.compare.engine.ComparisonException;
import io.github.tortaje.compare.engine.Difference;
import io.github.tortaje.compare.engine.DifferenceKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pairs expected and actual records: ordered, unordered by key, or unordered by equality. */
public final class RecordSequence {

    /** Maximum number of records of an unordered comparison without key. */
    public static final int MAX_UNKEYED = 10_000;

    private RecordSequence() {
    }

    /**
     * Compares two record sequences.
     *
     * @param format   the record format
     * @param expected expected records
     * @param actual   actual records
     * @param options  comparison options, {@code null} for ordered
     * @param <T>      record type
     * @return the differences, in emission order
     */
    public static <T> List<Difference> compare(RecordFormat<T> format, List<Rec<T>> expected,
                                               List<Rec<T>> actual, CompareOptions options) {
        List<Difference> out = new ArrayList<>();
        if (options == null || !options.isUnordered()) {
            ordered(format, expected, actual, out);
        } else if (!options.keyNames().isEmpty()) {
            keyed(format, expected, actual, options.keyNames(), out);
        } else {
            unkeyed(format, expected, actual, out);
        }
        return out;
    }

    private static <T> void ordered(RecordFormat<T> f, List<Rec<T>> expected, List<Rec<T>> actual,
                                    List<Difference> out) {
        int j = 0;
        for (int i = 0; i < expected.size(); i++) {
            Rec<T> e = expected.get(i);
            if (f.isIgnore(e.value())) {
                if (j >= actual.size()) {
                    continue;
                }
                int k = i + 1;
                while (k < expected.size() && f.isIgnore(expected.get(k).value())) {
                    k++;
                }
                if (k < expected.size() && matches(f, expected.get(k), actual.get(j))) {
                    continue;
                }
                j++;
            } else if (j >= actual.size()) {
                out.add(missing(f, e));
            } else {
                Rec<T> a = actual.get(j++);
                f.compare(f.label() + "[" + a.position() + "]", e.value(), a.value(), out);
            }
        }
        for (; j < actual.size(); j++) {
            out.add(unexpected(f, actual.get(j)));
        }
    }

    private static <T> void keyed(RecordFormat<T> f, List<Rec<T>> expected, List<Rec<T>> actual,
                                  List<String> key, List<Difference> out) {
        Map<String, ArrayDeque<Rec<T>>> byKey = new HashMap<>();
        List<Rec<T>> keyless = new ArrayList<>();
        for (Rec<T> a : actual) {
            String k = f.key(a.value(), key, false, a.position());
            if (k == null) {
                keyless.add(a);
            } else {
                byKey.computeIfAbsent(k, x -> new ArrayDeque<>()).addLast(a);
            }
        }
        for (Rec<T> e : expected) {
            String k = f.key(e.value(), key, true, e.position());
            ArrayDeque<Rec<T>> queue = k == null ? null : byKey.get(k);
            Rec<T> a = queue == null ? null : queue.pollFirst();
            if (a == null) {
                out.add(missing(f, e));
            } else {
                f.compare(f.label() + "[" + a.position() + "]", e.value(), a.value(), out);
            }
        }
        List<Rec<T>> left = new ArrayList<>(keyless);
        byKey.values().forEach(left::addAll);
        left.sort((x, y) -> Integer.compare(x.position(), y.position()));
        for (Rec<T> a : left) {
            out.add(unexpected(f, a));
        }
    }

    private static <T> void unkeyed(RecordFormat<T> f, List<Rec<T>> expected, List<Rec<T>> actual,
                                    List<Difference> out) {
        if (expected.size() > MAX_UNKEYED || actual.size() > MAX_UNKEYED) {
            throw new ComparisonException(
                    "unordered comparison without key is limited to " + MAX_UNKEYED + " records: use key");
        }
        boolean[] used = new boolean[actual.size()];
        for (Rec<T> e : expected) {
            if (f.isIgnore(e.value())) {
                continue;
            }
            boolean found = false;
            for (int j = 0; j < actual.size() && !found; j++) {
                if (!used[j] && matches(f, e, actual.get(j))) {
                    used[j] = true;
                    found = true;
                }
            }
            if (!found) {
                out.add(missing(f, e));
            }
        }
        for (int j = 0; j < actual.size(); j++) {
            if (!used[j]) {
                out.add(unexpected(f, actual.get(j)));
            }
        }
    }

    private static <T> boolean matches(RecordFormat<T> f, Rec<T> e, Rec<T> a) {
        return f.matches(e.value(), a.value());
    }

    private static <T> Difference missing(RecordFormat<T> f, Rec<T> e) {
        return new Difference("expected " + f.label() + "[" + e.position() + "]", DifferenceKind.MISSING,
                f.display(e.value()), null);
    }

    private static <T> Difference unexpected(RecordFormat<T> f, Rec<T> a) {
        return new Difference(f.label() + "[" + a.position() + "]", DifferenceKind.UNEXPECTED, null,
                f.display(a.value()));
    }
}
