package ugcs.processing;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public abstract class AbstractProcessor {
    private final ConcurrentMap<String, Object> evaluatedFields = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    protected final <T> T evaluateField(String fieldName, Supplier<T> evaluator) {
        return (T) evaluatedFields.computeIfAbsent(fieldName, k -> evaluator.get());
    }
}
