import { api } from "./client.js";

export async function listPolicies() {
  const response = await api.get("/pap/policies");
  return response.data;
}

export async function getPapSummary() {
  const response = await api.get("/pap/summary");
  return response.data;
}

export async function transitionPolicy(policyId, targetStatus) {
  const response = await api.post("/pap/transition", null, {
    params: { policyId, targetStatus },
  });
  return response.data;
}

export async function reloadPolicies() {
  const response = await api.post("/pap/reload");
  return response.data;
}
