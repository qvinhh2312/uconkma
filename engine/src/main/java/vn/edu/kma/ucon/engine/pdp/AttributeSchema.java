package vn.edu.kma.ucon.engine.pdp;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class AttributeSchema {

    public record AttributeRule(String type, boolean mutable) {
    }

    private final Map<String, Map<String, AttributeRule>> rulesByScope;

    @SuppressWarnings("unchecked")
    public AttributeSchema() {
        this.rulesByScope = new HashMap<>();

        try (InputStream input = new ClassPathResource("attribute-schema.yml").getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(input);
            for (Map.Entry<String, Object> scopeEntry : root.entrySet()) {
                Map<String, AttributeRule> scopedRules = new HashMap<>();
                Map<String, Object> attrs = (Map<String, Object>) scopeEntry.getValue();
                for (Map.Entry<String, Object> attrEntry : attrs.entrySet()) {
                    Map<String, Object> definition = (Map<String, Object>) attrEntry.getValue();
                    String type = String.valueOf(definition.get("type"));
                    boolean mutable = Boolean.parseBoolean(String.valueOf(definition.get("mutable")));
                    scopedRules.put(attrEntry.getKey(), new AttributeRule(type, mutable));
                }
                rulesByScope.put(scopeEntry.getKey().toUpperCase(), scopedRules);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load attribute-schema.yml", ex);
        }
    }

    public boolean hasAttribute(String scope, String path) {
        return rules(scope).containsKey(path);
    }

    public boolean isMutable(String scope, String path) {
        AttributeRule rule = rules(scope).get(path);
        return rule != null && rule.mutable();
    }

    public AttributeRule ruleFor(String scope, String path) {
        return rules(scope).get(path);
    }

    public Map<String, AttributeRule> rules(String scope) {
        return rulesByScope.getOrDefault(scope.toUpperCase(), Map.of());
    }
}
