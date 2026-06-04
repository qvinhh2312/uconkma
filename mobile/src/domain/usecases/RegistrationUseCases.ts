import { DropCommand, RegisterCommand } from "@domain/entities/Decision";
import { RegistrationRepository } from "@domain/repositories/RegistrationRepository";

export class RegistrationUseCases {
  constructor(private readonly registrationRepository: RegistrationRepository) {}

  register(command: RegisterCommand) {
    return this.registrationRepository.register(command);
  }

  drop(command: DropCommand) {
    return this.registrationRepository.drop(command);
  }
}
