import { ApiDecisionResponse, DropCommand, RegisterCommand } from "@domain/entities/Decision";

export interface RegistrationRepository {
  register(command: RegisterCommand): Promise<ApiDecisionResponse>;
  drop(command: DropCommand): Promise<ApiDecisionResponse>;
}
