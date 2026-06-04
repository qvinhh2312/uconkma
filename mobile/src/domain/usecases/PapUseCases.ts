import { PapRepository } from "@domain/repositories/PapRepository";

export class PapUseCases {
  constructor(private readonly papRepository: PapRepository) {}

  listPolicies() {
    return this.papRepository.listPolicies();
  }

  getSummary() {
    return this.papRepository.getSummary();
  }

  transitionPolicy(policyId: string, targetStatus: string) {
    return this.papRepository.transitionPolicy(policyId, targetStatus);
  }

  reloadPolicies() {
    return this.papRepository.reloadPolicies();
  }
}
