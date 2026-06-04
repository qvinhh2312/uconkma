import { normalizeError } from "@core/errors/normalizeError";
import { UconApiDataSource } from "@data/datasources/UconApiDataSource";
import { ApiDecisionResponse, DropCommand, RegisterCommand } from "@domain/entities/Decision";
import { RegistrationRepository } from "@domain/repositories/RegistrationRepository";

export class RegistrationRepositoryImpl implements RegistrationRepository {
  constructor(private readonly api: UconApiDataSource) {}

  async register(command: RegisterCommand): Promise<ApiDecisionResponse> {
    try {
      return await this.api.register(command);
    } catch (error) {
      throw normalizeError(error);
    }
  }

  async drop(command: DropCommand): Promise<ApiDecisionResponse> {
    try {
      return await this.api.drop(command);
    } catch (error) {
      throw normalizeError(error);
    }
  }
}
