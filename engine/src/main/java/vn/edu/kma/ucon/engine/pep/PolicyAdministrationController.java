package vn.edu.kma.ucon.engine.pep;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.kma.ucon.engine.pdp.PolicyLifecycleInfo;
import vn.edu.kma.ucon.engine.pdp.PolicyLifecycleService;

@RestController
@RequestMapping("/api/pap")
public class PolicyAdministrationController {

    private final PolicyLifecycleService policyLifecycleService;

    public PolicyAdministrationController(PolicyLifecycleService policyLifecycleService) {
        this.policyLifecycleService = policyLifecycleService;
    }

    @GetMapping("/policies")
    public ResponseEntity<?> listPolicies() {
        return ResponseEntity.ok(policyLifecycleService.listPolicies());
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(policyLifecycleService.summarizeStatuses());
    }

    @PostMapping("/transition")
    public ResponseEntity<?> transition(@RequestParam String policyId, @RequestParam String targetStatus) {
        PolicyLifecycleInfo info = policyLifecycleService.transitionPolicy(policyId, targetStatus);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("policyId", info.policyId());
        response.put("status", info.status());
        response.put("runtimeActivePolicies", policyLifecycleService.listRuntimePolicyIds().size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reload")
    public ResponseEntity<?> reload() {
        policyLifecycleService.reloadFromDisk();
        return ResponseEntity.ok(Map.of(
                "message", "Policy model reloaded from disk.",
                "runtimeActivePolicies", policyLifecycleService.listRuntimePolicyIds().size()));
    }
}
