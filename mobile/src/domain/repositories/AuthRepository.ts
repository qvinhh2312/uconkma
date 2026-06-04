import { AuthSession, LoginCommand } from "@domain/entities/AuthSession";

export interface AuthRepository {
  login(command: LoginCommand): Promise<AuthSession>;
  logout(): Promise<void>;
  getStoredSession(): Promise<AuthSession | null>;
}
