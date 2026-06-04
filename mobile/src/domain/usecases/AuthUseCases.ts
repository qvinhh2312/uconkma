import { LoginCommand } from "@domain/entities/AuthSession";
import { AuthRepository } from "@domain/repositories/AuthRepository";

export class AuthUseCases {
  constructor(private readonly authRepository: AuthRepository) {}

  login(command: LoginCommand) {
    return this.authRepository.login(command);
  }

  logout() {
    return this.authRepository.logout();
  }

  getStoredSession() {
    return this.authRepository.getStoredSession();
  }
}
