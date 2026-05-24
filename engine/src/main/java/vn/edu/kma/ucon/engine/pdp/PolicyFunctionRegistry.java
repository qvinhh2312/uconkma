package vn.edu.kma.ucon.engine.pdp;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import vn.edu.kma.ucon.engine.pep.UconRequest;
import vn.edu.kma.ucon.engine.pip.entity.ClassSection;
import vn.edu.kma.ucon.engine.pip.entity.Student;

@Component
public class PolicyFunctionRegistry {

    private final Map<String, FunctionSpec> functions;

    public PolicyFunctionRegistry(PolicyFunctionExecutor executor) {
        this.functions = List.of(
                new FunctionSpec("isEmpty", 1, ReturnType.BOOLEAN, Set.of("PRE", "ONGOING", "POST"),
                        args -> executor.isEmpty(args.get(0))),
                new FunctionSpec("checkExistsRegistration", 3, ReturnType.BOOLEAN, Set.of("PRE", "ONGOING"),
                        args -> executor.checkExistsRegistration(
                                stringArg(args.get(0)),
                                stringArg(args.get(1)),
                                stringArg(args.get(2))))
        ).stream().collect(Collectors.toMap(FunctionSpec::name, Function.identity()));
    }

    public FunctionSpec getRequired(String name) {
        FunctionSpec spec = functions.get(name);
        if (spec == null) {
            throw new IllegalStateException("Unknown DSL function: " + name);
        }
        return spec;
    }

    public Optional<FunctionSpec> get(String name) {
        return Optional.ofNullable(functions.get(name));
    }

    private static String stringArg(Object value) {
        return value == null ? null : value.toString();
    }

    public enum ReturnType {
        BOOLEAN
    }

    public record EvaluationContext(Student subject,
                                    ClassSection object,
                                    Environment environment,
                                    UconRequest request) {
    }

    @FunctionalInterface
    public interface FunctionImplementation {
        Object apply(List<Object> args);
    }

    public record FunctionSpec(String name,
                               int arity,
                               ReturnType returnType,
                               Set<String> allowedPhases,
                               FunctionImplementation implementation) {
        public FunctionSpec {
            Objects.requireNonNull(name);
            Objects.requireNonNull(returnType);
            Objects.requireNonNull(allowedPhases);
            Objects.requireNonNull(implementation);
        }
    }
}
