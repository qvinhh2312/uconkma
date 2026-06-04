import { PapPolicy, PapSummary } from "@domain/entities/Policy";

export interface PapRepository {
  listPolicies(): Promise<PapPolicy[]>;
  getSummary(): Promise<PapSummary>;
  transitionPolicy(policyId: string, targetStatus: string): Promise<unknown>;
  reloadPolicies(): Promise<unknown>;
}
