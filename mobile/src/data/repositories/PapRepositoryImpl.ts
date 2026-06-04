import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { PapPolicy, PapSummary } from "@domain/entities/Policy";
import { PapRepository } from "@domain/repositories/PapRepository";

export class PapRepositoryImpl implements PapRepository {
  constructor(private readonly api: UconApiDataSource) {}

  async listPolicies(): Promise<PapPolicy[]> {
    try {
      return await this.api.listPolicies();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async getSummary(): Promise<PapSummary> {
    try {
      return await this.api.getPapSummary();
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async transitionPolicy(policyId: string, targetStatus: string): Promise<unknown> {
    try {
      return await this.api.transitionPolicy(policyId, targetStatus);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async reloadPolicies(): Promise<unknown> {
    try {
      return await this.api.reloadPolicies();
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
